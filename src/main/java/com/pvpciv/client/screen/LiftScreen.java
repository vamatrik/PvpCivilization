package com.pvpciv.client.screen;

import com.pvpciv.client.ui.Skin;
import com.pvpciv.net.Network;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.network.PacketDistributor;

/** Табло Небесного лифта: этажи мира 1, закрытые — с замком. */
public class LiftScreen extends PlainScreen {
    private final int mask, current;
    private static final String[] NAMES = {"Нижний Шпиль", "Неоновый город", "Тихая библиотека", "Этаж 4", "Этаж 5", "Этаж 6", "Этаж 7", "Этаж 8", "Этаж 9", "Этаж 10"};

    public LiftScreen(int mask, int current) { super("Небесный лифт", 220, 200, false); this.mask = mask; this.current = current; }

    @Override
    protected void drawContent(GuiGraphics g, int mx, int my, float pt) {
        g.drawCenteredString(font, "Мир 1 · Шпиль над Бездной", left + w / 2, top + 18, Skin.DIM);
        for (int i = 0; i < 10; i++) {
            int col = i % 2, row = i / 2;
            int x = left + 12 + col * 100, y = top + 32 + row * 30;
            boolean open = (mask & (1 << i)) != 0, here = i + 1 == current;
            boolean hover = open && !here && in(mx, my, 12 + col * 100, 32 + row * 30, 96, 26);
            Skin.inset(g, x, y, 96, 26);
            if (here) g.fill(x + 1, y + 1, x + 95, y + 25, 0x308FE3FF);
            else if (hover) g.fill(x + 1, y + 1, x + 95, y + 25, 0x30FFD166);
            if (!open) Skin.icon(g, "lock", x + 4, y + 5);
            g.drawString(font, (i + 1) + "", x + (open ? 6 : 20), y + 4, open ? Skin.GOLD_L : Skin.FAINT, false);
            g.drawString(font, Skin.fit(font, open ? NAMES[i] : "закрыт", 70), x + (open ? 20 : 34), y + 4, open ? Skin.TEXT : Skin.FAINT, false);
            g.drawString(font, here ? "вы здесь" : open ? "поехать" : "не пройдено", x + 6, y + 15, here ? Skin.GOOD : open ? Skin.DIM : 0xFF8A5A5A, false);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        for (int i = 0; i < 10; i++) {
            int col = i % 2, row = i / 2;
            if ((mask & (1 << i)) != 0 && i + 1 != current && in(mx, my, 12 + col * 100, 32 + row * 30, 96, 26)) {
                PacketDistributor.sendToServer(new Network.LiftGo(i + 1));
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mx, my, b);
    }
}
