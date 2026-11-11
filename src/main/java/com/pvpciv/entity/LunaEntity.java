package com.pvpciv.entity;

import com.pvpciv.data.StoryData;
import com.pvpciv.game.DialogueManager;
import com.pvpciv.game.Dialogues;
import com.pvpciv.game.Story;
import com.pvpciv.progress.Progress;
import com.pvpciv.progress.Titles;
import com.pvpciv.registry.ModRegistry;
import com.pvpciv.world.CivWorld;
import com.pvpciv.world.DormBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Луна. Соседка, вечная соперница. Живёт по расписанию: стоит у своей двери (руки скрещены), гуляет по атриуму,
 * уходит к себе в комнату посидеть на кровати и снова выходит. Пока дверь открыта — можно проскочить внутрь:
 * там её дневник и шкатулка. Поймает внутри — выставит за дверь.
 */
public class LunaEntity extends GeoNpc {
    public static final int COLOR = 0xFF8AD8;
    private enum St { OUTSIDE, STROLL, BACK, ENTER, INSIDE, EXIT }
    private static final Vec3[] STROLL = {new Vec3(4.5, 100, 4.5), new Vec3(-9.5, 100, 9.5), new Vec3(10.5, 100, -9.5), new Vec3(-2.5, 100, -14.5), new Vec3(3.5, 100, 14.5)};
    private static final Vec3 OUT = Vec3.atBottomCenterOf(DormBuilder.LUNA_SPOT), DOOR_OUT = new Vec3(-3.5, 100, -23.5),
            DOOR_IN = new Vec3(-3.5, 100, -26.5), BED = Vec3.atBottomCenterOf(DormBuilder.LUNA_BED);
    private St st = St.OUTSIDE;
    private int timer = 20 * 40, stuck = 0, catchTimer = -1;
    private Vec3 goal = null, lastPos = Vec3.ZERO;
    private UUID caught = null;
    private int returnTimer = 0;

