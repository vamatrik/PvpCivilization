package com.pvpciv.game;

import com.pvpciv.item.CivWeaponItem;
import com.pvpciv.loot.Gear;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Заточка оружия звёздами с небольшим «ритуалом» ожидания. */
public final class Forge {
    private static final Set<UUID> BUSY = new HashSet<>();
    public static final java.util.Map<UUID, ItemStack> PENDING = new java.util.HashMap<>();

    private static int findStar(Inventory inv, Item star) {
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(star)) return i;
        return -1;
    }

    public static void tryEnhance(ServerPlayer p, BlockPos forge, boolean blessed) {
        if (BUSY.contains(p.getUUID())) return;
        ItemStack weapon = p.getMainHandItem();
        if (!(weapon.getItem() instanceof CivWeaponItem w)) {
            p.displayClientMessage(Component.literal("Возьми в руку оружие цивилизации.").withStyle(ChatFormatting.GOLD), true);
            return;
        }
        int s = Gear.stars(weapon);
        if (s >= Gear.MAX_STARS) {
            p.displayClientMessage(Component.literal("Это +10. Дальше — только Звезда Бездны с финального босса мира.").withStyle(ChatFormatting.DARK_PURPLE), true);
            return;
        }
        Item star = blessed ? ModRegistry.BLESSED_STAR_W1.get() : ModRegistry.STAR_W1.get();
        int slot = findStar(p.getInventory(), star);
        if (slot < 0) {
            p.displayClientMessage(Component.literal(blessed ? "Нет благословенной звезды (Shift+ПКМ)." : "Нет звезды заточки. Выбивай на этаже 1!").withStyle(ChatFormatting.RED), true);
            return;
        }
        p.getInventory().getItem(slot).shrink(1);
        ItemStack held = weapon.copy();
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        BUSY.add(p.getUUID());
        ServerLevel lvl = p.serverLevel();
        double chance = Gear.chance(s);
        boolean success = lvl.random.nextDouble() < chance;
        int target = s + 1;
        Vector3f col = rgb(Gear.starColor(target));
        double fx = forge.getX() + 0.5, fy = forge.getY() + 1.3, fz = forge.getZ() + 0.5;
        p.sendSystemMessage(Component.literal(String.format("Заточка %s до +%d • шанс %d%% • %s", com.pvpciv.util.Ru.name(held), target,
                (int) Math.round(chance * 100), blessed ? "благословенная звезда" : "обычная звезда (риск поломки)")).withStyle(ChatFormatting.GRAY));
        if (target >= 8) broadcast(lvl, p.getGameProfile().getName() + " точит " + com.pvpciv.util.Ru.name(held) + " до +" + target + "... шанс " + Math.round(chance * 100) + "%", ChatFormatting.LIGHT_PURPLE);
        // ритуал: 3 удара молота
        for (int i = 0; i < 3; i++) {
            int t = i * 12;
            float pitch = 0.8f + i * 0.15f;
            GameEvents.later(p.server, t, () -> {
                lvl.playSound(null, forge, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8f, pitch);
                lvl.sendParticles(new DustParticleOptions(col, 1.4f), fx, fy, fz, 20, 0.25, 0.3, 0.25, 0);
                lvl.sendParticles(ParticleTypes.LAVA, fx, fy, fz, 3, 0.1, 0.1, 0.1, 0);
            });
        }
        GameEvents.later(p.server, 44, () -> {
            BUSY.remove(p.getUUID());
            ServerPlayer pp = p.server.getPlayerList().getPlayer(p.getUUID());
            ItemStack result;
            if (success) {
                result = held;
                Gear.setStars(result, target);
                lvl.playSound(null, forge, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1f, 1.2f + target * 0.05f);
                lvl.playSound(null, forge, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1f, 1f);
                lvl.sendParticles(new DustParticleOptions(col, 2.0f), fx, fy, fz, 80, 0.5, 0.6, 0.5, 0);
                lvl.sendParticles(ParticleTypes.END_ROD, fx, fy, fz, 30, 0.2, 0.4, 0.2, 0.08);
                if (pp != null) Story.title(pp, "+" + target, "Успех! " + com.pvpciv.util.Ru.name(result), Gear.starColor(target));
                if (target >= 8) broadcast(lvl, "УСПЕХ! " + p.getGameProfile().getName() + " получил +" + target + "!", ChatFormatting.GOLD);
            } else if (blessed) {
                result = held;
                Gear.setStars(result, Math.max(0, s - 1));
                lvl.playSound(null, forge, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1f, 0.6f);
                lvl.sendParticles(ParticleTypes.SMOKE, fx, fy, fz, 30, 0.3, 0.3, 0.3, 0.02);
                if (pp != null) Story.title(pp, "Неудача", "Благословение спасло оружие: +" + Math.max(0, s - 1), 0xAAAAAA);
            } else {
                int tier = held.getItem() instanceof CivWeaponItem cw ? cw.tier : 1;
                result = held.is(ModRegistry.YUKI_KATANA.get()) ? com.pvpciv.game.Assassin.remainsOf(held, s) : new ItemStack(ModRegistry.WEAPON_SHARDS.get(), 1 + tier + s / 2);
                lvl.playSound(null, forge, SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 1f, 0.8f);
                lvl.playSound(null, forge, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1f, 0.6f);
                lvl.sendParticles(ParticleTypes.LARGE_SMOKE, fx, fy, fz, 40, 0.4, 0.4, 0.4, 0.03);
                lvl.sendParticles(ParticleTypes.CRIT, fx, fy, fz, 40, 0.4, 0.4, 0.4, 0.4);
                if (pp != null) Story.title(pp, "СЛОМАНО", com.pvpciv.util.Ru.name(held) + " → обломки", 0xFF5555);
                if (s >= 7) broadcast(lvl, p.getGameProfile().getName() + " сломал " + com.pvpciv.util.Ru.name(held) + ". Минута молчания.", ChatFormatting.DARK_GRAY);
            }
            if (pp != null) {
                if (pp.getMainHandItem().isEmpty()) pp.setItemInHand(InteractionHand.MAIN_HAND, result);
                else if (!pp.getInventory().add(result)) pp.drop(result, false);
                Story.ensureKit(pp);
            } else PENDING.put(p.getUUID(), result);
        });
    }

    private static void broadcast(ServerLevel lvl, String msg, ChatFormatting c) {
        for (ServerPlayer pl : lvl.getServer().getPlayerList().getPlayers())
            pl.sendSystemMessage(Component.literal("⚒ " + msg).withStyle(c));
    }

    private static Vector3f rgb(int c) {
        return new Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f);
    }

    public static boolean busy(ServerPlayer p) { return BUSY.contains(p.getUUID()); }
}
