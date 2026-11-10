package com.pvpciv.entity;

import com.pvpciv.data.CivData;
import com.pvpciv.game.DialogueManager;
import com.pvpciv.game.Dialogues;
import com.pvpciv.game.Story;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/** Охранник Тимофей Глыба. Приходит к новичку, стучит в дверь, объясняет правила и уходит. */
public class GuardEntity extends StoryNpc {
    public static final int COLOR = 0x6FA8FF;
    private UUID target;
    private BlockPos doorPos;
    private int knock = 40, cooldown = 0, leave = -1, tries = 0;

    public GuardEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level, "Тимофей Глыба", COLOR);
    }

    public static void ensureFor(ServerPlayer p, CivData.Apartment apt) {
        ServerLevel lvl = p.serverLevel();
        for (GuardEntity g : lvl.getEntitiesOfClass(GuardEntity.class, new AABB(apt.door).inflate(12)))
            if (p.getUUID().equals(g.target)) return;
        GuardEntity g = ModRegistry.GUARD.get().create(lvl);
        if (g == null) return;
        int ox = apt.sign.getX() - apt.door.getX(), oz = apt.sign.getZ() - apt.door.getZ();
        double x = apt.sign.getX() + ox + 0.5, z = apt.sign.getZ() + oz + 0.5;
        float yaw = (float) (Math.toDegrees(Math.atan2(-(apt.door.getX() - x + 0.5), apt.door.getZ() - z + 0.5)));
        g.moveTo(x, apt.door.getY(), z, yaw, 0);
        g.setYHeadRot(yaw);
        g.target = p.getUUID();
        g.doorPos = apt.door;
        lvl.addFreshEntity(g);
    }

    @Override public boolean shouldBeSaved() { return false; }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        ServerPlayer p = target == null ? null : (ServerPlayer) level().getPlayerByUUID(target);
        if (p == null) { if (tickCount > 40) discard(); return; }
        if (leave >= 0) {
            if (--leave <= 0) {
                ((ServerLevel) level()).sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 20, 0.3, 0.6, 0.3, 0.02);
                discard();
            }
            return;
        }
        if (Story.data(p).stage > Story.NEW) { leave = 60; return; }
        this.getLookControl().setLookAt(p);
        if (cooldown > 0) cooldown--;
        double dist = distanceTo(p);
        if (dist <= 4.0 && cooldown == 0 && !DialogueManager.inDialogue(p)) {
            cooldown = 120;
            DialogueManager.open(p, this, Dialogues.guardIntro(this));
        } else if (dist > 4.0 && --knock <= 0) {
            knock = 120;
            if (doorPos != null)
                level().playSound(null, doorPos, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.NEUTRAL, 0.35f, 1.4f);
            String[] msg = {
                    "Тук-тук! Новенький, открывай! Охрана!",
                    "Эй! Я знаю, что ты там. Ключ-карта у тебя в инвентаре, ПКМ по двери!",
                    "Я тут вообще-то бутерброд недоел. Открывай уже.",
                    "...Ты там уснул, что ли? Опять?"
            };
            Story.say(p, "Тимофей", COLOR, msg[Math.min(tries++, msg.length - 1)]);
        }
    }

    public void leaveSoon() { leave = 50; }

    @Override
    protected void onTalk(ServerPlayer p) {
        if (Story.data(p).stage == Story.NEW && !p.getUUID().equals(target)) {
            Story.say(p, "Тимофей", COLOR, "Я не к тебе, у меня тут другой новенький. Иди спи.");
            return;
        }
        if (Story.data(p).stage == Story.NEW && !DialogueManager.inDialogue(p))
            DialogueManager.open(p, this, Dialogues.guardIntro(this));
    }

    @Override
    protected void onPunched(ServerPlayer p) {
        Story.say(p, "Тимофей", COLOR, "Ай! Нападение на охрану — минус сто очков социального рейтинга. Шучу. Наверное.");
    }
}
