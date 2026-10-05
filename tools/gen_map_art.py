"""Torn Treasure Map icon: a curling, torn parchment with a dotted trail to a red X. Drawn from scratch."""
import os
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "piratecrew", "textures", "item", "treasure_map.png")

ROWS = [
    "................",
    "..oooooooooo....",
    ".oLLLLLLLLLLoo..",
    ".oLPPPPPPPPPLLo.",
    "..oPPPPPPPPPPPo.",
    "..oPPdPPPPPPPPo.",
    "..oPPPdPPPPRPRo.",
    ".oPPPPPdPPPPRPo.",
    ".oPPPPPPdPPRPRo.",
    ".oPPPPPPPdPPPPo.",
    "..oPPPPPPPddPPo.",
    "..oPPPPPPPPPPo..",
    "..oSSPPPPPPPPo..",
    ".oSSSSSPPPSSSo..",
    ".ooo..oSSSoooo..",
    "......ooo.......",
]
PAL = {
    "o": (58, 38, 20, 255),     # outline
    "L": (250, 236, 200, 255),  # lit curl
    "P": (226, 200, 150, 255),  # parchment
    "S": (186, 150, 100, 255),  # shaded / burnt edge
    "d": (120, 84, 50, 255),    # dotted trail
    "R": (200, 30, 36, 255),    # the X
}

im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
for y, row in enumerate(ROWS):
    for x, ch in enumerate(row):
        if ch in PAL:
            im.putpixel((x, y), PAL[ch])
# speckle the parchment so it reads as old paper
for (x, y) in [(4, 4), (9, 3), (12, 9), (5, 11), (10, 12), (3, 8)]:
    if ROWS[y][x] == "P":
        im.putpixel((x, y), (210, 182, 132, 255))
# break up the trail into dots
for (x, y) in [(6, 6), (8, 8), (10, 10)]:
    im.putpixel((x, y), PAL["P"])
im.save(OUT)
print("map drawn")
