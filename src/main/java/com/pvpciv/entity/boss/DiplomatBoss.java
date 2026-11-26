package com.pvpciv.entity.boss;

import com.pvpciv.progress.Progress;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;

/**
 * 1.0 — Инопланетный дипломат (босс Мостика, этаж 5). Требует подписать невыгодные соглашения.
 *  • Ближний бой — удар планшетом; каждое попадание «взимает пошлину» (2% искр, до 500). Всё взятое вернётся, когда он падёт.
 *  • Дальний бой — веер документов.
 *  • «Подпиши здесь!» — летящий контракт: сбей его (3 удара), иначе он прилипнет: обездвиживает на 3 с и забирает искры.
 *  • «Аудит» — зона: 6 с нельзя пить зелья, есть и торговать (кошелёк заморожен).
 *  • «Встреча на высшем уровне» — зовёт охрану; пока охрана жива, он получает вдвое меньше урона.
 */
public class DiplomatBoss extends CivBoss {
    private static final Vector3f PAPER = new Vector3f(0.95f, 0.93f, 0.8f), AUDIT = new Vector3f(0.3f, 0.85f, 1f), RED = new Vector3f(1f, 0.25f, 0.2f);
    final Map<UUID, Long> stolen = new HashMap<>();
    private static final Map<UUID, Long> AUDITED = new HashMap<>();
    private final List<UUID> guards = new ArrayList<>();
    private int summonCd = 400;

    private static final String[] DEALS = {
            "«Предлагаю обмен: ваша Искра — на мою визитку. Взаимовыгодно!»",
            "«Пункт 14.3: всё ваше имущество становится нашим. Подпишите внизу.»",
            "«Налог на воздух. Вы дышите — вы платите.»",
            "«Мир, дружба, проценты. Двести процентов.»",
            "«Это не грабёж. Это дипломатия.»"};

    public DiplomatBoss(EntityType<? extends Monster> type, Level level) { super(type, level); }

    @Override public String defId() { return "diplomat"; }
    @Override public String model() { return "diplomat"; }
    @Override protected String[] actions() { return new String[]{"attack", "throw", "sign", "audit", "summon"}; }

    public static boolean audited(Player p) { Long t = AUDITED.get(p.getUUID()); return t != null && t > System.currentTimeMillis(); }

    @Override
    public boolean doHurtTarget(Entity e) {
        boolean hit = super.doHurtTarget(e);
        if (hit && e instanceof ServerPlayer p) tax(p, 0.02, 500, "Пошлина");
        return hit;
    }

    private void tax(ServerPlayer p, double frac, long max, String what) {
        long take = Math.min(max, (long) (Progress.d(p).money * frac));
        if (take <= 0) return;
        Progress.addMoney(p, -take, false);
        stolen.merge(p.getUUID(), take, Long::sum);
        p.displayClientMessage(Component.literal(what + ": −" + take + " ✦ (вернутся, когда Дипломат падёт)").withStyle(ChatFormatting.GOLD), true);
        playSound(SoundEvents.VILLAGER_TRADE, 1f, 0.6f);
    }

    @Override
    protected void ability(ServerLevel lvl, ServerPlayer t) {
        int r = random.nextInt(10);
        if (--summonCd <= 0 && guardsAlive(lvl) == 0 && r < 4) { summit(lvl); summonCd = enraged ? 8 : 9; }   // 1.1: охрану зовёт реже
        else if (r < 4) papers(lvl, t);
        else if (r < 7) contract(lvl, t);
        else audit(lvl, t);
        cooldown = (enraged ? 35 : 50) + random.nextInt(18);   // 1.1.2: чаще
        if (random.nextInt(3) == 0) for (ServerPlayer p : victims(lvl, 30))
            p.sendSystemMessage(Component.literal("✉ Дипломат: " + DEALS[random.nextInt(DEALS.length)]).withStyle(ChatFormatting.AQUA));
    }

