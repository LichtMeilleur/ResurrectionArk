package com.licht_meilleur.resurrection_ark.screen;

import com.licht_meilleur.resurrection_ark.block.entity.ResurrectionArkBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public class ResurrectionArkScreenHandler extends ScreenHandler {

    private final BlockPos arkPos;

    // サーバ側：BlockEntity#createMenu から呼ばれる（arkPosはBlockEntityのpos）
    public ResurrectionArkScreenHandler(int syncId, PlayerInventory inventory, BlockPos arkPos) {
        super(ModScreenHandlers.RESURRECTION_ARK, syncId);
        this.arkPos = arkPos;

        // ★GUIを開いた瞬間に「死亡状態」を更新
        // （サーバーでのみ意味がある）
        if (inventory.player instanceof ServerPlayerEntity sp) {
            updateDeadFlags(sp);
        }
    }

    // クライアント側：ExtendedScreenHandlerType から呼ばれる（bufからposを受け取る）
    public ResurrectionArkScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId, inventory, buf.readBlockPos());

        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            UUID uuid = buf.readUuid();
            var typeId = buf.readIdentifier();
            String name = buf.readString();
            float maxHp = buf.readFloat();
            float curHp = buf.readFloat();
            boolean isDead = buf.readBoolean();
            var nbt = buf.readNbt();

            snapshots.add(new MobSnapshot(uuid, typeId, name, maxHp, curHp, isDead, nbt));
        }
    }

    public BlockPos getArkPos() {
        return arkPos;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        // 距離チェック（適当でOK。近くでしか開けないように）
        Vec3d center = Vec3d.ofCenter(arkPos);
        return player.squaredDistanceTo(center) <= 64.0; // 8ブロック以内
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    // =========================
    // ★ 死亡状態更新（開いたとき）
    // =========================
    private void updateDeadFlags(ServerPlayerEntity player) {
        if (!(player.getWorld() instanceof ServerWorld sw)) return;
        if (!(sw.getBlockEntity(arkPos) instanceof ResurrectionArkBlockEntity be)) return;

        boolean changed = false;

        for (ResurrectionArkBlockEntity.StoredMob mob : be.getStoredMobs()) {
            UUID id = mob.uuid;
            if (id == null) continue;

            Entity e = sw.getEntity(id);
            boolean aliveNow = (e instanceof LivingEntity le) && le.isAlive();

            boolean newDead = !aliveNow;
            if (mob.isDead != newDead) {
                mob.isDead = newDead;
                changed = true;
            }

            // 生きているならHPを最新化（表示用）
            if (aliveNow) {
                LivingEntity le = (LivingEntity) e;
                float newMax = le.getMaxHealth();
                float newCur = le.getHealth();

                if (mob.maxHp != newMax) { mob.maxHp = newMax; changed = true; }
                if (mob.currentHp != newCur) { mob.currentHp = newCur; changed = true; }
            }
        }

        // 変更があったときだけ同期
        if (changed) {
            be.markDirty();
            // BlockEntity側で markDirtyAndSync() を public にしているならそれを呼ぶのが理想
            // ここでは最低限 markDirty() でOK（クライアント反映が遅いなら公開syncに切り替え）
        }
    }

    // =========================
    // ★ 蘇生処理（UUID指定）
    // 画面の「蘇生ボタン」から送られてきたUUIDを受け取って、BEに委譲する
    // =========================
    public void attemptResurrect(UUID mobUuid, ServerPlayerEntity player) {
        if (mobUuid == null) return;

        if (!(player.getWorld() instanceof ServerWorld sw)) return;
        if (!(sw.getBlockEntity(arkPos) instanceof ResurrectionArkBlockEntity be)) return;

        // BlockEntity側に実処理がある前提
        be.attemptResurrect(mobUuid, player);
    }
    private final java.util.List<MobSnapshot> snapshots = new java.util.ArrayList<>();

    public static class MobSnapshot {
        public final UUID uuid;
        public final net.minecraft.util.Identifier typeId;
        public final String name;
        public final float maxHp;
        public final float currentHp;
        public final boolean isDead;
        public final net.minecraft.nbt.NbtCompound data;

        public MobSnapshot(UUID uuid, net.minecraft.util.Identifier typeId, String name,
                           float maxHp, float currentHp, boolean isDead,
                           net.minecraft.nbt.NbtCompound data) {
            this.uuid = uuid;
            this.typeId = typeId;
            this.name = name;
            this.maxHp = maxHp;
            this.currentHp = currentHp;
            this.isDead = isDead;
            this.data = data;
        }
    }

    public java.util.List<MobSnapshot> getSnapshots() {
        return snapshots;
    }
}