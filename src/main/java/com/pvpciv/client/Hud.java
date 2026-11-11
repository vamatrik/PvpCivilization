package com.pvpciv.client;

import com.pvpciv.client.ui.Skin;
import com.pvpciv.item.CivWeaponItem;
import com.pvpciv.loot.Gear;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Игровой HUD: плита игрока, таймер босса, цель, хотбар, полоски, навыки, баннер зоны, PvP-рамка. */
public final class Hud {
    private static final String[] ZONE_GEM = {"green", "amber", "red"};
    private static final String[] ZONE_SUB = {"Безопасная зона", "PvE-зона: монстры", "PvP-зона: игроки могут атаковать!"};
    private static final int[] SKILL_LEVELS = {1, 8, 18};

    public static void render(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.level == null) return;
        if (mc.getDebugOverlay().showDebugScreen() || mc.screen instanceof DialogueScreen || mc.screen instanceof AbstractContainerScreen<?>
                || mc.screen instanceof com.pvpciv.client.screen.PlainScreen || mc.screen instanceof com.pvpciv.client.screen.CivDeathScreen) return;
        Font font = mc.font;
        long now = mc.level.getGameTime();
        int w = g.guiWidth(), h = g.guiHeight();
        if (cinematic(g, font, w, h)) return;
        if (ClientState.jailSeconds >= 0) jail(g, font, w);
        phone(g, font, w, h);

