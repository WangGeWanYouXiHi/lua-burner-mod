"""Generate all PNG textures and item models for lua-burner-mod using pure stdlib (zlib+struct)."""
import struct, zlib, pathlib

ROOT = pathlib.Path(__file__).parent
RES = ROOT / "src/main/resources/assets/luaburner"
TEX_B = RES / "textures/block"
TEX_I = RES / "textures/item"
MOD_I = RES / "models/item"
for d in (TEX_B, TEX_I, MOD_I):
    d.mkdir(parents=True, exist_ok=True)


def write_png(path, w, h, pixel_fn):
    """pixel_fn(x,y) -> (r,g,b,a)"""
    raw = bytearray()
    for y in range(h):
        raw.append(0)  # filter type 0
        for x in range(w):
            r, g, b, a = pixel_fn(x, y)
            raw += bytes((r & 255, g & 255, b & 255, a & 255))

    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    path.write_bytes(png)


# ---- Block textures 16x16 ----
def front(x, y):
    # dark terminal screen with green scanlines
    if 2 <= x <= 13 and 2 <= y <= 12:
        if y in (3, 5, 7, 9, 11) and 3 <= x <= 11:
            return (60, 220, 90, 255)  # glowing text line
        return (10, 30, 16, 255)
    if y >= 14 and 2 <= x <= 13:
        return (90, 95, 105, 255)  # keyboard deck
    return (55, 58, 65, 255)  # casing


write_png(TEX_B / "burner_front.png", 16, 16, front)
write_png(TEX_B / "burner_back.png", 16, 16,
          lambda x, y: (45, 48, 55, 255) if 1 <= x <= 14 and 1 <= y <= 14 else (30, 32, 38, 255))
write_png(TEX_B / "burner_side.png", 16, 16,
          lambda x, y: (50, 53, 60, 255) if 1 <= x <= 14 and 1 <= y <= 14 else (30, 32, 38, 255))
write_png(TEX_B / "burner_top.png", 16, 16,
          lambda x, y: (60, 63, 70, 255) if 1 <= x <= 14 and 1 <= y <= 14 else (30, 32, 38, 255))
write_png(TEX_B / "burner_bottom.png", 16, 16, lambda x, y: (35, 37, 42, 255))

# ---- Floppy disk item texture 16x16 ----
def floppy(x, y):
    if 2 <= x <= 13 and 1 <= y <= 14:
        if 3 <= x <= 12 and 7 <= y <= 12:
            return (220, 220, 225, 255)  # label
        if 4 <= x <= 11 and 2 <= y <= 5:
            return (150, 155, 165, 255)  # metal shutter
        return (40, 42, 50, 255)  # body
    return (0, 0, 0, 0)
write_png(TEX_I / "floppy_disk.png", 16, 16, floppy)

# ---- Hard drive item textures: 10 tiers x 17 colors ----
COLORS = {
    "default": (120, 124, 132), "white": (245, 245, 245), "orange": (255, 120, 31),
    "magenta": (255, 60, 240), "light_blue": (90, 215, 240), "yellow": (240, 222, 30),
    "lime": (128, 199, 31), "pink": (240, 56, 129), "gray": (71, 79, 82),
    "light_gray": (157, 157, 151), "cyan": (22, 156, 157), "purple": (137, 50, 184),
    "blue": (60, 68, 170), "brown": (131, 84, 50), "green": (22, 128, 12),
    "red": (176, 46, 38), "black": (29, 29, 33),
}


def hdd_pixel(base):
    br, bg, bb = base

    def fn(x, y):
        if 1 <= x <= 14 and 3 <= y <= 12:
            if 2 <= x <= 13 and 4 <= y <= 11:
                # LED indicator at right
                if x >= 12 and y in (5, 6):
                    return (60, 240, 90, 255)
                # capacity stripes at bottom
                if y == 10 and 3 <= x <= 11:
                    return (max(0, br - 40), max(0, bg - 40), max(0, bb - 40), 255)
                return (br, bg, bb, 255)
            return (max(0, br - 60), max(0, bg - 60), max(0, bb - 60), 255)
        return (0, 0, 0, 0)
    return fn


for g in range(1, 11):
    for cname, rgb in COLORS.items():
        name = f"hard_drive_{g}gb_{cname}"
        write_png(TEX_I / f"{name}.png", 16, 16, hdd_pixel(rgb))
        # item model json
        (MOD_I / f"{name}.json").write_text(
            '{"parent":"minecraft:item/generated","textures":{"layer0":"luaburner:item/%s"}}' % name,
            encoding="utf-8")

# floppy item model + burner item model
(MOD_I / "floppy_disk.json").write_text(
    '{"parent":"minecraft:item/generated","textures":{"layer0":"luaburner:item/floppy_disk"}}',
    encoding="utf-8")

# ---- lang entries for 170 hard drives ----
lang = {
    "block.luaburner.burner": "Lua Blueprint Burner",
    "item.luaburner.floppy_disk": "Lua Floppy Disk (1.44 MB)",
    "item.luaburner.burner": "Lua Blueprint Burner",
}
DISPLAY = {"default": "", "white": "White ", "orange": "Orange ", "magenta": "Magenta ",
           "light_blue": "Light Blue ", "yellow": "Yellow ", "lime": "Lime ", "pink": "Pink ",
           "gray": "Gray ", "light_gray": "Light Gray ", "cyan": "Cyan ", "purple": "Purple ",
           "blue": "Blue ", "brown": "Brown ", "green": "Green ", "red": "Red ", "black": "Black "}
for g in range(1, 11):
    for cname, prefix in DISPLAY.items():
        key = f"hard_drive_{g}gb_{cname}"
        lang[f"item.luaburner.{key}"] = f"{prefix}{g} GB Hard Drive"
import json
(RES / "lang/en_us.json").write_text(
    json.dumps(lang, indent=2, ensure_ascii=False), encoding="utf-8")

print("Generated:", len(list(TEX_I.glob('*.png'))), "item textures,",
      len(lang), "lang entries")
