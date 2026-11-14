package com.pvpciv.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pvpciv.item.CivWeaponItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 0.10.3 — клиентская часть своей боёвки.
 *  • Без закликивания: пока рука не заряжена, клик не бьёт, а ждёт в буфере (до 0.2 с) и срабатывает сам.
 *  • Две руки: клик бьёт готовой рукой (основная — ванильная атака, вторая — свой пакет), у каждой свой заряд.
 *  • Взмахи без рук от первого лица: оружие проходит дугу по классу (меч/сабля — горизонтальный рез туда-обратно,
 *    кинжал — выпад, топор — диагональ, молот — удар сверху), за ним — шлейф-полумесяц (Lines.slash, его видят и другие).
 */
public final class ClientCombat {
    private ClientCombat() {}

    private static long offLast = -1000, pending = 0;
    private static boolean firing;

    // ------------------------------------------------------------------ 0.10.4: комбо
    /** Окно точного удара после готовности руки (мс) и буфер «чуть раньше» (мс). */
    public static final long WINDOW = 200, EARLY = 110;
    public static int combo = 0;
    public static long comboAt = 0, readyMain = 0, readyOff = 0, lastHitPrecise = 0;
    private static boolean wasMain = true, wasOff = true;
    private static boolean pendingPrecise;

