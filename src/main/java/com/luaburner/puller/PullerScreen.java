package com.luaburner.puller;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Checklist puller GUI — standard MC container layout.
 * Top: main panel with blueprint / upgrade / buffer slots and a status readout.
 * Bottom: separate player-inventory panel. Item icons are drawn by vanilla.
 */
public class PullerScreen extends AbstractContainerScreen<PullerMenu> {
    private static final int W = PullerMenu.GUI_W, H = PullerMenu.GUI_H;
    private static final int MAIN_H = PullerMenu.MAIN_H;

    private String status = "";
    private int stackLimit = 64;
    private long multiplier = 1;
    private int outputMode = 0;

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    public PullerScreen(PullerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = W;
        this.imageHeight = H;
    }

    public void setChecklist(java.util.List<ChecklistEntry> entries, boolean pulling,
                             String status, String statusArg, int stackLimit,
                             long multiplier, int outputMode) {
        this.status = status == null || status.isEmpty() ? "" : tr(status, statusArg == null ? "" : statusArg);
        this.stackLimit = stackLimit;
        this.multiplier = multiplier;
        this.outputMode = outputMode;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - W) / 2;
        topPos = (height - H) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        // Two clearly separated panels (classic MC beveled frame).
        panel(g, 0, 0, W, MAIN_H);
        panel(g, 0, MAIN_H + 4, W, H - MAIN_H - 4);

        // Amber accents framing the main panel
        g.fill(leftPos + 4, topPos + 4, leftPos + W - 4, topPos + 6, 0xFFFFB000);
        g.fill(leftPos + 4, topPos + MAIN_H - 6, leftPos + W - 4, topPos + MAIN_H - 4, 0xFFFFB000);

        // Subtle outlines for slot groups (visual only; real slots drawn by vanilla)
        outline(g, 4, 16, 24, 26);              // blueprint slot
        outline(g, 96, 16, 92, 44);             // upgrade slots (2x5)
        outline(g, 4, 58, 188, 94);             // buffer grid (5x10)
    }

    private void panel(GuiGraphics g, int x, int y, int w, int h) {
        int ax = leftPos + x, ay = topPos + y;
        g.fill(ax, ay, ax + w, ay + h, 0xFFC6C6C6);
        g.fill(ax, ay, ax + w, ay + 2, 0xFFFFFFFF);
        g.fill(ax, ay, ax + 2, ay + h, 0xFFFFFFFF);
        g.fill(ax, ay + h - 2, ax + w, ay + h, 0xFF555555);
        g.fill(ax + w - 2, ay, ax + w, ay + h, 0xFF555555);
        g.fill(ax + 4, ay + 4, ax + w - 4, ay + h - 4, 0xFF8B8B8B);
    }

    private void outline(GuiGraphics g, int x, int y, int w, int h) {
        int ax = leftPos + x, ay = topPos + y;
        g.fill(ax, ay, ax + w, ay + 1, 0xFF5A5A5A);
        g.fill(ax, ay + h - 1, ax + w, ay + h, 0xFF5A5A5A);
        g.fill(ax, ay, ax + 1, ay + h, 0xFF5A5A5A);
        g.fill(ax + w - 1, ay, ax + w, ay + h, 0xFF5A5A5A);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Handled in render() with correct absolute coords.
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick); // slots + items + widgets

        int x = leftPos, y = topPos;
        g.drawString(font, tr("gui.luaburner.checklist_puller"), x + 8, y + 6, 0xFF7A4B00, false);
        g.drawString(font, tr("gui.luaburner.bp"), x + 12, y + 40, 0xFF404040, false);
        g.drawString(font, tr("gui.luaburner.upgrades"), x + 100, y + 6, 0xFF7A4B00, false);
        g.drawString(font, tr("gui.luaburner.buffer"), x + 8, y + 50, 0xFF7A4B00, false);

        g.drawString(font, tr("gui.luaburner.stack_slot", stackLimit), x + 8, y + 158, 0xFF202020, false);
        g.drawString(font, tr("gui.luaburner.mult", multiplier), x + 100, y + 158, 0xFF7A4B00, false);
        g.drawString(font, tr("gui.luaburner.output",
                tr(outputMode == 0 ? "gui.luaburner.output_adjacent" : "gui.luaburner.output_internal")),
                x + 8, y + 172, 0xFF7A4B00, false);
        g.drawString(font, trim(status, 30), x + 8, y + 188, 0xFF404040, false);
        g.drawString(font, tr("gui.luaburner.use_cc"), x + 8, y + 206, 0xFF606060, false);

        // Player inventory label
        g.drawString(font, tr("gui.luaburner.inventory"), x + 8, topPos + MAIN_H + 10, 0xFF404040, false);

        renderTooltip(g, mouseX, mouseY);
    }

    private String trim(String s, int max) { return s.length() <= max ? s : s.substring(0, max); }
}
