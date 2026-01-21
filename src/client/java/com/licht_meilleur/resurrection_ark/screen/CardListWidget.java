package com.licht_meilleur.resurrection_ark.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class CardListWidget implements Drawable, Element, Selectable {

    public static final int CARD_W = 255;
    public static final int CARD_H = 156;

    private static final Identifier CARD_TEX =
            new Identifier("resurrection_ark", "textures/gui/resurrection_ark_gui.png");

    // ★ボタン画像は「クラスのフィールド」として宣言（render内に書かない）
    private static final Identifier BTN_TEX =
            new Identifier("resurrection_ark", "textures/gui/resurrection_ark_gui_button.png");
    private static final Identifier BTN_DOWN_TEX =
            new Identifier("resurrection_ark", "textures/gui/resurrection_ark_gui_buttondown.png");

    private final int x, y, width, height;
    private final int gap = 8;

    private final List<CardData> cards = new ArrayList<>();
    private int scrollY = 0;

    private boolean focused = false;

    private final Consumer<UUID> onDeleteRequested;
    private final Consumer<UUID> onResurrectRequested;

    // カード内の座標（名刺画像基準）
    private static final int PREVIEW_X = 18;
    private static final int PREVIEW_Y = 26;
    private static final int PREVIEW_W = 72;
    private static final int PREVIEW_H = 72;

    // 名前
    private static final int NAME_X = 150;
    private static final int NAME_Y = 12; // 4 → 12

    // ステータス
    private static final int STAT_X = PREVIEW_X + PREVIEW_W + 28; // +16
    private static final int STAT_Y = PREVIEW_Y + 28;            // +22
    private static final int STAT_LINE = 10;

    // コスト表示位置
    private static final int COST_ICON_X = 118;
    private static final int COST_ICON_Y = 110;
    private static final int COST_TEXT_X = 138;
    private static final int COST_TEXT_Y = 114;

    // 蘇生ボタン当たり判定
    private static final int RES_BTN_X = 200;
    private static final int RES_BTN_Y = 120;
    private static final int RES_BTN_W = 40;
    private static final int RES_BTN_H = 20;

    // 削除ボタン（右上）
    private static final int DEL_X = 235;
    private static final int DEL_Y = 8;
    private static final int DEL_W = 16;
    private static final int DEL_H = 16;

    // =====================
    // データ
    // =====================
    public static class CardData {
        public final UUID uuid;
        public final String name;
        public final EntityType<? extends LivingEntity> type;
        public final int cost;
        public final int scale;
        public final NbtCompound nbt; // 見た目反映用
        public final float maxHp;
        public final float currentHp; // ★finalにして確実に初期化
        public final boolean isDead;

        private LivingEntity cached;

        public CardData(UUID uuid, String name, EntityType<? extends LivingEntity> type,
                        int cost, int scale, NbtCompound nbt,
                        float maxHp, float currentHp, boolean isDead) {
            this.uuid = uuid;
            this.name = name;
            this.type = type;
            this.cost = cost;
            this.scale = scale;
            this.nbt = nbt;
            this.maxHp = maxHp;
            this.currentHp = currentHp;
            this.isDead = isDead;
        }

        public LivingEntity getOrCreateEntity(MinecraftClient client) {
            if (cached != null) return cached;
            if (client.world == null) return null;

            LivingEntity e = type.create(client.world);
            if (e == null) return null;

            if (nbt != null) {
                e.readNbt(nbt.copy());
            }

            cached = e;
            return cached;
        }
    }

    public CardListWidget(int x, int y, int width, int height,
                          Consumer<UUID> onResurrectRequested,
                          Consumer<UUID> onDeleteRequested) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.onResurrectRequested = onResurrectRequested;
        this.onDeleteRequested = onDeleteRequested;
    }

    public void addCard(CardData data) {
        cards.add(data);
    }

    public void clearCache() {
        for (CardData cd : cards) cd.cached = null;
    }

    public void removeCard(int index) {
        if (index >= 0 && index < cards.size()) {
            cards.remove(index);
        }
        scrollY = MathHelper.clamp(scrollY, 0, maxScroll());
    }

    private int contentHeight() {
        if (cards.isEmpty()) return 0;
        return cards.size() * (CARD_H + gap) - gap;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - height);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.enableScissor(x, y, x + width, y + height);

        int startY = y - scrollY;
        var tr = MinecraftClient.getInstance().textRenderer;

        for (int i = 0; i < cards.size(); i++) {
            int cardY = startY + i * (CARD_H + gap);
            if (cardY + CARD_H < y) continue;
            if (cardY > y + height) break;

            CardData cd = cards.get(i);

            // 1) 背景
            RenderSystem.setShaderTexture(0, CARD_TEX);
            context.drawTexture(CARD_TEX, x, cardY, 0, 0, CARD_W, CARD_H, 255, 156);

            if (cd.isDead) {
                context.fill(x, cardY, x + CARD_W, cardY + CARD_H, 0x88000000);
            }

            // 2) 名前
            context.drawText(tr, cd.name, x + NAME_X, cardY + NAME_Y, 0xFFFFFF, false);

            // 2.5) 削除ボタン（X）
            context.drawText(tr, "X", x + DEL_X + 5, cardY + DEL_Y + 4, 0xFF5555, false);

            // 3) ステータス表示（保存値を使う）
            float maxHp = cd.maxHp;
            float curHp = cd.currentHp;

            // 念のため fallback（保存が0の時だけ）
            LivingEntity preview = cd.getOrCreateEntity(MinecraftClient.getInstance());
            if ((maxHp <= 0.01f) && preview != null) {
                maxHp = preview.getMaxHealth();
                curHp = preview.getHealth();
            }

            context.drawText(tr,
                    Text.translatable("gui.resurrection_ark.max_hp", String.format("%.1f", maxHp)),
                    x + STAT_X, cardY + STAT_Y, 0xFFFFFF, false);

            context.drawText(tr,
                    Text.translatable("gui.resurrection_ark.hp", String.format("%.1f", curHp)),
                    x + STAT_X, cardY + STAT_Y + STAT_LINE, 0xFFFFFF, false);

            int statusColor = cd.isDead ? 0xFFFF5555 : 0xFF55FF55;
            Text statusText = cd.isDead
                    ? Text.translatable("gui.resurrection_ark.status_dead")
                    : Text.translatable("gui.resurrection_ark.status_alive");
            context.drawText(tr, statusText,
                    x + STAT_X, cardY + STAT_Y + STAT_LINE * 2, statusColor, false);

            // 4) コスト
            ItemStack emerald = new ItemStack(Items.EMERALD);
            context.drawItem(emerald, x + COST_ICON_X, cardY + COST_ICON_Y);
            context.drawText(tr, "x" + cd.cost, x + COST_TEXT_X, cardY + COST_TEXT_Y, 0xFFFFFF, false);

            // 5) Mobモデル
            if (preview != null) {
                int centerX = x + PREVIEW_X + PREVIEW_W / 2;
                int baseY = cardY + PREVIEW_Y + PREVIEW_H;
                InventoryScreen.drawEntity(
                        context,
                        centerX,
                        baseY,
                        cd.scale,
                        (float) (mouseX - centerX),
                        (float) (mouseY - baseY),
                        preview
                );
            }

            // 6) 蘇生ボタン（画像＋文字）
            boolean hovered =
                    mouseX >= x + RES_BTN_X && mouseX < x + RES_BTN_X + RES_BTN_W &&
                            mouseY >= cardY + RES_BTN_Y && mouseY < cardY + RES_BTN_Y + RES_BTN_H;

            Identifier btn = hovered ? BTN_DOWN_TEX : BTN_TEX;
            RenderSystem.setShaderTexture(0, btn);
            context.drawTexture(btn,
                    x + RES_BTN_X, cardY + RES_BTN_Y,
                    0, 0,
                    RES_BTN_W, RES_BTN_H,
                    RES_BTN_W, RES_BTN_H);

            Text resurrectText = Text.translatable("gui.resurrection_ark.resurrect");
            context.drawText(tr, resurrectText,
                    x + RES_BTN_X + 6, cardY + RES_BTN_Y + 6,
                    0xFFFFFF, false);
        }

        context.disableScissor();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!isMouseOver(mouseX, mouseY)) return false;

        int max = maxScroll();
        if (max <= 0) return false;

        int delta = (int) Math.signum(amount);
        scrollY = MathHelper.clamp(scrollY - delta * 20, 0, max);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) return false;

        this.setFocused(true);

        int localX = (int) mouseX - x;
        int localY = (int) mouseY - y + scrollY;

        int stride = CARD_H + gap;
        int index = localY / stride;

        if (index < 0 || index >= cards.size()) return false;

        int withinY = localY % stride;
        if (withinY >= CARD_H) return false;

        // 削除ボタン
        if (localX >= DEL_X && localX < DEL_X + DEL_W
                && withinY >= DEL_Y && withinY < DEL_Y + DEL_H) {
            UUID uuid = cards.get(index).uuid;
            if (onDeleteRequested != null) onDeleteRequested.accept(uuid);
            return true;
        }

        // 蘇生ボタン
        if (localX >= RES_BTN_X && localX < RES_BTN_X + RES_BTN_W
                && withinY >= RES_BTN_Y && withinY < RES_BTN_Y + RES_BTN_H) {

            // 生存中なら押しても何もしない（将来メッセージ出してもOK）
            if (!cards.get(index).isDead) return true;

            UUID uuid = cards.get(index).uuid;
            if (onResurrectRequested != null) onResurrectRequested.accept(uuid);
            return true;
        }

        return true;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
        // 今は不要
    }

    @Override
    public SelectionType getType() {
        return this.focused ? SelectionType.FOCUSED : SelectionType.NONE;
    }
}