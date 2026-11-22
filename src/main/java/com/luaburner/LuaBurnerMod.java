package com.luaburner;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Lua Blueprint Burner - a blueprint-desk style machine that burns local Lua
 * files onto CC:Tweaked floppy disks.
 */
@Mod(LuaBurnerMod.MOD_ID)
public class LuaBurnerMod {
    public static final String MOD_ID = "luaburner";

    public LuaBurnerMod(IEventBus modBus) {
        ModRegistries.register(modBus);
    }
}
