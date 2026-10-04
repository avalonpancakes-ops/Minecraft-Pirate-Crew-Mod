#!/usr/bin/env python3
"""
Add Minecraft skin PNGs to the mod as pirate skins.

    python3 tools/add_skins.py [--tier S] skin1.png [--tier F] skin2.png folder/ ...
    python3 tools/add_skins.py --retier pirate_3 A        (change an existing skin's tier)

Tiers (F, D, C, B, A, S) decide which pirates wear the skin: a pirate rolled as S tier wears one
of the S skins. --tier applies to every file after it. A file name starting with a tier and an
underscore (S_captain.png) or a folder named after a tier (skins/S/...) also sets it. Skins with
no tier can be worn by any tier.

For every skin it:
  * converts it to the standard 64x64 layout (old 64x32 skins upgraded, HD skins scaled down)
  * makes the inner body layer fully opaque, like vanilla does (stray transparent pixels there
    show up in game as holes / glitchy see-through patches)
  * clears overlay (hat/jacket/sleeve) areas that are completely solid filler
  * detects slim (Alex-style, 3px) arms
then saves it as textures/entity/pirate/pirate_N.png and rewrites pirate_skins.txt.
Also writes tools/skin_preview.png showing the front of every skin for a quick check.
"""
import os
import sys
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src/main/resources/assets/piratecrew")
SKIN_DIR = os.path.join(ASSETS, "textures/entity/pirate")
LIST = os.path.join(ASSETS, "pirate_skins.txt")

# Regions of the inner (base) layer, as (x0, y0, x1, y1)
BASE = [(0, 0, 32, 16), (0, 16, 64, 32), (16, 48, 48, 64)]
# Overlay regions: hat, jacket+right sleeve+right pants, left pants, left sleeve
OVERLAY = [(32, 0, 64, 16), (0, 32, 56, 48), (0, 48, 16, 64), (48, 48, 64, 64)]


