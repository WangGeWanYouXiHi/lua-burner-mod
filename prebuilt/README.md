# Prebuilt jar / 预编译版本

Can't build the mod because Gradle or the dependencies won't download? Grab the ready-made jar here — no compiler, no Java setup, no Gradle required.

编译工具（Gradle / JDK / 依赖）下不下来？直接用这里的成品 jar，不用编译、不用装 Java、不用碰 Gradle。

## File / 文件

| File | Version | Size |
| --- | --- | --- |
| `lua-burner-mod-1.0.0.jar` | 0.2 (was alpha-0.1) | 184 KB |

SHA-256: `1c01d56635369e7e28eea1b3e2caaed20557e05b5ab05ad74a4c73da56e4ed6c`

## How to install / 怎么装

1. Copy `lua-burner-mod-1.0.0.jar` into your modpack's `mods/` folder.
   把 `lua-burner-mod-1.0.0.jar` 丢进整合包的 `mods/` 文件夹。
2. Make sure the required dependencies below are installed.
   确保下面的必需依赖都装好了。
3. Launch the game. Done.
   启动游戏，收工。

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
sha256sum lua-burner-mod-1.0.0.jar
```

The output should match the SHA-256 above. If it doesn't, the download is corrupted — re-download it.

输出应和上面的 SHA-256 一致；不一致说明文件下坏了，重新下载即可。

## Building from source / 自己编译

If you'd rather compile it yourself (or want the latest unreleased changes), see the [root README](../README.md). The source build needs a JDK 21 and Gradle, plus the API jars in `libs/`.

想自己编译（或用最新的未发布改动），看根目录的 [README](../README.md)。源码编译需要 JDK 21 + Gradle，以及 `libs/` 里的 API jar。
