package com.pvpciv.block;

import com.pvpciv.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Витрина трофеев: какие трофеи выставлены (биты как в StoryData.trophies). */
public class ShowcaseBlockEntity extends BlockEntity {
    public int bits;

    public ShowcaseBlockEntity(BlockPos pos, BlockState st) { super(ModRegistry.SHOWCASE_BE.get(), pos, st); }

    /** Трофеи по битам: 1 Громила, 2 Хранительница, 4 Дон, 8 Крысиный король. */
    public static Item trophy(int i) {
        return switch (i) {
            case 0 -> ModRegistry.BRUTE_TROPHY.get(); case 1 -> ModRegistry.KEEPER_TROPHY.get();
            case 2 -> ModRegistry.DON_TROPHY.get(); case 3 -> ModRegistry.RAT_KING_TROPHY.get(); default -> null;
        };
    }
    public static int bitOf(Item it) { for (int i = 0; i < 4; i++) if (trophy(i) == it) return 1 << i; return 0; }

    public void add(int bit) {
        bits |= bit;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override protected void saveAdditional(CompoundTag t, HolderLookup.Provider r) { super.saveAdditional(t, r); t.putInt("bits", bits); }
    @Override protected void loadAdditional(CompoundTag t, HolderLookup.Provider r) { super.loadAdditional(t, r); bits = t.getInt("bits"); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { CompoundTag t = new CompoundTag(); saveAdditional(t, r); return t; }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
