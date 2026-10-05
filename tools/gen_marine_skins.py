#!/usr/bin/env python3
"""
Draws the Marines' skins (Order of the Tide) and the boss skins as standard 64x64 player skins:
charcoal-navy longcoats, brass buttons, teal sash, black tricorne with a band, rank details.
Writes them into the pirate texture folder and registers them in pirate_skins.txt.
"""
import os
import random
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
A = os.path.join(ROOT, "src/main/resources/assets/piratecrew")
SKIN_DIR = os.path.join(A, "textures/entity/pirate")
LIST = os.path.join(A, "pirate_skins.txt")

SKIN_TONES = [(236, 196, 160), (214, 165, 120), (176, 120, 80), (122, 80, 52), (92, 60, 40)]
HAIR = [(40, 28, 20), (90, 60, 30), (150, 110, 60), (20, 20, 20), (120, 120, 120), (180, 80, 30)]


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


class Skin:
    def __init__(self):
        self.im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.px = self.im.load()

    def rect(self, x0, y0, x1, y1, c, jitter=0.0, rng=None):
        for y in range(y0, y1):
            for x in range(x0, x1):
                cc = c
                if jitter and rng:
                    cc = shade(c, 1.0 + (rng.random() - 0.5) * jitter)
                self.px[x, y] = cc + (255,) if len(cc) == 3 else cc

    def dot(self, x, y, c):
        self.px[x, y] = c + (255,)


def draw(rank, seed, palette=None):
    """rank: recruit, rifleman, sergeant, captain, or a boss id."""
    rng = random.Random(seed)
    s = Skin()
    tone = rng.choice(SKIN_TONES)
    hair = rng.choice(HAIR)
    coat = (31, 42, 58)
    trim = (31, 163, 154)      # teal
    brass = (201, 162, 39)
    shirt = (230, 230, 222)
    trousers = (205, 205, 196)
    boots = (26, 24, 24)
    hat = (18, 18, 22)
    band = trim
    if palette:
        coat, trim, brass, hat, band, trousers = palette["coat"], palette["trim"], palette["brass"], palette["hat"], palette["band"], palette["trousers"]
        hair = palette.get("hair", hair)
        tone = palette.get("tone", tone)
    if rank == "sergeant":
        band = (170, 40, 40)

    # ---------------- head (base layer)
    for (x0, y0) in [(0, 8), (8, 8), (16, 8), (24, 8)]:
        s.rect(x0, y0, x0 + 8, y0 + 8, tone, 0.06, rng)
    s.rect(8, 0, 16, 8, hair)                      # top
    s.rect(16, 0, 24, 8, tone)                     # bottom
    s.rect(24, 8, 32, 14, hair)                    # back: hair
    s.rect(0, 8, 8, 12, hair); s.rect(16, 8, 24, 12, hair)   # sides
    s.rect(8, 8, 16, 10, hair)                     # fringe
    # face
    for x in (9, 13):
        s.dot(x, 12, (245, 245, 245)); s.dot(x + 1, 12, (30, 50, 70))
    s.rect(9, 11, 11, 12, shade(hair, 0.8)); s.rect(13, 11, 15, 12, shade(hair, 0.8))
    s.dot(11, 13, shade(tone, 0.85)); s.dot(12, 13, shade(tone, 0.85))
    s.rect(10, 14, 14, 15, (120, 60, 50))
    if rank in ("commodore", "fleet_admiral"):        # beard
        s.rect(8, 14, 16, 16, palette.get("beard", hair)); s.rect(10, 14, 14, 15, (120, 60, 50))
    if rank == "fleet_admiral":                       # scar
        s.dot(14, 10, (150, 70, 70)); s.dot(14, 11, (150, 70, 70)); s.dot(13, 12, (150, 70, 70))

    # ---------------- hat (overlay layer, tricorne)
    s.rect(40, 0, 48, 8, hat)                       # top
    for (x0, y0) in [(32, 8), (40, 8), (48, 8), (56, 8)]:
        s.rect(x0, y0, x0 + 8, y0 + 3, hat)
        s.rect(x0, y0 + 3, x0 + 8, y0 + 4, band)
    s.dot(43, 9, brass); s.dot(44, 9, brass)        # cockade
    if rank == "rifleman":                          # goggles on the hat
        s.rect(41, 10, 47, 11, (90, 60, 30)); s.dot(42, 10, (140, 200, 220)); s.dot(45, 10, (140, 200, 220))

    # ---------------- body
    s.rect(20, 16, 28, 20, coat); s.rect(28, 16, 36, 20, coat)
    s.rect(20, 20, 28, 32, coat, 0.08, rng); s.rect(32, 20, 40, 32, coat, 0.08, rng)
    s.rect(16, 20, 20, 32, coat); s.rect(28, 20, 32, 32, coat)
    s.rect(23, 20, 25, 23, shirt)                   # collar
    for y in (23, 25, 27, 29):                      # buttons
        s.dot(22, y, brass); s.dot(25, y, brass)
    for i in range(8):                              # sash, shoulder to hip
        s.dot(20 + i, 21 + i, trim)
        if 22 + i < 30:
            s.dot(20 + i, 22 + i, trim)
    s.rect(20, 30, 28, 31, shade(coat, 0.7))        # belt
    s.dot(24, 30, brass)
    if rank == "rifleman":                          # bandolier
        for i in range(8):
            s.dot(27 - i, 20 + i, (100, 66, 34)); s.dot(27 - i, 21 + i, (100, 66, 34))
    if rank in ("captain", "commodore", "tempest", "fleet_admiral"):
        s.rect(20, 20, 21, 32, brass); s.rect(27, 20, 28, 32, brass)        # gold trim
    # coat tails (jacket overlay, slightly longer)
    s.rect(20, 44, 28, 48, shade(coat, 0.9)); s.rect(32, 44, 40, 48, shade(coat, 0.9))

    # ---------------- arms (right: x40-56 y16-32; left: x32-48 y48-64)
    for (bx, by) in [(40, 16), (32, 48)]:
        s.rect(bx + 4, by, bx + 8, by + 4, coat)            # top
        s.rect(bx + 8, by, bx + 12, by + 4, tone)           # bottom (hand)
        s.rect(bx, by + 4, bx + 16, by + 14, coat, 0.08, rng)
        s.rect(bx, by + 14, bx + 16, by + 15, trim)         # cuff
        hand = (240, 240, 235) if rank in ("captain", "commodore", "fleet_admiral") else tone
        s.rect(bx, by + 15, bx + 16, by + 16, hand)
        if rank in ("captain", "commodore", "tempest", "fleet_admiral"):   # epaulettes
            s.rect(bx + 4, by, bx + 8, by + 2, brass)
            s.rect(bx, by + 4, bx + 16, by + 5, brass)
        if rank == "sergeant":                              # chevrons
            for i in range(3):
                s.dot(bx + 5 + i, by + 7 + i % 2, brass); s.dot(bx + 6 + i, by + 9 - i % 2, brass)

    # ---------------- legs (right: x0-16 y16-32; left: x16-32 y48-64)
    for (bx, by) in [(0, 16), (16, 48)]:
        s.rect(bx + 4, by, bx + 8, by + 4, trousers)
        s.rect(bx + 8, by, bx + 12, by + 4, boots)
        s.rect(bx, by + 4, bx + 16, by + 10, trousers, 0.05, rng)
        s.rect(bx, by + 10, bx + 16, by + 16, boots)
        s.rect(bx, by + 10, bx + 16, by + 11, shade(boots, 1.8))
    return s.im


