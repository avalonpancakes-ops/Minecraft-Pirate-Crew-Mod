#!/usr/bin/env python3
"""The Kraken's texture for its own model (64x64): a mottled crimson-violet hide, huge amber eyes with
slit pupils, old scars, a ridged crest, and pale suckers down the inner face of every tentacle."""
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pixelart import hexc, mix  # noqa: E402
from PIL import Image  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/assets/piratecrew/textures/entity/kraken.png")

HIDE = [hexc(c) for c in ("#1e0614", "#3a0c26", "#5a1438", "#7a2048", "#9a3a62")]
SPOT = hexc("#c45a86")
SUCKER = [hexc("#d898ac"), hexc("#ffd8e0")]
EYE = [hexc("#2a1400"), hexc("#c87a10"), hexc("#ffc23a"), hexc("#fff0a0")]
SCAR = hexc("#d88aa0")


def faces(u, v, w, h, d):
    return {"up": (u + d, v, w, d), "down": (u + d + w, v, w, d), "west": (u, v + d, d, h),
            "north": (u + d, v + d, w, h), "east": (u + d + w, v + d, d, h), "south": (u + d + w + d, v + d, w, h)}


def main():
    rnd = random.Random(42)
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = im.load()

    def paint(rect, light_top=True, spots=True):
        x0, y0, w, h = rect
        for y in range(h):
            for x in range(w):
                k = y / max(1, h - 1)
                i = 3 - int(k * 2.6) if light_top else 2
                if rnd.random() < 0.08:
                    i += 1
                elif rnd.random() < 0.08:
                    i -= 1
                px[x0 + x, y0 + y] = HIDE[max(0, min(4, i))]
        if spots:
            for _ in range(max(1, w * h // 20)):
                sx, sy = x0 + rnd.randrange(w), y0 + rnd.randrange(h)
                px[sx, sy] = SPOT

    def box(u, v, w, h, d, spots=True):
        f = faces(u, v, w, h, d)
        for name, rect in f.items():
            paint(rect, name not in ("up", "down"), spots)
        return f

    mantle = box(0, 0, 12, 20, 12)
    cap = box(0, 32, 8, 6, 8)
    seg0 = box(48, 0, 3, 8, 3, False)
    seg1 = box(48, 11, 2, 8, 2, False)
    seg2 = box(56, 11, 1, 8, 1, False)
    arm = box(48, 21, 2, 9, 2, False)

    # eyes low on the front of the mantle
    fx, fy, fw, fh = mantle["north"]
    for ex in (fx + 1, fx + 7):
        ey = fy + 12
        for y in range(4):
            for x in range(4):
                px[ex + x, ey + y] = EYE[1]
        for x, y in ((0, 0), (3, 0), (0, 3), (3, 3)):
            px[ex + x, ey + y] = HIDE[1]
        px[ex + 1, ey + 1] = EYE[3]
        px[ex + 2, ey + 1] = EYE[2]
        px[ex + 1, ey + 2] = EYE[2]
        for y in range(4):
            px[ex + 2, ey + y] = EYE[0] if y != 1 else EYE[0]
        # heavy brow above each eye
        for x in range(-1, 5):
            if 0 <= ex + x - fx < fw:
                px[ex + x, ey - 1] = HIDE[0]
    # scars
    for i in range(7):
        px[fx + 2 + i, fy + 4 + i // 2] = SCAR
    wx, wy, ww, wh = mantle["west"]
    for i in range(5):
        px[wx + 3 + i, wy + 9 + (i % 2)] = SCAR
    # ridged crest on the cap
    for name in ("north", "south", "west", "east"):
        x0, y0, w, h = cap[name]
        for x in range(w):
            if x % 2 == 0:
                px[x0 + x, y0] = HIDE[4]
    # suckers down the inner (north) face of tentacles and arms
    for seg, step in ((seg0, 2), (seg1, 2), (seg2, 2), (arm, 2)):
        x0, y0, w, h = seg["north"]
        for y in range(1, h, step):
            for x in range(w):
                px[x0 + x, y0 + y] = SUCKER[1] if (x + y) % 2 == 0 else SUCKER[0]
    # arm tips: hooked clubs
    x0, y0, w, h = arm["north"]
    for x in range(w):
        px[x0 + x, y0 + h - 1] = hexc("#e8d8b0")
    im.save(OUT)
    im.resize((64 * 8, 64 * 8), Image.NEAREST).save(os.path.join(ROOT, "tools/kraken_preview.png"))
    print("kraken drawn")


if __name__ == "__main__":
    main()
