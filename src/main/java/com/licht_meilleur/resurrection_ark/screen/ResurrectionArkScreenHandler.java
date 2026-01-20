package com.licht_meilleur.resurrection_ark.screen;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResurrectionArkScreenHandler extends ScreenHandler {

    public ResurrectionArkScreenHandler(int syncId, PlayerInventory inv) {
        super(ModScreenHandlers.RESURRECTION_ARK, syncId);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    // ====== GUI用（ダミー） ======
    public static class MobEntry {
        public final String name;
        public final String typeId; // "minecraft:wolf" みたいな文字列

        public MobEntry(String name, String typeId) {
            this.name = name;
            this.typeId = typeId;
        }

        public LivingEntity getEntity(World world) {
            // まずはダミー：不正なら null
            try {
                var type = Registries.ENTITY_TYPE.get(new Identifier(typeId));
                var e = type.create(world);
                return (e instanceof LivingEntity le) ? le : null;
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    public List<MobEntry> getMobList() {
        // TODO: 後で ResurrectionData から読んで同期
        return Collections.emptyList();
    }

    public void resurrectMob(int index) {
        // TODO: ここは「クライアント → サーバー」にパケットで送る必要あり
        // いまはダミー
    }
}