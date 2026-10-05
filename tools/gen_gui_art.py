#!/usr/bin/env python3
"""Tileable GUI textures for the pirate-themed screens: parchment, dark ship planks, and a rope strip."""
import math
import os
import random
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/assets/piratecrew/textures/gui")


def value_noise(w, h, cell, seed):
    rnd = random.Random(seed)
    gw, gh = w // cell, h // cell
    grid = [[rnd.random() for _ in range(gw)] for _ in range(gh)]

    def at(x, y):
        gx, gy = x / cell, y / cell
        x0, y0 = int(gx) % gw, int(gy) % gh
        x1, y1 = (x0 + 1) % gw, (y0 + 1) % gh
        tx, ty = gx - int(gx), gy - int(gy)
        tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
        a = grid[y0][x0] + (grid[y0][x1] - grid[y0][x0]) * tx
        b = grid[y1][x0] + (grid[y1][x1] - grid[y1][x0]) * tx
        return a + (b - a) * ty
    return at


def lerp(c1, c2, t):
    return tuple(int(a + (b - a) * t) for a, b in zip(c1, c2))


def parchment():
    n1, n2, n3 = value_noise(64, 64, 16, 1), value_noise(64, 64, 8, 2), value_noise(64, 64, 4, 3)
    rnd = random.Random(9)
    im = Image.new("RGBA", (64, 64))
    lo, hi = (205, 178, 128), (238, 220, 176)
    for y in range(64):
        for x in range(64):
            v = 0.55 * n1(x, y) + 0.3 * n2(x, y) + 0.15 * n3(x, y)
            c = lerp(lo, hi, min(1, max(0, (v - 0.2) / 0.6)))
            if rnd.random() < 0.02:
                c = lerp(c, (160, 128, 84), 0.4)       # fibres / flecks
            im.putpixel((x, y), c + (255,))
    return im


def wood():
    rnd = random.Random(4)
    im = Image.new("RGBA", (64, 64))
    base = [(58, 36, 20), (74, 47, 26), (88, 57, 32), (104, 69, 40)]
    for y in range(64):
        plank = y // 8
        row = y % 8
        off = (plank * 23) % 64
        for x in range(64):
            g = math.sin((x + off) * 0.21 + math.sin((x + off) * 0.05 + plank) * 2.0 + row * 0.6)
            k = 1 if g > 0.55 else 2 if g > -0.35 else 3 if g > -0.8 else 2
            if row == 0:
                k = 0                                  # gap between planks
            elif row == 1:
                k = min(3, k + 1)                      # top-edge highlight
            elif row == 7:
                k = max(1, k - 1)
            c = base[k]
            if (x + off) % 32 == 0 and row not in (0,):
                c = base[0]                            # plank seam
            if row == 4 and (x + off) % 32 == 3:
                c = (40, 26, 16)                       # nail
            im.putpixel((x, y), c + (255,))
    return im


def rope():
    """16x8 tile of twisted rope for dividers."""
    im = Image.new("RGBA", (16, 8), (0, 0, 0, 0))
    cols = [(92, 64, 32), (150, 112, 62), (196, 156, 96), (226, 194, 132)]
    for x in range(16):
        for y in range(2, 6):
            t = ((x + y * 2) % 8) / 8
            k = int(t * 4)
            im.putpixel((x, y), cols[k] + (255,))
        im.putpixel((x, 1), (60, 40, 20, 255))
        im.putpixel((x, 6), (60, 40, 20, 255))
    return im


def main():
    os.makedirs(OUT, exist_ok=True)
    parchment().save(os.path.join(OUT, "parchment.png"))
    wood().save(os.path.join(OUT, "wood.png"))
    rope().save(os.path.join(OUT, "rope.png"))
    prev = Image.new("RGBA", (64 * 2 + 16 + 8, 64), (0, 0, 0, 255))
    prev.paste(Image.open(os.path.join(OUT, "parchment.png")), (0, 0))
    prev.paste(Image.open(os.path.join(OUT, "wood.png")), (68, 0))
    prev.paste(Image.open(os.path.join(OUT, "rope.png")), (136, 0))
    prev.resize((prev.width * 4, prev.height * 4), Image.NEAREST).save(os.path.join(ROOT, "tools/gui_preview.png"))
    print("gui textures drawn")


if __name__ == "__main__":
    main()
