package com.luaburner;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Server/client container for the burner GUI. Standard MC layout: machine slots on top, player inventory below. */
public class BurnerMenu extends AbstractContainerMenu {
    // GUI geometry (must match BurnerScreen)
    public static final int GUI_W = 520, GUI_H = 292;
    // Player inventory anchor: 9-wide grid centered, hotbar near the bottom
    private static final int INV_X0 = (GUI_W - 9 * 18) / 2; // 179
    private static final int INV_Y = GUI_H - 82;            // 210 - main inventory 3 rows
    private static final int HOTBAR_Y = GUI_H - 26;         // 266

    private final BurnerBlockEntity blockEntity;

    public final DataSlot progressSlot;
    public final DataSlot modeSlot;
    public final DataSlot selectedSlot;
    public final DataSlot burningSlot;

    /** Client-side / factory constructor: read BlockPos from the network buffer. */
    public BurnerMenu(int containerId, Inventory playerInv,
                      net.minecraft.network.RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, resolveBlockEntity(playerInv, buf));
    }

    private static BurnerBlockEntity resolveBlockEntity(
            Inventory playerInv, net.minecraft.network.RegistryFriendlyByteBuf buf) {
        net.minecraft.core.BlockPos pos = buf.readBlockPos();
        var level = playerInv.player.level();
        var be = level.getBlockEntity(pos);
        return be instanceof BurnerBlockEntity b ? b : null;
    }

    public BurnerMenu(int containerId, Inventory playerInv, BurnerBlockEntity be) {
        super(ModRegistries.BURNER_MENU.get(), containerId);
        this.blockEntity = be;

        Container inv = be.getInventory();
        // Machine slots live in the dedicated left column of the main panel (y < 196).
        addSlot(new Slot(inv, BurnerBlockEntity.SLOT_INPUT, 16, 40));
        addSlot(new Slot(inv, BurnerBlockEntity.SLOT_OUTPUT, 16, 66) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // output slot is read-only to the player
            }
        });

        // Player inventory: 3 rows + hotbar, in the lower panel, centered.
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInv, 9 + row * 9 + col, INV_X0 + col * 18, INV_Y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInv, col, INV_X0 + col * 18, HOTBAR_Y));

        progressSlot = addDataSlot(DataSlot.standalone());
        modeSlot = addDataSlot(DataSlot.standalone());
        selectedSlot = addDataSlot(DataSlot.standalone());
        burningSlot = addDataSlot(DataSlot.standalone());

        progressSlot.set(be.progress);
        modeSlot.set(be.mode);
        selectedSlot.set(be.selectedIndex);
    }

    public BurnerBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel() != null
                && player.distanceToSqr(blockEntity.getBlockPos().getX() + 0.5,
                        blockEntity.getBlockPos().getY() + 0.5,
                        blockEntity.getBlockPos().getZ() + 0.5) <= 64.0;
    }

    /** Called server-side each tick to push block entity state into data slots. */
    public void syncState() {
        progressSlot.set(blockEntity.progress);
        modeSlot.set(blockEntity.mode);
        selectedSlot.set(blockEntity.selectedIndex);
        burningSlot.set(blockEntity.isBurning() ? 1 : 0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = getSlot(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < 2) {
                if (!moveItemStackTo(stack, 2, 38, true)) return ItemStack.EMPTY;
            } else {
                if (BurnerBlockEntity.isVanillaDisk(stack)) {
                    if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
                } else if (!moveItemStackTo(stack, 2, 38, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
