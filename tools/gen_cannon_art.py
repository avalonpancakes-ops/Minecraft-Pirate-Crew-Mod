"""Cannon block textures (gunmetal and tarred wood) and the cannonball item. Drawn from scratch."""
import os
import random
from PIL import Image

A = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "piratecrew", "textures")
rng = random.Random(9)


def hexc(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def iron():
    im = Image.new("RGBA", (16, 16))
    ramp = [hexc(c) for c in ("#17181c", "#24262c", "#30333b", "#3e424c", "#5a606c", "#8a92a0")]
    for y in range(16):
        for x in range(16):
            # cylinder shading across x, with a little cast-metal noise
            band = [0, 1, 2, 3, 3, 4, 4, 5, 4, 4, 3, 3, 2, 2, 1, 0][x]
            band = max(0, min(5, band + rng.choice([0, 0, 0, -1, 1])))
            im.putpixel((x, y), ramp[band])
    for y in (0, 15):   # dark seams top and bottom
        for x in range(16):
            im.putpixel((x, y), ramp[0])
    for (x, y) in [(2, 3), (13, 3), (2, 12), (13, 12)]:   # rivets
        im.putpixel((x, y), ramp[5])
        im.putpixel((x, y + 1), ramp[1])
    return im


def wood():
    im = Image.new("RGBA", (16, 16))
    ramp = [hexc(c) for c in ("#2a1a0e", "#3e2716", "#53351e", "#6a4527", "#7e5531")]
    for y in range(16):
        plank = y // 4
        for x in range(16):
            v = 2 + (1 if (x + plank * 5) % 7 == 0 else 0) - (1 if (x * 3 + y) % 11 == 0 else 0)
            v += rng.choice([0, 0, 0, 1, -1])
            if y % 4 == 3:
                v = 0
            im.putpixel((x, y), ramp[max(0, min(4, v))])
    for y in range(16):   # an iron strap down the middle
        for x in (7, 8):
            im.putpixel((x, y), hexc("#2c2e34") if x == 7 else hexc("#4a4e58"))
    for y in (1, 5, 9, 13):
        im.putpixel((8, y), hexc("#9aa2b0"))
    return im


def ball():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    import math
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 8.5)
            if d > 5.6:
                continue
            if d > 4.8:
                c = hexc("#0c0c10")
            else:
                l = 1 - math.hypot(x - 6, y - 7) / 6.5
                c = [hexc("#1c1e24"), hexc("#2c2f37"), hexc("#434854"), hexc("#6a7180")][max(0, min(3, int(l * 4)))]
            im.putpixel((x, y), c)
    im.putpixel((5, 6), hexc("#b8c0cc"))
    im.putpixel((6, 6), hexc("#8a92a0"))
    # a little fuse-hole glint
    im.putpixel((10, 11), hexc("#14161a"))
    return im


iron().save(os.path.join(A, "block", "cannon_iron.png"))
wood().save(os.path.join(A, "block", "cannon_wood.png"))
ball().save(os.path.join(A, "item", "cannonball.png"))
print("cannon art drawn")
