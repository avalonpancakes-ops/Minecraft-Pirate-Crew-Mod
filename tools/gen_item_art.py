#!/usr/bin/env python3
"""
Original pixel art for Sundered Sea materials and boss items (ingots, shards, cores, scales, hearts,
badges, summon items, the Siren Conch). Boss drops and summon items are animated.

Run: python3 tools/gen_item_art.py   (writes textures + tools/item_preview.png)
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pixelart import Canvas, hexc, strip, shine, pulse, sheet, twinkle  # noqa: E402
import item_sprites as S  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "src/main/resources/assets/piratecrew/textures/item")

PAL = {
    "tidesteel": (["#123c44", "#1f6670", "#3a9aa2", "#79d0cf", "#d8fbf6"], ["#2a5a40", "#4a8a5a", "#8ac890"], ["#3fd6d0", "#b8fff8"], "#0b1a1e"),
    "rawtide":   (["#3a3a40", "#5a5a62", "#7c7c86", "#a0a0aa", "#c8c8d0"], ["#1f6670", "#3a9aa2", "#9af0ee"], ["#3fd6d0", "#b8fff8"], "#141418"),
    "abyssal":   (["#120a26", "#2a1650", "#4b2c8c", "#7d58d0", "#c9b2ff"], ["#3a1a6a", "#7a3ad0", "#c890ff"], ["#b07aff", "#f0e0ff"], "#07040e"),
    "kraken":    (["#5a4a52", "#9c8a8a", "#cfc2b4", "#ece3d4", "#fffaf0"], ["#86306a", "#c45a9c", "#ff9ccc"], ["#ff7ab8", "#ffd8ec"], "#1c0c18"),
    "storm":     (["#0c1638", "#1c3270", "#2f56b0", "#5a8ef0", "#c8e2ff"], ["#8a7000", "#d8b000", "#ffe85a"], ["#ffe85a", "#ffffff"], "#060a18"),
    "leviathan": (["#06302a", "#0f5a4a", "#1f8c72", "#4cc9a4", "#c4ffec"], ["#0a5a66", "#1aa8b8", "#7af0ff"], ["#58ffd8", "#e0fff6"], "#03140f"),
    "sovereign": (["#4a0a0e", "#8c1a1c", "#c8382a", "#f08a3c", "#ffe2a0"], ["#8a5a10", "#d8a02a", "#ffe070"], ["#ffd84a", "#fff8d0"], "#1a0606"),
    "gold":      (["#6a4410", "#a8701a", "#d8a02a", "#f0cc5a", "#fff2b0"], ["#0e4a6a", "#1a7aa0", "#5ac8e8"], ["#fff2b0", "#ffffff"], "#2a1806"),
    "navy":      (["#101a2a", "#1c2c48", "#2c4470", "#4a6aa0", "#8aa8d8"], ["#6a4a12", "#b8862a", "#f0cc6a"], ["#ffe08a", "#ffffff"], "#060a12"),
    "flare":     (["#4a1010", "#8a1c1c", "#c83030", "#e86040", "#ffb080"], ["#3a2a1a", "#6a5030", "#9a7a50"], ["#ffcc40", "#fff6c0"], "#1a0606"),
    "parch":     (["#7a5a34", "#a8844e", "#d0b07a", "#e8d2a0", "#f8ecc8"], ["#6a1010", "#a01c1c", "#e04040"], ["#d02030", "#ff8090"], "#2a1a0c"),
    "bonehorn":  (["#4a3a2a", "#7a6248", "#a88c68", "#d0b890", "#f0e4c8"], ["#0a5a66", "#1aa8b8", "#7af0ff"], ["#58ffd8", "#e0fff6"], "#1a120a"),
    "conch":     (["#7a2a4a", "#b04a6a", "#e07a90", "#f8b0b8", "#fff0ec"], ["#1a7a80", "#3ac0c0", "#a0fff4"], ["#a0fff4", "#ffffff"], "#2a0a18"),
}

# item: (sprite, palette, animated?, extra)
ITEMS = {
    "raw_tidesteel": (S.RAW, "rawtide", False),
    "tidesteel_ingot": (S.INGOT, "tidesteel", False),
    "abyssal_shard": (S.SHARD, "abyssal", False),
    "abyssal_ingot": (S.INGOT, "abyssal", False),
    "kraken_bone": (S.BONE, "kraken", True),
    "krakenbone_ingot": (S.INGOT, "kraken", True),
    "stormglass_shard": (S.SHARD, "storm", False),
    "storm_core": (S.ORB, "storm", True),
    "stormforged_ingot": (S.INGOT, "storm", True),
    "leviathan_scale": (S.SCALE, "leviathan", True),
    "leviathan_ingot": (S.INGOT, "leviathan", True),
    "sovereign_heart": (S.HEART, "sovereign", True),
    "sovereign_ingot": (S.INGOT, "sovereign", True),
    "marine_badge": (S.BADGE, "navy", False),
    "commodore_insignia": (S.INSIGNIA, "gold", True),
    "signal_flare": (S.FLARE, "flare", True),
    "kraken_lure": (S.LURE, "bonehorn", True),
    "storm_sigil": (S.SIGIL, "storm", True),
    "leviathan_horn": (S.HORN, "bonehorn", True),
    "admirals_warrant": (S.WARRANT, "parch", True),
    "siren_conch": (S.CONCH, "conch", True),
}

FRAMES = 24


def draw(name, rows, palname):
    main, acc, glow, outline = PAL[palname]
    m = [hexc(c) for c in main]
    a = [hexc(c) for c in acc]
    g = [hexc(c) for c in glow]
    key = {"1": m[0], "2": m[1], "3": m[2], "4": m[3], "5": m[4], "a": a[0], "b": a[1], "c": a[2],
           "x": g[0], "y": g[1], "w": (255, 255, 255, 255)}
    c = Canvas()
    c.sprite(rows, key)
    metal = {(x, y) for y, r in enumerate(rows) for x, ch in enumerate(r) if ch in "12345"}
    glow_px = {(x, y) for y, r in enumerate(rows) for x, ch in enumerate(r) if ch in "xy"}
    acc_px = {(x, y) for y, r in enumerate(rows) for x, ch in enumerate(r) if ch in "abc"}
    # item-specific details
    if name == "stormforged_ingot":
        for p in [(9, 6), (8, 7), (9, 7), (8, 8)]:
            c.set(*p, g[0]); glow_px.add(p)
    if name == "abyssal_ingot":
        for p in [(5, 8), (6, 8), (10, 9), (11, 10)]:
            c.set(*p, g[0]); glow_px.add(p)
    if name == "krakenbone_ingot":
        for p in [(4, 8), (8, 9), (12, 9), (6, 10)]:
            c.set(*p, a[1]); glow_px.add(p)
    if name == "leviathan_ingot":
        for (x, y) in list(metal):
            if (2 * x + y) % 4 == 0 and c.get(x, y) != m[4]:
                c.set(x, y, m[1])
    if name == "sovereign_ingot":
        for p in [(7, 7), (8, 7), (9, 8)]:
            c.set(*p, a[2]); glow_px.add(p)
    if name == "tidesteel_ingot":
        for p in [(6, 8), (10, 9)]:
            c.set(*p, m[4])
    if name == "stormglass_shard":
        for p in [(6, 7), (7, 8), (8, 7), (9, 9)]:
            c.set(*p, g[0]); glow_px.add(p)
    c.outline(hexc(outline))
    return c, metal, glow_px, acc_px


def animate(name, c, metal, glow, acc):
    frames = []
    spot = max(metal, key=lambda p: p[0] - p[1]) if metal else None
    for i in range(FRAMES):
        t = i / FRAMES
        f = shine(c, metal, t, width=2.0, strength=1.5)
        f = pulse(f, glow, t, 0.7, 1.4)
        if name in ("signal_flare", "admirals_warrant", "commodore_insignia"):
            f = pulse(f, acc, (t + 0.5) % 1, 0.85, 1.25)
        if spot and name not in ("signal_flare", "kraken_lure"):
            f = twinkle(f, spot, t)
        if name == "signal_flare":
            # sparks drifting up from the fuse
            import random
            rnd = random.Random(i)
            for _ in range(2):
                x, y = 10 + rnd.randint(-2, 2), 4 - rnd.randint(0, 4)
                if f.get(x, y) is None:
                    f.set(x, y, hexc("#ffe080"))
        frames.append(f.image())
    return strip(frames), {"animation": {"frametime": 2}}


def main():
    previews = []
    for name, (rows, palname, animated) in ITEMS.items():
        c, metal, glow, acc = draw(name, rows, palname)
        if animated:
            img, meta = animate(name, c, metal, glow, acc)
        else:
            img, meta = c.image(), None
        path = os.path.join(TEX, name + ".png")
        img.save(path)
        if meta:
            json.dump(meta, open(path + ".mcmeta", "w"))
        elif os.path.exists(path + ".mcmeta"):
            os.remove(path + ".mcmeta")
        previews.append(img)
    sheet(previews, scale=5, cols=7).save(os.path.join(ROOT, "tools/item_preview.png"))
    print("drew", len(previews), "items")


if __name__ == "__main__":
    main()
