package com.pvpciv.net;

import com.pvpciv.PvpCiv;
import com.pvpciv.game.DashSkill;
import com.pvpciv.game.DialogueManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

public final class Network {

    /** Сервер → клиент: показать реплику диалога. */
    public record DialogueOpen(int entityId, String speaker, int color, String text, List<String> options) implements CustomPacketPayload {
        public static final Type<DialogueOpen> TYPE = new Type<>(PvpCiv.id("dialogue_open"));
        public static final StreamCodec<ByteBuf, DialogueOpen> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, DialogueOpen::entityId,
                ByteBufCodecs.STRING_UTF8, DialogueOpen::speaker,
                ByteBufCodecs.INT, DialogueOpen::color,
                ByteBufCodecs.STRING_UTF8, DialogueOpen::text,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), DialogueOpen::options,
                DialogueOpen::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Сервер → клиент: закрыть диалог. */
    public record DialogueClose() implements CustomPacketPayload {
        public static final Type<DialogueClose> TYPE = new Type<>(PvpCiv.id("dialogue_close"));
        public static final StreamCodec<ByteBuf, DialogueClose> CODEC = StreamCodec.unit(new DialogueClose());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Сервер → клиент: состояние HUD. */
    public record SyncState(int stage, String objective, int level, long xp, long xpNext, long money, String zone, int zoneType, int bossSeconds,
                            String quest, int questSeconds, int jailSeconds, String titles, String activeTitle) implements CustomPacketPayload {
        public static final Type<SyncState> TYPE = new Type<>(PvpCiv.id("sync_state"));
        public static final StreamCodec<ByteBuf, SyncState> CODEC = new StreamCodec<>() {
            @Override public SyncState decode(ByteBuf b) {
                return new SyncState(ByteBufCodecs.VAR_INT.decode(b), ByteBufCodecs.STRING_UTF8.decode(b), ByteBufCodecs.VAR_INT.decode(b),
                        ByteBufCodecs.VAR_LONG.decode(b), ByteBufCodecs.VAR_LONG.decode(b), ByteBufCodecs.VAR_LONG.decode(b),
                        ByteBufCodecs.STRING_UTF8.decode(b), ByteBufCodecs.VAR_INT.decode(b), ByteBufCodecs.INT.decode(b),
                        ByteBufCodecs.STRING_UTF8.decode(b), ByteBufCodecs.INT.decode(b), ByteBufCodecs.INT.decode(b), ByteBufCodecs.STRING_UTF8.decode(b), ByteBufCodecs.STRING_UTF8.decode(b));
            }
            @Override public void encode(ByteBuf b, SyncState s) {
                ByteBufCodecs.VAR_INT.encode(b, s.stage); ByteBufCodecs.STRING_UTF8.encode(b, s.objective); ByteBufCodecs.VAR_INT.encode(b, s.level);
                ByteBufCodecs.VAR_LONG.encode(b, s.xp); ByteBufCodecs.VAR_LONG.encode(b, s.xpNext); ByteBufCodecs.VAR_LONG.encode(b, s.money);
                ByteBufCodecs.STRING_UTF8.encode(b, s.zone); ByteBufCodecs.VAR_INT.encode(b, s.zoneType); ByteBufCodecs.INT.encode(b, s.bossSeconds);
                ByteBufCodecs.STRING_UTF8.encode(b, s.quest); ByteBufCodecs.INT.encode(b, s.questSeconds); ByteBufCodecs.INT.encode(b, s.jailSeconds);
                ByteBufCodecs.STRING_UTF8.encode(b, s.titles); ByteBufCodecs.STRING_UTF8.encode(b, s.activeTitle);
            }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Сервер → клиент: катсцена (1 — Кармилла уводит, 2 — укус). */
    public record Cinematic(int kind, int ticks, String text) implements CustomPacketPayload {
        public static final Type<Cinematic> TYPE = new Type<>(PvpCiv.id("cinematic"));
        public static final StreamCodec<ByteBuf, Cinematic> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Cinematic::kind, ByteBufCodecs.VAR_INT, Cinematic::ticks, ByteBufCodecs.STRING_UTF8, Cinematic::text, Cinematic::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Клиент → сервер: выбрать активный титул ("" — снять). */
    public record SelectTitle(String id) implements CustomPacketPayload {
        public static final Type<SelectTitle> TYPE = new Type<>(PvpCiv.id("select_title"));
        public static final StreamCodec<ByteBuf, SelectTitle> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SelectTitle::id, SelectTitle::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Клиент → сервер: выбор варианта ответа (-1 = закрыл окно). */
    public record DialogueChoice(int index) implements CustomPacketPayload {
        public static final Type<DialogueChoice> TYPE = new Type<>(PvpCiv.id("dialogue_choice"));
        public static final StreamCodec<ByteBuf, DialogueChoice> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, DialogueChoice::index, DialogueChoice::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Клиент → сервер: использовать Рывок. */
    public record Dash() implements CustomPacketPayload {
        public static final Type<Dash> TYPE = new Type<>(PvpCiv.id("dash"));
        public static final StreamCodec<ByteBuf, Dash> CODEC = StreamCodec.unit(new Dash());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record OfferView(net.minecraft.world.item.ItemStack stack, int price, int left) {
        public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, OfferView> CODEC = StreamCodec.composite(
                net.minecraft.world.item.ItemStack.STREAM_CODEC, OfferView::stack,
                ByteBufCodecs.VAR_INT, OfferView::price,
                ByteBufCodecs.VAR_INT, OfferView::left, OfferView::new);
    }

    /** Сервер → клиент: ассортимент магазина. */
    public record TraderOffers(List<OfferView> offers, long msLeft) implements CustomPacketPayload {
        public static final Type<TraderOffers> TYPE = new Type<>(PvpCiv.id("trader_offers"));
        public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, TraderOffers> CODEC = StreamCodec.composite(
                OfferView.CODEC.apply(ByteBufCodecs.list()), TraderOffers::offers,
                ByteBufCodecs.VAR_LONG, TraderOffers::msLeft, TraderOffers::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Сервер → клиент: что потеряно при смерти (для экрана смерти). */
    public record DeathInfo(long lostMoney, long lostXp, boolean pvp, String killer) implements CustomPacketPayload {
        public static final Type<DeathInfo> TYPE = new Type<>(PvpCiv.id("death_info"));
        public static final StreamCodec<ByteBuf, DeathInfo> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, DeathInfo::lostMoney, ByteBufCodecs.VAR_LONG, DeathInfo::lostXp,
                ByteBufCodecs.BOOL, DeathInfo::pvp, ByteBufCodecs.STRING_UTF8, DeathInfo::killer, DeathInfo::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Клиент → сервер: открыть RPG-инвентарь. */
    public record OpenInventory() implements CustomPacketPayload {
        public static final Type<OpenInventory> TYPE = new Type<>(PvpCiv.id("open_inventory"));
        public static final StreamCodec<ByteBuf, OpenInventory> CODEC = StreamCodec.unit(new OpenInventory());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToClient(DialogueOpen.TYPE, DialogueOpen.CODEC, (p, ctx) -> com.pvpciv.client.ClientPayloads.dialogueOpen(p));
        r.playToClient(DialogueClose.TYPE, DialogueClose.CODEC, (p, ctx) -> com.pvpciv.client.ClientPayloads.dialogueClose());
        r.playToClient(SyncState.TYPE, SyncState.CODEC, (p, ctx) -> com.pvpciv.client.ClientPayloads.sync(p));
        r.playToServer(DialogueChoice.TYPE, DialogueChoice.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) DialogueManager.choose(sp, p.index());
        });
        r.playToClient(DeathInfo.TYPE, DeathInfo.CODEC, (p, ctx) -> com.pvpciv.client.ClientPayloads.death(p));
        r.playToClient(Cinematic.TYPE, Cinematic.CODEC, (p, ctx) -> com.pvpciv.client.ClientPayloads.cinematic(p));
        r.playToServer(SelectTitle.TYPE, SelectTitle.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof net.minecraft.server.level.ServerPlayer sp) com.pvpciv.progress.Titles.select(sp, p.id());
        });
        r.playToClient(TraderOffers.TYPE, TraderOffers.CODEC, (p, ctx) -> com.pvpciv.client.ClientPayloads.offers(p));
        r.playToServer(OpenInventory.TYPE, OpenInventory.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp && !sp.isCreative() && !sp.isSpectator())
                sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new com.pvpciv.menu.CivInventoryMenu(id, inv),
                        net.minecraft.network.chat.Component.literal("Сумка")));
        });
        r.playToServer(Dash.TYPE, Dash.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) DashSkill.tryDash(sp);
        });
    }
}
