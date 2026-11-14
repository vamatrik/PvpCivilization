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
        bus.addListener(CombatEvents::onArmor);   // 0.10.2: броня числом — последним шагом
        // 0.10: своя боёвка — без ванильных критов и ванильного размаха
        bus.addListener((net.neoforged.neoforge.event.entity.player.CriticalHitEvent e) -> { e.setCriticalHit(false); e.setDisableSweep(true); });
        bus.addListener((net.neoforged.neoforge.event.entity.player.SweepAttackEvent e) -> e.setSweeping(false));
        // 0.10.3: удар основной рукой — шлейф взмаха для остальных игроков
        bus.addListener((net.neoforged.neoforge.event.entity.player.AttackEntityEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer sp && sp.getMainHandItem().getItem() instanceof com.pvpciv.item.CivWeaponItem) {
                Melee.noteCharge(sp, sp.getAttackStrengthScale(0.5f));   // 0.10.8: заряд этого удара — для полного урона
                Melee.slash(sp, sp.getMainHandItem(), false); Melee.afterMain(sp);
            }
        });
        bus.addListener(CombatEvents::onXpDrop);
        bus.addListener(CombatEvents::onMobTick);
        bus.addListener(com.pvpciv.game.Leash::onTarget);
        bus.addListener(CombatEvents::onJoin);
    }

    /** Урон моба по игроку от разницы уровней: выше — +15% за уровень; ниже — 20/30/40/50/60% защиты (бета 12.11). */
    public static double levelTaken(int dl) {
        if (dl > 0) return 1 + 0.15 * dl;
        return switch (-dl) { case 0 -> 1.0; case 1 -> 0.92; case 2 -> 0.85; case 3 -> 0.78; case 4 -> 0.72; default -> 0.65; };   // бета 0.9.8: мягче (было 0.8…0.4)
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
        // бета 0.9.6: в кабине лифта путь идёт сквозь остров — не задыхаться
        if (target instanceof ServerPlayer lp && e.getSource().is(net.minecraft.world.damagesource.DamageTypes.IN_WALL) && lp.getVehicle() instanceof com.pvpciv.entity.LiftCabin) { e.setCanceled(true); return; }
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
        // 0.10.5: ближний бой игрока — без ванильной неуязвимости 0.5 с после удара (иначе быстрые удары парой «съедались»;
        // темп и так ограничен зарядом рук)
        if (src instanceof ServerPlayer && e.getSource().getDirectEntity() == src) e.setInvulnerabilityTicks(0);
        // 0.9.9: ванильная броня (потолок 30, тает от больших чисел) выключена — своя формула ниже
        if (target instanceof Player || Mobs.level(target) > 0) e.addReductionModifier(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ARMOR, (c, r) -> 0f);
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
            e.setAmount(e.getAmount() * (float) Melee.chargeFix(attacker, e) * (float) (1 + 0.015 * (la - 1)) * (float) Melee.handMul(attacker));
            crit(e, attacker, victim);
            if (com.pvpciv.game.Enchanting.dodge(victim)) { e.setCanceled(true); return; }
            boolean ml = e.getSource().getDirectEntity() == attacker;
            e.setAmount(com.pvpciv.game.Enchanting.onDefend(victim, attacker, com.pvpciv.game.Enchanting.onAttack(attacker, victim, e.getAmount(), ml), ml));
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
                DamageNumbers.show((ServerLevel) mob.level(), mob, Component.literal("ИММУНИТЕТ").withStyle(x -> x.withColor(0xB8B0C8).withBold(true)));
                p.displayClientMessage(Component.literal("Враг слишком силён для тебя (+" + dl + " ур.)").withStyle(ChatFormatting.DARK_RED), true);
                return;
            }
            e.setAmount((float) (e.getAmount() * Melee.chargeFix(p, e) * (1 - r) * (1 + 0.015 * (Progress.d(p).level - 1)) * Melee.handMul(p)));
            crit(e, p, mob);
            e.setAmount(com.pvpciv.game.Enchanting.onAttack(p, mob, e.getAmount(), e.getSource().getDirectEntity() == p));
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
            if (com.pvpciv.game.Enchanting.dodge(p)) { e.setCanceled(true); return; }
            e.setAmount((float) (e.getAmount() * levelTaken(dl) * crowd(p, mob)));
            e.setAmount(com.pvpciv.game.Enchanting.onDefend(p, mob, e.getAmount(), e.getSource().getDirectEntity() == mob));
            Mobs.Kind k = Mobs.Kind.of(mob.getType());
            if (k == Mobs.Kind.FROST_ZOMBIE || k == Mobs.Kind.FROST_ARCHER || k == Mobs.Kind.FROST_SPIDER) frost(p, lvlOf(p));
            if (k == Mobs.Kind.FOX || k == Mobs.Kind.KITSUNE_ARCHER || mob.getTags().contains("kumiho_court")) Bleed.hit(p);   // 0.9.9: кровотечение лис
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

    /** Отладка баланса: /civ admin dmglog — печать каждого удара игрока в лог сервера. */
    public static boolean DMGLOG;

    private static void onDamaged(LivingDamageEvent.Post e) {
        LivingEntity target = e.getEntity();
        if (!(target.level() instanceof ServerLevel lvl)) return;
        if (!(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        float dmg = e.getNewDamage();
        if (DMGLOG) System.out.printf("DMGLOG t=%d w=%s off=%s raw=%.2f new=%.2f target=%s%n", p.serverLevel().getGameTime(),
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).getPath(), net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(p.getOffhandItem().getItem()).getPath(),
                e.getOriginalDamage(), dmg, target.getType().toShortString());
        if (dmg <= 0) return;
        boolean crit = false;   // 0.10: ванильных критов в прыжке больше нет
        if (target instanceof LivingEntity && e.getSource().getDirectEntity() == p && !Melee.isSweeping()) Melee.cleave(p, target, dmg);
        if (e.getSource().getDirectEntity() == p) com.pvpciv.game.Enchanting.onDealt(p, dmg);
        Long ct = CRIT_MARK.remove(target.getUUID());
        boolean bigCrit = ct != null && lvl.getGameTime() - ct <= 1;   // крит Юки — крупное красное число
        String txt = Balance.fmt(dmg);
        int cmb = Melee.combo(p);
        Component c = bigCrit ? Component.literal("✦" + txt + "!").withStyle(x -> x.withColor(0xFF2A2A).withBold(true))
                : cmb > 0 ? Component.literal(txt).withStyle(x -> x.withColor(comboColor(cmb)).withBold(true))   // удар комбо: от светло-жёлтого (×1) к насыщенному оранжевому (×10)
                : crit ? Component.literal(txt + "!").withStyle(x -> x.withColor(0xFFC830).withBold(true))
                : Component.literal(txt).withStyle(x -> x.withColor(0xFFFFFF));
        DamageNumbers.show(lvl, target, c, bigCrit ? 2.0f : cmb > 0 ? 1.1f + 0.04f * cmb : 1f);
        if (target instanceof Mob mob && Mobs.level(mob) > 0) {
            Progress.d(p).hits++;
            BossManager.recordDamage(mob, p, dmg);
            if (mob instanceof com.pvpciv.entity.boss.CivBoss cb) com.pvpciv.game.Marks.onHitBoss(p, cb);
            // 0.10.5: HP цели больше не пишется над хотбаром — видно на полоске над мобом и на полосе босса
        }
    }

    private static void onDeath(LivingDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (dead.level().isClientSide) return;
        if (dead instanceof ServerPlayer p) {
            Entity killer = e.getSource().getEntity();
            boolean pvp = killer instanceof ServerPlayer k && k != p;
            if (pvp) com.pvpciv.game.Assassin.onKill((ServerPlayer) killer, com.pvpciv.game.Assassin.PLAYER);
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
            // бета 0.9.6: призванные боссом мобы опыта не дают
            boolean minion = mob.getTags().stream().anyMatch(t -> t.equals("boss_minion") || t.equals("don_goon") || t.equals("king_rat") || t.equals("kumiho_court"));
            // бета 0.9.8: уровень игрока выше потолка этажа (5/10/15) → опыт ×0.5
            int fl = Math.max(1, com.pvpciv.world.CivWorld.floor(mob.level()));
            if (Progress.d(p).level > fl * 5) mult *= 0.5;
            long xp = minion ? 0 : Progress.xpForMob(Progress.d(p).level, Mobs.level(mob), mult);
            if (xp > 0) Progress.addXp(p, xp);
            com.pvpciv.game.Vampire.onKill(p, mob, kind.endsWith("_elite"));
            com.pvpciv.game.Library.onKill(p, mob, kind.endsWith("_elite"));
            com.pvpciv.game.Enchanter.onKill(p, mob);
            com.pvpciv.game.Assassin.onKill(p, kind.endsWith("_elite") ? com.pvpciv.game.Assassin.ELITE : com.pvpciv.game.Assassin.MOB);
            com.pvpciv.floor.SpawnZones.onDeath(mob);
        }
    }

    private static void onDrops(LivingDropsEvent e) {
        LivingEntity dead = e.getEntity();
        if (!(dead instanceof Mob mob) || Mobs.level(mob) <= 0 || BossManager.isBoss(mob)) return;
        e.getDrops().clear();
        // бета 0.9.8: призванные боссом мобы и лута не дают
        if (mob.getTags().stream().anyMatch(t -> t.equals("boss_minion") || t.equals("don_goon") || t.equals("king_rat") || t.equals("kumiho_court"))) return;
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

    /** 0.9.0: криты Юки — только прямой удар в ближнем бою. */
    private static void crit(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent e, ServerPlayer p, LivingEntity target) {
        if (e.getSource().getDirectEntity() != p) return;
        double ch = com.pvpciv.game.Assassin.critChance(p);
        if (ch <= 0 || p.getRandom().nextDouble() >= ch) return;
        e.setAmount((float) (e.getAmount() * com.pvpciv.game.Assassin.critMult(p)));
        CRIT_MARK.put(target.getUUID(), p.serverLevel().getGameTime());
        com.pvpciv.game.Assassin.particlesCrit(p, target);
        if (target instanceof com.pvpciv.entity.boss.KappaBoss k) k.critHit(p);
    }

    /** 0.10.2: броня вычитается из итогового удара (после уровней, толпы, критов). Броня ≥ 2× удара — иммунитет. */
    private static void onArmor(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre e) {
        LivingEntity t = e.getEntity();
        if (t.level().isClientSide) return;
        if (e.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR) || e.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (!(t instanceof Player) && Mobs.level(t) <= 0) return;
        if (!(e.getSource().getEntity() instanceof LivingEntity)) return;   // броня — только от ударов существ (лава, падение, голод — мимо)
        int pl = t instanceof ServerPlayer sp ? Progress.d(sp).level : 0;
        double armor = Balance.armor(t, pl);
        if (armor <= 0 && Balance.protection(t) <= 0) return;
        float before = e.getNewDamage();
        // 0.11: броня «срезает» удар пропорционально весу оружия. Раньше она вычиталась одинаково из любого удара,
        // и быстрые лёгкие клинки (сабля ×0.42, кинжал ×0.32 урона за удар) теряли в разы больше, чем топор и молот.
        if (e.getSource().getEntity() instanceof ServerPlayer wp && e.getSource().getDirectEntity() == wp
                && Melee.striking(wp).getItem() instanceof com.pvpciv.item.CivWeaponItem cw) armor *= cw.cls.dmg;
        double pen = Balance.PEN;
        if (e.getSource().getEntity() instanceof ServerPlayer ap && e.getSource().getDirectEntity() == ap) pen = Math.max(pen, com.pvpciv.game.Enchanting.pen(ap));   // 0.11: «Пробой»
        double after = Balance.mitigate(before, armor, pen, Balance.protection(t));
        e.setNewDamage((float) after);
        if (after <= 0 && before > 0 && e.getSource().getEntity() instanceof ServerPlayer atk && t.level() instanceof ServerLevel lvl) {
            DamageNumbers.show(lvl, t, Component.literal("ИММУНИТЕТ").withStyle(x -> x.withColor(0xB8B0C8).withBold(true)));
            atk.displayClientMessage(Component.literal("Броня цели вдвое крепче твоего удара").withStyle(ChatFormatting.DARK_GRAY), true);
        }
    }

    /** Цвет цифры комбо: ×1 — 0xFFD890, ×10 — 0xFF5A00. */
    public static int comboColor(int c) {
        float t = Math.max(0, Math.min(1, (c - 1) / 9f));
        int r = 255, g = Math.round(0xD8 + (0x5A - 0xD8) * t), b = Math.round(0x90 + (0x00 - 0x90) * t);
        return r << 16 | g << 8 | b;
    }

    /** Цель → тик крита Юки (для крупного красного числа). */
    private static final java.util.Map<java.util.UUID, Long> CRIT_MARK = new java.util.HashMap<>();

    private static void onXpDrop(LivingExperienceDropEvent e) {
        if (e.getEntity() instanceof Mob mob && Mobs.level(mob) > 0) e.setCanceled(true);
    }

    private static void onMobTick(EntityTickEvent.Post e) {
        if (e.getEntity() instanceof Mob m && !m.level().isClientSide) {
            if (m.isOnFire() && !m.isInLava() && Mobs.level(m) > 0) m.clearFire();
            com.pvpciv.game.Leash.tick(m);
        }
    }

    private static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide) return;
        Entity en = e.getEntity();
        // 0.9.9: мобы, заспавненные до новой шкалы, — пересчитать HP и урон пропорционально
        if (en instanceof Mob m && !m.getTags().contains("bal99") && Mobs.level(m) > 0) {
            int l = Mobs.level(m);
            double kh = Balance.mobHp(l) / Balance.oldHp(l), kd = Balance.mobDmg(l) / Balance.oldDmg(l);
            var hpA = m.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
            var dA = m.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            float frac = m.getMaxHealth() > 0 ? m.getHealth() / m.getMaxHealth() : 1f;
            if (hpA != null) hpA.setBaseValue(hpA.getBaseValue() * kh);
            if (dA != null) dA.setBaseValue(dA.getBaseValue() * kd);
            m.setHealth(Math.max(1f, m.getMaxHealth() * frac));
            m.addTag("bal99");
        }
        if (en.getTags().contains("pvpciv_dmgnum")) {
            if (!LIVE_NUMBERS.contains(en.getUUID())) e.setCanceled(true);
        }
    }

    @SuppressWarnings("unused") private static final Class<?> P = Player.class;
}
