package com.pvpciv.block;

import com.pvpciv.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

/** Пьедестал/алтарь инфузии: один предмет, парит над блоком; забрать может только тот, кто положил. */
public class PedestalBlockEntity extends BlockEntity {
    private ItemStack item = ItemStack.EMPTY;
    @Nullable private UUID owner;
    private String ownerName = "";

    public PedestalBlockEntity(BlockPos pos, BlockState st) { super(ModRegistry.PEDESTAL_BE.get(), pos, st); }

    public ItemStack item() { return item; }
    @Nullable public UUID owner() { return owner; }
    public String ownerName() { return ownerName; }

    public void set(ItemStack st, @Nullable UUID who, String name) {
        item = st;
        owner = st.isEmpty() ? null : who;
        ownerName = st.isEmpty() ? "" : name;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag t, HolderLookup.Provider reg) {
        super.saveAdditional(t, reg);
        if (!item.isEmpty()) t.put("item", item.save(reg));
        if (owner != null) t.putUUID("owner", owner);
        t.putString("ownerName", ownerName);
    }

    @Override
    protected void loadAdditional(CompoundTag t, HolderLookup.Provider reg) {
        super.loadAdditional(t, reg);
        item = t.contains("item") ? ItemStack.parseOptional(reg, t.getCompound("item")) : ItemStack.EMPTY;
        owner = t.hasUUID("owner") ? t.getUUID("owner") : null;
        ownerName = t.getString("ownerName");
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider reg) { CompoundTag t = new CompoundTag(); saveAdditional(t, reg); return t; }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
