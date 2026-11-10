package com.pvpciv.game;

import com.pvpciv.combat.Mobs;
import com.pvpciv.data.StoryData;
import com.pvpciv.entity.CarmillaEntity;
import com.pvpciv.game.DialogueManager.Dialogue;
import com.pvpciv.net.Network;
import com.pvpciv.progress.Progress;
import com.pvpciv.progress.Titles;
import com.pvpciv.registry.ModRegistry;
import com.pvpciv.world.Basement;
import com.pvpciv.world.CivWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static com.pvpciv.game.DialogueManager.opt;

/**
 * Кровавые контракты Кармиллы. Берёшь контракт — убиваешь мобов за 20 минут — сдаёшь в подвале и получаешь Искры,
 * звёзды (на сложных — благословенные). Не успел или отказался — Кармилла сама приходит за тобой,
 * уводит в подвал (катсцена), кусает и запирает в клетке на 3 минуты. Первый раз — титул «Жертва вампиризма».
 */
public final class Vampire {
    public static final String NAME = "Кармилла";
    public static final int C = CarmillaEntity.COLOR;
    public static final long TIME_LIMIT = 20L * 60 * 20, COOLDOWN_DONE = 20L * 60 * 45, COOLDOWN_FAIL = 20L * 60 * 30, JAIL = 20L * 60 * 3;

    /** tier, kind (Mobs.Kind или ELITE), сколько, как назвать во множественном числе. */
    public record Contract(int tier, String kind, int need, String what) {}
    private static final List<Contract> POOL = List.of(
            new Contract(0, "ASH_ZOMBIE", 12, "пепельных зомби"),
            new Contract(0, "ASH_SPIDER", 10, "пепельных пауков"),
            new Contract(1, "WITHERED", 10, "иссохших"),
            new Contract(1, "ASH_ARCHER", 10, "пепельных лучников"),
            new Contract(2, "FROST_ZOMBIE", 10, "обмороженных"),
            new Contract(2, "FROST_SPIDER", 8, "ледяных пауков"),
            new Contract(2, "FROST_ARCHER", 8, "инеевых стрелков"),
            new Contract(3, "ELITE", 3, "элитных мобов (★)"));
    private static final String[] TIER = {"Лёгкий", "Средний", "Сложный", "Кровавый"};
    private static final int[] TIER_COLOR = {0xB8B8B8, 0x6FD3FF, 0xC77DFF, 0xFF5566};

    private static long now(MinecraftServer s) { return CivWorld.level(s).getGameTime(); }

    // ------------------------------------------------------------------ состояние
    public static boolean jailed(ServerPlayer p) { return Progress.d(p).jailUntil > now(p.server); }
    public static boolean hasQuest(StoryData d) { return !d.questKind.isEmpty(); }
    public static boolean questDone(StoryData d) { return hasQuest(d) && d.questHave >= d.questNeed; }

    public static Contract contract(StoryData d) {
        for (Contract c : POOL) if (c.kind().equals(d.questKind) && c.tier() == d.questTier) return c;
        return null;
    }

    /** Строка для HUD-трекера: «Контракт: 5/12 пепельных зомби». */
    public static String hudLine(StoryData d) {
        Contract c = contract(d);
        if (c == null) return "";
        if (questDone(d)) return "Контракт выполнен — вернись к Кармилле";
        return "Контракт: " + d.questHave + "/" + d.questNeed + " " + c.what();
    }

    public static int hudSeconds(ServerPlayer p, StoryData d) {
        if (!hasQuest(d) || questDone(d)) return -1;
        return (int) Math.max(0, (d.questDeadline - now(p.server)) / 20);
    }

    public static int jailSeconds(ServerPlayer p) {
        long left = Progress.d(p).jailUntil - now(p.server);
        return left > 0 ? (int) (left / 20) : -1;
    }

    private static Contract offerFor(ServerPlayer p) {
        int lvl = Progress.d(p).level;
        int lo = lvl <= 2 ? 0 : lvl <= 4 ? 1 : 2, hi = lvl <= 2 ? 1 : lvl <= 4 ? 2 : 3;
        List<Contract> ok = new ArrayList<>();
        for (Contract c : POOL) if (c.tier() >= lo && c.tier() <= hi) ok.add(c);
        long seed = p.getUUID().getLeastSignificantBits() ^ (now(p.server) / (20L * 60 * 10));   // предложение меняется раз в 10 минут
        return ok.get((int) Math.floorMod(seed, ok.size()));
    }

