package com.pvpciv.game;

import com.pvpciv.data.StoryData;
import com.pvpciv.entity.CarmillaEntity;
import com.pvpciv.entity.LunaGuideEntity;
import com.pvpciv.entity.LiftCabin;
import net.minecraft.core.BlockPos;
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
    public static final String[] FLOORS = {"Нижний Шпиль", "Неоновый город", "Тихая библиотека", "НТЦ «NaZa»", "Станция «Орбита»"};
    /** 1.0: лифт станции починен адским следом — этаж 5 в списке лифта. */
    public static boolean f5Ok(StoryData d) { return d.gates.getInt("lift5") > 0; }

    /** Этаж 3 — когда пали оба босса этажа 2 (Дон и Крысиный король, их трофеи). */
    public static boolean f3Ok(StoryData d) { return d.liftOk && (d.trophies & 12) == 12 && (d.showcase & 12) == 12; }   // бета 0.9.6: и трофеи на витрине
    /** 0.11: этаж 4 — когда пали братья-каппы и Кумихо (трофеи на витрине). */
    public static boolean f4Ok(StoryData d) { return f3Ok(d) && (d.trophies & 48) == 48 && (d.showcase & 48) == 48; }
    public static int unlocked(StoryData d) { return !d.liftOk ? 0b1 : f4Ok(d) ? (f5Ok(d) ? 0b11111 : 0b1111) : f3Ok(d) ? 0b111 : 0b11; }

    /** 1.0: пульт у шахты лифта в атриуме станции: ремонт (адский след) или вызов лифта. */
    public static boolean onF5Console(ServerPlayer p, BlockPos pos) {
        if (!CivWorld.isF5(p.level()) || !pos.equals(com.pvpciv.floor.Floor5.LIFT_CONSOLE())) return false;
        StoryData d = Progress.d(p);
        if (f5Ok(d)) { use(p); return true; }
        if (com.pvpciv.inv.Bag.count(p, s -> s.is(com.pvpciv.registry.ModF5.HELL_TRAIL.get())) <= 0) {
            Story.say(p, "Лифт станции", C, "НЕИСПРАВЕН. Кабина застряла между мирами. Для ремонта нужен «адский след» — его оставляет демоница Пустоты (пояс астероидов, после Галактического червя).");
            return true;
        }
        com.pvpciv.inv.Bag.take(p, s -> s.is(com.pvpciv.registry.ModF5.HELL_TRAIL.get()), 1);
        d.gates.putInt("lift5", 1);
        p.setData(ModRegistry.STORY.get(), d);
        ServerLevel l = p.serverLevel();
        l.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1f, 0.8f);
        l.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5f, 1f);
        l.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 60, 1.5, 2, 1.5, 0.05);
        Story.title(p, "Лифт починен", "Этаж 5 теперь в лифте — ракета больше не нужна", 0xFF8A30);
        if (!LiftShaft.isOpen(p.server)) LiftShaft.start(p);   // 1.1: шахта раскрывается, кабина уходит вниз
        com.pvpciv.progress.Titles.grant(p, "liftfixer");
        p.sendSystemMessage(Component.literal("✦ Адский след вплавлен в механизм. Небесный лифт ходит до станции «Орбита». Первый мир пройден — дальше только Ад (этаж 6, скоро).").withStyle(ChatFormatting.GOLD));
        return true;
    }

    /** ПКМ по табло лифта. */
    private static final java.util.Map<UUID, Long> LAST = new java.util.HashMap<>();

    public static void use(ServerPlayer p) {
        if (RIDING.contains(p.getUUID())) return;
        long now = p.serverLevel().getGameTime();
        Long last = LAST.put(p.getUUID(), now);
        if (last != null && now - last < 10) return;   // ПКМ приходит дважды (обе руки) — один раз
        StoryData d = Progress.d(p);
        if (!d.liftOk && CivWorld.is(p.level()) && !CivWorld.isF2(p.level())) { summonLuna(p); return; }
        int cur = Math.max(1, CivWorld.floor(p.level()));
        if (cur == 3 && !f4Ok(d)) p.displayClientMessage(Component.literal((d.trophies & 48) != 48 ? "Этаж 4 откроется, когда падут братья-каппы и Кумихо." : "Этаж 4: поставь трофеи капп и Кумихо на витрину в квартире.").withStyle(ChatFormatting.GRAY), true);
        if (cur == 2 && !f3Ok(d)) p.displayClientMessage(Component.literal((d.trophies & 12) != 12 ? "Этаж 3 откроется, когда падут Дон Витторио и Крысиный король." : "Этаж 3: поставь трофеи Дона и Крысиного короля на витрину в квартире.").withStyle(ChatFormatting.GRAY), true);
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
        // катсцена: Луна выходит из кабины, игрок стоит напротив двери (без застреваний в стенах)
        Vec3 at = new Vec3(0.5, com.pvpciv.world.RoofBuilder.RY, -24.6);
        p.teleportTo(p.serverLevel(), 0.5, com.pvpciv.world.RoofBuilder.RY, -23.3, 180, 8);   // внутри зала (z=-21 — уже над атриумом общаги)
        g.moveTo(at.x, at.y, at.z, 0, 0);
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
            dl.node("a", "Луна", C, "О, трофеи-то у тебя есть. А на витрине пусто. Стесняешься? Поставь их на витрину у себя в квартире — полки на стенах, по этажам. "
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
              .node("e", "Луна", C, "Лифт твой. Табло этажей — в кабине: просто нажми на неё. И… не разочаруй меня, ладно?", pp -> {
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

    /** Поездка: кабина едет по монорельсу с игроком внутри, на чёрном экране — переход между этажами, вторая кабина довозит до станции. */
    public static void ride(ServerPlayer p, int floor) {
        StoryData d = Progress.d(p);
        if ((unlocked(d) & (1 << (floor - 1))) == 0 || RIDING.contains(p.getUUID())) return;
        int cur = Math.max(1, CivWorld.floor(p.level()));
        if (floor == cur) return;
        if (floor == 5 || cur == 5) LiftShaft.ensureOpen(p.server);   // 1.1: на станцию — тоже кабиной, через шахту атриума
        RIDING.add(p.getUUID());
        if (p.isPassenger()) p.stopRiding();
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 4, false, false));
        String title = "ЭТАЖ " + floor + "\n" + FLOORS[floor - 1];
        ServerLevel from = (ServerLevel) p.level();
        boolean up = floor > cur;
        java.util.List<Vec3> path1 = com.pvpciv.world.Monorail.pathFrom(cur, floor > cur);
        int dur1 = cur == 1 ? 130 : 90;
        PacketDistributor.sendToPlayer(p, new Network.Cinematic(5, dur1, ""));
        from.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2f, 0.6f);
        com.pvpciv.world.Monorail.hideStation(from, com.pvpciv.world.Monorail.station(cur));   // 1.2: стоячую кабину не видно во время поездки
        LiftCabin c1 = cabin(from, path1.get(0));
        if (c1 == null) { RIDING.remove(p.getUUID()); return; }
        p.startRiding(c1, true);
        c1.start(path1, dur1, true, false, cab -> {
            p.stopRiding();
            if (!p.isAlive() || p.hasDisconnected()) { RIDING.remove(p.getUUID()); return; }
            ServerLevel to = CivWorld.byFloor(p.server, floor);
            com.pvpciv.world.Monorail.hideStation(to, com.pvpciv.world.Monorail.station(floor));
            java.util.List<Vec3> path2 = com.pvpciv.world.Monorail.pathTo(floor, floor > cur);
            Vec3 s = path2.get(0);
            p.teleportTo(to, s.x, s.y, s.z, 0, 0);
            p.resetFallDistance();
            LiftCabin c2 = cabin(to, s);
            PacketDistributor.sendToPlayer(p, new Network.Cinematic(8, 120, title));   // 0.9.7: прибытие тоже от 3 лица
            if (c2 == null) { arrive(p, floor, null); return; }
            p.startRiding(c2, true);
            Vec3 end = path2.get(path2.size() - 1);
            c2.start(path2, 110, false, true, cc -> arrive(p, floor, end));
        });
    }

    /** 1.0: лифт до станции и обратно — затемнение, гул, прибытие. */
    private static void rideF5(ServerPlayer p, int floor) {
        RIDING.add(p.getUUID());
        if (p.isPassenger()) p.stopRiding();
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 4, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 10, false, false));
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(22, "{\"cut\":\"lift\",\"len\":60}"));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2f, 0.6f);
        GameEvents.later(p.server, 60, () -> {
            RIDING.remove(p.getUUID());
            if (!p.isAlive() || p.hasDisconnected()) return;
            ServerLevel to = CivWorld.byFloor(p.server, floor);
            Vec3 a = floor == 5 ? com.pvpciv.floor.Floor5.LIFT_ARRIVE() : floor == 4 ? com.pvpciv.floor.Floor4.LIFT_ARRIVE : floor == 3 ? com.pvpciv.floor.Floor3.LIFT_ARRIVE : floor == 2 ? com.pvpciv.floor.Floor2.LIFT_ARRIVE : com.pvpciv.world.DormBuilder.LIFT_ARRIVE;
            p.teleportTo(to, a.x, a.y, a.z, floor == 5 ? 0 : p.getYRot(), 0);
            p.resetFallDistance();
            PacketDistributor.sendToPlayer(p, new Network.PhoneData(22, "{\"cut\":\"arrive\",\"len\":30}"));
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal("ЭТАЖ " + floor).withStyle(ChatFormatting.GOLD)));
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.literal(FLOORS[floor - 1]).withStyle(ChatFormatting.GRAY)));
            if (floor == 5) com.pvpciv.floor.Floor5.onArrive(p);
            if (floor == 4) com.pvpciv.floor.Floor4.onArrive(p);
            if (floor == 3) com.pvpciv.game.Library.onArrive(p);
        });
    }

    private static LiftCabin cabin(ServerLevel l, Vec3 at) {
        LiftCabin c = ModRegistry.LIFT_CABIN.get().create(l);
        if (c == null) return null;
        c.moveTo(at.x, at.y, at.z, 0, 0);
        l.addFreshEntity(c);
        return c;
    }

    private static void arrive(ServerPlayer p, int floor, Vec3 cabinEnd) {
        RIDING.remove(p.getUUID());
        p.stopRiding();
        p.resetFallDistance();
        Vec3 a = floor == 5 ? com.pvpciv.floor.Floor5.LIFT_ARRIVE() : floor == 4 ? com.pvpciv.floor.Floor4.LIFT_ARRIVE : floor == 3 ? com.pvpciv.floor.Floor3.LIFT_ARRIVE : floor == 2 ? com.pvpciv.floor.Floor2.LIFT_ARRIVE : com.pvpciv.world.DormBuilder.LIFT_ARRIVE;
        // бета 0.9.8: остаёмся там, где остановилась кабина (а не телепорт «перед лифтом», где могло зажать в блоках)
        if (cabinEnd != null) {
            ServerLevel tl = CivWorld.byFloor(p.server, floor);
            Vec3 c = cabinEnd;
            for (int i = 0; i < 4; i++) {
                BlockPos fb = BlockPos.containing(c);
                if (tl.getBlockState(fb).getCollisionShape(tl, fb).isEmpty() && tl.getBlockState(fb.above()).getCollisionShape(tl, fb.above()).isEmpty()) break;
                c = c.add(0, 1, 0);
            }
            a = c;
        }
        p.teleportTo(CivWorld.byFloor(p.server, floor), a.x, a.y, a.z, p.getYRot(), 0);
        // бета 0.9.6: после схода с кабины игрока иногда сносило «под фонарь» — через 2 тика ставим на точку ещё раз
        final var lvl = CivWorld.byFloor(p.server, floor);
        final Vec3 fa = a;
        GameEvents.later(p.server, 2, () -> { if (p.level() == lvl && p.position().distanceTo(fa) > 0.8 && p.position().distanceTo(fa) < 6) p.teleportTo(lvl, fa.x, fa.y, fa.z, p.getYRot(), 0); });
        if (floor == 3) com.pvpciv.game.Library.onArrive(p);
        if (floor == 4) com.pvpciv.floor.Floor4.onArrive(p);
        if (floor == 5) com.pvpciv.floor.Floor5.onArrive(p);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1f, 1.4f);
    }
}
