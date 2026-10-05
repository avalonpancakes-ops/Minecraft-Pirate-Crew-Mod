"""
Jolly Roger banner/shield pattern masks (white + alpha, tinted by the game) and the pattern item icon.
The skull and crossed bones are drawn from scratch.
"""
import os
from PIL import Image

A = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "piratecrew", "textures")

# 20 x 40 banner face: skull with eye sockets and teeth over crossed bones
BANNER = [
    "....................",
    "....................",
    "....................",
    "....................",
    "....................",
    "....................",
    "......########......",
    ".....##########.....",
    "....############....",
    "....############....",
    "....##...##...##....",
    "....##...##...##....",
    "....##...##...##....",
    "....#####..#####....",
    ".....####..####.....",
    "......########......",
    ".....#.#.##.#.#.....",
    "......########......",
    "....................",
    "##................##",
    "###..............###",
    ".####..........####.",
    "...####......####...",
    ".....####..####.....",
    ".......######.......",
    "........####........",
    ".......######.......",
    ".....####..####.....",
    "...####......####...",
    ".####..........####.",
    "###..............###",
    "##................##",
    "....................",
    "....................",
    "....................",
    "....................",
    "....................",
    "....................",
    "....................",
    "....................",
]

# 12 x 22 shield face
SHIELD = [
    "............",
    "............",
    "...######...",
    "..########..",
    "..#..##..#..",
    "..#..##..#..",
    "..###..###..",
    "...######...",
    "..#.#..#.#..",
    "............",
    "#..........#",
    "##........##",
    ".###....###.",
    "...######...",
    "....####....",
    "...######...",
    ".###....###.",
    "##........##",
    "#..........#",
    "............",
    "............",
    "............",
]


def mask(rows, ox, oy):
    im = Image.new("RGBA", (64, 64), (255, 255, 255, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == "#":
                im.putpixel((ox + x, oy + y), (255, 255, 255, 255))
    return im


def icon():
    rows = [
        "................",
        "...pppppppppp...",
        "..pPPPPPPPPPPp..",
        "..pPPPkkkkPPPp..",
        "..pPPkkkkkkPPp..",
        "..pPPkPkkPkPPp..",
        "..pPPkkkkkkPPp..",
        "..pPPPkPkPPPPp..",
        "..pPkPPPPPPkPp..",
        "..pPPkPPPPkPPp..",
        "..pPPPkkkkPPPp..",
        "..pPPkPPPPkPPp..",
        "..pPkPPPPPPkPp..",
        "..pPPPPPPPPPPp..",
        "...pppppppppp...",
        "................",
    ]
    pal = {"p": (90, 70, 46, 255), "P": (232, 214, 172, 255), "k": (24, 18, 14, 255)}
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                im.putpixel((x, y), pal[ch])
    return im


for d in ("entity/banner", "entity/shield"):
    os.makedirs(os.path.join(A, d), exist_ok=True)
mask(BANNER, 1, 1).save(os.path.join(A, "entity", "banner", "jolly_roger.png"))
mask(SHIELD, 1, 1).save(os.path.join(A, "entity", "shield", "jolly_roger.png"))
icon().save(os.path.join(A, "item", "jolly_roger_pattern.png"))
print("jolly roger drawn")