    private static String rewardText(int tier) {
        return switch (tier) {
            case 0 -> "100 ✦ и звезда заточки";
            case 1 -> "180 ✦, 2 звезды и шанс 15% на благословенную";
            case 2 -> "320 ✦, 2 звезды и шанс 60% на благословенную";
            default -> "450 ✦ и благословенная звезда (30% — вторая)";
        };
    }

    // ------------------------------------------------------------------ события
    public static void onKill(ServerPlayer p, Mob mob, boolean elite) {
        StoryData d = Progress.d(p);
        if (!hasQuest(d) || questDone(d)) return;
        Mobs.Kind k = Mobs.Kind.of(mob.getType());
        boolean counts = d.questKind.equals("ELITE") ? elite : k != null && k.name().equals(d.questKind);
        if (!counts) return;
        d.questHave++;
        p.setData(ModRegistry.STORY.get(), d);
        if (questDone(d)) {
            d.questDeadline = 0;
            Story.title(p, "КОНТРАКТ ВЫПОЛНЕН", "Вернись в подвал к Кармилле за наградой", C);
            p.level().playSound(null, p.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1f, 1.2f);
        } else p.displayClientMessage(Component.literal("Контракт: " + d.questHave + "/" + d.questNeed).withStyle(s -> s.withColor(C)), true);
        Story.sync(p);
    }

    /** Раз в секунду: сроки контракта и заточение. */
    public static void tick(ServerPlayer p) {
        if (!CivWorld.is(p.level())) return;
        StoryData d = Progress.d(p);
        long now = now(p.server);
        if (d.jailUntil > 0) {
            if (now >= d.jailUntil) release(p);
            else if (!Basement.inCell(p.position()) && !ABDUCTING.contains(p.getUUID())) toCell(p);
            return;
        }
        if (hasQuest(d) && !questDone(d) && d.questDeadline > 0 && now >= d.questDeadline && !ABDUCTING.contains(p.getUUID())) {
            Story.say(p, NAME, C, "Время вышло, милый. Долг платежом красен — особенно кровью.");
            abduct(p);
        }
    }

    // ------------------------------------------------------------------ диалог
    public static Dialogue dialogue(ServerPlayer p, CarmillaEntity c) {
        StoryData d = Progress.d(p);
        long now = now(p.server);
        Dialogue dl = new Dialogue("a");
        if (jailed(p)) {
            return dl.node("a", NAME, C, "Тсс. Ты отбываешь долг. Посиди, подумай о сроках. Мне нравится, как ты думаешь — громко.", opt("[Молчать]", null));
        }
        if (questDone(d)) {
            Contract k = contract(d);
            return dl.node("a", NAME, C, "Учуяла тебя ещё с лестницы. " + d.questNeed + " " + (k != null ? k.what() : "") + " — пахнет честной работой. Держи плату.",
                    pp -> reward(pp), opt("[Забрать награду]", null));
        }
        if (hasQuest(d)) {
            int sec = hudSeconds(p, d);
            return dl.node("a", NAME, C, String.format("Контракт ещё не закрыт: %d/%d. У тебя %d:%02d. Я считаю каждую секунду — у меня их много.", d.questHave, d.questNeed, sec / 60, sec % 60),
                            opt("Работаю!", null), opt("Я передумал… (отказаться)", "quit"))
                    .node("quit", NAME, C, "Передумал? Как мило. Тогда заплатишь сейчас. Не бойся — больно только первые пару секунд.",
                            opt("Подожди, я…", "quit2"))
                    .node("quit2", NAME, C, "…", pp -> { clearQuest(pp); abduct(pp); }, opt("[…]", null));
        }
        if (d.questCooldown > now) {
            long min = (d.questCooldown - now) / 1200 + 1;
            return dl.node("a", NAME, C, d.cdFail
                    ? "Свой долг ты уже отсидел. Новый контракт — через " + min + " мин. Ешь гранаты, у тебя бледный вид."
                    : "Ты сегодня уже был мне полезен. Приходи через " + min + " мин. И береги шею.", opt("[Уйти]", null));
        }
        Contract k = offerFor(p);
        boolean first = d.questsDone == 0 && d.jailed == 0;
        String intro = first
                ? "Новенький? В подвал общежития студенты обычно не спускаются. А те, кто спускается… становятся моими должниками. Шучу. Наполовину."
                : "Снова ты. Соскучился по подвалу? Или по мне?";
        dl.node("a", NAME, C, intro, opt("Кто ты такая?", first ? "who" : "offer"), opt("Есть работа?", "offer"), opt("[Уйти]", null));
        dl.node("who", NAME, C, "Кармилла. Этот подвал был моим склепом задолго до того, как сверху построили ваше общежитие. Комендант Агафья делает вид, что меня нет. Я делаю вид, что не голодна.",
                opt("И что тебе нужно?", "deal"));
        dl.node("deal", NAME, C, "Мне скучно, а мобы на этажах мешают моим… поставкам. Я даю контракт — ты чистишь этаж и получаешь плату. Не успел — плату беру я. Кровью. Немного. Обычно.",
                opt("Звучит честно…?", "offer"));
        dl.node("offer", NAME, C, "Контракт «" + TIER[k.tier()] + "»: убить " + k.need() + " " + k.what() + " за 20 минут. Плата: " + rewardText(k.tier())
                        + ". Не успеешь — я приду за тобой сама. Где бы ты ни был.",
                opt("Подписываю кровью", "take"), opt("Не сегодня", "no"));
        dl.node("take", NAME, C, "Ммм, хороший почерк. Беги. Часы уже тикают — я их слышу.", pp -> take(pp, k), opt("[Уйти]", null));
        dl.node("no", NAME, C, "Как скажешь. Предложение меняется каждые десять минут — как и моё настроение. Кстати, если увидишь блондинку с арбалетом на третьем этаже — передай, что я соскучилась.", opt("[Уйти]", null));
        return dl;
    }

