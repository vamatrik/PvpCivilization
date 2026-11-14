package com.pvpciv.world;

import com.pvpciv.data.CivData;
import com.pvpciv.floor.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Supplier;

/**
 * 0.10 — дорожные указатели: деревянный столб с фонарём и 3D-стрелками к локациям (название и расстояние на обеих сторонах).
 * Ставятся у причалов, развилок и входов в деревни, когда чанк загружен; один раз (флаг в CivData).
 */
public final class Signposts {
    private Signposts() {}

    record Dest(String name, Supplier<BlockPos> at) {}
    record Post(String id, int floor, Supplier<BlockPos> at, List<Dest> to) {}

    private static Dest d(String n, Supplier<BlockPos> p) { return new Dest(n, p); }

    private static final BlockPos DORM = DormBuilder.ATRIUM_CENTER;
    public static final List<Post> POSTS = List.of(
            // этаж 1 — у южного и северного причалов, в лагере пустоши и у заставы кряжа
            new Post("f1_dock_s", 1, () -> DormBuilder.DOCK_CENTER.offset(6, 0, -2), List.of(
                    d("Пепельная пустошь", () -> Floor1.CAMP), d("Арена", () -> Floor1.ARENA), d("Ледяной кряж", () -> FloorNorth.OUTPOST))),
            new Post("f1_dock_n", 1, () -> DormBuilder.NORTH_DOCK.offset(6, 0, 2), List.of(
                    d("Ледяной кряж", () -> FloorNorth.OUTPOST), d("Пепельная пустошь", () -> Floor1.CAMP))),
            new Post("f1_camp", 1, () -> Floor1.CAMP.offset(6, 0, 3), List.of(
                    d("Руины Старого Квартала", () -> Floor1.RUINS), d("Мёртвая роща", () -> Floor1.GROVE), d("Арена", () -> Floor1.ARENA), d("Общежитие", () -> DORM))),
            new Post("f1_camp2", 1, () -> Floor1.CAMP.offset(-4, 0, 3), List.of(
                    d("Каменные столбы", () -> Floor1.PILLARS), d("Логово Громилы", () -> Floor1.LAIR))),
            new Post("f1_outpost", 1, () -> FloorNorth.OUTPOST.offset(6, 0, -3), List.of(
                    d("Озеро", () -> FloorNorth.LAKE), d("Ледяные шпили", () -> FloorNorth.SPIRES), d("Сосняк", () -> FloorNorth.PINES), d("Общежитие", () -> DORM))),
            new Post("f1_outpost2", 1, () -> FloorNorth.OUTPOST.offset(-4, 0, -3), List.of(
                    d("Снежные руины", () -> FloorNorth.SNOW_RUINS), d("Ледяной склеп", () -> FloorNorth.CRYPT))),
            // этаж 2 — у станции лифта и на площадях
            new Post("f2_lift", 2, () -> new BlockPos(6, Floor2.Y, 10), List.of(
                    d("Неоновая площадь", () -> Floor2.NEON), d("Рынок", () -> Floor2.MARKET), d("Парк", () -> Floor2.PARK), d("Клуб «Серра»", () -> Floor2.CLUB))),
            new Post("f2_lift2", 2, () -> new BlockPos(-6, Floor2.Y, 10), List.of(
                    d("Небоскрёб «Альтаир»", () -> Floor2.TOWER), d("Спортзал", () -> Floor2.GYM))),
            new Post("f2_neon", 2, () -> Floor2.NEON.offset(4, 0, 4), List.of(d("Лифт", () -> BlockPos.ZERO.atY(Floor2.Y)), d("Клуб «Серра»", () -> Floor2.CLUB), d("Рынок", () -> Floor2.MARKET))),
            new Post("f2_market", 2, () -> Floor2.MARKET.offset(4, 0, 4), List.of(d("Лифт", () -> BlockPos.ZERO.atY(Floor2.Y)), d("Неоновая площадь", () -> Floor2.NEON), d("Парк", () -> Floor2.PARK))),
            new Post("f2_park", 2, () -> Floor2.PARK.offset(4, 0, 4), List.of(d("Лифт", () -> BlockPos.ZERO.atY(Floor2.Y)), d("Рынок", () -> Floor2.MARKET), d("Клуб «Серра»", () -> Floor2.CLUB))),
            new Post("f2_club", 2, () -> Floor2.CLUB.offset(4, 0, 4), List.of(d("Лифт", () -> BlockPos.ZERO.atY(Floor2.Y)), d("Парк", () -> Floor2.PARK), d("Неоновая площадь", () -> Floor2.NEON))),
            // этаж 3 — у выходов из сада библиотеки и на въездах в деревни
            new Post("f3_east", 3, () -> new BlockPos(Floor3.ISLAND - 4, Floor3.Y, 6), List.of(
                    d("Деревня самураев", () -> Floor3Areas.SV), d("Деревня Кумихо", () -> Floor3Areas.KV), d("Библиотека", () -> new BlockPos(0, Floor3.Y, 0)))),
            new Post("f3_west", 3, () -> new BlockPos(-Floor3.ISLAND + 4, Floor3.Y, 6), List.of(
                    d("Деревня Кумихо", () -> Floor3Areas.KV), d("Деревня самураев", () -> Floor3Areas.SV), d("Библиотека", () -> new BlockPos(0, Floor3.Y, 0)))),
            new Post("f3_sv", 3, () -> Floor3Areas.SV.offset(-Floor3Areas.SV_R + 8, 0, 7), List.of(
                    d("Тихая библиотека", () -> new BlockPos(0, Floor3.Y, 0)), d("Пруд капп", () -> Floor3Areas.KAPPA_POND))),
            new Post("f3_kv", 3, () -> Floor3Areas.KV.offset(Floor3Areas.KV_R - 8, 0, 7), List.of(
                    d("Тихая библиотека", () -> new BlockPos(0, Floor3.Y, 0)), d("Святилище Кумихо", () -> Floor3Areas.KUMIHO_SHRINE))),
            // этаж 4 — в вестибюле NaZa у шахты лифта
            new Post("f4_hub_e", 4, () -> new BlockPos(5, Floor4.Y, 7), List.of(
                    d("Оранжерея · 16–18", () -> Floor4.GREEN), d("Криолаборатория · 18–20", () -> Floor4.CRYO))),
            new Post("f4_hub_w", 4, () -> new BlockPos(-5, Floor4.Y, 7), List.of(
                    d("Реактор · 20–22", () -> Floor4.REACTOR), d("«Ковчег» · 22–24", () -> Floor4.ARK)))
    );