    private void papers(ServerLevel lvl, ServerPlayer t) {
        faceTo(t.position());
        play("throw", 18);
        List<ServerPlayer> tg = spread(lvl, t, 3);   // 1.2: много игроков — бумаги делятся на нескольких
        int n = tg.size() > 1 ? (enraged ? 5 : 4) : (enraged ? 7 : 5);
        for (int k = 0; k < (enraged ? 2 : 1); k++) for (ServerPlayer tt : tg) later(6 + k * 12, () -> {
            if (!isAlive() || !tt.isAlive()) return;
            Vec3 dir = tt.getEyePosition().subtract(getEyePosition()).normalize();
            for (int i = 0; i < n; i++) {
                double a = Math.toRadians((i - (n - 1) / 2.0) * 9);
                Vec3 d = new Vec3(dir.x * Math.cos(a) - dir.z * Math.sin(a), dir.y, dir.x * Math.sin(a) + dir.z * Math.cos(a));
                Paper s = new Paper(lvl, this, dmg() * 0.9f);
                s.setItem(new ItemStack(Items.PAPER));
                s.setPos(getX(), getEyeY() - 0.3, getZ());
                s.shoot(d.x, d.y + 0.05, d.z, 1.1f, 2f);
                lvl.addFreshEntity(s);
            }
            lvl.playSound(null, blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.HOSTILE, 2f, 0.7f);
        });
    }

    /** Документ: бумага-снежок с уроном. На клиенте — обычный снежок с картинкой бумаги. */
    public static final class Paper extends Snowball {
        private final float dmg;
        public Paper(Level l, LivingEntity owner, float dmg) { super(l, owner); this.dmg = dmg; }
        @Override protected void onHitEntity(EntityHitResult r) {
            if (r.getEntity() instanceof ServerPlayer p && getOwner() instanceof LivingEntity o) { p.invulnerableTime = 0; p.hurt(damageSources().mobProjectile(this, o), dmg); }
        }
    }

    private void contract(ServerLevel lvl, ServerPlayer t) {
        play("sign", 22);
        castYaw = getYRot();
        lvl.playSound(null, blockPosition(), SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.HOSTILE, 2f, 0.8f);
        int n = enraged ? 2 : 1;
        List<ServerPlayer> vs = victims(lvl, 28);
        for (int i = 0; i < n && !vs.isEmpty(); i++) {
            ServerPlayer target = i == 0 ? t : vs.get(random.nextInt(vs.size()));
            later(10 + i * 6, () -> {
                if (!isAlive()) return;
                ContractEntity c = com.pvpciv.registry.ModMobs.CONTRACT.get().create(lvl);
                if (c == null) return;
                c.moveTo(getX(), getEyeY() + 0.5, getZ(), 0, 0);
                c.setup(this, target);
                lvl.addFreshEntity(c);
                target.displayClientMessage(Component.literal("«ПОДПИШИ ЗДЕСЬ!» — собей летящий контракт (3 удара)!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
            });
        }
    }

    /** Контракт долетел: обездвиживает и берёт искры. */
    void signed(ServerPlayer p) {
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 10, false, true));
        com.pvpciv.combat.CombatEvents.noJump(p, 60);
        tax(p, 0.05, 1500, "Контракт подписан");
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal("ПОДПИСАНО").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
    }

