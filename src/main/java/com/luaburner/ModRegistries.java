package com.luaburner;

import com.luaburner.item.FloppyDiskItem;
import com.luaburner.item.HardDriveItem;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

/** Central registry for all items, blocks, block entities and menus. */
public final class ModRegistries {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(LuaBurnerMod.MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(LuaBurnerMod.MOD_ID);
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(LuaBurnerMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, LuaBurnerMod.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, LuaBurnerMod.MOD_ID);

    // ---- Data components for floppy disks ----
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> DISK_ID =
            DATA_COMPONENTS.registerComponentType("disk_id",
                    b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> DISK_LABEL =
            DATA_COMPONENTS.registerComponentType("disk_label",
                    b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    // ---- Floppy disk (1.44 MB, CC:T media) ----
    public static final DeferredItem<FloppyDiskItem> FLOPPY_DISK =
            ITEMS.register("floppy_disk", FloppyDiskItem::new);

    // ---- Hard drives: 10 capacities x 17 colors = 170 items ----
    /** key = "hard_drive_<gb>gb_<color>" */
    public static final Map<String, DeferredItem<HardDriveItem>> HARD_DRIVES = new LinkedHashMap<>();

    static {
        for (int gb = 1; gb <= 10; gb++) {
            final int capacity = gb;
            for (HardDriveItem.ColorVariant color : HardDriveItem.ColorVariant.values()) {
                String id = "hard_drive_" + gb + "gb_" + color.name;
                HARD_DRIVES.put(id, ITEMS.register(id, () -> new HardDriveItem(capacity, color)));
            }
        }
    }

    // ---- Burner machine ----
    public static final DeferredBlock<Block> BURNER =
            BLOCKS.register("burner", BurnerBlock::new);
    public static final DeferredItem<Item> BURNER_ITEM =
            ITEMS.register("burner", () -> new net.minecraft.world.item.BlockItem(
                    BURNER.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BurnerBlockEntity>> BURNER_BE =
            BLOCK_ENTITIES.register("burner", () -> BlockEntityType.Builder.of(
                    BurnerBlockEntity::new, BURNER.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<BurnerMenu>> BURNER_MENU =
            MENUS.register("burner", () ->
                    net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(
                            BurnerMenu::new));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        BLOCKS.register(bus);
        DATA_COMPONENTS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
    }
}
