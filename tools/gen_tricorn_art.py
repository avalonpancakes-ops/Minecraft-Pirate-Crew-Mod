"""Captain's Tricorn: the 64x32 texture for its 3D model and the 16x16 item icon. Drawn from scratch."""
import os
import random
from PIL import Image

A = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "piratecrew", "textures")
rng = random.Random(33)


def hexc(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


FELT = [hexc("#1c130e"), hexc("#2b1d16"), hexc("#38271d")]
GOLD = [hexc("#8a6420"), hexc("#d8a63a"), hexc("#ffe08a")]
RED = [hexc("#6a0e18"), hexc("#a01828"), hexc("#e04050")]
BONE = hexc("#f0e6cc")


def felt(im, x0, y0, w, h, dark=0):
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            im.putpixel((x, y), FELT[max(0, min(2, 1 - dark + rng.choice([0, 0, 0, 1, -1])))])


def model_texture():
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    # crown 9x4x9 at (0,0): top (9,0) 9x9, bottom (18,0), sides at v=9: (0,9) (9,9) (18,9) (27,9)
    felt(im, 9, 0, 9, 9)
    felt(im, 18, 0, 9, 9, dark=1)
    for i, x0 in enumerate((0, 9, 18, 27)):
        felt(im, x0, 9, 9, 4)
        for x in range(x0, x0 + 9):              # ruby hat band along the bottom
            im.putpixel((x, 12), RED[1])
            im.putpixel((x, 11), RED[2] if (x % 3 == 0) else RED[1])
    # three walls 17x4x1 at (0, 14 + 6i): up (1,v) 17x1, down (18,v), sides at v+1: (0) 1x4, north (1) 17x4, (18) 1x4, south (19) 17x4
    for i in range(3):
        v = 14 + i * 6
        for x in range(1, 18):
            im.putpixel((x, v), GOLD[2 if x % 4 else 1])
            im.putpixel((x + 17, v), GOLD[0])
        felt(im, 1, v + 1, 17, 4)           # outer face
        felt(im, 19, v + 1, 17, 4, dark=1)  # inner face
        for x in range(1, 18):               # gold piping along the upturned edge (top of the wall)
            im.putpixel((x, v + 1), GOLD[1])
        for x in range(19, 36):
            im.putpixel((x, v + 1), GOLD[0])
        for (x, y) in [(0, v + 1), (0, v + 2), (0, v + 3), (0, v + 4), (18, v + 1), (18, v + 2), (18, v + 3), (18, v + 4)]:
            im.putpixel((x, y), GOLD[1])
        if i == 0:   # skull badge on the front wall
            skull = [".###.", "#.#.#", ".###."]
            for dy, row in enumerate(skull):
                for dx, ch in enumerate(row):
                    if ch == "#":
                        im.putpixel((7 + dx, v + 2 + dy), BONE)
    return im


def icon():
    rows = [
        "................",
        "................",
        "................",
        "......oooo......",
        ".....oFFFFo.....",
        "....oFffffFo....",
        "...oFffffffFo...",
        ".oogFffffffFgoo.",
        "oGGgFffWWWffFgGo",
        "oGgFfffWfWffffgo",
        ".oGgFffWWWffffGo",
        "..ooGgggggggGoo.",
        "....ooooooooo...",
        "................",
        "................",
        "................",
    ]
    pal = {"o": hexc("#0c0806"), "F": FELT[2], "f": FELT[1], "g": GOLD[1], "G": GOLD[2], "W": BONE}
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                im.putpixel((x, y), pal[ch])
    # ruby band highlight across the crown
    for x in range(5, 11):
        if rows[7][x] in "fF":
            im.putpixel((x, 7), RED[1])
    return im


os.makedirs(os.path.join(A, "models", "armor"), exist_ok=True)
model_texture().save(os.path.join(A, "models", "armor", "tricorn.png"))
icon().save(os.path.join(A, "item", "tricorn.png"))
print("tricorn drawn")
