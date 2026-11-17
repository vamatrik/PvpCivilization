package com.pvpciv.game;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 0.12 — Луна на станции «Орбита». Как она оказалась на орбите раньше тебя — не говорит. Держит лавку;
 * с 0.13 — ещё и испытания (бои по этапам, тёмная материя, классы) и свиток забвения.
 */
public final class LunaStation {
    private LunaStation() {}

    public static void talk(ServerPlayer p, com.pvpciv.entity.AnimeNpc npc) {
        var d = com.pvpciv.progress.Progress.d(p);
        String[] hi;
        if (!d.gates.contains("luna5_met")) {
            d.gates.putInt("luna5_met", 1);   // первая встреча — сразу лавка; дальше — меню (Shift+ПКМ — сразу лавка)
            p.setData(com.pvpciv.registry.ModRegistry.STORY.get(), d);
            hi = new String[]{"Ты долго летел. Я — быстрее. Не спрашивай как: всё равно не поверишь. Смотри, у меня тут лавка. Космическая наценка — за доставку."};
        } else hi = new String[]{"Сжатый лазуратрон, катализаторы, торт. Торт — вне конкурса.", "Подерёмся? Стадия за стадией — пока не упадёшь. За первые победы — тёмная материя.",
                "Видел планету внизу? Это Шпиль. Отсюда он маленький и почти не страшный.", "Дипломат на мостике опять требует подписать «взаимовыгодное соглашение». Не подписывай."};
        Story.say(p, npc.kind.name, npc.kind.color, hi[p.getRandom().nextInt(hi.length)]);
        if (d.gates.getInt("luna5_met") > 0 && d.level >= 30 && !p.isShiftKeyDown()) { LunaTrial.talk(p, npc); return; }   // 1.0: выбор — испытание, лавка, дерево
        shop(p, npc);
    }

    public static void shop(ServerPlayer p, com.pvpciv.entity.AnimeNpc npc) {
        Shop.VENDOR.put(p.getUUID(), "luna");
        p.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new com.pvpciv.menu.TraderMenu(id, inv), Component.literal("Луна")));
        Shop.sendOffers(p);
    }
}
