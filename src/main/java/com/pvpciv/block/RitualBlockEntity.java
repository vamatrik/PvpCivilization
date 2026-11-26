package com.pvpciv.block;

import com.pvpciv.registry.ModV11;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/** Ритуальный зал: у ядра — оружие и владелец, у инжектора — заряжен ли. anim > 0 — идёт ритуал (тики). */
public class RitualBlockEntity extends BlockEntity {
    public ItemStack item = ItemStack.EMPTY;
    public UUID owner;
    public String ownerName = "";
    public boolean lit;
    public int anim;
    /** 1.4: итог ритуала лежит на ядре: тиков до авто-отправки в сумку владельца. */
    public int wait;
    /** Клиент: значения на момент прихода пакета (для плавной анимации между синхронизациями). */
    public int animBase, waitBase; public long recvAt = -1;

    public RitualBlockEntity(BlockPos pos, BlockState st) { super(ModV11.RITUAL_BE.get(), pos, st); }

    public void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override protected void saveAdditional(CompoundTag t, HolderLookup.Provider r) {
        super.saveAdditional(t, r);
        if (!item.isEmpty()) t.put("item", item.save(r));
        if (owner != null) t.putUUID("owner", owner);
        t.putString("ownerName", ownerName);
        t.putBoolean("lit", lit);
        t.putInt("anim", anim);
        t.putInt("wait", wait);
    }
    @Override protected void loadAdditional(CompoundTag t, HolderLookup.Provider r) {
        super.loadAdditional(t, r);
        item = t.contains("item") ? ItemStack.parseOptional(r, t.getCompound("item")) : ItemStack.EMPTY;
        owner = t.hasUUID("owner") ? t.getUUID("owner") : null;
        ownerName = t.getString("ownerName");
        lit = t.getBoolean("lit");
        anim = t.getInt("anim");
        wait = t.getInt("wait");
        if (level != null && level.isClientSide) { animBase = anim; waitBase = wait; recvAt = level.getGameTime(); }
    }
    /** Клиент: текущий тик анимации с экстраполяцией. */
    public float animNow(float pt) { return anim <= 0 ? 0 : level == null || recvAt < 0 ? anim : animBase + (level.getGameTime() - recvAt) + pt; }
    public float waitNow(float pt) { return wait <= 0 ? 0 : level == null || recvAt < 0 ? wait : Math.max(0, waitBase - (level.getGameTime() - recvAt) - pt); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { CompoundTag t = new CompoundTag(); saveAdditional(t, r); return t; }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
