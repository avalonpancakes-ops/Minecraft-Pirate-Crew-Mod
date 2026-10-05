#!/usr/bin/env python3
"""
Original pixel art for the six Sundered Sea gear tiers: cutlasses, pickaxes, axes and armor icons.
Boss-tier gear (Krakenbone, Stormforged, Leviathan, Sovereign) is animated: a shine sweeps across the
metal and gems/glows pulse (written as animation strips with .mcmeta files).

Run: python3 tools/gen_gear_art.py   (writes textures + tools/gear_preview.png)
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pixelart import Canvas, hexc, mix, shade, strip, shine, pulse, sheet, twinkle  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "src/main/resources/assets/piratecrew/textures/item")

# ----------------------------------------------------------------------------- palettes
# blade: 5 steps dark->light; guard: 3; grip: 3; gem: 3; glow: 2; outline
TIERS = {
    "tidesteel": dict(
        blade=["#123c44", "#1f6670", "#3a9aa2", "#79d0cf", "#d8fbf6"],
        guard=["#6b4a12", "#b8862a", "#f0cc6a"], grip=["#3a2416", "#5e3a22", "#875a36"],
        gem=["#0d5a63", "#2fb3b5", "#a8fff4"], glow=["#3fd6d0", "#b8fff8"], outline="#0b1a1e"),
    "abyssal": dict(
        blade=["#120a26", "#2a1650", "#4b2c8c", "#7d58d0", "#c9b2ff"],
        guard=["#141019", "#2b2436", "#4a4060"], grip=["#1d1428", "#33234a", "#4d3670"],
        gem=["#5a1aa0", "#a65cff", "#f0d8ff"], glow=["#a46bff", "#ead6ff"], outline="#07040e"),
    "krakenbone": dict(
        blade=["#5a4a52", "#9c8a8a", "#cfc2b4", "#ece3d4", "#fffaf0"],
        guard=["#4a1838", "#86306a", "#c45a9c"], grip=["#3a1230", "#622050", "#8e3a74"],
        gem=["#7a1c4a", "#e05a9a", "#ffc2e0"], glow=["#ff7ab8", "#ffd8ec"], outline="#1c0c18"),
    "stormforged": dict(
        blade=["#0c1638", "#1c3270", "#2f56b0", "#5a8ef0", "#c8e2ff"],
        guard=["#2a3448", "#56688a", "#a4b6d4"], grip=["#141a2a", "#262f4a", "#3a4670"],
        gem=["#d8b000", "#ffe14a", "#fff8c0"], glow=["#ffe85a", "#ffffff"], outline="#060a18"),
    "leviathan": dict(
        blade=["#06302a", "#0f5a4a", "#1f8c72", "#4cc9a4", "#c4ffec"],
        guard=["#0a3a44", "#167080", "#3ab8c8"], grip=["#10241e", "#1c3e34", "#2c5e4e"],
        gem=["#0a7a8a", "#36e0e0", "#d0ffff"], glow=["#58ffd8", "#e0fff6"], outline="#03140f"),
    "sovereign": dict(
        blade=["#4a0a0e", "#8c1a1c", "#c8382a", "#f08a3c", "#ffe2a0"],
        guard=["#6a4410", "#c8901e", "#ffe070"], grip=["#2a0c0c", "#4a1414", "#701e1e"],
        gem=["#7a0012", "#ff2a44", "#ffc0c8"], glow=["#ffcc3a", "#fff6c8"], outline="#1a0606"),
}

ANIMATED = {"krakenbone", "stormforged", "leviathan", "sovereign"}
FRAMES = 24


def P(t):
    d = TIERS[t]
    pal = {k: [hexc(c) for c in v] for k, v in d.items() if k != "outline"}
    pal["outline"] = hexc(d["outline"])
    return pal


# ----------------------------------------------------------------------------- sprites
from sword_sprites import SWORDS  # noqa: E402
from tool_sprites import PICKAXE, AXE  # noqa: E402
from armor_sprites import HELMET, CHESTPLATE, LEGGINGS, BOOTS  # noqa: E402

# Trim colour per tier (armor bands, bindings): [mid, light]
TRIM = {
    "tidesteel": ["#b8862a", "#f0cc6a"], "abyssal": ["#8a4cf0", "#d6b4ff"], "krakenbone": ["#b84a8a", "#ff8ac4"],
    "stormforged": ["#d8b000", "#ffe85a"], "leviathan": ["#1aa8b8", "#7af0ff"], "sovereign": ["#c8901e", "#ffe070"],
}


def paint(tier, rows, haft=False):
    pal = P(tier)
    b, g, h = pal["blade"], pal["guard"], pal["grip"]
    tr = [hexc(c) for c in TRIM[tier]]
    key = {"1": b[0], "2": b[1], "3": b[2], "4": b[3], "5": b[4], "g": g[0], "G": g[1], "K": g[2],
           "h": h[0], "H": h[2], "w": h[1], "W": h[2], "e": pal["gem"][1], "x": pal["glow"][0], "y": pal["glow"][1],
           "T": tr[0], "U": tr[1]}
    c = Canvas()
    c.sprite(rows, key)
    metal, glow, gems = set(), set(), set()
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch in "12345":
                metal.add((x, y))
            elif ch in "xy":
                glow.add((x, y))
            elif ch == "e":
                gems.add((x, y))
            elif ch == "w" and x % 2 == 0:
                c.set(x, y, h[2])
    return c, pal, metal, glow, gems


def flourish(tier, c, pal, metal, glow, kind):
    """Tier patterns over the metal."""
    b = pal["blade"]
    for (x, y) in sorted(metal):
        col = c.get(x, y)
        if tier == "krakenbone" and (x + 2 * y) % 7 == 0 and col not in (b[4],):
            c.set(x, y, pal["glow"][0]); glow.add((x, y))      # suckers
        elif tier == "leviathan" and (2 * x + y) % 4 == 0 and col != b[4]:
            c.set(x, y, b[1])                                   # scales
        elif tier == "leviathan" and (2 * x + y) % 4 == 1 and col == b[2]:
            c.set(x, y, b[3])
        elif tier == "abyssal" and (x * 3 + y * 5) % 11 == 0 and col != b[4]:
            c.set(x, y, pal["glow"][0]); glow.add((x, y))      # glowing cracks
        elif tier == "sovereign" and col == b[4]:
            glow.add((x, y))


def armor_piece(tier, kind):
    rows = {"helmet": HELMET, "chestplate": CHESTPLATE, "leggings": LEGGINGS, "boots": BOOTS}[kind]
    rows = [r.replace("G", "T").replace("K", "U") for r in rows]
    c, pal, metal, glow, gems = paint(tier, rows)
    trim = {(x, y) for y, r in enumerate(rows) for x, ch in enumerate(r) if ch in "TU"}
    flourish(tier, c, pal, metal, glow, kind)
    if tier in ("abyssal", "stormforged", "leviathan", "sovereign"):
        glow |= trim
    if tier == "stormforged" and kind == "chestplate":
        for p in [(8, 5), (7, 6), (8, 6), (7, 7)]:
            c.set(*p, pal["gem"][1]); glow.add(p)
    if tier == "sovereign" and kind == "helmet":
        for p in [(5, 1), (8, 1), (11, 1)]:
            c.set(*p, pal["guard"][2]); glow.add(p)
    if tier == "tidesteel":
        for p in gems:
            c.set(*p, hexc(TRIM[tier][1]))
    c.outline(pal["outline"])
    return c, metal, glow, gems


def cutlass(tier):
    c, pal, metal, glow, gems = paint(tier, SWORDS[tier])
    flourish(tier, c, pal, metal, glow, "sword")
    c.outline(pal["outline"])
    return c, metal, glow, gems


def tool(tier, kind):
    rows = PICKAXE if kind == "pickaxe" else AXE
    c, pal, metal, glow, gems = paint(tier, rows)
    # binding in trim colour, plus a gem on the head for the higher tiers
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch in "K":
                c.set(x, y, hexc(TRIM[tier][1]))
                if tier != "tidesteel":
                    glow.add((x, y))
    if tier != "tidesteel":
        g = (9, 1) if kind == "pickaxe" else (6, 3)
        c.set(*g, pal["gem"][1]); gems.add(g)
    flourish(tier, c, pal, metal, glow, kind)
    c.outline(pal["outline"])
    return c, metal, glow, gems


def armor(tier, kind):
    return armor_piece(tier, kind)


# ----------------------------------------------------------------------------- output
def sparkle_spot(c, metal):
    """The metal pixel furthest toward the top-right (a blade tip, a pick's corner)."""
    if not metal:
        return None
    return max(metal, key=lambda p: p[0] - p[1])


def animate(tier, c, metal, glow, gems):
    if tier not in ANIMATED:
        return c.image(), None
    frames = []
    spot = sparkle_spot(c, metal)
    for i in range(FRAMES):
        t = i / FRAMES
        f = shine(c, metal, t, width=2.0, strength=1.6)
        f = pulse(f, glow, t, 0.8, 1.35)
        f = pulse(f, gems, (t + 0.5) % 1, 0.85, 1.5)
        if spot:
            f = twinkle(f, spot, t)
        frames.append(f.image())
    frametime = {"krakenbone": 2, "stormforged": 1, "leviathan": 2, "sovereign": 2}[tier]
    return strip(frames), {"animation": {"frametime": frametime, "interpolate": False}}


def save(name, img, meta):
    path = os.path.join(TEX, name + ".png")
    img.save(path)
    mpath = path + ".mcmeta"
    if meta:
        with open(mpath, "w") as f:
            json.dump(meta, f)
    elif os.path.exists(mpath):
        os.remove(mpath)


def main():
    previews = []
    for tier in TIERS:
        items = {
            "sword": cutlass(tier),
            "pickaxe": tool(tier, "pickaxe"),
            "axe": tool(tier, "axe"),
            "helmet": armor(tier, "helmet"),
            "chestplate": armor(tier, "chestplate"),
            "leggings": armor(tier, "leggings"),
            "boots": armor(tier, "boots"),
        }
        for kind, (c, metal, glow, gems) in items.items():
            img, meta = animate(tier, c, metal if kind != "sword" else metal, glow, gems)
            save(f"{tier}_{kind}", img, meta)
            previews.append(img)
    sheet(previews, scale=4, cols=7).save(os.path.join(ROOT, "tools/gear_preview.png"))
    print("drew", len(previews), "gear icons")


if __name__ == "__main__":
    main()