    /** 0.10.5: старые таблички «куда идти» у лагеря пустоши и заставы кряжа — убрать (есть 3D-указатели). */
    private static void removeOldSigns(MinecraftServer s, CivData cd) {
        if (cd.bossNext.containsKey("oldsigns_gone")) return;
        ServerLevel l = CivWorld.level(s);
        BlockPos[][] spots = {
                {Floor1.CAMP.offset(5, 1, -4), Floor1.CAMP.offset(6, 0, -4), Floor1.CAMP.offset(6, 1, -4)},
                {FloorNorth.OUTPOST.offset(5, 1, 3), FloorNorth.OUTPOST.offset(6, 0, 3), FloorNorth.OUTPOST.offset(6, 1, 3)}};
        for (BlockPos[] sp : spots) if (!l.isLoaded(sp[0])) return;
        for (BlockPos[] sp : spots) {
            if (l.getBlockState(sp[0]).getBlock() instanceof net.minecraft.world.level.block.WallSignBlock) l.setBlock(sp[0], Blocks.AIR.defaultBlockState(), 3);
            for (int i = 1; i < 3; i++) if (l.getBlockState(sp[i]).is(Blocks.STONE_BRICKS)) l.setBlock(sp[i], Blocks.AIR.defaultBlockState(), 3);
        }
        cd.bossNext.put("oldsigns_gone", 1L); cd.setDirty();
    }

