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
        if (com.pvpciv.game.DialogueManager.inDialogue(p)) return;
        var d = com.pvpciv.progress.Progress.d(p);
        if (d.liftOk && d.eleStage < 2) { com.pvpciv.game.DialogueManager.open(p, this, quest(p, d)); return; }
        if (d.eleStage >= 2) { com.pvpciv.game.DialogueManager.open(p, this, com.pvpciv.game.Marks.dialogue(p, this)); return; }
        String[] lines = {
                "Не сейчас. Я выслеживаю кое-кого. ...От тебя пахнет подвалом.",
                "Серебро, святая вода, осиновый кол. Всё при мне. Не хватает только приказа.",
                "Ты видел кого-нибудь с крыльями и плохим характером? Тогда держись от неё подальше.",
                "Комендант говорит, внизу «просто склад». Склады не пьют кровь, новенький.",
        };
        Story.say(p, "Элеонора", COLOR, lines[random.nextInt(lines.length)]);
    }

    private com.pvpciv.game.DialogueManager.Dialogue quest(ServerPlayer p, com.pvpciv.data.StoryData d) {
        var dl = new com.pvpciv.game.DialogueManager.Dialogue("a");
        if (d.eleStage == 0) {
            dl.node("a", "Элеонора", COLOR, "Ты был на втором этаже? Значит, слышал про Крысиного короля. Вампиры — те хотя бы честные: пьют кровь и не скрывают. "
                            + "А эта тварь гноит город изнутри, разносит заразу по стокам. Он хуже вампиров, понимаешь?",
                            com.pvpciv.game.DialogueManager.opt("Что от меня нужно?", "b"))
              .node("b", "Элеонора", COLOR, "Убей его. Тронный сток, запад канализации. Принеси мне весть о его смерти — и я сделаю тебя своим.",
                      pp -> { var dd = com.pvpciv.progress.Progress.d(pp); dd.eleStage = 1; pp.setData(com.pvpciv.registry.ModRegistry.STORY.get(), dd);
                          pp.sendSystemMessage(net.minecraft.network.chat.Component.literal("✦ Квест Элеоноры: убей Крысиного короля (канализация этажа 2).").withStyle(net.minecraft.ChatFormatting.AQUA)); },
                      com.pvpciv.game.DialogueManager.opt("Сделаю", null));
        } else if ((d.trophies & 8) == 0) {
            dl.node("a", "Элеонора", COLOR, "Крысиный король ещё жив. Я чую его отсюда. Тронный сток, запад канализации.", com.pvpciv.game.DialogueManager.opt("Иду", null));
        } else {
            dl.node("a", "Элеонора", COLOR, "Он мёртв. Я знала, что ты справишься. Держи — мой старый телефон. Номер мой там уже есть.",
                    pp -> {
                        var dd = com.pvpciv.progress.Progress.d(pp); dd.eleStage = 2; pp.setData(com.pvpciv.registry.ModRegistry.STORY.get(), dd);
                        com.pvpciv.inv.Bag.give(pp, new net.minecraft.world.item.ItemStack(com.pvpciv.registry.ModRegistry.PHONE.get()));
                        com.pvpciv.game.Phone.ringtone(pp);
                        com.pvpciv.game.GameEvents.later(pp.server, 100, () -> com.pvpciv.game.Phone.stopRingtone(pp));
                        pp.sendSystemMessage(net.minecraft.network.chat.Component.literal("📱 Получен телефон «Поко X3 Про»: выкуп из темницы, «Шпиль.Еда», такси. ПКМ телефоном или клавиша [Y]. А ещё у Элеоноры теперь есть метки на боссов и своя лавка.").withStyle(net.minecraft.ChatFormatting.GOLD));
                    }, com.pvpciv.game.DialogueManager.opt("Спасибо!", null));
        }
        return dl;
    }

    @Override
    protected void onPunched(ServerPlayer p) {
        Story.say(p, "Элеонора", COLOR, "Ещё раз — и проверю, не вампир ли ты. Колом.");
    }

    @Override public boolean shouldBeSaved() { return false; }
}
