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

    /** Урон моба по игроку от разницы уровней: выше — +15% за уровень; ниже — 20/30/40/50/60% защиты (бета 12.11). */
    public static double levelTaken(int dl) {
        if (dl > 0) return 1 + 0.15 * dl;
        return switch (-dl) { case 0 -> 1.0; case 1 -> 0.8; case 2 -> 0.7; case 3 -> 0.6; case 4 -> 0.5; default -> 0.4; };
    }

    /** Толпа: каждый следующий моб, бивший тебя за последние 3 с, бьёт слабее (2 — ×0.91, 3 — ×0.83, 5 — ×0.71). */
    private static final java.util.Map<java.util.UUID, java.util.Map<Integer, Long>> HITTERS = new java.util.HashMap<>();
    private static double crowd(ServerPlayer p, Mob mob) {
        long now = p.serverLevel().getGameTime();
        var m = HITTERS.computeIfAbsent(p.getUUID(), k -> new java.util.HashMap<>());
        m.values().removeIf(t -> now - t > 60);
        m.put(mob.getId(), now);
        return 1.0 / (1 + 0.10 * (m.size() - 1));
    }

    public static final int PVP_GAP = 7;

    public static void forget(ServerPlayer p) { HITTERS.remove(p.getUUID()); }

    public static double resist(int deltaLevel) {
        return Math.min(1.0, Math.max(0.0, 0.25 * (deltaLevel - 1)));   // +2 ур. — 25%, +3 — 50%, +4 — 75%, +5 и выше — иммунитет
    }

    private static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity target = e.getEntity();
        if (target.level().isClientSide) return;
        Entity src = e.getSource().getEntity();
        // в клетке Кармиллы от голода не умирают
        if (target instanceof ServerPlayer jp && e.getSource().is(net.minecraft.world.damagesource.DamageTypes.STARVE) && com.pvpciv.game.Vampire.jailed(jp)) { e.setCanceled(true); return; }
        // мобы цивилизации друг друга не ранят (стрела лучника в зомби и т. п.)
        if (src instanceof net.minecraft.world.entity.Mob && !(target instanceof net.minecraft.world.entity.player.Player)
                && Mobs.level((net.minecraft.world.entity.Mob) src) > 0 && target instanceof net.minecraft.world.entity.Mob tm && Mobs.level(tm) > 0) {
            e.setCanceled(true);
            return;
        }
        // похищение Кармиллой: жертву никто не трогает
        if (target instanceof ServerPlayer tp && tp.getPersistentData().getLong("abductSafeUntil") > tp.serverLevel().getGameTime()) { e.setCanceled(true); return; }
        // оглушённый не атакует
        if (src instanceof LivingEntity le && le.hasEffect(com.pvpciv.registry.ModRegistry.STUN) && e.getSource().getDirectEntity() == src) { e.setCanceled(true); return; }
        // PvP
        if (target instanceof ServerPlayer victim && src instanceof ServerPlayer attacker && attacker != victim) {
            Zones.Zone za = Zones.at(attacker), zv = Zones.at(victim);
            if (za.type() != Zones.Type.PVP || zv.type() != Zones.Type.PVP) {
                e.setCanceled(true);
                attacker.displayClientMessage(Component.literal("Здесь PvP выключено").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            int la = Progress.d(attacker).level, lv = Progress.d(victim).level;
            if ((Progress.d(victim).pvpShield && la - lv >= PVP_GAP) || (Progress.d(attacker).pvpShield && lv - la >= PVP_GAP)) {
                e.setCanceled(true);
                attacker.displayClientMessage(Component.literal(Progress.d(attacker).pvpShield
                        ? "У тебя выключено PvP с игроками на " + PVP_GAP + "+ уровней выше (Герой → Настройки)"
                        : "Этот игрок выключил PvP с теми, кто на " + PVP_GAP + "+ уровней выше").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            e.setAmount(e.getAmount() * (float) (1 + 0.015 * (la - 1)));
            // «Оглушающий удар» — только если удар прошёл (раньше оглушал и в безопасной зоне, где урон отменён)
            if (e.getSource().getDirectEntity() == attacker) com.pvpciv.game.WeaponSkills.onHit(attacker, victim);
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
            e.setAmount((float) (e.getAmount() * (1 - r) * (1 + 0.015 * (Progress.d(p).level - 1))));
            if (e.getSource().getDirectEntity() == p) com.pvpciv.game.WeaponSkills.onHit(p, mob);
            return;
        }
        // моб с уровнем бьёт игрока
        if (target instanceof ServerPlayer p && src instanceof Mob mob && Mobs.level(mob) > 0) {
            // стрелы лучников — от уровня моба (раньше ванильные ≈4 на любом уровне)
            if (e.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow
                    && mob instanceof com.pvpciv.entity.mob.CivSkeleton && !(mob instanceof com.pvpciv.entity.mob.CivGunner))
                e.setAmount((float) (mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 1.4));   // бета 12.11: лучники бьют сильнее (стреляют реже, чем бьют в ближнем бою)
            int dl = Mobs.level(mob) - Progress.d(p).level;
            e.setAmount((float) (e.getAmount() * levelTaken(dl) * crowd(p, mob)));
            Mobs.Kind k = Mobs.Kind.of(mob.getType());
            if (k == Mobs.Kind.FROST_ZOMBIE || k == Mobs.Kind.FROST_ARCHER || k == Mobs.Kind.FROST_SPIDER) frost(p, lvlOf(p));
        }
    }

    private static ServerLevel lvlOf(ServerPlayer p) { return p.serverLevel(); }

    /** Мобы Кряжа: каждый удар — стак холода (сброс через 6 с без ударов), на 5 стаках — заморозка 1.5 с. */
    public static void frost(ServerPlayer p, ServerLevel lvl) {
        var t = p.getPersistentData();
        long now = lvl.getGameTime();
        int stacks = now - t.getLong("frostAt") > 120 ? 0 : t.getInt("frost");
        stacks++;
        t.putLong("frostAt", now);
        lvl.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 1, p.getZ(), 6 + stacks * 3, 0.4, 0.6, 0.4, 0.02);
        if (stacks >= 5) {
            stacks = 0;
            p.setTicksFrozen(Math.max(p.getTicksFrozen(), 200));   // синяя рамка мороза
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 30, 6, false, false));
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.JUMP, 30, 128, false, false));
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN, 30, 4, false, false));
            lvl.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.GLASS_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.6f);
            lvl.sendParticles(net.minecraft.core.particles.ParticleTypes.ITEM_SNOWBALL, p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.8, 0.4, 0.1);
            p.displayClientMessage(Component.literal("❄ ЗАМОРОЖЕН").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
        } else {
            p.setTicksFrozen(Math.max(p.getTicksFrozen(), stacks * 32));   // ванильный иней по краям экрана растёт со стаками
            if (stacks >= 3) p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 120, 0, false, false));
            p.displayClientMessage(Component.literal("❄ Холод " + stacks + "/5").withStyle(ChatFormatting.AQUA), true);
        }
        t.putInt("frost", stacks);
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
            Progress.d(p).hits++;
            BossManager.recordDamage(mob, p, dmg);
            if (mob instanceof com.pvpciv.entity.boss.CivBoss cb) com.pvpciv.game.Marks.onHitBoss(p, cb);
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
                    : "Сгорело " + lost + " ✦ и 15% опыта до следующего уровня.").withStyle(ChatFormatting.RED));
            return;
        }
        if (dead instanceof Mob mob && Mobs.level(mob) > 0 && e.getSource().getEntity() instanceof ServerPlayer p && !BossManager.isBoss(mob)) {
            String kind = Mobs.lootKind(mob);
            double mult = kind.endsWith("_elite") ? 3.0 : 1.0;
            Progress.addXp(p, Progress.xpFor(Progress.d(p).level, Mobs.level(mob), mult));
            com.pvpciv.game.Vampire.onKill(p, mob, kind.endsWith("_elite"));
            com.pvpciv.floor.SpawnZones.onDeath(mob);
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
            Story.announceRareDrop(mob.level(), st, e.getSource().getEntity() instanceof ServerPlayer kp ? kp.getGameProfile().getName() : null);
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
