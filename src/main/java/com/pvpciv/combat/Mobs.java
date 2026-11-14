package com.pvpciv.combat;

import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Создание мобов с уровнем. HP = 20·1.065^(L−1), урон = 3·1.05^(L−1) (5 уровней на этаж ≈ ×1.37 за этаж). */
public final class Mobs {
    public enum Kind {
        ASH_ZOMBIE(() -> com.pvpciv.registry.ModMobs.ASH_ZOMBIE.get(), "Пепельный зомби", "floor1_mob", 1),
        WITHERED(() -> com.pvpciv.registry.ModMobs.WITHERED.get(), "Иссохший", "floor1_mob", 2),
        ASH_ARCHER(() -> com.pvpciv.registry.ModMobs.ASH_ARCHER.get(), "Пепельный лучник", "floor1_mob", 2),
        ASH_SPIDER(() -> com.pvpciv.registry.ModMobs.ASH_SPIDER.get(), "Пепельный паук", "floor1_mob", 1),
        FROST_ZOMBIE(() -> com.pvpciv.registry.ModMobs.FROST_ZOMBIE.get(), "Обмороженный", "floor1b_mob", 5),
        FROST_ARCHER(() -> com.pvpciv.registry.ModMobs.FROST_ARCHER.get(), "Инеевый стрелок", "floor1b_mob", 6),
        FROST_SPIDER(() -> com.pvpciv.registry.ModMobs.FROST_SPIDER.get(), "Ледяной паук", "floor1b_mob", 6),
        POLICE(() -> com.pvpciv.registry.ModMobs.POLICE.get(), "Продажный коп", "floor2_mob", 7),
        THUG(() -> com.pvpciv.registry.ModMobs.THUG.get(), "Гопник", "floor2_mob", 6),
        STREET_DOG(() -> com.pvpciv.registry.ModMobs.STREET_DOG.get(), "Бешеная дворняга", "floor2_mob", 6),
        RAT(() -> com.pvpciv.registry.ModMobs.RAT.get(), "Канализационная крыса", "floor2b_mob", 9),
        GHOUL(() -> com.pvpciv.registry.ModMobs.GHOUL.get(), "Сточный упырь", "floor2b_mob", 9),
        GUNNER(() -> com.pvpciv.registry.ModMobs.GUNNER.get(), "Автоматчик", "floor2b_mob", 10),
        SAMURAI(() -> com.pvpciv.registry.ModMobs.SAMURAI.get(), "Самурай-ронин", "floor3_mob", 11),
        NINJA(() -> com.pvpciv.registry.ModMobs.NINJA.get(), "Ниндзя", "floor3_mob", 11),
        ASHIGARU(() -> com.pvpciv.registry.ModMobs.ASHIGARU.get(), "Асигару-лучник", "floor3_mob", 10),
        WEREWOLF(() -> com.pvpciv.registry.ModMobs.WEREWOLF.get(), "Оборотень", "floor3b_mob", 14),
        FOX(() -> com.pvpciv.registry.ModMobs.FOX.get(), "Лиса-оборотень", "floor3b_mob", 13),
        KITSUNE_ARCHER(() -> com.pvpciv.registry.ModMobs.KITSUNE_ARCHER.get(), "Лучница в маске кицунэ", "floor3b_mob", 14),
        BOTANIST(() -> com.pvpciv.registry.ModMobs.BOTANIST.get(), "Обезумевший ботаник", "floor4_mob", 16),
        SPORELING(() -> com.pvpciv.registry.ModMobs.SPORELING.get(), "Споровый ползун", "floor4_mob", 17),
        THAWED(() -> com.pvpciv.registry.ModMobs.THAWED.get(), "Размороженный образец", "floor4_mob", 18),
        SENTRY(() -> com.pvpciv.registry.ModMobs.SENTRY.get(), "Охранник NaZa", "floor4_mob", 19),
        ROBOT(() -> com.pvpciv.registry.ModMobs.ROBOT.get(), "Робот-техник", "floor4_mob", 20),
        TURRET(() -> com.pvpciv.registry.ModMobs.TURRET.get(), "Турель", "floor4_mob", 21),
        CHIMERA(() -> com.pvpciv.registry.ModMobs.CHIMERA.get(), "Химера", "floor4b_mob", 23),
        LAB_TECH(() -> com.pvpciv.registry.ModMobs.LAB_TECH.get(), "Лаборант с плазмаганом", "floor4b_mob", 22);
        private final java.util.function.Supplier<? extends EntityType<? extends Mob>> type;
        public final String title, loot; public final int defLevel;
        Kind(java.util.function.Supplier<? extends EntityType<? extends Mob>> t, String n, String loot, int lvl) { type = t; title = n; this.loot = loot; defLevel = lvl; }
        public EntityType<? extends Mob> type() { return type.get(); }
        public static Kind of(EntityType<?> t) {
            for (Kind k : values()) if (k.type() == t) return k;
            return null;
        }
    }

