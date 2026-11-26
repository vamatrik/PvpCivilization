package com.pvpciv.world;

import com.pvpciv.data.CivData;
import com.pvpciv.floor.Floor1;
import com.pvpciv.floor.FloorNorth;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;

/** Полная сборка мира 1 в его измерении: общежитие, подвал, обе локации этажа 1, трассы. */
public final class WorldBuild {
    public static final int VERSION = 48;

    /** Мелкие правки уже построенного мира (без полной перестройки). */
    public static void patch(MinecraftServer server, int from) {
        ServerLevel lvl = CivWorld.level(server);
        if (from < 18) { buildAll(server, true); return; }
        if (from < 19) {   // 0.7: алтарь инфузии в подвале, стол брони и домашний сундук в квартирах, табличка склепа
            CivData d1 = CivData.get(server);
            Basement.placeAltar(lvl);
            DormBuilder.placeHomeStations(lvl, d1);
            FloorNorth.cryptSign(lvl);
        }
        if (from < 20) { Floor1.lairSign(lvl); FloorNorth.cryptSign(lvl); }
        if (from < 21 && from >= 19) Basement.placeAltar(lvl);
        if (from < 22 && from >= 19) DormBuilder.placeHomeStations(lvl, CivData.get(server));   // 0.8: витрина трофеев
        if (from < 24) { RoofBuilder.build(lvl); com.pvpciv.floor.Floor2.build(CivWorld.f2(server)); }   // 0.8: Небесный лифт и этаж 2 (0.8.2 — 3D-кабина и монорельс)   // 0.7.1: пьедесталы с блок-сущностями (предметы кладутся руками)   // 0.7.1: Громила раз в 20 мин, Хранительница раз в 40   // 0.6.8: соединённые решётки/заборы, повороты станций
        if (from < 33 && from >= 24) com.pvpciv.floor.Floor2.sealSewer(CivWorld.f2(server));   // 0.9.7
        if (from < 34) com.pvpciv.floor.Floor2.fillAboveSewer(CivWorld.f2(server));   // 0.9.9: прослойка над канализацией
        if (from >= 28 && from < 32) com.pvpciv.floor.Floor3.fixStairs(CivWorld.f3(server));
        if (from < 31 && from >= 28) com.pvpciv.floor.Floor3.build(CivWorld.f3(server));   // 0.9.0: деревни самураев и Кумихо, тропа и путь
        if (from < 28) { com.pvpciv.floor.Floor3.build(CivWorld.f3(server)); com.pvpciv.game.Library.placeBooks(server); }   // 0.9.0: этаж 3 — Тихая библиотека
        if (from < 35) com.pvpciv.floor.Floor3Areas.fixPlants(CivWorld.f3(server));   // 0.9.9: осыпавшиеся цветы и грибы
        if (from < 41) com.pvpciv.floor.Floor4.build(CivWorld.f4(server));
        if (from < 42) com.pvpciv.floor.Floor5.build(CivWorld.f5(server));   // 1.0: шлюз, пульт лифта, пояс астероидов   // 0.12: станция «Орбита»   // 0.11: этаж 4 — НТЦ «NaZa» (0.11.1: пол лифта, галереи впритык)
        if (from < 48 && from >= 20) com.pvpciv.floor.Floor3Areas.placeDecoy(CivWorld.f3(server));   // 1.2: обманка на пути к Кумихо
        if (from < 47 && from >= 41) com.pvpciv.game.Rocket.rebuild(server);   // 1.2: лестница и кабина с пультом в ракетах
        if (from < 46 && from >= 42) com.pvpciv.game.StationTeleport.decommission(CivWorld.f5(server));   // 1.2: одна система телепортов на станции
        if (from < 45 && from >= 41) com.pvpciv.floor.Floor4.fixReactorRings(CivWorld.f4(server));   // 1.2
        if (from < 44 && from >= 40) com.pvpciv.floor.Belt.rebuildField(CivWorld.f5(server));   // 1.1.1: поле астероидов реже
        if (from < 43 && from >= 19) com.pvpciv.game.Showcases.build(lvl, CivData.get(server));   // 1.1: витрины трофеев по этажам
        if (from < 43 && from >= 42) {   // 1.1: ритуальный зал, телепорты и пульты станции
            com.pvpciv.game.Ritual.build(CivWorld.f5(server));
        }
        if (from < 43 && from >= 18) com.pvpciv.game.StationTeleport.convertConsoles(server);
        if (from < 27 && from >= 24) { RoofBuilder.collar(lvl); com.pvpciv.floor.Floor2.fixDen(CivWorld.f2(server)); }   // 0.8.8: рамка проёма монорельса, негорючий пол у контрабандиста
        if (from < 26 && from >= 18) DormBuilder.lunaPouf(lvl);   // 0.8.6: пуф-укрытие в комнате Луны
        if (from < 25 && from >= 24) com.pvpciv.floor.Floor2.build(CivWorld.f2(server));   // 0.8.5: канализация (торцы туннелей, лава, лестницы люков), выход на крышу «Альтаира»
        if (from < 17) { CivData d0 = CivData.get(server); DormBuilder.placeForges(lvl, d0); DormBuilder.placeDismantlers(lvl, d0); }   // 0.6.7: повороты станций, сакура   // 0.6.4: лёд не тает, бортик первого поворота   // 0.7: гравий, фонари, арки трассы, бортики
        if (from < 11) Basement.rebuildThrone(lvl);
        CivData d = CivData.get(server);
        d.version = VERSION;
        d.setDirty();
    }

