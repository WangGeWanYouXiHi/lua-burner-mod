package com.luaburner.puller;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Container for the checklist puller: blueprint + upgrades + 50-slot buffer + player inventory. */
public class PullerMenu extends AbstractContainerMenu {
    public static final int BUFFER_ROWS = 5;
    public static final int BUFFER_COLS = 10;
    public static final int GUI_W = 196, GUI_H = 344;
    private static final int INV_X0 = 8;
    public static final int MAIN_H = 250;              // main panel ends here
    private static final int INV_Y = GUI_H - 82;       // 262 - player 3 rows
    private static final int HOTBAR_Y = GUI_H - 26;    // 318

    private final PullerBlockEntity blockEntity;

    public PullerMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, resolveBE(playerInv, buf));
    }

    private static PullerBlockEntity resolveBE(Inventory playerInv, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        var level = playerInv.player.level();
        var be = level.getBlockEntity(pos);
        return be instanceof PullerBlockEntity p ? p : null;
    }

    public PullerMenu(int containerId, Inventory playerInv, PullerBlockEntity be) {
        super(PullerRegistry.PULLER_MENU.get(), containerId);
        this.blockEntity = be;

        // Blueprint input slot (top-left of main panel)
        addSlot(new SlotItemHandler(be.inventory, PullerBlockEntity.SLOT_BLUEPRINT, 8, 20));

        // 10 upgrade slots (2 rows of 5)
        for (int i = 0; i < PullerBlockEntity.UPGRADE_SLOTS; i++) {
            int col = i % 5, row = i / 5;
            addSlot(new SlotItemHandler(be.upgrades, i, 100 + col * 18, 20 + row * 18));
        }

        // 50 buffer slots (5 rows of 10)
        for (int i = 0; i < PullerBlockEntity.BUFFER_SLOTS; i++) {
            int col = i % BUFFER_COLS, row = i / BUFFER_COLS;
            addSlot(new SlotItemHandler(be.buffer, i, 8 + col * 18, 62 + row * 18));
        }

        // Player inventory in its own lower panel — no overlap with buffer slots.
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInv, 9 + row * 9 + col, INV_X0 + col * 18, INV_Y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInv, col, INV_X0 + col * 18, HOTBAR_Y));
    }

    public PullerBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel() != null
                && player.distanceToSqr(blockEntity.getBlockPos().getX() + 0.5,
                        blockEntity.getBlockPos().getY() + 0.5,
                        blockEntity.getBlockPos().getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // no shift-click crafting
    }
}
