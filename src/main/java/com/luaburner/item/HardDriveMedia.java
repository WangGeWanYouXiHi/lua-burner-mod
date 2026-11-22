package com.luaburner.item;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.media.IMedia;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;

/**
 * Shared IMedia singleton for all Lua hard drives. Every method works off the
 * passed ItemStack, so a single stateless instance can serve all 170 variants.
 * The disk id (DataComponent) is the folder name under the world save; the
 * capacity is derived from the item's capacityGb.
 */
public final class HardDriveMedia implements IMedia {
    public static final HardDriveMedia INSTANCE = new HardDriveMedia();
    private static final String SAVE_SUBPATH = "luaburner/harddrive";

    private HardDriveMedia() {}

    @Override
    public String getLabel(HolderLookup.Provider registries, ItemStack stack) {
        if (!(stack.getItem() instanceof HardDriveItem)) return null;
        String label = stack.get(ModDataComponents.DISK_LABEL.get());
        return label == null || label.isEmpty() ? null : label;
    }

    @Override
    public boolean setLabel(ItemStack stack, String label) {
        if (!(stack.getItem() instanceof HardDriveItem)) return false;
        if (label == null || label.isEmpty()) {
            stack.remove(ModDataComponents.DISK_LABEL.get());
        } else {
            stack.set(ModDataComponents.DISK_LABEL.get(), label);
        }
        return true;
    }

    @Override
    public Holder<JukeboxSong> getAudio(HolderLookup.Provider registries, ItemStack stack) {
        return null; // hard drives have no audio
    }

    @Override
    public Mount createDataMount(ItemStack stack, ServerLevel level) {
        if (!(stack.getItem() instanceof HardDriveItem drive)) return null;

        // Allocate a stable numeric disk id on first use and store it in the stack,
        // so the same physical drive always maps to the same folder.
        Integer id = stack.get(ModDataComponents.DISK_ID.get());
        if (id == null) {
            id = ComputerCraftAPI.createUniqueNumberedSaveDir(level.getServer(), SAVE_SUBPATH);
            if (id < 0) return null;
            stack.set(ModDataComponents.DISK_ID.get(), id);
        }

        long capacityBytes = (long) drive.getCapacityGb() * 1024L * 1024L * 1024L;
        return ComputerCraftAPI.createSaveDirMount(
                level.getServer(), SAVE_SUBPATH + "/" + id, capacityBytes);
    }
}
