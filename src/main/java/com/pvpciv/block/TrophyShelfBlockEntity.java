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

/** Ячейка витрины: какой трофей сюда (бит StoryData.trophies) и выставлен ли он. */
public class TrophyShelfBlockEntity extends BlockEntity {
    public int bit;
    public boolean shown;

    public TrophyShelfBlockEntity(BlockPos pos, BlockState st) { super(ModV11.TROPHY_SHELF_BE.get(), pos, st); }

    public void set(int bit, boolean shown) {
        if (this.bit == bit && this.shown == shown) return;
        this.bit = bit; this.shown = shown;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override protected void saveAdditional(CompoundTag t, HolderLookup.Provider r) { super.saveAdditional(t, r); t.putInt("bit", bit); t.putBoolean("shown", shown); }
    @Override protected void loadAdditional(CompoundTag t, HolderLookup.Provider r) { super.loadAdditional(t, r); bit = t.getInt("bit"); shown = t.getBoolean("shown"); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { CompoundTag t = new CompoundTag(); saveAdditional(t, r); return t; }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