    /** Раз в несколько секунд: поставить указатели, чьи чанки загружены. */
    public static void ensure(MinecraftServer s) {
        CivData cd = CivData.get(s);
        removeOldSigns(s, cd);
        for (Post p : POSTS) {
            String key = "sign4_" + p.id;
            if (cd.bossNext.containsKey(key)) continue;
            if (cd.bossNext.containsKey("sign2_" + p.id) || cd.bossNext.containsKey("sign3_" + p.id)) {   // 0.10.3: уже стоит — переписать стрелки (2 строки), столб не трогаем
                ServerLevel ol = CivWorld.byFloor(s, p.floor);
                if (ol == null) continue;
                BlockPos ob = oldBase(ol, p);
                if (ob == null) continue;
                for (var en : ol.getEntitiesOfClass(net.minecraft.world.entity.Display.class, new net.minecraft.world.phys.AABB(ob).inflate(4), x -> x.getTags().contains("signpost_" + p.id))) en.discard();
                labels(ol, ob, p);
                cd.bossNext.put(key, 1L); cd.setDirty();
                continue;
            }
            ServerLevel l = CivWorld.byFloor(s, p.floor);
            if (l == null || (p.floor == 2 && !CivWorld.isF2(l)) || (p.floor == 3 && !CivWorld.isF3(l)) || (p.floor == 4 && !CivWorld.isF4(l))) continue;
            BlockPos base = spot(l, p.at.get());
            if (base == null) continue;
            if (!l.isLoaded(base) || !l.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(base))) continue;
            build(l, base, p);
            cd.bossNext.put(key, 1L);
            cd.setDirty();
        }
    }

    /** Свободное место рядом: твёрдый блок под ногами и 4 блока воздуха. */
    private static BlockPos spot(ServerLevel l, BlockPos want) {
        if (!l.isLoaded(want)) return null;
        for (int r = 0; r <= 4; r++) for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
            for (int dy = -1; dy <= 1; dy++) {
                BlockPos b = want.offset(dx, dy, dz);
                if (!l.getBlockState(b.below()).isSolid()) continue;
                boolean free = true;
                for (int h = 0; h < 4 && free; h++) free = l.getBlockState(b.above(h)).isAir() || l.getBlockState(b.above(h)).canBeReplaced();
                if (free) return b;
            }
        }
        return null;
    }

    /** Основание уже поставленного столба: ищем по стрелке этого указателя забор с фонарём сверху. */
    private static BlockPos oldBase(ServerLevel l, Post p) {
        BlockPos want = p.at.get();
        if (!l.isLoaded(want) || !l.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(want))) return null;
        var arrows = l.getEntitiesOfClass(net.minecraft.world.entity.Display.ItemDisplay.class, new net.minecraft.world.phys.AABB(want).inflate(8), x -> x.getTags().contains("signpost_" + p.id));
        if (arrows.isEmpty()) return null;
        BlockPos a = arrows.get(0).blockPosition();
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = -4; dy <= 1; dy++) {
            BlockPos b = a.offset(dx, dy, dz);
            if (l.getBlockState(b).is(Blocks.DARK_OAK_FENCE) && l.getBlockState(b.above(3)).is(Blocks.LANTERN) && !l.getBlockState(b.below()).is(Blocks.DARK_OAK_FENCE)) return b;
        }
        return null;
    }

    private static void build(ServerLevel l, BlockPos b, Post p) {
        for (int h = 0; h < 3; h++) l.setBlock(b.above(h), Blocks.DARK_OAK_FENCE.defaultBlockState(), 3);
        l.setBlock(b.above(3), Blocks.LANTERN.defaultBlockState(), 3);
        labels(l, b, p);
    }

    private static void labels(ServerLevel l, BlockPos b, Post p) {
        for (int i = 0; i < p.to.size(); i++) {
            Dest d = p.to.get(i);
            BlockPos t = d.at.get();
            double dx = t.getX() + 0.5 - (b.getX() + 0.5), dz = t.getZ() + 0.5 - (b.getZ() + 0.5);
            double dist = Math.sqrt(dx * dx + dz * dz);
            float alpha = (float) Math.atan2(-dz, dx);   // поворот модели (+X) к цели
            double ux = dx / Math.max(1e-3, dist), uz = dz / Math.max(1e-3, dist);
            double y = b.getY() + 2.55 - i * 0.48;
            Vec3 c = new Vec3(b.getX() + 0.5 + ux * 0.86, y, b.getZ() + 0.5 + uz * 0.86);
            arrow(l, c, alpha, p.id);
            String txt = d.name + "\n" + Math.round(dist) + " м";   // 0.10.3: две строки — длинные названия влезают
            Vec3 nrm = new Vec3(uz, 0, -ux);   // нормаль к доске (повёрнутая +Z)
            Vec3 tc = c.subtract(ux * 0.2, 0, uz * 0.2);   // середина доски (без острия)
            text(l, tc.add(nrm.scale(0.1)).add(0, -0.04, 0), alpha + (float) Math.PI, txt, p.id);   // текст смотрит наружу со своей стороны доски
            text(l, tc.subtract(nrm.scale(0.1)).add(0, -0.04, 0), alpha, txt, p.id);
        }
    }

    private static CompoundTag tr(float alpha, float sx, float sy, float sz) {
        CompoundTag t = new CompoundTag();
        t.put("translation", fl(0, 0, 0));
        t.put("scale", fl(sx, sy, sz));
        t.put("left_rotation", fl(0, (float) Math.sin(alpha / 2), 0, (float) Math.cos(alpha / 2)));
        t.put("right_rotation", fl(0, 0, 0, 1));
        return t;
    }

    private static void arrow(ServerLevel l, Vec3 c, float alpha, String id) {
        var e = EntityType.ITEM_DISPLAY.create(l);
        if (e == null) return;
        e.moveTo(c.x, c.y, c.z, 0, 0);
        CompoundTag nbt = new CompoundTag();
        e.saveWithoutId(nbt);
        CompoundTag item = new CompoundTag();
        item.putString("id", "pvpciv:signpost_arrow"); item.putInt("count", 1);
        nbt.put("item", item);
        nbt.putString("item_display", "fixed");
        nbt.put("transformation", tr(alpha + (float) Math.PI, 2.0f, 1.6f, 1.6f));   // fixed-режим разворачивает предмет на 180°
        e.load(nbt);
        e.addTag("signpost"); e.addTag("signpost_" + id);
        l.addFreshEntity(e);
    }

    private static void text(ServerLevel l, Vec3 c, float alpha, String s, String id) {
        var e = EntityType.TEXT_DISPLAY.create(l);
        if (e == null) return;
        e.moveTo(c.x, c.y, c.z, 0, 0);
        CompoundTag nbt = new CompoundTag();
        e.saveWithoutId(nbt);
        nbt.putString("text", Component.Serializer.toJson(Component.literal(s).withStyle(x -> x.withColor(0xFFF0D8A0).withBold(true)), l.registryAccess()));
        nbt.putInt("background", 0);
        nbt.putBoolean("shadow", true);
        nbt.putInt("line_width", 200);
        nbt.put("transformation", tr(alpha, 0.33f, 0.33f, 0.33f));
        e.load(nbt);
        e.addTag("signpost"); e.addTag("signpost_" + id);
        l.addFreshEntity(e);
    }

    private static ListTag fl(float... v) { ListTag t = new ListTag(); for (float f : v) t.add(FloatTag.valueOf(f)); return t; }
}