        if (ClientState.zoneType == 2) pvpFrame(g, w, h);
        if (!ClientState.zone.isEmpty()) {
            playerPlate(g, font, now);
            zoneBanner(g, font, w);
        }
        if (!mc.player.isSpectator()) bottom(g, mc, font, now, w, h);
    }

    private static void pvpFrame(GuiGraphics g, int w, int h) {
        int a = 0x38 + (int) (0x20 * Math.sin(System.currentTimeMillis() / 350.0));
        int c = (a << 24) | 0xFF1818;
        g.fillGradient(0, 0, w, 12, c, 0x00FF1818);
        g.fillGradient(0, h - 12, w, h, 0x00FF1818, c);
        for (int i = 0; i < 10; i++) { int ai = (a * (10 - i) / 10) << 24 | 0xFF1818; g.fill(i, 0, i + 1, h, ai); g.fill(w - i - 1, 0, w - i, h, ai); }
    }

    private static void playerPlate(GuiGraphics g, Font font, long now) {
        int x = 5, y = 5, pw = 128;
        Skin.plate(g, x, y, pw, 43);
        g.drawString(font, "Ур. " + ClientState.level, x + 8, y + 6, Skin.GOLD_L, true);
        Skin.money(g, font, x + pw - 8, y + 6, ClientState.money, now < ClientState.moneyFlashUntil ? 0xFFFFFFFF : Skin.GOLD_L);
        float f = ClientState.xpNext > 0 ? ClientState.xp / (float) ClientState.xpNext : 0;
        Skin.thinBar(g, x + 8, y + 18, pw - 16, 3, f, 0xFF8FE3FF, 0xFF2A7FB8);
        String xp = ClientState.xp + "/" + ClientState.xpNext;
        g.pose().pushPose();
        g.pose().translate(x + pw - 8 - font.width(xp) * 0.75f, y + 23.5f, 0);
        g.pose().scale(0.75f, 0.75f, 1);
        g.drawString(font, xp, 0, 0, Skin.FAINT, false);
        g.pose().popPose();
        Skin.gem(g, ZONE_GEM[Math.min(2, ClientState.zoneType)], x + 5, y + 29);
        g.drawString(font, Skin.fit(font, shortZone(), pw - 26), x + 17, y + 30, ClientState.zoneType == 2 ? 0xFFFF8080 : Skin.TEXT, false);

        int ny = y + 46;
        for (String[] bd : new String[][]{{"brute", "Громила"}, {"keeper", "Хранительница"}, {"don", "Дон"}, {"rat_king", "Крысиный король"}}) {
            if (!ClientState.pinned(bd[0])) continue;
            int s = ClientState.bossSeconds(bd[0]);
            boolean awake = s < 0;
            Skin.plate(g, x, ny, pw, 17);
            if (awake) g.fill(x + 2, ny + 2, x + pw - 2, ny + 15, (0x50 + (int) (0x30 * Math.sin(System.currentTimeMillis() / 250.0))) << 24 | (bd[0].equals("keeper") ? 0x1060C0 : 0xC01010));
            Skin.icon(g, "skull", x + 4, ny + 2);
            String bt = awake ? bd[1] + (bd[0].equals("keeper") ? " проснулась!" : " проснулся!")
                    : s == 0 ? bd[1] + " ждёт" : String.format("%s: %d:%02d", bd[1], s / 60, s % 60);
            g.drawString(font, Skin.fit(font, bt, pw - 24), x + 19, ny + 5, awake ? (bd[0].equals("keeper") ? 0xFF80D0FF : 0xFFFF7060) : s <= 60 ? Skin.WARN : Skin.DIM, false);
            ny += 20;
        }

        if (!ClientState.quest.isEmpty()) {
            List<FormattedCharSequence> ql = font.split(Component.literal(ClientState.quest), pw - 26);
            int qh = 8 + ql.size() * 10 + (ClientState.questSeconds >= 0 ? 10 : 0);
            Skin.plate(g, x, ny, pw, qh);
            g.fill(x + 2, ny + 2, x + 4, ny + qh - 2, 0xFFD23A54);
            int ly = ny + 5;
            for (FormattedCharSequence l : ql) { g.drawString(font, l, x + 9, ly, 0xFFF0C8D0, false); ly += 10; }
            if (ClientState.questSeconds >= 0) {
                int qs = ClientState.questSeconds;
                g.drawString(font, String.format("⏳ %d:%02d", qs / 60, qs % 60), x + 9, ly, qs < 120 ? 0xFFFF6070 : Skin.DIM, false);
            }
            ny += qh + 3;
        }

        if (ClientState.stage == 0 || now < ClientState.objectiveFlashUntil + 500) {
            List<FormattedCharSequence> lines = font.split(Component.literal(ClientState.objective), pw - 16);
            Skin.plate(g, x, ny, pw, 10 + lines.size() * 10);
            int ly = ny + 5;
            for (FormattedCharSequence l : lines) { g.drawString(font, l, x + 8, ly, 0xFFD8D2E6, false); ly += 10; }
        }
    }

    /** Входящий звонок (трясущаяся плашка) и такси, которое ждёт на точке посадки. */
    private static void phone(GuiGraphics g, Font font, int w, int h) {
        long now = System.currentTimeMillis();
        if (ClientState.callUntil > now) {
            int s = (int) ((ClientState.callUntil - now) / 1000) + 1;
            int shake = (int) Math.round(Math.sin(now / 30.0) * 2), x = w - 188 + shake, y = h / 2 - 40;
            Skin.plate(g, x, y, 182, 46);
            Skin.glow(g, x + 20, y + 22, 18, 0x6050FF80);
            g.renderItem(new net.minecraft.world.item.ItemStack(com.pvpciv.registry.ModRegistry.PHONE.get()), x + 12, y + 14);
            g.drawString(font, "Звонит Элеонора…", x + 36, y + 8, 0xFF8FFF9F, true);
            String key = com.pvpciv.client.ClientInit.PHONE.getTranslatedKeyMessage().getString().toUpperCase();
            g.drawString(font, "[" + key + "] — ответить", x + 36, y + 20, 0xFFFFFFFF, true);
            g.drawString(font, "не ответишь — обидится (" + s + ")", x + 36, y + 31, 0xFFB0B8C8, false);
        } else if (ClientState.taxiUntil > now) {
            int s = (int) ((ClientState.taxiUntil - now) / 1000);
            String t = ClientState.taxiHere ? "Такси здесь: стой спокойно…" : "Такси ждёт у «" + ClientState.taxiFrom + "» " + String.format("%d:%02d", s / 60, s % 60);
            int tw = font.width(t) + 16, x = w - tw - 6, y = h / 2 - 30;
            Skin.plate(g, x, y, tw, 16);
            g.drawString(font, t, x + 8, y + 4, 0xFFFFE070, true);
        }
    }

    /** Заточение у Кармиллы: крупный таймер сверху по центру. */
    private static void jail(GuiGraphics g, Font font, int w) {
        int s = ClientState.jailSeconds, cx = w / 2, y = 30;
        Skin.spr(g, "header", cx - 80, y, 160, 22, 0xFFFFC0C8);
        g.drawCenteredString(font, "ЗАТОЧЕНИЕ В ПОДВАЛЕ", cx, y + 3, 0xFFFF8090);
        g.drawCenteredString(font, String.format("%d:%02d", s / 60, s % 60), cx, y + 12, 0xFFFFFFFF);
    }

    /** Катсцена: кинорамки, багровая виньетка, затемнение. true — HUD не рисовать. */
    private static boolean cinematic(GuiGraphics g, Font font, int w, int h) {
        if (ClientState.cineKind == 0) return false;
        long t = System.currentTimeMillis() - ClientState.cineStart;
        if (t > ClientState.cineLen) { ClientState.cineKind = 0; return false; }
        float k = t / (float) ClientState.cineLen;
        if (ClientState.cineKind == 1) {
            int bar = (int) (h * 0.12f * Math.min(1, t / 400f));
            float pulse = 0.5f + 0.5f * (float) Math.sin(t / 180.0);
            int va = (int) (90 + 80 * pulse);
            for (int i = 0; i < 24; i++) {
                int a = va * (24 - i) / 24;
                int c = (a << 24) | 0x600008;
                g.fill(i * 3, 0, i * 3 + 3, h, c); g.fill(w - i * 3 - 3, 0, w - i * 3, h, c);
                g.fill(0, i * 2, w, i * 2 + 2, c); g.fill(0, h - i * 2 - 2, w, h - i * 2, c);
            }
            g.fill(0, 0, w, bar, 0xFF000000);
            g.fill(0, h - bar, w, h, 0xFF000000);
            if (k > 0.62f) g.fill(0, 0, w, h, (int) (Math.min(1, (k - 0.62f) / 0.25f) * 255) << 24);
            float ta = Math.min(1, t / 500f) * (k > 0.85f ? 0 : 1);
            if (ta > 0.05f) {
                g.pose().pushPose();
                g.pose().translate(w / 2f, h - bar + bar / 2f - 6, 0);
                g.pose().scale(1.4f, 1.4f, 1);
                g.drawCenteredString(font, ClientState.cineText, 0, 0, Skin.alpha(0xFFFF6A7A, ta));
                g.pose().popPose();
            }
        } else if (ClientState.cineKind == 5 || ClientState.cineKind == 6) {   // поездка на лифте: полосы кино, затемнение/проявление, титр
            int bar = (int) (h * 0.09f);
            g.fill(0, 0, w, bar, 0xFF000000);
            g.fill(0, h - bar, w, h, 0xFF000000);
            float black = ClientState.cineKind == 5 ? Math.max(0, (k - 0.78f) / 0.22f) : Math.max(0, 1 - k / 0.18f);
            if (black > 0) g.fill(0, 0, w, h, ((int) (Math.min(1, black) * 255)) << 24);
            if (ClientState.cineKind == 6 && !ClientState.cineText.isEmpty()) {
                String[] lines = ClientState.cineText.split("\\n");
                float ta = k < 0.15f ? 0 : k < 0.3f ? (k - 0.15f) / 0.15f : k > 0.85f ? (1 - k) / 0.15f : 1;
                if (ta > 0.05f) {
                    g.pose().pushPose(); g.pose().translate(w / 2f, h * 0.22f, 0); g.pose().scale(3f, 3f, 1);
                    g.drawCenteredString(font, lines[0], 0, 0, Skin.alpha(0xFFFFE08A, ta)); g.pose().popPose();
                    if (lines.length > 1) { g.pose().pushPose(); g.pose().translate(w / 2f, h * 0.22f + 32, 0); g.pose().scale(1.5f, 1.5f, 1);
                        g.drawCenteredString(font, lines[1], 0, 0, Skin.alpha(0xFF8FE3FF, ta)); g.pose().popPose(); }
                }
            }
            return true;
        } else if (ClientState.cineKind == 3) {   // Небесный лифт: подъём, затемнение, титр этажа
            int bar = (int) (h * 0.1f);
            if (k < 0.5f) {
                float sk = k / 0.5f;
                g.fillGradient(0, 0, w, h, ((int) (sk * 160) << 24) | 0x0A1840, ((int) (sk * 120) << 24) | 0x40A0E0);
                for (int i = 0; i < 40; i++) {
                    int x = (i * 97) % w, len = 20 + (i * 31) % 50;
                    int y = (int) ((t * (0.6 + (i % 5) * 0.15) + i * 53) % (h + len)) - len;
                    g.fill(x, y, x + 1, y + len, 0x60FFFFFF);
                }
            }
            float black = k < 0.38f ? 0 : k < 0.5f ? (k - 0.38f) / 0.12f : k < 0.62f ? 1 : Math.max(0, 1 - (k - 0.62f) / 0.18f);
            if (black > 0) g.fill(0, 0, w, h, ((int) (black * 255)) << 24);
            g.fill(0, 0, w, bar, 0xFF000000);
            g.fill(0, h - bar, w, h, 0xFF000000);
            String[] lines = ClientState.cineText.split("\\n");
            float ta = k < 0.55f ? 0 : k < 0.65f ? (k - 0.55f) / 0.1f : k > 0.92f ? (1 - k) / 0.08f : 1;
            if (ta > 0.05f) {
                g.pose().pushPose();
                g.pose().translate(w / 2f, h / 2f - 20, 0);
                g.pose().scale(3f, 3f, 1);
                g.drawCenteredString(font, lines[0], 0, 0, Skin.alpha(0xFFFFE08A, ta));
                g.pose().popPose();
                if (lines.length > 1) {
                    g.pose().pushPose();
                    g.pose().translate(w / 2f, h / 2f + 14, 0);
                    g.pose().scale(1.5f, 1.5f, 1);
                    g.drawCenteredString(font, lines[1], 0, 0, Skin.alpha(0xFF8FE3FF, ta));
                    g.pose().popPose();
                }
            }
            return true;
        } else if (ClientState.cineKind == 2) {
            float a = k < 0.15f ? k / 0.15f : 1 - (k - 0.15f) / 0.85f;
            float beat = (float) Math.abs(Math.sin(t / 260.0));
            g.fill(0, 0, w, h, ((int) (a * (110 + 70 * beat)) << 24) | 0x8A0010);
            for (int i = 0; i < 30; i++) {
                int c = ((int) (a * 200 * (30 - i) / 30) << 24);
                g.fill(i * 4, 0, i * 4 + 4, h, c); g.fill(w - i * 4 - 4, 0, w - i * 4, h, c);
            }
        }
        return ClientState.cineKind == 1;
    }

    /** 20 → «20», 20.5 → «20.5». */
    private static String num(float v) {
        float r = Math.round(v * 10) / 10f;
        return r == Math.floor(r) ? String.valueOf((int) r) : String.format(java.util.Locale.ROOT, "%.1f", r);
    }

    private static String shortZone() {
        String z = ClientState.zone;
        return z.contains("· ") ? z.substring(z.lastIndexOf("· ") + 2) : z;
    }

    private static void zoneBanner(GuiGraphics g, Font font, int w) {
        if (ClientState.cineKind != 0) return;
        long d = System.currentTimeMillis() - ClientState.zoneChangedAt;
        if (ClientState.zoneChangedAt <= 0 || d > 3200) return;
        float a = d < 300 ? d / 300f : d > 2600 ? 1 - (d - 2600) / 600f : 1;
        if (a < 0.06f) return;
        String name = shortZone();
        int bw = Math.max(120, font.width(name) * 3 / 2 + 60), cx = w / 2, y = 78;
        Skin.spr(g, "header", cx - bw / 2, y, bw, 22, Skin.alpha(0xFFFFFFFF, a));
        g.pose().pushPose();
        g.pose().translate(cx, y + 5, 0);
        g.pose().scale(1.5f, 1.5f, 1);
        g.drawCenteredString(font, name, 0, 0, Skin.alpha(Skin.GOLD_L, a));
        g.pose().popPose();
        int zt = Math.min(2, ClientState.zoneType);
        g.drawCenteredString(font, ZONE_SUB[zt], cx, y + 27, Skin.alpha(zt == 2 ? 0xFFFF7060 : zt == 1 ? 0xFFE8C07A : 0xFF9AE8B0, a));
    }

    private static void bottom(GuiGraphics g, Minecraft mc, Font font, long now, int w, int h) {
        int cx = w / 2, hw = 190, hx = cx - hw / 2, hy = h - 30;
        // хотбар
        Skin.plate(g, hx, hy, hw, 28);
        int sel = mc.player.getInventory().selected;
        for (int i = 0; i < 9; i++) {
            int ix = hx + 7 + i * 20, iy = hy + 6;
            Skin.slot(g, ix, iy);
            ItemStack st = mc.player.getInventory().items.get(i);
            if (st.getItem() instanceof CivWeaponItem cw)
                Skin.rarity(g, ix, iy, (Gear.rarity(st).ordinal() < cw.minRarity.ordinal() ? cw.minRarity : Gear.rarity(st)).color);
            if (i == sel) Skin.spr(g, "slot_selected", ix - 4, iy - 4, 24, 24);
            if (!st.isEmpty()) {
                g.renderItem(st, ix, iy);
                g.renderItemDecorations(font, st, ix, iy);
                int stars = Gear.stars(st);
                if (stars > 0 && st.getItem() instanceof CivWeaponItem) {
                    g.pose().pushPose(); g.pose().translate(0, 0, 200);
                    g.drawString(font, "+" + stars, ix, iy, 0xFF000000 | Gear.starColor(stars), true);
                    g.pose().popPose();
                }
            }
        }
        ItemStack off = mc.player.getOffhandItem();
        if (!off.isEmpty()) {
            Skin.plate(g, hx - 32, hy, 28, 28);
            Skin.slot(g, hx - 26, hy + 6);
            g.renderItem(off, hx - 26, hy + 6);
            g.renderItemDecorations(font, off, hx - 26, hy + 6);
        }
        // навыки
        int kx = hx + hw + 4;
        ItemStack main = mc.player.getMainHandItem();
        boolean wskill = main.getItem() instanceof com.pvpciv.item.CivWeaponItem cw && cw.skill != null;
        Skin.plate(g, kx, hy, wskill ? 92 : 70, 28);
        if (wskill) {
            int ix = kx + 7 + 3 * 20 + 2, iy = hy + 6;
            Skin.slot(g, ix, iy);
            g.renderItem(main, ix, iy);
            float cf = mc.player.getCooldowns().getCooldownPercent(main.getItem(), 0);
            if (cf > 0) {
                g.pose().pushPose(); g.pose().translate(0, 0, 200);
                g.fill(ix, iy + Math.round(16 * (1 - cf)), ix + 16, iy + 16, 0xC0000000);
                int secs = Math.round(cf * ((com.pvpciv.item.CivWeaponItem) main.getItem()).skill.cd);
                g.drawCenteredString(font, String.valueOf(Math.max(1, secs)), ix + 8, iy + 4, 0xFFFFFFFF);
                g.pose().popPose();
            } else Skin.glow(g, ix + 8, iy + 8, 11, 0x30FFD060);
            g.pose().pushPose();
            g.pose().translate(ix + 6, iy + 10, 210);
            g.pose().scale(0.75f, 0.75f, 1);
            g.drawString(font, "ПКМ", 0, 0, Skin.GOLD_L, true);
            g.pose().popPose();
        }
        for (int i = 0; i < 3; i++) {
            int ix = kx + 7 + i * 20, iy = hy + 6;
            boolean open = ClientState.level >= SKILL_LEVELS[i];
            if (!open) { Skin.slotLocked(g, ix, iy); continue; }
            Skin.slot(g, ix, iy);
            if (i == 0) {
                g.drawString(font, "»", ix + 5, iy + 1, Skin.ICE, true);
                long left = ClientState.dashReadyAt - now;
                if (left > 0) {
                    float cf = left / (float) com.pvpciv.game.DashSkill.COOLDOWN_TICKS;
                    g.fill(ix, iy + Math.round(16 * (1 - cf)), ix + 16, iy + 16, 0xC0000000);
                    g.drawCenteredString(font, String.valueOf((left + 19) / 20), ix + 8, iy + 4, 0xFFFFFFFF);
                } else Skin.glow(g, ix + 8, iy + 8, 11, 0x307FDBFF);
            }
            g.pose().pushPose();
            g.pose().translate(ix + 11, iy + 10, 200);
            g.pose().scale(0.75f, 0.75f, 1);
            g.drawString(font, com.pvpciv.client.ClientInit.SKILL_KEYS[i].getTranslatedKeyMessage().getString().toUpperCase(), 0, 0, Skin.GOLD_L, true);
            g.pose().popPose();
        }
        if (mc.player.isCreative()) return;
        // полоски
        int bw = 84, by = hy - 15;
        float hp = mc.player.getHealth(), max = mc.player.getMaxHealth();
        boolean low = hp / max < 0.3f;
        if (low) Skin.glow(g, hx + 6 + bw / 2, by + 6, 30, (0x30 + (int) (0x28 * Math.sin(System.currentTimeMillis() / 150.0))) << 24 | 0xFF2020);
        Skin.bar(g, hx + 8, by, bw, hp / max, low ? 0xFFFF5050 : 0xFFE84858, low ? 0xFF8A1010 : 0xFF8A1828, "red");
        float abs = mc.player.getAbsorptionAmount();
        if (abs > 0) g.fill(hx + 11, by + 8, hx + 11 + Math.round((bw - 6) * Math.min(1, abs / max)), by + 10, 0xFFFFD040);
        g.drawCenteredString(font, num(Math.min(hp, max)) + "/" + num(max), hx + 8 + bw / 2, by + 2, 0xFFFFFFFF);
        int food = mc.player.getFoodData().getFoodLevel();
        Skin.bar(g, hx + hw - 8 - bw, by, bw, food / 20f, food <= 6 ? 0xFFC87020 : 0xFFF0B040, food <= 6 ? 0xFF6A3008 : 0xFF9A5A10, "amber");
        g.drawCenteredString(font, food + "/20", hx + hw - 8 - bw / 2, by + 2, 0xFFFFFFFF);
        int armor = mc.player.getArmorValue();
        if (armor > 0) {
            Skin.icon(g, "shield", hx + 4, by - 11);
            g.drawString(font, String.valueOf(armor), hx + 16, by - 10, 0xFFA8C0E0, true);
        }
        int air = mc.player.getAirSupply(), maxAir = mc.player.getMaxAirSupply();
        if (air < maxAir) Skin.thinBar(g, hx + hw - 8 - bw, by - 6, bw, 2, Math.max(0, air) / (float) maxAir, 0xFFBFEFFF, 0xFF4090D0);
    }

    /** Своя полоса босса (вызывается из события; возвращает занятую высоту). */
    public static int bossBar(GuiGraphics g, LerpingBossEvent ev, int x, int y) {
        Font font = Minecraft.getInstance().font;
        float p = ev.getProgress();
        int bw = 150, bx = x + 91 - bw / 2;
        boolean rage = p < 0.5f;
        if (rage) Skin.glow(g, x + 91, y + 7, 60, (0x20 + (int) (0x18 * Math.sin(System.currentTimeMillis() / 180.0))) << 24 | 0xFF3010);
        g.drawCenteredString(font, ev.getName(), x + 91, y - 9, 0xFFFFFFFF);
        Skin.bar(g, bx, y, bw, p, rage ? 0xFFFF6030 : 0xFFE03040, rage ? 0xFF7A1408 : 0xFF6A0C18, null);
        if (rage) Skin.spr(g, "crack", bx + 12, y, 40, 12, 0xB0FFFFFF);
        if (p < 0.25f) Skin.spr(g, "crack", bx + 92, y, 40, 12, 0xB0FFFFFF);
        Skin.icon(g, "skull", bx - 5, y); Skin.icon(g, "skull", bx + bw - 7, y);
        return 28;
    }
}
