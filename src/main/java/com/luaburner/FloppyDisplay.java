package com.luaburner;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Display-only tweak for CC's vanilla floppy disk. Does NOT change the item,
 * its registration, capabilities or NBT — only what the player sees in the
 * tooltip, so saves and disk drives stay fully compatible.
 *
 * Appends a capacity note to the item's name line and explains how to read the
 * real capacity in Lua (the value is config-driven and can be unlimited).
 */
@EventBusSubscriber(modid = LuaBurnerMod.MOD_ID, value = Dist.CLIENT)
public final class FloppyDisplay {
    private FloppyDisplay() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        if (!BuiltInRegistries.ITEM.getKey(stack.getItem())
                .equals(BurnerBlockEntity.CC_DISK_ID)) return;

        var lines = event.getToolTip();
        if (lines.isEmpty()) return;

        // Append to the name line (index 0) so capacity sits right after the name.
        Component name = lines.get(0).copy()
                .append(Component.translatable("tooltip.luaburner.floppy.suffix"));
        lines.set(0, name);

        // Detailed explanation as an extra line (avoid duplicating if already present).
        lines.add(Component.translatable("tooltip.luaburner.floppy"));
    }
}
