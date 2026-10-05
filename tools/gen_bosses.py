#!/usr/bin/env python3
"""
Assets and data for the Sundered Sea bosses: summon item models, recipes and names, spawn eggs,
entity names, and fallback textures (the client normally recolours vanilla textures at load, see
SunderedTexturesPack; these only show if that fails).
"""
import json
import os
import random
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
A = os.path.join(ROOT, "src/main/resources/assets/piratecrew")
D = os.path.join(ROOT, "src/main/resources/data/piratecrew")

PAL = {
    "marine": [(0, 10, 22, 40), (90, 30, 80, 100), (170, 60, 170, 170), (255, 230, 220, 140)],
    "kraken": [(0, 20, 4, 18), (80, 90, 20, 60), (160, 170, 60, 110), (255, 250, 190, 200)],
    "storm": [(0, 8, 12, 36), (70, 32, 60, 128), (140, 80, 140, 224), (200, 170, 210, 250), (255, 250, 248, 150)],
    "leviathan": [(0, 4, 26, 22), (70, 16, 88, 72), (140, 50, 170, 136), (200, 130, 230, 190), (255, 220, 255, 235)],
    "sovereign": [(0, 36, 6, 6), (70, 128, 26, 18), (140, 210, 110, 28), (200, 248, 196, 70), (255, 255, 246, 190)],
}

SUMMONS = {
    # item: (english, palette, recipe ingredients)
    "signal_flare": ("Signal Flare", "marine", ["piratecrew:marine_badge"] * 7 + ["minecraft:gunpowder", "piratecrew:tidesteel_ingot"]),
    "kraken_lure": ("Kraken Lure", "kraken", ["piratecrew:commodore_insignia"] + ["piratecrew:abyssal_shard"] * 4
                    + ["minecraft:tropical_fish"] * 2 + ["minecraft:ink_sac"]),
    "storm_sigil": ("Storm Sigil", "storm", ["piratecrew:kraken_bone"] * 4 + ["piratecrew:stormglass_shard"] * 4 + ["minecraft:ender_eye"]),
    "leviathan_horn": ("Leviathan Horn", "leviathan", ["piratecrew:storm_core"] * 4 + ["minecraft:prismarine_crystals"] * 4
                       + ["minecraft:nautilus_shell"]),
    "admirals_warrant": ("Admiral's Warrant", "sovereign", ["piratecrew:leviathan_scale"] * 4 + ["piratecrew:marine_badge"] * 4
                         + ["piratecrew:commodore_insignia"]),
}

BOSSES = {
    "commodore": "Commodore Graves",
    "kraken": "The Kraken",
    "tempest_admiral": "Tempest Admiral Sorel",
    "leviathan": "The Leviathan",
    "fleet_admiral": "Fleet Admiral Vane",
}


def ramp(pal, lum):
    for (l0, *c0), (l1, *c1) in zip(pal, pal[1:]):
        if lum <= l1:
            t = (lum - l0) / max(1, l1 - l0)
            return tuple(int(a + (b - a) * t) for a, b in zip(c0, c1))
    return tuple(pal[-1][1:])


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)


def recolour(src, dst, pal):
    im = Image.open(os.path.join(A, "textures", src)).convert("RGBA")
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = ramp(pal, int(0.3 * r + 0.59 * g + 0.11 * b)) + (a,)
    im.save(os.path.join(A, "textures", dst))


def noise_texture(dst, w, h, pal, seed):
    rng = random.Random(seed)
    im = Image.new("RGBA", (w, h))
    px = im.load()
    for y in range(h):
        for x in range(w):
            lum = 90 + int(60 * ((x * 7 + y * 13) % 17) / 17) + rng.randint(-25, 25)
            px[x, y] = ramp(pal, max(0, min(255, lum))) + (255,)
    os.makedirs(os.path.dirname(os.path.join(A, "textures", dst)), exist_ok=True)
    im.save(os.path.join(A, "textures", dst))


def main():
    lang_path = os.path.join(A, "lang/en_us.json")
    lang = json.load(open(lang_path))
    for name, (eng, pal, ingredients) in SUMMONS.items():
        write(os.path.join(A, f"models/item/{name}.json"),
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"piratecrew:item/{name}"}})
        recolour("item/ruby.png", f"item/{name}.png", PAL[pal])
        lang[f"item.piratecrew.{name}"] = eng
        write(os.path.join(D, f"recipes/{name}.json"),
              {"type": "minecraft:crafting_shapeless", "category": "misc",
               "ingredients": [{"item": i} for i in ingredients], "result": {"item": f"piratecrew:{name}"}})
    for boss, eng in BOSSES.items():
        lang[f"entity.piratecrew.{boss}"] = eng
        lang[f"item.piratecrew.{boss}_spawn_egg"] = f"{eng.replace('The ', '')} Spawn Egg"
        write(os.path.join(A, f"models/item/{boss}_spawn_egg.json"), {"parent": "minecraft:item/template_spawn_egg"})
    noise_texture("entity/kraken.png", 64, 32, PAL["kraken"], 11)
    noise_texture("entity/leviathan.png", 64, 64, PAL["leviathan"], 12)
    json.dump(lang, open(lang_path, "w"), indent=2, ensure_ascii=False)
    print("generated", len(SUMMONS), "summon items and", len(BOSSES), "bosses")


if __name__ == "__main__":
    main()
