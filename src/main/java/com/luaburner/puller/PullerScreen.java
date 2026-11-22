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

    // CC:T advanced computer palette
    private static final int CC_BG = 0xFF0F0F0B;
    private static final int CC_AMBER = 0xFFFFB000;
    private static final int CC_YELLOW = 0xFFFFFF00;
    private static final int CC_GREEN = 0xFF33CC33;
    private static final int CC_BORDER = 0xFF8A8A00;

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        // CC terminal look: black panels with thin amber borders.
        panel(g, 0, 0, W, MAIN_H);
        panel(g, 0, MAIN_H + 4, W, H - MAIN_H - 4);

        outline(g, 4, 16, 24, 26);              // blueprint slot
        outline(g, 96, 16, 92, 44);             // upgrade slots (2x5)
        outline(g, 4, 58, 188, 94);             // buffer grid (5x10)
    }

    private void panel(GuiGraphics g, int x, int y, int w, int h) {
        int ax = leftPos + x, ay = topPos + y;
        g.fill(ax, ay, ax + w, ay + h, CC_BG);
        g.fill(ax, ay, ax + w, ay + 1, CC_BORDER);
        g.fill(ax, ay, ax + 1, ay + h, CC_BORDER);
        g.fill(ax, ay + h - 1, ax + w, ay + h, CC_BORDER);
        g.fill(ax + w - 1, ay, ax + w, ay + h, CC_BORDER);
    }

    private void outline(GuiGraphics g, int x, int y, int w, int h) {
        int ax = leftPos + x, ay = topPos + y;
        g.fill(ax, ay, ax + w, ay + 1, 0xFF6A5A00);
        g.fill(ax, ay + h - 1, ax + w, ay + h, 0xFF6A5A00);
        g.fill(ax, ay, ax + 1, ay + h, 0xFF6A5A00);
        g.fill(ax + w - 1, ay, ax + w, ay + h, 0xFF6A5A00);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Handled in render() with correct absolute coords.
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick); // slots + items + widgets

        int x = leftPos, y = topPos;
        g.drawString(font, tr("gui.luaburner.checklist_puller"), x + 8, y + 6, CC_AMBER, false);
        g.drawString(font, tr("gui.luaburner.bp"), x + 12, y + 40, CC_AMBER, false);
        g.drawString(font, tr("gui.luaburner.upgrades"), x + 100, y + 6, CC_AMBER, false);
        g.drawString(font, tr("gui.luaburner.buffer"), x + 8, y + 50, CC_AMBER, false);

        g.drawString(font, tr("gui.luaburner.stack_slot", stackLimit), x + 8, y + 158, 0xFF66DD66, false);
        g.drawString(font, tr("gui.luaburner.mult", multiplier), x + 100, y + 158, CC_YELLOW, false);
        g.drawString(font, tr("gui.luaburner.output",
                tr(outputMode == 0 ? "gui.luaburner.output_adjacent" : "gui.luaburner.output_internal")),
                x + 8, y + 172, CC_YELLOW, false);
        g.drawString(font, "> " + trim(status, 28), x + 8, y + 188, CC_GREEN, false);
        g.drawString(font, tr("gui.luaburner.use_cc"), x + 8, y + 206, CC_AMBER, false);

        // Player inventory label
        g.drawString(font, tr("gui.luaburner.inventory"), x + 8, topPos + MAIN_H + 8, CC_AMBER, false);

        renderTooltip(g, mouseX, mouseY);
    }

    private String trim(String s, int max) { return s.length() <= max ? s : s.substring(0, max); }
}
