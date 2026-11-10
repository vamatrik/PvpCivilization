package com.pvpciv.entity;

import com.pvpciv.game.DialogueManager;
import com.pvpciv.game.Dialogues;
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
import java.util.UUID;

/** Сорока — скупщица хлама в атриуме. */
public class TraderEntity extends StoryNpc {
    public static final int COLOR = 0x8FE3C8;
    public static final BlockPos SPOT = new BlockPos(6, DormBuilder.Y0, 9);

    public TraderEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level, "Сорока • Скупщица", COLOR);
    }

    public static void ensure(MinecraftServer server) {
        ServerLevel lvl = server.overworld();
        if (!lvl.areEntitiesLoaded(ChunkPos.asLong(SPOT))) return;
        List<TraderEntity> all = lvl.getEntitiesOfClass(TraderEntity.class, new AABB(SPOT).inflate(40));
        for (int i = 1; i < all.size(); i++) all.get(i).discard();
        if (!all.isEmpty()) {
            TraderEntity t = all.get(0);
            if (t.distanceToSqr(Vec3.atBottomCenterOf(SPOT)) > 4) t.teleportTo(SPOT.getX() + 0.5, SPOT.getY(), SPOT.getZ() + 0.5);
            return;
        }
        TraderEntity t = com.pvpciv.registry.ModRegistry.TRADER.get().create(lvl);
        if (t == null) return;
        t.moveTo(SPOT.getX() + 0.5, SPOT.getY(), SPOT.getZ() + 0.5, 180, 0);
        lvl.addFreshEntity(t);
    }

    @Override
    protected void onTalk(ServerPlayer p) {
        UUID talker = DialogueManager.talkerOf(this);
        if (talker != null && !talker.equals(p.getUUID())) {
            com.pvpciv.game.Story.say(p, "Сорока", COLOR, "Секунду, у меня тут другой клиент! Не толкайся.");
            return;
        }
        String[] hi = {"О, клиент! Смотри, щупай, покупай!", "Блестяшки принёс? ПКМ по ним — и они мои!", "Новый завоз — каждые восемь часов. Не проспи!"};
        com.pvpciv.game.Story.say(p, "Сорока", COLOR, hi[p.getRandom().nextInt(hi.length)]);
        p.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new com.pvpciv.menu.TraderMenu(id, inv), net.minecraft.network.chat.Component.literal("Сорока")));
        com.pvpciv.game.Shop.sendOffers(p);
    }
}
