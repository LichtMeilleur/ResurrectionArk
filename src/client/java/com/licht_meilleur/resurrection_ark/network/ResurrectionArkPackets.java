package com.licht_meilleur.resurrection_ark.network;

import com.licht_meilleur.resurrection_ark.screen.ResurrectionArkScreenHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.server.MinecraftServer;

public class ResurrectionArkPackets {
    public static final Identifier RESURRECT = new Identifier("resurrection_ark", "resurrect");

    // クライアント側呼び出し用
    public static void sendResurrectRequest(int index) {
        ClientPlayNetworking.send(RESURRECT, buf -> buf.writeInt(index));
    }

    // サーバ側受信登録（`ResurrectionArkMod` の初期化で呼ぶ）
    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(RESURRECT, (server, player, handler, buf, responseSender) -> {
            int idx = buf.readInt();
            server.execute(() -> {
                // プレイヤーの開いているスクリーンハンドラが ResurrectionArkScreenHandler なら処理
                if (player.currentScreenHandler instanceof ResurrectionArkScreenHandler handlerInstance) {
                    handlerInstance.attemptResurrect(idx, player);
                }
            });
        });
    }
}