    private static void take(ServerPlayer p, Contract k) {
        StoryData d = Progress.d(p);
        d.questKind = k.kind(); d.questTier = k.tier(); d.questNeed = k.need(); d.questHave = 0;
        d.questDeadline = now(p.server) + TIME_LIMIT;
        p.setData(ModRegistry.STORY.get(), d);
        Story.title(p, "КОНТРАКТ", k.need() + " " + k.what() + " • 20:00", TIER_COLOR[k.tier()]);
        p.level().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 0.8f);
        Story.sync(p);
    }

    private static void clearQuest(ServerPlayer p) {
        StoryData d = Progress.d(p);
        d.questKind = ""; d.questNeed = 0; d.questHave = 0; d.questDeadline = 0;
        p.setData(ModRegistry.STORY.get(), d);
    }

    private static void reward(ServerPlayer p) {
        StoryData d = Progress.d(p);
        int tier = d.questTier;
        clearQuest(p);
        d = Progress.d(p);
        d.questsDone++;
        d.questCooldown = now(p.server) + COOLDOWN_DONE;
        d.cdFail = false;
        p.setData(ModRegistry.STORY.get(), d);
        var r = p.getRandom();
        int money = switch (tier) { case 0 -> 100; case 1 -> 180; case 2 -> 320; default -> 450; };
        int stars = tier == 0 ? 1 : tier == 3 ? 0 : 2;
        int blessed = switch (tier) { case 1 -> r.nextFloat() < 0.15f ? 1 : 0; case 2 -> r.nextFloat() < 0.6f ? 1 : 0; case 3 -> 1 + (r.nextFloat() < 0.3f ? 1 : 0); default -> 0; };
        Progress.addMoney(p, money, true);
        if (stars > 0) com.pvpciv.inv.Bag.give(p, new ItemStack(ModRegistry.STAR_W1.get(), stars));
        if (blessed > 0) {
            ItemStack b = new ItemStack(ModRegistry.BLESSED_STAR_W1.get(), blessed);
            com.pvpciv.inv.Bag.give(p, b);
            Story.announceRareDrop(p.level(), b);
        }
        p.sendSystemMessage(Component.literal("Плата Кармиллы: " + money + " ✦" + (stars > 0 ? ", звёзды ×" + stars : "") + (blessed > 0 ? ", благословенные ×" + blessed : ""))
                .withStyle(s -> s.withColor(C)));
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 0.8f);
        if (d.questsDone >= 5) Titles.grant(p, "contractor");
        Story.sync(p);
    }

    // ------------------------------------------------------------------ похищение
    private static final java.util.Set<java.util.UUID> ABDUCTING = new java.util.HashSet<>();

    /** Катсцена: Кармилла появляется за спиной, хватает, темнота — подвал, укус, клетка. */
    public static void abduct(ServerPlayer p) {
        MinecraftServer server = p.server;
        if (ABDUCTING.contains(p.getUUID())) return;
        clearQuest(p);
        ABDUCTING.add(p.getUUID());
        DialogueManager.forget(p);
        PacketDistributor.sendToPlayer(p, new Network.DialogueClose());
        if (p.isPassenger()) p.stopRiding();
        if (!CivWorld.is(p.level())) toCell(p);
        ServerLevel lvl = CivWorld.level(server);
        CarmillaEntity c = CarmillaEntity.find(server);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 10, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP, 70, 128, false, false));   // прыжок отключён
        PacketDistributor.sendToPlayer(p, new Network.Cinematic(1, 70, "Кармилла пришла за долгом…"));
        lvl.playSound(null, p.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 2f, 0.7f);
        if (c != null) {
            c.busy = 140;
            Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
            Vec3 at = p.position().add(look.scale(3.0));   // прямо перед глазами — чтобы её было видно
            CarmillaEntity.puff(lvl, at);
            c.teleportTo(at.x, p.getY(), at.z);
            c.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, p.getEyePosition());
            c.setAnim("spread");
            lvl.playSound(null, p.blockPosition(), SoundEvents.PHANTOM_BITE, SoundSource.HOSTILE, 1.5f, 0.6f);
        }
        Story.say(p, NAME, C, "Попался.");
        GameEvents.later(server, 22, () -> {
            if (c != null) { c.setAnim("drag"); lvl.sendParticles(new DustParticleOptions(new Vector3f(0.7f, 0, 0.1f), 2f), p.getX(), p.getY() + 1, p.getZ(), 50, 0.5, 0.8, 0.5, 0); }
            p.level().playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 1.2f, 1.3f);
        });
        GameEvents.later(server, 52, () -> {
            if (!p.isAlive() || p.hasDisconnected()) { ABDUCTING.remove(p.getUUID()); return; }
            StoryData d = Progress.d(p);
            d.jailUntil = now(server) + JAIL;
            d.jailed++;
            d.questCooldown = 0;
            p.setData(ModRegistry.STORY.get(), d);
            Basement.setDoor(lvl, false);
            toCell(p);
            if (c != null) {
                Vec3 cs = Vec3.atBottomCenterOf(Basement.CELL_SPOT);
                c.teleportTo(cs.x - 1.2, cs.y, cs.z);
                c.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, p.getEyePosition());
                c.setAnim("bite");
            }
            PacketDistributor.sendToPlayer(p, new Network.Cinematic(2, 50, "Укус"));
            lvl.playSound(null, p.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.HOSTILE, 1.5f, 0.6f);
            lvl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, SoundSource.PLAYERS, 1f, 0.8f);
            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, false));
        });
        GameEvents.later(server, 110, () -> {
            ABDUCTING.remove(p.getUUID());
            if (c != null) { CarmillaEntity.puff(lvl, c.position()); c.busy = 0; c.setAnim(""); }
            Story.say(p, NAME, C, "Вкусно. Посиди тут три минутки, остынь. Дверь откроется сама — я же не зверь.");
            Titles.grant(p, "vampire_victim");
            Story.sync(p);
        });
    }

    public static void toCell(ServerPlayer p) {
        ServerLevel lvl = CivWorld.level(p.server);
        Vec3 cs = Vec3.atBottomCenterOf(Basement.CELL_SPOT);
        p.resetFallDistance();
        p.teleportTo(lvl, cs.x, cs.y, cs.z, 90, 0);
        p.setDeltaMovement(Vec3.ZERO);
    }

    private static void release(ServerPlayer p) {
        StoryData d = Progress.d(p);
        d.jailUntil = 0;
        d.questCooldown = now(p.server) + COOLDOWN_FAIL;
        d.cdFail = true;
        p.setData(ModRegistry.STORY.get(), d);
        boolean other = false;
        for (ServerPlayer o : p.server.getPlayerList().getPlayers()) if (o != p && jailed(o)) other = true;
        ServerLevel lvl = CivWorld.level(p.server);
        if (!other) Basement.setDoor(lvl, true);
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 120, 0));
        Story.say(p, NAME, C, "Свободен. Пока что. Анемия пройдёт через пару минут — съешь что-нибудь красное.");
        lvl.playSound(null, Basement.CELL_DOOR, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1f, 0.8f);
        Story.sync(p);
    }

    /** Вход в игру: должник возвращается в клетку; просроченный контракт — Кармилла придёт чуть позже. */
    public static void onLogin(ServerPlayer p) {
        if (jailed(p)) { Basement.setDoor(CivWorld.level(p.server), false); toCell(p); }
        else if (Progress.d(p).jailUntil > 0) release(p);
    }
}