    public LunaEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level, "Луна", COLOR);
    }

    @Override public String model() { return "luna"; }
    @Override public String texture() { return "luna_hd"; }
    @Override public boolean shouldBeSaved() { return false; }

    /** Следит, чтобы Луна существовала ровно одна (вызывается периодически). */
    public static void ensure(MinecraftServer server) {
        ServerLevel lvl = CivWorld.level(server);
        BlockPos spot = DormBuilder.LUNA_SPOT;
        if (!lvl.areEntitiesLoaded(ChunkPos.asLong(spot))) return;
        List<LunaEntity> all = lvl.getEntitiesOfClass(LunaEntity.class, new AABB(spot).inflate(64));
        for (int i = 1; i < all.size(); i++) all.get(i).discard();
        if (!all.isEmpty()) return;
        LunaEntity l = ModRegistry.LUNA.get().create(lvl);
        if (l == null) return;
        l.moveTo(OUT.x, OUT.y, OUT.z, 180, 0);
        lvl.addFreshEntity(l);
    }

    public static boolean inRoom(Vec3 p) {
        return p.x >= -6 && p.x < -1 && p.z >= -30.9 && p.z < -25.9 && p.y >= 99.5 && p.y < 104;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        ServerLevel sl = (ServerLevel) level();
        if (returnTimer > 0) { returnTimer--; return; }
        boolean lunaInside = inRoom(position());
        // незваные гости
        for (ServerPlayer p : sl.players()) {
            if (!inRoom(p.position()) || p.isSpectator()) continue;
            if (!Titles.has(p, "uninvited") && tickCount % 10 == 0) {
                Titles.grant(p, "uninvited");
                Story.say(p, "Луна", COLOR, lunaInside ? "…" : "(Ты в комнате Луны. Тут пахнет вишней и опасностью. Дневник на кафедре, шкатулка у окна.)");
            }
            if (lunaInside && caught == null && catchTimer < 0) startCatch(p);
        }
        if (catchTimer >= 0) { tickCatch(sl); return; }

        timer--;
        switch (st) {
            case OUTSIDE -> {
                setAnim("cross");
                hold(OUT, 180);
                if (timer <= 0) {
                    if (random.nextFloat() < 0.45f) { st = St.ENTER; walk(DOOR_OUT); }
                    else { st = St.STROLL; walk(STROLL[random.nextInt(STROLL.length)]); timer = 20 * (12 + random.nextInt(10)); }
                }
            }
            case STROLL -> {
                setAnim(arrived() ? "" : "");
                if (arrived() && timer > 0) getNavigation().stop();
                if (timer <= 0) { st = St.BACK; walk(OUT); }
                else if (!arrived()) followGoal(sl);
            }
            case BACK -> {
                setAnim("");
                if (arrived()) { st = St.OUTSIDE; timer = 20 * (40 + random.nextInt(50)); }
                else followGoal(sl);
            }
            case ENTER -> {
                setAnim("");
                if (goal == DOOR_OUT && arrived()) { openDoor(sl); walk(BED); }
                else if (goal == BED && arrived()) { st = St.INSIDE; timer = 20 * (35 + random.nextInt(35)); }
                else followGoal(sl);
            }
            case INSIDE -> {
                setAnim("sit");
                hold(BED, 90);
                if (timer <= 0) { st = St.EXIT; walk(DOOR_IN); }
            }
            case EXIT -> {
                setAnim("");
                if (goal == DOOR_IN && arrived()) { openDoor(sl); walk(OUT); }
                else if (goal == OUT && arrived()) { st = St.OUTSIDE; timer = 20 * (40 + random.nextInt(50)); }
                else followGoal(sl);
            }
        }
    }

    private void walk(Vec3 to) {
        goal = to; stuck = 0;
        getNavigation().moveTo(to.x, to.y, to.z, 0.9);
    }

    private boolean arrived() { return goal != null && position().distanceToSqr(goal) < 0.6; }

    /** Идёт к цели; застряла на 3 секунды — «Рывок» прямо туда (с лепестками). */
    private void followGoal(ServerLevel sl) {
        if (goal == null) return;
        if (getNavigation().isDone()) getNavigation().moveTo(goal.x, goal.y, goal.z, 0.9);
        if (position().distanceToSqr(lastPos) < 0.002) stuck++; else stuck = 0;
        lastPos = position();
        if (stuck > 60) { petals(sl); teleportTo(goal.x, goal.y, goal.z); petals(sl); stuck = 0; }
    }

    private void hold(Vec3 at, float yaw) {
        getNavigation().stop();
        if (position().distanceToSqr(at) > 0.5) teleportTo(at.x, at.y, at.z);
        if (getTarget() == null && tickCount % 40 == 0 && level().getNearestPlayer(this, 6) == null) { setYRot(yaw); setYBodyRot(yaw); }
    }

    private void openDoor(ServerLevel sl) {
        BlockPos low = DormBuilder.LUNA_DOOR;
        BlockState s = sl.getBlockState(low);
        if (s.getBlock() instanceof DoorBlock door && !s.getValue(DoorBlock.OPEN)) {
            door.setOpen(null, sl, s, low, true);
            sl.scheduleTick(low, door, 100);   // 5 секунд, чтобы успеть проскочить
            sl.playSound(null, low, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.6f, 1.6f);
        }
    }

    private void petals(ServerLevel sl) {
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 1, getZ(), 25, 0.4, 0.6, 0.4, 0.05);
    }

    // ------------------------------------------------------------------ поймала!
    private void startCatch(ServerPlayer p) {
        caught = p.getUUID();
        catchTimer = 40;
        getNavigation().stop();
        lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, p.getEyePosition());
        setAnim("cross");
        String[] lines = {"…ЧТО. ТЫ. ДЕЛАЕШЬ. В МОЕЙ КОМНАТЕ?!", "Ты… ты рылся в моих вещах?! Бака! Бака-бака-бака!", "Опять ты?! Я же сменила замок! …Как ты вообще сюда попал?!"};
        StoryData d = Progress.d(p);
        Story.say(p, "Луна", COLOR, lines[Math.min(lines.length - 1, d.lunaCaught)]);
        level().playSound(null, blockPosition(), SoundEvents.ALLAY_HURT, SoundSource.NEUTRAL, 1f, 1.4f);
    }

    private void tickCatch(ServerLevel sl) {
        ServerPlayer p = caught == null ? null : (ServerPlayer) sl.getPlayerByUUID(caught);
        if (p == null) { catchTimer = -1; caught = null; return; }
        lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, p.getEyePosition());
        if (--catchTimer > 0) return;
        // выставить за дверь с «Рывком»
        StoryData d = Progress.d(p);
        d.lunaCaught++;
        boolean plush = false;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(ModRegistry.LUNA_PLUSH.get())) { inv.setItem(i, ItemStack.EMPTY); plush = true; }
        p.setData(ModRegistry.STORY.get(), d);
        petals(sl);
        openDoor(sl);
        p.teleportTo(sl, -3.5, 100, -21.5, 0, 0);
        p.setDeltaMovement(new Vec3(0, 0.45, 1.1));
        p.hurtMarked = true;
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, p.getX(), p.getY() + 1, p.getZ(), 40, 0.5, 0.6, 0.5, 0.1);
        sl.playSound(null, p.blockPosition(), SoundEvents.BREEZE_JUMP, SoundSource.NEUTRAL, 1f, 1.3f);
        Story.title(p, "БАКА!", plush ? "Луна отобрала Плюшевого Пепла" : "Луна выставила тебя за дверь", COLOR);
        if (plush) Story.say(p, "Луна", COLOR, "И ЭТО МОЁ! …Я его не тискаю. Он просто… лежит.");
        catchTimer = -1; caught = null;
        setAnim("");
    }

    // ------------------------------------------------------------------ диалоги
    @Override
    protected void onTalk(ServerPlayer p) {
        UUID talker = DialogueManager.talkerOf(this);
        if (talker != null && !talker.equals(p.getUUID())) {
            Story.say(p, "Луна", COLOR, "Жди очереди! Я тут другого идиота отчитываю.");
            return;
        }
        if (DialogueManager.inDialogue(p)) return;
        if (st == St.INSIDE) { Story.say(p, "Луна", COLOR, "Я ОТДЫХАЮ. У себя. Без тебя. Уходи."); return; }
        DialogueManager.open(p, this, Dialogues.lunaRepeat(p));
    }

    @Override
    protected void onPunched(ServerPlayer p) {
        String[] lines = {"Это что сейчас было? Комарик?", "Ещё раз — и я сделаю тебе Рывок. В Бездну.", "Хи-хи. Щекотно, Пепел."};
        Story.say(p, "Луна", COLOR, lines[random.nextInt(lines.length)]);
        level().playSound(null, blockPosition(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.NEUTRAL, 1f, 1.5f);
    }

    /** «Рывок II» — показательный, с лепестками. */
    public void showOffDash(ServerPlayer watcher) {
        ServerLevel sl = (ServerLevel) level();
        Vec3 dir = watcher.position().subtract(position()).multiply(1, 0, 1).normalize();
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        setDeltaMovement(side.scale(1.8).add(0, 0.45, 0));
        hurtMarked = true;
        returnTimer = 45;
        sl.playSound(null, blockPosition(), SoundEvents.BREEZE_JUMP, SoundSource.NEUTRAL, 1.0f, 1.3f);
        petals(sl);
        sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 15, 0.3, 0.5, 0.3, 0.05);
    }
}
