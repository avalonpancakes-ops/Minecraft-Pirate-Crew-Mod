#!/usr/bin/env python3
"""
Worn armor textures (layer_1: helmet, chestplate, boots; layer_2: leggings) for the six Sundered Sea
tiers, drawn from scratch on the standard 64x32 humanoid armor layout: bevelled plates, trims in the
tier's metal, rivets and gems, and a motif per tier (cracks, suckers, lightning, scales, crowns).

Run: python3 tools/gen_armor_art.py
"""
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pixelart import hexc, shade  # noqa: E402
from gen_gear_art import TIERS, TRIM  # noqa: E402
from PIL import Image  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/assets/piratecrew/textures/models/armor")


class Layer:
    def __init__(self, tier, seed):
        d = TIERS[tier]
        self.tier = tier
        self.m = [hexc(c) for c in d["blade"]]
        self.trim = [hexc(c) for c in TRIM[tier]]
        self.gem = [hexc(c) for c in d["gem"]]
        self.glow = [hexc(c) for c in d["glow"]]
        self.dark = hexc(d["outline"])
        self.im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
        self.rnd = random.Random(seed)

    def px(self, x, y, c):
        if 0 <= x < 64 and 0 <= y < 32:
            self.im.putpixel((x, y), c)

    def get(self, x, y):
        return self.im.getpixel((x, y))

    def plate(self, x0, y0, w, h, rows=None, light=True):
        """A clean bevelled plate: light top edge, gentle top-to-bottom gradient, dark lower/right rim."""
        rows = list(rows if rows is not None else range(h))
        top, bot = min(rows), max(rows)
        span = max(1, bot - top)
        for yy in rows:
            k = (yy - top) / span
            for xx in range(w):
                c = self.m[3] if k < 0.25 else self.m[2] if k < 0.75 else self.m[1]
                if xx == 0:
                    c = self.m[3]
                if yy == top:
                    c = self.m[4] if light else self.m[3]
                if xx == w - 1 or yy == bot:
                    c = self.m[1] if yy != bot else self.m[0]
                self.px(x0 + xx, y0 + yy, c)

    def band(self, x0, y, w, rivets=True):
        for xx in range(w):
            c = self.trim[1] if xx % 3 == 1 else self.trim[0]
            if rivets and self.tier == "tidesteel" and xx % 3 == 1:
                c = shade(self.trim[1], 1.2)
            self.px(x0 + xx, y, c)

    def vband(self, x, y0, h):
        for yy in range(h):
            self.px(x, y0 + yy, self.trim[0] if yy % 2 else self.trim[1])

    def motif(self, x0, y0, w, h, rows=None):
        rows = list(rows if rows is not None else range(h))
        for yy in rows:
            for xx in range(w):
                x, y = x0 + xx, y0 + yy
                if self.get(x, y)[3] == 0:
                    continue
                t = self.tier
                if xx in (0, w - 1) or yy in (rows[0], rows[-1]):
                    continue
                if t == "leviathan" and yy % 2 == 0 and (xx + yy // 2) % 2 == 0:
                    self.px(x, y, self.m[1])                       # overlapping scale rows
                elif t == "krakenbone" and (xx * 3 + yy * 5) % 11 == 0:
                    self.px(x, y, self.glow[0])                    # suckers
                elif t == "abyssal" and (xx - yy) % 7 == 0 and yy % 3 != 0:
                    self.px(x, y, self.glow[0])                    # glowing seams


def faces(x, y, w, h, d):
    """UV rectangles of a box at (x, y) with size w,h,d: dict of face -> (x0, y0, fw, fh)."""
    return {
        "top": (x + d, y, w, d), "bottom": (x + d + w, y, w, d),
        "right": (x, y + d, d, h), "front": (x + d, y + d, w, h),
        "left": (x + d + w, y + d, d, h), "back": (x + d + w + d, y + d, w, h),
    }


def helmet(L):
    f = faces(0, 0, 8, 8, 8)
    for name in ("top", "right", "left", "back"):
        L.plate(*f[name])
    L.motif(*f["top"])
    x0, y0, w, h = f["front"]
    # brow and cheek guards, open face
    L.plate(x0, y0, w, h, rows=range(0, 3))
    for yy in range(3, 8):
        for xx in (0, 7):
            L.px(x0 + xx, y0 + yy, L.m[2] if yy < 7 else L.m[1])
    L.px(x0 + 3, y0 + 3, L.m[2]); L.px(x0 + 4, y0 + 3, L.m[2]); L.px(x0 + 3, y0 + 4, L.m[1]); L.px(x0 + 4, y0 + 4, L.m[1])
    L.band(x0, y0 + 2, 8)
    L.px(x0 + 3, y0 + 1, L.gem[1]); L.px(x0 + 4, y0 + 1, L.gem[2])
    for name in ("right", "left", "back"):
        bx, by, bw, bh = f[name]
        L.band(bx, by + 2, bw)
    tx, ty, tw, th = f["top"]
    for yy in range(th):
        L.px(tx + 3, ty + yy, L.trim[0]); L.px(tx + 4, ty + yy, L.trim[1])   # crest ridge
    if L.tier == "sovereign":
        for xx in (1, 3, 4, 6):
            L.px(x0 + xx, y0, L.trim[1])
    if L.tier == "stormforged":
        for (xx, yy) in ((1, 0), (2, 1), (5, 0), (6, 1)):
            L.px(x0 + xx, y0 + yy, L.glow[0])


def chest(L):
    f = faces(16, 16, 8, 12, 4)
    for name in ("top", "bottom", "right", "left", "back", "front"):
        L.plate(*f[name])
    L.motif(*f["front"])
    L.motif(*f["back"])
    x0, y0, w, h = f["front"]
    L.band(x0, y0, w)                     # collar
    L.band(x0, y0 + 10, w)                # belt
    L.px(x0 + 3, y0 + 10, L.trim[1]); L.px(x0 + 4, y0 + 10, L.trim[1])
    for yy in range(1, 10):               # central ridge
        L.px(x0 + 3, y0 + yy, L.m[3]); L.px(x0 + 4, y0 + yy, L.m[1])
    emblem = {
        "tidesteel": [(3, 4), (4, 4), (3, 5), (4, 5)],
        "abyssal": [(3, 3), (4, 4), (3, 5), (4, 6)],
        "krakenbone": [(2, 4), (5, 4), (3, 5), (4, 5), (3, 6), (4, 6)],
        "stormforged": [(4, 2), (3, 3), (4, 3), (3, 4), (2, 5), (3, 5), (4, 5), (4, 6), (3, 7)],
        "leviathan": [(3, 3), (4, 3), (2, 4), (5, 4), (3, 5), (4, 5)],
        "sovereign": [(2, 3), (4, 3), (6, 3), (2, 4), (3, 4), (4, 4), (5, 4), (6, 4), (3, 5), (4, 5), (5, 5)],
    }[L.tier]
    for (xx, yy) in emblem:
        L.px(x0 + xx, y0 + yy, L.gem[1] if L.tier not in ("sovereign", "stormforged") else L.trim[1])
    if L.tier == "sovereign":
        L.px(x0 + 4, y0 + 6, L.gem[1])
    bx, by, bw, bh = f["back"]
    L.band(bx, by, bw); L.band(bx, by + 10, bw)
    for name in ("right", "left"):
        sx, sy, sw, sh = f[name]
        L.band(sx, sy + 10, sw)
    # arms (pauldrons): right arm box at (40,16)
    a = faces(40, 16, 4, 12, 4)
    for name in ("top", "bottom", "right", "front", "left", "back"):
        L.plate(*a[name])
    for name in ("right", "front", "left", "back"):
        ax, ay, aw, ah = a[name]
        L.band(ax, ay + 4, aw)            # pauldron edge
        L.band(ax, ay + 10, aw)           # cuff
        for xx in range(aw):
            L.px(ax + xx, ay, L.m[4] if xx == 0 else L.m[3])


def boots(L):
    f = faces(0, 16, 4, 12, 4)
    for name in ("right", "front", "left", "back"):
        x0, y0, w, h = f[name]
        L.plate(x0, y0, w, h, rows=range(6, 12))
        L.band(x0, y0 + 6, w)
    x0, y0, w, h = f["bottom"]
    L.plate(x0, y0, w, h)
    fx, fy, fw, fh = f["front"]
    L.px(fx + 1, fy + 10, L.m[4]); L.px(fx + 2, fy + 10, L.m[4])   # toe cap


def leggings(L):
    b = faces(16, 16, 8, 12, 4)
    for name in ("right", "front", "left", "back"):
        x0, y0, w, h = b[name]
        L.plate(x0, y0, w, h, rows=range(7, 12))
        L.band(x0, y0 + 7, w)
    fx, fy, fw, fh = b["front"]
    L.px(fx + 3, fy + 8, L.gem[1]); L.px(fx + 4, fy + 8, L.gem[2])   # belt buckle
    g = faces(0, 16, 4, 12, 4)
    for name in ("right", "front", "left", "back"):
        x0, y0, w, h = g[name]
        L.plate(x0, y0, w, h, rows=range(0, 10))
        if name in ("front", "back"):
            L.motif(x0, y0, w, h, rows=range(0, 5))
        L.band(x0, y0 + 5, w)             # knee guard
    for name in ("top",):
        L.plate(*g[name])


def main():
    for tier in TIERS:
        l1 = Layer(tier, sum(map(ord, tier)))
        helmet(l1); chest(l1); boots(l1)
        l1.im.save(os.path.join(OUT, f"{tier}_layer_1.png"))
        l2 = Layer(tier, sum(map(ord, tier)) * 7)
        leggings(l2)
        l2.im.save(os.path.join(OUT, f"{tier}_layer_2.png"))
    # preview
    prev = Image.new("RGBA", (6 * 136, 40), (60, 60, 64, 255))
    for i, tier in enumerate(TIERS):
        prev.alpha_composite(Image.open(os.path.join(OUT, f"{tier}_layer_1.png")), (i * 136 + 2, 4))
        prev.alpha_composite(Image.open(os.path.join(OUT, f"{tier}_layer_2.png")), (i * 136 + 68, 4))
    prev.resize((prev.width * 2, prev.height * 2), Image.NEAREST).save(os.path.join(ROOT, "tools/armor_preview.png"))
    # front view of a figure wearing each set (helmet+chest+boots from layer 1, leggings from layer 2)
    fig = Image.new("RGBA", (6 * 24, 36), (90, 120, 150, 255))
    for i, tier in enumerate(TIERS):
        a = Image.open(os.path.join(OUT, f"{tier}_layer_1.png"))
        b = Image.open(os.path.join(OUT, f"{tier}_layer_2.png"))
        ox = i * 24 + 4
        fig.alpha_composite(a.crop((8, 8, 16, 16)), (ox + 4, 1))           # head front
        fig.alpha_composite(a.crop((20, 20, 28, 32)), (ox + 4, 9))         # body front
        fig.alpha_composite(a.crop((44, 20, 48, 32)), (ox, 9))             # right arm front
        fig.alpha_composite(a.crop((44, 20, 48, 32)).transpose(Image.FLIP_LEFT_RIGHT), (ox + 12, 9))
        fig.alpha_composite(b.crop((20, 20, 28, 32)), (ox + 4, 9))         # leggings waist
        fig.alpha_composite(b.crop((4, 20, 8, 32)), (ox + 4, 21))          # leggings legs
        fig.alpha_composite(b.crop((4, 20, 8, 32)).transpose(Image.FLIP_LEFT_RIGHT), (ox + 8, 21))
        fig.alpha_composite(a.crop((4, 20, 8, 32)), (ox + 4, 21))          # boots
        fig.alpha_composite(a.crop((4, 20, 8, 32)).transpose(Image.FLIP_LEFT_RIGHT), (ox + 8, 21))
    fig.resize((fig.width * 6, fig.height * 6), Image.NEAREST).save(os.path.join(ROOT, "tools/armor_figures.png"))
    print("armor layers drawn")


if __name__ == "__main__":
    main()
