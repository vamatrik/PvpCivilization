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

    // этаж 2, улица — Неоновый город (ур. 6–8)
    public static final DeferredHolder<EntityType<?>, EntityType<CivPolice>> POLICE = mob("police", CivPolice::new, 0.6f, 1.95f, 0x2a3a6a, 0xe8d24a);
    public static final DeferredHolder<EntityType<?>, EntityType<CivZombie>> THUG = mob("thug", CivZombie::new, 0.6f, 1.95f, 0x3a3a3a, 0xd04060);
    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.beast.CivDog>> STREET_DOG = mob("street_dog", com.pvpciv.entity.beast.CivDog::new, 0.95f, 0.95f, 0x8a6a4a, 0x3a2a1a);
    // этаж 2, канализация (ур. 9–10)
    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.beast.CivRat>> RAT = mob("rat", com.pvpciv.entity.beast.CivRat::new, 0.85f, 0.6f, 0x6a6058, 0xd09090);
    public static final DeferredHolder<EntityType<?>, EntityType<CivZombie>> GHOUL = mob("sewer_ghoul", CivZombie::new, 0.6f, 1.95f, 0x4a5a3a, 0x8ad040);
    public static final DeferredHolder<EntityType<?>, EntityType<CivGunner>> GUNNER = mob("gunner", CivGunner::new, 0.6f, 1.99f, 0x2a2a2a, 0xe0a040);
    // этаж 3: деревня самураев (ур. 10–12) и деревня Кумихо (ур. 13–15)
    public static final DeferredHolder<EntityType<?>, EntityType<CivZombie>> SAMURAI = mob("samurai", CivZombie::new, 0.6f, 1.95f, 0x7a1a1a, 0xd8b048);
    public static final DeferredHolder<EntityType<?>, EntityType<CivZombie>> NINJA = mob("ninja", CivZombie::new, 0.6f, 1.95f, 0x1a1a24, 0xc02040);
    public static final DeferredHolder<EntityType<?>, EntityType<CivSkeleton>> ASHIGARU = mob("ashigaru", CivSkeleton::new, 0.6f, 1.99f, 0x4a3a24, 0x6a2a1a);
    public static final DeferredHolder<EntityType<?>, EntityType<CivZombie>> WEREWOLF = mob("werewolf", CivZombie::new, 0.7f, 2.2f, 0x6a5a4a, 0xffd040);
    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.beast.CivFox>> FOX = mob("fox_spirit", com.pvpciv.entity.beast.CivFox::new, 0.8f, 0.85f, 0xe0782a, 0xf4ece0);
    public static final DeferredHolder<EntityType<?>, EntityType<CivSkeleton>> KITSUNE_ARCHER = mob("kitsune_archer", CivSkeleton::new, 0.6f, 1.99f, 0xf4ece0, 0xd02030);
    // этаж 4: НТЦ «NaZa» — Оранжерея 16–18, Криолаборатория 18–20, Реактор 20–22, «Ковчег» 22–24 (0.11)
    public static final DeferredHolder<EntityType<?>, EntityType<CivZombie>> BOTANIST = mob("botanist", CivZombie::new, 0.6f, 1.95f, 0x4a8a3a, 0xe8f0e0);
    public static final DeferredHolder<EntityType<?>, EntityType<CivSpider>> SPORELING = mob("sporeling", CivSpider::new, 1.2f, 0.8f, 0x6a8a2a, 0xd0e040);
    public static final DeferredHolder<EntityType<?>, EntityType<CivZombie>> THAWED = mob("thawed", CivZombie::new, 0.6f, 1.95f, 0x8ac8e8, 0x2a4a6a);
    public static final DeferredHolder<EntityType<?>, EntityType<CivGunner>> SENTRY = mob("naza_guard", CivGunner::new, 0.6f, 1.99f, 0x2a3a5a, 0x40d0ff);
    public static final DeferredHolder<EntityType<?>, EntityType<CivZombie>> ROBOT = mob("tech_robot", CivZombie::new, 0.7f, 2.1f, 0x8a8e94, 0xff8a20);
    public static final DeferredHolder<EntityType<?>, EntityType<CivGunner>> TURRET = mob("turret", CivGunner::new, 0.9f, 1.9f, 0x5a5e64, 0xff3030);
    public static final DeferredHolder<EntityType<?>, EntityType<CivZombie>> CHIMERA = mob("chimera", CivZombie::new, 0.8f, 2.3f, 0x7a2a6a, 0xd0ff40);
    public static final DeferredHolder<EntityType<?>, EntityType<CivGunner>> LAB_TECH = mob("lab_tech", CivGunner::new, 0.6f, 1.99f, 0xe8e8f0, 0xc040ff);
    // этаж 5: станция «Орбита» (0.12)
    public static final DeferredHolder<EntityType<?>, EntityType<CivGunner>> ANDROID = mob("android", CivGunner::new, 0.6f, 1.99f, 0xE8ECF0, 0x30E0FF);
    public static final DeferredHolder<EntityType<?>, EntityType<CivZombie>> XENO = mob("xeno", CivZombie::new, 0.7f, 2.1f, 0x1A1A22, 0x6AF040);
    public static final DeferredHolder<EntityType<?>, EntityType<CivSpider>> VOID_CRAWLER = mob("void_crawler", CivSpider::new, 1.3f, 0.8f, 0x2A1A4A, 0xC060FF);
    public static final DeferredHolder<EntityType<?>, EntityType<CivZombie>> ROGUE_ASTRO = mob("rogue_astro", CivZombie::new, 0.6f, 1.95f, 0xF0F0F0, 0xFF8A20);
    public static final DeferredHolder<EntityType<?>, EntityType<CivGunner>> DRONE = mob("drone", CivGunner::new, 0.8f, 0.9f, 0xE8C020, 0x30E0FF);
    public static final DeferredHolder<EntityType<?>, EntityType<CivZombie>> COLLECTOR = mob("collector", CivZombie::new, 0.6f, 1.99f, 0x3A2A5A, 0xE8C84A);
    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.boss.KappaBoss>> KAPPA_BOSS = ENTITIES.register("kappa",
            () -> EntityType.Builder.of(com.pvpciv.entity.boss.KappaBoss::new, MobCategory.MONSTER).sized(0.9f, 1.7f).fireImmune().clientTrackingRange(10).build("kappa"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.boss.KumihoBoss>> KUMIHO_BOSS = ENTITIES.register("kumiho",
            () -> EntityType.Builder.of(com.pvpciv.entity.boss.KumihoBoss::new, MobCategory.MONSTER).sized(0.8f, 2.3f).fireImmune().clientTrackingRange(10).build("kumiho"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.boss.NoelBoss>> NOEL_BOSS = ENTITIES.register("noel",
            () -> EntityType.Builder.of(com.pvpciv.entity.boss.NoelBoss::new, MobCategory.MONSTER).sized(0.7f, 1.9f).fireImmune().clientTrackingRange(10).build("noel"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.boss.LianaBoss>> LIANA_BOSS = ENTITIES.register("liana",
            () -> EntityType.Builder.of(com.pvpciv.entity.boss.LianaBoss::new, MobCategory.MONSTER).sized(2.6f, 5.4f).fireImmune().clientTrackingRange(10).build("liana"));
    // 1.0: боссы этажа 5
    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.boss.LunaBoss>> LUNA_BOSS = ENTITIES.register("luna_boss",
            () -> EntityType.Builder.of(com.pvpciv.entity.boss.LunaBoss::new, MobCategory.MONSTER).sized(0.6f, 1.8f).fireImmune().clientTrackingRange(10).build("luna_boss"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.boss.DiplomatBoss>> DIPLOMAT_BOSS = ENTITIES.register("diplomat",
            () -> EntityType.Builder.of(com.pvpciv.entity.boss.DiplomatBoss::new, MobCategory.MONSTER).sized(0.8f, 2.4f).fireImmune().clientTrackingRange(10).build("diplomat"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.boss.ContractEntity>> CONTRACT = ENTITIES.register("contract",
            () -> EntityType.Builder.of(com.pvpciv.entity.boss.ContractEntity::new, MobCategory.MISC).sized(0.7f, 0.7f).fireImmune().clientTrackingRange(8).updateInterval(1).build("contract"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.boss.WormBoss>> WORM_BOSS = ENTITIES.register("worm",
            () -> EntityType.Builder.of(com.pvpciv.entity.boss.WormBoss::new, MobCategory.MONSTER).sized(2.6f, 2.6f).fireImmune().clientTrackingRange(16).updateInterval(1).build("worm"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.boss.WormSegment>> WORM_SEG = ENTITIES.register("worm_segment",
            () -> EntityType.Builder.of(com.pvpciv.entity.boss.WormSegment::new, MobCategory.MISC).sized(2.2f, 2.2f).fireImmune().clientTrackingRange(16).updateInterval(1).build("worm_segment"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.boss.VoidDemon>> VOID_DEMON = ENTITIES.register("void_demon",
            () -> EntityType.Builder.of(com.pvpciv.entity.boss.VoidDemon::new, MobCategory.MONSTER).sized(0.7f, 2.0f).fireImmune().clientTrackingRange(12).build("void_demon"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.beast.PetRat>> PET_RAT = ENTITIES.register("pet_rat",
            () -> EntityType.Builder.of(com.pvpciv.entity.beast.PetRat::new, MobCategory.MISC).sized(0.5f, 0.45f).clientTrackingRange(8).build("pet_rat"));

    // боссы (GeckoLib): спавнит BossManager (/civ boss brute|keeper)
    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.boss.BruteBoss>> BRUTE_BOSS = ENTITIES.register("rotten_brute_boss",
            () -> EntityType.Builder.of(com.pvpciv.entity.boss.BruteBoss::new, MobCategory.MONSTER).sized(1.6f, 3.2f).fireImmune().clientTrackingRange(10).build("rotten_brute_boss"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.boss.KeeperBoss>> KEEPER_BOSS = ENTITIES.register("crypt_keeper",
            () -> EntityType.Builder.of(com.pvpciv.entity.boss.KeeperBoss::new, MobCategory.MONSTER).sized(0.8f, 2.6f).fireImmune().clientTrackingRange(10).build("crypt_keeper"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.boss.DonBoss>> DON_BOSS = ENTITIES.register("don_vittorio",
            () -> EntityType.Builder.of(com.pvpciv.entity.boss.DonBoss::new, MobCategory.MONSTER).sized(0.8f, 2.4f).fireImmune().clientTrackingRange(10).build("don_vittorio"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.boss.RatKingBoss>> RAT_KING_BOSS = ENTITIES.register("rat_king",
            () -> EntityType.Builder.of(com.pvpciv.entity.boss.RatKingBoss::new, MobCategory.MONSTER).sized(1.0f, 2.8f).fireImmune().clientTrackingRange(10).build("rat_king"));

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
        e.put(POLICE.get(), Zombie.createAttributes().build());
        e.put(THUG.get(), Zombie.createAttributes().build());
        e.put(GHOUL.get(), Zombie.createAttributes().build());
        e.put(GUNNER.get(), Skeleton.createAttributes().build());
        e.put(SAMURAI.get(), Zombie.createAttributes().build());
        e.put(NINJA.get(), Zombie.createAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.31).build());
        e.put(ASHIGARU.get(), Skeleton.createAttributes().build());
        e.put(WEREWOLF.get(), Zombie.createAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.27).build());
        e.put(FOX.get(), com.pvpciv.entity.beast.CivBeast.attributes(0.36).build());
        e.put(KITSUNE_ARCHER.get(), Skeleton.createAttributes().build());
        e.put(BOTANIST.get(), Zombie.createAttributes().build());
        e.put(SPORELING.get(), Spider.createAttributes().build());
        e.put(THAWED.get(), Zombie.createAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.2).build());
        e.put(SENTRY.get(), Skeleton.createAttributes().build());
        e.put(ROBOT.get(), Zombie.createAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, 0.6).build());
        e.put(TURRET.get(), Skeleton.createAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, 1).build());
        e.put(CHIMERA.get(), Zombie.createAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.28).build());
        e.put(LAB_TECH.get(), Skeleton.createAttributes().build());
        e.put(ANDROID.get(), Skeleton.createAttributes().build());
        e.put(XENO.get(), Zombie.createAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.3).build());
        e.put(VOID_CRAWLER.get(), Spider.createAttributes().build());
        e.put(ROGUE_ASTRO.get(), Zombie.createAttributes().build());
        e.put(DRONE.get(), Skeleton.createAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, 1).build());
        e.put(COLLECTOR.get(), Zombie.createAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.27).build());
        e.put(KAPPA_BOSS.get(), com.pvpciv.entity.boss.CivBoss.attributes(400, 8, 0.28).build());
        e.put(NOEL_BOSS.get(), com.pvpciv.entity.boss.CivBoss.attributes(900, 10, 0.27).build());
        e.put(LUNA_BOSS.get(), com.pvpciv.entity.boss.CivBoss.attributes(900, 10, 0.3).build());
        e.put(DIPLOMAT_BOSS.get(), com.pvpciv.entity.boss.CivBoss.attributes(900, 10, 0.25).build());
        e.put(WORM_BOSS.get(), com.pvpciv.entity.boss.CivBoss.attributes(900, 10, 0.3).add(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, 1).add(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE, 80).build());
        e.put(VOID_DEMON.get(), com.pvpciv.entity.boss.CivBoss.attributes(900, 10, 0.0).add(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, 1).build());
        e.put(CONTRACT.get(), com.pvpciv.entity.boss.ContractEntity.attributes().build());
        e.put(WORM_SEG.get(), com.pvpciv.entity.boss.WormSegment.attributes().build());
        e.put(LIANA_BOSS.get(), com.pvpciv.entity.boss.CivBoss.attributes(900, 10, 0.0).add(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, 1).build());
        e.put(KUMIHO_BOSS.get(), com.pvpciv.entity.boss.CivBoss.attributes(900, 10, 0.3).build());
        e.put(STREET_DOG.get(), com.pvpciv.entity.beast.CivBeast.attributes(0.33).build());
        e.put(RAT.get(), com.pvpciv.entity.beast.CivBeast.attributes(0.32).build());
        e.put(PET_RAT.get(), com.pvpciv.entity.beast.CivBeast.attributes(0.34).build());
        e.put(DON_BOSS.get(), com.pvpciv.entity.boss.CivBoss.attributes(500, 8, 0.25).build());
        e.put(RAT_KING_BOSS.get(), com.pvpciv.entity.boss.CivBoss.attributes(600, 9, 0.27).build());
        e.put(BRUTE_BOSS.get(), com.pvpciv.entity.boss.CivBoss.attributes(250, 6, 0.22).build());
        e.put(KEEPER_BOSS.get(), com.pvpciv.entity.boss.CivBoss.attributes(380, 7, 0.24).build());
    }
}
