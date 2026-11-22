package com.luaburner.item;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.component.DataComponentType;

/** Data components backing real CC media on the hard drive items. */
public final class ModDataComponents {
    private ModDataComponents() {}

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "luaburner");

    /** Stable numeric id mapping this stack to a folder under the world save. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> DISK_ID =
            DATA_COMPONENTS.register("disk_id", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** Disk label shown by CC's disk.getLabel() and in the drive. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> DISK_LABEL =
            DATA_COMPONENTS.register("disk_label", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    public static void register(IEventBus bus) {
        DATA_COMPONENTS.register(bus);
    }
}
