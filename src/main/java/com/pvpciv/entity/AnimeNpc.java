package com.pvpciv.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

/** HD-NPC этажа 3 (0.9.7): Юки, Хранитель Сёдо, повар Харуки — аниме-модели GeckoLib вместо скинов. */
public class AnimeNpc extends GeoNpc {
    public enum Kind {
        YUKI("Юки, Тихая тень", 0xE0405A, "yuki"), SHODO("Хранитель Сёдо", 0xC8A86A, "shodo"), HARUKI("Повар Харуки", 0xF0A040, "haruki"),
        AURELIUS("Чарователь Аурелий", 0x40D0FF, "aurelius"), STELLA("Стелла", 0xC060FF, "stella");
        public final String name, model; public final int color;
        Kind(String n, int c, String m) { name = n; color = c; model = m; }
    }
    public final Kind kind;

    public AnimeNpc(EntityType<? extends PathfinderMob> type, Level level, Kind kind) {
        super(type, level, kind.name, kind.color);
        this.kind = kind;
    }

    @Override public String model() { return kind.model; }
    @Override protected String idleAnim() { return kind == Kind.YUKI ? "cross" : "idle"; }

    @Override
    protected void onTalk(ServerPlayer p) {
        switch (kind) {
            case YUKI -> com.pvpciv.game.Assassin.talk(p, this);
            case SHODO -> com.pvpciv.game.Library.talk(p, this);
            case HARUKI -> com.pvpciv.game.Library.chef(p, this);
            case AURELIUS -> com.pvpciv.game.Enchanter.talk(p, this);
            case STELLA -> com.pvpciv.game.Stella.talk(p, this);
        }
    }
}
