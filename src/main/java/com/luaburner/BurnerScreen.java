package com.luaburner;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Burner GUI — standard MC container layout.
 * Top: one main panel (disk slots | code preview | blueprint list | info + controls).
 * Bottom: a separate, clearly bounded player-inventory panel. Slot sprites and
 * item icons are drawn by the vanilla container renderer (we never draw items).
 */
public class BurnerScreen extends AbstractContainerScreen<BurnerMenu> {
    private static final int W = BurnerMenu.GUI_W, H = BurnerMenu.GUI_H;
    private static final int MAIN_H = 196;              // main panel height
    // Sub-panels inside the main panel (relative to leftPos/topPos)
    private static final int SLOT_X = 8,  SLOT_Y = 26,  SLOT_W = 44, SLOT_H = 150;
    private static final int PREVIEW_X = 58, PREVIEW_Y = 26, PREVIEW_W = 224, PREVIEW_H = 150;
    private static final int LIST_X = 288, LIST_Y = 26, LIST_W = 122, LIST_H = 150;
    private static final int INFO_X = 416, INFO_Y = 26, INFO_W = 96,  INFO_H = 150;

    private final List<String> bpNames = new ArrayList<>();
    private final List<String> bpContents = new ArrayList<>();
    private final List<Long> bpSizes = new ArrayList<>();
    private int selected = 0;

    private int previewScroll = 0;
    private int listScroll = 0;

    private Button burnButton;
    private Button modeButton;
    private String status = "";
    private int progress = 0;

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    public BurnerScreen(BurnerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = W;
        this.imageHeight = H;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - W) / 2;
        topPos = (height - H) / 2;