    public static void buildAll(MinecraftServer server, boolean clear) {
        var log = com.mojang.logging.LogUtils.getLogger();
        ServerLevel lvl = CivWorld.level(server);
        CivData data = CivData.get(server);
        // владельцы квартир сохраняются по положению двери
        Map<BlockPos, CivData.Apartment> old = new HashMap<>();
        for (CivData.Apartment a : data.apartments) old.put(a.door, a);
        long t = System.currentTimeMillis();
        DormBuilder.build(lvl, data);
        for (CivData.Apartment a : data.apartments) {
            CivData.Apartment o = old.get(a.door);
            if (o != null && o.owner != null && a.npc.isEmpty()) {
                a.owner = o.owner; a.ownerName = o.ownerName;
                DormBuilder.updateApartmentSign(lvl, a);
            }
        }
        Basement.build(lvl);
        log.info("[PvpCiv] Dormitory + basement: {} ms", System.currentTimeMillis() - t);
        t = System.currentTimeMillis();
        Floor1.build(lvl, clear);
        log.info("[PvpCiv] Floor 1 / Ash Wastes: {} ms", System.currentTimeMillis() - t);
        t = System.currentTimeMillis();
        FloorNorth.build(lvl, clear);
        IceTrack.build(lvl);
        log.info("[PvpCiv] Floor 1 / Rime Ridge + Ice Track: {} ms", System.currentTimeMillis() - t);
        DormBuilder.placeForges(lvl, data);
        DormBuilder.placeDismantlers(lvl, data);
        DormBuilder.placeHomeStations(lvl, data);
        RoofBuilder.build(lvl);
        com.pvpciv.floor.Floor2.build(CivWorld.f2(server));
        com.pvpciv.floor.Floor2.sealSewer(CivWorld.f2(server));
        com.pvpciv.floor.Floor2.fillAboveSewer(CivWorld.f2(server));
        com.pvpciv.floor.Floor3.build(CivWorld.f3(server));
        com.pvpciv.floor.Floor3Areas.fixPlants(CivWorld.f3(server));
        com.pvpciv.floor.Floor4.build(CivWorld.f4(server));
        com.pvpciv.floor.Floor5.build(CivWorld.f5(server));
        com.pvpciv.game.Library.placeBooks(server);
        com.pvpciv.game.Showcases.build(lvl, data);
        // при стройке отрываются цветы/свечи/ковры — убрать выпавшие предметы, чтобы не лагало
        java.util.List<net.minecraft.world.entity.Entity> drops = new java.util.ArrayList<>();
        for (var e : lvl.getAllEntities()) if (e instanceof net.minecraft.world.entity.item.ItemEntity) drops.add(e);
        drops.forEach(net.minecraft.world.entity.Entity::discard);
        log.info("[PvpCiv] Removed {} dropped items after build", drops.size());
        data.version = VERSION;
        data.built = true;
        data.setDirty();
    }
}
