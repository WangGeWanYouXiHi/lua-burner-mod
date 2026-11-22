package com.luaburner.puller;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;

import java.util.List;
import java.util.Map;

/**
 * CC:Tweaked peripheral wrapper for the checklist puller.
 *
 * Kept as a separate class from the BlockEntity because BlockEntity already has
 * a getType() method with an incompatible return type. All methods run on the
 * main server thread (mainThread = true) and simply delegate to the BE.
 */
public class PullerPeripheral implements IPeripheral {
    private final PullerBlockEntity be;

    public PullerPeripheral(PullerBlockEntity be) {
        this.be = be;
    }

    @Override
    public String getType() {
        return "checklist_puller";
    }

    @Override
    public boolean equals(IPeripheral other) {
        return other instanceof PullerPeripheral p && p.be == be;
    }

    @LuaFunction(mainThread = true)
    public final String getStatus() {
        return be.ccStatus();
    }

    @LuaFunction(mainThread = true)
    public final boolean isPulling() {
        return be.ccIsPulling();
    }

    /** Re-scan the blueprint; returns the number of distinct required items. */
    @LuaFunction(mainThread = true)
    public final int refresh() {
        return be.ccRefresh();
    }

    /** Toggle pulling; returns the new state. */
    @LuaFunction(mainThread = true)
    public final boolean togglePull() {
        return be.ccTogglePull();
    }

    /** Array of {name, id, required, available, missing} tables. */
    @LuaFunction(mainThread = true)
    public final List<Map<String, Object>> getChecklist() {
        return be.ccChecklist();
    }

    /** Storage / upgrade info table. */
    @LuaFunction(mainThread = true)
    public final Map<String, Object> getInfo() {
        return be.ccInfo();
    }

    /** 0 = adjacent-first, 1 = internal-only. Returns mode name. */
    @LuaFunction(mainThread = true)
    public final String setOutputMode(int mode) {
        return be.ccSetOutputMode(mode);
    }
}