def upgrade_legacy(old):
    """64x32 -> 64x64, mirroring the right arm/leg onto the left like vanilla."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    img.paste(old, (0, 0))

    def copy(sx, sy, dx, dy, w, h):
        part = img.crop((sx, sy, sx + w, sy + h)).transpose(Image.FLIP_LEFT_RIGHT)
        img.paste(part, (sx + dx, sy + dy))

    # left leg from right leg
    copy(4, 16, 16, 32, 4, 4); copy(8, 16, 16, 32, 4, 4)
    copy(0, 20, 24, 32, 4, 12); copy(4, 20, 16, 32, 4, 12)
    copy(8, 20, 8, 32, 4, 12); copy(12, 20, 16, 32, 4, 12)
    # left arm from right arm
    copy(44, 16, -8, 32, 4, 4); copy(48, 16, -8, 32, 4, 4)
    copy(40, 20, 0, 32, 4, 12); copy(44, 20, -8, 32, 4, 12)
    copy(48, 20, -16, 32, 4, 12); copy(52, 20, -8, 32, 4, 12)
    return img


def normalise(img):
    img = img.convert("RGBA")
    w, h = img.size
    if w >= 128 and w % 64 == 0 and h in (w, w // 2):
        f = w // 64
        img = img.resize((64, h // f), Image.NEAREST)
        w, h = img.size
    if (w, h) == (64, 32):
        img = upgrade_legacy(img)
        w, h = img.size
    if (w, h) != (64, 64):
        return None
    return img


def is_slim(img):
    # Wide arms use pixels x=54,55 on row 20 (back of the right arm); slim arms leave them empty.
    return img.getpixel((54, 20))[3] == 0 and img.getpixel((55, 20))[3] == 0


def fix(img):
    px = img.load()
    for x0, y0, x1, y1 in BASE:
        for y in range(y0, y1):
            for x in range(x0, x1):
                r, g, b, a = px[x, y]
                px[x, y] = (r, g, b, 255)
    # An overlay region with zero transparency (including its unused corners) is filler, not art.
    for x0, y0, x1, y1 in OVERLAY:
        if all(px[x, y][3] >= 250 for y in range(y0, y1) for x in range(x0, x1)):
            for y in range(y0, y1):
                for x in range(x0, x1):
                    px[x, y] = (0, 0, 0, 0)
    return img


def front_view(img, slim, scale=4):
    """Flat front view: head, body, arms, legs with overlays on top."""
    out = Image.new("RGBA", (16, 32), (0, 0, 0, 0))
    aw = 3 if slim else 4

    def put(sx, sy, w, h, dx, dy):
        out.alpha_composite(img.crop((sx, sy, sx + w, sy + h)), (dx, dy))

    put(8, 8, 8, 8, 4, 0); put(40, 8, 8, 8, 4, 0)          # head + hat
    put(20, 20, 8, 12, 4, 8); put(20, 36, 8, 12, 4, 8)     # body + jacket
    put(44, 20, aw, 12, 4 - aw, 8); put(44, 36, aw, 12, 4 - aw, 8)   # right arm (viewer's left)
    put(36, 52, aw, 12, 12, 8); put(52, 52, aw, 12, 12, 8)           # left arm
    put(4, 20, 4, 12, 4, 20); put(4, 36, 4, 12, 4, 20)     # right leg
    put(20, 52, 4, 12, 8, 20); put(4, 52, 4, 12, 8, 20)    # left leg
    return out.resize((16 * scale, 32 * scale), Image.NEAREST)


TIERS = ["F", "D", "C", "B", "A", "S"]


def guess_tier(path):
    base = os.path.basename(path)
    if len(base) > 2 and base[0].upper() in TIERS and base[1] in "_- ":
        return base[0].upper()
    parent = os.path.basename(os.path.dirname(os.path.abspath(path))).upper()
    return parent if parent in TIERS else None


def gather(args):
    """Returns [(file, tier)] honouring --tier flags."""
    files, tier = [], None
    it = iter(args)
    for a in it:
        if a == "--tier":
            t = next(it, "").upper()
            if t not in TIERS:
                sys.exit("--tier must be one of " + " ".join(TIERS))
            tier = t
            continue
        paths = []
        if os.path.isdir(a):
            for root, _, names in os.walk(a):
                paths += [os.path.join(root, n) for n in sorted(names) if n.lower().endswith(".png")]
        elif a.lower().endswith(".png"):
            paths.append(a)
        for p in paths:
            files.append((p, tier or guess_tier(p)))
    return files


def read_list():
    entries = []
    if os.path.exists(LIST):
        for line in open(LIST):
            line = line.strip()
            if line and not line.startswith("#"):
                parts = line.split()
                tier = parts[2].upper() if len(parts) > 2 and parts[2].upper() in TIERS else None
                entries.append([parts[0], len(parts) > 1 and parts[1] == "slim", tier])
    return entries


def write_list(entries):
    with open(LIST, "w") as out:
        out.write("# name model tier (model: wide = Steve arms, slim = Alex arms; tier F-S, '-' = any tier)\n")
        out.write("# Generated by tools/add_skins.py\n")
        for name, slim, tier in entries:
            out.write(f"{name} {'slim' if slim else 'wide'} {tier or '-'}\n")


def main():
    os.makedirs(SKIN_DIR, exist_ok=True)
    entries = read_list()
    args = sys.argv[1:]

    if args[:1] == ["--retier"]:
        if len(args) != 3 or args[2].upper() not in TIERS + ["-"]:
            sys.exit("usage: --retier NAME TIER")
        for e in entries:
            if e[0] == args[1]:
                e[2] = None if args[2] == "-" else args[2].upper()
                break
        else:
            sys.exit("no skin named " + args[1])
        write_list(entries)
        preview(entries)
        print(f"{args[1]} is now tier {args[2].upper()}")
        return

    used = {e[0] for e in entries}
    n = 1
    added, skipped = [], []
    for f, tier in gather(args):
        try:
            img = normalise(Image.open(f))
        except Exception as e:
            skipped.append((f, str(e)))
            continue
        if img is None:
            skipped.append((f, "not a Minecraft skin size"))
            continue
        slim = is_slim(img)
        img = fix(img)
        while f"pirate_{n}" in used:
            n += 1
        name = f"pirate_{n}"
        used.add(name)
        img.save(os.path.join(SKIN_DIR, name + ".png"))
        entries.append([name, slim, tier])
        added.append((name, os.path.basename(f), slim, tier))

    write_list(entries)
    preview(entries)
    for name, src, slim, tier in added:
        print(f"{name}: {src}  ({'slim' if slim else 'wide'} arms, tier {tier or 'any'})")
    print(f"added {len(added)}, total {len(entries)}")
    for f, why in skipped:
        print(f"skipped {f}: {why}")


def preview(entries):
    views = []
    for name, slim, tier in entries:
        img = Image.open(os.path.join(SKIN_DIR, name + ".png")).convert("RGBA")
        views.append((name, tier, front_view(img, slim)))
    if not views:
        return
    cols = min(10, len(views))
    rows = (len(views) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * 80, rows * 160), (90, 120, 160, 255))
    d = ImageDraw.Draw(sheet)
    for i, (name, tier, v) in enumerate(views):
        x, y = (i % cols) * 80 + 8, (i // cols) * 160 + 4
        sheet.alpha_composite(v, (x, y))
        d.text((x, y + 132), f"{name.replace('pirate_', '#')}  {tier or 'any'}", fill=(255, 255, 255, 255))
    sheet.save(os.path.join(ROOT, "tools/skin_preview.png"))


if __name__ == "__main__":
    main()
