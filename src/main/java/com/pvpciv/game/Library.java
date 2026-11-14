package com.pvpciv.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pvpciv.combat.Mobs;
import com.pvpciv.data.StoryData;
import com.pvpciv.entity.CityNpcEntity;
import com.pvpciv.floor.BossManager;
import com.pvpciv.floor.Floor1;
import com.pvpciv.floor.Floor2;
import com.pvpciv.floor.Floor3;
import com.pvpciv.floor.SpawnZones;
import com.pvpciv.floor.Zones;
import com.pvpciv.inv.Bag;
import com.pvpciv.item.CivArmorItem;
import com.pvpciv.item.CivWeaponItem;
import com.pvpciv.loot.LootTables;
import com.pvpciv.loot.Rarity;
import com.pvpciv.net.Network;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModRegistry;
import com.pvpciv.util.Ru;
import com.pvpciv.world.CivWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.pvpciv.game.DialogueManager.opt;

/**
 * Этаж 3 — Хранитель Сёдо и Альманах (0.9.0).
 * Квест: три утерянных тома на трёх этажах (пюпитры: руины Пустоши, логово контрабандиста, архив библиотеки).
 * Награда — Альманах: бестиарий (открывается после первого убийства вида), книги оружия и брони.
 */
public final class Library {
    private Library() {}
    public static final int C = 0xC8A86A;

    // ------------------------------------------------------------------ тома
    public record Tome(int bit, String name, String where, java.util.function.Supplier<Item> item, int floor) {}
    public static final Tome[] TOMES = {
            new Tome(1, "«Пепел и кости»", "Этаж 1, руины Старого Квартала — пюпитр у площади с колодцем.", () -> ModRegistry.TOME_ASH.get(), 1),
            new Tome(2, "«Песни неона»", "Этаж 2, канализация — у контрабандиста за лавой.", () -> ModRegistry.TOME_NEON.get(), 2),
            new Tome(4, "«О тишине»", "Здесь, на втором этаже библиотеки, — в западном архиве, за стеллажами.", () -> ModRegistry.TOME_SILENCE.get(), 3)};

