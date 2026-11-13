package com.pvpciv.menu;

import com.pvpciv.game.GameEvents;
import com.pvpciv.inv.Bag;
import com.pvpciv.item.ArmorCoreItem;
import com.pvpciv.item.CivWeaponItem;
import com.pvpciv.loot.Gear;
import com.pvpciv.registry.ModRegistry;
import com.pvpciv.world.Basement;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import com.pvpciv.block.PedestalBlockEntity;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Алтарь инфузии: предметы кладутся руками на алтарь (центр) и 8 пьедесталов вокруг; окно только показывает круг и запускает ритуал. Рецепты:
 *  • Благословенная звезда — в центре оружие +5 и выше (сгорает), по краям 8 обычных звёзд. Шанс 50% на +5, +15% за каждый плюс (с +9 — 100%).
 *  • Ядро брони +N — в центре Медальон инея (или фильтр), по краям N звёзд (1–8); +9 — 7 обычных + 1 благословенная, +10 — 6 + 2.
 * Ритуал длится 2.5 с: предметы всплывают над пьедесталами и стягиваются к алтарю.
 */
public class InfusionMenu extends CivMenu {
    public static final int BTN_GO = 0, CX = 77, CY = 66, R = 40;
    public enum Kind { NONE, BLESSED, CORE }
    public final BlockPos altar;

    public InfusionMenu(int id, Inventory inv, BlockPos altar) {
        super(ModRegistry.INFUSION_MENU.get(), id, inv);
        this.altar = altar;
        addBag(169, 37, () -> true);
        addHotbar(inv, 169, 181);
    }

