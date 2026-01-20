package com.licht_meilleur.resurrection_ark.screen;

import com.licht_meilleur.resurrection_ark.data.ResurrectionData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity; // 修正: 正しいパッケージ
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ResurrectionArkScreenHandler extends ScreenHandler {
    private final ServerWorld world;
    private final BlockPos pos;

    public ResurrectionArkScreenHandler(int syncId, PlayerInventory playerInventory, ServerWorld world, BlockPos pos) {
        super(ScreenHandlerType.GENERIC_9X3, syncId); // 型は簡易的に Generic を流用
        this.world = world;
        this.pos = pos;
    }

    // GUI 表示用に現在の登録一覧を返す（サーバ側のデータをそのまま渡す）
    public List<Map.Entry<UUID, String>> getEntries() {
        Map<UUID, String> map = ResurrectionData.get(this.world).getAllMobs();
        return new ArrayList<>(map.entrySet());
    }

    // クライアントからの復活要求をサーバ側で処理するメソッド
    public void attemptResurrect(int index, ServerPlayerEntity player) {
        List<Map.Entry<UUID, String>> entries = getEntries();
        if (index < 0 || index >= entries.size()) return;

        Map.Entry<UUID, String> entry = entries.get(index);
        String typeId = entry.getValue();

        // エメラルドチェック（インベントリから1つ消費）
        if (!player.getInventory().contains(new net.minecraft.item.ItemStack(net.minecraft.item.Items.EMERALD))) {
            player.sendMessage(Text.literal("エメラルドが必要です！"), false);
            return;
        }

        // 消費
        int slot = player.getInventory().getSlotWithStack(new net.minecraft.item.ItemStack(net.minecraft.item.Items.EMERALD));
        if (slot >= 0) {
            player.getInventory().removeStack(slot, 1);
        }

        EntityType<?> type = Registries.ENTITY_TYPE.get(new Identifier(typeId));
        if (type == null) {
            player.sendMessage(Text.literal("EntityType が見つかりません"), false);
            return;
        }

        Entity entity = type.create(this.world);
        if (entity != null) {
            entity.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0f, 0f);
            this.world.spawnEntity(entity);
            // 必要なら復活後に ResurrectionData から削除する等の処理
            ResurrectionData.get(this.world).remove(entry.getKey());
            player.sendMessage(Text.literal("Mobを蘇生しました！"), false);
        } else {
            player.sendMessage(Text.literal("蘇生に失敗しました"), false);
        }
    }

    @Override
    public boolean canUse(net.minecraft.entity.player.PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        // 最低限の stub 実装（必要に応じてスロット移動を実装）
        return ItemStack.EMPTY;
    }
}