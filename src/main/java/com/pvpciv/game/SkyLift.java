package com.pvpciv.game;

import com.pvpciv.data.StoryData;
import com.pvpciv.entity.CarmillaEntity;
import com.pvpciv.entity.LunaGuideEntity;
import com.pvpciv.net.Network;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModRegistry;
import com.pvpciv.world.CivWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.pvpciv.game.DialogueManager.opt;

/**
 * Небесный лифт: крыша общежития (этаж 1) ⇄ хаб этажа 2. Впервые к лифту не пускает Луна:
 * нужно убить Громилу и Хранительницу и выставить их трофеи на витрину, потом — разговор с ней.
 */
public final class SkyLift {
    public static final int C = 0x9A5AE0;
    private static final Set<UUID> RIDING = new HashSet<>();
    public static final String[] FLOORS = {"Нижний Шпиль", "Неоновый город", "???", "???", "Луна"};

    public static int unlocked(StoryData d) { return d.liftOk ? 0b11 : 0b1; }

    /** ПКМ по табло лифта. */
    private static final java.util.Map<UUID, Long> LAST = new java.util.HashMap<>();

    public static void use(ServerPlayer p) {
        if (RIDING.contains(p.getUUID())) return;
        long now = p.serverLevel().getGameTime();
        Long last = LAST.put(p.getUUID(), now);
        if (last != null && now - last < 10) return;   // ПКМ приходит дважды (обе руки) — один раз
        StoryData d = Progress.d(p);
        if (!d.liftOk && CivWorld.is(p.level()) && !CivWorld.isF2(p.level())) { summonLuna(p); return; }
        int cur = CivWorld.isF2(p.level()) ? 2 : 1;
        PacketDistributor.sendToPlayer(p, new Network.OpenLift(unlocked(d), cur));
    }

    private static void summonLuna(ServerPlayer p) {
        ServerLevel lvl = (ServerLevel) p.level();
        for (var e : lvl.getEntitiesOfClass(LunaGuideEntity.class, p.getBoundingBox().inflate(20), g -> p.getUUID().equals(g.onlyFor))) {
            lunaTalk(p, e);
            return;
        }
        LunaGuideEntity g = ModRegistry.LUNA_GUIDE.get().create(lvl);
        if (g == null) return;
        Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 at = p.position().add(look.scale(2.5));
        g.moveTo(at.x, p.getY(), at.z, p.getYRot() + 180, 0);
        g.onlyFor = p.getUUID();
        lvl.addFreshEntity(g);
        lvl.sendParticles(ParticleTypes.WITCH, at.x, at.y + 1, at.z, 40, 0.4, 0.8, 0.4, 0.05);
        lvl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.5f, 1.2f);
        lunaTalk(p, g);
    }

    public static void lunaTalk(ServerPlayer p, LunaGuideEntity g) {
        StoryData d = Progress.d(p);
        g.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, p.getEyePosition());
        DialogueManager.Dialogue dl = new DialogueManager.Dialogue("a");
        boolean killed = (d.trophies & 3) == 3, shown = (d.showcase & 3) == 3;
        if (!killed) {
            dl.node("a", "Луна", C, "Куда это ты собрался? Лифт — для тех, кто держит свой этаж. А на твоём до сих пор бродят Гнилой Громила и Хранительница Склепа. "
                    + "Убей обоих — и трофеи на витрину, чтобы все видели. Тогда и поговорим.", opt("Ладно…", null));
        } else if (!shown) {
            dl.node("a", "Луна", C, "О, трофеи-то у тебя есть. А на витрине пусто. Стесняешься? Поставь их на витрину у себя в квартире — она у стены, рядом со столом брони. "
                    + "Без витрины ты для этого мира никто.", opt("Сейчас поставлю", null));
        } else {
            dl.node("a", "Луна", C, "Ну надо же. Громила, Хранительница — и всё это стоит у тебя под стеклом. Я впечатлена. Чуть-чуть.",
                            opt("Что это за лифт?", "b"))
              .node("b", "Луна", C, "Это Шпиль. Башня над Бездной, и мы на её нижнем ярусе. Над каждым этажом — следующий, в своём небе. "
                            + "Десять этажей — один мир. На вершине мира сидит бог, и он кормится Искрой — той самой, что летит из тебя в каждой драке.",
                            opt("Бог? Серьёзно?", "c"))
              .node("c", "Луна", C, "Семь богов, семь миров. Вас тут разводят, как кур: дерётесь, растёте, отдаёте Искру. "
                            + "Правила для всех… кроме меня. Я давно вышла за их края. Скучно, знаешь ли, быть единственной.",
                            opt("Зачем ты мне это рассказываешь?", "d"))
              .node("d", "Луна", C, "Потому что ты забавный. Этаж 2 — Неоновый город. Улицы держит мафия, внизу, в канализации, — крысы со своим королём. "
                            + "Упадёшь с края — полетишь обратно сюда, вниз. Без парашюта не советую.",
                            opt("Поехали!", "e"))
              .node("e", "Луна", C, "Лифт твой. Табло — у кабины. И… не разочаруй меня, ладно?", pp -> {
                  StoryData dd = Progress.d(pp);
                  dd.liftOk = true;
                  pp.setData(ModRegistry.STORY.get(), dd);
                  com.pvpciv.progress.Titles.grant(pp, "sky_pass");
                  g.life = 60;
                  pp.sendSystemMessage(Component.literal("✦ Небесный лифт открыт: этаж 2 — Неоновый город.").withStyle(ChatFormatting.LIGHT_PURPLE));
              }, opt("[Уйти]", null));
        }
        DialogueManager.open(p, g, dl);
    }

    /** Поездка: 7-секундная катсцена, телепорт в середине (на чёрном экране). */
    public static void ride(ServerPlayer p, int floor) {
        StoryData d = Progress.d(p);
        if ((unlocked(d) & (1 << (floor - 1))) == 0 || RIDING.contains(p.getUUID())) return;
        int cur = CivWorld.isF2(p.level()) ? 2 : 1;
        if (floor == cur) return;
        RIDING.add(p.getUUID());
        if (p.isPassenger()) p.stopRiding();
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 10, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP, 80, 128, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 0, false, false));
        String title = "ЭТАЖ " + floor + "\n" + FLOORS[floor - 1];
        PacketDistributor.sendToPlayer(p, new Network.Cinematic(3, 150, title));
        ServerLevel from = (ServerLevel) p.level();
        from.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2f, 0.6f);
        from.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 40, 0.5, 1.5, 0.5, 0.05);
        GameEvents.later(p.server, 68, () -> {
            RIDING.remove(p.getUUID());
            if (!p.isAlive() || p.hasDisconnected()) return;
            p.removeEffect(MobEffects.LEVITATION);
            p.resetFallDistance();
            if (floor == 2) {
                Vec3 a = com.pvpciv.floor.Floor2.LIFT_ARRIVE;
                p.teleportTo(CivWorld.f2(p.server), a.x, a.y, a.z, 0, 0);
            } else {
                Vec3 a = com.pvpciv.world.DormBuilder.LIFT_ARRIVE;
                p.teleportTo(CivWorld.level(p.server), a.x, a.y, a.z, 0, 0);
            }
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1f, 1.4f);
        });
    }
}
