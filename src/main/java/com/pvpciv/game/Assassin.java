package com.pvpciv.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pvpciv.data.StoryData;
import com.pvpciv.entity.CityNpcEntity;
import com.pvpciv.inv.Bag;
import com.pvpciv.loot.Gear;
import com.pvpciv.loot.LootTables;
import com.pvpciv.loot.Rarity;
import com.pvpciv.net.Network;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.pvpciv.game.DialogueManager.opt;

/**
 * Юки, Тихая тень — убийца на 2 этаже библиотеки (0.9.0).
 *  • Цепочка охоты: очки за любые убийства (игрок = 50 мобов = 20 элиток = 5 боссов), 6 ступеней → крит-шанс и крит-урон,
 *    в конце — навык «Касание убийцы» и титул.
 *  • Загадка дня: слово из 5 букв за 6 попыток (как Wordle) → случайное оружие или броня, +дружба.
 *  • Дружба 0–100: ступени, загадки, разговоры. На 100 — легендарная катана «Тихий снег».
 *  • Обломки катаны после неудачной заточки: испытание «10 слов за 10 минут» — победа возвращает катану.
 */
public final class Assassin {
    private Assassin() {}
    public static final int C = 0xE0405A;
    public static final int MOB = 0, ELITE = 1, BOSS = 2, PLAYER = 3;
    private static final double[] PTS = {1, 2.5, 10, 50};
    /** Ступени: сколько очков, что даёт (шанс %, крит-урон %). */
    private static final int[][] STAGES = {{50, 5, 0}, {120, 5, 0}, {200, 0, 15}, {300, 5, 0}, {450, 0, 20}, {650, 5, 15}};
    public static final int BASE_CRIT_DMG = 50;

