# Prebuilt jar / 预编译版本

Can't build the mod because Gradle or the dependencies won't download? Grab the ready-made jar here — no compiler, no Java setup, no Gradle required.

编译工具（Gradle / JDK / 依赖）下不下来？直接用这里的成品 jar，不用编译、不用装 Java、不用碰 Gradle。

## File / 文件

| File | Version | Size |
| --- | --- | --- |
| `lua-burner-mod-0.4.0.jar` | 0.4（中文 + 硬盘/软盘/GUI 修复 + 断连修复） | 191 KB |

SHA-256: `6ff697d1ee925dcdd41d7af99df28d868e923c35279977a5df0a70d2653f1bc0`

## What's new in 0.4 / 0.4 更新内容

- **Fixed the "Connection Lost" crash** when opening the burner with a large `.lua` file in `lua-blueprints/`. The blueprint list is now sent as raw bytes instead of a 32767-byte-capped UTF string, so big files (40 KB+) no longer kick you from the world.
  修复了 `lua-blueprints/` 里有大 `.lua` 文件时打开刻录机就"连接丢失"的崩溃。蓝图列表改用字节传输，不再受 32767 字节上限限制，40 KB+ 的大文件不会再把玩家踢下线。
- **Hard drives are now real CC media**: 170 drives (1–10 GB × 17 colors) work in a Disk Drive — mount, read/write files, `disk.setLabel()`. Capacity is enforced.
  硬盘现在是真正的 CC 存储介质：170 个硬盘（1–10 GB × 17 色）能放进磁盘驱动器挂载、读写文件、`disk.setLabel()`，容量真实生效。
- **CC vanilla floppy tooltip** shows a capacity note (capacity is config-driven and may be unlimited — check with `fs.getCapacity("disk/")` in Lua). Item registration untouched, saves stay compatible.
  CC 原版软盘的提示框加上容量说明（容量由配置决定、可能无上限——在 Lua 里用 `fs.getCapacity("disk/")` 查看）。不改动物品注册，存档兼容。
- **Rebuilt both GUIs** into standard Minecraft container layout: main panel on top, separate player-inventory panel below, no overlapping slots.
  两个 GUI 重做为标准 MC 容器布局：上方主面板 + 下方独立背包面板，槽位不再重叠。

## How to install / 怎么装

1. Copy `lua-burner-mod-0.4.0.jar` into your modpack's `mods/` folder.
   把 `lua-burner-mod-0.4.0.jar` 丢进整合包的 `mods/` 文件夹。
2. Make sure the required dependencies below are installed.
   确保下面的必需依赖都装好了。
3. Launch the game. Done.
   启动游戏，收工。

## Language / 语言

Since 0.3 the mod ships with Simplified Chinese (`zh_cn.json`). Set the game language to 简体中文 and all item names, GUI labels and status messages follow automatically. The CC terminal program (`cc-programs/checklist_puller.lua`) defaults to Chinese — change `LANG = "zh"` to `"en"` near the top of the file for English.

0.3 起内置简体中文（`zh_cn.json`）。把游戏语言设为简体中文，物品名、GUI 文字和状态提示会自动切换。CC 终端程序（`cc-programs/checklist_puller.lua`）默认中文——把文件开头的 `LANG = "zh"` 改成 `"en"` 即可切回英文。

## Dependencies / 依赖

| Mod | Required? | Version | Notes |
| --- | --- | --- | --- |
| NeoForge | **Yes** | 21.0+ (MC 1.21.1) | Mod loader itself |
| Minecraft | **Yes** | 1.21.1 | Game version |
| CC:Tweaked | **Yes** | 1.118.0+ | Provides the floppy disk / peripheral API |
| Create | Optional | 6.0.0+ | Needed for the checklist puller machine |
| AE2 | Optional | 19.0.0+ | Needed to pull items from an ME network |

> The checklist puller machine only registers when **Create or AE2** is installed. The Lua blueprint burner works with just CC:Tweaked.
>
> 清单拉取器只有在装了 **Create 或 AE2** 时才会注册；Lua 蓝图刻录机只要 CC:Tweaked 就能用。

The CC terminal program for the checklist puller lives one folder up, in [`cc-programs/checklist_puller.lua`](../cc-programs/checklist_puller.lua) — upload it to an advanced computer placed next to the machine.

清单拉取器的 CC 终端程序在上一层的 [`cc-programs/checklist_puller.lua`](../cc-programs/checklist_puller.lua)，上传到机器旁边的**高级计算机**即可。

## Verify the file / 校验文件

```bash
sha256sum lua-burner-mod-0.4.0.jar
```

The output should match the SHA-256 above. If it doesn't, the download is corrupted — re-download it.

输出应和上面的 SHA-256 一致；不一致说明文件下坏了，重新下载即可。

## Building from source / 自己编译

If you'd rather compile it yourself (or want the latest unreleased changes), see the [root README](../README.md). The source build needs a JDK 21 and Gradle, plus the API jars in `libs/`.

想自己编译（或用最新的未发布改动），看根目录的 [README](../README.md)。源码编译需要 JDK 21 + Gradle，以及 `libs/` 里的 API jar。
