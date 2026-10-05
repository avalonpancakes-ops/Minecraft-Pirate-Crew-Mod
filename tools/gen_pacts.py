#!/usr/bin/env python3
"""Draws the Soul Pact scrolls (parchment with a coloured wax seal and the pact's sigil), plus models and names."""
import json
import os
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
A = os.path.join(ROOT, "src/main/resources/assets/piratecrew")

# id: (name, seal colour, sigil pixels relative to the seal centre)
PACTS = {
    "ember": ("Ember", 0xE8641E, [(0, -1), (-1, 0), (0, 0), (1, 0), (0, 1)]),
    "tempest": ("Tempest", 0x5AC8FF, [(1, -1), (0, 0), (1, 0), (0, 1)]),
    "frost": ("Frost", 0xBFEFFF, [(-1, -1), (1, -1), (0, 0), (-1, 1), (1, 1)]),
    "iron": ("Iron", 0x9AA0A6, [(-1, -1), (0, -1), (1, -1), (0, 0), (0, 1)]),
    "gale": ("Gale", 0x9CF5B0, [(-1, -1), (0, -1), (1, 0), (0, 1), (-1, 1)]),
    "shadow": ("Shadow", 0x5A2A82, [(0, -1), (-1, 0), (1, 0), (0, 1)]),
    "quake": ("Quake", 0x8A6A3A, [(-1, 0), (0, -1), (0, 1), (1, 0), (1, -1)]),
    "venom": ("Venom", 0x6ED23C, [(-1, -1), (1, -1), (0, 0), (0, 1)]),
    "gravity": ("Gravity", 0x3C3CC8, [(0, 0), (-1, 1), (0, 1), (1, 1)]),
    "blood": ("Blood", 0xB4141E, [(0, -1), (-1, 0), (0, 0), (1, 0), (0, 1), (0, -1)]),
}

PAPER = [(118, 86, 50), (196, 160, 104), (226, 198, 142), (240, 220, 170)]
ROLL = [(80, 54, 30), (150, 112, 66), (190, 150, 92)]


def rgb(c):
    return ((c >> 16) & 255, (c >> 8) & 255, c & 255)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def draw(seal):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()

    def put(x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            px[x, y] = c + (255,)

    # parchment sheet
    for y in range(3, 13):
        for x in range(3, 13):
            put(x, y, PAPER[2] if (x + y) % 5 else PAPER[3])
        put(3, y, PAPER[1])
        put(12, y, PAPER[1])
    # writing
    for y in (5, 7):
        for x in range(5, 11):
            if (x * 3 + y) % 4:
                put(x, y, PAPER[0])
    # rolled ends
    for y, (x0, x1) in ((2, (2, 13)), (13, (2, 13))):
        for x in range(x0, x1 + 1):
            put(x, y, ROLL[2])
            put(x, y - 1 if y == 2 else y + 1, ROLL[1])
        put(x0, y, ROLL[0])
        put(x1, y, ROLL[0])
    # wax seal with ribbon
    c = rgb(seal)
    cx, cy = 8, 10
    for dy in range(-2, 3):
        for dx in range(-2, 3):
            if dx * dx + dy * dy <= 5:
                put(cx + dx, cy + dy, shade(c, 0.7) if dx * dx + dy * dy >= 4 else c)
    put(cx - 1, cy + 3, shade(c, 0.6))
    put(cx + 1, cy + 3, shade(c, 0.6))
    put(cx - 2, cy + 4, shade(c, 0.5))
    put(cx + 2, cy + 4, shade(c, 0.5))
    return im, c, (cx, cy)


def main():
    lang_path = os.path.join(A, "lang/en_us.json")
    lang = json.load(open(lang_path))
    for pid, (name, seal, sigil) in PACTS.items():
        im, c, (cx, cy) = draw(seal)
        px = im.load()
        light = tuple(min(255, int(v * 0.4 + 255 * 0.6)) for v in c)
        for dx, dy in sigil:
            if dx * dx + dy * dy <= 2:
                px[cx + dx, cy + dy] = light + (255,)
        item = f"soul_pact_{pid}"
        # animate: the wax seal breathes with light and a mote of soul-fire drifts up from it
        import math
        frames = []
        seal_px = [(cx + dx, cy + dy) for dy in range(-2, 3) for dx in range(-2, 3) if dx * dx + dy * dy <= 5]
        for f in range(16):
            fr = im.copy()
            fp = fr.load()
            k = 0.5 - 0.5 * math.cos(f / 16 * 2 * math.pi)
            for (x, y) in seal_px:
                r0, g0, b0, a0 = px[x, y]
                fp[x, y] = (min(255, int(r0 + (255 - r0) * 0.35 * k)), min(255, int(g0 + (255 - g0) * 0.35 * k)),
                            min(255, int(b0 + (255 - b0) * 0.35 * k)), a0)
            my = cy - 3 - (f % 8)
            if f < 8 and my >= 0 and fp[cx + (1 if f % 4 < 2 else 0), my][3] == 0 or (f < 8 and my >= 0):
                fp[cx + (1 if f % 4 < 2 else 0), my] = light + (220,)
            frames.append(fr)
        anim = Image.new("RGBA", (16, 16 * len(frames)))
        for i, fr in enumerate(frames):
            anim.paste(fr, (0, i * 16))
        anim.save(os.path.join(A, f"textures/item/{item}.png"))
        json.dump({"animation": {"frametime": 2}}, open(os.path.join(A, f"textures/item/{item}.png.mcmeta"), "w"))
        with open(os.path.join(A, f"models/item/{item}.json"), "w") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": f"piratecrew:item/{item}"}}, f, indent=2)
        lang[f"item.piratecrew.{item}"] = f"{name} Soul Pact"
    lang["key.piratecrew.pact_power"] = "Use Soul Pact Power"
    json.dump(lang, open(lang_path, "w"), indent=2, ensure_ascii=False)

    sheet = Image.new("RGBA", (len(PACTS) * 36, 36), (60, 60, 70, 255))
    for i, pid in enumerate(PACTS):
        im = Image.open(os.path.join(A, f"textures/item/soul_pact_{pid}.png")).crop((0, 0, 16, 16)).resize((32, 32), Image.NEAREST)
        sheet.alpha_composite(im, (i * 36 + 2, 2))
    sheet.save(os.path.join(ROOT, "tools/pact_preview.png"))
    print("drew", len(PACTS), "pacts")


if __name__ == "__main__":
    main()
