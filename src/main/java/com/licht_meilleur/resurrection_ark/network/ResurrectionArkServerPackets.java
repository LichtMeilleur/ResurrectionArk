package com.licht_meilleur.resurrection_ark.network;

import com.licht_meilleur.resurrection_ark.ResurrectionArkMod;
import com.licht_meilleur.resurrection_ark.block.entity.ResurrectionArkBlockEntity;
import com.licht_meilleur.resurrection_ark.screen.ResurrectionArkScreenHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class ResurrectionArkServerPackets {

    public static final Identifier RESURRECT =
            new Identifier(ResurrectionArkMod.MOD_ID, "resurrect");

    public static final Identifier DELETE_MOB =
            new Identifier(ResurrectionArkMod.MOD_ID, "delete_mob");

    public static void register() {

        // 蘇生（デバッグログもここで出す）
        ServerPlayNetworking.registerGlobalReceiver(
                RESURRECT,
                (server, player, handler, buf, responseSender) -> {

                    UUID idx = buf.readUuid();

                    server.execute(() -> {
                        if (player.currentScreenHandler instanceof ResurrectionArkScreenHandler screenHandler) {
                            screenHandler.attemptResurrect(idx, player);
                        }
                    });
                }
        );

        // 削除
        ServerPlayNetworking.registerGlobalReceiver(
                DELETE_MOB,
                (server, player, handler, buf, responseSender) -> {

                    BlockPos pos = buf.readBlockPos();
                    UUID mobUuid = buf.readUuid();

                    // ★ ここで player は使える
                    ResurrectionArkMod.LOGGER.info(
                            "DELETE request by {} playerUuid={} mobUuid={} pos={}",
                            player.getName().getString(),
                            player.getUuid(),
                            mobUuid,
                            pos
                    );

                    server.execute(() -> {
                        if (!(player.getWorld().getBlockEntity(pos)
                                instanceof ResurrectionArkBlockEntity arkBe)) {
                            return;
                        }


                        boolean ok = arkBe.removeMob(mobUuid);

                        if (ok) {
                            player.sendMessage(Text.literal("登録を削除しました。"), false);
                        } else {
                            player.sendMessage(Text.literal("削除に失敗しました。"), false);
                        }
                    });
                }
        );
    }
}