    private void audit(ServerLevel lvl, ServerPlayer t) {
        play("audit", 26);
        castYaw = getYRot();
        lvl.playSound(null, blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 2f, 1.4f);
        List<ServerPlayer> tg = spread(lvl, t, 2);
        for (ServerPlayer tt : tg) zone(lvl, tt.position(), tg.size() > 1 ? 5.0 : 6.5, 24, AUDIT).onHit(p -> {
            AUDITED.put(p.getUUID(), System.currentTimeMillis() + 6000);
            p.hurt(damageSources().indirectMagic(this, this), dmg() * 0.8f);
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal("АУДИТ").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)));
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.literal("6 с: зелья, еда и торговля заморожены").withStyle(ChatFormatting.GRAY)));
        });
    }

    private void summit(ServerLevel lvl) {
        play("summon", 30);
        castYaw = getYRot();
        lvl.playSound(null, blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 2f, 1f);
        for (ServerPlayer p : victims(lvl, 32))
            p.sendSystemMessage(Component.literal("✉ Дипломат: «ВСТРЕЧА НА ВЫСШЕМ УРОВНЕ! Охрана!» — пока охрана жива, он под защитой.").withStyle(ChatFormatting.AQUA));
        guards.clear();
        Vec3 c = home != null ? home : position();
        int lvlM = com.pvpciv.combat.Mobs.level(this);
        int n = enraged ? 3 : 2;   // 1.1: было 3/4
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            Vec3 at = safeSpot(lvl, c.add(Math.cos(a) * 6, 0, Math.sin(a) * 6), c);
            Mob g = com.pvpciv.registry.ModMobs.ANDROID.get().create(lvl);
            if (g == null) continue;
            g.moveTo(at.x, at.y, at.z, 0, 0);
            com.pvpciv.combat.Mobs.setupDefault(g);
            com.pvpciv.combat.Mobs.setup(g, Math.max(1, lvlM - 1), false, "Охрана посольства", "floor5_mob");
            g.addTag("boss_minion");
            var mh = g.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);   // 1.1: охрана вдвое хрупче
            if (mh != null) { mh.setBaseValue(mh.getBaseValue() * 0.5); g.setHealth(g.getMaxHealth()); }
            g.setCustomName(Component.literal("[" + (lvlM - 1) + "] Охрана посольства").withStyle(ChatFormatting.AQUA));
            lvl.addFreshEntity(g);
            guards.add(g.getUUID());
            com.pvpciv.entity.FxEntity.spawn(lvl, com.pvpciv.entity.FxEntity.PILLAR, at, 0.8f, 20, 0x40D0FF, 0xE0F8FF);
        }
    }

    private int guardsAlive(ServerLevel lvl) {
        int n = 0;
        for (UUID u : guards) if (lvl.getEntity(u) instanceof LivingEntity le && le.isAlive()) n++;
        return n;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (!level().isClientSide && level() instanceof ServerLevel lvl && src.getEntity() instanceof Player && guardsAlive(lvl) > 0) {
            amount *= 0.5f;   // 1.1: было 0.25 — «резисты бешеные»
            if (tickCount % 6 == 0) com.pvpciv.combat.DamageNumbers.show(lvl, this, Component.literal("ДИПЛОМАТИЧЕСКИЙ ИММУНИТЕТ").withStyle(x -> x.withColor(0x40D0FF).withBold(true)));
        }
        return super.hurt(src, amount);
    }

    @Override
    protected void passive(ServerLevel lvl) {
        if (tickCount % 20 == 0) for (UUID u : guards) if (lvl.getEntity(u) instanceof LivingEntity le && le.isAlive())
            for (int i = 0; i < 6; i++) { Vec3 q = position().add(0, 1.2, 0).lerp(le.position().add(0, 1, 0), i / 6.0); lvl.sendParticles(ParticleTypes.ELECTRIC_SPARK, q.x, q.y, q.z, 1, 0, 0, 0, 0); }
    }

    @Override
    protected void onEnrage(ServerLevel lvl) {
        summonCd = 2;
        for (ServerPlayer p : victims(lvl, 32))
            p.sendSystemMessage(Component.literal("✉ Дипломат: «Переговоры окончены. Начинаются САНКЦИИ.»").withStyle(ChatFormatting.RED));
    }

    @Override
    public void die(DamageSource src) {
        if (!level().isClientSide && level() instanceof ServerLevel sl) {
            stolen.forEach((u, v) -> { ServerPlayer p = sl.getServer().getPlayerList().getPlayer(u); if (p != null) { Progress.addMoney(p, v, true); p.sendSystemMessage(Component.literal("Возврат пошлин: +" + v + " ✦").withStyle(ChatFormatting.GOLD)); } });
            stolen.clear();
            for (UUID u : guards) if (sl.getEntity(u) != null) sl.getEntity(u).discard();
            for (ContractEntity c : sl.getEntitiesOfClass(ContractEntity.class, getBoundingBox().inflate(48))) c.discard();
        }
        super.die(src);
    }

    @SuppressWarnings("unused") private static final Vector3f R2 = RED;
    @SuppressWarnings("unused") private static final Vector3f P2 = PAPER;

    /** 1.1.2: «служебные записки» — быстрые документы в каждого между умениями; в ярости — лучи-сканеры. */
    @Override
    protected void pressure(ServerLevel lvl) {
        if (castTicks > 0) return;
        if (tickCount % (enraged ? 18 : 26) == 0)
            for (ServerPlayer p : victims(lvl, 24)) bolt(lvl, muzzle(), p, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER), dmg() * 0.28f, 1.3f, 3f);
        if (enraged && tickCount % 90 == 45)
            for (ServerPlayer p : victims(lvl, 24)) beam(lvl, muzzle(), p.position().add(0, 1, 0), 20, 1.0, 10, dmg() * 0.9f, new org.joml.Vector3f(0.4f, 1f, 0.6f), null);
    }
}
