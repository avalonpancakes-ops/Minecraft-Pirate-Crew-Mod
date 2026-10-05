#!/usr/bin/env python3
"""Bank counter block: mahogany panelling with brass trim and a ruby crest (side), a polished
counter top with a brass coin tray, a ledger and a quill (top)."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pixelart import hexc, mix  # noqa: E402
from PIL import Image  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
B = os.path.join(ROOT, "src/main/resources/assets/piratecrew/textures/block")
WOOD = [hexc(c) for c in ("#2a140a", "#3e1e10", "#542a16", "#6a381e", "#804828")]
BRASS = [hexc(c) for c in ("#6a4a12", "#b8862a", "#f0cc6a", "#fff0b0")]
RUBY = [hexc(c) for c in ("#5a0812", "#a01628", "#e0344a", "#ff9aa8")]


def side():
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            grain = (x * 7 + (y // 3) * 5) % 9
            px[x, y] = WOOD[2] if grain else WOOD[1]
    # brass frame
    for i in range(16):
        px[i, 0] = BRASS[2]; px[i, 15] = BRASS[0]; px[0, i] = BRASS[1]; px[15, i] = BRASS[0]
        px[i, 1] = BRASS[1] if i not in (0, 15) else px[i, 1]
    # recessed panel
    for y in range(4, 13):
        for x in range(3, 13):
            px[x, y] = WOOD[1] if (x + y) % 7 else WOOD[0]
    for x in range(3, 13):
        px[x, 3] = WOOD[0]; px[x, 13] = WOOD[4]
    for y in range(3, 14):
        px[2, y] = WOOD[0]; px[13, y] = WOOD[4]
    # ruby crest in a brass ring
    cx, cy = 8, 8
    for (x, y) in [(6, 6), (7, 5), (8, 5), (9, 6), (10, 7), (10, 8), (9, 9), (8, 10), (7, 10), (6, 9), (5, 8), (5, 7)]:
        px[x, y] = BRASS[1]
    for (x, y), k in {(7, 6): 3, (8, 6): 2, (6, 7): 2, (7, 7): 2, (8, 7): 1, (9, 7): 1, (6, 8): 1, (7, 8): 1, (8, 8): 0, (9, 8): 0, (7, 9): 0, (8, 9): 0}.items():
        px[x, y] = RUBY[k] if k else RUBY[0]
    px[7, 6] = RUBY[3]
    # rivets
    for (x, y) in [(1, 1), (14, 1), (1, 14), (14, 14)]:
        px[x, y] = BRASS[3]
    return im


def top():
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            band = (y // 4) % 2
            px[x, y] = WOOD[3] if band else WOOD[2]
            if x % 8 == 0 and y % 4 == 1:
                px[x, y] = WOOD[1]
    for i in range(16):
        px[i, 0] = BRASS[1]; px[i, 15] = BRASS[0]; px[0, i] = BRASS[1]; px[15, i] = BRASS[0]
    # brass coin tray with rubies
    for y in range(9, 14):
        for x in range(2, 8):
            px[x, y] = BRASS[1] if y in (9, 13) or x in (2, 7) else BRASS[0]
    px[4, 11] = RUBY[2]; px[5, 11] = RUBY[3]; px[5, 12] = RUBY[1]; px[3, 12] = RUBY[2]
    # ledger
    for y in range(2, 8):
        for x in range(8, 14):
            px[x, y] = hexc("#e8d2a0") if x != 11 else hexc("#a88a5a")
    for x in (9, 12):
        for y in (3, 5):
            px[x, y] = hexc("#6a5a40")
    # quill
    for i, (x, y) in enumerate([(3, 2), (4, 3), (5, 4), (6, 5), (7, 6)]):
        px[x, y] = hexc("#f4f0e8") if i < 3 else hexc("#3a2a1a")
    px[2, 2] = hexc("#ffffff")
    return im


side().save(os.path.join(B, "bank_counter_side.png"))
top().save(os.path.join(B, "bank_counter_top.png"))
prev = Image.new("RGBA", (36, 16))
prev.paste(Image.open(os.path.join(B, "bank_counter_side.png")), (0, 0))
prev.paste(Image.open(os.path.join(B, "bank_counter_top.png")), (20, 0))
prev.resize((36 * 8, 16 * 8), Image.NEAREST).save(os.path.join(ROOT, "tools/bank_preview.png"))
print("bank counter drawn")