BOSSES = {
    "boss_commodore": ("commodore", {"coat": (24, 34, 74), "trim": (201, 162, 39), "brass": (230, 196, 70), "hat": (16, 18, 34),
                                     "band": (201, 162, 39), "trousers": (220, 220, 212), "hair": (150, 150, 150),
                                     "beard": (170, 170, 170), "tone": (214, 165, 120)}),
    "boss_tempest": ("tempest", {"coat": (44, 38, 70), "trim": (90, 200, 255), "brass": (190, 220, 255), "hat": (28, 24, 46),
                                 "band": (90, 200, 255), "trousers": (60, 60, 80), "hair": (235, 235, 245), "tone": (236, 196, 160)}),
    "boss_fleet_admiral": ("fleet_admiral", {"coat": (14, 14, 16), "trim": (170, 20, 30), "brass": (226, 186, 60), "hat": (10, 10, 12),
                                             "band": (170, 20, 30), "trousers": (30, 30, 34), "hair": (196, 196, 204),
                                             "beard": (210, 210, 216), "tone": (176, 120, 80)}),
}


def main():
    os.makedirs(SKIN_DIR, exist_ok=True)
    entries = []
    for rank in ("recruit", "rifleman", "sergeant", "captain"):
        for i in range(1, 5):
            name = f"marine_{rank}_{i}"
            draw(rank, sum(map(ord, rank)) * 31 + i).save(os.path.join(SKIN_DIR, name + ".png"))
            entries.append(name)
    for name, (rank, pal) in BOSSES.items():
        draw(rank, 7, pal).save(os.path.join(SKIN_DIR, name + ".png"))
        entries.append(name)

    lines = [l.rstrip("\n") for l in open(LIST)] if os.path.exists(LIST) else []
    keep = [l for l in lines if not l.split(" ")[0].startswith(("marine_", "boss_"))]
    keep += [f"{n} wide -" for n in entries]
    open(LIST, "w").write("\n".join(keep) + "\n")

    # preview: front views
    sheet = Image.new("RGBA", (len(entries) * 40, 80), (90, 120, 160, 255))
    for i, n in enumerate(entries):
        im = Image.open(os.path.join(SKIN_DIR, n + ".png"))
        f = Image.new("RGBA", (16, 32))
        f.alpha_composite(im.crop((8, 8, 16, 16)), (4, 0)); f.alpha_composite(im.crop((40, 8, 48, 16)), (4, 0))
        f.alpha_composite(im.crop((20, 20, 28, 32)), (4, 8)); f.alpha_composite(im.crop((44, 20, 48, 32)), (0, 8))
        f.alpha_composite(im.crop((36, 52, 40, 64)), (12, 8)); f.alpha_composite(im.crop((4, 20, 8, 32)), (4, 20))
        f.alpha_composite(im.crop((20, 52, 24, 64)), (8, 20))
        sheet.alpha_composite(f.resize((32, 64), Image.NEAREST), (i * 40 + 4, 8))
    sheet.save(os.path.join(ROOT, "tools/marine_preview.png"))
    print("wrote", len(entries), "marine/boss skins")


if __name__ == "__main__":
    main()
