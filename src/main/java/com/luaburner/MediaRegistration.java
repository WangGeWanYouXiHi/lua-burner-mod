package com.luaburner;

import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.media.MediaCapability;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Registers our floppy disk with CC:Tweaked's media capability so the mod's
 * disk drives recognise it as readable/writable media.
 */
@EventBusSubscriber(modid = LuaBurnerMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class MediaRegistration {
    /** Singleton IMedia - must not capture an ItemStack (per MediaCapability contract). */
    private static final IMedia FLOPPY_MEDIA = new IMedia() {
        @Override
        public String getLabel(HolderLookup.Provider registries, ItemStack stack) {
            return com.luaburner.item.FloppyDiskItem.getLabelRaw(stack);
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
        public dan200.computercraft.api.filesystem.Mount createDataMount(
                ItemStack stack, ServerLevel level) {
            Integer id = com.luaburner.item.FloppyDiskItem.getDiskId(stack);
            if (id == null) return null;
            return dan200.computercraft.api.ComputerCraftAPI.createSaveDirMount(
                    level.getServer(), "computercraft/disk/" + id,
                    com.luaburner.item.FloppyDiskItem.CAPACITY);
        }
    };

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(MediaCapability.get(), (stack, ctx) -> FLOPPY_MEDIA,
                ModRegistries.FLOPPY_DISK.get());
    }
}
