package com.luaburner.puller;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Slot-management GUI for the checklist puller. The material checklist itself
 * is rendered on a CC:Tweaked Advanced Computer (see the bundled Lua program),
 * so this screen only exposes blueprint / upgrade / buffer slots and a status
 * line.
 */
public class PullerScreen extends AbstractContainerScreen<PullerMenu> {
    private static final int W = 196, H = 256;
    private String status = "";
    private int stackLimit = 64;
    private long multiplier = 1;
    private int outputMode = 0;

    /** Resolve a lang key using the client's current language. */
    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    public PullerScreen(PullerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = W;
        this.imageHeight = H;
    }

    /** Receives live state from the server (the checklist itself lives on the CC screen). */
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
        int x = leftPos, y = topPos;
        // terminal-ish dark panel
        g.fill(x, y, x + W, y + H, 0xFF121212);
        g.fill(x + 2, y + 2, x + W - 2, y + H - 2, 0xFF1C1C1C);
        // blueprint slot outline
        g.fill(x + 6, y + 18, x + 26, y + 38, 0xFF2E2E2E);
        // upgrade slots outline (2 rows of 5)
        g.fill(x + 96, y + 18, x + 192, y + 56, 0xFF2A2410);
        // buffer grid outline
        g.fill(x + 6, y + 60, x + 188, y + 152, 0xFF222222);
        // amber accent lines (Advanced Computer look)
        g.fill(x + 2, y + 2, x + W - 2, y + 4, 0xFFFFB000);
        g.fill(x + 2, y + H - 4, x + W - 2, y + H - 2, 0xFFFFB000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        int x = leftPos, y = topPos;

        g.drawString(font, tr("gui.luaburner.checklist_puller"), x + 8, y + 6, 0xFFFFB000, false);
        g.drawString(font, tr("gui.luaburner.bp"), x + 12, y + 40, 0xFF888888, false);
        g.drawString(font, tr("gui.luaburner.upgrades"), x + 98, y + 8, 0xFFFFB000, false);
        g.drawString(font, tr("gui.luaburner.buffer"), x + 8, y + 50, 0xFFFFB000, false);

        g.drawString(font, tr("gui.luaburner.stack_slot", stackLimit), x + 8, y + 156, 0xFFFFFFFF, false);
        g.drawString(font, tr("gui.luaburner.mult", multiplier), x + 8, y + 168, 0xFFFFD45C, false);
        g.drawString(font, tr("gui.luaburner.output",
                        tr(outputMode == 0 ? "gui.luaburner.output_adjacent" : "gui.luaburner.output_internal")),
                x + 8, y + 180, 0xFFFFD45C, false);
        g.drawString(font, status.length() > 30 ? status.substring(0, 30) : status,
                x + 8, y + 194, 0xFFAAAAAA, false);
        g.drawString(font, tr("gui.luaburner.use_cc"), x + 8, y + H - 16,
                0xFF8A8A8A, false);
    }
}
