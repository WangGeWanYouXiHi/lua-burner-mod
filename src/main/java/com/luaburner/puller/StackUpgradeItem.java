package com.luaburner.puller;

import net.minecraft.world.item.Item;

/**
 * Capacity upgrade for the checklist puller.
 *
 * Each tier doubles the per-slot stack limit of the puller's buffer:
 *   tier 1 -> x2, tier 2 -> x4, tier 3 -> x8 ... tier N -> 2^N
 *
 * Multiple upgrades stack MULTIPLICATIVELY. With two tier-1 upgrades installed
 * the multiplier is 2 * 2 = 4 (x4). The machine has 10 upgrade slots, each
 * holding a single upgrade, so the total multiplier is the product of all
 * installed upgrades' multipliers.
 */
public class StackUpgradeItem extends Item {
    /** 1-based tier. */
    private final int tier;

    public StackUpgradeItem(int tier) {
        super(new Item.Properties().stacksTo(1));
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    /** Multiplier contributed by this single upgrade (2^tier). */
    public int getMultiplier() {
        return 1 << tier; // 2^tier, tier 1..10 -> 2..1024
    }
}
