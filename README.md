# Lua Blueprint Burner

A Minecraft mod (NeoForge 1.21.1) that adds a blueprint-desk style machine. It burns local `.lua` files onto CC:Tweaked floppy disks — no more typing 1000+ lines by hand in-game.

> Works together with CC:Tweaked 1.118+ (via NeoForge/Sinytra compatibility in modpacks with Synaxis / 控制学).

## Download / 下载

Don't want to build from source? A ready-to-use jar is in [`prebuilt/`](prebuilt/README.md) — just drop it into your `mods/` folder. No Gradle, no JDK, no compiling. 不想自己编译？[`prebuilt/`](prebuilt/README.md) 里有现成的 jar，丢进 `mods/` 就能用。

## 语言 / Language

0.3 起内置简体中文（`zh_cn.json`）：把游戏语言设为简体中文，物品名、GUI 文字、状态提示会自动切换。CC 终端程序 `cc-programs/checklist_puller.lua` 默认中文，把文件顶部的 `LANG = "zh"` 改成 `"en"` 即可切回英文。

Since 0.3, Simplified Chinese is bundled (`zh_cn.json`) and follows the game language automatically. The CC terminal program `cc-programs/checklist_puller.lua` defaults to Chinese — set `LANG = "en"` at the top of the file for English.

## Features

- **Blueprint burner block** — a computer-styled machine with a GUI:
  - Left: scrollable **code preview** with a persistent scrollbar and line numbers
  - Middle: **blueprint file list** (scroll wheel to switch, ellipsis at list edges)
  - Right: **blueprint info** (name / size / capacity) + **mode toggle**
  - Disk slots: **input** (blank or used disk) and **output** (burned disk)
  - Bottom: chunked **progress bar** (40 lines/tick — never freezes the game)
  - Single **Burn** button at bottom-right
- **Two burn modes**
  - **Overwrite** — wipe the disk, write only the selected Lua file
  - **Incremental** — keep existing files, append the selected file (burn multiple files into one disk)
- **Vanilla CC floppy** — uses CC:Tweaked's own **Floppy Disk** (`computercraft:disk`); no custom disk item. Writes go through CC's `MediaCapability` / `IMedia.createDataMount`, so capacity follows your CC config.
- **Hard drives** — 10 capacity tiers (1 GB → 10 GB) × 17 colors (default + 16 dyes) = **170 items**, procedurally generated. No recipes yet (as requested).
- **Creative tabs** — hard drives live in their own **"Lua Hard Drives"** tab; the machine blocks and stack upgrades are added to CC:Tweaked's original **ComputerCraft** tab.
- **Blueprint source** — drop your `.lua` files into the world save's `lua-blueprints/` folder; the GUI lists them automatically.

## Checklist Puller (Create / AE2)

A second machine that reads a Create **schematic/blueprint** and pulls every missing material from the storage network. It registers only when **Create** or **AE2** is installed.

- **Reads the real cannon checklist** — uses Create's own `SchematicPrinter` / `MaterialChecklist` pipeline, so the displayed requirements match the Schematicannon exactly (hundreds of block types, thousands of blocks).
- **Massive capacity**
  - Blueprint slot ×1
  - **50 output buffer slots** (5 × 10), **64 items per slot** by default
  - **10 upgrade slots** (one upgrade each). **Stack Upgrades** (tiers 1–10) multiply per-slot stack size: tier N = ×2ⁿ. Multipliers are **multiplicative** — two tier-1 upgrades = ×4, two ×100-class upgrades = ×10 000.
- **Two output modes**
  - `adjacent` — push pulled items into nearby containers first, overflow into the internal buffer
  - `internal` — keep everything in the machine's buffer
- **CC:Tweaked peripheral** — type `checklist_puller`. Place an **Advanced Computer** next to it, run the bundled `cc-programs/checklist_puller.lua`, and get a classic **amber-on-black Advanced Computer terminal** showing the checklist with **color-coded counts** (green = stocked, red/orange = missing), scrolling/paging, hotkeys to pull / rescan / switch mode.

### Peripheral API

| Method | Returns | Effect |
|---|---|---|
| `getChecklist()` | array of `{name, id, required, available, missing}` | the material checklist |
| `getInfo()` | table | `outputMode`, `multiplier`, `stackLimit`, `pulling`, buffer usage… |
| `getStatus()` | string | current status line |
| `togglePull()` | boolean | start/stop pulling |
| `refresh()` | int | rescan the blueprint |
| `setOutputMode(mode)` | string | `0` = adjacent, `1` = internal |

## How to use

1. Install alongside CC:Tweaked (and your pack's NeoForge compatibility layer).
2. Copy your `.lua` files into `<world save>/lua-blueprints/`.
3. Find the **Lua Blueprint Burner** in the **ComputerCraft** creative tab, place it and right-click it.
4. Scroll the blueprint list to pick a file, review the preview, choose Overwrite/Incremental.
5. Put a **vanilla CC Floppy Disk** in the **IN** slot, click **Burn to Disk**, watch the progress bar.
6. Take the burned disk from the **OUT** slot. Insert it into a CC:T disk drive — the file appears on `disk/`.

**Checklist Puller:**
1. Install Create or AE2 alongside this mod.
2. Place the **Blueprint Checklist Puller**; put a Create schematic (blueprint/clipboard-written) in its **BP** slot.
3. Put **Stack Upgrades** in the upgrade slots to raise per-slot stack size (optional).
4. Place an **Advanced Computer** directly next to the machine; upload `cc-programs/checklist_puller.lua` to it and run `checklist_puller`.
5. Watch the amber checklist: `P` toggles pulling, `R` rescans, `O` switches adjacent/internal output, arrows/wheel scroll.

## Building from source

This project builds fully offline using a local `file://` Maven mirror of the Gradle cache (see `repo/`).

```
# Requires JDK 21 and Gradle 8.14
gradle build --console=plain --no-configuration-cache
```

The CC:Tweaked API jars are vendored in `libs/` (compile-only; not shipped in the output jar).

## Project layout

- `src/main/java/com/luaburner/` — mod source
- `src/main/java/com/luaburner/puller/` — checklist puller (block, BE, CC peripheral, upgrades, network)
- `prebuilt/` — ready-to-use compiled jar + install instructions (no build required)
- `src/main/resources/assets/luaburner/` — lang, models, textures
- `cc-programs/checklist_puller.lua` — amber Advanced Computer UI for the puller
- `gen_assets.py` — regenerates all textures, item models and lang entries
- `repo/` — local Maven mirror (generated by the offline-gradle-build skill)
- `libs/` — CC:Tweaked API jars (compile-only)

## License

LGPL-3.0 — see [LICENSE](LICENSE) (LGPLv3) and [COPYING](COPYING) (GPLv3, which LGPLv3 incorporates).

In plain terms:

- **Using this mod as-is does not require you to open-source your own project.** You may depend on it, import it, and ship it inside a modpack or your own mod without releasing your source.
- **If you modify this repository's code and distribute that modified version**, you must release your changes under LGPLv3 (dynamic linking is still fine — the copyleft applies to changes to this library itself, not to programs that use it).

Copyright (c) 2026 WangGeWanYouXiHi
