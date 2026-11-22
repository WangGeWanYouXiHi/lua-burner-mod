--[[
  checklist_puller.lua — 清单拉取器的高级计算机终端界面

  把高级计算机（或通过有线调制解调器连接的显示器）放在清单拉取器旁边，
  把本文件上传上去，然后运行：

      checklist_puller

  程序会寻找附近的 "checklist_puller" 外设，并在经典的黄底高级计算机
  终端上显示物料清单。

  按键：
    上/下 或 鼠标滚轮  滚动
    左/右              翻页
    P                  开始/停止拉取
    R                  重新扫描蓝图
    O                  切换输出模式（相邻容器 / 内部缓冲）
    Q                  退出

  语言：把下面的 LANG 改成 "zh"（中文）或 "en"（English）。
]]

-- 界面语言："zh" = 中文， "en" = English
local LANG = "zh"

local TEXT = {
    zh = {
        title = "清单拉取器",
        mode = "模式:%s  倍率:x%s  每格:%s",
        pulling = "[拉取中] ",
        idle = "[空闲]    ",
        item = "物品",
        have_need = "已有/需要",
        no_bp = "未放入蓝图。",
        footer = " P拉取  R刷新  O输出  ^v翻页 | %s",
        error = "找不到 checklist_puller 外设。请把高级计算机放在机器旁边。",
    },
    en = {
        title = "CHECKLIST PULLER",
        mode = "Mode:%s  Mult:x%s  Slot:%s",
        pulling = "[PULLING] ",
        idle = "[IDLE]    ",
        item = "ITEM",
        have_need = "HAVE/NEED",
        no_bp = "No blueprint inserted.",
        footer = " P pull  R scan  O out  ^v scroll | %s",
        error = "No checklist_puller peripheral found. Place an Advanced Computer next to the machine.",
    },
}
local T = TEXT[LANG] or TEXT.en

-- Cobalt Lua 5.2 的 # 和 string.sub 按字节计算；中文一个字占 3 字节。
-- 下面两个函数按"字符"计数，避免中文被截断成乱码、表头补白错位。
local function ulen(s)
    local n, i = 0, 1
    while i <= #s do
        local b = s:byte(i)
        if b < 128 then i = i + 1
        elseif b < 224 then i = i + 2
        elseif b < 240 then i = i + 3
        else i = i + 4 end
        n = n + 1
    end
    return n
end

local function usub(s, maxchars)
    local n, i = 0, 1
    while i <= #s do
        local b = s:byte(i)
        local step = b < 128 and 1 or (b < 224 and 2 or (b < 240 and 3 or 4))
        if n >= maxchars then break end
        i = i + step
        n = n + 1
    end
    return s:sub(1, i - 1)
end

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
    error(T.error)
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
    -- pad the rest of the header row (count characters, not bytes)
    local used = ulen(txt) + 2
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
    heading(T.title)
    monitor.setTextColor(C_DIM)
    monitor.setCursorPos(1, 2)
    local mode = info.outputMode or "?"
    local mult = info.multiplier or 1
    local lim = info.stackLimit or 64
    monitor.write(T.mode:format(
        tostring(mode), fmt(tonumber(mult) or 1), fmt(tonumber(lim) or 64)))

    -- status / pull indicator line
    monitor.setCursorPos(1, 3)
    if info.pulling then
        monitor.setTextColor(C_ACCENT)
        monitor.write(T.pulling)
    else
        monitor.setTextColor(C_DIM)
        monitor.write(T.idle)
    end
    monitor.setTextColor(C_TEXT)
    monitor.write(usub(status or "", math.max(1, W - ulen(T.pulling) - 1)))

    -- column header
    monitor.setCursorPos(1, 4)
    monitor.setTextColor(C_HEADER)
    monitor.write(T.item)
    monitor.setCursorPos(W - ulen(T.have_need) + 1, 4)
    monitor.write(T.have_need)

    if #checklist == 0 then
        monitor.setCursorPos(1, 6)
        monitor.setTextColor(C_DIM)
        monitor.write(T.no_bp)
    end

    for i = 1, rowsPerPage do
        local e = checklist[scroll + i]
        local y = 5 + i
        if y <= H and e then
            monitor.setCursorPos(1, y)
            local name = tostring(e.name or e.id or "?")
            local nameMax = math.max(1, W - 17) -- keep room for the HAVE/NEED column
            if ulen(name) > nameMax then name = usub(name, nameMax) end
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
    monitor.write(usub(T.footer:format(pos), W))
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
