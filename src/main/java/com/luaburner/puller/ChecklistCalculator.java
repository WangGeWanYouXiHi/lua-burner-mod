package com.luaburner.puller;

import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.cannon.MaterialChecklist;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps Create's own schematic-requirement pipeline (the one the Schematicannon
 * uses) to compute the material checklist of a blueprint. This class is only
 * ever loaded when Create is present (guarded by IntegrationCaps).
 */
public final class ChecklistCalculator {
    private ChecklistCalculator() {}

    /**
     * @param level the server level (needed to resolve block states / BEs in the schematic)
     * @param blueprintStack a Schematic ItemStack (Create blueprint / clipboard-written schematic)
     * @return the material checklist, empty if the stack is not a valid blueprint
     */
    public static List<ChecklistEntry> calculate(Level level, ItemStack blueprintStack) {
        List<ChecklistEntry> out = new ArrayList<>();
        if (level == null || blueprintStack.isEmpty()) return out;

        SchematicPrinter printer = new SchematicPrinter();
        try {
            printer.loadSchematic(blueprintStack, level, true);
        } catch (Throwable t) {
            return out; // not a valid schematic
        }
        if (!printer.isLoaded() || printer.isErrored()) return out;

        MaterialChecklist checklist = new MaterialChecklist();
        // Predicate: count every block in the schematic (the "place all" case).
        SchematicPrinter.PlacementPredicate predicate =
                (pos, state, be, current, replacement, defer) -> true;

        try {
            printer.markAllBlockRequirements(checklist, level, predicate);
            printer.markAllEntityRequirements(checklist);
        } catch (Throwable t) {
            return out;
        }

        Object2IntMap<Item> required = checklist.required;
        if (required == null) return out;

        for (Object2IntMap.Entry<Item> e : required.object2IntEntrySet()) {
            Item item = e.getKey();
            int amount = e.getIntValue();
            if (item == null || amount <= 0) continue;
            out.add(new ChecklistEntry(new ItemStack(item), amount));
        }
        return out;
    }

    /** True if the given stack looks like a Create blueprint we can read. */
    public static boolean isBlueprint(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String name = stack.getItem().getClass().getName();
        // Create's blueprint item lives under content.schematics.SchematicItem
        return name.contains("create") && name.contains("Schematic");
    }
}
