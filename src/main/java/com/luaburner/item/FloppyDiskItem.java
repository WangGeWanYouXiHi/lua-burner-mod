package com.luaburner.item;

import com.luaburner.ModRegistries;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.media.IMedia;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.io.IOException;
import java.util.List;

/**
 * A floppy disk with a fixed 1.44 MB capacity (1440 KiB), exposed to CC:Tweaked
 * as writable media. The disk id is stored in a DataComponent; data lives in the
 * world save under {@code computercraft/disk/<id>/}.
 */
public class FloppyDiskItem extends Item implements IMedia {
    /** 1.44 MB floppy = 1440 * 1024 bytes. */
    public static final long CAPACITY = 1440L * 1024L;

    public FloppyDiskItem() {
        super(new Item.Properties().stacksTo(1));
    }

    /** Returns the disk id, or null if the disk has never been burned. */
    public static Integer getDiskId(ItemStack stack) {
        return stack.get(ModRegistries.DISK_ID);
    }

    public static void setDiskId(ItemStack stack, int id) {
        stack.set(ModRegistries.DISK_ID, id);
    }

    public static String getLabelRaw(ItemStack stack) {
        return stack.get(ModRegistries.DISK_LABEL);
    }

    @Override
    public String getLabel(HolderLookup.Provider registries, ItemStack stack) {
        return getLabelRaw(stack);
    }

    @Override
    public boolean setLabel(ItemStack stack, String label) {
        if (label == null) {
            stack.remove(ModRegistries.DISK_LABEL);
        } else {
            stack.set(ModRegistries.DISK_LABEL, label);
        }
        return true;
    }

    @Override
    public Mount createDataMount(ItemStack stack, ServerLevel level) {
        Integer id = getDiskId(stack);
        if (id == null) return null; // blank disk: nothing mounted yet

        MinecraftServer server = level.getServer();
        WritableMount mount = ComputerCraftAPI.createSaveDirMount(
                server, "computercraft/disk/" + id, CAPACITY);
        return mount;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        Integer id = getDiskId(stack);
        if (id == null) {
            tooltip.add(Component.literal("Blank floppy - 1.44 MB"));
        } else {
            tooltip.add(Component.literal("Disk #" + id + " - 1.44 MB"));
        }
    }

    /** Helper used by the burner: ensure a disk has an id, allocating a unique save dir. */
    public static int ensureDiskId(ItemStack stack, MinecraftServer server) {
        Integer existing = getDiskId(stack);
        if (existing != null) return existing;

        int id = ComputerCraftAPI.createUniqueNumberedSaveDir(server, "computercraft/disk");
        if (id < 0) {
            // Fallback: a large deterministic id based on stack identity hash.
            id = Math.abs(stack.hashCode()) + 1_000_000;
        }
        setDiskId(stack, id);
        return id;
    }

    /** Write the given bytes as a file on the disk. Returns bytes written, or -1 on failure. */
    public static int writeFile(ItemStack stack, MinecraftServer server, String path, byte[] data) {
        int id = ensureDiskId(stack, server);
        WritableMount mount;
        try {
            mount = ComputerCraftAPI.createSaveDirMount(
                    server, "computercraft/disk/" + id, CAPACITY);
        } catch (Exception e) {
            return -1;
        }
        if (mount == null) return -1;

        long remaining;
        try {
            remaining = mount.getRemainingSpace();
        } catch (IOException e) {
            return -1;
        }
        if (remaining < data.length) return -1; // not enough space

        try (var channel = mount.openFile(path, java.util.Set.of(
                java.nio.file.StandardOpenOption.WRITE,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING))) {
            channel.write(java.nio.ByteBuffer.wrap(data));
            return data.length;
        } catch (IOException e) {
            return -1;
        }
    }
}