        modeButton = Button.builder(Component.translatable("gui.luaburner.mode_overwrite"), b -> {
            int next = (menu.modeSlot.get() == 0) ? 1 : 0;
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new NetworkMessages.BurnPayload(-1, next));
        }).bounds(leftPos + INFO_X + 4, topPos + INFO_Y + 106, INFO_W - 8, 20).build();
        addRenderableWidget(modeButton);

        burnButton = Button.builder(Component.translatable("gui.luaburner.burn"), b ->
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                        new NetworkMessages.BurnPayload(selected, menu.modeSlot.get()))
        ).bounds(leftPos + INFO_X + 4, topPos + INFO_Y + 128, INFO_W - 8, 20).build();
        addRenderableWidget(burnButton);
    }

    public void setBlueprintData(List<String> names, List<String> contents, List<Long> sizes) {
        bpNames.clear(); bpContents.clear(); bpSizes.clear();
        bpNames.addAll(names); bpContents.addAll(contents); bpSizes.addAll(sizes);
        if (selected >= bpNames.size()) selected = Math.max(0, bpNames.size() - 1);
        previewScroll = 0;
    }

    public void setStatus(String key, String arg, int prog) {
        this.status = tr(key, arg);
        this.progress = prog;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        // Main panel (classic MC container look: light frame, dark inset)
        panel(g, 0, 0, W, MAIN_H);
        // Player inventory panel — separate, clearly bounded
        panel(g, 0, MAIN_H + 4, W, H - MAIN_H - 4);

        // Sub-panel backgrounds inside the main panel
        subPanel(g, SLOT_X, SLOT_Y, SLOT_W, SLOT_H);
        subPanel(g, PREVIEW_X, PREVIEW_Y, PREVIEW_W, PREVIEW_H);
        subPanel(g, LIST_X, LIST_Y, LIST_W, LIST_H);
        subPanel(g, INFO_X, INFO_Y, INFO_W, INFO_H);

        // Progress bar track at the bottom of the main panel
        g.fill(leftPos + 8, topPos + MAIN_H - 12, leftPos + W - 8, topPos + MAIN_H - 4, 0xFF000000);
        if (progress > 0) {
            int barW = (int) ((W - 16) * progress / 100L);
            int color = progress >= 100 ? 0xFF4CAF50 : 0xFF4A90D9;
            g.fill(leftPos + 8, topPos + MAIN_H - 12, leftPos + 8 + barW, topPos + MAIN_H - 4, color);
        }
    }

    /** Classic container panel: beveled light-gray frame with darker inset. */
    private void panel(GuiGraphics g, int x, int y, int w, int h) {
        int ax = leftPos + x, ay = topPos + y;
        g.fill(ax, ay, ax + w, ay + h, 0xFFC6C6C6);          // frame body
        g.fill(ax, ay, ax + w, ay + 2, 0xFFFFFFFF);          // top highlight
        g.fill(ax, ay, ax + 2, ay + h, 0xFFFFFFFF);          // left highlight
        g.fill(ax, ay + h - 2, ax + w, ay + h, 0xFF555555);  // bottom shadow
        g.fill(ax + w - 2, ay, ax + w, ay + h, 0xFF555555);  // right shadow
        g.fill(ax + 4, ay + 4, ax + w - 4, ay + h - 4, 0xFF8B8B8B); // inset
    }

    /** Inset dark area for text panels. */
    private void subPanel(GuiGraphics g, int x, int y, int w, int h) {
        int ax = leftPos + x, ay = topPos + y;
        g.fill(ax, ay, ax + w, ay + h, 0xFF1E1E24);
        g.fill(ax, ay, ax + w, ay + 16, 0xFF3A3A48);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 8, 0xFFFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        progress = menu.progressSlot.get();
        int mode = menu.modeSlot.get();
        selected = menu.selectedSlot.get();
        modeButton.setMessage(Component.translatable(
                mode == 0 ? "gui.luaburner.mode_overwrite" : "gui.luaburner.mode_incremental"));

        super.render(g, mouseX, mouseY, partialTick); // draws slots, items, widgets, labels

        // --- Slot column labels (below the two machine slots) ---
        g.drawString(font, tr("gui.luaburner.in"), leftPos + SLOT_X + 4, topPos + SLOT_Y + 18, 0xFFEEEEEE, false);
        g.drawString(font, tr("gui.luaburner.out"), leftPos + SLOT_X + 4, topPos + SLOT_Y + 44, 0xFFEEEEEE, false);

        // --- Panel headings ---
        g.drawString(font, tr("gui.luaburner.code_preview"), leftPos + PREVIEW_X + 4, topPos + PREVIEW_Y + 4, 0xFFEEEEEE, false);
        g.drawString(font, tr("gui.luaburner.blueprints"), leftPos + LIST_X + 4, topPos + LIST_Y + 4, 0xFFEEEEEE, false);
        g.drawString(font, tr("gui.luaburner.info"), leftPos + INFO_X + 4, topPos + INFO_Y + 4, 0xFFEEEEEE, false);

        drawPreview(g);
        drawBlueprintList(g);
        drawInfo(g);

        // Status + progress text
        g.drawString(font, trim(status, 60), leftPos + 8, topPos + MAIN_H - 26, 0xFFEEEEEE, false);

        // Player inventory label in the lower panel
        g.drawString(font, tr("gui.luaburner.inventory"), leftPos + 179, topPos + MAIN_H + 10, 0xFF404040, false);

        renderTooltip(g, mouseX, mouseY);
    }

    private void drawPreview(GuiGraphics g) {
        if (bpContents.isEmpty() || selected >= bpContents.size()) return;
        String[] lines = bpContents.get(selected).split("\n", -1);
        int maxLines = (PREVIEW_H - 22) / 10;
        int start = Math.min(previewScroll, Math.max(0, lines.length - maxLines));
        int y = topPos + PREVIEW_Y + 20;
        for (int i = start; i < Math.min(lines.length, start + maxLines); i++) {
            String line = lines[i];
            if (line.length() > 36) line = line.substring(0, 36);
            g.drawString(font, String.format("%3d", i + 1) + " " + line,
                    leftPos + PREVIEW_X + 4, y, 0xFFD0D0D0, false);
            y += 10;
        }
        drawScrollbar(g, PREVIEW_X + PREVIEW_W - 6, PREVIEW_Y + 20, PREVIEW_H - 24,
                lines.length, maxLines, previewScroll);
    }

    private void drawBlueprintList(GuiGraphics g) {
        int maxRows = (LIST_H - 22) / 12;
        int total = bpNames.size();
        int startRow = Math.min(listScroll, Math.max(0, total - maxRows));
        int y = topPos + LIST_Y + 20;
        for (int i = startRow; i < Math.min(total, startRow + maxRows); i++) {
            String name = bpNames.get(i);
            if (name.length() > 15) name = name.substring(0, 15);
            int color = (i == selected) ? 0xFFFFD45C : 0xFFCCCCCC;
            g.drawString(font, (i == selected ? "> " : "  ") + name,
                    leftPos + LIST_X + 4, y, color, false);
            y += 12;
        }
        if (startRow > 0)
            g.drawString(font, "  ...", leftPos + LIST_X + 4, topPos + LIST_Y + 20, 0xFF888888, false);
        if (startRow + maxRows < total)
            g.drawString(font, "  ...", leftPos + LIST_X + 4,
                    topPos + LIST_Y + 20 + (maxRows - 1) * 12, 0xFF888888, false);
    }

    private void drawInfo(GuiGraphics g) {
        int iy = topPos + INFO_Y + 22;
        if (!bpNames.isEmpty() && selected < bpNames.size()) {
            String name = bpNames.get(selected);
            long size = bpSizes.get(selected);
            g.drawString(font, tr("gui.luaburner.name"), leftPos + INFO_X + 4, iy, 0xFF888888, false); iy += 10;
            g.drawString(font, trim(name, 14), leftPos + INFO_X + 4, iy, 0xFFFFFF00, false); iy += 18;
            g.drawString(font, tr("gui.luaburner.size"), leftPos + INFO_X + 4, iy, 0xFF888888, false); iy += 10;
            g.drawString(font, size + " B", leftPos + INFO_X + 4, iy, 0xFFD0D0D0, false);
        } else {
            g.drawString(font, tr("gui.luaburner.no_lua"), leftPos + INFO_X + 4, iy, 0xFF888888, false); iy += 12;
            g.drawString(font, tr("gui.luaburner.lua_folder"), leftPos + INFO_X + 4, iy, 0xFF888888, false);
        }
    }

    private void drawScrollbar(GuiGraphics g, int x, int y, int h, int totalLines,
                               int maxLines, int scroll) {
        if (totalLines <= maxLines) return;
        int ax = leftPos + x, ay = topPos + y;
        g.fill(ax, ay, ax + 4, ay + h, 0xFF000000);
        int thumbH = Math.max(8, h * maxLines / totalLines);
        int maxScroll = Math.max(1, totalLines - maxLines);
        int thumbY = ay + (int) ((h - thumbH) * (scroll / (double) maxScroll));
        g.fill(ax, thumbY, ax + 4, thumbY + thumbH, 0xFF888899);
    }

    private String trim(String s, int max) { return s.length() <= max ? s : s.substring(0, max); }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        double rx = mouseX - leftPos, ry = mouseY - topPos;
        if (rx >= PREVIEW_X && rx <= PREVIEW_X + PREVIEW_W && ry >= PREVIEW_Y && ry <= PREVIEW_Y + PREVIEW_H) {
            if (!bpContents.isEmpty() && selected < bpContents.size()) {
                int lines = bpContents.get(selected).split("\n", -1).length;
                int maxLines = (PREVIEW_H - 22) / 10;
                previewScroll = clamp(previewScroll - (int) Math.signum(deltaY), 0, Math.max(0, lines - maxLines));
            }
            return true;
        }
        if (rx >= LIST_X && rx <= LIST_X + LIST_W && ry >= LIST_Y && ry <= LIST_Y + LIST_H) {
            int maxRows = (LIST_H - 22) / 12;
            if (bpNames.size() > maxRows) {
                int newSel = clamp(selected - (int) Math.signum(deltaY), 0, bpNames.size() - 1);
                if (newSel != selected) {
                    selected = newSel; previewScroll = 0; menu.selectedSlot.set(selected);
                    net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                            new NetworkMessages.BurnPayload(-2, selected));
                }
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    private int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(v, hi)); }
}
