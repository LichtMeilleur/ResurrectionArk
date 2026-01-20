package com.licht_meilleur.resurrection_ark.screen;

import com.licht_meilleur.resurrection_ark.ResurrectionArkMod;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import com.licht_meilleur.resurrection_ark.network.ResurrectionArkServerPackets;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.gui.screen.ConfirmScreen;

import java.util.UUID;

public class ResurrectionArkScreen extends HandledScreen<ResurrectionArkScreenHandler> {

    private CardListWidget cardList;

    private void requestDelete(UUID uuid) {
        if (this.client == null) return;
        if (uuid == null) {
            System.out.println("[ResurrectionArk] requestDelete called with null uuid");
            return;
        }

        UUID target = uuid;

        this.client.setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        sendDeletePacket(target);
                        // ★ここは安定優先で閉じる（成功/失敗はチャットに出る）
                        this.close();
                        return;
                    }
                    this.client.setScreen(this);
                },
                Text.literal("登録削除の確認"),
                Text.literal("このMob登録を削除しますか？")
        ));
    }

    private void sendDeletePacket(UUID uuid) {
        if (uuid == null) {
            ResurrectionArkMod.LOGGER.error("sendDeletePacket called with null uuid");
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(handler.getArkPos());
        buf.writeUuid(uuid);
        ClientPlayNetworking.send(ResurrectionArkServerPackets.DELETE_MOB, buf);
    }

    public ResurrectionArkScreen(ResurrectionArkScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 255;
        this.backgroundHeight = 156;
    }

    @Override
    protected void init() {
        super.init();

        int left = this.x;
        int top  = this.y;

        // “カードを縦に並べてスクロール”領域（ひとまず画面全体）
        int listX = left;
        int listY = top;
        int listW = 255;
        int listH = 156 + 80; // 少しはみ出してスクロール確認しやすく

        cardList = new CardListWidget(listX, listY, listW, listH, this::requestDelete);

        // ★ここは「今Arkに登録されているMob一覧」を入れる場所
        // いまはダミーでOK（表示確認用）
        // cardList.addCard(...); はあなたの現状に合わせて残してOK
        // ★開いているArkのBlockEntityから登録Mobを読み取ってカードにする
        if (this.client != null && this.client.world != null) {
            var be = this.client.world.getBlockEntity(handler.getArkPos());
            if (be instanceof com.licht_meilleur.resurrection_ark.block.entity.ResurrectionArkBlockEntity arkBe) {

                for (com.licht_meilleur.resurrection_ark.block.entity.ResurrectionArkBlockEntity.StoredMob mob
                        : arkBe.getStoredMobs()) {

                    net.minecraft.util.Identifier id = mob.typeId;
                    net.minecraft.entity.EntityType<?> type = net.minecraft.registry.Registries.ENTITY_TYPE.get(id);

                    var created = type.create(this.client.world);
                    if (created instanceof net.minecraft.entity.LivingEntity) {
                        @SuppressWarnings("unchecked")
                        net.minecraft.entity.EntityType<? extends net.minecraft.entity.LivingEntity> livingType =
                                (net.minecraft.entity.EntityType<? extends net.minecraft.entity.LivingEntity>) type;

                        cardList.addCard(new CardListWidget.CardData(
                                mob.uuid,
                                mob.name,
                                livingType,
                                32,
                                28
                        ));
                    }
                }
            }
        }

        this.addDrawableChild(cardList);
        this.addSelectableChild(cardList);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // Screen側は何も描かない（カード側が背景を描く）
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (cardList != null && cardList.isMouseOver(mouseX, mouseY)) {
            return cardList.mouseScrolled(mouseX, mouseY, amount);
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // タイトル不要
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}