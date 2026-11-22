--[[
  checklist_puller.lua — Advanced Computer UI for the Checklist Puller machine

  Place an Advanced Computer (or Advanced Monitor via a wired modem) next to the
  Checklist Puller block, put this file on it, and run:

      checklist_puller

  The program finds the nearby "checklist_puller" peripheral and renders the
  material checklist in the classic amber/yellow Advanced Computer terminal.

  Keys:
    Up/Down or mouse wheel  scroll
    Left/Right              page
    P                       toggle pull
    R                       rescan blueprint
    O                       toggle output mode (adjacent / internal)
    Q                       quit
]]

local peripheral = peripheral
local term = term
local colors = colors or colours

local args = { ... }
local SIDE = args[1] -- optional: explicit side, e.g. "checklist_puller north"

-- Resolve the peripheral (explicit side "type side", or auto-find).
local monitor, p
if SIDE then
    local space = SIDE:find(" ")
    if space then
        p = peripheral.wrap(SIDE:sub(space + 1))
    else
        p = peripheral.wrap(SIDE)
    end
end
if not p then
    local found
    found = peripheral.find and peripheral.find("checklist_puller")
    if found then
        if type(found) == "table" and found.getChecklist then p = found
        else
            -- find() may return side,peripheral
            local a, b = peripheral.find("checklist_puller")
            if b then p = b elseif a and type(a) == "table" then p = a end
        end
    end
end
if not p or not p.getChecklist then
    error("No checklist_puller peripheral found. Place an Advanced Computer next to the machine.")
end

-- Use an adjacent advanced monitor if present, else the computer's own display.
local function findMonitor()
    for _, side in ipairs(rs and rs.getSides and rs.getSides() or
            {"north", "south", "east", "west", "top", "bottom"}) do
        local ok, dev = pcall(peripheral.wrap, side)
        if ok and dev and dev.getTextScale then
            local t = { type = dev.getType and dev.getType() or "" }
            if t.type:find("monitor") then return dev end
        end
    end
    return nil
end
monitor = findMonitor() or term
pcall(function() monitor.setTextScale(0.5) end)

local W, H = monitor.getSize()
local function clear() monitor.setBackgroundColor(colors.black) monitor.clear() end

-- Amber palette: black background, amber header, white body, color-coded counts.
local C_BG      = colors.black
local C_HEADER  = colors.yellow
local C_HEADER_T= colors.black
local C_TEXT    = colors.white
local C_DIM     = colors.gray
local C_OK      = colors.lime
local C_MISS    = colors.red
local C_ACCENT  = colors.orange

local function heading(txt)
    monitor.setBackgroundColor(C_HEADER)
    monitor.setTextColor(C_HEADER_T)
    monitor.setCursorPos(1, 1)
    monitor.write(" " .. txt)
    -- pad the rest of the header row
    local used = #txt + 2
    monitor.setCursorPos(math.min(used, W), 1)
    monitor.write(string.rep(" ", math.max(0, W - used + 1)))
    monitor.setBackgroundColor(C_BG)
end

local scroll = 0
local rowsPerPage = math.max(1, H - 5)
local checklist, info, status = {}, {}, ""
local running = true

