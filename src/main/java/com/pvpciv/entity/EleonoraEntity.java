package com.pvpciv.entity;

import com.pvpciv.game.Story;
import com.pvpciv.registry.ModRegistry;
import com.pvpciv.world.CivWorld;
import com.pvpciv.world.DormBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Элеонора — охотница на вампиров. Пока просто стоит на 3-м этаже общежития и следит за атриумом. */
public class EleonoraEntity extends GeoNpc {
    public static final int COLOR = 0xE3C27A;

    public EleonoraEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level, "Элеонора", COLOR);
    }

    @Override public String model() { return "eleonora"; }

    public static void ensure(MinecraftServer server) {
        ServerLevel lvl = CivWorld.level(server);
        BlockPos spot = DormBuilder.ELEONORA_SPOT;
        if (!lvl.areEntitiesLoaded(ChunkPos.asLong(spot))) return;
        List<EleonoraEntity> all = lvl.getEntitiesOfClass(EleonoraEntity.class, new AABB(spot).inflate(48));
        for (int i = 1; i < all.size(); i++) all.get(i).discard();
        if (!all.isEmpty()) return;
        EleonoraEntity e = ModRegistry.ELEONORA.get().create(lvl);
        if (e == null) return;
        e.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 180, 0);
        lvl.addFreshEntity(e);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        Vec3 home = Vec3.atBottomCenterOf(DormBuilder.ELEONORA_SPOT);
        if (position().distanceToSqr(home) > 2) { teleportTo(home.x, home.y, home.z); setDeltaMovement(Vec3.ZERO); }
        if (tickCount % 40 == 0 && level().getNearestPlayer(this, 6) == null) { setYRot(180); setYBodyRot(180); setYHeadRot(180); }
    }

    @Override
    protected void onTalk(ServerPlayer p) {
        String[] lines = {
                "Не сейчас. Я выслеживаю кое-кого. ...От тебя пахнет подвалом.",
                "Серебро, святая вода, осиновый кол. Всё при мне. Не хватает только приказа.",
                "Ты видел кого-нибудь с крыльями и плохим характером? Тогда держись от неё подальше.",
                "Комендант говорит, внизу «просто склад». Склады не пьют кровь, новенький.",
        };
        Story.say(p, "Элеонора", COLOR, lines[random.nextInt(lines.length)]);
    }

    @Override
    protected void onPunched(ServerPlayer p) {
        Story.say(p, "Элеонора", COLOR, "Ещё раз — и проверю, не вампир ли ты. Колом.");
    }

    @Override public boolean shouldBeSaved() { return false; }
}
