package com.licht_meilleur.resurrection_ark.block.entity;

import com.licht_meilleur.resurrection_ark.screen.ResurrectionArkScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ResurrectionArkBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory {

    public ResurrectionArkBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESURRECTION_ARK_ENTITY_TYPE, pos, state);
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("Resurrection Ark");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inventory, PlayerEntity player) {
        // refreshAliveStatesOnServer(); ←消す
        return new ResurrectionArkScreenHandler(syncId, inventory, this.pos);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    // =========================
    // 保存するMob情報（★正規化）
    // =========================
    public static class StoredMob {
        public UUID uuid;
        public Identifier typeId;
        public String name;
        public UUID ownerUuid;

        public float maxHp;
        public float currentHp;
        public boolean isDead;

        public NbtCompound data; // 見た目・個体差（NBT）

        public StoredMob(UUID uuid,
                         Identifier typeId,
                         String name,
                         UUID ownerUuid,
                         float maxHp,
                         float currentHp,
                         boolean isDead,
                         NbtCompound data) {
            this.uuid = uuid;
            this.typeId = typeId;
            this.name = name;
            this.ownerUuid = ownerUuid;
            this.maxHp = maxHp;
            this.currentHp = currentHp;
            this.isDead = isDead;
            this.data = data;
        }
    }

    private final List<StoredMob> storedMobs = new ArrayList<>();
    private static final int MAX_MOBS = 64;

    public List<StoredMob> getStoredMobs() {
        return storedMobs;
    }

    // =========================
    // 登録：所有Mobだけにする版（必要なら）
    // =========================
    public boolean addMobFromEntity(LivingEntity entity, PlayerEntity player) {
        if (this.world == null || this.world.isClient) return false;

        if (storedMobs.size() >= MAX_MOBS) {
            player.sendMessage(Text.literal("登録上限に達しました。"), false);
            return false;
        }

        // ★所有チェック（必要ならON）
        // 1) Tameable（オオカミ/ネコなど）
        if (entity instanceof TameableEntity tame) {
            UUID ou = tame.getOwnerUuid();
            if (ou == null || !ou.equals(player.getUuid())) {
                player.sendMessage(Text.literal("所有Mobのみ登録できます。"), false);
                return false;
            }
        }
        // 2) 馬系（所有者が付く）
        else if (entity instanceof AbstractHorseEntity horse) {
            UUID ou = horse.getOwnerUuid();
            if (ou == null || !ou.equals(player.getUuid())) {
                player.sendMessage(Text.literal("所有Mobのみ登録できます。"), false);
                return false;
            }
        }
        // 3) それ以外は今は弾く（味方判定を広げたいならここを拡張）
        else {
            player.sendMessage(Text.literal("所有Mobのみ登録できます。"), false);
            return false;
        }

        UUID mobUuid = entity.getUuid();

        // 重複（UUID）防止
        for (StoredMob m : storedMobs) {
            if (m.uuid != null && m.uuid.equals(mobUuid)) {
                player.sendMessage(Text.literal("このMobはすでに登録済みです。"), false);
                return false;
            }
        }

        Identifier typeId = Registries.ENTITY_TYPE.getId(entity.getType());
        if (typeId == null) return false;

        String displayName = entity.getName().getString();

        // ★NBT保存（見た目/個体差）
        NbtCompound nbt = new NbtCompound();
        entity.writeNbt(nbt);

        // 位置・速度などは消す（蘇生位置はArk側で決める）
        nbt.remove("Pos");
        nbt.remove("Motion");
        nbt.remove("Rotation");
        nbt.remove("FallDistance");
        nbt.remove("Fire");
        nbt.remove("Air");
        nbt.remove("OnGround");
        nbt.remove("PortalCooldown");
        nbt.remove("Passengers");
        nbt.remove("Leash");

        float maxHp = entity.getMaxHealth();
        float curHp = entity.getHealth();
        boolean isDead = false;

        storedMobs.add(new StoredMob(
                mobUuid,
                typeId,
                displayName,
                player.getUuid(),
                maxHp,
                curHp,
                isDead,
                nbt
        ));

        markDirtyAndSync();
        return true;
    }

    // =========================
    // ★ロード済み個体から status 更新
    // =========================
    private void refreshStatusesFromWorld(ServerWorld sw) {
        for (StoredMob mob : storedMobs) {
            if (mob.uuid == null) continue;

            Entity e = sw.getEntity(mob.uuid);
            if (e instanceof LivingEntity le) {
                mob.maxHp = le.getMaxHealth();
                mob.currentHp = le.getHealth();
                mob.isDead = !le.isAlive();
            }
            // ※ロードされてない場合は更新できないので「保存値のまま」
        }
    }

    // =========================
    // 削除（UUID）
    // =========================
    public boolean removeMob(UUID uuid) {
        if (uuid == null) return false;

        for (int i = 0; i < storedMobs.size(); i++) {
            StoredMob m = storedMobs.get(i);
            if (m.uuid != null && m.uuid.equals(uuid)) {
                storedMobs.remove(i);
                markDirtyAndSync();
                return true;
            }
        }
        return false;
    }

    // =========================
    // NBT 保存/読込（★data と hp/Dead も保存）
    // =========================
    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);

        NbtList list = new NbtList();
        for (StoredMob mob : storedMobs) {
            NbtCompound e = new NbtCompound();

            if (mob.uuid != null) e.putUuid("MobUUID", mob.uuid);
            if (mob.ownerUuid != null) e.putUuid("OwnerUUID", mob.ownerUuid);

            e.putString("Type", mob.typeId.toString());
            e.putString("Name", mob.name);

            e.putFloat("MaxHp", mob.maxHp);
            e.putFloat("CurHp", mob.currentHp);
            e.putBoolean("Dead", mob.isDead);

            if (mob.data != null) e.put("Data", mob.data);

            list.add(e);
        }

        nbt.put("StoredMobs", list);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        storedMobs.clear();

        NbtList list = nbt.getList("StoredMobs", 10);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound e = list.getCompound(i);

            Identifier typeId = new Identifier(e.getString("Type"));
            String name = e.getString("Name");

            UUID mobUuid = e.containsUuid("MobUUID") ? e.getUuid("MobUUID") : null;
            UUID ownerUuid = e.containsUuid("OwnerUUID") ? e.getUuid("OwnerUUID") : null;

            float maxHp = e.contains("MaxHp") ? e.getFloat("MaxHp") : 0f;
            float curHp = e.contains("CurHp") ? e.getFloat("CurHp") : 0f;
            boolean dead = e.contains("Dead") && e.getBoolean("Dead");

            NbtCompound data = e.contains("Data") ? e.getCompound("Data") : null;

            storedMobs.add(new StoredMob(mobUuid, typeId, name, ownerUuid, maxHp, curHp, dead, data));
        }
    }

    // =========================
    // クライアント同期用
    // =========================
    private void markDirtyAndSync() {
        this.markDirty();
        if (this.world != null && !this.world.isClient) {
            BlockState state = this.getCachedState();
            this.world.updateListeners(this.pos, state, state, 3);
        }
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        NbtCompound n = new NbtCompound();
        this.writeNbt(n);
        return n;
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
    // =========================
// 蘇生（UUID指定）
// =========================
    public void attemptResurrect(UUID mobUuid, ServerPlayerEntity player) {
        if (this.world == null || this.world.isClient) return;
        if (mobUuid == null) return;

        StoredMob mob = null;
        for (StoredMob m : storedMobs) {
            if (m.uuid != null && m.uuid.equals(mobUuid)) { mob = m; break; }
        }

        if (mob == null) {
            player.sendMessage(Text.literal("対象の登録が見つかりません。"), false);
            return;
        }

        // 所有者チェック（必要なら）
        if (mob.ownerUuid != null && !mob.ownerUuid.equals(player.getUuid())) {
            player.sendMessage(Text.literal("このMobの主はあなたではありません。"), false);
            return;
        }

        // すでに生存していたら蘇生しない（重複スポーン防止）
        if (this.world instanceof net.minecraft.server.world.ServerWorld sw0) {
            net.minecraft.entity.Entity existing = sw0.getEntity(mobUuid);
            if (existing instanceof LivingEntity le && le.isAlive()) {
                player.sendMessage(Text.literal("このMobはまだ生きています。"), false);
                return;
            }
        }

        // 死亡扱いじゃないなら蘇生しない（UI側でも弾くけどサーバでも安全に）
        if (!mob.isDead) {
            player.sendMessage(Text.literal("このMobは死亡していません。"), false);
            return;
        }

        final int COST = 32;
        if (!consumeEmerald(player, COST)) {
            player.sendMessage(Text.literal("エメラルドが足りません (x" + COST + ")"), false);
            return;
        }

        if (!(this.world instanceof net.minecraft.server.world.ServerWorld sw)) return;

        var type = Registries.ENTITY_TYPE.get(mob.typeId);
        var ent = type.create(sw);
        if (!(ent instanceof LivingEntity living)) {
            player.sendMessage(Text.literal("このEntityは蘇生できません。"), false);
            return;
        }

        // ★見た目/個体差を復元（NBT）
        if (mob.data != null) {
            // UUID/Posなどは後で上書きするので気にしなくてOK
            living.readNbt(mob.data.copy());
        }

        // ★所有状態を戻す（今までの実装を継続）
        if (living instanceof net.minecraft.entity.passive.TameableEntity tame) {
            tame.setOwner(player);
            tame.setTamed(true);
        }
        if (living instanceof net.minecraft.entity.passive.AbstractHorseEntity horse) {
            horse.setOwnerUuid(player.getUuid());
        }

        // Arkの前にスポーン
        living.refreshPositionAndAngles(
                this.pos.getX() + 0.5,
                this.pos.getY() + 1.0,
                this.pos.getZ() + 0.5,
                0f, 0f
        );

        sw.spawnEntity(living);

        // 登録情報を更新（新しい個体UUID / 生存化 / HP）
        mob.uuid = living.getUuid();
        mob.isDead = false;
        mob.maxHp = living.getMaxHealth();
        mob.currentHp = living.getHealth();

        markDirtyAndSync();
        player.sendMessage(Text.literal("蘇生しました。"), false);
    }

    private boolean consumeEmerald(ServerPlayerEntity player, int amount) {
        int remain = amount;
        var inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            var st = inv.getStack(i);
            if (st.isOf(net.minecraft.item.Items.EMERALD)) {
                int take = Math.min(remain, st.getCount());
                st.decrement(take);
                remain -= take;
                if (remain <= 0) return true;
            }
        }
        return false;
    }
    public void refreshAliveStatesOnServer() {
        if (this.world == null || this.world.isClient) return;
        if (!(this.world instanceof net.minecraft.server.world.ServerWorld sw)) return;

        boolean changed = false;

        for (StoredMob mob : storedMobs) {
            if (mob.uuid == null) continue;

            var e = sw.getEntity(mob.uuid);

            if (e instanceof LivingEntity le) {
                boolean deadNow = !le.isAlive();
                float max = le.getMaxHealth();
                float cur = le.getHealth();

                if (mob.isDead != deadNow) { mob.isDead = deadNow; changed = true; }
                if (Math.abs(mob.maxHp - max) > 0.001f) { mob.maxHp = max; changed = true; }
                if (Math.abs(mob.currentHp - cur) > 0.001f) { mob.currentHp = cur; changed = true; }

            } else {
                // エンティティが見つからない＝基本「死亡扱い」にする
                if (!mob.isDead) { mob.isDead = true; changed = true; }
                if (mob.currentHp != 0f) { mob.currentHp = 0f; changed = true; }
            }
        }

        if (changed) markDirtyAndSync();
    }
}