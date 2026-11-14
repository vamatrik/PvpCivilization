package com.pvpciv.progress;

import com.pvpciv.PvpCiv;
import com.pvpciv.data.StoryData;
import com.pvpciv.game.Story;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Титулы: выдаются за поступки, активен один (виден над ником). Бонус действует, пока титул активен.
 * Чтобы добавить титул — строка в DEFS и вызов Titles.grant(p, "id") там, где он зарабатывается.
 */
public final class Titles {
    public enum Bonus { HP, SPEED, DAMAGE, BOAT, NONE }
    public record Def(String id, String name, int color, String how, String bonusText, Bonus bonus, double value) {}

    public static final Map<String, Def> DEFS = new LinkedHashMap<>();
    static {
        add(new Def("vampire_victim", "Жертва", 0xD23A54, "Побывай в подвале у Кармиллы", "+2.5% к макс. здоровью", Bonus.HP, 0.025));
        add(new Def("hammer_hands", "Молотилка", 0xE05050, "Веха Тимофея: 25 000 ударов", "+3% к урону", Bonus.DAMAGE, 0.03));
        add(new Def("marathon", "Марафонец", 0x60D060, "Веха Тимофея: 100 км пешком", "+4% к скорости бега", Bonus.SPEED, 0.04));
        add(new Def("exterminator", "Истребитель", 0xD0A040, "Веха Тимофея: 5 000 убитых мобов", "+3% к здоровью", Bonus.HP, 0.03));
        add(new Def("donor", "Донор", 0xC02040, "Веха Тимофея: 20 укусов Кармиллы", "+3% к здоровью", Bonus.HP, 0.03));
        add(new Def("sky_pass", "Небожитель", 0xC0A0FF, "Получи у Луны допуск к Небесному лифту", "+2% к здоровью", Bonus.HP, 0.02));
        add(new Def("uninvited", "Незваный", 0xFF8AD8, "Проберись в комнату Луны", "+3% к скорости бега", Bonus.SPEED, 0.03));
        add(new Def("contractor", "Подрядчик", 0xE0B040, "Выполни 5 контрактов Кармиллы", "+3% к урону", Bonus.DAMAGE, 0.03));
        add(new Def("ice_racer", "Гонщик", 0x9ADFFF, "Пройди Ледяную Трассу быстрее 1:20", "+6% к скорости лодки", Bonus.BOAT, 0.06));
        add(new Def("reader", "Читатель", 0xC8A86A, "Доберись до Тихой библиотеки (этаж 3)", "+2% к здоровью", Bonus.HP, 0.02));
        add(new Def("shadow", "Тень", 0xE0405A, "Пройди всю цепочку охоты Юки", "+3% к урону", Bonus.DAMAGE, 0.03));
        add(new Def("gates_1", "Привратник", 0x8FE3FF, "Почини все врата этажа 1", "+2% к скорости бега", Bonus.SPEED, 0.02));
        add(new Def("gates_2", "Наладчик", 0x8FE3FF, "Почини все врата этажа 2", "+2% к здоровью", Bonus.HP, 0.02));
        add(new Def("gates_3", "Странник", 0x8FE3FF, "Почини все врата этажа 3", "+2% к урону", Bonus.DAMAGE, 0.02));
        add(new Def("naza_staff", "Стажёр", 0x40D0FF, "Доберись до НТЦ «NaZa» (этаж 4)", "+2% к здоровью", Bonus.HP, 0.02));
        add(new Def("molecular", "Синтезатор", 0x40D0FF, "Получи зачарование IV ранга", "+2% к урону", Bonus.DAMAGE, 0.02));
        add(new Def("phase", "Фазовый", 0xC060FF, "Зарядь прибор Стеллы: 1000 рывков", "+3% к скорости бега", Bonus.SPEED, 0.03));
        add(new Def("gardener", "Прополка", 0x6AD040, "Победи доктора Ноэля и Лиану", "+3% к здоровью", Bonus.HP, 0.03));
        add(new Def("black_star", "Чёрная звезда", 0x9A6AFF, "Заточи оружие до +11", "чёрная аура (скоро)", Bonus.NONE, 0));
        add(new Def("luna_100", "Неудачник", 0xFF8AD8, "Проиграй Луне 100 раз (скоро)", "до +10% урона", Bonus.NONE, 0));
    }
    private static void add(Def d) { DEFS.put(d.id(), d); }

    private static final ResourceLocation MOD_ID = PvpCiv.id("title_bonus");

    public static boolean has(ServerPlayer p, String id) { return Progress.d(p).titles.contains(id); }

    public static Def active(StoryData d) { return DEFS.get(d.activeTitle); }

    /** Выдать титул (если новый — объявить и сделать активным). */
    public static void grant(ServerPlayer p, String id) {
        StoryData d = Progress.d(p);
        Def def = DEFS.get(id);
        if (def == null || !d.titles.add(id)) return;
        d.activeTitle = id;
        p.setData(ModRegistry.STORY.get(), d);
        apply(p);
        p.refreshDisplayName();
        p.refreshTabListName();
        Story.title(p, "НОВЫЙ ТИТУЛ", "«" + def.name() + "»", def.color());
        p.sendSystemMessage(Component.literal("Бонус титула: " + def.bonusText() + ". Сменить титул — инвентарь → «Титулы».").withStyle(ChatFormatting.GRAY));
        p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.1f);
        Component msg = Component.literal("✦ " + p.getGameProfile().getName() + " получает титул ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("«" + def.name() + "»").withStyle(s -> s.withColor(def.color())));
        for (ServerPlayer o : p.server.getPlayerList().getPlayers()) o.sendSystemMessage(msg);
        Story.sync(p);
    }

    public static void select(ServerPlayer p, String id) {
        StoryData d = Progress.d(p);
        if (!id.isEmpty() && !d.titles.contains(id)) return;
        d.activeTitle = id;
        p.setData(ModRegistry.STORY.get(), d);
        apply(p);
        p.refreshDisplayName();
        p.refreshTabListName();
        Story.sync(p);
    }

    /** Применить бонус активного титула (вызывается из Progress.applyStats). */
    public static void apply(ServerPlayer p) {
        clear(p, Attributes.MAX_HEALTH); clear(p, Attributes.MOVEMENT_SPEED); clear(p, Attributes.ATTACK_DAMAGE);
        Def def = active(Progress.d(p));
        if (def == null) return;
        switch (def.bonus()) {
            case HP -> mod(p, Attributes.MAX_HEALTH, def.value());
            case SPEED -> mod(p, Attributes.MOVEMENT_SPEED, def.value());
            case DAMAGE -> mod(p, Attributes.ATTACK_DAMAGE, def.value());
            default -> {}
        }
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    public static double boatBonus(ServerPlayer p) {
        Def def = active(Progress.d(p));
        return def != null && def.bonus() == Bonus.BOAT ? def.value() : 0;
    }

    private static void clear(ServerPlayer p, Holder<Attribute> a) {
        AttributeInstance i = p.getAttribute(a);
        if (i != null) i.removeModifier(MOD_ID);
    }

    private static void mod(ServerPlayer p, Holder<Attribute> a, double v) {
        AttributeInstance i = p.getAttribute(a);
        if (i != null) i.addPermanentModifier(new AttributeModifier(MOD_ID, v, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    /** Префикс над ником. */
    public static Component prefix(StoryData d) {
        Def def = active(d);
        if (def == null) return null;
        return Component.literal("«" + def.name() + "» ").withStyle(s -> s.withColor(def.color()));
    }
}