    /** Отметки момента готовности рук (для окна) — каждый кадр/тик. */
    public static void trackReady() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        long now = System.currentTimeMillis();
        boolean m = p.getAttackStrengthScale(0f) >= 0.999f, o = offCharge(p) >= 0.999f;
        if (m && !wasMain) readyMain = now;
        if (o && !wasOff) readyOff = now;
        wasMain = m; wasOff = o;
        if (combo > 0 && now - comboAt > 1500) combo = 0;
    }

    /** Классифицировать удар рукой (0 — основная, 1 — вторая) и отправить стак на сервер. */
    private static void judge(int hand, boolean buffered) {
        long now = System.currentTimeMillis(), ready = hand == 0 ? readyMain : readyOff;
        var hr = Minecraft.getInstance().hitResult;
        if (hr == null || hr.getType() != HitResult.Type.ENTITY) {   // 0.10.5: по воздуху комбо не набивается (и не тратится)
            PacketDistributor.sendToServer(new com.pvpciv.net.Network.PhoneCmd("combo:0"));
            return;
        }
        boolean precise = buffered || (ready > 0 && now - ready <= WINDOW);
        if (precise) {
            combo = Math.min(com.pvpciv.combat.Melee.COMBO_MAX, combo + 1);
            lastHitPrecise = now;
            var mc = Minecraft.getInstance();
            if (mc.player != null) mc.player.playSound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HAT.value(), 0.6f, 1.3f + combo * 0.12f);
        } else combo = 0;
        comboAt = now;
        PacketDistributor.sendToServer(new com.pvpciv.net.Network.PhoneCmd("combo:" + (precise ? combo : 0)));
    }

    /** Насколько полоска руки должна «вспыхнуть» (окно точного удара): 0…1. */
    public static float flash(int hand) {
        long r = hand == 0 ? readyMain : readyOff, el = System.currentTimeMillis() - r;
        return r > 0 && el <= WINDOW ? 1f - el / (float) WINDOW : 0f;
    }

    /** Анимация руки: начало (мс), длительность, класс, направление. */
    private static final long[] START = new long[2];
    private static final int[] CLS = new int[2], DIR = {1, 1};
    private static final int[] DUR = new int[2];

    private static boolean weapon(ItemStack st) { return st.getItem() instanceof CivWeaponItem; }
    private static boolean dual(LocalPlayer p) { return weapon(p.getMainHandItem()) && weapon(p.getOffhandItem()); }

    public static float offCharge(LocalPlayer p) {
        double d = com.pvpciv.combat.Melee.delay(p, p.getOffhandItem(), true);
        return (float) Math.min(1, (p.level().getGameTime() - offLast + 0.5) / d);
    }
    private static boolean mainReady(LocalPlayer p) { return p.getAttackStrengthScale(0f) >= 0.92f; }

    /** 0.10.6: в паре — строгая очередь рук (лево-право); клик бьёт рукой, чья очередь, по её заряду. */
    private static boolean nextOff = false;

    /** Для шкалы под прицелом: чья очередь (true — вторая рука) и её заряд. */
    public static boolean nextIsOff(LocalPlayer p) { return dual(p) && nextOff; }
    public static float nextCharge(LocalPlayer p) { return nextIsOff(p) ? offCharge(p) : p.getAttackStrengthScale(0f); }
    public static boolean isDual(LocalPlayer p) { return dual(p); }

    private static long lastPlantEvt;

    public static void onAttackKey(InputEvent.InteractionKeyMappingTriggered e) {
        if (!e.isAttack() || firing) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || p.isSpectator() || !weapon(p.getMainHandItem())) return;   // без оружия — ваниль
        boolean plant = false;
        if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bh && bh.getType() == HitResult.Type.BLOCK) {
            if (p.isCreative()) return;   // в творческом — копать как обычно
            // 0.11 (бета): ломать блоки в мире всё равно нельзя — клик оружием по блоку (куст, трава, земля, стена) это взмах по воздуху.
            // Раньше удар уходил в «копание»: зажатая атака долбила куст каждый тик, а взмаха и удара не было.
            e.setCanceled(true);
            e.setSwingHand(false);
            long now = p.level().getGameTime();
            boolean fresh = now - lastPlantEvt > 1;
            lastPlantEvt = now;
            if (!fresh) return;
            mc.hitResult = net.minecraft.world.phys.BlockHitResult.miss(bh.getLocation(), bh.getDirection(), bh.getBlockPos());
            plant = true;
        }
        boolean dual = dual(p);
        if (!dual) nextOff = false;
        trackReady();
        boolean off = dual && nextOff;
        float ch = off ? offCharge(p) : p.getAttackStrengthScale(0f);
        if (!off && ch >= 0.92f) {   // основная рука: ваниль дальше сделает атаку/взмах
            judge(0, pendingPrecise); pendingPrecise = false; pending = 0;
            mainStrike(mc, p);
            if (plant) p.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            if (dual) nextOff = true;
            return;
        }
        e.setCanceled(true);
        e.setSwingHand(false);
        if (off && ch >= 0.92f) { judge(1, pendingPrecise); pendingPrecise = false; pending = 0; offStrike(mc, p); nextOff = false; return; }
        // рано: в последние доли секунды — в буфер (засчитается точным), совсем рано (< 70%) — срыв ритма; закликивание не бьёт
        double d = off ? com.pvpciv.combat.Melee.delay(p, p.getOffhandItem(), true) : 20.0 / Math.max(0.05, p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED));
        double left = (1 - ch) * d * 50;
        if (ch < 0.7f) { combo = 0; pending = 0; return; }
        if (left <= EARLY) pending = System.currentTimeMillis();
    }

    /** Основная рука: анимация + (по воздуху) дуга на сервере. Атаку по сущности делает ваниль. */
    private static void mainStrike(Minecraft mc, LocalPlayer p) {
        pending = 0;
        if (dual(p)) {   // вторая рука — не раньше чем через полпериода (ровный ритм левой-правой)
            double d = com.pvpciv.combat.Melee.delay(p, p.getOffhandItem(), true);
            offLast = Math.max(offLast, p.level().getGameTime() - (long) Math.floor(d * 0.5));
            wasOff = false;
        }
        anim(0, p.getMainHandItem());
        boolean miss = mc.hitResult == null || mc.hitResult.getType() == HitResult.Type.MISS;
        if (miss) {
            PacketDistributor.sendToServer(new com.pvpciv.net.Network.PhoneCmd("swing"));
            p.resetAttackStrengthTicker();
        }
        Lines.slash(p, p.getMainHandItem(), false);
    }

    private static void offStrike(Minecraft mc, LocalPlayer p) {
        pending = 0;
        offLast = p.level().getGameTime();
        int id = mc.hitResult instanceof EntityHitResult eh ? eh.getEntity().getId() : -1;
        PacketDistributor.sendToServer(new com.pvpciv.net.Network.PhoneCmd("off:" + id));
        com.pvpciv.combat.Melee.capMain(p, 0.5);
        wasMain = false;
        anim(1, p.getOffhandItem());
        Lines.slash(p, p.getOffhandItem(), true);
    }

    /** Каждый тик: клик из буфера — как только рука готова. */
    public static void tick() {
        trackReady();
        if (pending == 0) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || System.currentTimeMillis() - pending > 220 || mc.screen != null) { pending = 0; return; }
        boolean off = dual(p) && nextOff;
        if (!off && mainReady(p)) {
            pending = 0;
            pendingPrecise = true;   // клик из буфера: сработает точно в момент готовности
            firing = true;
            try { net.minecraft.client.KeyMapping.click(mc.options.keyAttack.getKey()); } finally { firing = false; }
            // клик из буфера пройдёт обычным путём (onAttackKey): анимация, дуга, ванильная атака
        } else if (off && offCharge(p) >= 0.92f) { pending = 0; judge(1, true); offStrike(mc, p); nextOff = false; }
    }

    private static void anim(int hand, ItemStack st) {
        int cls = st.getItem() instanceof CivWeaponItem w ? w.cls.ordinal() : 0;
        CLS[hand] = cls;
        DIR[hand] = -DIR[hand];
        int base = switch (cls) { case 2 -> 170; case 1 -> 230; case 0 -> 280; case 3 -> 330; default -> 400; };   // кинжал/сабля/меч/топор/молот
        // 0.10.5: скорость взмаха — от реальной скорости атаки игрока (бонусы, пара): не дольше 85% перезарядки руки
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) {
            double ms = (hand == 0 ? 20.0 / Math.max(0.05, p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED))
                    : com.pvpciv.combat.Melee.delay(p, st, dual(p))) * 50;
            base = (int) Math.max(110, Math.min(base, ms * 0.85));
        }
        DUR[hand] = base;
        START[hand] = System.currentTimeMillis();
    }

    private static float ease(float t) { return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2; }

    /** Своя отрисовка оружия в руке от первого лица: без руки, взмах по дуге. */
    public static void onRenderHand(RenderHandEvent e) {
        ItemStack st = e.getItemStack();
        if (!weapon(st)) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        int hand = e.getHand() == InteractionHand.MAIN_HAND ? 0 : 1;
        boolean right = (p.getMainArm() == HumanoidArm.RIGHT) == (hand == 0);
        e.setCanceled(true);
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        float side = right ? 1 : -1;
        float equip = e.getEquipProgress();
        ps.translate(side * 0.56f, -0.52f - equip * 0.6f, -0.72f);
        long el = System.currentTimeMillis() - START[hand];
        if (el < DUR[hand]) {
            float t = el / (float) DUR[hand], d = DIR[hand] * side;
            switch (CLS[hand]) {
                case 2 -> {   // кинжал: выпад вперёд с поворотом
                    float k = t < 0.35f ? t / 0.35f : 1 - (t - 0.35f) / 0.65f;
                    k = ease(Math.max(0, k));
                    ps.translate(-side * 0.18f * k, 0.08f * k, -0.45f * k);
                    ps.mulPose(Axis.XP.rotationDegrees(-35 * k));
                    ps.mulPose(Axis.ZP.rotationDegrees(d * 25 * k));
                }
                case 3 -> {   // топор: диагональ сверху-сбоку вниз
                    float k = t < 0.22f ? -t / 0.22f : -1 + 2 * ease((t - 0.22f) / 0.78f);
                    float back = t > 0.75f ? (t - 0.75f) / 0.25f : 0;
                    ps.translate(-side * 0.25f * (1 - back), 0.15f * (1 - back), -0.2f * (1 - back));
                    ps.mulPose(Axis.YP.rotationDegrees(d * 40 * k * (1 - back)));
                    ps.mulPose(Axis.XP.rotationDegrees((-70 + 120 * (k + 1) / 2) * (1 - back)));
                    ps.mulPose(Axis.ZP.rotationDegrees(d * 35 * (1 - back)));
                }
                case 4 -> {   // молот: замах вверх и удар
                    float up = t < 0.35f ? ease(t / 0.35f) : 1 - ease(Math.min(1, (t - 0.35f) / 0.3f)) * 1.6f;
                    float back = t > 0.75f ? (t - 0.75f) / 0.25f : 0;
                    up = up * (1 - back);
                    ps.translate(-side * 0.15f, 0.25f * Math.max(0, up) - 0.1f * Math.max(0, -up), -0.2f);
                    ps.mulPose(Axis.XP.rotationDegrees(-95 * up));
                }
                default -> {  // меч и сабля: горизонтальный рез, следующий — в обратную сторону
                    float k = t < 0.2f ? -ease(t / 0.2f) * 0.6f : -0.6f + 1.9f * ease(Math.min(1, (t - 0.2f) / 0.55f));
                    float back = t > 0.75f ? ease((t - 0.75f) / 0.25f) : 0;
                    float a = k * (1 - back);
                    ps.translate(-side * 0.32f * Math.abs(a), 0.1f * Math.abs(a), -0.25f * Math.abs(a));
                    ps.mulPose(Axis.YP.rotationDegrees(-d * 70 * a));
                    ps.mulPose(Axis.ZP.rotationDegrees(d * 70 * Math.min(1, Math.abs(a) * 1.4f)));
                    ps.mulPose(Axis.XP.rotationDegrees(-30 * Math.abs(a)));
                }
            }
        } else {
            // покачивание в покое
            float b = (float) Math.sin((p.tickCount + e.getPartialTick()) * 0.07) * 0.012f;
            ps.translate(0, b, 0);
        }
        mc.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(p, st,
                right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !right,
                ps, e.getMultiBufferSource(), e.getPackedLight());
        ps.popPose();
    }
}
