package com.pvpciv.entity;

import com.pvpciv.game.DialogueManager;
import com.pvpciv.game.Milestones;
import com.pvpciv.game.Story;
import com.pvpciv.inv.Bag;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import static com.pvpciv.game.DialogueManager.opt;

/** NPC Неонового города: Тимофей (вехи), парашютистка Ника (парашют), сантехник Гоша (канализация). */
public class CityNpcEntity extends StoryNpc {
    public enum Role { TIMOFEY("Тимофей Глыба", 0xE05050), PARACHUTIST("Ника-парашютистка", 0xF09040), PLUMBER("Сантехник Гоша", 0x4A8AE0), SMUGGLER("Контрабандист Шнырь", 0xA070E0),
        COURIER("Курьер «Шпиль.Еды»", 0xF2C230), LIBRARIAN("Хранитель Сёдо", 0xC8A86A), ASSASSIN("Юки, Тихая тень", 0xE0405A), CHEF("Повар Харуки", 0xF0A040);
        public final String name; public final int color;
        Role(String n, int c) { name = n; color = c; } }
    public final Role role;
    /** Курьер: чей заказ привёз (не сохраняется — курьер живёт только пока ждёт у двери). */
    public java.util.UUID owner;
    public net.minecraft.world.phys.Vec3 home;
    public int leaveAt = -1;

    public CityNpcEntity(EntityType<? extends PathfinderMob> type, Level level, Role role) {
        super(type, level, role.name, role.color);
        this.role = role;
    }

    @Override public boolean shouldBeSaved() { return false; }

    @Override
    public void tick() {
        super.tick();
        if (role != Role.COURIER || level().isClientSide) return;
        if (home != null && position().distanceToSqr(home) > 1) { teleportTo(home.x, home.y, home.z); setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO); }
        if (owner == null) {
            if (leaveAt > 0 && tickCount >= leaveAt) {
                if (level() instanceof net.minecraft.server.level.ServerLevel l) l.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, getX(), getY() + 1, getZ(), 12, 0.3, 0.5, 0.3, 0.02);
                discard();
            } else if (leaveAt < 0 && tickCount > 200) discard();
            return;
        }
        if (tickCount % 20 == 0 && getServer() != null) {
            var pl = getServer().getPlayerList().getPlayer(owner);
            if (pl == null || (com.pvpciv.progress.Progress.d(pl).food.getInt("state") != 2 && !com.pvpciv.game.DialogueManager.inDialogue(pl))) { discard(); return; }
            if (pl.level() == level() && distanceToSqr(pl) < 100) getLookControl().setLookAt(pl);
        }
    }

    @Override
    protected void onTalk(ServerPlayer p) {
        if (DialogueManager.inDialogue(p)) return;
        switch (role) {
            case COURIER -> com.pvpciv.game.FoodApp.talk(p, this);
            case LIBRARIAN, ASSASSIN -> {}
            case CHEF -> {}
            case TIMOFEY -> {
                Story.say(p, role.name, role.color, "Дневник подвигов открыт! Смотри, как растёшь — прям как акции.");
                Milestones.open(p);
            }
            case PARACHUTIST -> {
                boolean has = Bag.has(p, s -> s.is(ModRegistry.PARACHUTE.get()));
                DialogueManager.Dialogue d = new DialogueManager.Dialogue("a");
                if (has) d.node("a", role.name, role.color, "Парашют при тебе? Отлично. Упадёшь с края — сам раскроется. Только не целься в Громилу.", opt("[Уйти]", null));
                else d.node("a", role.name, role.color, "Эй, новенький! Знаешь, что будет, если шагнуть с края этажа? Полетишь вниз, на этаж 1. Быстро. Насмерть. "
                                + "Держи парашют — раскроется сам, если долго падать. Мне не жалко, у меня их сорок.",
                        pp -> {
                            Bag.give(pp, new ItemStack(ModRegistry.PARACHUTE.get()));
                            pp.sendSystemMessage(Component.literal("⛱ Получен парашют. Работает из сумки.").withStyle(ChatFormatting.GOLD));
                        }, opt("Спасибо!", null));
                DialogueManager.open(p, this, d);
            }
            case SMUGGLER -> {
                String[] hi = {"Тс-с. Дошёл по лаве — значит, свой. Смотри товар.", "Катана с третьего этажа? Есть. Но не за просто так.", "Яблоки золотые — две штуки в завоз. Больше не проси."};
                Story.say(p, role.name, role.color, hi[p.getRandom().nextInt(hi.length)]);
                com.pvpciv.game.Shop.VENDOR.put(p.getUUID(), "smuggler");
                p.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new com.pvpciv.menu.TraderMenu(id, inv), Component.literal("Контрабандист")));
                com.pvpciv.game.Shop.sendOffers(p);
            }
            case PLUMBER -> DialogueManager.open(p, this, new DialogueManager.Dialogue("a")
                    .node("a", role.name, role.color, "Тише ты! Тут крысы слушают. У них король — здоровый, в короне, и, клянусь, с прессом. Сидит в Тронном стоке на западе. "
                            + "Если идёшь к нему — бери противогаз. И не наступай в зелёное.", opt("А автоматчики?", "b"), opt("[Уйти]", null))
                    .node("b", role.name, role.color, "Люди Дона. Прячут тут товар. Бьют очередями — уходи с линии огня за угол. Я их не трогаю, они меня тоже.", opt("[Уйти]", null)));
        }
    }

    @Override
    protected void onPunched(ServerPlayer p) {
        if (role == Role.TIMOFEY) Story.say(p, role.name, role.color, "Неплохо! Но по груше бей, а не по тренеру.");
    }
}
