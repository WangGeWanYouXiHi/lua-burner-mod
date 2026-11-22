package com.luaburner.puller;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.GridHelper;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Performs the actual item extraction. Loaded only when at least one network
 * mod is present. Kept separate from the BlockEntity so AE2/Create references
 * never leak into always-loaded classes.
 */
public final class PullExecutor {
    private PullExecutor() {}

    /**
     * Try to pull up to `amount` of `template` from all available sources.
     * Sources are tried in order: AE2 ME network (if node has a grid), then
     * adjacent inventories (Create logistics lands on IItemHandler).
     *
     * @return the stack that was actually pulled (may be empty)
     */
    public static ItemStack pull(Level level, BlockPos pos, ItemStack template, int amount, boolean ae2Enabled) {
        if (level == null || template.isEmpty() || amount <= 0) return ItemStack.EMPTY;

        ItemStack pulled = ItemStack.EMPTY;

        // --- AE2 ME network ---
        if (ae2Enabled) {
            pulled = pullFromAe2(level, pos, template, amount);
            if (!pulled.isEmpty()) return pulled;
        }

        // --- Adjacent inventories (Create logistics / plain chests) ---
        pulled = pullFromAdjacent(level, pos, template, amount);
        return pulled;
    }

    /** Count how many of `template` are available across all sources (no extraction). */
    public static int countAvailable(Level level, BlockPos pos, ItemStack template, boolean ae2Enabled) {
        if (level == null || template.isEmpty()) return 0;
        int total = 0;
        if (ae2Enabled) total += countAe2(level, pos, template);
        total += countAdjacent(level, pos, template);
        return total;
    }

    // ---------------- AE2 ----------------

    private static IGridNode findNode(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            IGridNode node = GridHelper.getExposedNode(level, pos, dir);
            if (node != null && node.isActive() && node.getGrid() != null) return node;
        }
        return null;
    }

    private static ItemStack pullFromAe2(Level level, BlockPos pos, ItemStack template, int amount) {
        try {
            IGridNode node = findNode(level, pos);
            if (node == null) return ItemStack.EMPTY;
            IGrid grid = node.getGrid();
            IStorageService storage = grid.getStorageService();
            IEnergyService energy = grid.getEnergyService();
            if (storage == null) return ItemStack.EMPTY;
            MEStorage inv = storage.getInventory();
            AEItemKey key = AEItemKey.of(template);
            if (key == null) return ItemStack.EMPTY;
            IActionSource src = IActionSource.empty();
            long got = StorageHelper.poweredExtraction(energy, inv, key, amount, src,
                    appeng.api.config.Actionable.MODULATE);
            if (got <= 0) return ItemStack.EMPTY;
            return key.toStack((int) got);
        } catch (Throwable t) {
            return ItemStack.EMPTY;
        }
    }

    private static int countAe2(Level level, BlockPos pos, ItemStack template) {
        try {
            IGridNode node = findNode(level, pos);
            if (node == null) return 0;
            IGrid grid = node.getGrid();
            IStorageService storage = grid.getStorageService();
            if (storage == null) return 0;
            AEItemKey key = AEItemKey.of(template);
            if (key == null) return 0;
            long count = storage.getInventory().extract(key, Long.MAX_VALUE,
                    appeng.api.config.Actionable.SIMULATE, IActionSource.empty());
            return (int) Math.min(Integer.MAX_VALUE, count);
        } catch (Throwable t) {
            return 0;
        }
    }

    // ---------------- Adjacent inventories ----------------

    private static ItemStack pullFromAdjacent(Level level, BlockPos pos, ItemStack template, int amount) {
        ItemStack extracted = ItemStack.EMPTY;
        for (Direction dir : Direction.values()) {
            BlockPos np = pos.relative(dir);
            IItemHandler handler = level.getCapability(
                    net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    np, dir.getOpposite());
            if (handler == null) continue;
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack slot = handler.getStackInSlot(i);
                if (slot.isEmpty() || !ItemStack.isSameItemSameComponents(slot, template)) continue;
                ItemStack taken = handler.extractItem(i, amount, false);
                if (!taken.isEmpty()) {
                    if (extracted.isEmpty()) extracted = taken;
                    else extracted.grow(taken.getCount());
                    return extracted;
                }
            }
        }
        return extracted;
    }

    private static int countAdjacent(Level level, BlockPos pos, ItemStack template) {
        int total = 0;
        for (Direction dir : Direction.values()) {
            BlockPos np = pos.relative(dir);
            IItemHandler handler = level.getCapability(
                    net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    np, dir.getOpposite());
            if (handler == null) continue;
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack slot = handler.getStackInSlot(i);
                if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, template)) {
                    total += slot.getCount();
                }
            }
        }
        return total;
    }
}
