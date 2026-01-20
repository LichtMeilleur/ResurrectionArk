package com.licht_meilleur.resurrection_ark.block;

import com.licht_meilleur.resurrection_ark.block.entity.ResurrectionArkBlockEntity;
import com.licht_meilleur.resurrection_ark.screen.ResurrectionArkScreenHandler;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class ResurrectionArkBlock extends Block implements BlockEntityProvider {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    // ★無引数でOK
    public ResurrectionArkBlock() {
        super(AbstractBlock.Settings.copy(Blocks.STONE).strength(4f));
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    // ★これが無いと BlockEntity が存在しないので GUI が絶対に開かない
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ResurrectionArkBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;

        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof ResurrectionArkBlockEntity arkBe)) {
            return ActionResult.PASS;
        }

        // ✅ しゃがみ右クリック：リンク開始
        if (player.isSneaking()) {
            com.licht_meilleur.resurrection_ark.ArkLinkState.set(player.getUuid(), pos);
            player.sendMessage(Text.literal("Arkにリンクしました。次に所有Mobをしゃがみ+右クリックで登録。"), false);
            return ActionResult.CONSUME;
        }

        // ✅ 通常右クリック：GUI
        arkBe.refreshAliveStatesOnServer();  // 先に状態確定
        arkBe.markDirty();                   // 保存フラグ
        world.updateListeners(pos, state, state, 3); // クライアントへBE更新
        player.openHandledScreen(arkBe);
        return ActionResult.CONSUME;

    }
}