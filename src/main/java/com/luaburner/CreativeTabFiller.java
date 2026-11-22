package com.luaburner;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * Fills creative tabs:
 *  - Our own "hard_drives" tab gets all 170 hard drives.
 *  - CC:Tweaked's "computercraft:tab" gets our machines and upgrades, since
 *    this mod is an extension of CC computers.
 */
@EventBusSubscriber(modid = LuaBurnerMod.MOD_ID)
public final class CreativeTabFiller {
    private CreativeTabFiller() {}

    private static final ResourceKey<CreativeModeTab> CC_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                    ResourceLocation.fromNamespaceAndPath("computercraft", "tab"));

    @SubscribeEvent
    public static void fill(BuildCreativeModeTabContentsEvent event) {
        ResourceKey<CreativeModeTab> key = event.getTabKey();

        // 170 hard drives -> dedicated tab
        if (ModRegistries.HARD_DRIVE_TAB.getKey().equals(key)) {
            for (var holder : ModRegistries.HARD_DRIVES.values()) {
                event.accept(new ItemStack(holder.get()));
            }
            return;
        }

        // Machines + upgrades -> CC's original tab
        if (CC_TAB.equals(key)) {
            event.accept(new ItemStack(ModRegistries.BURNER_ITEM.get()));
            if (IntegrationCaps.pullerEnabled()) {
                event.accept(new ItemStack(
                        com.luaburner.puller.PullerRegistry.PULLER_ITEM.get()));
                for (var holder : com.luaburner.puller.PullerRegistry.STACK_UPGRADES.values()) {
                    event.accept(new ItemStack(holder.get()));
                }
            }
        }
    }
}
