package com.pvpciv.game;

import com.pvpciv.block.RitualBlock;
import com.pvpciv.block.RitualBlockEntity;
import com.pvpciv.data.StoryData;
import com.pvpciv.floor.Floor5;
import com.pvpciv.item.CivWeaponItem;
import com.pvpciv.loot.Gear;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModF5;
import com.pvpciv.registry.ModRegistry;
import com.pvpciv.registry.ModV11;
import com.pvpciv.world.CivWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.1: Ритуальный зал этажа 5 — за Луной (дверь в северной стене атриума), открывается после Демоницы Пустоты.
 * Ядро-пьедестал в центре и 16 инжекторов: по 4 с каждой стороны, узором x·x / ·x· / x·x (сверху и снизу — инжекторы, в середине — кристалл).
 * Зажги все инжекторы тёмной звездой (касанием), положи оружие +10 на ядро и ПКМ: одна тёмная звезда сгорает,
 * энергия стягивается в центр — взрыв. 50% — +11, 50% — оружие рассыпается в 2–4 осколка темноты.
 */
public final class Ritual {
    private Ritual() {}
    public static final int Y = Floor5.Y;
    public static final int X0 = 5, X1 = 21, Z0 = -30, Z1 = -16;          // стены зала
    public static final BlockPos CENTER = new BlockPos(13, Y, -23);
    public static final double CHANCE = 0.5;
    public static final String GATE = "ritual_open";
    private static final int RISE = 90, BOOM = 120, END = 160;

    public static BlockPos hubOff(int x, int z) { BlockPos c = Floor5.HUB.center(); return c.offset(x, 0, z); }
    public static BlockPos center() { return hubOff(CENTER.getX(), CENTER.getZ()); }

