package com.pvpciv.game;

import com.pvpciv.combat.Mobs;
import com.pvpciv.data.StoryData;
import com.pvpciv.entity.boss.LunaBoss;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModF5;
import com.pvpciv.registry.ModRegistry;
import com.pvpciv.world.CivWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * 1.0 — испытание Луны. Говоришь с Луной → тебя переносит на личную копию арены (у каждого игрока своя).
 * Стадии идут подряд, пока не проиграешь: каждая стадия — сила следующего этажа (5, 6, 7…).
 * Первое прохождение стадии — тёмная материя (дерево классов); некоторые — тёмная звезда (+11).
 * После первого прохождения можно начинать со следующей стадии. Проигрыш — не смерть: Луна возвращает тебя на станцию.
 * Между стадиями 6 с передышки; присел (Shift) — ушёл с наградой.
 */
public final class LunaTrial {
    private LunaTrial() {}
    public static final int AX = 4000, AY = 100, AZ = 4000, STEP = 160, R = 15;
    private static final int PAUSE = 120, COUNT = 60;

    private static final class Session {
        final int slot; int stage; int t; UUID boss; int phase;   // 0 — отсчёт, 1 — бой, 2 — передышка
        long started;   // 1.0.1: начало испытания (музыка)
        final List<ItemStack> pending = new ArrayList<>();   // награды выдаются на станции (на арене при полной сумке они бы выпали в пустоту)
        final ServerBossEvent bar = new ServerBossEvent(Component.literal("Луна"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
        Session(int slot, int stage) { this.slot = slot; this.stage = stage; }
    }
    private static final Map<UUID, Session> S = new HashMap<>();

    public static boolean in(ServerPlayer p) { return S.containsKey(p.getUUID()); }
    public static int best(StoryData d) { return d.gates.getInt("luna_best"); }
    public static int floorOf(int stage) { return 4 + stage; }
    public static int powerLevel(int stage) { return 30 + 6 * (stage - 1); }

    static BlockPos center(int slot) { return new BlockPos(AX + slot * STEP, AY, AZ); }

    // ------------------------------------------------------------------ диалог Луны
    public static void talk(ServerPlayer p, com.pvpciv.entity.AnimeNpc npc) {
        StoryData d = Progress.d(p);
        int best = best(d);
        DialogueManager.Dialogue dl = new DialogueManager.Dialogue("a");
        String hello = best == 0 ? "Хочешь подраться со мной? Предупреждаю: я не поддаюсь. Каждая стадия — сила следующего этажа. За первую победу на стадии — тёмная материя."
                : "Лучшая твоя стадия — " + best + ". Продолжим? Или опять за тортом пришёл?";
        List<DialogueManager.Opt> opts = new ArrayList<>();
        opts.add(DialogueManager.opt("Испытание: стадия 1", "!s1"));
        if (best >= 1) opts.add(DialogueManager.opt("Испытание: со стадии " + (best + 1), "!sn"));
        opts.add(DialogueManager.opt("Лавка", "!shop"));
        opts.add(DialogueManager.opt("Дерево классов", "!tree"));
        opts.add(DialogueManager.opt("[Уйти]", null));
        dl.node("a", "Луна", 0xE070C0, hello, opts.toArray(new DialogueManager.Opt[0]));
        dl.action("s1", pp -> start(pp, 1));
        dl.action("sn", pp -> start(pp, best(Progress.d(pp)) + 1));
        dl.action("shop", pp -> LunaStation.shop(pp, npc));
        dl.action("tree", pp -> Classes.sync(pp, true));
        DialogueManager.open(p, npc, dl);
    }

    public static void cmd(ServerPlayer p, String cmd) {}

    /** 1.0.1: музыка испытания — тому, кто на арене. */
    public static void music(MinecraftServer server, Map<UUID, String[]> out) {
        long gt = server.overworld().getGameTime();
        S.forEach((u, s) -> out.put(u, new String[]{"luna", String.valueOf((gt - s.started) / 20.0)}));
    }

    // ------------------------------------------------------------------ старт / стадии
    public static void start(ServerPlayer p, int stage) {
        if (in(p) || Rocket.flying(p)) return;
        if (Progress.d(p).level < 30) { Story.say(p, "Луна", 0xE070C0, "Подрасти до 30 уровня. А то мне будет скучно, а тебе — больно."); return; }
        int slot = 0;
        Set<Integer> used = new HashSet<>();
        for (Session s : S.values()) used.add(s.slot);
        while (used.contains(slot)) slot++;
        Session s = new Session(slot, Math.max(1, stage));
        s.started = p.server.overworld().getGameTime();
        S.put(p.getUUID(), s);
        ServerLevel l = CivWorld.f5(p.server);
        build(l, center(slot));
        for (var b : l.getEntitiesOfClass(LunaBoss.class, new net.minecraft.world.phys.AABB(center(slot)).inflate(R + 20))) b.discard();
        BlockPos c = center(slot);
        p.teleportTo(l, c.getX() + 0.5, AY, c.getZ() + 10.5, 180, 0);
        p.resetFallDistance();
        p.setHealth(p.getMaxHealth());
        s.bar.addPlayer(p);
        begin(p, s);
    }

    private static void begin(ServerPlayer p, Session s) {
        s.phase = 0; s.t = 0;
        title(p, "Стадия " + s.stage, "Сила этажа " + floorOf(s.stage) + (best(Progress.d(p)) < s.stage ? " · впервые: тёмная материя" : ""), ChatFormatting.LIGHT_PURPLE);
        s.bar.setName(Component.literal("Луна · стадия " + s.stage + " · сила этажа " + floorOf(s.stage)));
        s.bar.setProgress(1f);
    }

    private static void spawn(ServerPlayer p, Session s) {
        ServerLevel l = CivWorld.f5(p.server);
        BlockPos c = center(s.slot);
        LunaBoss b = com.pvpciv.registry.ModMobs.LUNA_BOSS.get().create(l);
        if (b == null) return;
        b.moveTo(c.getX() + 0.5, AY, c.getZ() - 6.5, 0, 0);
        b.setHome(Vec3.atBottomCenterOf(c));
        b.stage = s.stage;
        int pl = Progress.d(p).level, pw = powerLevel(s.stage);
        Mobs.setup(b, Math.min(pw, pl + 1), false, "Луна", "none");   // уровень (броня, иммунитет) — не выше твоего +1; сила — от этажа стадии
        double hp = Mobs.hp(pw) * (4.5 + 0.5 * s.stage);
        b.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        b.setHealth((float) hp);
        b.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Mobs.dmg(pw) * 1.25);
        b.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3 + 0.01 * s.stage);
        b.setCustomName(Component.literal("☾ Луна · стадия " + s.stage).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        b.setCustomNameVisible(true);
        b.addTag("luna_trial");
        b.setTarget(p);
        l.addFreshEntity(b);
        s.boss = b.getUUID();
        l.sendParticles(ParticleTypes.END_ROD, b.getX(), b.getY() + 1, b.getZ(), 60, 0.6, 1, 0.6, 0.1);
        l.playSound(null, b.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2f, 0.8f);
    }

