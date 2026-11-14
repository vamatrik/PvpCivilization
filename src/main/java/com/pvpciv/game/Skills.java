package com.pvpciv.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pvpciv.data.StoryData;
import com.pvpciv.net.Network;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Predicate;

/**
 * 0.10.5 — навыки по слотам. У каждого слота свой тип и свой список; в меню (Герой → «Навыки») выбирается, что стоит в слоте.
 *  Слот 1 (R) — перемещение, слот 2 (с ур. 10) — боевые, слот 3 (с ур. 35) — особые (появятся на следующих этажах).
 */
public final class Skills {
    private Skills() {}

    public record Skill(String id, int slot, String name, String icon, String desc, int cd, String how, Predicate<StoryData> open) {}

    public static final List<Skill> ALL = List.of(
            new Skill("dash", 1, "Рывок", "»", "Резкий рывок вперёд по взгляду.", 8, "Есть с начала", d -> true),
            new Skill("dash2", 1, "Рывок II", "»»", "Рывок дальше и быстрее. Перезарядка 6 с.", 6, "Стелла: 1000 рывков с её прибором", d -> d.gates.getBoolean("dash2")),
            new Skill("backstep", 1, "Отскок", "«", "Прыжок назад; 0.5 с неуязвимости к ударам.", 8, "С 5 уровня", d -> d.level >= 5),
            new Skill("touch", 2, "Касание убийцы", "✦", "6 с: +40% шанс и +50% урон крита Юки, скорость.", 45, "Юки — в конце охоты", d -> d.touchSkill),
            new Skill("whirl", 2, "Вихрь", "⟳", "Удар по кругу оружием в руке: ×1.5 урона всем рядом.", 12, "С 10 уровня", d -> d.level >= 10)
    );
    public static final int[] SLOT_LEVEL = {1, 10, 35};
    public static final String[] SLOT_TYPE = {"Перемещение", "Боевые", "Особые"};

    public static Skill byId(String id) { for (Skill s : ALL) if (s.id.equals(id)) return s; return null; }

    public static String selected(StoryData d, int slot) { return slot == 1 ? d.skill1 : slot == 2 ? d.skill2 : d.skill3; }

    /** Выбрать навык в слот (из меню). */
    public static void choose(ServerPlayer p, int slot, String id) {
        StoryData d = Progress.d(p);
        Skill s = byId(id);
        if (s == null || s.slot != slot || !s.open.test(d)) return;
        if (slot == 1) d.skill1 = id; else if (slot == 2) d.skill2 = id; else d.skill3 = id;
        p.setData(ModRegistry.STORY.get(), d);
        sync(p);
    }

    private static final java.util.Map<String, Long> CD = new java.util.HashMap<>();

    /** Нажата клавиша слота. */
    public static void use(ServerPlayer p, int slot) {
        StoryData d = Progress.d(p);
        if (d.level < SLOT_LEVEL[slot - 1]) { p.displayClientMessage(Component.literal("Слот навыка " + slot + " откроется на " + SLOT_LEVEL[slot - 1] + " уровне.").withStyle(ChatFormatting.GRAY), true); return; }
        String id = selected(d, slot);
        if (slot == 2 && id.isEmpty()) id = d.touchSkill ? "touch" : "whirl";   // по умолчанию — что есть
        Skill s = byId(id);
        if (s == null || !s.open.test(d)) { p.displayClientMessage(Component.literal("Слот " + slot + " пуст — выбери навык: Герой → «Навыки».").withStyle(ChatFormatting.GRAY), true); return; }
        if (id.equals("touch")) { Assassin.touch(p); return; }   // у «Касания» своя перезарядка
        String key = p.getUUID() + ":" + slot;
        long now = System.currentTimeMillis();
        if (now < CD.getOrDefault(key, 0L)) return;
        CD.put(key, now + s.cd * 1000L);
        switch (id) {
            case "dash" -> { DashSkill.perform(p); com.pvpciv.game.Stella.onDash(p); }
            case "dash2" -> { DashSkill.perform(p, 1.6); com.pvpciv.game.Stella.onDash(p); }
            case "backstep" -> backstep(p);
            case "whirl" -> whirl(p);
            default -> {}
        }
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(11, "{\"slot\":" + slot + ",\"cd\":" + s.cd + "}"));
    }

    private static void backstep(ServerPlayer p) {
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1e-4) flat = Vec3.directionFromRotation(0, p.getYRot());
        Vec3 v = flat.normalize().scale(-1.15).add(0, p.onGround() ? 0.38 : 0.12, 0);
        p.setDeltaMovement(v);
        p.hurtMarked = true;
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(p));
        p.fallDistance = 0;
        p.invulnerableTime = 10;
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 10, 4, false, false));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 0.6f, 1.3f);
        p.serverLevel().sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.3, p.getZ(), 10, 0.3, 0.1, 0.3, 0.02);
    }

    private static void whirl(ServerPlayer p) {
        var st = p.getMainHandItem();
        double dmg = st.getItem() instanceof com.pvpciv.item.CivWeaponItem w ? w.damage(st) * 1.5 : p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 1.5;
        var lvl = p.serverLevel();
        int n = 0;
        for (var e : lvl.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, p.getBoundingBox().inflate(3.6), x -> x != p && x.isAlive()
                && (x instanceof net.minecraft.world.entity.monster.Enemy || com.pvpciv.combat.Mobs.level(x) > 0))) {
            if (p.distanceToSqr(e) > 3.6 * 3.6) continue;
            e.invulnerableTime = 0;
            e.hurt(p.damageSources().playerAttack(p), (float) dmg);
            Vec3 k = e.position().subtract(p.position()).normalize().scale(0.5);
            e.push(k.x, 0.15, k.z);
            n++;
        }
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12;
            lvl.sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX() + Math.cos(a) * 2, p.getY(0.5), p.getZ() + Math.sin(a) * 2, 1, 0, 0, 0, 0);
        }
        lvl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.7f);
        if (n > 0) lvl.playSound(null, p.blockPosition(), ModRegistry.FX_IMPACT.get(), SoundSource.PLAYERS, 0.6f, 1.3f);
    }

    /** На клиент: список навыков, что открыто, что выбрано. */
    public static void sync(ServerPlayer p) {
        StoryData d = Progress.d(p);
        JsonObject j = new JsonObject();
        JsonArray sel = new JsonArray();
        for (int i = 1; i <= 3; i++) {
            String s = selected(d, i);
            if (i == 2 && s.isEmpty()) s = d.touchSkill ? "touch" : d.level >= 10 ? "whirl" : "";
            sel.add(s);
        }
        j.add("sel", sel);
        JsonArray all = new JsonArray();
        for (Skill s : ALL) {
            JsonObject o = new JsonObject();
            o.addProperty("id", s.id); o.addProperty("slot", s.slot); o.addProperty("name", s.name); o.addProperty("icon", s.icon);
            o.addProperty("desc", s.desc); o.addProperty("cd", s.cd); o.addProperty("how", s.how); o.addProperty("open", s.open.test(d));
            all.add(o);
        }
        j.add("all", all);
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(18, j.toString()));
    }
}