    public static BlockPos spot(MinecraftServer s, Tome t) {
        return switch (t.floor) {
            case 1 -> { BlockPos b = Floor1.RUINS.offset(3, 0, -6); ServerLevel l = CivWorld.level(s); yield new BlockPos(b.getX(), l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, b.getX(), b.getZ()), b.getZ()); }
            case 2 -> Floor2.den().offset(1, 0, 1);
            default -> Floor3.BOOK3;
        };
    }

    /** Ставит пюпитры (при стройке мира). На этаже 1 — сверху земли, один раз (координата запоминается в CivData). */
    public static void placeBooks(MinecraftServer s) {
        var d = com.pvpciv.data.CivData.get(s);
        BlockPos b1 = spot(s, TOMES[0]);
        CivWorld.level(s).setBlock(b1, Blocks.LECTERN.defaultBlockState(), 3);
        d.bossNext.put("book1", b1.asLong());
        d.setDirty();
        CivWorld.f2(s).setBlock(spot(s, TOMES[1]), Blocks.LECTERN.defaultBlockState(), 3);
    }

    private static BlockPos pos(MinecraftServer s, Tome t) {
        // BlockPos.asLong() бывает отрицательным (отрицательные X/Z) — проверяем наличие ключа, а не знак
        if (t.floor == 1) { var m = com.pvpciv.data.CivData.get(s).bossNext; if (m.containsKey("book1")) return BlockPos.of(m.get("book1")); }
        return spot(s, t);
    }

    public static void register(IEventBus bus) {
        bus.addListener(Library::onUseBlock);
    }

    private static void onUseBlock(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        int fl = CivWorld.floor(p.level());
        for (Tome t : TOMES) {
            if (t.floor != fl || !pos(p.server, t).equals(e.getPos())) continue;
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS);
            StoryData d = Progress.d(p);
            if (d.libQuest != 1) { p.displayClientMessage(Component.literal("На пюпитре — пыльные свитки. Ничего интересного… пока.").withStyle(ChatFormatting.GRAY), true); return; }
            if ((d.libFound & t.bit) != 0) { p.displayClientMessage(Component.literal("Том " + t.name + " ты уже забрал.").withStyle(ChatFormatting.GRAY), true); return; }
            d.libFound |= t.bit;
            p.setData(ModRegistry.STORY.get(), d);
            Bag.give(p, new ItemStack(t.item.get()));
            Story.title(p, "Найден том", t.name, C);
            p.serverLevel().playSound(null, e.getPos(), SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1f, 0.8f);
            p.serverLevel().sendParticles(ParticleTypes.ENCHANT, e.getPos().getX() + 0.5, e.getPos().getY() + 1.3, e.getPos().getZ() + 0.5, 40, 0.3, 0.5, 0.3, 0.6);
            return;
        }
    }

    /** Раз в 2 с: подсветка пюпитров для тех, кто ищет тома (видно только им). */
    public static void tick(ServerPlayer p) {
        StoryData d = Progress.d(p);
        if (d.libQuest != 1) return;
        int fl = CivWorld.floor(p.level());
        for (Tome t : TOMES) {
            if (t.floor != fl || (d.libFound & t.bit) != 0) continue;
            BlockPos b = pos(p.server, t);
            if (p.blockPosition().distSqr(b) < 48 * 48)
                p.serverLevel().sendParticles(p, ParticleTypes.END_ROD, true, b.getX() + 0.5, b.getY() + 1.4, b.getZ() + 0.5, 6, 0.15, 0.5, 0.15, 0.01);
        }
    }

    // ------------------------------------------------------------------ NPC и прибытие
    /** 0.9.7: пьедестал + лежащая 3D-книга на каждом месте тома (бета: «на этаже 1 книги нет, только частицы»). Самовосстанавливается. */
    private static void ensureTomes(MinecraftServer s) {
        for (Tome t : TOMES) {
            ServerLevel l = CivWorld.byFloor(s, t.floor);
            BlockPos b = pos(s, t);
            var cd = com.pvpciv.data.CivData.get(s);
            if (t.floor == 1 && !cd.bossNext.containsKey("book1")) {
                if (!l.isLoaded(b)) continue;
                cd.bossNext.put("book1", b.asLong()); cd.setDirty();
            }
            if (t.floor == 1 && !cd.bossNext.containsKey("book1fix2")) {
                // 0.9.7: пьедестал рос столбом вверх — сносим столб до самого нижнего блока
                if (!l.isLoaded(b) || !l.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(b))) continue;
                BlockPos bot = b;
                while (l.getBlockState(bot.below()).is(Blocks.CHISELED_STONE_BRICKS) && b.getY() - bot.getY() < 400) bot = bot.below();
                for (BlockPos c = bot.above(); c.getY() <= b.getY() + 1; c = c.above())
                    if (l.getBlockState(c).is(Blocks.CHISELED_STONE_BRICKS)) l.setBlock(c, Blocks.AIR.defaultBlockState(), 3);
                for (var e : l.getEntitiesOfClass(net.minecraft.world.entity.Display.ItemDisplay.class,
                        new net.minecraft.world.phys.AABB(bot).inflate(2, 0, 2).expandTowards(0, b.getY() - bot.getY() + 4, 0), e -> e.getTags().contains("tome_" + t.bit)))
                    e.discard();
                if (bot.getY() < com.pvpciv.floor.Floor1.Y - 12) {   // мусорная координата (ниже руин) — ставим заново на землю
                    if (l.getBlockState(bot).is(Blocks.CHISELED_STONE_BRICKS)) l.setBlock(bot, Blocks.AIR.defaultBlockState(), 3);
                    bot = spot(s, t);
                }
                cd.bossNext.put("book1", bot.asLong()); cd.bossNext.put("book1fix2", 1L); cd.setDirty();
                b = bot;
            }
            if (!l.isLoaded(b) || !l.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(b))) continue;
            var st = l.getBlockState(b);
            net.minecraft.world.level.block.Block ped = t.floor == 1 ? Blocks.CHISELED_STONE_BRICKS : t.floor == 2 ? Blocks.CHISELED_POLISHED_BLACKSTONE : Blocks.CHISELED_QUARTZ_BLOCK;
            if (!st.is(ped)) l.setBlock(b, ped.defaultBlockState(), 3);
            if (!l.getBlockState(b.above()).isAir()) l.setBlock(b.above(), Blocks.AIR.defaultBlockState(), 3);
            String tag = "tome_" + t.bit;
            var list = l.getEntitiesOfClass(net.minecraft.world.entity.Display.ItemDisplay.class, new net.minecraft.world.phys.AABB(b).inflate(2), e -> e.getTags().contains(tag));
            for (int i = 1; i < list.size(); i++) list.get(i).discard();
            if (!list.isEmpty()) continue;
            var d = net.minecraft.world.entity.EntityType.ITEM_DISPLAY.create(l);
            if (d == null) continue;
            d.moveTo(b.getX() + 0.5, b.getY() + 1.5, b.getZ() + 0.5, 30, 0);   // модель предмета центрирована: низ книги = y + 1.0
            net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
            d.saveWithoutId(nbt);
            net.minecraft.nbt.CompoundTag item = new net.minecraft.nbt.CompoundTag();
            item.putString("id", BuiltInRegistries.ITEM.getKey(t.item.get()).toString()); item.putInt("count", 1);
            nbt.put("item", item);
            nbt.putString("item_display", "fixed");
            nbt.putFloat("view_range", 0.6f);
            d.load(nbt);
            d.addTag(tag);
            l.addFreshEntity(d);
        }
    }

    public static void ensure(MinecraftServer s) {
        ensureTomes(s);
        ServerLevel l = CivWorld.f3(s);
        if (!CivWorld.isF3(l)) return;
        Floor2.ensureNpc(l, ModRegistry.LIBRARIAN.get(), Floor3.LIBRARIAN, 180);
        Floor2.ensureNpc(l, ModRegistry.ASSASSIN.get(), Floor3.ASSASSIN, 90);
        Floor2.ensureNpc(l, ModRegistry.CHEF.get(), com.pvpciv.floor.Floor3Areas.CHEF, 0);
    }

    public static void onArrive(ServerPlayer p) {
        StoryData d = Progress.d(p);
        if (d.gates.contains("f3_seen")) return;
        d.gates.putInt("f3_seen", 1);
        p.setData(ModRegistry.STORY.get(), d);
        p.sendSystemMessage(Component.literal("✦ Этаж 3 — Тихая библиотека. Здесь не дерутся: в зале тишина. Поговори с Хранителем Сёдо у стойки.").withStyle(ChatFormatting.GOLD));
        com.pvpciv.progress.Titles.grant(p, "reader");
    }

    // ------------------------------------------------------------------ диалог
    public static void talk(ServerPlayer p, com.pvpciv.entity.AnimeNpc npc) {
        StoryData d = Progress.d(p);
        String n = npc.kind.name;
        DialogueManager.Dialogue dl = new DialogueManager.Dialogue("a");
        int carried = 0;
        for (Tome t : TOMES) if ((d.libGiven & t.bit) == 0 && Bag.has(p, s -> s.is(t.item.get()))) carried |= t.bit;
        if (d.libQuest == 0) {
            dl.node("a", n, C, "Тсс… Здесь говорят шёпотом, гость. Я — Сёдо, хранитель свитков. Эта библиотека помнит каждого, кто прошёл Шпиль: каждого зверя, каждый клинок.",
                            opt("Что это за место?", "b"), opt("[Уйти]", null))
              .node("b", n, C, "Здесь записано всё о мирах внизу. Было записано. Три тома унесли — ветер, воры, крысы… Без них Альманах неполон, а без Альманаха ты бродишь вслепую.",
                            opt("Я найду их", "c"), opt("Позже", null))
              .node("c", n, C, "«Пепел и кости» — на первом этаже, в руинах Старого Квартала. «Песни неона» утащил контрабандист из канализации второго этажа. "
                            + "А «О тишине»… стыдно сказать, он где-то здесь, на втором этаже, в западном архиве. Пюпитры будут светиться для тебя.", pp -> {
                  StoryData dd = Progress.d(pp);
                  dd.libQuest = 1;
                  pp.setData(ModRegistry.STORY.get(), dd);
                  pp.sendSystemMessage(Component.literal("Квест «Утерянные тома»: найди три тома и принеси Сёдо.").withStyle(ChatFormatting.GOLD));
              }, opt("[Записать и уйти]", null));
        } else if (d.libQuest == 1 && carried != 0) {
            final int give = carried;
            dl.node("a", n, C, "Это… да! Я узнаю переплёт. Давай сюда, осторожно — бумага старше тебя.", pp -> turnIn(pp, give), opt("Держите", null));
        } else if (d.libQuest == 1) {
            StringBuilder left = new StringBuilder();
            for (Tome t : TOMES) if ((d.libGiven & t.bit) == 0) left.append("\n• ").append(t.name).append(" — ").append(t.where);
            dl.node("a", n, C, "Ещё ищешь? Осталось:" + left, opt("Иду", null));
        } else {
            String[] hi = {"Альманах при тебе? Листай — он сам дописывается, когда ты убиваешь кого-то нового.", "Книга оружия знает, с кого что падает. Не благодари.",
                    "На втором этаже сидит Юки. Будь вежлив. Она тоже умеет читать… людей.", "Тишина — лучший учитель. После меня, конечно."};
            dl.node("a", n, C, hi[p.getRandom().nextInt(hi.length)], opt("[Уйти]", null));
            if (!Bag.has(p, s -> s.is(ModRegistry.ALMANAC.get())))
                dl.node("a", n, C, "Потерял Альманах? Ох… Держи копию. Последнюю! (Он всегда так говорит.)", pp -> Bag.give(pp, new ItemStack(ModRegistry.ALMANAC.get())), opt("Спасибо", null));
        }
        DialogueManager.open(p, npc, dl);
    }

    private static void turnIn(ServerPlayer p, int bits) {
        StoryData d = Progress.d(p);
        for (Tome t : TOMES) if ((bits & t.bit) != 0) { Bag.take(p, s -> s.is(t.item.get()), 1); d.libGiven |= t.bit; }
        Progress.addMoney(p, 150L * Integer.bitCount(bits), true);
        if (d.libGiven == 7) {
            d.libQuest = 2;
            p.setData(ModRegistry.STORY.get(), d);
            Bag.give(p, new ItemStack(ModRegistry.ALMANAC.get()));
            Progress.addXp(p, Progress.xpFor(d.level, 11, 25));
            Story.title(p, "Альманах", "Бестиарий, оружие и броня — в одной книге", C);
            Story.say(p, "Хранитель Сёдо", C, "Все три. Ты не представляешь, что ты сделал. Вот — Альманах. Он пишет сам: убей нового зверя — появится страница. ПКМ, чтобы открыть.");
        } else {
            p.setData(ModRegistry.STORY.get(), d);
            Story.say(p, "Хранитель Сёдо", C, "Осталось " + (3 - Integer.bitCount(d.libGiven)) + ". Не торопись. Книги не любят спешки.");
        }
    }

    // ------------------------------------------------------------------ убийства
    /** Повар Харуки: приветствие и лавка. */
    public static void chef(ServerPlayer p, com.pvpciv.entity.AnimeNpc npc) {
        String[] hi = {"Ирассяимасэ! Рамен горячий, суши свежие, бэнто — для героев.", "Каппы опять воровали огурцы. Купи что-нибудь — мне на новый забор.",
                "Императорский бэнто — один в завоз. Сила как у золотого яблока, только вкуснее."};
        Story.say(p, npc.kind.name, npc.kind.color, hi[p.getRandom().nextInt(hi.length)]);
        Shop.VENDOR.put(p.getUUID(), "chef");
        p.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new com.pvpciv.menu.TraderMenu(id, inv), Component.literal("Повар Харуки")));
        Shop.sendOffers(p);
    }

    public static void onKill(ServerPlayer p, Mob mob, boolean elite) {
        Mobs.Kind k = Mobs.Kind.of(mob.getType());
        if (k == null) return;
        StoryData d = Progress.d(p);
        if (elite) d.bestiary.putInt(k.name() + "_e", d.bestiary.getInt(k.name() + "_e") + 1);   // сами убийства считает Vampire.onKill
        p.setData(ModRegistry.STORY.get(), d);
        if (d.kills.getInt(k.name()) == 1 && d.libQuest == 2)
            p.displayClientMessage(Component.literal("✎ Альманах: новая страница — " + k.title).withStyle(ChatFormatting.GOLD), true);
    }

    public static void onKillBoss(ServerPlayer p, String id) {
        StoryData d = Progress.d(p);
        d.bestiary.putInt("boss_" + id, d.bestiary.getInt("boss_" + id) + 1);
        p.setData(ModRegistry.STORY.get(), d);
    }

    // ------------------------------------------------------------------ Альманах
    private static final Map<String, String> TABLE_NAMES = new LinkedHashMap<>();
    static {
        TABLE_NAMES.put("floor1_mob", "мобы Пепельной пустоши"); TABLE_NAMES.put("floor1_elite", "элитки Пустоши"); TABLE_NAMES.put("floor1_stash", "тайники Пустоши");
        TABLE_NAMES.put("floor1_dungeon", "подземелье Пустоши"); TABLE_NAMES.put("floor1_dungeon_clear", "зачистка подземелья");
        TABLE_NAMES.put("floor1_boss_personal", "Гнилой Громила"); TABLE_NAMES.put("floor1_boss_ground", "Громила (на земле)");
        TABLE_NAMES.put("floor1b_mob", "мобы Инеевого Кряжа"); TABLE_NAMES.put("floor1b_elite", "элитки Кряжа"); TABLE_NAMES.put("floor1b_stash", "тайники Кряжа"); TABLE_NAMES.put("floor1b_crypt", "Ледяной Склеп");
        TABLE_NAMES.put("floor1b_boss_personal", "Хранительница Склепа"); TABLE_NAMES.put("floor1b_boss_ground", "Хранительница (на земле)");
        TABLE_NAMES.put("floor2_mob", "мобы Неонового города"); TABLE_NAMES.put("floor2_elite", "элитки города"); TABLE_NAMES.put("floor2_stash", "тайники города");
        TABLE_NAMES.put("floor2_boss_personal", "Дон Витторио"); TABLE_NAMES.put("floor2_boss_ground", "Дон (на земле)");
        TABLE_NAMES.put("floor2b_mob", "мобы канализации"); TABLE_NAMES.put("floor2b_elite", "элитки канализации"); TABLE_NAMES.put("floor2b_stash", "тайники канализации");
        TABLE_NAMES.put("floor2b_boss_personal", "Крысиный король"); TABLE_NAMES.put("floor2b_boss_ground", "Король (на земле)");
        TABLE_NAMES.put("floor3_mob", "мобы деревни самураев"); TABLE_NAMES.put("floor3_elite", "элитки деревни"); TABLE_NAMES.put("floor3_stash", "тайники деревни самураев");
        TABLE_NAMES.put("floor3_boss_personal", "Братья-каппы"); TABLE_NAMES.put("floor3b_mob", "мобы деревни Кумихо"); TABLE_NAMES.put("floor3b_elite", "элитки Кумихо");
        TABLE_NAMES.put("floor4_mob", "мобы НТЦ NaZa"); TABLE_NAMES.put("floor4_elite", "элитки NaZa"); TABLE_NAMES.put("floor4b_mob", "мобы «Ковчега»"); TABLE_NAMES.put("floor4b_elite", "элитки «Ковчега»");
        TABLE_NAMES.put("floor4_stash", "тайники NaZa"); TABLE_NAMES.put("floor4_boss_personal", "Ноэль и Лиана"); TABLE_NAMES.put("floor4_boss_ground", "Ноэль (на земле)");
        TABLE_NAMES.put("floor3b_stash", "тайники деревни Кумихо"); TABLE_NAMES.put("floor3b_boss_personal", "Кумихо");
    }
    private static String tableName(String t) { return TABLE_NAMES.getOrDefault(t, t); }

    private static String pct(double c) { return c >= 0.995 ? "100%" : c >= 0.1 ? Math.round(c * 100) + "%" : String.format("%.1f%%", c * 100).replace('.', ','); }

    /** Строки дропа таблицы: «предмет — шанс (кол-во)». */
    private static JsonArray drops(String table) {
        JsonArray out = new JsonArray();
        JsonArray entries = LootTables.tables().get(table);
        if (entries == null) return out;
        for (JsonElement el : entries) {
            JsonObject e = el.getAsJsonObject();
            double ch = e.has("chance") ? e.get("chance").getAsDouble() : 1.0;
            JsonObject o = new JsonObject();
            if (e.has("weapons") || e.has("gear")) {
                JsonArray ws = e.getAsJsonArray(e.has("weapons") ? "weapons" : "gear");
                o.addProperty("n", (e.has("weapons") ? "Оружие" : "Броня") + " (одно из " + ws.size() + ")" + (e.has("min_rarity") ? ", от «" + Rarity.valueOf(e.get("min_rarity").getAsString()).title + "»" : ""));
                o.add("ids", ws.deepCopy());   // 0.10.5: иконки (по очереди)
            } else {
                Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(e.get("item").getAsString()));
                int mn = e.has("min") ? e.get("min").getAsInt() : 1, mx = e.has("max") ? e.get("max").getAsInt() : mn;
                o.addProperty("n", Ru.name(it) + (mx > 1 ? " ×" + (mn == mx ? mn + "" : mn + "–" + mx) : ""));
                o.addProperty("id", e.get("item").getAsString());
                if (mx > 1) o.addProperty("cnt", mn == mx ? mn + "" : mn + "–" + mx);
            }
            o.addProperty("c", pct(ch));
            out.add(o);
        }
        return out;
    }

    /** Откуда падает предмет: «таблица — шанс». */
    private static JsonArray sources(String id) {
        JsonArray out = new JsonArray();
        for (var t : LootTables.tables().entrySet()) for (JsonElement el : t.getValue()) {
            JsonObject e = el.getAsJsonObject();
            String key = e.has("weapons") ? "weapons" : e.has("gear") ? "gear" : null;
            if (key == null) continue;
            JsonArray ws = e.getAsJsonArray(key);
            for (JsonElement w : ws) if (w.getAsString().equals(id)) {
                double ch = (e.has("chance") ? e.get("chance").getAsDouble() : 1.0) / ws.size();
                JsonObject o = new JsonObject();
                o.addProperty("n", tableName(t.getKey()));
                o.addProperty("c", pct(ch));
                out.add(o);
            }
        }
        return out;
    }

    private static String where(Mobs.Kind k) {
        java.util.LinkedHashSet<String> s = new java.util.LinkedHashSet<>();
        for (SpawnZones.Zone z : SpawnZones.zones()) for (Mobs.Kind kk : z.kinds()) if (kk == k) {
            Vec3 c = Vec3.atCenterOf(z.center());
            String name = z.id().startsWith("f3") ? Floor3.zone(c).name() : z.f2() ? Floor2.zone(c).name() : Zones.at(c).name();
            s.add(name.replace("Этаж 1 · ", "").replace("Этаж 2 · ", "").replace("Этаж 3 · ", "") + " (ур. " + z.minLvl() + "–" + z.maxLvl() + ")");
        }
        return s.isEmpty() ? "призыв/особые места" : String.join(", ", s);
    }

    /** Кнопка «Альманах» в герое: открыта после квеста Сёдо (Альманах получен). */
    public static void almanacFromHero(ServerPlayer p) {
        if (Progress.d(p).libQuest < 2) {
            p.displayClientMessage(Component.literal("Альманах откроется, когда Хранитель Сёдо вручит его тебе (этаж 3, квест «Тихая библиотека»).").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        openAlmanac(p);
    }

    public static void openAlmanac(ServerPlayer p) {
        StoryData d = Progress.d(p);
        JsonObject j = new JsonObject();
        JsonArray mobs = new JsonArray();
        for (Mobs.Kind k : Mobs.Kind.values()) {
            JsonObject o = new JsonObject();
            int n = d.kills.getInt(k.name());
            o.addProperty("known", n > 0);
            o.addProperty("name", n > 0 ? k.title : "???");
            o.addProperty("kills", n);
            o.addProperty("elite", d.bestiary.getInt(k.name() + "_e"));
            o.addProperty("id", BuiltInRegistries.ENTITY_TYPE.getKey(k.type()).toString());
            o.addProperty("loc", k.loot.startsWith("floor1b") ? "Этаж 1 · Ледяной кряж" : k.loot.startsWith("floor1") ? "Этаж 1 · Пепельная пустошь"
                    : k.loot.startsWith("floor2b") ? "Этаж 2 · Канализация" : k.loot.startsWith("floor2") ? "Этаж 2 · Неоновый город"
                    : k.loot.startsWith("floor3b") ? "Этаж 3 · Деревня Кумихо" : "Этаж 3 · Деревня самураев");
            if (n > 0) {
                o.addProperty("lvl", k.defLevel);
                o.addProperty("where", where(k));
                o.addProperty("hp", Math.round(Mobs.hp(k.defLevel)));
                o.addProperty("dmg", String.format("%.1f", Mobs.dmg(k.defLevel)));
                o.add("drops", drops(k.loot));
                o.add("edrops", drops(k.loot.replace("_mob", "_elite")));
            }
            mobs.add(o);
        }
        for (BossManager.Def b : BossManager.DEFS) {
            JsonObject o = new JsonObject();
            int n = d.bestiary.getInt("boss_" + b.id());
            o.addProperty("known", n > 0);
            o.addProperty("boss", true);
            o.addProperty("id", BuiltInRegistries.ENTITY_TYPE.getKey(b.type().get()).toString());
            o.addProperty("loc", "Боссы");
            o.addProperty("name", n > 0 ? "☠ " + b.name() : "☠ ???");
            o.addProperty("kills", n);
            if (n > 0) {
                o.addProperty("lvl", b.level());
                o.addProperty("where", b.where());
                o.addProperty("hp", Math.round(Mobs.hp(b.level()) * b.hpMul()));
                o.addProperty("dmg", String.format("%.1f", Mobs.dmg(b.level()) * b.dmgMul()));
                o.add("drops", drops(b.personal()));
                o.add("edrops", drops(b.ground()));
            }
            mobs.add(o);
        }
        j.add("mobs", mobs);
        JsonArray weps = new JsonArray(), arms = new JsonArray();
        for (Item it : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(it);
            if (!id.getNamespace().equals("pvpciv")) continue;
            if (it instanceof CivWeaponItem w) {
                JsonObject o = new JsonObject();
                o.addProperty("name", Ru.name(it));
                o.addProperty("id", id.toString());
                o.addProperty("rar", w.minRarity.ordinal());
                o.addProperty("tier", w.tier);
                o.addProperty("cls", w.cls.title);
                o.addProperty("min", w.minRarity.title);
                o.addProperty("dmg", String.format("%.1f", CivWeaponItem.damage(w.tier, w.cls, w.minRarity, 0)));
                o.addProperty("dmg10", String.format("%.1f", CivWeaponItem.damage(w.tier, w.cls, w.minRarity, 10)));
                o.addProperty("lore", w.lore);
                JsonArray src = sources(id.toString());
                if (it == ModRegistry.SAMURAI_KATANA.get()) { JsonObject s = new JsonObject(); s.addProperty("n", "контрабандист Шнырь"); s.addProperty("c", "4800 ✦"); src.add(s); }
                o.add("src", src);
                weps.add(o);
            } else if (it instanceof CivArmorItem a) {
                JsonObject o = new JsonObject();
                ItemStack st = new ItemStack(it);
                o.addProperty("name", Ru.name(it));
                o.addProperty("id", id.toString());
                o.addProperty("rar", 0);
                o.addProperty("set", a.set);
                o.addProperty("tier", a.tier);
                o.addProperty("armor", String.format("%.1f", a.armor(st)));
                o.addProperty("hp", String.format("%.1f", a.health(st)));
                o.addProperty("core", "в центре алтаря — " + (a.tier < 2 ? "Медальон инея" : a.tier < 3 ? "Фильтр противогаза" : "Нефритовая печать") + ", по краям — звёзды (ядро подходит только к броне своего этажа и ниже)");
                o.addProperty("junk", "любой хлам, родной ×1,5 — " + (a.set.startsWith("Инеевый") ? "Инеевый осколок" : a.set.contains("ликвидатора") ? "Обрезки резины" : a.set.startsWith("Самурай") ? "Обрезки шёлка" : a.set.startsWith("Лисий") ? "Лисий мех" : "—"));
                o.add("src", sources(id.toString()));
                arms.add(o);
            }
        }
        j.add("weapons", weps);
        j.add("armor", arms);
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(9, j.toString()));
    }
}
