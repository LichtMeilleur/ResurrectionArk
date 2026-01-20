package com.licht_meilleur.resurrection_ark.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;

import java.util.List;

public class ResurrectionArkScreenHandler extends ScreenHandler {

    // ★これが「ScreenHandlerType<>(ResurrectionArkScreenHandler::new)」と一致する形
    public ResurrectionArkScreenHandler(int syncId, PlayerInventory inventory) {
        super(ModScreenHandlers.RESURRECTION_ARK, syncId);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    // ---- ダミー（GUIが呼ぶので、とりあえず用意）----
    public void resurrectMob(int index) {
        // TODO: 後で packet を送る
    }

    public List<MobEntry> getMobList() {
        return List.of(); // とりあえず空
    }

    public static class MobEntry {
        public final String name;
        public MobEntry(String name) { this.name = name; }
    }
}