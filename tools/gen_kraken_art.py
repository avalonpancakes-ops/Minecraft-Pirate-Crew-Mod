#!/usr/bin/env python3
"""The Kraken's own texture on the squid model's UV layout (64x32): a mottled crimson-violet hide,
huge amber eyes with slit pupils, old scars, and pale suckers down the inside of each tentacle."""
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
SUCKER = [hexc("#e8b0c0"), hexc("#ffd8e0")]
EYE = [hexc("#2a1400"), hexc("#c87a10"), hexc("#ffc23a"), hexc("#fff0a0")]


def main():
    rnd = random.Random(42)
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = im.load()

    def face(x0, y0, w, h, top_dark=False):
        for y in range(h):
            for x in range(w):
                k = y / max(1, h - 1)
                i = 3 - int(k * 2.5) if not top_dark else 1 + int(k * 2)
                i = max(0, min(4, i + (1 if rnd.random() < 0.08 else 0) - (1 if rnd.random() < 0.08 else 0)))
                px[x0 + x, y0 + y] = HIDE[i]
        # spots
        for _ in range(w * h // 18):
            sx, sy = x0 + rnd.randrange(w), y0 + rnd.randrange(h)
            px[sx, sy] = SPOT
            if sx + 1 < x0 + w and rnd.random() < 0.5:
                px[sx + 1, sy] = mix(SPOT, HIDE[3], 0.5)

    # body box (12x16x12 at 0,0)
    face(12, 0, 12, 12)          # top
    face(24, 0, 12, 12, True)    # bottom
    face(0, 12, 12, 16)          # right
    face(12, 12, 12, 16)         # front
    face(24, 12, 12, 16)         # left
    face(36, 12, 12, 16)         # back
    # eyes on the front, low on the mantle
    for ex in (13, 19):
        ey = 21
        for y in range(4):
            for x in range(4):
                px[ex + x, ey + y] = EYE[1]
        for x, y in ((0, 0), (3, 0), (0, 3), (3, 3)):
            px[ex + x, ey + y] = HIDE[1]
        px[ex + 1, ey + 1] = EYE[3]
        px[ex + 2, ey + 1] = EYE[2]
        px[ex + 1, ey + 2] = EYE[2]
        # slit pupil
        px[ex + 2, ey] = EYE[0]
        px[ex + 2, ey + 2] = EYE[0]
        px[ex + 2, ey + 3] = EYE[0]
        px[ex + 1, ey + 3] = EYE[1]
    # scars across the front and side
    for i in range(6):
        px[14 + i, 15 + i // 2] = hexc("#d88aa0")
    for i in range(5):
        px[3 + i, 18 + (i % 2)] = hexc("#d88aa0")
    # tentacles (2x18x2 at 48,0)
    for x in range(48, 56):
        for y in range(0, 20):
            if y < 2 and not (50 <= x < 54):
                continue
            k = y / 19
            px[x, y] = HIDE[max(0, 3 - int(k * 3))]
    for y in range(3, 20, 2):          # suckers down the inner face
        px[50, y] = SUCKER[0]
        px[51, y] = SUCKER[1] if y % 4 == 1 else SUCKER[0]
    im.save(OUT)
    im.resize((64 * 8, 32 * 8), Image.NEAREST).save(os.path.join(ROOT, "tools/kraken_preview.png"))
    print("kraken drawn")


if __name__ == "__main__":
    main()