    /** Пьедесталы круга: 8 точек на радиусе 3 вокруг алтаря (в том же порядке, что и при постройке). */
    public static List<BlockPos> ring(BlockPos altar) {
        List<BlockPos> out = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            out.add(altar.offset((int) Math.round(Math.cos(a) * 3), 0, (int) Math.round(Math.sin(a) * 3)));
        }
        return out;
    }

    public static ItemStack at(Level level, BlockPos p) {
        return level.getBlockEntity(p) instanceof PedestalBlockEntity be ? be.item() : ItemStack.EMPTY;
    }

    public static int edges(List<ItemStack> e, net.minecraft.world.item.Item it) {
        int n = 0;
        for (ItemStack st : e) if (st.is(it)) n++;
        return n;
    }

    /** Уровень ядра по краям круга: N обычных звёзд (1–8) → +N; 7+1 благословенная → +9; 6+2 → +10. Иначе 0. */
    public static int coreLevel(List<ItemStack> e) {
        int normal = edges(e, ModRegistry.STAR_W1.get()), blessed = edges(e, ModRegistry.BLESSED_STAR_W1.get()), other = 0;
        for (ItemStack s : e) if (!s.isEmpty() && !s.is(ModRegistry.STAR_W1.get()) && !s.is(ModRegistry.BLESSED_STAR_W1.get())) other++;
        if (other > 0) return 0;
        if (blessed == 0 && normal >= 1 && normal <= 8) return normal;
        if (normal + blessed == 8 && (blessed == 1 || blessed == 2)) return 8 + blessed;
        return 0;
    }

    public static Kind kind(ItemStack c, List<ItemStack> e) {
        int normal = edges(e, ModRegistry.STAR_W1.get()), blessed = edges(e, ModRegistry.BLESSED_STAR_W1.get());
        if (c.getItem() instanceof CivWeaponItem && Gear.stars(c) >= 5 && normal == 8) return Kind.BLESSED;
        if ((c.is(ModRegistry.FROZEN_LOCKET.get()) || c.is(ModRegistry.GAS_MASK_FILTER.get()) || c.is(ModRegistry.JADE_SEAL.get())) && coreLevel(e) > 0) return Kind.CORE;
        return Kind.NONE;
    }

    public List<ItemStack> edgeItems(Level level) {
        List<ItemStack> e = new ArrayList<>();
        for (BlockPos p : ring(altar)) e.add(at(level, p));
        return e;
    }

    /** Шанс благословенной звезды от заточки оружия. */
    public static double chance(int stars) { return Math.min(1.0, 0.5 + 0.15 * (stars - 5)); }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (!(p instanceof ServerPlayer sp) || id != BTN_GO) return true;
        Level lvl = sp.level();
        if (!(lvl.getBlockEntity(altar) instanceof PedestalBlockEntity cbe)) return true;
        ItemStack center = cbe.item().copy();
        List<ItemStack> edgeItems = edgeItems(lvl);
        Kind k = kind(center, edgeItems);
        if (k == Kind.NONE) return true;
        // всё на круге должно быть твоим
        List<PedestalBlockEntity> all = new ArrayList<>();
        all.add(cbe);
        for (BlockPos q : ring(altar)) if (lvl.getBlockEntity(q) instanceof PedestalBlockEntity be) all.add(be);
        for (PedestalBlockEntity be : all)
            if (be.owner() != null && !be.owner().equals(sp.getUUID())) {
                sp.sendSystemMessage(Component.literal("На круге лежат чужие вещи (" + be.ownerName() + "). Ритуал проводят только своими.").withStyle(ChatFormatting.RED));
                return true;
            }
        List<ItemStack> copies = new ArrayList<>();
        for (ItemStack st : edgeItems) copies.add(st.copy());
        int coreLvl = coreLevel(edgeItems);
        for (PedestalBlockEntity be : all) be.set(ItemStack.EMPTY, null, "");
        ItemStack result;
        boolean ok;
        if (k == Kind.BLESSED) {
            ok = sp.getRandom().nextDouble() < chance(Gear.stars(center));
            result = ok ? new ItemStack(ModRegistry.BLESSED_STAR_W1.get()) : ItemStack.EMPTY;
        } else {
            ok = true;
            result = ArmorCoreItem.of(coreLvl);
        }
        sp.closeContainer();
        ritual(sp, center, copies, result, ok, k);
        return true;
    }

    private void ritual(ServerPlayer sp, ItemStack center, List<ItemStack> edgeItems, ItemStack result, boolean ok, Kind k) {
        ServerLevel lvl = sp.serverLevel();
        BlockPos a = altar;
        Vec3 c = Vec3.atBottomCenterOf(a).add(0, 1.3, 0);
        List<Display.ItemDisplay> shown = new ArrayList<>();
        List<Vec3> spots = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            double ang = Math.PI * 2 * i / 8;
            spots.add(Vec3.atBottomCenterOf(a).add(Math.round(Math.cos(ang) * 3), 1.3, Math.round(Math.sin(ang) * 3)));
        }
        for (int i = 0; i < 8; i++) shown.add(display(lvl, spots.get(i), edgeItems.get(i)));
        shown.add(display(lvl, c.add(0, 0.4, 0), center));
        lvl.playSound(null, a, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.2f, 0.7f);
        // 0.9.0: неоновые лучи от пьедесталов к алтарю и столб над ним
        // бета 0.9.6: лучи — только от пьедесталов, где что-то лежит; дугой вверх, с бегущим к центру огоньком
        for (int i = 0; i < 8; i++) if (!edgeItems.get(i).isEmpty()) { var f = com.pvpciv.entity.FxEntity.spawn(lvl, com.pvpciv.entity.FxEntity.BEAM, spots.get(i), 0.1f, 46, 0xE0204A, 0xFF9AD8); if (f != null) f.to(c); }
        com.pvpciv.entity.FxEntity.spawn(lvl, com.pvpciv.entity.FxEntity.ZONE, Vec3.atBottomCenterOf(a), 3.6f, 50, 0xC0103A, 0xFF80C0);
        DustParticleOptions red = new DustParticleOptions(new Vector3f(0.9f, 0.1f, 0.25f), 1.0f);
        for (int t = 0; t < 10; t++) {
            final int step = t;
            GameEvents.later(lvl.getServer(), 4 + t * 4, () -> {
                double f = (step + 1) / 10.0;
                for (int i = 0; i < 8; i++) {
                    Vec3 s = spots.get(i), cur = s.lerp(c, f * 0.85);
                    Display.ItemDisplay d = shown.get(i);
                    if (d != null) d.teleportTo(cur.x, cur.y + Math.sin(f * Math.PI) * 0.8, cur.z);
                }
                lvl.sendParticles(ParticleTypes.ENCHANT, c.x, c.y + 0.5, c.z, 20, 0.4, 0.6, 0.4, 0.6);
                lvl.playSound(null, a, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8f, 0.6f + step * 0.12f);
            });
        }
        GameEvents.later(lvl.getServer(), 50, () -> {
            for (Display.ItemDisplay d : shown) if (d != null) d.discard();
            if (ok) {
                lvl.playSound(null, a, SoundEvents.TOTEM_USE, SoundSource.BLOCKS, 0.8f, 1.3f);
                lvl.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 0.5, c.z, 40, 0.3, 0.6, 0.3, 0.12);
                lvl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y, c.z, 10, 1.2, 0.3, 1.2, 0.02);
                com.pvpciv.entity.FxEntity.spawn(lvl, com.pvpciv.entity.FxEntity.PILLAR, Vec3.atBottomCenterOf(a).add(0, 1, 0), 0.6f, 24, 0xFF4FA0, 0xFFFFFF);
                // бета 0.8.7: результат ложится на алтарь — забираешь сам (ПКМ по алтарю)
                if (lvl.getBlockEntity(a) instanceof PedestalBlockEntity be && be.item().isEmpty()) be.set(result.copy(), sp.getUUID(), sp.getGameProfile().getName());
                else Bag.give(sp, result);
                sp.sendSystemMessage(Component.literal(k == Kind.BLESSED ? "Инфузия удалась: Благословенная звезда! " : "Ритуал завершён: ").withStyle(ChatFormatting.LIGHT_PURPLE)
                        .append(k == Kind.CORE ? result.getHoverName() : Component.empty())
                        .append(Component.literal(" — забери с алтаря (ПКМ).").withStyle(ChatFormatting.GRAY)));
            } else {
                lvl.playSound(null, a, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.2f, 0.6f);
                lvl.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y, c.z, 40, 0.6, 0.4, 0.6, 0.03);
                sp.sendSystemMessage(Component.literal("Инфузия сорвалась: звёзды погасли, оружие рассыпалось пеплом.").withStyle(ChatFormatting.RED));
            }
        });
    }

    private static Display.ItemDisplay display(ServerLevel lvl, Vec3 at, ItemStack st) {
        Display.ItemDisplay d = EntityType.ITEM_DISPLAY.create(lvl);
        if (d == null) return null;
        d.moveTo(at.x, at.y, at.z);
        d.getSlot(0).set(st.copyWithCount(1));
        d.addTag("pvpciv_infusion");
        lvl.addFreshEntity(d);
        return d;
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) { return moveBetween(index, -1, -1); }
}
