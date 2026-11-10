package com.pvpciv.combat;

import com.pvpciv.floor.BossManager;
import com.pvpciv.floor.Zones;
import com.pvpciv.game.Story;
import com.pvpciv.loot.LootTables;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Уровни, сопротивления, цифры урона, дроп, штрафы смерти. */
public final class CombatEvents {
    public static final Set<UUID> LIVE_NUMBERS = new HashSet<>();

    public static void register(IEventBus bus) {
        bus.addListener(CombatEvents::onIncoming);
        bus.addListener(CombatEvents::onDamaged);
        bus.addListener(CombatEvents::onDeath);
        bus.addListener(CombatEvents::onDrops);
        bus.addListener(CombatEvents::onXpDrop);
        bus.addListener(CombatEvents::onMobTick);
        bus.addListener(CombatEvents::onJoin);
    }

    public static double resist(int deltaLevel) {
        return Math.min(1.0, Math.max(0.0, 0.2 * (deltaLevel - 1)));
    }

    private static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity target = e.getEntity();
        if (target.level().isClientSide) return;
        Entity src = e.getSource().getEntity();
        // PvP
        if (target instanceof ServerPlayer victim && src instanceof ServerPlayer attacker && attacker != victim) {
            Zones.Zone za = Zones.at(attacker.position()), zv = Zones.at(victim.position());
            if (za.type() != Zones.Type.PVP || zv.type() != Zones.Type.PVP) {
                e.setCanceled(true);
                attacker.displayClientMessage(Component.literal("Здесь PvP выключено").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            e.setAmount(e.getAmount() * (float) (1 + 0.01 * (Progress.d(attacker).level - 1)));
            return;
        }
        // игрок бьёт моба с уровнем
        if (src instanceof ServerPlayer p && target instanceof Mob mob && Mobs.level(mob) > 0) {
            int dl = Mobs.level(mob) - Progress.d(p).level;
            double r = resist(dl);
            if (r >= 1.0) {
                e.setCanceled(true);
                DamageNumbers.show((ServerLevel) mob.level(), mob, Component.literal("ИММУНЕТ").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD));
                p.displayClientMessage(Component.literal("Враг слишком силён для тебя (+" + dl + " ур.)").withStyle(ChatFormatting.DARK_RED), true);
                return;
            }
            e.setAmount((float) (e.getAmount() * (1 - r) * (1 + 0.01 * (Progress.d(p).level - 1))));
            return;
        }
        // моб с уровнем бьёт игрока
        if (target instanceof ServerPlayer p && src instanceof Mob mob && Mobs.level(mob) > 0) {
            int dl = Mobs.level(mob) - Progress.d(p).level;
            if (dl > 0) e.setAmount((float) (e.getAmount() * (1 + 0.07 * dl)));
        }
    }

    private static void onDamaged(LivingDamageEvent.Post e) {
        LivingEntity target = e.getEntity();
        if (!(target.level() instanceof ServerLevel lvl)) return;
        if (!(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        float dmg = e.getNewDamage();
        if (dmg <= 0) return;
        boolean crit = p.fallDistance > 0 && !p.onGround() && !p.isInWater();
        String txt = dmg >= 10 ? String.valueOf(Math.round(dmg)) : String.format("%.1f", dmg);
        Component c = crit ? Component.literal(txt + "!").withStyle(x -> x.withColor(0xFFC830).withBold(true))
                : Component.literal(txt).withStyle(x -> x.withColor(0xFFFFFF));
        DamageNumbers.show(lvl, target, c);
        if (target instanceof Mob mob && Mobs.level(mob) > 0) {
            BossManager.recordDamage(mob, p, dmg);
            Component name = mob.getCustomName() != null ? mob.getCustomName() : mob.getName();
            p.displayClientMessage(name.copy().append(Component.literal(String.format("  ❤ %d/%d", (int) Math.ceil(Math.max(0, mob.getHealth())), (int) mob.getMaxHealth())).withStyle(ChatFormatting.RED)), true);
        }
    }

    private static void onDeath(LivingDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (dead.level().isClientSide) return;
        if (dead instanceof ServerPlayer p) {
            Entity killer = e.getSource().getEntity();
            boolean pvp = killer instanceof ServerPlayer k && k != p;
            long xpBefore = Progress.d(p).xp;
            long lost = Progress.deathPenalty(p, pvp ? 0.25 : 0.10);
            String killerName = killer == null ? "" : killer.getName().getString();
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, new com.pvpciv.net.Network.DeathInfo(lost, xpBefore - Progress.d(p).xp, pvp, killerName));
            if (pvp && lost > 0) {
                ItemEntity drop = new ItemEntity(p.level(), p.getX(), p.getY() + 0.5, p.getZ(), new ItemStack(ModRegistry.SPARKS.get(), (int) Math.min(lost, 64 * 36)));
                p.level().addFreshEntity(drop);
            }
            p.sendSystemMessage(Component.literal(pvp
                    ? "Ты выронил " + lost + " ✦ — их можно подобрать. Половина опыта до уровня сгорела."
                    : "Сгорело " + lost + " ✦ и половина опыта до следующего уровня.").withStyle(ChatFormatting.RED));
            return;
        }
        if (dead instanceof Mob mob && Mobs.level(mob) > 0 && e.getSource().getEntity() instanceof ServerPlayer p && !BossManager.isBoss(mob)) {
            String kind = Mobs.lootKind(mob);
            double mult = kind.endsWith("_elite") ? 3.0 : 1.0;
            Progress.addXp(p, Progress.xpFor(Progress.d(p).level, Mobs.level(mob), mult));
            com.pvpciv.game.Vampire.onKill(p, mob, kind.endsWith("_elite"));
        }
    }

    private static void onDrops(LivingDropsEvent e) {
        LivingEntity dead = e.getEntity();
        if (!(dead instanceof Mob mob) || Mobs.level(mob) <= 0 || BossManager.isBoss(mob)) return;
        e.getDrops().clear();
        String kind = Mobs.lootKind(mob);
        if (kind.isEmpty()) return;
        List<ItemStack> loot = LootTables.roll(kind, mob.getRandom(), 1.0);
        for (ItemStack st : loot) {
            ItemEntity ie = new ItemEntity(mob.level(), mob.getX(), mob.getY() + 0.5, mob.getZ(), st);
            ie.setDefaultPickUpDelay();
            e.getDrops().add(ie);
            Story.announceRareDrop(mob.level(), st);
        }
    }

    private static void onXpDrop(LivingExperienceDropEvent e) {
        if (e.getEntity() instanceof Mob mob && Mobs.level(mob) > 0) e.setCanceled(true);
    }

    private static void onMobTick(EntityTickEvent.Post e) {
        if (e.getEntity() instanceof Mob m && !m.level().isClientSide && m.isOnFire() && !m.isInLava() && Mobs.level(m) > 0) m.clearFire();
    }

    private static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide) return;
        Entity en = e.getEntity();
        if (en.getTags().contains("pvpciv_dmgnum")) {
            if (!LIVE_NUMBERS.contains(en.getUUID())) e.setCanceled(true);
        }
    }

    @SuppressWarnings("unused") private static final Class<?> P = Player.class;
}
