package com.pvpciv.client.screen;

import com.google.gson.JsonObject;
import com.pvpciv.client.ui.Skin;
import com.pvpciv.net.Network;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Загадка Юки: 5 букв, 6 попыток. Печатай с клавиатуры (русская раскладка), Enter — проверить. В испытании — таймер и счёт 0/10. */
public class WordScreen extends PlainScreen {
    private JsonObject j;
    private String cur = "";
    private final List<String> words = new ArrayList<>(), cols = new ArrayList<>();
    private static final String KB = "йцукенгшщзхъфывапролджэячсмитьбю";

    public WordScreen(JsonObject j) { super("Загадка Юки", 240, 262, false); data(j); }

    public void data(JsonObject j) {
        this.j = j;
        words.clear(); cols.clear();
        for (var el : j.getAsJsonArray("g")) { words.add(el.getAsJsonObject().get("w").getAsString()); cols.add(el.getAsJsonObject().get("c").getAsString()); }
        cur = "";
    }

    private boolean over() { return j.get("solved").getAsBoolean() || j.get("failed").getAsBoolean(); }

    @Override
    protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
        boolean trial = j.has("trial");
        String head = trial ? "Испытание: " + j.get("done").getAsInt() + "/10 · " + left() : "Слово дня — пять букв, шесть попыток";
        g.drawCenteredString(font, head, left + w / 2, top + 20, trial ? Skin.WARN : Skin.DIM);
        int cell = 20, gx = left + (w - cell * 5 - 3 * 4) / 2, gy = top + 34;
        for (int r = 0; r < 6; r++) for (int c = 0; c < 5; c++) {
            int x = gx + c * (cell + 3), y = gy + r * (cell + 3);
            String ch = ""; int bg = 0xFF221E2C, border = 0xFF4A4458;
            if (r < words.size()) {
                ch = String.valueOf(words.get(r).charAt(c));
                char k = cols.get(r).charAt(c);
                bg = k == '2' ? 0xFF3A9A50 : k == '1' ? 0xFFB89A2A : 0xFF3A3644;
                border = bg;
            } else if (r == words.size() && !over() && c < cur.length()) { ch = String.valueOf(cur.charAt(c)); border = 0xFFD9A441; }
            g.fill(x - 1, y - 1, x + cell + 1, y + cell + 1, border);
            g.fill(x, y, x + cell, y + cell, bg);
            if (!ch.isEmpty()) {
                g.pose().pushPose();
                g.pose().translate(x + cell / 2f, y + 5, 0);
                g.pose().scale(1.4f, 1.4f, 1);
                g.drawCenteredString(font, ch.toUpperCase(), 0, 0, Skin.TEXT);
                g.pose().popPose();
            }
        }
        // подсказка-клавиатура: цвета использованных букв
        int ky = gy + 6 * (cell + 3) + 4;
        for (int i = 0; i < KB.length(); i++) {
            char k = KB.charAt(i);
            int best = -1;
            for (int r = 0; r < words.size(); r++) for (int c = 0; c < 5; c++) if (words.get(r).charAt(c) == k) best = Math.max(best, cols.get(r).charAt(c) - '0');
            int row = i < 12 ? 0 : i < 23 ? 1 : 2, col = i < 12 ? i : i < 23 ? i - 12 : i - 23;
            int x = left + 12 + col * 18 + row * 9, y = ky + row * 14;
            boolean hv = mx >= x && mx < x + 16 && my >= y && my < y + 12;
            int bg = best == 2 ? 0xFF3A9A50 : best == 1 ? 0xFFB89A2A : best == 0 ? 0xFF2A2630 : 0xFF3E3850;
            g.fill(x, y, x + 16, y + 12, hv ? 0xFF6A5A8A : bg);
            g.drawCenteredString(font, String.valueOf(k).toUpperCase(), x + 8, y + 2, best == 0 ? Skin.FAINT : Skin.TEXT);
        }
        Skin.button(g, font, left + 40, ky + 44, 76, "⌫ Стереть", mx, my, !cur.isEmpty(), false);
        Skin.button(g, font, left + 124, ky + 44, 76, "Проверить", mx, my, cur.length() == 5 && !over(), true);
        String foot = j.get("solved").getAsBoolean() ? (trial ? "Катана собрана!" : "Угадано! Награда — в сумке.")
                : j.get("failed").getAsBoolean() ? "Слово: «" + (j.has("answer") ? j.get("answer").getAsString() : "?") + "»"
                : "Печатай или жми буквы · Enter — проверить";
        g.drawCenteredString(font, Skin.fit(font, foot, w - 16), left + w / 2, top + h - 14, j.get("solved").getAsBoolean() ? Skin.GOOD : Skin.FAINT);
    }

    private String left() { long s = Math.max(0, j.get("left").getAsLong() - (System.currentTimeMillis() - opened) / 1000); return s / 60 + ":" + String.format("%02d", s % 60); }
    private final long opened = System.currentTimeMillis();

    private int[] kbd() { return new int[]{34 + 6 * 23 + 4}; }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        int ky = top + kbd()[0];
        for (int i = 0; i < KB.length(); i++) {
            int row = i < 12 ? 0 : i < 23 ? 1 : 2, col = i < 12 ? i : i < 23 ? i - 12 : i - 23;
            int x = left + 12 + col * 18 + row * 9, y = ky + row * 14;
            if (mx >= x && mx < x + 16 && my >= y && my < y + 12) { if (!over() && cur.length() < 5) cur += KB.charAt(i); return true; }
        }
        if (in(mx, my, 40, kbd()[0] + 44, 76, 16) && !cur.isEmpty()) { cur = cur.substring(0, cur.length() - 1); return true; }
        if (in(mx, my, 124, kbd()[0] + 44, 76, 16) && cur.length() == 5 && !over()) { PacketDistributor.sendToServer(new Network.PhoneCmd("word:" + cur)); return true; }
        return super.mouseClicked(mx, my, b);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        char l = Character.toLowerCase(c == 'ё' || c == 'Ё' ? 'е' : c);
        if (!over() && l >= 'а' && l <= 'я' && cur.length() < 5) { cur += l; return true; }
        return super.charTyped(c, mods);
    }

    @Override
    public boolean keyPressed(int key, int sc, int mods) {
        if (key == GLFW.GLFW_KEY_BACKSPACE && !cur.isEmpty()) { cur = cur.substring(0, cur.length() - 1); return true; }
        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && cur.length() == 5 && !over()) {
            PacketDistributor.sendToServer(new Network.PhoneCmd("word:" + cur));
            return true;
        }
        return super.keyPressed(key, sc, mods);
    }
}
