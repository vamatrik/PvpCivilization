package com.pvpciv.entity;

import com.pvpciv.game.DialogueManager;
import com.pvpciv.game.Dialogues;
import com.pvpciv.game.Story;
import com.pvpciv.registry.ModRegistry;
import com.pvpciv.world.DormBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
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

import java.util.List;
import java.util.UUID;

/** Луна. Соседка, вечная соперница. Всегда на шаг впереди. */
public class LunaEntity extends StoryNpc {
    public static final int COLOR = 0xFF8AD8;
    private BlockPos home = DormBuilder.LUNA_SPOT;
    private int returnTimer = 0;

    public LunaEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level, "Луна", COLOR);
    }

    /** Следит, чтобы Луна существовала ровно одна (вызывается периодически). */
    public static void ensure(MinecraftServer server) {
        ServerLevel lvl = server.overworld();
        BlockPos spot = DormBuilder.LUNA_SPOT;
        if (!lvl.areEntitiesLoaded(ChunkPos.asLong(spot))) return;
        List<LunaEntity> all = lvl.getEntitiesOfClass(LunaEntity.class, new AABB(spot).inflate(48));
        for (int i = 1; i < all.size(); i++) all.get(i).discard();
        if (!all.isEmpty()) return;
        LunaEntity l = ModRegistry.LUNA.get().create(lvl);
        if (l == null) return;
        l.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0, 0);
        l.home = spot;
        lvl.addFreshEntity(l);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (returnTimer > 0) { returnTimer--; return; }
        if (home != null && distanceToSqr(Vec3.atBottomCenterOf(home)) > 2.0) {
            ServerLevel sl = (ServerLevel) level();
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 1, getZ(), 25, 0.4, 0.6, 0.4, 0.05);
            teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
            setDeltaMovement(Vec3.ZERO);
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 1, getZ(), 25, 0.4, 0.6, 0.4, 0.05);
        }
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
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 1, getZ(), 40, 0.5, 0.6, 0.5, 0.1);
        sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 15, 0.3, 0.5, 0.3, 0.05);
    }

    @Override
    protected void onTalk(ServerPlayer p) {
        UUID talker = DialogueManager.talkerOf(this);
        if (talker != null && !talker.equals(p.getUUID())) {
            Story.say(p, "Луна", COLOR, "Жди очереди! Я тут другого идиота отчитываю.");
            return;
        }
        if (DialogueManager.inDialogue(p)) return;
        DialogueManager.open(p, this, Dialogues.lunaRepeat(p));
    }

    @Override
    protected void onPunched(ServerPlayer p) {
        String[] lines = {"Это что сейчас было? Комарик?", "Ещё раз — и я сделаю тебе Рывок. В Бездну.", "Хи-хи. Щекотно, Пепел."};
        Story.say(p, "Луна", COLOR, lines[random.nextInt(lines.length)]);
        level().playSound(null, blockPosition(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.NEUTRAL, 1f, 1.5f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) tag.put("home", NbtUtils.writeBlockPos(home));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "home").ifPresent(h -> home = h);
    }
}
