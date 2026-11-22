package com.luaburner.item;

import net.minecraft.world.item.Item;

/** A hard drive item: 10 capacity tiers (1-10 GB) x 17 colors. No recipes yet. */
public class HardDriveItem extends Item {
    private final int capacityGb;
    private final ColorVariant color;

    public HardDriveItem(int capacityGb, ColorVariant color) {
        super(new Item.Properties().stacksTo(1));
        this.capacityGb = capacityGb;
        this.color = color;
    }

    public int getCapacityGb() {
        return capacityGb;
    }

    public ColorVariant getColor() {
        return color;
    }

    /** 17 variants: default (no dye) + 16 Minecraft dye colors. */
    public enum ColorVariant {
        DEFAULT("default", 0x000000),
        WHITE("white", 0xFFFFFF),
        ORANGE("orange", 0xFF781F),
        MAGENTA("magenta", 0xFF3BF0),
        LIGHT_BLUE("light_blue", 0x5AD7F0),
        YELLOW("yellow", 0xF0DE1E),
        LIME("lime", 0x80C71F),
        PINK("pink", 0xF03881),
        GRAY("gray", 0x474F52),
        LIGHT_GRAY("light_gray", 0x9D9D97),
        CYAN("cyan", 0x169C9D),
        PURPLE("purple", 0x8932B8),
        BLUE("blue", 0x3C44AA),
        BROWN("brown", 0x835432),
        GREEN("green", 0x16800C),
        RED("red", 0xB02E26),
        BLACK("black", 0x1D1D21);

        public final String name;
        public final int rgb;

        ColorVariant(String name, int rgb) {
            this.name = name;
            this.rgb = rgb;
        }
    }
}
