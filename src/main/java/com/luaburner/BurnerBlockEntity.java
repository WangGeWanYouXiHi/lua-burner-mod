package com.luaburner;

import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.media.MediaCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Block entity holding the burner's inventory and burn-job state. */
public class BurnerBlockEntity extends BlockEntity {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_COUNT = 2;

    /** Resource id of CC:Tweaked's vanilla floppy disk. */
    public static final ResourceLocation CC_DISK_ID =
            ResourceLocation.fromNamespaceAndPath("computercraft", "disk");

    /** Mode 0 = overwrite (truncate file, write fresh), 1 = incremental (append). */
    public int mode = 0;
    public int selectedIndex = 0;
    public int progress = 0; // 0..100, synced to GUI via data slot
    public String status = "status.luaburner.idle";
    public String statusArg = "";

    private final SimpleContainer inventory = new SimpleContainer(SLOT_COUNT) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot == SLOT_INPUT && isVanillaDisk(stack);
        }
    };

    // Pending burn job (server side only)
    private String[] pendingLines;
    private String pendingName;
    private int lineCursor = 0;
    private boolean burning = false;
    private int pendingTotalLines = 0;

    public BurnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.BURNER_BE.get(), pos, state);
    }

    /** True only for CC:Tweaked's own floppy disk item (computercraft:disk). */
    public static boolean isVanillaDisk(ItemStack stack) {
        return !stack.isEmpty()
                && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(CC_DISK_ID);
    }

    /** Obtain the CC media handler for a disk (null if the item is not media). */
    private static IMedia mediaOf(ItemStack stack) {
        return MediaCapability.get().getCapability(stack, null);
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    public MenuProvider getMenuProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Lua Blueprint Burner");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
                return new BurnerMenu(containerId, playerInv, BurnerBlockEntity.this);
            }
        };
    }

    /** Start a burn job. Called on the server when the client clicks Burn. */
    public void startBurn(MinecraftServer server, int index, int burnMode) {
        if (burning) return;
        ItemStack disk = inventory.getItem(SLOT_INPUT);
        if (!isVanillaDisk(disk)) {
            status = "status.luaburner.no_disk";
            statusArg = "";
            progress = 0;
            broadcastStatus();
            return;
        }
        List<BlueprintManager.Blueprint> bps = BlueprintManager.list(server);
        if (index < 0 || index >= bps.size()) {
            status = "status.luaburner.no_bp";
            statusArg = "";
            progress = 0;
            broadcastStatus();
            return;
        }
        BlueprintManager.Blueprint bp = bps.get(index);

        // Let CC allocate the disk id / save dir on first write. Set the disk label now.
        IMedia media = mediaOf(disk);
        if (media != null) {
            media.setLabel(disk, bp.name());
        }

        pendingLines = bp.content().split("\n", -1);
        pendingName = bp.name();
        lineCursor = 0;
        pendingTotalLines = pendingLines.length;
        mode = burnMode;
        burning = true;
        progress = 0;
        status = "status.luaburner.burning_file";
        statusArg = bp.name();
        setChanged();
    }

    /** Called each server tick to write the next chunk of the burn job. */
    public void serverTick() {
        if (!burning || level == null || level.isClientSide) return;
        MinecraftServer server = level.getServer();

        int chunk = 40; // lines per tick - keeps the game responsive
        int end = Math.min(lineCursor + chunk, pendingLines.length);
        StringBuilder sb = new StringBuilder();
        for (int i = lineCursor; i < end; i++) {
            if (i > 0) sb.append('\n');
            sb.append(pendingLines[i]);
        }
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);

        // Append on later chunks, truncate on the first chunk.
        boolean ok = appendToDisk(diskMount(), pendingName, bytes, lineCursor == 0);
        if (!ok) {
            status = "status.luaburner.write_failed";
            statusArg = "";
            burning = false;
            progress = 0;
            setChanged();
            return;
        }

        lineCursor = end;
        progress = (int) (100L * lineCursor / Math.max(1, pendingTotalLines));
        status = "status.luaburner.burning_pct";
        statusArg = String.valueOf(progress);

        if (lineCursor >= pendingLines.length) {
            finishBurn();
        }
        broadcastStatus();
        setChanged();
    }

    /** Get the writable mount for the disk currently in the input slot. */
    private WritableMount diskMount() {
        if (level == null || !(level instanceof ServerLevel sl)) return null;
        ItemStack disk = inventory.getItem(SLOT_INPUT);
        IMedia media = mediaOf(disk);
        if (media == null) return null;
        Mount mount = media.createDataMount(disk, sl);
        return mount instanceof WritableMount wm ? wm : null;
    }

    /** Push progress + status text to any players currently viewing this burner. */
    private void broadcastStatus() {
        if (level == null || level.isClientSide) return;
        for (Player player : level.players()) {
            if (player.containerMenu instanceof BurnerMenu menu
                    && menu.getBlockEntity() == this) {
                menu.syncState();
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                        (net.minecraft.server.level.ServerPlayer) player,
                        new NetworkMessages.StatusPayload(status, statusArg, progress));
            }
        }
    }

    private boolean appendToDisk(WritableMount mount, String path,
                                 byte[] data, boolean truncate) {
        if (mount == null) return false;
        try {
            if (truncate && mount.exists(path)) {
                mount.delete(path);
            }
            var opts = new java.util.HashSet<java.nio.file.OpenOption>();
            opts.add(java.nio.file.StandardOpenOption.WRITE);
            opts.add(java.nio.file.StandardOpenOption.CREATE);
            if (truncate) opts.add(java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
            else opts.add(java.nio.file.StandardOpenOption.APPEND);
            try (var ch = mount.openFile(path, opts)) {
                ch.write(java.nio.ByteBuffer.wrap(data));
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void finishBurn() {
        burning = false;
        progress = 100;
        status = "status.luaburner.done";
        statusArg = pendingName;
        // Move the burned disk from input slot to output slot.
        ItemStack disk = inventory.getItem(SLOT_INPUT);
        inventory.setItem(SLOT_INPUT, ItemStack.EMPTY);
        ItemStack out = inventory.getItem(SLOT_OUTPUT);
        if (out.isEmpty()) {
            inventory.setItem(SLOT_OUTPUT, disk);
        } else {
            // Output occupied: drop the disk at the block position.
            if (level != null) {
                net.minecraft.world.level.block.Block.popResource(level, getBlockPos(), disk);
            }
        }
        pendingLines = null;
    }

    public boolean isBurning() {
        return burning;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("mode", mode);
        tag.putInt("selected", selectedIndex);
        tag.put("inv", inventory.createTag(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        mode = tag.getInt("mode");
        selectedIndex = tag.getInt("selected");
        if (tag.contains("inv")) {
            inventory.fromTag(tag.getList("inv", net.minecraft.nbt.Tag.TAG_COMPOUND), registries);
        }
    }

    /** Server ticker registration helper. */
    public static void tick(net.minecraft.world.level.Level level, BlockPos pos,
                            BlockState state, BurnerBlockEntity be) {
        be.serverTick();
    }
}
