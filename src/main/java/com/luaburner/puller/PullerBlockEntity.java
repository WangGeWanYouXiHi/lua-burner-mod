package com.luaburner.puller;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IManagedGridNode;
import com.luaburner.IntegrationCaps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Checklist puller block entity.
 *
 * - Reads a Create schematic (blueprint) and computes its material checklist.
 * - Pulls missing items from AE2 / adjacent inventories.
 * - 50 output buffer slots + 10 upgrade slots. Each buffer slot holds 64 by
 *   default; capacity upgrades multiply that (2^tier each, multiplied together).
 * - Two output modes: push to adjacent containers, or keep in internal buffer.
 * - Exposed to CC:Tweaked as a peripheral of type "checklist_puller" so an
 *   Advanced Computer can render a yellow-on-terminal checklist display.
 *
 * AE2 references are isolated here and in PullExecutor so they only load when
 * AE2 is actually present (guarded by IntegrationCaps).
 */
public class PullerBlockEntity extends BlockEntity
        implements MenuProvider, IInWorldGridNodeHost {

    // ---- slot layout ----
    public static final int SLOT_BLUEPRINT = 0;
    public static final int BUFFER_SLOTS = 50;
    public static final int UPGRADE_SLOTS = 10;
    public static final int BASE_STACK = 64;

    /** Blueprint input slot. */
    public final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return ChecklistCalculator.isBlueprint(stack);
        }
    };

    /** Capacity upgrades: one per slot, must be a StackUpgradeItem. */
    public final ItemStackHandler upgrades = new ItemStackHandler(UPGRADE_SLOTS) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() instanceof StackUpgradeItem;
        }
        @Override public int getSlotLimit(int slot) { return 1; }
    };

    /** Output buffer: per-slot limit scales with installed upgrades. */
    public final ItemStackHandler buffer = new ItemStackHandler(BUFFER_SLOTS) {
        @Override public int getSlotLimit(int slot) { return stackLimit(); }
    };

    /** Cached checklist. */
    public List<ChecklistEntry> checklist = new ArrayList<>();
    /** Whether the pull action is running. */
    public boolean pulling = false;
    /** Status line shown in GUI / CC terminal. */
    /** Status is stored as a lang key; statusArg holds its optional %s argument. */
    public String status = "";
    public String statusArg = "";
    /** 0 = push to adjacent containers first, 1 = keep everything in internal buffer. */
    public int outputMode = 0;

    /** AE2 managed node (null when AE2 not installed). */
    private IManagedGridNode mainNode = null;

    private static final IGridNodeListener<PullerBlockEntity> NODE_LISTENER =
            new IGridNodeListener<PullerBlockEntity>() {
                @Override public void onSaveChanges(PullerBlockEntity be, IGridNode node) {
                    be.setChanged();
                }
            };

    public PullerBlockEntity(BlockPos pos, BlockState state) {
        super(PullerRegistry.PULLER_BE.get(), pos, state);
        if (IntegrationCaps.ae2Available()) {
            mainNode = GridHelper.createManagedNode(this, NODE_LISTENER)
                    .setFlags(GridFlags.REQUIRE_CHANNEL)
                    .setIdlePowerUsage(0.0)
                    .setVisualRepresentation(PullerRegistry.PULLER_ITEM.get().getDefaultInstance());
        }
    }

    // ---------------- Capacity math ----------------

    /** Total multiplier from all installed capacity upgrades (product of 2^tier), saturated. */
    public long capacityMultiplier() {
        long mult = 1;
        final long CAP = Integer.MAX_VALUE; // no stack can exceed this; clamp to avoid overflow
        for (int i = 0; i < UPGRADE_SLOTS; i++) {
            ItemStack s = upgrades.getStackInSlot(i);
            if (!s.isEmpty() && s.getItem() instanceof StackUpgradeItem u) {
                long m = u.getMultiplier();
                if (mult > CAP / m) return CAP; // saturating multiply
                mult *= m;
            }
        }
        return mult;
    }

    /** Per-slot stack limit, clamped to what an ItemStack can hold. */
    public int stackLimit() {
        long limit = BASE_STACK * capacityMultiplier();
        return (int) Math.min(Integer.MAX_VALUE, limit);
    }

    /** How many buffer slots currently hold something. */
    public int usedBufferSlots() {
        int n = 0;
        for (int i = 0; i < BUFFER_SLOTS; i++) {
            if (!buffer.getStackInSlot(i).isEmpty()) n++;
        }
        return n;
    }

    /** Total number of items currently in the internal buffer. */
    public long bufferedCount() {
        long n = 0;
        for (int i = 0; i < BUFFER_SLOTS; i++) n += buffer.getStackInSlot(i).getCount();
        return n;
    }

    // ---------------- AE2 node host ----------------

    @Override
    public IGridNode getGridNode(Direction dir) {
        return mainNode == null ? null : mainNode.getNode();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (mainNode != null && level != null && !level.isClientSide) {
            mainNode.create(level, worldPosition);
        }
    }

    @Override
    public void onChunkUnloaded() {
        if (mainNode != null) mainNode.destroy();
        super.onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        if (mainNode != null) mainNode.destroy();
        super.setRemoved();
    }

    // ---------------- Tick ----------------

    public static void tick(Level level, BlockPos pos, BlockState state, PullerBlockEntity be) {
        if (level == null || level.isClientSide) return;
        // Push live state to any player currently viewing the slot GUI.
        if (level.getServer() != null && (be.syncCounter++ % 10 == 0)) {
            for (var player : level.getServer().getPlayerList().getPlayers()) {
                if (player.containerMenu instanceof PullerMenu pm && pm.getBlockEntity() == be) {
                    PullMessages.sendChecklist(player, be);
                }
            }
        }
        if (be.pulling) be.doTickPull();
    }

    private int syncCounter = 0;
    private int pullTickCounter = 0;

    private void doTickPull() {
        recomputeAvailability();
        if (++pullTickCounter % 4 != 0) return;
        ItemStack blueprint = inventory.getStackInSlot(SLOT_BLUEPRINT);
        if (blueprint.isEmpty()) {
            status = "status.luaburner.insert_bp";
            statusArg = "";
            pulling = false;
            return;
        }
        for (ChecklistEntry e : checklist) {
            if (e.missing() <= 0) continue;
            int take = Math.min(e.missing(), stackLimit());
            ItemStack got = PullExecutor.pull(level, worldPosition, e.stack, take,
                    IntegrationCaps.ae2Available());
            if (!got.isEmpty()) {
                distribute(got);
                status = "status.luaburner.pulled";
                statusArg = got.getCount() + "x " + got.getHoverName().getString();
                recomputeAvailability();
                setChanged();
                return;
            }
        }
        status = "status.luaburner.waiting";
        statusArg = "";
    }

    /**
     * Route pulled items according to outputMode.
     * 0 = fill adjacent containers first, overflow into internal buffer.
     * 1 = keep everything in the internal buffer.
     */
    private void distribute(ItemStack stack) {
        ItemStack remaining = stack.copy();
        if (outputMode == 0) {
            for (Direction dir : Direction.values()) {
                BlockPos np = worldPosition.relative(dir);
                var cap = level.getCapability(
                        net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                        np, dir.getOpposite());
                if (cap == null) continue;
                for (int i = 0; i < cap.getSlots() && !remaining.isEmpty(); i++) {
                    remaining = cap.insertItem(i, remaining.copy(), false);
                }
            }
        }
        if (!remaining.isEmpty()) {
            for (int i = 0; i < BUFFER_SLOTS && !remaining.isEmpty(); i++) {
                remaining = buffer.insertItem(i, remaining.copy(), false);
            }
        }
        if (!remaining.isEmpty()) {
            status = "status.luaburner.buffer_full";
            statusArg = "";
        }
    }

    // ---------------- Actions (menu / network / CC peripheral) ----------------

    public void refreshChecklist() {
        ItemStack blueprint = inventory.getStackInSlot(SLOT_BLUEPRINT);
        checklist = ChecklistCalculator.calculate(level, blueprint);
        recomputeAvailability();
        status = blueprint.isEmpty() ? "status.luaburner.insert_bp"
                : (checklist.isEmpty() ? "status.luaburner.no_requirements" : "status.luaburner.ready");
        statusArg = "";
        setChanged();
    }

    public void startPull() {
        if (checklist.isEmpty()) refreshChecklist();
        pulling = !pulling;
        status = pulling ? "status.luaburner.pulling" : "status.luaburner.stopped";
        statusArg = "";
        setChanged();
    }

    /** Send full state to a player opening the slot GUI. */
    public void sendChecklistTo(net.minecraft.server.level.ServerPlayer player) {
        refreshChecklist();
        PullMessages.sendChecklist(player, this);
    }

    private void recomputeAvailability() {
        for (ChecklistEntry e : checklist) {
            int buffered = countInBuffer(e.stack);
            int external = PullExecutor.countAvailable(level, worldPosition, e.stack,
                    IntegrationCaps.ae2Available());
            e.available = buffered + external;
        }
    }

    /** Count matching items already sitting in the internal buffer. */
    private int countInBuffer(ItemStack template) {
        int n = 0;
        for (int i = 0; i < BUFFER_SLOTS; i++) {
            ItemStack s = buffer.getStackInSlot(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, template)) n += s.getCount();
        }
        return n;
    }

    // ---------------- MenuProvider ----------------

    @Override public Component getDisplayName() {
        return Component.translatable("block.luaburner.checklist_puller");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new PullerMenu(id, inv, this);
    }

    // ---------------- CC peripheral backing methods ----------------
    // (Called by PullerPeripheral; kept here so all world access stays in the BE.)

    public String ccStatus() {
        if (status == null || status.isEmpty()) return "";
        return Component.translatable(status, statusArg == null ? "" : statusArg).getString();
    }

    public boolean ccIsPulling() {
        return pulling;
    }

    /** Re-scan the blueprint and return the number of distinct required items. */
    public int ccRefresh() {
        refreshChecklist();
        return checklist.size();
    }

    /** Toggle pulling on/off. Returns the new state. */
    public boolean ccTogglePull() {
        startPull();
        return pulling;
    }

    /** Checklist rows: {name, id, required, available, missing}. */
    public List<Map<String, Object>> ccChecklist() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ChecklistEntry e : checklist) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", e.stack.getHoverName().getString());
            row.put("id", net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getKey(e.stack.getItem()).toString());
            row.put("required", e.required);
            row.put("available", e.available);
            row.put("missing", e.missing());
            rows.add(row);
        }
        return rows;
    }

    /** Storage / upgrade info for the status header. */
    public Map<String, Object> ccInfo() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("hasBlueprint", !inventory.getStackInSlot(SLOT_BLUEPRINT).isEmpty());
        m.put("bufferSlots", BUFFER_SLOTS);
        m.put("usedSlots", usedBufferSlots());
        m.put("buffered", bufferedCount());
        m.put("upgradeSlots", UPGRADE_SLOTS);
        m.put("multiplier", capacityMultiplier());
        m.put("stackLimit", stackLimit());
        m.put("outputMode", outputMode == 0 ? "adjacent" : "internal");
        m.put("pulling", pulling);
        return m;
    }

    /** 0 = adjacent-first, 1 = internal-only. Returns the mode name. */
    public String ccSetOutputMode(int mode) {
        this.outputMode = mode == 0 ? 0 : 1;
        setChanged();
        return this.outputMode == 0 ? "adjacent" : "internal";
    }

    // ---------------- Persistence ----------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.put("upgrades", upgrades.serializeNBT(registries));
        tag.put("buffer", buffer.serializeNBT(registries));
        tag.putBoolean("pulling", pulling);
        tag.putInt("outputMode", outputMode);
        tag.putString("status", status == null ? "" : status);
        tag.putString("statusArg", statusArg == null ? "" : statusArg);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        if (tag.contains("upgrades")) upgrades.deserializeNBT(registries, tag.getCompound("upgrades"));
        if (tag.contains("buffer")) buffer.deserializeNBT(registries, tag.getCompound("buffer"));
        pulling = tag.getBoolean("pulling");
        outputMode = tag.getInt("outputMode");
        status = tag.getString("status");
        statusArg = tag.getString("statusArg");
        if (level != null && !level.isClientSide) refreshChecklist();
    }
}
