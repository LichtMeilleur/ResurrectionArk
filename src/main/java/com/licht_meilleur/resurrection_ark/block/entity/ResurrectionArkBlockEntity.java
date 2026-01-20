package com.licht_meilleur.resurrection_ark.block.entity;

import com.licht_meilleur.resurrection_ark.screen.ResurrectionArkScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class ResurrectionArkBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {

    // ★追加：登録モード
    private boolean registering = false;

    public ResurrectionArkBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESURRECTION_ARK_ENTITY_TYPE, pos, state);
    }

    // ★追加：getter/setter
    public boolean isRegistering() {
        return registering;
    }

    public void setRegistering(boolean registering) {
        this.registering = registering;
        markDirty();
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putBoolean("Registering", registering);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        registering = nbt.getBoolean("Registering");
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("Resurrection Ark");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new ResurrectionArkScreenHandler(syncId, inv);
    }
}