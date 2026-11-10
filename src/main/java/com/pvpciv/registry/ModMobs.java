package com.pvpciv.registry;

import com.pvpciv.entity.mob.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import static com.pvpciv.registry.ModRegistry.ENTITIES;
import static com.pvpciv.registry.ModRegistry.ITEMS;

/** Собственные мобы цивилизации (свои типы сущностей: /summon pvpciv:ash_zombie и яйца призыва). */
public final class ModMobs {
    private static <T extends net.minecraft.world.entity.Mob> DeferredHolder<EntityType<?>, EntityType<T>> mob(
            String id, EntityType.EntityFactory<T> f, float w, float h, int bg, int fg) {
        DeferredHolder<EntityType<?>, EntityType<T>> holder = ENTITIES.register(id,
                () -> EntityType.Builder.of(f, MobCategory.MONSTER).sized(w, h).clientTrackingRange(8).build(id));
        ITEMS.register(id + "_spawn_egg", () -> new DeferredSpawnEggItem(holder, bg, fg, new Item.Properties()));
        return holder;
    }

    // этаж 1, локация 1 — Пепельная пустошь
    public static final DeferredHolder<EntityType<?>, EntityType<CivZombie>> ASH_ZOMBIE = mob("ash_zombie", CivZombie::new, 0.6f, 1.95f, 0x5a524a, 0xd08040);
    public static final DeferredHolder<EntityType<?>, EntityType<CivHusk>> WITHERED = mob("withered", CivHusk::new, 0.6f, 1.95f, 0x6a5a40, 0x2a2018);
    public static final DeferredHolder<EntityType<?>, EntityType<CivSkeleton>> ASH_ARCHER = mob("ash_archer", CivSkeleton::new, 0.6f, 1.99f, 0x8a8478, 0x3a3430);
    public static final DeferredHolder<EntityType<?>, EntityType<CivSpider>> ASH_SPIDER = mob("ash_spider", CivSpider::new, 1.4f, 0.9f, 0x3a3430, 0xd06030);
    public static final DeferredHolder<EntityType<?>, EntityType<CivZombie>> BRUTE = mob("rotten_brute", CivZombie::new, 0.6f, 1.95f, 0x3a4a2a, 0xd0a040);
    // этаж 1, локация 2 — Инеевый Кряж
    public static final DeferredHolder<EntityType<?>, EntityType<CivZombie>> FROST_ZOMBIE = mob("frost_zombie", CivZombie::new, 0.6f, 1.95f, 0x7aa8c8, 0xe8f6ff);
    public static final DeferredHolder<EntityType<?>, EntityType<CivSkeleton>> FROST_ARCHER = mob("frost_archer", CivSkeleton::new, 0.6f, 1.99f, 0xb8d0e0, 0x3a5a7a);
    public static final DeferredHolder<EntityType<?>, EntityType<CivSpider>> FROST_SPIDER = mob("frost_spider", CivSpider::new, 1.4f, 0.9f, 0x2a3a5a, 0x9adfff);

    public static void init() {}

    public static boolean isCiv(net.minecraft.world.entity.Entity e) {
        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace().equals(com.pvpciv.PvpCiv.MODID);
    }

    public static void onAttributes(EntityAttributeCreationEvent e) {
        e.put(ASH_ZOMBIE.get(), Zombie.createAttributes().build());
        e.put(BRUTE.get(), Zombie.createAttributes().build());
        e.put(FROST_ZOMBIE.get(), Zombie.createAttributes().build());
        e.put(WITHERED.get(), Zombie.createAttributes().build());
        e.put(ASH_ARCHER.get(), Skeleton.createAttributes().build());
        e.put(FROST_ARCHER.get(), Skeleton.createAttributes().build());
        e.put(ASH_SPIDER.get(), Spider.createAttributes().build());
        e.put(FROST_SPIDER.get(), Spider.createAttributes().build());
    }
}