    public static void tick(MinecraftServer server) {
        ServerLevel l = CivWorld.f5(server);
        // перезашёл посреди испытания (или после рестарта сервера) — не оставлять на пустой арене / в разломе
        if (server.getTickCount() % 40 == 0 && CivWorld.isF5(l)) for (ServerPlayer p : l.players()) {
            if (p.isCreative() || p.isSpectator()) continue;
            if (p.getX() > AX - 200 && !in(p)) {
                for (var b : l.getEntitiesOfClass(LunaBoss.class, p.getBoundingBox().inflate(40))) b.discard();
                Vec3 back = com.pvpciv.floor.Floor5.LUNA().add(-2, 0, 2);
                p.teleportTo(back.x, back.y, back.z);
                p.sendSystemMessage(Component.literal("☾ Испытание прервано — Луна вернула тебя на станцию.").withStyle(ChatFormatting.LIGHT_PURPLE));
            } else if (p.getX() < AX - 200 && p.getZ() > VoidRift.C.getZ() - 200 && !VoidRift.inRift(p)) {
                BlockPos pad = com.pvpciv.floor.Belt.PAD;
                p.teleportTo(pad.getX() + 0.5, pad.getY(), pad.getZ() + 0.5);
                p.sendSystemMessage(Component.literal("Разлом Пустоты закрылся — тебя выбросило на шлюзовую площадку.").withStyle(ChatFormatting.DARK_PURPLE));
            }
        }
        if (S.isEmpty()) return;
        for (Iterator<Map.Entry<UUID, Session>> it = S.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            Session s = en.getValue();
            if (p == null || p.level() != l || p.position().distanceTo(Vec3.atCenterOf(center(s.slot))) > R + 30) {   // вышел, телепорт, дисконнект
                cleanup(l, s);
                it.remove();
                continue;
            }
            s.t++;
            if (p.getY() < AY - 8) { lose(p, s); cleanup(l, s); it.remove(); continue; }
            switch (s.phase) {
                case 0 -> {
                    if (s.t % 20 == 0 && s.t < COUNT) p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(String.valueOf(3 - s.t / 20)).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)));
                    if (s.t >= COUNT) { s.phase = 1; s.t = 0; spawn(p, s); }
                }
                case 1 -> {
                    LunaBoss b = s.boss == null ? null : (l.getEntity(s.boss) instanceof LunaBoss lb ? lb : null);
                    if (b != null && b.isAlive()) { s.bar.setProgress(Math.max(0, b.getHealth() / b.getMaxHealth())); if (b.getTarget() != p) b.setTarget(p); }
                    else if (s.t > 5) { cleared(p, s); s.phase = 2; s.t = 0; }
                }
                case 2 -> {
                    if (p.isShiftKeyDown() && s.t > 10) { leave(p, s, "Ты ушёл после стадии " + (s.stage - 1) + ". Луна машет вслед."); cleanup(l, s); it.remove(); continue; }
                    if (s.t % 20 == 0) p.displayClientMessage(Component.literal("Следующая стадия через " + Math.max(0, (PAUSE - s.t) / 20) + " с · присядь (Shift) — уйти с наградой").withStyle(ChatFormatting.LIGHT_PURPLE), true);
                    if (s.t >= PAUSE) begin(p, s);
                }
                default -> {}
            }
        }
    }

    private static void cleared(ServerPlayer p, Session s) {
        StoryData d = Progress.d(p);
        int st = s.stage;
        boolean first = best(d) < st;
        List<ItemStack> loot = new ArrayList<>();
        long money = 1500L * st;
        loot.add(new ItemStack(ModF5.CMP_LAZURATRON.get(), 1 + (st >= 3 ? 1 : 0)));
        if (first) {
            d.gates.putInt("luna_best", st);
            p.setData(ModRegistry.STORY.get(), d);
            loot.add(new ItemStack(ModF5.DARK_MATTER.get(), st >= 4 ? 3 : 2));
            if (st == 2 || st == 4 || st >= 6) loot.add(new ItemStack(ModF5.DARK_STAR.get()));
            money *= 2;
        } else if (st >= 3 && p.getRandom().nextDouble() < 0.10) loot.add(new ItemStack(ModF5.DARK_STAR.get()));
        for (ItemStack it : loot) { Story.announceRareDrop(p.level(), it, p.getGameProfile().getName()); s.pending.add(it); }
        p.sendSystemMessage(Component.literal("☾ Награда стадии " + st + " (выдам на станции): " + Story.lootLine(loot)).withStyle(ChatFormatting.GOLD));
        Progress.addMoney(p, money, true);
        Progress.addXp(p, Progress.xpFor(d.level, powerLevel(st), 6));
        title(p, "Пройдено!", "Стадия " + st + (first ? " впервые · тёмная материя ×" + (st >= 4 ? 3 : 2) : " · Луна: «Ещё?»"), ChatFormatting.GOLD);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1.2f);
        if (first && st >= 3) for (ServerPlayer o : p.server.getPlayerList().getPlayers())
            o.sendSystemMessage(Component.literal("☾ " + p.getGameProfile().getName() + " прошёл стадию " + st + " испытания Луны!").withStyle(ChatFormatting.LIGHT_PURPLE));
        if (st >= 3) com.pvpciv.progress.Titles.grant(p, "moon_rival");
        p.heal(p.getMaxHealth() * 0.5f);
        s.stage++;
        s.boss = null;
    }

    /** Смерть игрока на арене — не смерть: возвращаем на станцию. Возвращает true, если смерть отменена. */
    public static boolean onPlayerDeath(ServerPlayer p) {
        Session s = S.remove(p.getUUID());
        if (s == null) return false;
        lose(p, s);
        cleanup(CivWorld.f5(p.server), s);
        return true;
    }

    private static void lose(ServerPlayer p, Session s) {
        p.setHealth(p.getMaxHealth() * 0.3f);
        p.removeAllEffects();
        p.clearFire();
        int done = s.stage - 1;
        StoryData d = Progress.d(p);
        d.gates.putInt("luna_losses", d.gates.getInt("luna_losses") + 1);
        p.setData(ModRegistry.STORY.get(), d);
        if (d.gates.getInt("luna_losses") >= 100) com.pvpciv.progress.Titles.grant(p, "luna_100");
        leave(p, s, done > 0 ? "Испытание окончено на стадии " + s.stage + ". Пройдено: " + done + "." : "Испытание окончено. Луна: «Приходи, когда отдохнёшь».");
    }

    private static void leave(ServerPlayer p, Session s, String msg) {
        s.bar.removeAllPlayers();
        Vec3 back = com.pvpciv.floor.Floor5.LUNA().add(-2, 0, 2);
        p.teleportTo(CivWorld.f5(p.server), back.x, back.y, back.z, 45, 0);
        p.resetFallDistance();
        p.sendSystemMessage(Component.literal("☾ " + msg).withStyle(ChatFormatting.LIGHT_PURPLE));
        for (ItemStack it : s.pending) com.pvpciv.inv.Bag.give(p, it);
        s.pending.clear();
    }

    private static void cleanup(ServerLevel l, Session s) {
        s.bar.removeAllPlayers();
        if (s.boss != null && l.getEntity(s.boss) != null) l.getEntity(s.boss).discard();
        for (var e : l.getEntitiesOfClass(LunaBoss.class, new net.minecraft.world.phys.AABB(center(s.slot)).inflate(R + 20))) e.discard();
    }

    private static void title(ServerPlayer p, String t, String sub, ChatFormatting c) {
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(t).withStyle(c, ChatFormatting.BOLD)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(sub).withStyle(ChatFormatting.GRAY)));
    }

    // ------------------------------------------------------------------ арена
    private static final Set<Integer> BUILT = new HashSet<>();
    private static void build(ServerLevel l, BlockPos c) {
        if (!BUILT.add(c.getX()) && l.getBlockState(c.below()).is(Blocks.CRYING_OBSIDIAN)) return;
        BlockState floor = Blocks.BLACK_CONCRETE.defaultBlockState(), ring = Blocks.PURPLE_STAINED_GLASS.defaultBlockState(), lamp = Blocks.SEA_LANTERN.defaultBlockState();
        for (int x = -R - 2; x <= R + 2; x++) for (int z = -R - 2; z <= R + 2; z++) {
            double r = Math.hypot(x, z);
            if (r > R + 1.5) continue;
            BlockPos q = c.offset(x, -1, z);
            BlockState s = r < 2.5 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Math.abs(r - 7) < 0.5 || Math.abs(r - 12) < 0.5 ? ring : r > R + 0.5 ? lamp : floor;
            l.setBlock(q, s, 2);
            l.setBlock(q.below(), Blocks.OBSIDIAN.defaultBlockState(), 2);
            for (int y = 0; y < 8; y++) l.setBlock(c.offset(x, y, z), r > R + 0.5 ? (y < 6 ? Blocks.BARRIER.defaultBlockState() : Blocks.AIR.defaultBlockState()) : Blocks.AIR.defaultBlockState(), 2);
        }
        // полумесяц из светящихся блоков у края — «трон» Луны
        for (int i = -4; i <= 4; i++) {
            double a = Math.PI / 2 + i * 0.18;
            int x = (int) Math.round(Math.cos(a) * (R - 1)), z = -(int) Math.round(Math.sin(a) * (R - 1));
            l.setBlock(c.offset(x, 0, z), Blocks.END_ROD.defaultBlockState(), 2);
        }
    }
}
