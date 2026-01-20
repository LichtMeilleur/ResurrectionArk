package com.licht_meilleur.resurrection_ark.event;

import com.licht_meilleur.resurrection_ark.block.entity.ResurrectionArkBlockEntity;
import com.licht_meilleur.resurrection_ark.data.ResurrectionData;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public class MobRegisterEvent {

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {

            if (world.isClient) {
                return ActionResult.PASS;
            }

            if (!(world instanceof ServerWorld serverWorld)) {
                return ActionResult.PASS;
            }

            // 直近で開いた Ark を探す（簡易版）
            var be = serverWorld.getBlockEntity(player.getBlockPos());
            if (!(be instanceof ResurrectionArkBlockEntity arkBe)) {
                return ActionResult.PASS;
            }

            if (!arkBe.isRegistering()) {
                return ActionResult.PASS;
            }

            // Mob 登録
            ResurrectionData data = ResurrectionData.get(serverWorld);
            String typeId = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
            data.registerMob(entity.getUuid(), typeId);

            arkBe.setRegistering(false);
            player.sendMessage(net.minecraft.text.Text.literal("Mob を登録しました"), false);

            return ActionResult.SUCCESS;
        });
    }
}