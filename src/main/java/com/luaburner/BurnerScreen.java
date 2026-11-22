package com.luaburner;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * The burner GUI. Layout (left to right): code preview | blueprint list |
 * info + mode | disk slots. Bottom: progress bar + burn button.
 */
public class BurnerScreen extends AbstractContainerScreen<BurnerMenu> {
    // Panel geometry (relative to leftPos/topPos)
    private static final int W = 460, H = 250;
    private static final int PREVIEW_X = 8, PREVIEW_Y = 24, PREVIEW_W = 190, PREVIEW_H = 150;
    private static final int LIST_X = 206, LIST_Y = 24, LIST_W = 120, LIST_H = 150;
    private static final int INFO_X = 334, INFO_Y = 24, INFO_W = 118, INFO_H = 150;

    // Blueprint data received from server
    private final List<String> bpNames = new ArrayList<>();
    private final List<String> bpContents = new ArrayList<>();
    private final List<Long> bpSizes = new ArrayList<>();
    private int selected = 0;

    // Scroll offsets
    private int previewScroll = 0; // in lines
    private int listScroll = 0;    // in rows

    private Button burnButton;
    private Button modeButton;
    private String status = tr("status.luaburner.idle");
    private int progress = 0;

    /** Resolve a lang key using the client's current language. */
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
        topPos = (height - H) / 2 - 10;

        // Mode toggle button (overwrite / incremental)
        modeButton = Button.builder(Component.translatable("gui.luaburner.mode_overwrite"), b -> {
            int next = (menu.modeSlot.get() == 0) ? 1 : 0;
            PacketDistributor.sendToServer(new NetworkMessages.BurnPayload(-1, next));
        }).bounds(leftPos + INFO_X, topPos + INFO_Y + 78, INFO_W - 4, 20).build();
        addRenderableWidget(modeButton);

