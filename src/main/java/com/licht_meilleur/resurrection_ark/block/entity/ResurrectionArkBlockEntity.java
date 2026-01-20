package com.licht_meilleur.resurrection_ark.block.entity;

import com.licht_meilleur.resurrection_ark.screen.ResurrectionArkScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
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

    // =========================
    // GUI タイトル
    // =========================
    @Override
    public Text getDisplayName() {
        return Text.literal("Resurrection Ark");
    }

    // =========================
    // ScreenHandler 生成
    // =========================
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inventory, PlayerEntity player) {
        return new ResurrectionArkScreenHandler(syncId, inventory, this.pos);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    // =========================
    // 保存するMob情報
    // =========================
    public static class StoredMob {
        public UUID uuid;            // MobのUUID（削除/重複判定に使う）
        public Identifier typeId;    // EntityTypeのID
        public String name;          // 表示名
        public UUID ownerUuid;       // 所有者（プレイヤーUUID）

        public StoredMob(UUID uuid, Identifier typeId, String name, UUID ownerUuid) {
            this.uuid = uuid;
            this.typeId = typeId;
            this.name = name;
            this.ownerUuid = ownerUuid;
        }
    }

    private final List<StoredMob> storedMobs = new ArrayList<>();
    private static final int MAX_MOBS = 64;

    // GUIで読む用（そのまま返す。UI側で変更しないこと）
    public List<StoredMob> getStoredMobs() {
        return storedMobs;
    }

    // =========================
    // 登録：しゃがみ+右クリ等から呼ぶ想定
    // =========================
    public boolean addMobFromEntity(LivingEntity entity, PlayerEntity player) {
        if (this.world == null || this.world.isClient) return false;
        if (storedMobs.size() >= MAX_MOBS) {
            player.sendMessage(Text.literal("登録上限に達しました。"), false);
            return false;
        }

        UUID mobUuid = entity.getUuid();

        // すでに同じUUIDが登録済みなら弾く
        for (StoredMob m : storedMobs) {
            if (m.uuid != null && m.uuid.equals(mobUuid)) {
                player.sendMessage(Text.literal("このMobはすでに登録済みです。"), false);
                return false;
            }
        }

        Identifier id = Registries.ENTITY_TYPE.getId(entity.getType());
        if (id == null) return false;

        String displayName = entity.getName().getString();
        UUID ownerUuid = player.getUuid();

        storedMobs.add(new StoredMob(mobUuid, id, displayName, ownerUuid));

        markDirtyAndSync();
        return true;
    }

    // =========================
    // 削除：UUIDで安定削除
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
    // NBT 保存/読込
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

            list.add(e);
        }

        nbt.put("StoredMobs", list);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        storedMobs.clear();

        NbtList list = nbt.getList("StoredMobs", 10); // 10=Compound
        for (int i = 0; i < list.size(); i++) {
            NbtCompound e = list.getCompound(i);

            Identifier typeId = new Identifier(e.getString("Type"));
            String name = e.getString("Name");

            UUID mobUuid = e.containsUuid("MobUUID") ? e.getUuid("MobUUID") : null;
            UUID ownerUuid = e.containsUuid("OwnerUUID") ? e.getUuid("OwnerUUID") : null;

            storedMobs.add(new StoredMob(mobUuid, typeId, name, ownerUuid));
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
        NbtCompound nbt = new NbtCompound();
        this.writeNbt(nbt);
        return nbt;
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}