local function refresh()
    local ok, r = pcall(function() return p.getChecklist() end)
    if ok and type(r) == "table" then checklist = r end
    local ok2, i = pcall(function() return p.getInfo() end)
    if ok2 and type(i) == "table" then info = i end
    local ok3, s = pcall(function() return p.getStatus() end)
    if ok3 then status = tostring(s) end
    if scroll > math.max(0, #checklist - rowsPerPage) then
        scroll = math.max(0, #checklist - rowsPerPage)
    end
end

local function fmt(n)
    if n >= 1000000 then return string.format("%.1fM", n / 1000000) end
    if n >= 1000 then return string.format("%.1fk", n / 1000) end
    return tostring(n)
end

local function draw()
    clear()
    heading("CHECKLIST PULLER")
    monitor.setTextColor(C_DIM)
    monitor.setCursorPos(1, 2)
    local mode = info.outputMode or "?"
    local mult = info.multiplier or 1
    local lim = info.stackLimit or 64
    monitor.write(("Mode:%s  Mult:x%s  Slot:%s"):format(
        tostring(mode), fmt(tonumber(mult) or 1), fmt(tonumber(lim) or 64)))

    -- status / pull indicator line
    monitor.setCursorPos(1, 3)
    if info.pulling then
        monitor.setTextColor(C_ACCENT)
        monitor.write("[PULLING] ")
    else
        monitor.setTextColor(C_DIM)
        monitor.write("[IDLE]    ")
    end
    monitor.setTextColor(C_TEXT)
    monitor.write((status or ""):sub(1, W - 11))

    -- column header
    monitor.setCursorPos(1, 4)
    monitor.setTextColor(C_HEADER)
    monitor.write("ITEM")
    monitor.setCursorPos(W - 15, 4)
    monitor.write("HAVE/NEED")

    if #checklist == 0 then
        monitor.setCursorPos(1, 6)
        monitor.setTextColor(C_DIM)
        monitor.write("No blueprint inserted.")
    end

    for i = 1, rowsPerPage do
        local e = checklist[scroll + i]
        local y = 5 + i
        if y <= H and e then
            monitor.setCursorPos(1, y)
            local name = tostring(e.name or e.id or "?")
            if #name > W - 17 then name = name:sub(1, W - 17) end
            local missing = tonumber(e.missing) or 0
            monitor.setTextColor(missing > 0 and C_MISS or C_OK)
            monitor.write(name)

            local have = tonumber(e.available) or 0
            local need = tonumber(e.required) or 0
            local cnt = fmt(have) .. "/" .. fmt(need)
            monitor.setCursorPos(W - #cnt + 1, y)
            monitor.setTextColor(missing > 0 and C_ACCENT or C_OK)
            monitor.write(cnt)
        end
    end

    -- footer: controls + scroll position
    monitor.setCursorPos(1, H)
    monitor.setBackgroundColor(colors.gray)
    monitor.setTextColor(colors.black)
    local pos = (#checklist > 0) and (scroll + 1) .. "-" ..
            math.min(#checklist, scroll + rowsPerPage) .. "/" .. #checklist or "0/0"
    local footer = (" P pull  R scan  O out  ^v scroll | %s"):format(pos)
    monitor.write(footer:sub(1, W))
    monitor.setBackgroundColor(C_BG)
end

local function action(key)
    if key == "p" or key == "P" then
        pcall(function() p.togglePull() end)
    elseif key == "r" or key == "R" then
        pcall(function() p.refresh() end)
    elseif key == "o" or key == "O" then
        local cur = info and info.outputMode or "adjacent"
        pcall(function() p.setOutputMode(cur == "adjacent" and 1 or 0) end)
    end
    refresh()
end

-- Main loop: refresh periodically, handle keys + monitor touch.
local tick = 0
while running do
    draw()
    tick = tick + 1
    if tick % 20 == 0 then refresh() end

    local ev = { os.pullEvent() }
    local kind = ev[1]
    if kind == "key" then
        local k = ev[2]
        if k == keys.q then running = false
        elseif k == keys.p then action("p")
        elseif k == keys.r then action("r")
        elseif k == keys.o then action("o")
        elseif k == keys.up then scroll = math.max(0, scroll - 1)
        elseif k == keys.down then scroll = math.min(math.max(0, #checklist - rowsPerPage), scroll + 1)
        elseif k == keys.left then scroll = math.max(0, scroll - rowsPerPage)
        elseif k == keys.right then scroll = math.min(math.max(0, #checklist - rowsPerPage), scroll + rowsPerPage)
        end
    elseif kind == "mouse_scroll" then
        local d = ev[2]
        scroll = math.max(0, math.min(math.max(0, #checklist - rowsPerPage), scroll + d))
    elseif kind == "monitor_touch" or kind == "mouse_click" then
        local x, y = ev[3], ev[4]
        if y == H then action("p")
        elseif y == 1 then action("r") end
    end
end
clear()