        // Burn button (bottom-right, above progress bar)
        burnButton = Button.builder(Component.translatable("gui.luaburner.burn"), b -> {
            PacketDistributor.sendToServer(
                    new NetworkMessages.BurnPayload(selected, menu.modeSlot.get()));
        }).bounds(leftPos + W - 110, topPos + H - 44, 100, 20).build();
        addRenderableWidget(burnButton);
    }

    /** Receive the blueprint list from the server. */
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
        // Main panel background
        g.fill(leftPos, topPos, leftPos + W, topPos + H, 0xFF1E1E24);
        // Title bar
        g.fill(leftPos, topPos, leftPos + W, topPos + 20, 0xFF3A3A48);

        // Preview panel
        panel(g, PREVIEW_X, PREVIEW_Y, PREVIEW_W, PREVIEW_H, tr("gui.luaburner.code_preview"));
        // Blueprint list panel
        panel(g, LIST_X, LIST_Y, LIST_W, LIST_H, tr("gui.luaburner.blueprints"));
        // Info panel
        panel(g, INFO_X, INFO_Y, INFO_W, INFO_H, tr("gui.luaburner.info"));

        // Progress bar track (bottom)
        g.fill(leftPos + 8, topPos + H - 34, leftPos + W - 8, topPos + H - 22, 0xFF000000);
        if (progress > 0) {
            int barW = (int) ((W - 16) * progress / 100L);
            int color = progress >= 100 ? 0xFF3CB371 : 0xFF4A90D9;
            g.fill(leftPos + 8, topPos + H - 34, leftPos + 8 + barW, topPos + H - 22, color);
        }
    }

    private void panel(GuiGraphics g, int x, int y, int w, int h, String title) {
        int ax = leftPos + x, ay = topPos + y;
        g.fill(ax, ay, ax + w, ay + h, 0xFF121216);
        g.fill(ax, ay, ax + w, ay + 16, 0xFF2A2A34);
        g.drawString(font, title, ax + 4, ay + 3, 0xFFAAAAAA, false);
        g.fill(ax, ay, ax + 1, ay + h, 0xFF444455);
        g.fill(ax + w - 1, ay, ax + w, ay + h, 0xFF444455);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, 0xFFFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // sync data slots
        progress = menu.progressSlot.get();
        int mode = menu.modeSlot.get();
        selected = menu.selectedSlot.get();
        modeButton.setMessage(Component.translatable(
                mode == 0 ? "gui.luaburner.mode_overwrite" : "gui.luaburner.mode_incremental"));

        super.render(g, mouseX, mouseY, partialTick);

        // Draw preview lines
        if (!bpContents.isEmpty() && selected < bpContents.size()) {
            String content = bpContents.get(selected);
            String[] lines = content.split("\n", -1);
            int maxLines = (PREVIEW_H - 20) / 10;
            int start = Math.min(previewScroll, Math.max(0, lines.length - maxLines));
            int y = topPos + PREVIEW_Y + 18;
            for (int i = start; i < Math.min(lines.length, start + maxLines); i++) {
                String line = lines[i];
                if (line.length() > 34) line = line.substring(0, 34);
                g.drawString(font, String.format("%3d", i + 1) + " " + line,
                        leftPos + PREVIEW_X + 4, y, 0xFFD0D0D0, false);
                y += 10;
            }
            // Persistent scrollbar track + thumb
            drawScrollbar(g, PREVIEW_X + PREVIEW_W - 6, PREVIEW_Y + 18, PREVIEW_H - 20,
                    lines.length, maxLines, previewScroll);
        }

        // Draw blueprint list (windowed with ellipsis)
        int maxRows = (LIST_H - 20) / 12;
        int total = bpNames.size();
        int startRow = Math.min(listScroll, Math.max(0, total - maxRows));
        int y = topPos + LIST_Y + 18;
        for (int i = startRow; i < Math.min(total, startRow + maxRows); i++) {
            String name = bpNames.get(i);
            if (name.length() > 16) name = name.substring(0, 16);
            int color = (i == selected) ? 0xFF4A90D9 : 0xFFCCCCCC;
            g.drawString(font, (i == selected ? "> " : "  ") + name,
                    leftPos + LIST_X + 4, y, color, false);
            y += 12;
        }
        if (startRow > 0) {
            g.drawString(font, "  ...", leftPos + LIST_X + 4, topPos + LIST_Y + 18, 0xFF888888, false);
        }
        if (startRow + maxRows < total) {
            g.drawString(font, "  ...", leftPos + LIST_X + 4,
                    topPos + LIST_Y + 18 + (maxRows - 1) * 12, 0xFF888888, false);
        }

        // Draw info panel
        int iy = topPos + INFO_Y + 20;
        if (!bpNames.isEmpty() && selected < bpNames.size()) {
            String name = bpNames.get(selected);
            long size = bpSizes.get(selected);
            g.drawString(font, tr("gui.luaburner.name"), leftPos + INFO_X + 4, iy, 0xFF888888, false); iy += 10;
            g.drawString(font, trim(name, 17), leftPos + INFO_X + 4, iy, 0xFFFFFF00, false); iy += 20;
            g.drawString(font, tr("gui.luaburner.size"), leftPos + INFO_X + 4, iy, 0xFF888888, false); iy += 10;
            g.drawString(font, size + " B", leftPos + INFO_X + 4, iy, 0xFFD0D0D0, false); iy += 10;
            g.drawString(font, tr("gui.luaburner.disk_capacity"), leftPos + INFO_X + 4, iy, 0xFF888888, false);
        } else {
            g.drawString(font, tr("gui.luaburner.no_lua"), leftPos + INFO_X + 4, iy, 0xFF888888, false);
            iy += 12;
            g.drawString(font, tr("gui.luaburner.lua_folder"), leftPos + INFO_X + 4, iy, 0xFF888888, false);
        }

        // Draw status line
        g.drawString(font, trim(status, 30), leftPos + 8, topPos + H - 16, 0xFFAAAAAA, false);

        // Draw machine slot item icons at their positions (input 30,35 / output 30,75)
        ItemStack in = menu.getSlot(0).getItem();
        ItemStack out = menu.getSlot(1).getItem();
        if (!in.isEmpty()) g.renderItem(in, leftPos + 28, topPos + 33);
        if (!out.isEmpty()) g.renderItem(out, leftPos + 28, topPos + 73);

        g.drawString(font, tr("gui.luaburner.in"), leftPos + 8, topPos + 40, 0xFF888888, false);
        g.drawString(font, tr("gui.luaburner.out"), leftPos + 6, topPos + 80, 0xFF888888, false);
        renderTooltip(g, mouseX, mouseY);
    }

    private void drawScrollbar(GuiGraphics g, int x, int y, int h, int totalLines,
                               int maxLines, int scroll) {
        int ax = leftPos + x, ay = topPos + y;
        g.fill(ax, ay, ax + 4, ay + h, 0xFF000000); // track
        if (totalLines <= maxLines) return;
        int thumbH = Math.max(8, h * maxLines / totalLines);
        int maxScroll = totalLines - maxLines;
        int thumbY = ay + (int) ((h - thumbH) * (scroll / (double) maxScroll));
        g.fill(ax, thumbY, ax + 4, thumbY + thumbH, 0xFF888899); // thumb
    }

    private String trim(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        // Preview area
        if (inRect(mouseX, mouseY, PREVIEW_X, PREVIEW_Y, PREVIEW_W, PREVIEW_H)) {
            if (!bpContents.isEmpty() && selected < bpContents.size()) {
                int lines = bpContents.get(selected).split("\n", -1).length;
                int maxLines = (PREVIEW_H - 20) / 10;
                previewScroll = clamp(previewScroll - (int) Math.signum(deltaY),
                        0, Math.max(0, lines - maxLines));
            }
            return true;
        }
        // Blueprint list area
        if (inRect(mouseX, mouseY, LIST_X, LIST_Y, LIST_W, LIST_H)) {
            int maxRows = (LIST_H - 20) / 12;
            if (bpNames.size() > maxRows) {
                int newSel = clamp(selected - (int) Math.signum(deltaY), 0, bpNames.size() - 1);
                if (newSel != selected) {
                    selected = newSel;
                    previewScroll = 0;
                    menu.selectedSlot.set(selected);
                    requestCurrentPreview();
                }
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    private void requestCurrentPreview() {
        // Re-request the full list so the preview panel updates to the new selection.
        PacketDistributor.sendToServer(new NetworkMessages.BurnPayload(-2, selected));
    }

    private boolean inRect(double mx, double my, int x, int y, int w, int h) {
        double rx = mx - leftPos, ry = my - topPos;
        return rx >= x && rx <= x + w && ry >= y && ry <= y + h;
    }

    private int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(v, hi));
    }
}
