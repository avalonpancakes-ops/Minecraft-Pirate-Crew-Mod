"""
Legendary boss weapons: one unique blade per boss, each hand-drawn with its own palette and an
animated shine, pulsing glow and a twinkle at the tip (24 frames). Drawn from scratch.

  broadside_cutlass   Commodore Graves  brass basket guard, ember-lit edge
  krakens_grasp       The Kraken        a hooked, tentacle-curled blade with glowing suckers
  stormcaller         Sorel             a slim saber with a lightning groove down the blade
  leviathans_fang     The Leviathan     a serrated ivory tooth on a sea-glass hilt
  iron_tide           Fleet Admiral     a great blade of dark iron with a crimson-gold guard
"""
import os

import gen_gear_art as G
from pixelart import shine, pulse, twinkle, strip, sheet

LEGENDS = {
    "broadside_cutlass": dict(
        pal=dict(blade=["#1d3a44", "#3a6670", "#6aa0a8", "#a8d8dc", "#effcfa"],
                 guard=["#6b4a12", "#b8862a", "#f0cc6a"], grip=["#2a1a10", "#4a2e1c", "#6a4428"],
                 gem=["#7a1a10", "#e0502a", "#ffd0a0"], glow=["#ff8a2a", "#ffe0a0"], outline="#0b1214"),
        trim=["#b8862a", "#f0cc6a"], frametime=2,
        rows=[
            "................",
            "............55..",
            "...........5432.",
            "..........54321.",
            ".........54321..",
            "........54321x..",
            ".......54321x...",
            "......54321x....",
            ".....54321x.....",
            "..GGG4321x......",
            ".GKKG321x.......",
            ".GK.GGGx........",
            ".GKhhG..........",
            "..GHhG..........",
            "..Hh............",
            ".ee.............",
        ]),
    "krakens_grasp": dict(
        pal=dict(blade=["#3a1430", "#6a2a5a", "#a04a88", "#d47ab4", "#ffd0ec"],
                 guard=["#7a6a5a", "#bfb09a", "#f0e8d8"], grip=["#1a1020", "#2e1c3a", "#4a2e5a"],
                 gem=["#2a8a7a", "#5ae0c8", "#d0fff4"], glow=["#5ae0c8", "#d0fff4"], outline="#120612"),
        trim=["#bfb09a", "#f0e8d8"], frametime=2,
        rows=[
            "..........xx....",
            ".........x55x...",
            "...........54x..",
            "..........5432..",
            ".........5432x..",
            "........5432....",
            ".......543x2....",
            "......5432......",
            ".....5x32.......",
            "....5432........",
            "..G5432.........",
            "..KG32..........",
            "...GhG..........",
            "...Hh.G.........",
            "..Hh............",
            ".ee.............",
        ]),
    "stormcaller": dict(
        pal=dict(blade=["#1a2440", "#2e4478", "#5a7ac0", "#a8c0f0", "#f0f6ff"],
                 guard=["#8a6a00", "#d8b000", "#ffe85a"], grip=["#141428", "#24244a", "#3a3a6a"],
                 gem=["#3a5aff", "#8ab0ff", "#e8f0ff"], glow=["#ffe85a", "#fffbd0"], outline="#080a14"),
        trim=["#d8b000", "#ffe85a"], frametime=1,
        rows=[
            "..............y.",
            ".............5y.",
            "............543.",
            "...........5y4..",
            "..........54y...",
            ".........5y43...",
            "........54y.....",
            ".......5y43.....",
            "......54y.......",
            ".....5y43.......",
            "..K.54y.........",
            "..GK4y..........",
            "...GhG..........",
            "..GHh.K.........",
            "..Hh............",
            ".ee.............",
        ]),
    "leviathans_fang": dict(
        pal=dict(blade=["#5a5040", "#9c9078", "#d0c6ac", "#ece6d4", "#fffef6"],
                 guard=["#0e5a66", "#1aa8b8", "#7af0ff"], grip=["#0a2a30", "#124650", "#1e6a76"],
                 gem=["#0e8a9a", "#5af0ff", "#e0ffff"], glow=["#3af0d0", "#c8fff4"], outline="#061416"),
        trim=["#1aa8b8", "#7af0ff"], frametime=2,
        rows=[
            "................",
            "............5...",
            "...........54...",
            "..........5432..",
            ".........54321..",
            "........54321.3.",
            ".......54321....",
            "......54321.3...",
            ".....54321......",
            "....5x321.3.....",
            "...Gx321........",
            "..GKGx2.........",
            "...GhKG.........",
            "..Hh..G.........",
            ".Hh.............",
            ".e..............",
        ]),
    "iron_tide": dict(
        pal=dict(blade=["#1a1a22", "#34343f", "#5a5a68", "#9a9aa8", "#e8e8f0"],
                 guard=["#a0281e", "#c8901e", "#ffe070"], grip=["#2a0e0e", "#4a1a1a", "#6a2a2a"],
                 gem=["#8a0a14", "#ff3a4a", "#ffd0d4"], glow=["#ff4a5a", "#ffd0d4"], outline="#0a0a0e"),
        trim=["#c8901e", "#ffe070"], frametime=2,
        rows=[
            "............555.",
            "...........54445",
            "..........544432",
            ".........5444322",
            "........5444x22.",
            ".......5444x22..",
            "......5444x22...",
            ".....5444x22....",
            "....5444x22.....",
            "...5444x22......",
            ".K.444x22.......",
            "..KK4x22........",
            "...GKe2.........",
            "...hGKG.........",
            "..Hh..KG........",
            ".ee.............",
        ]),
}

FRAMES = 24


def draw(name, d):
    G.TIERS[name] = d["pal"]
    G.TRIM[name] = d["trim"]
    c, pal, metal, glow, gems = G.paint(name, d["rows"])
    c.outline(pal["outline"])
    spot = G.sparkle_spot(c, metal)
    frames = []
    for i in range(FRAMES):
        t = i / FRAMES
        f = shine(c, metal, t, width=2.0, strength=1.6)
        f = pulse(f, glow, t, 0.7, 1.45)
        f = pulse(f, gems, (t + 0.5) % 1, 0.85, 1.5)
        if spot:
            f = twinkle(f, spot, t)
        frames.append(f.image())
    G.save(name, strip(frames), {"animation": {"frametime": d["frametime"], "interpolate": False}})
    return frames[0], frames[FRAMES // 3]


def main():
    previews = []
    for name, d in LEGENDS.items():
        previews.extend(draw(name, d))
    out = os.path.join(os.path.dirname(__file__), "..", "build", "legend_preview.png")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    sheet(previews, scale=8, cols=2).save(out)
    print("legendary weapons drawn ->", out)


if __name__ == "__main__":
    main()
