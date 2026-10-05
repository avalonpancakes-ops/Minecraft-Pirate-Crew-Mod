#!/usr/bin/env python3
"""The Leviathan serpent's texture (128x64): deep teal scales over a pale belly, glowing aqua eyes,
bone-white fangs along the jaw, ivory horns and translucent-looking fins."""
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pixelart import hexc, mix  # noqa: E402
from PIL import Image  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/assets/piratecrew/textures/entity/leviathan.png")

SCALE = [hexc(c) for c in ("#042820", "#0a4a3a", "#127058", "#1f9a78", "#4cc9a4")]
BELLY = [hexc(c) for c in ("#8ab8a0", "#b8dcc4", "#d8f0e0")]
FIN = [hexc(c) for c in ("#0e6a7a", "#2ab0c0", "#8ae8f0")]
EYE = [hexc("#004a4a"), hexc("#3affe0"), hexc("#e0fff8")]
FANG = hexc("#f4f0dc")
HORN = [hexc("#a89878"), hexc("#e8dcc0")]


def faces(u, v, w, h, d):
    return {"up": (u + d, v, w, d), "down": (u + d + w, v, w, d), "west": (u, v + d, d, h),
            "north": (u + d, v + d, w, h), "east": (u + d + w, v + d, d, h), "south": (u + d + w + d, v + d, w, h)}


def main():
    rnd = random.Random(7)
    im = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    px = im.load()

    def scales(rect, belly_rows=0):
        x0, y0, w, h = rect
        for y in range(h):
            for x in range(w):
                if belly_rows and y >= h - belly_rows:
                    px[x0 + x, y0 + y] = BELLY[1] if (x + y) % 3 else BELLY[0]
                    continue
                # overlapping scale rows: a lighter crescent at the top of each scale
                row, col = y // 2, (x + (y // 2) % 2) // 2
                k = 3 if y % 2 == 0 and (x + row) % 2 == 0 else 2
                if rnd.random() < 0.06:
                    k = 1
                px[x0 + x, y0 + y] = SCALE[k]

    def belly(rect):
        x0, y0, w, h = rect
        for y in range(h):
            for x in range(w):
                px[x0 + x, y0 + y] = BELLY[2] if y % 2 == 0 else BELLY[1]

    def box(u, v, w, h, d, belly_side=True):
        f = faces(u, v, w, h, d)
        scales(f["up"])
        belly(f["down"]) if belly_side else scales(f["down"])
        for side in ("west", "east", "north", "south"):
            scales(f[side], belly_rows=max(1, f[side][3] // 4) if belly_side else 0)
        return f

    head = box(0, 0, 10, 9, 12)
    jaw = box(0, 21, 8, 3, 11)
    for (u, v), s in zip(((0, 36), (40, 21), (40, 40), (76, 0), (76, 16), (76, 32)), (9, 8, 7, 6, 5, 4)):
        box(u, v, s, s, 10)
    # eyes near the front of each side of the head
    for side in ("west", "east"):
        x0, y0, w, h = head[side]
        ex = x0 + (1 if side == "east" else w - 4)
        for x in range(3):
            px[ex + x, y0 + 2] = EYE[1]
            px[ex + x, y0 + 3] = EYE[0]
        px[ex + 1, y0 + 2] = EYE[2]
        for x in range(-1, 4):
            px[ex + x, y0 + 1] = SCALE[0]
    # snout front: nostrils
    x0, y0, w, h = head["north"]
    px[x0 + 3, y0 + 2] = SCALE[0]
    px[x0 + 6, y0 + 2] = SCALE[0]
    # fangs along the upper jaw line (bottom row of head sides/front) and lower jaw top edge
    for side in ("west", "east", "north"):
        x0, y0, w, h = head[side]
        for x in range(0, w, 2):
            px[x0 + x, y0 + h - 1] = FANG
        x0, y0, w, h = jaw[side]
        for x in range(1, w, 2):
            px[x0 + x, y0] = FANG
    # horns
    for name, (x0, y0, w, h) in faces(44, 0, 1, 1, 6).items():
        for y in range(h):
            for x in range(w):
                px[x0 + x, y0 + y] = HORN[1] if (x + y) % 2 else HORN[0]
    # fins
    for (u, v, w, h, d) in ((110, 0, 1, 3, 6), (110, 10, 1, 8, 6)):
        for name, (x0, y0, fw, fh) in faces(u, v, w, h, d).items():
            for y in range(fh):
                for x in range(fw):
                    px[x0 + x, y0 + y] = FIN[2] if y == 0 else FIN[1] if (x + y) % 3 else FIN[0]
    im.save(OUT)
    im.resize((128 * 4, 64 * 4), Image.NEAREST).save(os.path.join(ROOT, "tools/leviathan_preview.png"))
    print("leviathan drawn")


if __name__ == "__main__":
    main()
