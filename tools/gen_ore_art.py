#!/usr/bin/env python3
"""
Sundered Sea ore textures. The stone and deepslate backgrounds are lifted from the mod's own ruby ore
textures (gem pixels painted over with neighbouring stone), then each ore gets its own deposit:
streaky tidesteel veins, glowing abyssal crystals (animated), and stormglass shards with sparks
(animated).
"""
import json
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pixelart import hexc, shade  # noqa: E402
from PIL import Image  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
B = os.path.join(ROOT, "src/main/resources/assets/piratecrew/textures/block")


def base_from(name):
    im = Image.open(os.path.join(B, name)).convert("RGBA").crop((0, 0, 16, 16))
    px = im.load()

    def gemmy(c):
        r, g, b, a = c
        return max(r, g, b) - min(r, g, b) > 28
    for _ in range(4):
        for y in range(16):
            for x in range(16):
                if gemmy(px[x, y]):
                    nb = [px[(x + dx) % 16, (y + dy) % 16] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if not gemmy(px[(x + dx) % 16, (y + dy) % 16])]
                    if nb:
                        px[x, y] = nb[(x * 7 + y) % len(nb)]
    return im


def deposit(im, cells, ramp, outline):
    """cells: list of (x, y, k) with k an index into ramp; outline drawn around the cluster."""
    px = im.load()
    filled = {(x, y) for x, y, _ in cells}
    for x, y, _ in cells:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            p = (x + dx, y + dy)
            if 0 <= p[0] < 16 and 0 <= p[1] < 16 and p not in filled:
                px[p] = outline
    for x, y, k in cells:
        px[x, y] = ramp[k]
    return im


VEIN = [(0, 0, 1), (1, 0, 2), (1, 1, 1), (2, 1, 3), (3, 1, 2), (3, 2, 1)]
CRYSTAL = [(1, 0, 3), (1, 1, 2), (0, 2, 1), (1, 2, 2), (2, 2, 3), (1, 3, 1), (2, 1, 2)]
SHARD = [(0, 0, 3), (0, 1, 2), (1, 1, 2), (1, 2, 1), (2, 2, 2)]


def place(pattern, spots, flip=None):
    cells = []
    for i, (ox, oy) in enumerate(spots):
        f = flip[i] if flip else False
        for x, y, k in pattern:
            cells.append(((ox + (3 - x if f else x)) % 16, (oy + y) % 16, k))
    return cells


def main():
    stone = base_from("ruby_ore.png")
    deep = base_from("deepslate_ruby_ore.png")
    tide = [hexc(c) for c in ("#123c44", "#2a7a84", "#5ab8b8", "#b8fff4")]
    tide_out = hexc("#0b2226")
    spots = [(1, 2), (9, 1), (4, 7), (11, 8), (2, 12), (9, 12)]
    flips = [False, True, True, False, True, False]
    deposit(stone.copy(), place(VEIN, spots, flips), tide, tide_out).save(os.path.join(B, "tidesteel_ore.png"))
    deposit(deep.copy(), place(VEIN, spots, flips), tide, tide_out).save(os.path.join(B, "deepslate_tidesteel_ore.png"))

    # abyssal: crystals on deepslate, glow pulsing (animated)
    aby = [hexc(c) for c in ("#2a1650", "#6a3ac0", "#a46bff", "#f0d8ff")]
    cells = place(CRYSTAL, [(2, 1), (10, 3), (5, 9), (12, 11)])
    frames = []
    for f in range(16):
        k = 0.5 - 0.5 * __import__("math").cos(f / 16 * 2 * 3.14159)
        ramp = [aby[0], aby[1], shade(aby[2], 0.85 + 0.35 * k), shade(aby[3], 0.8 + 0.25 * k)]
        frames.append(deposit(deep.copy(), cells, ramp, hexc("#0a0418")))
    strip(frames).save(os.path.join(B, "abyssal_ore.png"))
    json.dump({"animation": {"frametime": 3, "interpolate": True}}, open(os.path.join(B, "abyssal_ore.png.mcmeta"), "w"))

    # stormglass: blue shards with a spark that hops between them (animated)
    storm = [hexc(c) for c in ("#1c3270", "#2f56b0", "#7ab0ff", "#e0f0ff")]
    sh_spots = [(2, 2), (10, 1), (6, 7), (12, 9), (3, 12), (9, 13)]
    cells = place(SHARD, sh_spots)
    frames = []
    for f in range(12):
        im = deposit(stone.copy(), cells, storm, hexc("#0a1430"))
        sx, sy = sh_spots[f // 2]
        if f % 2 == 0:
            im.putpixel(((sx + 1) % 16, (sy + 1) % 16), hexc("#ffe85a"))
            im.putpixel(((sx + 2) % 16, (sy + 2) % 16), hexc("#fff8c0"))
        frames.append(im)
    strip(frames).save(os.path.join(B, "stormglass_ore.png"))
    json.dump({"animation": {"frametime": 3}}, open(os.path.join(B, "stormglass_ore.png.mcmeta"), "w"))

    names = ["tidesteel_ore", "deepslate_tidesteel_ore", "abyssal_ore", "stormglass_ore"]
    out = Image.new("RGBA", (20 * len(names), 16), (0, 0, 0, 255))
    for i, n in enumerate(names):
        out.paste(Image.open(os.path.join(B, n + ".png")).crop((0, 0, 16, 16)), (i * 20, 0))
    out.resize((out.width * 6, out.height * 6), Image.NEAREST).save(os.path.join(ROOT, "tools/ore_preview.png"))
    print("ores drawn")


def strip(frames):
    im = Image.new("RGBA", (16, 16 * len(frames)))
    for i, f in enumerate(frames):
        im.paste(f, (0, i * 16))
    return im


if __name__ == "__main__":
    main()
