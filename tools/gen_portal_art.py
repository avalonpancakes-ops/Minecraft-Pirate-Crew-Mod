#!/usr/bin/env python3
"""Animated Siren portal texture: a swirling whirlpool of sea-green light with drifting sparkles."""
import json
import math
import os
import random
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/assets/piratecrew/textures/block/siren_portal.png")
FRAMES = 32
RAMP = [(6, 40, 52), (10, 78, 92), (18, 120, 128), (40, 176, 168), (110, 226, 206), (210, 255, 240)]


def col(v):
    v = max(0.0, min(0.999, v))
    i = v * (len(RAMP) - 1)
    a, b = RAMP[int(i)], RAMP[int(i) + 1]
    t = i - int(i)
    return tuple(int(a[k] + (b[k] - a[k]) * t) for k in range(3))


def main():
    rnd = random.Random(7)
    sparks = [(rnd.random() * 16, rnd.random() * 16, rnd.random()) for _ in range(6)]
    im = Image.new("RGBA", (16, 16 * FRAMES))
    for f in range(FRAMES):
        t = f / FRAMES
        for y in range(16):
            for x in range(16):
                # tile-able swirl: angle around the tile centre plus radial bands, wrapped with sin/cos
                dx, dy = (x + 0.5) / 16 * 2 * math.pi, (y + 0.5) / 16 * 2 * math.pi
                ang = math.atan2(math.sin(dy - math.pi), math.sin(dx - math.pi))
                r = math.hypot(math.sin((dx - math.pi) / 2), math.sin((dy - math.pi) / 2))
                v = 0.5 + 0.28 * math.sin(3 * ang + r * 9 - t * 2 * math.pi * 2)
                v += 0.16 * math.sin(dx * 2 + t * 2 * math.pi) * math.cos(dy * 2 - t * 2 * math.pi)
                v += 0.06 * math.sin((dx + dy) * 3 - t * 2 * math.pi * 3)
                c = col(v)
                a = int(170 + 70 * max(0, min(1, v)))
                im.putpixel((x, f * 16 + y), c + (a,))
        # sparkles drift upward and twinkle
        for (sx, sy, ph) in sparks:
            yy = (sy - t * 16) % 16
            k = 0.5 + 0.5 * math.sin((t + ph) * 2 * math.pi * 2)
            if k > 0.55:
                im.putpixel((int(sx), f * 16 + int(yy)), (230, 255, 248, 255))
    im.save(OUT)
    json.dump({"animation": {"frametime": 2}}, open(OUT + ".mcmeta", "w"))
    prev = Image.new("RGBA", (16 * 8, 16), (0, 0, 0, 255))
    for i in range(8):
        prev.alpha_composite(im.crop((0, i * 4 * 16, 16, i * 4 * 16 + 16)), (i * 16, 0))
    prev.resize((16 * 8 * 6, 16 * 6), Image.NEAREST).save(os.path.join(ROOT, "tools/portal_preview.png"))
    print("portal drawn")


if __name__ == "__main__":
    main()
