package com.luaburner;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Server/client container for the burner GUI. */
public class BurnerMenu extends AbstractContainerMenu {
    private final BurnerBlockEntity blockEntity;

    // Data slots synced to the client
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
        // Machine slots: input (0) at top, output (1) below it
        addSlot(new Slot(inv, BurnerBlockEntity.SLOT_INPUT, 30, 35));
        addSlot(new Slot(inv, BurnerBlockEntity.SLOT_OUTPUT, 30, 75) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // output slot is read-only to the player
            }
        });

        // Player inventory: 9 hotbar + 3x9
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, 9 + row * 9 + col, 8 + col * 18, 84 + row * 18));
            }
        }

        progressSlot = addDataSlot(DataSlot.standalone());
        modeSlot = addDataSlot(DataSlot.standalone());
        selectedSlot = addDataSlot(DataSlot.standalone());
        burningSlot = addDataSlot(DataSlot.standalone());

        // Initial values from the block entity
        progressSlot.set(be.progress);
        modeSlot.set(be.mode);
        selectedSlot.set(be.selectedIndex);
    }

    public BurnerBlockEntity getBlockEntity() {
        return blockEntity;
    }

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
                // machine slots -> player inventory
                if (!moveItemStackTo(stack, 2, 38, true)) return ItemStack.EMPTY;
            } else {
                // player inventory -> input slot if it's a floppy
                if (stack.getItem() instanceof com.luaburner.item.FloppyDiskItem) {
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
