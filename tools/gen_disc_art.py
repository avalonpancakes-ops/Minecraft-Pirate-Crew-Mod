"""
Music disc item icons, drawn from scratch: a front-on record with grooves, a glinting rim, and a
painted label with a tiny emblem (bottle for the shanty, ruby-red diamond for the jig, tentacle for
the lullaby).
"""
import math
import os
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "piratecrew", "textures", "item")


def hexc(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


DISCS = {
    "sailor": dict(label=["#7a1a10", "#c0402a", "#ff7a52"], emblem=["....", ".##.", ".##.", "####", "####"], ecol="#3a7a3a", ehi="#9adf8a"),
    "jig": dict(label=["#6a4a0a", "#c8901e", "#ffe070"], emblem=["..#..", ".###.", "#####", ".###.", "..#.."], ecol="#b01828", ehi="#ff6070"),
    "kraken": dict(label=["#2a1050", "#6a3ab0", "#c090ff"], emblem=["#...#", "#.#.#", ".###.", "..#..", ".#.#."], ecol="#ff8ac4", ehi="#ffd0e8"),
}


def draw(name, d):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cx = cy = 7.5
    vinyl = [hexc("#0e0c10"), hexc("#1c1a22"), hexc("#2a2832")]
    lab = [hexc(c) for c in d["label"]]
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - cx, y - cy)
            if r > 7.6:
                continue
            if r > 6.9:
                c = hexc("#060508")                      # outline
            elif r > 3.2:
                groove = int(r * 2) % 2
                c = vinyl[1] if groove else vinyl[0]
                ang = math.atan2(y - cy, x - cx)
                if -2.6 < ang < -1.9 or 0.6 < ang < 1.2:   # light catching the grooves
                    c = vinyl[2] if groove else vinyl[1]
            elif r > 2.6:
                c = lab[0]
            else:
                c = lab[1] if (x + y) % 5 else lab[2]
            im.putpixel((x, y), c)
    # spindle hole
    im.putpixel((7, 7), (0, 0, 0, 0))
    im.putpixel((8, 8), lab[0])
    # rim glint
    for (x, y) in [(3, 2), (4, 1), (2, 3), (12, 13), (13, 12)]:
        im.putpixel((x, y), hexc("#5a5866"))
    # emblem on the label is tiny; tint the label's top-left pixels with the emblem colour instead
    e = hexc(d["ecol"]); hi = hexc(d["ehi"])
    rows = d["emblem"]
    ox, oy = 8 - len(rows[0]) // 2, 8 - len(rows) // 2
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch == "#":
                px, py = ox + i, oy + j
                if math.hypot(px - cx, py - cy) <= 3.3:
                    im.putpixel((px, py), hi if j == 0 or (i == 0) else e)
    im.putpixel((7, 7), (0, 0, 0, 0))
    im.save(os.path.join(OUT, f"music_disc_{name}.png"))


for n, d in DISCS.items():
    draw(n, d)
print("discs drawn")
