package com.luaburner;

import net.neoforged.fml.ModList;

/**
 * Central place that decides whether the optional "checklist puller" machine
 * is available. This class deliberately references NO Create / AE2 types so it
 * is always safe to load, even when neither mod is installed.
 *
 * The machine is registered only if at least one supported storage network is
 * present. All code that touches Create or AE2 lives in separate classes that
 * are loaded lazily, behind these flags.
 */
public final class IntegrationCaps {
    private IntegrationCaps() {}

    private static final boolean CREATE = ModList.get().isLoaded("create");
    private static final boolean AE2 = ModList.get().isLoaded("ae2");

    /** True when the checklist puller machine should be registered. */
    public static boolean pullerEnabled() {
        return CREATE || AE2;
    }

    public static boolean createAvailable() {
        return CREATE;
    }

    public static boolean ae2Available() {
        return AE2;
    }
}
