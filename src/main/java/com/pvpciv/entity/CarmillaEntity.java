package com.pvpciv.entity;

import com.pvpciv.game.DialogueManager;
import com.pvpciv.game.Story;
import com.pvpciv.game.Vampire;
import com.pvpciv.registry.ModRegistry;
import com.pvpciv.world.Basement;
import com.pvpciv.world.CivWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

/** Кармилла — вампирша, хозяйка подвала общежития. Сидит на троне, раздаёт кровавые контракты. */
public class CarmillaEntity extends GeoNpc {
    public static final int COLOR = 0xD23A54;
    public static final Vec3 THRONE = new Vec3(Basement.CARMILLA_SPOT.getX() + 0.5, Basement.FY + 0.75, Basement.Z0 + 1.5);   // сидит на сиденье трона (верх сиденья — FY+1, коврик)
    /** Пока занята (уводит должника) — не возвращается на трон. */
    public int busy = 0;
    /** Сколько ещё тиков держать крылья раскрытыми. */
    private int wings = 0;

    public void showWings(int ticks) { wings = Math.max(wings, ticks); }

    public CarmillaEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level, "Кармилла", COLOR);
        setNoGravity(true);
    }

    @Override public String model() { return "carmilla"; }
    @Override protected String idleAnim() { return "sit"; }

    public static CarmillaEntity find(MinecraftServer server) {
        ServerLevel lvl = CivWorld.level(server);
        for (var e : lvl.getAllEntities()) if (e instanceof CarmillaEntity c && c.isAlive()) return c;
        return null;
    }

    public static void ensure(MinecraftServer server) {
        ServerLevel lvl = CivWorld.level(server);
        BlockPos spot = Basement.CARMILLA_SPOT;
        if (!lvl.areEntitiesLoaded(ChunkPos.asLong(spot))) return;
        List<CarmillaEntity> all = lvl.getEntitiesOfClass(CarmillaEntity.class, new AABB(spot).inflate(64));
        for (int i = 1; i < all.size(); i++) all.get(i).discard();
        if (!all.isEmpty() || find(server) != null) return;
        CarmillaEntity c = ModRegistry.CARMILLA.get().create(lvl);
        if (c == null) return;
        c.moveTo(THRONE.x, THRONE.y, THRONE.z, 0, 0);
        lvl.addFreshEntity(c);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (busy > 0) { busy--; return; }
        if (wings > 0) { wings--; setAnim(wings > 0 ? "sit_spread" : ""); }
        else if (tickCount % 400 == 200 && random.nextFloat() < 0.6f) showWings(70);
        if (position().distanceToSqr(THRONE) > 0.3) {
            puff((ServerLevel) level(), position());
            teleportTo(THRONE.x, THRONE.y, THRONE.z);
            setYRot(0); setYBodyRot(0); setYHeadRot(0);
            setDeltaMovement(Vec3.ZERO);
            if (wings <= 0) setAnim("");
        }
        if (tickCount % 200 == 0 && random.nextFloat() < 0.4f)
            ((ServerLevel) level()).sendParticles(new DustParticleOptions(new Vector3f(0.7f, 0.05f, 0.1f), 1.2f), getX(), getY() + 1.4, getZ(), 12, 0.4, 0.5, 0.4, 0);
    }

    public static void puff(ServerLevel l, Vec3 at) {
        l.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 1, at.z, 30, 0.4, 0.8, 0.4, 0.03);
        l.sendParticles(new DustParticleOptions(new Vector3f(0.6f, 0.0f, 0.08f), 1.6f), at.x, at.y + 1, at.z, 40, 0.5, 0.9, 0.5, 0);
        l.playSound(null, BlockPos.containing(at), SoundEvents.BAT_TAKEOFF, SoundSource.HOSTILE, 1f, 0.7f);
    }

    @Override
    protected void onTalk(ServerPlayer p) {
        if (busy > 0) return;
        UUID talker = DialogueManager.talkerOf(this);
        if (talker != null && !talker.equals(p.getUUID())) {
            Story.say(p, "Кармилла", COLOR, "Подожди, сладкий. Я занята другим должником.");
            return;
        }
        if (DialogueManager.inDialogue(p)) return;
        showWings(120);
        level().playSound(null, blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.NEUTRAL, 0.8f, 0.7f);
        DialogueManager.open(p, this, Vampire.dialogue(p, this));
    }

    @Override
    protected void onPunched(ServerPlayer p) {
        String[] lines = {"Ах. Как пылко. Ещё раз — и это будет считаться приглашением.", "Ты бьёшь вампира в её же склепе? Смело. Глупо, но смело.", "Хм-м. Кровь у тебя быстрее, когда ты злишься."};
        Story.say(p, "Кармилла", COLOR, lines[random.nextInt(lines.length)]);
        level().playSound(null, blockPosition(), SoundEvents.WITCH_CELEBRATE, SoundSource.NEUTRAL, 0.8f, 0.7f);
    }

    @Override public boolean shouldBeSaved() { return false; }
}
