"""
Particle sprites for Soul Pact powers and boss effects: ember, frost, spark, wisp, blood, glyph.
Each is a few 8x8 frames (played over the particle's life via setSpriteFromAge). Drawn from scratch.
"""
import json
import math
import os
import random
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "piratecrew")
TEX = os.path.join(ROOT, "textures", "particle")
JSN = os.path.join(ROOT, "particles")


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def blank():
    return Image.new("RGBA", (8, 8), (0, 0, 0, 0))


def disc(img, cx, cy, r, col, soft=True):
    for y in range(8):
        for x in range(8):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if d <= r:
                a = col[3]
                if soft and d > r - 1:
                    a = int(a * (r - d))
                if a > 0:
                    old = img.getpixel((x, y))
                    if old[3] <= a:
                        img.putpixel((x, y), col[:3] + (a,))


def ember():
    cols = ["#FFF6C8", "#FFD45A", "#FF9A2A", "#E0501A", "#8A2A10"]
    out = []
    for i, r in enumerate([3.4, 3.0, 2.6, 2.1, 1.6, 1.1]):
        im = blank()
        k = min(i, 3)
        disc(im, 4, 4.2, r + 0.4, hexc(cols[k + 1], 180))
        disc(im, 4, 4.2, r - 0.3, hexc(cols[k], 255))
        if r > 2:
            disc(im, 4, 3.6, max(0.8, r - 1.8), hexc(cols[0]))
        out.append(im)
    return out


def frost():
    out = []
    pale, mid, deep = hexc("#F4FDFF"), hexc("#A8E6FF"), hexc("#4AA8E0")
    for i in range(4):
        im = blank()
        arm = 3 - (i // 2)
        for d in range(1, arm + 1):
            c = pale if d == 1 else (mid if d < arm else deep)
            for dx, dy in [(1, 0), (-1, 0), (0, 1), (0, -1)]:
                im.putpixel((4 + dx * d - (1 if dx < 0 else 0) * 0, 4 + dy * d), c) if 0 <= 4 + dx * d < 8 and 0 <= 4 + dy * d < 8 else None
        for dx, dy in [(1, 1), (-1, -1), (1, -1), (-1, 1)]:
            if i < 3:
                im.putpixel((4 + dx, 4 + dy), mid)
        if i < 2:
            for dx, dy in [(2, 2), (-2, -2), (2, -2), (-2, 2)]:
                im.putpixel((4 + dx, 4 + dy), deep)
        im.putpixel((4, 4), pale)
        out.append(im)
    return out


def spark():
    out = []
    white, cyan, blue = hexc("#FFFFFF"), hexc("#9FF4FF"), hexc("#4A7CFF")
    rng = random.Random(7)
    for i in range(4):
        im = blank()
        x, y = 4, 0
        while y < 8:
            im.putpixel((x, y), white if i < 2 else cyan)
            if x + 1 < 8 and i < 3:
                im.putpixel((x + 1, y), cyan if i < 2 else blue)
            y += 1
            x = max(1, min(6, x + rng.choice([-1, 0, 1])))
        if i == 0:
            for (px, py) in [(1, 2), (6, 5), (2, 6)]:
                im.putpixel((px, py), cyan)
        out.append(im)
    return out


def wisp():
    out = []
    for i in range(5):
        im = blank()
        a = 220 - i * 35
        disc(im, 4, 4, 3.6, hexc("#2A1240", int(a * 0.55)))
        disc(im, 4, 4, 2.6, hexc("#5B2E8A", a))
        disc(im, 3.6, 3.6, 1.4, hexc("#B48CFF", a))
        # curl of smoke that rotates
        ang = i * 0.9
        for t in range(3):
            px = int(4 + math.cos(ang + t * 0.6) * 3)
            py = int(4 + math.sin(ang + t * 0.6) * 3)
            if 0 <= px < 8 and 0 <= py < 8:
                im.putpixel((px, py), hexc("#8A5CC8", a))
        out.append(im)
    return out


def blood():
    out = []
    shapes = [
        ["...##...", "..####..", ".######.", ".####@#.", ".######.", "..####..", "...##...", "........"],
        ["........", "...##...", "..####..", "..##@#..", "..####..", "...##...", "........", "........"],
        ["........", "........", "...##...", "..#@##..", "...##...", "........", "........", "........"],
    ]
    pal = {"#": hexc("#9A0E1A"), "@": hexc("#FF5A5A")}
    for rows in shapes:
        im = blank()
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch in pal:
                    im.putpixel((x, y), pal[ch])
        # darker rim at the bottom of each drop
        for x in range(8):
            for y in range(7, -1, -1):
                if im.getpixel((x, y))[3]:
                    im.putpixel((x, y), hexc("#5A0610"))
                    break
        out.append(im)
    return out


GLYPHS = [
    ["..##..", ".#..#.", "######", ".#..#.", "..##..", "..##.."],   # anchor-ish
    ["#....#", ".#..#.", "..##..", "..##..", ".#..#.", "#....#"],   # crossed bones
    ["..#...", ".###..", "#.#.#.", "..#...", "..#.#.", "...#.."],   # rune
    ["######", "#....#", "#.##.#", "#.##.#", "#....#", "######"],   # seal
]


def glyph():
    out = []
    gold, glow = hexc("#FFE08A"), hexc("#7AF0D0", 150)
    for rows in GLYPHS:
        im = blank()
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch == "#":
                    for dx, dy in [(1, 0), (-1, 0), (0, 1), (0, -1)]:
                        px, py = x + 1 + dx, y + 1 + dy
                        if 0 <= px < 8 and 0 <= py < 8 and im.getpixel((px, py))[3] == 0:
                            im.putpixel((px, py), glow)
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch == "#":
                    im.putpixel((x + 1, y + 1), gold)
        out.append(im)
    return out


def main():
    os.makedirs(TEX, exist_ok=True)
    os.makedirs(JSN, exist_ok=True)
    for name, fn in [("ember", ember), ("frost", frost), ("spark", spark), ("wisp", wisp), ("blood", blood), ("glyph", glyph)]:
        frames = fn()
        names = []
        for i, im in enumerate(frames):
            n = f"{name}_{i}"
            im.save(os.path.join(TEX, n + ".png"))
            names.append("piratecrew:" + n)
        with open(os.path.join(JSN, name + ".json"), "w") as f:
            json.dump({"textures": names}, f, indent=2)
        print(name, len(frames))


if __name__ == "__main__":
    main()