    /** 16 инжекторов: [позиция, направление к ядру]. */
    public static List<BlockPos> injectors() {
        BlockPos c = center();
        List<BlockPos> out = new ArrayList<>();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos side = c.relative(d, 4);
            Direction along = d.getClockWise();
            for (int k = -1; k <= 1; k += 2) for (int h = 0; h <= 2; h += 2) out.add(side.relative(along, k).above(h));
        }
        return out;
    }
    private static Direction toCenter(BlockPos p) {
        BlockPos c = center();
        int dx = c.getX() - p.getX(), dz = c.getZ() - p.getZ();
        return Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
    }

    // ------------------------------------------------------------------ постройка
    public static void build(ServerLevel l) {
        BlockPos h = Floor5.HUB.center();
        int hx = h.getX(), hz = h.getZ();
        BlockState wall = Blocks.NETHER_BRICKS.defaultBlockState(), pillar = Blocks.RED_NETHER_BRICKS.defaultBlockState(),
                floor = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), ceil = Blocks.BLACKSTONE.defaultBlockState(), win = Blocks.RED_STAINED_GLASS.defaultBlockState();
        int top = 8;
        for (int x = X0; x <= X1; x++) for (int z = Z0; z <= Z1; z++) {
            boolean edge = x == X0 || x == X1 || z == Z0 || z == Z1, corner = (x == X0 || x == X1) && (z == Z0 || z == Z1);
            int ax = hx + x, az = hz + z;
            l.setBlock(new BlockPos(ax, Y - 2, az), Blocks.BLACKSTONE.defaultBlockState(), 2);
            double r = Math.hypot(x - CENTER.getX(), z - CENTER.getZ());
            BlockState f = edge ? Blocks.POLISHED_BLACKSTONE.defaultBlockState()
                    : r < 1.6 ? Blocks.GILDED_BLACKSTONE.defaultBlockState()
                    : Math.abs(r - 4) < 0.55 ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                    : Math.abs(r - 6.2) < 0.5 ? Blocks.RED_NETHER_BRICKS.defaultBlockState()
                    : (x + z) % 2 == 0 ? floor : Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
            l.setBlock(new BlockPos(ax, Y - 1, az), f, 2);
            for (int y = 0; y < top; y++) {
                BlockState s = Blocks.AIR.defaultBlockState();
                if (edge) s = corner || (x - X0) % 4 == 0 && (z == Z0 || z == Z1) || (z - Z0) % 4 == 0 && (x == X0 || x == X1) ? pillar
                        : (y == 3 || y == 4) && !(z == Z1) ? win : wall;
                l.setBlock(new BlockPos(ax, Y + y, az), s, 2);
            }
            boolean light = !edge && (x - X0) % 4 == 2 && (z - Z0) % 4 == 2 && r > 2;
            l.setBlock(new BlockPos(ax, Y + top, az), edge ? pillar : light ? Blocks.SHROOMLIGHT.defaultBlockState() : ceil, 2);
            l.setBlock(new BlockPos(ax, Y + top + 1, az), Blocks.BLACKSTONE.defaultBlockState(), 2);
        }
        // дверь из атриума (за лавкой Луны): проём 3×3 в стене атриума и зала
        for (int x = 7; x <= 9; x++) for (int y = 0; y < 3; y++) for (int z = -16; z <= -15; z++)
            l.setBlock(new BlockPos(hx + x, Y + y, hz + z), Blocks.AIR.defaultBlockState(), 2);
        for (int y = 0; y <= 3; y++) for (int x : new int[]{6, 10}) l.setBlock(new BlockPos(hx + x, Y + y, hz - 15), y == 3 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : pillar, 2);
        for (int x = 6; x <= 10; x++) l.setBlock(new BlockPos(hx + x, Y + 3, hz - 15), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 2);
        // висячие фонари душ
        for (int[] o : new int[][]{{8, -19}, {18, -19}, {8, -27}, {18, -27}}) {
            for (int y = 5; y < top; y++) l.setBlock(new BlockPos(hx + o[0], Y + y, hz + o[1]), Blocks.CHAIN.defaultBlockState(), 2);
            l.setBlock(new BlockPos(hx + o[0], Y + 4, hz + o[1]), Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 2);
        }
        // ядро и инжекторы
        BlockPos c = center();
        l.setBlock(c, ModV11.RITUAL_CORE.get().defaultBlockState(), Block.UPDATE_ALL);
        for (BlockPos p : injectors())
            l.setBlock(p, ModV11.RITUAL_INJECTOR.get().defaultBlockState().setValue(RitualBlock.FACING, toCenter(p)), Block.UPDATE_ALL);
        // кристаллы «·x·» между инжекторами
        for (Direction d : Direction.Plane.HORIZONTAL) l.setBlock(c.relative(d, 4).above(1), Blocks.AMETHYST_CLUSTER.defaultBlockState(), 2);
        // таблички
        com.pvpciv.world.Signs.place(l, hubOff(8, -14).above(3), Direction.SOUTH, 5, "РИТУАЛЬНЫЙ ЗАЛ", "только для тех,", "кто изгнал", "Демоницу");
        com.pvpciv.world.Signs.place(l, hubOff(13, -17).above(2), Direction.NORTH, 5, "РИТУАЛ ТЬМЫ", "1) зажги 16 ★тьмой", "2) оружие +10", "3) ПКМ — 50/50");
    }

    /** Админ: перестроить зал и зажечь все инжекторы. */
    public static void debugLight(MinecraftServer s) {
        ServerLevel l = CivWorld.f5(s);
        build(l);
        for (BlockPos p : injectors()) if (l.getBlockEntity(p) instanceof RitualBlockEntity b) { b.lit = true; b.sync(); l.setBlock(p, l.getBlockState(p).setValue(RitualBlock.LIT, true), Block.UPDATE_ALL); }
    }

    // ------------------------------------------------------------------ 1.2: печать тьмы в проёме
    public static final String SEAL = "pvpciv_seal";

    /** Тёмная завеса в двери зала (блок-дисплеи, сквозь неё можно пройти — но без допуска зал выталкивает). */
    public static void ensureSeal(MinecraftServer server) {
        ServerLevel l = CivWorld.f5(server);
        if (l == null) return;
        BlockPos d0 = hubOff(7, -16);
        if (!l.isLoaded(d0) || !l.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(d0))) return;
        var have = l.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new net.minecraft.world.phys.AABB(d0).inflate(6), e -> e.getTags().contains(SEAL));
        Object[][] parts = {
                {Blocks.BLACK_STAINED_GLASS, 0f, 0f, 0.35f, 3f, 3f, 0.12f},
                {Blocks.BLACK_STAINED_GLASS, 0f, 0f, 0.55f, 3f, 3f, 0.12f},
                {Blocks.PURPLE_STAINED_GLASS, 0.15f, 0.15f, 0.75f, 2.7f, 2.7f, 0.08f},
                {Blocks.BLACK_STAINED_GLASS, 0f, 0f, 0.9f, 3f, 3f, 0.12f},
                {Blocks.BLACK_STAINED_GLASS, 0f, 0f, 1.1f, 3f, 3f, 0.12f},
                {Blocks.BLACK_STAINED_GLASS, 0f, 0f, 1.3f, 3f, 3f, 0.12f},
                {Blocks.CRYING_OBSIDIAN, 1.3f, 1.3f, 0.8f, 0.4f, 0.4f, 0.4f},
                {Blocks.SCULK, 0f, 2.8f, 0.35f, 3f, 0.2f, 1.1f},
                {Blocks.SCULK, 0f, 0f, 0.35f, 0.2f, 3f, 1.1f},
                {Blocks.SCULK, 2.8f, 0f, 0.35f, 0.2f, 3f, 1.1f}};
        if (have.size() >= parts.length) return;
        for (var e : have) e.discard();
        for (Object[] p : parts) {
            var e = Rocket.display(l, ((net.minecraft.world.level.block.Block) p[0]).defaultBlockState(), (float) p[4], (float) p[5], (float) p[6]);
            if (e == null) continue;
            e.removeTag("pvpciv_rocket_seat");
            e.addTag(SEAL);
            e.moveTo(d0.getX() + (float) p[1], d0.getY() + (float) p[2], d0.getZ() + (float) p[3], 0, 0);
            l.addFreshEntity(e);
        }
    }

    // ------------------------------------------------------------------ доступ
    public static boolean inside(ServerPlayer p) {
        if (!CivWorld.isF5(p.level())) return false;
        BlockPos h = Floor5.HUB.center();
        double x = p.getX() - h.getX(), z = p.getZ() - h.getZ();
        return x > X0 && x < X1 + 1 && z > Z0 && z < Z1 + 0.3 && p.getY() > Y - 2 && p.getY() < Y + 10;
    }
    public static boolean open(StoryData d) { return d.gates.getInt(GATE) > 0 || (d.trophies & 512) != 0; }

    public static void tick(MinecraftServer server) {
        ServerLevel l = CivWorld.f5(server);
        if (l == null) return;
        if (server.getTickCount() % 10 == 0) for (ServerPlayer p : l.players()) {
            if (!inside(p)) continue;
            StoryData d = Progress.d(p);
            if (open(d)) continue;
            BlockPos b = hubOff(8, -13);
            p.teleportTo(b.getX() + 0.5, Y, b.getZ() + 0.5);
            p.displayClientMessage(Component.literal("Зал запечатан тьмой. Откроется, когда изгонишь Демоницу Пустоты.").withStyle(ChatFormatting.DARK_PURPLE), true);
        }
        if (server.getTickCount() % 100 == 31) ensureSeal(server);
        if (server.getTickCount() % 6 == 0) {   // завеса «дышит» тьмой
            BlockPos d0 = hubOff(8, -15);
            if (l.isLoaded(d0)) l.sendParticles(ParticleTypes.SQUID_INK, d0.getX() + 0.5, d0.getY() + 1.5, d0.getZ() + 0.5, 2, 1.0, 1.0, 0.2, 0.01);
        }
        BlockPos c = center();
        if (!l.isLoaded(c) || !(l.getBlockEntity(c) instanceof RitualBlockEntity core) || core.anim <= 0) return;
        animate(l, core);
    }

    // ------------------------------------------------------------------ взаимодействие
    public static void use(ServerPlayer sp, BlockPos pos, boolean isCore, ItemStack st) {
        ServerLevel l = sp.serverLevel();
        if (!open(Progress.d(sp))) { sp.displayClientMessage(Component.literal("Тьма не слушается тебя. Сначала — Демоница Пустоты.").withStyle(ChatFormatting.DARK_PURPLE), true); return; }
        if (!(l.getBlockEntity(center()) instanceof RitualBlockEntity core)) return;
        if (core.anim > 0) { sp.displayClientMessage(Component.literal("Ритуал уже идёт…").withStyle(ChatFormatting.DARK_PURPLE), true); return; }
        if (!isCore) {
            if (!(l.getBlockEntity(pos) instanceof RitualBlockEntity inj)) return;
            if (inj.lit) { sp.displayClientMessage(Component.literal("Инжектор уже заряжен. Зажжено " + lit(l) + "/16.").withStyle(ChatFormatting.LIGHT_PURPLE), true); return; }
            if (!st.is(ModF5.DARK_STAR.get())) { sp.displayClientMessage(Component.literal("Коснись инжектора тёмной звездой (не тратится; при запуске сгорит одна).").withStyle(ChatFormatting.GRAY), true); return; }
            inj.lit = true; inj.sync();
            l.setBlock(pos, l.getBlockState(pos).setValue(RitualBlock.LIT, true), Block.UPDATE_ALL);
            l.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 0.8f, 0.7f + lit(l) * 0.05f);
            l.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 25, 0.2, 0.2, 0.2, 0.05);
            int n = lit(l);
            sp.displayClientMessage(Component.literal("Инжектор заряжен · " + n + "/16" + (n == 16 ? " — положи оружие +10 на ядро и ПКМ" : "")).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return;
        }
        // ядро
        if (core.item.isEmpty()) {
            if (st.getItem() instanceof CivWeaponItem) {
                int s = Gear.stars(st);
                if (s != Gear.MAX_STARS) { sp.displayClientMessage(Component.literal(s > Gear.MAX_STARS ? "Это уже +11 — предел." : "Ритуал берёт только оружие +10.").withStyle(ChatFormatting.DARK_PURPLE), true); return; }
                core.item = st.split(1); core.owner = sp.getUUID(); core.ownerName = sp.getGameProfile().getName(); core.sync();
                l.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS, 1f, 0.6f);
                sp.displayClientMessage(Component.literal("Оружие на ядре. Зажжено инжекторов: " + lit(l) + "/16. ПКМ по ядру — начать · Shift+ПКМ — забрать.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
                return;
            }
            sp.displayClientMessage(Component.literal("Ядро ритуала: положи оружие +10 (ПКМ оружием). Инжекторов зажжено: " + lit(l) + "/16.").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (core.owner != null && !core.owner.equals(sp.getUUID())) { sp.displayClientMessage(Component.literal("Это оружие положил(а) " + core.ownerName + ".").withStyle(ChatFormatting.RED), true); return; }
        boolean result = !(core.item.getItem() instanceof CivWeaponItem) || Gear.stars(core.item) != Gear.MAX_STARS;   // итог ритуала, пока владельца не было
        if (sp.isShiftKeyDown() || result) {
            com.pvpciv.inv.Bag.give(sp, core.item.copy()); core.item = ItemStack.EMPTY; core.owner = null; core.ownerName = ""; core.sync();
            l.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8f, 1f);
            return;
        }
        int n = lit(l);
        if (n < 16) { sp.displayClientMessage(Component.literal("Зажжено " + n + "/16 инжекторов. Коснись остальных тёмной звездой.").withStyle(ChatFormatting.LIGHT_PURPLE), true); return; }
        if (!takeStar(sp)) { sp.displayClientMessage(Component.literal("Для запуска нужна тёмная звезда — она сгорит.").withStyle(ChatFormatting.DARK_PURPLE), true); return; }
        core.anim = 1; core.sync();
        l.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2f, 0.5f);
        l.playSound(null, pos, SoundEvents.WARDEN_EMERGE, SoundSource.BLOCKS, 1.2f, 1.4f);
        for (ServerPlayer o : l.players()) if (inside(o)) Story.title(o, "РИТУАЛ ТЬМЫ", core.item.getHoverName().getString() + " · 50 на 50", 0xB040FF);
    }

    private static int lit(ServerLevel l) {
        int n = 0;
        for (BlockPos p : injectors()) if (l.getBlockEntity(p) instanceof RitualBlockEntity b && b.lit) n++;
        return n;
    }

    private static boolean takeStar(ServerPlayer sp) {
        for (var hand : net.minecraft.world.InteractionHand.values()) {
            ItemStack h = sp.getItemInHand(hand);
            if (h.is(ModF5.DARK_STAR.get())) { h.shrink(1); return true; }
        }
        var bag = com.pvpciv.inv.Bag.get(sp);
        for (int i = 0; i < bag.getSlots(); i++) if (bag.getStackInSlot(i).is(ModF5.DARK_STAR.get())) { bag.extractItem(i, 1, false); return true; }
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            ItemStack s = sp.getInventory().getItem(i);
            if (s.is(ModF5.DARK_STAR.get())) { s.shrink(1); return true; }
        }
        return false;
    }

    // ------------------------------------------------------------------ анимация
    private static final DustParticleOptions DARK = new DustParticleOptions(new Vector3f(0.45f, 0.05f, 0.75f), 1.6f);

    private static void animate(ServerLevel l, RitualBlockEntity core) {
        int t = ++core.anim;
        BlockPos c = center();
        Vec3 mid = Vec3.atBottomCenterOf(c).add(0, 1.6 + Math.min(1.0, t / (double) RISE) * 1.2, 0);
        List<BlockPos> inj = injectors();
        if (t % 5 == 0) core.sync();
        if (t < BOOM) {
            // лучи от инжекторов к центру: бегущие сгустки
            for (int i = 0; i < inj.size(); i++) {
                BlockPos p = inj.get(i);
                if (!(l.getBlockEntity(p) instanceof RitualBlockEntity b) || !b.lit) continue;
                Vec3 tip = Vec3.atCenterOf(p).add(Vec3.atLowerCornerOf(toCenter(p).getNormal()).scale(0.6));
                double ph = ((t * 0.06) + i * 0.13) % 1.0;
                for (int k = 0; k < 3; k++) {
                    Vec3 q = tip.lerp(mid, (ph + k / 3.0) % 1.0);
                    l.sendParticles(DARK, q.x, q.y, q.z, 1, 0, 0, 0, 0);
                }
                if (t % 4 == 0) l.sendParticles(ParticleTypes.REVERSE_PORTAL, tip.x, tip.y, tip.z, 2, 0.05, 0.05, 0.05, 0.01);
                // инжекторы гаснут один за другим — энергия ушла в ядро
                if (t == 30 + i * 4) {
                    b.lit = false; b.sync();
                    l.setBlock(p, l.getBlockState(p).setValue(RitualBlock.LIT, false), Block.UPDATE_ALL);
                    l.playSound(null, p, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 0.6f, 1.2f);
                }
            }
            // сжатие в центре
            for (int k = 0; k < 3; k++) {
                double a = t * 0.25 + k * 2.09, rr = Math.max(0.2, 2.6 - t / (double) BOOM * 2.4);
                l.sendParticles(ParticleTypes.PORTAL, mid.x + Math.cos(a) * rr, mid.y + Math.sin(t * 0.1 + k) * 0.3, mid.z + Math.sin(a) * rr, 2, 0, 0, 0, 0);
            }
            if (t > 60) l.sendParticles(ParticleTypes.SCULK_SOUL, mid.x, mid.y, mid.z, 1, 0.2, 0.2, 0.2, 0.01);
            if (t % 20 == 0) l.playSound(null, c, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 2f, 0.5f + t / 200f);
            if (t == RISE) l.playSound(null, c, SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.BLOCKS, 2f, 0.8f);
            return;
        }
        if (t == BOOM) {
            l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, mid.x, mid.y, mid.z, 2, 0.3, 0.3, 0.3, 0);
            l.sendParticles(ParticleTypes.SONIC_BOOM, mid.x, mid.y, mid.z, 1, 0, 0, 0, 0);
            l.sendParticles(ParticleTypes.DRAGON_BREATH, mid.x, mid.y, mid.z, 120, 1.5, 0.8, 1.5, 0.05);
            l.playSound(null, c, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 2f, 0.6f);
            l.playSound(null, c, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.BLOCKS, 2f, 0.7f);
            outcome(l, core);
            return;
        }
        if (t < END) {
            if (t % 3 == 0) l.sendParticles(ParticleTypes.SMOKE, mid.x, mid.y - 0.5, mid.z, 4, 0.4, 0.2, 0.4, 0.01);
            return;
        }
        core.anim = 0; core.sync();
    }

    private static void outcome(ServerLevel l, RitualBlockEntity core) {
        ServerPlayer p = core.owner == null ? null : l.getServer().getPlayerList().getPlayer(core.owner);
        ItemStack w = core.item;
        boolean ok = l.random.nextDouble() < CHANCE;
        String name = w.getHoverName().getString(), who = core.ownerName;
        java.util.UUID owner = core.owner;
        core.item = ItemStack.EMPTY; core.owner = null; core.ownerName = "";
        if (ok) {
            Gear.setStars(w, Gear.MAX_STARS + 1);
            l.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, center().getX() + 0.5, Y + 2.5, center().getZ() + 0.5, 80, 0.6, 0.8, 0.6, 0.4);
            l.playSound(null, center(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.5f, 0.8f);
            if (p != null) { com.pvpciv.inv.Bag.give(p, w); Story.title(p, "+11!", "оружие впитало тьму", 0xD060FF); }
            else { core.item = w; core.owner = owner; core.ownerName = who; }
            for (ServerPlayer o : l.getServer().getPlayerList().getPlayers())
                o.sendSystemMessage(Component.literal("☾ " + who + ": ритуал тьмы удался — «" + name + "» теперь +11!").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            int n = 2 + l.random.nextInt(3);
            l.sendParticles(ParticleTypes.SQUID_INK, center().getX() + 0.5, Y + 2.2, center().getZ() + 0.5, 60, 0.5, 0.5, 0.5, 0.05);
            if (p == null) { core.item = new ItemStack(ModV11.DARK_SHARD.get(), n); core.owner = owner; core.ownerName = who; }
            else {
                com.pvpciv.inv.Bag.give(p, new ItemStack(ModV11.DARK_SHARD.get(), n));
                Story.title(p, "Провал", "оружие рассыпалось · осколок темноты ×" + n, 0x8040A0);
            }
            for (ServerPlayer o : l.getServer().getPlayerList().getPlayers())
                o.sendSystemMessage(Component.literal("☾ " + who + ": ритуал тьмы поглотил «" + name + "». Остались осколки…").withStyle(ChatFormatting.DARK_PURPLE));
        }
        core.sync();
    }
}
