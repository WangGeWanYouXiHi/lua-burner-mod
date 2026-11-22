package com.luaburner.puller;

import com.luaburner.LuaBurnerMod;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registers the checklist-puller machine and its capacity upgrades. This class
 * references the optional integration + CC peripheral types and is only touched
 * when IntegrationCaps.pullerEnabled() is true.
 */
public final class PullerRegistry {
    private PullerRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(LuaBurnerMod.MOD_ID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(LuaBurnerMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    LuaBurnerMod.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, LuaBurnerMod.MOD_ID);

    public static final DeferredBlock<Block> PULLER =
            BLOCKS.register("checklist_puller", PullerBlock::new);
    public static final DeferredItem<Item> PULLER_ITEM =
            ITEMS.register("checklist_puller", () -> new BlockItem(PULLER.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PullerBlockEntity>> PULLER_BE =
            BLOCK_ENTITIES.register("checklist_puller", () -> BlockEntityType.Builder.of(
                    PullerBlockEntity::new, PULLER.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<PullerMenu>> PULLER_MENU =
            MENUS.register("checklist_puller", () ->
                    net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(
                            PullerMenu::new));

    /** Capacity upgrades, tiers 1..10. Each doubles per-slot stack size; multipliers multiply. */
    public static final Map<Integer, DeferredItem<StackUpgradeItem>> STACK_UPGRADES = new LinkedHashMap<>();
    static {
        for (int tier = 1; tier <= 10; tier++) {
            final int t = tier;
            STACK_UPGRADES.put(tier,
                    ITEMS.register("stack_upgrade_" + tier, () -> new StackUpgradeItem(t)));
        }
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);

        // Register the puller as a CC peripheral. CC:T is a required dependency,
        // so PeripheralCapability is always available when this class loads.
        bus.addListener((RegisterCapabilitiesEvent event) -> {
            event.registerBlockEntity(PeripheralCapability.get(), PULLER_BE.get(),
                    (be, side) -> new PullerPeripheral(be));
        });
    }
}
