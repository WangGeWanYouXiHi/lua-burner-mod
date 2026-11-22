package com.luaburner.puller;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/**
 * One line of the blueprint's material checklist.
 * Pure data (no Create/AE2 types) so it is safe to send over the network and
 * to display on the client even when only one of the two mods is installed.
 */
public final class ChecklistEntry {
    /** A representative stack for this requirement (carries strict NBT for tools). */
    public final ItemStack stack;
    /** Total amount required to build the whole schematic. */
    public final int required;
    /** Amount currently available (from AE2 network / adjacent inventory). */
    public int available;

    public ChecklistEntry(ItemStack stack, int required) {
        this.stack = stack;
        this.required = required;
    }

    public int missing() {
        return Math.max(0, required - available);
    }

    public void write(RegistryFriendlyByteBuf buf) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
        buf.writeVarInt(required);
        buf.writeVarInt(available);
    }

    public static ChecklistEntry read(RegistryFriendlyByteBuf buf) {
        ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        int required = buf.readVarInt();
        int available = buf.readVarInt();
        ChecklistEntry e = new ChecklistEntry(stack, required);
        e.available = available;
        return e;
    }
}
