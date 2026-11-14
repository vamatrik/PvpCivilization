package com.pvpciv.game;

import com.pvpciv.floor.Floor1;
import com.pvpciv.floor.Floor2;
import com.pvpciv.inv.Bag;
import com.pvpciv.registry.ModRegistry;
import com.pvpciv.world.CivWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Падение с этажа 2 — на этаж 1 с неба. Парашют (в сумке или инвентаре) раскрывается сам при долгом падении. */
public final class Falling {
    public static void tick(ServerPlayer p) {
        if (p.isCreative() || p.isSpectator() || p.getVehicle() instanceof com.pvpciv.entity.LiftCabin) return;
        if (CivWorld.isF4(p.level()) && p.getY() < com.pvpciv.floor.Floor4.Y - 40) {   // 0.11: с этажа 4 — на остров библиотеки
            var to = CivWorld.f3(p.server);
            net.minecraft.world.phys.Vec3 at = landingF3(to, p.getRandom());
            p.teleportTo(to, at.x, at.y, at.z, p.getYRot(), 30);
            p.resetFallDistance();
            p.sendSystemMessage(Component.literal("Ты сорвался с платформы NaZa и летишь вниз, на этаж 3!").withStyle(ChatFormatting.RED));
            return;
        }
        if (CivWorld.isF3(p.level()) && p.getY() < com.pvpciv.floor.Floor3.Y - 40) {
            var to = CivWorld.f2(p.server);
            net.minecraft.world.phys.Vec3 at = landingF2(to, p.getRandom());
            p.teleportTo(to, at.x, at.y, at.z, p.getYRot(), 30);
            p.resetFallDistance();
            p.sendSystemMessage(Component.literal("Ты сорвался с острова библиотеки и летишь вниз, на этаж 2!").withStyle(ChatFormatting.RED));
            return;
        }
        if (CivWorld.isF2(p.level()) && p.getY() < Floor2.YS - 16) {
            var to = CivWorld.level(p.server);
            net.minecraft.world.phys.Vec3 at = landing(to, p.getRandom());
            p.teleportTo(to, at.x, at.y, at.z, p.getYRot(), 30);
            p.resetFallDistance();
            p.sendSystemMessage(Component.literal("Ты сорвался с края Неонового города и летишь вниз, на этаж 1!").withStyle(ChatFormatting.RED));
            return;
        }
        var tag = p.getPersistentData();
        if (p.onGround() || p.isInWater() || p.isPassenger() || p.getAbilities().flying) { tag.remove("chute"); canopy(p, false); return; }
        boolean open = tag.getBoolean("chute");
        if (!open && p.fallDistance > 10 && hasChute(p)) {
            tag.putBoolean("chute", true);
            open = true;
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_ELYTRA.value(), SoundSource.PLAYERS, 1f, 0.8f);
            p.serverLevel().sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 2.5, p.getZ(), 20, 0.8, 0.2, 0.8, 0.01);
            p.displayClientMessage(Component.literal("⛱ Парашют раскрылся!").withStyle(ChatFormatting.GOLD), true);
        }
        if (open) {
            canopy(p, true);
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 30, 0, false, false));
            p.resetFallDistance();
        }
    }

    /**
     * Куда падаешь: случайная точка этажа 1, под которой есть земля (не бездна, не вода и не лава), на 150 блоков выше неё.
     * Раньше — всегда у лагеря Пустоши (бета 12.11).
     */
    /** Падение с этажа 3: случайная улица Неонового города (не крыша небоскрёба). */
    private static net.minecraft.world.phys.Vec3 landingF3(net.minecraft.server.level.ServerLevel l, net.minecraft.util.RandomSource r) {
        int fy = com.pvpciv.floor.Floor3.Y;
        for (int i = 0; i < 60; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = 34 + r.nextDouble() * 14;
            int x = (int) Math.floor(Math.cos(a) * d), z = (int) Math.floor(Math.sin(a) * d);
            int y = l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y < fy - 2 || y > fy + 3) continue;
            return new net.minecraft.world.phys.Vec3(x + 0.5, y + 150, z + 0.5);
        }
        return new net.minecraft.world.phys.Vec3(com.pvpciv.floor.Floor3.LIFT_ARRIVE.x, fy + 150, com.pvpciv.floor.Floor3.LIFT_ARRIVE.z + 38);
    }

    private static net.minecraft.world.phys.Vec3 landingF2(net.minecraft.server.level.ServerLevel l, net.minecraft.util.RandomSource r) {
        for (int i = 0; i < 60; i++) {
            int x = -110 + r.nextInt(221), z = -110 + r.nextInt(221);
            int y = l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y < Floor2.Y - 1 || y > Floor2.Y + 3) continue;
            if (!l.getBlockState(new net.minecraft.core.BlockPos(x, y - 1, z)).getFluidState().isEmpty()) continue;
            return new net.minecraft.world.phys.Vec3(x + 0.5, y + 150, z + 0.5);
        }
        return new net.minecraft.world.phys.Vec3(Floor2.LIFT_ARRIVE.x, Floor2.Y + 150, Floor2.LIFT_ARRIVE.z);
    }

    private static net.minecraft.world.phys.Vec3 landing(net.minecraft.server.level.ServerLevel l, net.minecraft.util.RandomSource r) {
        for (int i = 0; i < 40; i++) {
            int x = -90 + r.nextInt(191), z = -595 + r.nextInt(1280);
            int y = l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y < Floor1.Y - 6 || y > Floor1.Y + 30) continue;   // пустота или крыша/башня
            if (Math.abs(x) < 34 && Math.abs(z) < 34) continue;      // не на крышу общежития
            var ground = l.getBlockState(new net.minecraft.core.BlockPos(x, y - 1, z));
            if (!ground.getFluidState().isEmpty() || ground.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)) continue;
            return new net.minecraft.world.phys.Vec3(x + 0.5, y + 150, z + 0.5);
        }
        return new net.minecraft.world.phys.Vec3(Floor1.CAMP.getX() + 0.5, Floor1.Y + 150, Floor1.CAMP.getZ() + 40.5);
    }

    /** Купол над головой — ItemDisplay с 3D-моделью, едет на игроке (видят все). */
    /** Купол парашюта: свой дисплей на игрока, каждый тик переносится за ним (бета 0.9.6: «модель остаётся в воздухе»). */
    private static final java.util.Map<java.util.UUID, java.util.UUID> CANOPY = new java.util.HashMap<>();

    /** Каждый тик: купола — за своими игроками. */
    public static void follow(net.minecraft.server.MinecraftServer server) {
        if (CANOPY.isEmpty()) return;
        var it = CANOPY.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            net.minecraft.world.entity.Entity d = p == null ? null : p.serverLevel().getEntity(en.getValue());
            if (p == null || d == null) { if (d != null) d.discard(); it.remove(); continue; }
            d.teleportTo(p.getX(), p.getY(), p.getZ());
        }
    }

    public static void canopy(ServerPlayer p, boolean on) {
        var l = p.serverLevel();
        java.util.UUID id = CANOPY.get(p.getUUID());
        net.minecraft.world.entity.Entity cur = id == null ? null : l.getEntity(id);
        if (!on) {
            if (cur != null) cur.discard();
            CANOPY.remove(p.getUUID());
            // подчистить старые «висящие» купола из прошлых версий
            l.getEntitiesOfClass(net.minecraft.world.entity.Display.ItemDisplay.class, p.getBoundingBox().inflate(48), e -> e.getTags().contains("chute_" + p.getUUID())).forEach(net.minecraft.world.entity.Entity::discard);
            return;
        }
        if (cur != null && cur.isAlive()) return;
        var d = net.minecraft.world.entity.EntityType.ITEM_DISPLAY.create(l);
        if (d == null) return;
        d.moveTo(p.getX(), p.getY(), p.getZ());
        d.addTag("chute_" + p.getUUID());
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        d.saveWithoutId(t);
        net.minecraft.nbt.CompoundTag item = new net.minecraft.nbt.CompoundTag();
        item.putString("id", "pvpciv:parachute_canopy"); item.putInt("count", 1);
        t.put("item", item);
        t.putString("item_display", "fixed");
        net.minecraft.nbt.CompoundTag tr = new net.minecraft.nbt.CompoundTag();
        tr.put("translation", floats(0, 2.4f, 0)); tr.put("scale", floats(3.2f, 2.4f, 3.2f));
        tr.put("left_rotation", floats(0, 0, 0, 1)); tr.put("right_rotation", floats(0, 0, 0, 1));
        t.put("transformation", tr);
        t.putInt("teleport_duration", 1);
        d.load(t);
        l.addFreshEntity(d);
        CANOPY.put(p.getUUID(), d.getUUID());
    }

    private static net.minecraft.nbt.ListTag floats(float... v) {
        net.minecraft.nbt.ListTag l = new net.minecraft.nbt.ListTag();
        for (float f : v) l.add(net.minecraft.nbt.FloatTag.valueOf(f));
        return l;
    }

    private static boolean hasChute(ServerPlayer p) {
        return Bag.has(p, s -> s.is(ModRegistry.PARACHUTE.get()));
    }
}