    // ------------------------------------------------------------------ очки и криты
    public static void onKill(ServerPlayer p, int kind) {
        StoryData d = Progress.d(p);
        if (d.yukiTalk == 0 || d.huntStage >= STAGES.length) return;
        double before = d.huntPts;
        d.huntPts += PTS[kind];
        p.setData(ModRegistry.STORY.get(), d);
        int need = STAGES[d.huntStage][0];
        if (before < need && d.huntPts >= need)
            p.sendSystemMessage(Component.literal("✦ Юки: «Достаточно. Возвращайся — получишь урок.» (охота: " + (int) d.huntPts + "/" + need + ")").withStyle(ChatFormatting.LIGHT_PURPLE));
        else if (kind >= BOSS) p.displayClientMessage(Component.literal("Охота Юки: " + (int) d.huntPts + "/" + need).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    private static final Map<UUID, Long> TOUCH = new HashMap<>(), TOUCH_CD = new HashMap<>();

    /** Шанс крита (0..1) с учётом катаны и «Касания убийцы». */
    public static double critChance(ServerPlayer p) {
        StoryData d = Progress.d(p);
        double c = d.critChance;
        ItemStack w = p.getMainHandItem();
        if (w.is(ModRegistry.YUKI_KATANA.get())) c += 10;
        c += weaponCrit(w)[0];
        if (TOUCH.getOrDefault(p.getUUID(), 0L) > System.currentTimeMillis()) c += 40;
        if (fullSet(p, "Самурай")) c += 5;   // полный самурайский доспех
        return Math.min(0.85, c / 100.0);
    }

    /** Множитель урона крита (1.5 = +50%). */
    public static double critMult(ServerPlayer p) {
        StoryData d = Progress.d(p);
        double m = BASE_CRIT_DMG + d.critDmg;
        ItemStack w = p.getMainHandItem();
        if (w.is(ModRegistry.YUKI_KATANA.get())) m += 25;
        m += weaponCrit(w)[1];
        if (TOUCH.getOrDefault(p.getUUID(), 0L) > System.currentTimeMillis()) m += 50;
        return 1 + m / 100.0;
    }

    /** Крит-статы оружия этажа 3 (в CustomData: crit, critDmg). */
    public static int[] weaponCrit(ItemStack w) {
        int[] base = ModRegistry.crit(w.getItem());
        if (base != null) return base;
        CustomData cd = w.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return new int[]{0, 0};
        CompoundTag t = cd.copyTag();
        return new int[]{t.getInt("crit"), t.getInt("critDmg")};
    }

    /** Навык «Касание убийцы» (клавиша второго навыка): 6 с — +40% шанс и +50% урон крита, скорость; перезарядка 45 с. */
    public static void touch(ServerPlayer p) {
        StoryData d = Progress.d(p);
        if (d.level < 10) { p.displayClientMessage(Component.literal("Слот навыка 2 откроется на 10 уровне.").withStyle(ChatFormatting.GRAY), true); return; }
        if (!d.touchSkill) { p.displayClientMessage(Component.literal("Слот навыка 2 пуст. Юки учит «Касанию убийцы» в конце своей охоты.").withStyle(ChatFormatting.GRAY), true); return; }
        long now = System.currentTimeMillis(), cd = TOUCH_CD.getOrDefault(p.getUUID(), 0L);
        if (now < cd) { p.displayClientMessage(Component.literal("Касание убийцы: ещё " + ((cd - now) / 1000 + 1) + " с").withStyle(ChatFormatting.GRAY), true); return; }
        TOUCH.put(p.getUUID(), now + 6000);
        TOUCH_CD.put(p.getUUID(), now + 45000);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1, false, false));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.7f, 1.6f);
        com.pvpciv.entity.FxEntity.spawn(p.serverLevel(), com.pvpciv.entity.FxEntity.PILLAR, p.position(), 0.8f, 20, 0xE0405A, 0xFFD0E0);
        Story.title(p, "", "Касание убийцы", C);
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(11, "{\"cd\":45,\"dur\":6}"));
    }

    /** Полный комплект брони (по началу названия сета). */
    public static boolean fullSet(ServerPlayer p, String prefix) {
        for (net.minecraft.world.entity.EquipmentSlot s : new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET})
            if (!(p.getItemBySlot(s).getItem() instanceof com.pvpciv.item.CivArmorItem a) || !a.set.startsWith(prefix)) return false;
        return true;
    }

    /** Раз в секунду: лисий костюм целиком — прыжок и скорость. */
    public static void setBonus(ServerPlayer p) {
        if (fullSet(p, "Лисий")) {
            p.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 0, true, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, true, false, true));
        }
    }

    public static boolean touchActive(ServerPlayer p) { return TOUCH.getOrDefault(p.getUUID(), 0L) > System.currentTimeMillis(); }

    // ------------------------------------------------------------------ дружба
    private static void friend(ServerPlayer p, int add) {
        StoryData d = Progress.d(p);
        int before = d.yukiFriend;
        d.yukiFriend = Math.min(100, d.yukiFriend + add);
        p.setData(ModRegistry.STORY.get(), d);
        if (d.yukiFriend > before) p.displayClientMessage(Component.literal("♥ Дружба с Юки: " + d.yukiFriend + "/100").withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    // ------------------------------------------------------------------ диалог
    public static void talk(ServerPlayer p, com.pvpciv.entity.AnimeNpc npc) {
        StoryData d = Progress.d(p);
        String n = npc.kind.name;
        DialogueManager.Dialogue dl = new DialogueManager.Dialogue("a");
        if (d.yukiTalk == 0) {
            dl.node("a", n, C, "…Ты шумно ходишь. В библиотеке так нельзя. И в моём деле — тоже.",
                            opt("Кто ты?", "b"), opt("Извини", null))
              .node("b", n, C, "Юки. Я убираю тех, кого нужно убрать. Мобов, зверей… людей. Мне всё равно, кого ты выберешь — мне важно, как.",
                            opt("Научи меня", "c"), opt("Звучит жутко", "x"))
              .node("x", n, C, "Жутко — это когда тебя бьют, а ты не видишь откуда. Захочешь научиться — приходи.", opt("[Уйти]", null))
              .node("c", n, C, "Охота. Каждое убийство — очки. Человек стоит как пятьдесят тварей, двадцать элитных или пять хозяев логова. "
                            + "Выбирай сам, кого. Набери очки — и я покажу, куда бить, чтобы было больно.", pp -> {
                  StoryData dd = Progress.d(pp);
                  dd.yukiTalk = 1;
                  pp.setData(ModRegistry.STORY.get(), dd);
                  pp.sendSystemMessage(Component.literal("Охота Юки: моб 1 очко · элитка 2.5 · босс 10 · игрок 50. Первая цель — " + STAGES[0][0] + " очков.").withStyle(ChatFormatting.LIGHT_PURPLE));
              }, opt("Договорились", null));
            DialogueManager.open(p, npc, dl);
            return;
        }
        boolean remains = Bag.has(p, s -> s.is(ModRegistry.YUKI_REMAINS.get()));
        List<DialogueManager.Opt> o = new ArrayList<>();
        if (d.huntStage < STAGES.length) {
            int need = STAGES[d.huntStage][0];
            if (d.huntPts >= need) o.add(opt("Я набрал очки (" + (int) d.huntPts + "/" + need + ")", "lesson"));
            else o.add(opt("Как моя охота? (" + (int) d.huntPts + "/" + need + ")", "hunt"));
        }
        o.add(opt(today(d) ? "Загадка дня (решено)" : "Загадка дня", "word"));
        if (remains) o.add(opt("Катана сломалась…", "rem"));
        o.add(opt("Просто поболтать", "chat"));
        o.add(opt("[Уйти]", null));
        String hello = d.yukiFriend >= 80 ? "А, это ты. Я… рада. Не говори никому." : d.yukiFriend >= 40 ? "Пришёл. Тихо, как я учила. Почти." : "Опять ты. Говори быстрее.";
        dl.node("a", n, C, hello + "\n♥ " + d.yukiFriend + "/100 · крит " + d.critChance + "% · крит-урон +" + (BASE_CRIT_DMG + d.critDmg) + "%", o.toArray(new DialogueManager.Opt[0]));
        dl.node("hunt", n, C, "Мало. Иди и возвращайся, когда руки устанут. Человек — пятьдесят тварей. Подумай.", opt("[Уйти]", null));
        if (d.huntStage < STAGES.length) {
            int[] st = STAGES[d.huntStage];
            String what = (st[1] > 0 ? "+" + st[1] + "% шанс крита" : "") + (st[1] > 0 && st[2] > 0 ? " и " : "") + (st[2] > 0 ? "+" + st[2] + "% крит-урона" : "");
            String[] lines = {"Бей не сильнее — бей точнее. Вот сюда, между пластин.", "Видишь, как он переносит вес? В этот миг он открыт.",
                    "Крит — это не удача. Это когда ты знаешь, а он нет.", "Дыши тише. Сердце выдаёт тебя раньше клинка.", "Шея. Подмышка. Колено. Запомни порядок.",
                    "Последний урок. Касание. Один миг, когда ты — сама смерть. Береги его."};
            dl.node("lesson", n, C, lines[d.huntStage] + "\n(" + what + (d.huntStage == STAGES.length - 1 ? " + навык «Касание убийцы»" : "") + ")", pp -> lesson(pp), opt("[Запомнить]", null));
        }
        dl.node("word", n, C, today(d) ? "Сегодня ты уже думал. Завтра — новое слово." : "Пять букв, шесть попыток. Зелёная — на месте, жёлтая — есть, но не там. Думай, а не гадай.",
                opt(today(d) ? "Ладно" : "Давай", today(d) ? null : "!word"));
        dl.action("word", Assassin::openWord);
        if (remains) dl.node("rem", n, C, "…Ты сломал мою катану. Ладно. Соберу. Но ты докажешь, что голова у тебя работает: десять слов, десять минут. Не успеешь — обломки останутся обломками.",
                opt("Начинай", "!trial"), opt("Не сейчас", null));
        dl.action("trial", Assassin::startTrial);
        dl.node("chat", n, C, chat(p, d), pp -> { StoryData dd = Progress.d(pp); long day = epochDay(); if (dd.gates.getLong("yuki_chat") != day) { dd.gates.putLong("yuki_chat", day); pp.setData(ModRegistry.STORY.get(), dd); friend(pp, 2); } }, opt("[Уйти]", null));
        DialogueManager.open(p, npc, dl);
    }

    private static String chat(ServerPlayer p, StoryData d) {
        String[][] byLvl = {
                {"Сёдо думает, что я читаю. Я слушаю, о чём шепчутся у полок.", "Здесь тихо. Мне нравится тишина. В ней слышно, кто врёт.", "Не трогай мои подушки."},
                {"Говорят, каппы в деревне самураев кланяются в ответ. Проверь. Только не кланяйся первым.", "Луна? Видела её однажды. Она смеялась. Мне не понравилось.", "Чай остыл. Как всегда, когда ты приходишь."},
                {"Знаешь, почему я учу тебя? Ты не ноешь. Почти.", "Кумихо умеет быть кем угодно. Если увидишь двух меня — бей ту, что улыбается.", "Мне снился снег. Тихий. Как в детстве."},
                {"Я рада, что ты приходишь. Не смотри так.", "Если кто-то тебя обидит — скажи. Я никому не скажу, что это была я.", "Ты — единственный, кто знает, где я сижу. Цени."}};
        String[] pool = byLvl[Math.min(3, d.yukiFriend / 25)];
        return pool[p.getRandom().nextInt(pool.length)];
    }

    private static void lesson(ServerPlayer p) {
        StoryData d = Progress.d(p);
        if (d.huntStage >= STAGES.length) return;
        int[] st = STAGES[d.huntStage];
        if (d.huntPts < st[0]) return;
        d.huntPts -= st[0];
        d.critChance += st[1];
        d.critDmg += st[2];
        d.huntStage++;
        if (d.huntStage == STAGES.length) d.touchSkill = true;
        p.setData(ModRegistry.STORY.get(), d);
        friend(p, 12);
        Story.title(p, "Урок " + d.huntStage + "/" + STAGES.length, "Крит " + d.critChance + "% · крит-урон +" + (BASE_CRIT_DMG + d.critDmg) + "%", C);
        if (d.huntStage == STAGES.length) {
            com.pvpciv.progress.Titles.grant(p, "shadow");
            p.sendSystemMessage(Component.literal("✦ Навык «Касание убийцы» — клавиша второго навыка (" + "G по умолчанию" + "). 6 с: +40% шанс и +50% урон крита, скорость. Перезарядка 45 с.").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        checkKatana(p);
    }

    private static void checkKatana(ServerPlayer p) {
        StoryData d = Progress.d(p);
        if (d.yukiFriend < 100 || d.yukiKatana) return;
        d.yukiKatana = true;
        p.setData(ModRegistry.STORY.get(), d);
        Bag.give(p, Gear.withRarity(new ItemStack(ModRegistry.YUKI_KATANA.get()), Rarity.LEGENDARY));
        Story.title(p, "«Тихий снег»", "Юки дарит тебе свою катану", C);
        Story.say(p, "Юки", C, "Возьми. Это… моя. Была. Теперь твоя. Сломаешь — принеси обломки. И не смей благодарить вслух.");
        Story.announceRareDrop(p.serverLevel(), new ItemStack(ModRegistry.YUKI_KATANA.get()), p.getGameProfile().getName());
    }

    // ------------------------------------------------------------------ загадка дня
    private static List<String> WORDS;
    public static List<String> words() {
        if (WORDS != null) return WORDS;
        List<String> l = new ArrayList<>();
        try (var in = Assassin.class.getResourceAsStream("/assets/pvpciv/words/ru5.txt")) {
            if (in != null) new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines().map(String::trim).filter(s -> s.length() == 5).forEach(l::add);
        } catch (Exception ignored) {}
        if (l.isEmpty()) l.add("книга");
        return WORDS = l;
    }

    private static java.util.Set<String> VALID;
    /** Допустимые догадки: ~4000 существительных из 5 букв (OpenCorpora) + все загадываемые слова. */
    public static java.util.Set<String> valid() {
        if (VALID != null) return VALID;
        java.util.Set<String> v = new java.util.HashSet<>(words());
        try (var in = Assassin.class.getResourceAsStream("/assets/pvpciv/words/valid5.txt")) {
            if (in != null) new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines().map(String::trim).filter(x -> x.length() == 5).forEach(v::add);
        } catch (Exception ignored) {}
        return VALID = v;
    }

    /** День по Москве. */
    public static long epochDay() { return java.time.LocalDate.now(java.time.ZoneOffset.ofHours(3)).toEpochDay(); }
    public static String wordOf(long day) { List<String> w = words(); return w.get((int) Math.floorMod(day * 2654435761L + 17, w.size())); }
    private static boolean today(StoryData d) { return d.wordDay == epochDay() && (d.wordSolved || d.wordTries >= 6); }

    private static final class Trial { long until; int solved; String word; final List<String> guesses = new ArrayList<>(); }
    private static final Map<UUID, Trial> TRIAL = new HashMap<>();

    private static void openWord(ServerPlayer p) {
        StoryData d = Progress.d(p);
        if (d.wordDay != epochDay()) { d.wordDay = epochDay(); d.wordTries = 0; d.wordSolved = false; d.wordGuesses = ""; p.setData(ModRegistry.STORY.get(), d); }
        send(p, wordOf(d.wordDay), d.wordGuesses.isEmpty() ? List.of() : Arrays.asList(d.wordGuesses.split(",")), d.wordSolved, d.wordTries >= 6, null);
    }

    private static void startTrial(ServerPlayer p) {
        if (!Bag.has(p, s -> s.is(ModRegistry.YUKI_REMAINS.get()))) return;
        Trial t = new Trial();
        t.until = System.currentTimeMillis() + 10 * 60_000L;
        t.word = words().get(p.getRandom().nextInt(words().size()));
        TRIAL.put(p.getUUID(), t);
        send(p, t.word, t.guesses, false, false, t);
    }

    /** Состояние для экрана: попытки с раскраской (2 — на месте, 1 — есть, 0 — нет). */
    private static void send(ServerPlayer p, String word, List<String> guesses, boolean solved, boolean failed, Trial t) {
        JsonObject j = new JsonObject();
        JsonArray g = new JsonArray();
        for (String s : guesses) {
            JsonObject o = new JsonObject();
            o.addProperty("w", s);
            o.addProperty("c", colors(word, s));
            g.add(o);
        }
        j.add("g", g);
        j.addProperty("solved", solved);
        j.addProperty("failed", failed);
        if (failed || solved) j.addProperty("answer", word);
        if (t != null) { j.addProperty("trial", true); j.addProperty("left", Math.max(0, (t.until - System.currentTimeMillis()) / 1000)); j.addProperty("done", t.solved); }
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(10, j.toString()));
    }

    public static String colors(String word, String guess) {
        char[] res = "00000".toCharArray();
        int[] left = new int[64];
        for (int i = 0; i < 5; i++) if (guess.charAt(i) == word.charAt(i)) res[i] = '2'; else left[word.charAt(i) - 'а' & 63]++;
        for (int i = 0; i < 5; i++) if (res[i] != '2' && left[guess.charAt(i) - 'а' & 63] > 0) { res[i] = '1'; left[guess.charAt(i) - 'а' & 63]--; }
        return new String(res);
    }

    /** Команда из экрана: «word:<слово>». */
    public static void guess(ServerPlayer p, String raw) {
        String s = raw.toLowerCase(Locale.ROOT).replace('ё', 'е').trim();
        if (s.length() != 5 || !s.chars().allMatch(c -> c >= 'а' && c <= 'я')) { p.displayClientMessage(Component.literal("Нужно слово из 5 русских букв.").withStyle(ChatFormatting.RED), true); return; }
        if (!valid().contains(s)) { p.displayClientMessage(Component.literal("«" + s + "» — такого слова нет в словаре Юки (нужно существительное).").withStyle(ChatFormatting.RED), true);
            PacketDistributor.sendToPlayer(p, new Network.PhoneData(12, "{}")); return; }
        Trial t = TRIAL.get(p.getUUID());
        if (t != null) { trialGuess(p, t, s); return; }
        StoryData d = Progress.d(p);
        if (d.wordDay != epochDay() || d.wordSolved || d.wordTries >= 6) return;
        String w = wordOf(d.wordDay);
        d.wordTries++;
        d.wordGuesses = d.wordGuesses.isEmpty() ? s : d.wordGuesses + "," + s;
        boolean win = s.equals(w);
        if (win) d.wordSolved = true;
        p.setData(ModRegistry.STORY.get(), d);
        send(p, w, Arrays.asList(d.wordGuesses.split(",")), win, !win && d.wordTries >= 6, null);
        p.playNotifySound(win ? ModRegistry.FX_WORD_OK.get() : ModRegistry.FX_WORD_BAD.get(), SoundSource.PLAYERS, 0.8f, 1f);
        if (win) wordReward(p, d.wordTries);
        else if (d.wordTries >= 6) { Story.say(p, "Юки", C, "Слово было «" + w + "». Ошибки… надо запоминать телом."); punish(p); }
    }

    /** Бета 0.9.6: проиграл загадку — Юки рубит катаной: 90% максимального здоровья (но не насмерть). */
    private static void punish(ServerPlayer p) {
        var yuki = p.serverLevel().getEntitiesOfClass(com.pvpciv.entity.AnimeNpc.class, p.getBoundingBox().inflate(16), n -> n.kind == com.pvpciv.entity.AnimeNpc.Kind.YUKI);
        if (yuki.isEmpty()) return;
        var y = yuki.get(0);
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(13, "{}"));   // закрыть экран загадки — пусть видит
        y.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, p.getEyePosition());
        y.setAnim("slash");
        p.serverLevel().playSound(null, y.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2f, 0.7f);
        GameEvents.later(p.server, 8, () -> {
            if (!p.isAlive()) return;
            float dmg = Math.min(p.getMaxHealth() * 0.9f, p.getHealth() - 1f);
            if (dmg > 0) p.hurt(p.damageSources().magic(), dmg);
            p.serverLevel().sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX(), p.getY() + 1.1, p.getZ(), 1, 0, 0, 0, 0);
            p.serverLevel().sendParticles(ParticleTypes.CRIT, p.getX(), p.getY() + 1.1, p.getZ(), 20, 0.3, 0.4, 0.3, 0.3);
            p.serverLevel().playSound(null, p.blockPosition(), ModRegistry.FX_CRIT.get(), SoundSource.HOSTILE, 1f, 0.6f);
        });
        GameEvents.later(p.server, 24, () -> y.setAnim(""));
    }

    private static void wordReward(ServerPlayer p, int tries) {
        friend(p, 5);
        String table = p.getRandom().nextBoolean() ? "floor2_elite" : "floor2b_elite";
        ItemStack prize = ItemStack.EMPTY;
        for (int i = 0; i < 12 && prize.isEmpty(); i++)
            for (ItemStack st : LootTables.roll(table, p.getRandom(), 3.0))
                if (st.getItem() instanceof com.pvpciv.item.CivWeaponItem || st.getItem() instanceof com.pvpciv.item.CivArmorItem) { prize = st; break; }
        if (prize.isEmpty()) prize = new ItemStack(ModRegistry.BLESSED_STAR_W1.get());
        if (tries <= 3 && prize.getItem() instanceof com.pvpciv.item.CivWeaponItem && Gear.rarity(prize).ordinal() < Rarity.EPIC.ordinal()) Gear.withRarity(prize, Rarity.EPIC);
        Story.announceRareDrop(p.serverLevel(), prize, p.getGameProfile().getName());
        Bag.give(p, prize);
        Story.say(p, "Юки", C, tries <= 2 ? "…Быстро. Подозрительно быстро. Держи." : "Угадал. Держи — заслужил.");
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.4f);
        checkKatana(p);
    }

    private static void trialGuess(ServerPlayer p, Trial t, String s) {
        if (System.currentTimeMillis() > t.until) { TRIAL.remove(p.getUUID()); Story.say(p, "Юки", C, "Время вышло. " + t.solved + " из 10. Обломки — твои. Приходи, когда соберёшься."); send(p, t.word, t.guesses, false, true, t); punish(p); return; }
        t.guesses.add(s);
        p.playNotifySound(s.equals(t.word) ? ModRegistry.FX_WORD_OK.get() : ModRegistry.FX_WORD_BAD.get(), SoundSource.PLAYERS, 0.8f, 1f);
        if (s.equals(t.word)) {
            t.solved++;
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.8f, 1.2f + t.solved * 0.05f);
            if (t.solved >= 10) {
                TRIAL.remove(p.getUUID());
                restore(p);
                send(p, t.word, t.guesses, true, false, t);
                return;
            }
            t.word = words().get(p.getRandom().nextInt(words().size()));
            t.guesses.clear();
        } else if (t.guesses.size() >= 6) {
            Story.say(p, "Юки", C, "Это было «" + t.word + "». Следующее.");
            t.word = words().get(p.getRandom().nextInt(words().size()));
            t.guesses.clear();
        }
        send(p, t.word, t.guesses, false, false, t);
    }

    private static void restore(ServerPlayer p) {
        ItemStack rem = ItemStack.EMPTY;
        for (int i = 0; i < Bag.get(p).getSlots(); i++) if (Bag.get(p).getStackInSlot(i).is(ModRegistry.YUKI_REMAINS.get())) { rem = Bag.get(p).getStackInSlot(i).copy(); break; }
        int stars = 0;
        CustomData cd = rem.get(DataComponents.CUSTOM_DATA);
        if (cd != null) stars = cd.copyTag().getInt("stars");
        Bag.take(p, s -> s.is(ModRegistry.YUKI_REMAINS.get()), 1);
        ItemStack k = Gear.withRarity(new ItemStack(ModRegistry.YUKI_KATANA.get()), Rarity.LEGENDARY);
        Gear.setStars(k, stars);
        Bag.give(p, k);
        Story.title(p, "«Тихий снег»", "Катана снова цела (+" + stars + ")", C);
        Story.say(p, "Юки", C, "Десять из десяти. Ладно. Ты не безнадёжен. Держи — и больше не ломай.");
        friend(p, 5);
    }

    /** Кузница: катана Юки ломается не в обломки-хлам, а в «Обломки Тихого снега» (с запомненной заточкой). */
    public static ItemStack remainsOf(ItemStack weapon, int stars) {
        ItemStack r = new ItemStack(ModRegistry.YUKI_REMAINS.get());
        CompoundTag t = new CompoundTag();
        t.putInt("stars", stars);
        r.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        return r;
    }

    public static void particlesCrit(ServerPlayer p, net.minecraft.world.entity.Entity target) {
        p.serverLevel().sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 14, 0.3, 0.3, 0.3, 0.3);
        p.serverLevel().playSound(null, target.blockPosition(), ModRegistry.FX_CRIT.get(), SoundSource.PLAYERS, 0.9f, 0.95f + p.getRandom().nextFloat() * 0.1f);
    }
}
