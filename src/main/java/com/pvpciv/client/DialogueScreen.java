package com.pvpciv.client;

import com.pvpciv.net.Network;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Окно диалога в стиле визуальной новеллы: портрет, плашка с именем, печатающийся текст, варианты ответов. */
public class DialogueScreen extends Screen {
    private int entityId, color;
    private String speaker, text;
    private List<String> options;
    private float shown;
    private int lastSoundAt;
    private boolean closedByServer, waiting;
    private final List<int[]> optRects = new ArrayList<>();

    public DialogueScreen(Network.DialogueOpen p) {
        super(Component.literal("Диалог"));
        update(p);
    }

    public void update(Network.DialogueOpen p) {
        entityId = p.entityId();
        speaker = p.speaker();
        color = p.color();
        text = p.text();
        options = p.options();
        shown = 0;
        lastSoundAt = 0;
        waiting = false;
    }

    public void closeFromServer() {
        closedByServer = true;
        onClose();
    }

    private boolean typing() { return shown < text.length(); }

    @Override
    public void tick() {
        if (typing()) {
            shown = Math.min(text.length(), shown + 1.8f);
            if ((int) shown - lastSoundAt >= 3 && minecraft != null) {
                lastSoundAt = (int) shown;
                float pitch = color == 0xFF8AD8 ? 1.8f : 1.1f;
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT.value(), pitch, 0.12f));
            }
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        g.fillGradient(0, height - 200, width, height, 0x00000000, 0xD0000000);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g, mx, my, pt);
        int x0 = 20, x1 = width - 20, y1 = height - 12, y0 = y1 - 86;

        // портрет говорящего
        Entity e = minecraft.level != null && entityId >= 0 ? minecraft.level.getEntity(entityId) : null;
        if (e instanceof LivingEntity le) {
            boolean nameVis = le.isCustomNameVisible();
            net.minecraft.network.chat.Component name = le.getCustomName();
            le.setCustomNameVisible(false);
            le.setCustomName(null);
            int px0 = x0 + 8, px1 = x0 + 108;
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, px0, y0 - 140, px1, y0 + 140, 80, 0.0625F, mx, my, le);
            le.setCustomName(name);
            le.setCustomNameVisible(nameVis);
        }
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);

        // панель
        g.fill(x0, y0, x1, y1, 0xEC0D0F1C);
        int border = 0xFF000000 | color;
        g.fill(x0, y0, x1, y0 + 1, border);
        g.fill(x0, y1 - 1, x1, y1, 0x60FFFFFF & border);
        g.fill(x0, y0, x0 + 1, y1, border);
        g.fill(x1 - 1, y0, x1, y1, border);

        // плашка с именем
        int nw = font.width(speaker) + 20;
        g.fill(x0 + 12, y0 - 14, x0 + 12 + nw, y0 + 2, 0xFF000000 | darken(color));
        g.fill(x0 + 12, y0 - 14, x0 + 12 + nw, y0 - 13, 0xFFFFFFFF);
        g.drawString(font, Component.literal(speaker).withStyle(s -> s.withBold(true)), x0 + 22, y0 - 10, 0xFFFFFF, true);

        // текст
        String vis = text.substring(0, (int) shown);
        int ty = y0 + 12;
        for (FormattedCharSequence l : font.split(Component.literal(vis), x1 - x0 - 32)) {
            g.drawString(font, l, x0 + 16, ty, 0xF2F2F7, false);
            ty += 11;
        }

        // варианты ответа
        optRects.clear();
        if (!typing() && !waiting) {
            int ow = 170;
            for (String o : options) ow = Math.max(ow, font.width(o) + 34);
            int oy = y0 - 8 - options.size() * 20;
            for (int i = 0; i < options.size(); i++) {
                int ox = x1 - ow, by = oy + i * 20;
                boolean hover = mx >= ox && mx <= x1 && my >= by && my <= by + 17;
                g.fill(ox, by, x1, by + 17, hover ? (0xF0000000 | color) : 0xE0151827);
                g.fill(ox, by, ox + 2, by + 17, 0xFF000000 | color);
                g.drawString(font, (i + 1) + ".", ox + 8, by + 5, hover ? 0x101010 : 0x8FA3B8, false);
                g.drawString(font, options.get(i), ox + 22, by + 5, hover ? 0x101010 : 0xFFFFFF, false);
                optRects.add(new int[]{ox, by, x1, by + 17});
            }
            if ((System.currentTimeMillis() / 400) % 2 == 0) g.drawString(font, "▼", x1 - 16, y1 - 14, border, false);
        }
        g.drawString(font, "[ЛКМ/Пробел] — дальше   [1-9] — ответ   [Esc] — уйти", x0 + 16, y1 - 12, 0x5A6478, false);
        g.pose().popPose();
    }

    private static int darken(int c) {
        int r = (c >> 16 & 255) * 2 / 3, gg = (c >> 8 & 255) * 2 / 3, b = (c & 255) * 2 / 3;
        return r << 16 | gg << 8 | b;
    }

    private void choose(int i) {
        if (waiting || i < 0 || i >= options.size()) return;
        waiting = true;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.4f));
        PacketDistributor.sendToServer(new Network.DialogueChoice(i));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (typing()) { shown = text.length(); return true; }
        for (int i = 0; i < optRects.size(); i++) {
            int[] r = optRects.get(i);
            if (mx >= r[0] && mx <= r[2] && my >= r[1] && my <= r[3]) { choose(i); return true; }
        }
        if (options.size() == 1) choose(0);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER) {
            if (typing()) shown = text.length();
            else if (options.size() == 1) choose(0);
            return true;
        }
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9 && !typing()) { choose(key - GLFW.GLFW_KEY_1); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void onClose() {
        if (!closedByServer) PacketDistributor.sendToServer(new Network.DialogueChoice(-1));
        super.onClose();
    }
}
