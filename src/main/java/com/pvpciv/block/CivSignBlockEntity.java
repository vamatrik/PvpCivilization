package com.pvpciv.block;

import com.pvpciv.registry.ModV11;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Текст стилизованной таблички: 4 строки и цвет. Игрок не редактирует. */
public class CivSignBlockEntity extends BlockEntity {
    public final String[] lines = {"", "", "", ""};
    public int color = 0xFFFFFF;

    public CivSignBlockEntity(BlockPos pos, BlockState st) { super(ModV11.SIGN_BE.get(), pos, st); }

    public void set(int color, String... ls) {
        this.color = color;
        for (int i = 0; i < 4; i++) lines[i] = i < ls.length && ls[i] != null ? ls[i] : "";
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    public void line(int i, String s) { lines[i] = s == null ? "" : s; setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }

    @Override protected void saveAdditional(CompoundTag t, HolderLookup.Provider r) {
        super.saveAdditional(t, r);
        for (int i = 0; i < 4; i++) t.putString("l" + i, lines[i]);
        t.putInt("c", color);
    }
    @Override protected void loadAdditional(CompoundTag t, HolderLookup.Provider r) {
        super.loadAdditional(t, r);
        for (int i = 0; i < 4; i++) lines[i] = t.getString("l" + i);
        color = t.contains("c") ? t.getInt("c") : 0xFFFFFF;
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { CompoundTag t = new CompoundTag(); saveAdditional(t, r); return t; }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
