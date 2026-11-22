# Prebuilt jar / 预编译版本

Can't build the mod because Gradle or the dependencies won't download? Grab the ready-made jar here — no compiler, no Java setup, no Gradle required.

编译工具（Gradle / JDK / 依赖）下不下来？直接用这里的成品 jar，不用编译、不用装 Java、不用碰 Gradle。

## File / 文件

| File | Version | Size |
| --- | --- | --- |
| `lua-burner-mod-0.5.0.jar` | 0.5（CC 终端风 GUI + 任意文件刻录） | 192 KB |

SHA-256: `93613734c3c7b5a7228fd9c990aba6d8d17dc182c5b9326c84cce6abde49b26c`

## What's new in 0.5 / 0.5 更新内容

- **CC terminal-style GUIs**: both machines now use the Advanced Computer look — black panels, amber/yellow text, green progress bar and status prompt. Item slots keep vanilla rendering inside dark terminal frames.
  两个机器的 GUI 改成 CC 高级计算机风格：黑色面板、琥珀/黄色文字、绿色进度条和状态提示符；物品槽保留原生渲染，外面套深色终端边框。
- **Burn ANY file, not just `.lua`**: put `.py`, `.txt`, `.java`, `.json`, binaries — anything — in the world's `lua-blueprints/` folder and burn it. Files are transferred and written as raw bytes, so binary data is not corrupted. Text previews show control bytes as dots.
  可以刻录**任意文件**，不再限于 `.lua`：把 `.py`/`.txt`/`.java`/`.json` 甚至二进制文件放进世界的 `lua-blueprints/` 文件夹都能刻。文件按原始字节传输和写入，二进制不会被破坏；文本预览把控制字符显示成点。
- **py2lua interactive UI** (separate program, in [`py2lua/`](../py2lua/)): run `py2lua ui` on an Advanced Computer with a monitor to browse files on a floppy, click to select a `.py`, click CONVERT to produce `.lua` — no more typing code line by line.
  py2lua 交互界面（独立程序，在 [`py2lua/`](../py2lua/)）：在带显示屏的高级计算机上运行 `py2lua ui`，即可用鼠标浏览软盘里的文件、点选 `.py`、点 CONVERT 生成 `.lua`，不用再一行行敲代码。

## How to install / 怎么装

1. Copy `lua-burner-mod-0.5.0.jar` into your modpack's `mods/` folder.
   把 `lua-burner-mod-0.5.0.jar` 丢进整合包的 `mods/` 文件夹。
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
sha256sum lua-burner-mod-0.5.0.jar
```

The output should match the SHA-256 above. If it doesn't, the download is corrupted — re-download it.

输出应和上面的 SHA-256 一致；不一致说明文件下坏了，重新下载即可。

## Building from source / 自己编译

If you'd rather compile it yourself (or want the latest unreleased changes), see the [root README](../README.md). The source build needs a JDK 21 and Gradle, plus the API jars in `libs/`.

想自己编译（或用最新的未发布改动），看根目录的 [README](../README.md)。源码编译需要 JDK 21 + Gradle，以及 `libs/` 里的 API jar。
