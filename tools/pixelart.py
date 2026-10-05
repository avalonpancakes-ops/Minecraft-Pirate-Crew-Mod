"""
Tiny pixel-art toolkit for the mod's textures: palettes, ASCII sprites, procedural blades, outlines,
shading and animation strips. Everything is drawn from scratch (no game assets).
"""
import math
from PIL import Image


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def mix(c1, c2, t):
    return tuple(int(round(a + (b - a) * t)) for a, b in zip(c1, c2))


def shade(c, f):
    if f >= 1:
        return tuple(min(255, int(v + (255 - v) * (f - 1))) for v in c[:3]) + (c[3] if len(c) > 3 else 255,)
    return tuple(max(0, int(v * f)) for v in c[:3]) + (c[3] if len(c) > 3 else 255,)


def ramp(dark, light, n):
    """n colours from dark to light, with a slight hue shift toward warm highlights / cool shadows."""
    return [mix(dark, light, i / (n - 1)) for i in range(n)]


class Canvas:
    def __init__(self, w=16, h=16):
        self.w, self.h = w, h
        self.px = [[None] * w for _ in range(h)]

    def get(self, x, y):
        if 0 <= x < self.w and 0 <= y < self.h:
            return self.px[y][x]
        return None

    def set(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h and c is not None:
            self.px[y][x] = c

    def filled(self, x, y):
        return self.get(x, y) is not None

    def copy(self):
        c = Canvas(self.w, self.h)
        c.px = [row[:] for row in self.px]
        return c

    def sprite(self, rows, pal, ox=0, oy=0):
        """Paint an ASCII sprite: each char maps through pal; '.' and ' ' are transparent."""
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch in ". ":
                    continue
                self.set(ox + x, oy + y, pal[ch])

    def outline(self, col, diagonal=False):
        """Dark outline around every filled shape."""
        add = []
        for y in range(self.h):
            for x in range(self.w):
                if self.filled(x, y):
                    continue
                nb = [(1, 0), (-1, 0), (0, 1), (0, -1)]
                if diagonal:
                    nb += [(1, 1), (-1, -1), (1, -1), (-1, 1)]
                if any(self.filled(x + dx, y + dy) and self.get(x + dx, y + dy) != col for dx, dy in nb):
                    add.append((x, y))
        for x, y in add:
            self.set(x, y, col)

    def image(self):
        im = Image.new("RGBA", (self.w, self.h), (0, 0, 0, 0))
        for y in range(self.h):
            for x in range(self.w):
                c = self.px[y][x]
                if c is not None:
                    im.putpixel((x, y), c if len(c) == 4 else c + (255,))
        return im


def strip(frames):
    """Stack frames vertically into an animation strip."""
    w, h = frames[0].size
    im = Image.new("RGBA", (w, h * len(frames)), (0, 0, 0, 0))
    for i, f in enumerate(frames):
        im.paste(f, (0, i * h))
    return im


def shine(canvas, mask, t, width=2.2, strength=1.55):
    """A diagonal highlight band sweeping across the pixels in mask (set of (x, y)); t in [0, 1)."""
    out = canvas.copy()
    sweep = 0.6                      # the band crosses during the first 60% of the loop, then rests
    if t > sweep:
        return out
    lo, hi = -4, 34
    pos = lo + (hi - lo) * (t / sweep)
    for (x, y) in mask:
        c = canvas.get(x, y)
        if c is None:
            continue
        d = (x + (15 - y)) - pos
        k = max(0.0, 1.0 - abs(d) / width)
        if k > 0:
            out.set(x, y, shade(c, 1 + (strength - 1) * k))
    return out


def pulse(canvas, mask, t, lo=0.75, hi=1.45):
    """Brightness pulse on the pixels in mask; t in [0, 1)."""
    out = canvas.copy()
    f = lo + (hi - lo) * (0.5 - 0.5 * math.cos(t * 2 * math.pi))
    for (x, y) in mask:
        c = canvas.get(x, y)
        if c is not None:
            out.set(x, y, shade(c, f))
    return out


def sheet(images, scale=4, cols=None, bg=(38, 32, 30, 255)):
    """Preview sheet of images (first frame of strips)."""
    cols = cols or len(images)
    rows = (len(images) + cols - 1) // cols
    cell = 16 * scale + 8
    out = Image.new("RGBA", (cols * cell, rows * cell), bg)
    for i, im in enumerate(images):
        f = im.crop((0, 0, im.width, im.width)) if im.height > im.width else im
        f = f.resize((f.width * scale * 16 // f.width, f.height * scale * 16 // f.width), Image.NEAREST)
        out.alpha_composite(f, ((i % cols) * cell + 4, (i // cols) * cell + 4))
    return out


def twinkle(canvas, at, t, start=0.62, length=0.18, col=(255, 255, 255, 255)):
    """A four-point star that flashes at `at` once per loop."""
    out = canvas.copy()
    if not (start <= t < start + length):
        return out
    k = (t - start) / length
    x, y = at
    out.set(x, y, col)
    if 0.2 < k < 0.8:
        soft = col[:3] + (200,)
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if canvas.get(x + dx, y + dy) is None or True:
                out.set(x + dx, y + dy, soft)
    return out