    public static int level(LivingEntity e) { return e.getData(ModRegistry.MOB_LEVEL.get()); }
    public static String lootKind(LivingEntity e) { return e.getData(ModRegistry.MOB_KIND.get()); }

    public static double hp(int lvl) { return Balance.mobHp(lvl); }     // 0.9.9: своя шкала (было 20·1.065^л)
    public static double dmg(int lvl) { return Balance.mobDmg(lvl); }   // 0.9.9: (было 3·1.07^л)

    public static int levelColor(int lvl) {
        if (lvl <= 3) return 0xCFCFCF;
        if (lvl <= 6) return 0xFFE066;
        return 0xFF6B6B;
    }

    public static Mob spawn(ServerLevel lvl, Kind kind, int level, boolean elite, Vec3 pos, String lootTable, String tag) {
        Mob m = kind.type().create(lvl);
        if (m == null) return null;
        m.moveTo(pos.x, pos.y, pos.z, lvl.random.nextFloat() * 360, 0);
        setup(m, level, elite, (elite ? "★ " : "") + kind.title, lootTable);
        if (m instanceof Zombie z) z.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(0);
        if (kind == Kind.ASH_ARCHER || kind == Kind.FROST_ARCHER) m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        if (kind == Kind.GUNNER) m.setItemSlot(EquipmentSlot.MAINHAND, prop(com.pvpciv.registry.ModRegistry.RIFLE.get()));
        if ((kind == Kind.ASH_ZOMBIE || kind == Kind.FROST_ZOMBIE) && lvl.random.nextFloat() < 0.3f) m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SHOVEL));
        if (kind == Kind.POLICE) m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModRegistry.POLICE_BATON.get()));
        if (kind == Kind.THUG && lvl.random.nextFloat() < 0.6f) m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModRegistry.NAIL_BAT.get()));
        if (kind == Kind.GHOUL && lvl.random.nextFloat() < 0.4f) m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModRegistry.PIPE_WRENCH.get()));
        if (kind == Kind.SAMURAI) m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModRegistry.NODACHI.get()));
        if (kind == Kind.NINJA) m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModRegistry.KUNAI.get()));
        if (kind == Kind.ASHIGARU || kind == Kind.KITSUNE_ARCHER) m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        if (kind == Kind.WEREWOLF) set(m, Attributes.SCALE, 1.15);
        f4(m, kind);
        for (EquipmentSlot s : EquipmentSlot.values()) m.setDropChance(s, 0f);
        if (!tag.isEmpty()) m.addTag(tag);
        lvl.addFreshEntity(m);
        return m;
    }

    /** Моб призван командой или яйцом — выдать уровень и имя по умолчанию. */
    public static void setupDefault(Mob m) {
        Kind k = Kind.of(m.getType());
        if (m.getType() == com.pvpciv.registry.ModMobs.BRUTE.get()) { setup(m, 4, false, "Гнилой Громила", "floor1_mob"); return; }
        if (k == null) return;
        setup(m, k.defLevel, false, k.title, k.loot);
        if (m instanceof Zombie z) z.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(0);
        if (k == Kind.ASH_ARCHER || k == Kind.FROST_ARCHER) m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        if (k == Kind.GUNNER) m.setItemSlot(EquipmentSlot.MAINHAND, prop(com.pvpciv.registry.ModRegistry.RIFLE.get()));   // 0.10: АК
        if (k == Kind.SAMURAI) m.setItemSlot(EquipmentSlot.MAINHAND, prop(com.pvpciv.registry.ModRegistry.NODACHI.get()));   // 0.9.9: оружие в руках
        if (k == Kind.NINJA) m.setItemSlot(EquipmentSlot.MAINHAND, prop(com.pvpciv.registry.ModRegistry.KUNAI.get()));
        f4(m, k);
        for (EquipmentSlot s : EquipmentSlot.values()) m.setDropChance(s, 0f);
    }

    /** 0.11: снаряжение и размеры мобов этажа 4. */
    private static void f4(Mob m, Kind k) {
        switch (k) {
            case SENTRY -> m.setItemSlot(EquipmentSlot.MAINHAND, prop(ModRegistry.RIFLE.get()));
            case LAB_TECH -> m.setItemSlot(EquipmentSlot.MAINHAND, prop(com.pvpciv.registry.ModF4.PLASMA_CELL.get()));
            case BOTANIST -> m.setItemSlot(EquipmentSlot.MAINHAND, prop(Items.IRON_HOE));
            case ROBOT -> { m.setItemSlot(EquipmentSlot.MAINHAND, prop(com.pvpciv.registry.ModF4.GRAVITY_HAMMER.get())); set(m, Attributes.SCALE, 1.1); }
            case CHIMERA -> set(m, Attributes.SCALE, 1.2);
            case TURRET -> m.setNoGravity(false);
            default -> {}
        }
    }

    /** Оружие «для вида»: без бонусов к урону (иначе модификатор меча прибавлялся к урону моба). */
    private static ItemStack prop(net.minecraft.world.item.Item it) {
        ItemStack st = new ItemStack(it);
        st.set(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS, net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                .add(Attributes.LUCK, new net.minecraft.world.entity.ai.attributes.AttributeModifier(com.pvpciv.PvpCiv.id("prop"), 0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                        net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND).build().withTooltip(false));
        return st;
    }

    public static void setup(Mob m, int level, boolean elite, String name, String lootTable) {
        m.setData(ModRegistry.MOB_LEVEL.get(), level);
        m.setData(ModRegistry.MOB_KIND.get(), elite ? lootTable.replace("_mob", "_elite") : lootTable);
        double hp = hp(level) * (elite ? 3 : 1);
        set(m, Attributes.MAX_HEALTH, hp);
        set(m, Attributes.ATTACK_DAMAGE, dmg(level) * (elite ? 1.3 : 1));
        set(m, Attributes.FOLLOW_RANGE, m instanceof net.minecraft.world.entity.monster.AbstractSkeleton ? 20 : 28);   // бета 12.11: лучники не тянутся за тобой через пол-карты
        m.setHealth((float) hp);
        m.setPersistenceRequired();
        m.addTag("bal99");
        if (elite) {
            m.setGlowingTag(true);
            set(m, Attributes.SCALE, 1.25);
        }
        int c = elite ? 0xFFB020 : levelColor(level);
        m.setCustomName(Component.literal("[" + level + "] ").withStyle(x -> x.withColor(levelColor(level)))
                .append(Component.literal(name).withStyle(x -> x.withColor(c))));
        m.setCustomNameVisible(false);
    }

    private static void set(Mob m, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> a, double v) {
        AttributeInstance inst = m.getAttribute(a);
        if (inst != null) inst.setBaseValue(v);
    }

    @SuppressWarnings("unused") private static final ChatFormatting F = ChatFormatting.GRAY;
}
