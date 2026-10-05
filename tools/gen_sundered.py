#!/usr/bin/env python3
"""
Generates the data and assets for the Sundered Sea gear and materials: item/block models,
blockstates, ore loot tables, mining tags, recipes, English names and fallback textures.

Fallback textures are the mod's own ruby textures recoloured per tier. In game, the client builds
nicer ones from the player's own vanilla textures (see SunderedTexturesPack), so these only show if
that fails.
"""
import json
import os
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src/main/resources")
A = os.path.join(RES, "assets/piratecrew")
D = os.path.join(RES, "data/piratecrew")
MC = os.path.join(RES, "data/minecraft")

# Colour ramps by luminance: (lum, r, g, b). Keep in sync with SunderedTexturesPack.
PALETTES = {
    "tidesteel":   [(0, 8, 30, 36), (70, 30, 92, 104), (140, 70, 160, 168), (200, 140, 220, 218), (255, 225, 255, 250)],
    "abyssal":     [(0, 6, 4, 16), (70, 28, 18, 66), (140, 70, 48, 150), (200, 128, 98, 225), (255, 210, 190, 255)],
    "krakenbone":  [(0, 34, 16, 34), (70, 96, 66, 92), (140, 168, 140, 156), (200, 222, 206, 210), (255, 255, 248, 240)],
    "stormforged": [(0, 8, 12, 36), (70, 32, 60, 128), (140, 80, 140, 224), (200, 170, 210, 250), (255, 250, 248, 150)],
    "leviathan":   [(0, 4, 26, 22), (70, 16, 88, 72), (140, 50, 170, 136), (200, 130, 230, 190), (255, 220, 255, 235)],
    "sovereign":   [(0, 36, 6, 6), (70, 128, 26, 18), (140, 210, 110, 28), (200, 248, 196, 70), (255, 255, 246, 190)],
    "marine":      [(0, 10, 22, 40), (90, 30, 80, 100), (170, 60, 170, 170), (255, 230, 220, 140)],
    "siren":       [(0, 40, 6, 20), (80, 150, 30, 70), (160, 240, 110, 130), (255, 255, 230, 220)],
}

TIERS = ["tidesteel", "abyssal", "krakenbone", "stormforged", "leviathan", "sovereign"]
TIER_NAMES = {"tidesteel": "Tidesteel", "abyssal": "Abyssal", "krakenbone": "Krakenbone",
              "stormforged": "Stormforged", "leviathan": "Leviathan", "sovereign": "Sovereign"}
PIECES = {"helmet": "Helmet", "chestplate": "Chestplate", "leggings": "Leggings", "boots": "Boots",
          "sword": "Cutlass", "pickaxe": "Pickaxe", "axe": "Axe"}
INGOT = {"tidesteel": "tidesteel_ingot", "abyssal": "abyssal_ingot", "krakenbone": "krakenbone_ingot",
         "stormforged": "stormforged_ingot", "leviathan": "leviathan_ingot", "sovereign": "sovereign_ingot"}

# material item -> (english name, palette, fallback source in our textures)
MATERIALS = {
    "raw_tidesteel": ("Raw Tidesteel", "tidesteel", "item/ruby.png"),
    "tidesteel_ingot": ("Tidesteel Ingot", "tidesteel", "item/ruby.png"),
    "abyssal_shard": ("Abyssal Shard", "abyssal", "item/ruby.png"),
    "abyssal_ingot": ("Abyssal Ingot", "abyssal", "item/ruby.png"),
    "kraken_bone": ("Kraken Bone", "krakenbone", "item/ruby.png"),
    "krakenbone_ingot": ("Krakenbone Ingot", "krakenbone", "item/ruby.png"),
    "stormglass_shard": ("Stormglass Shard", "stormforged", "item/ruby.png"),
    "storm_core": ("Storm Core", "stormforged", "item/ruby.png"),
    "stormforged_ingot": ("Stormforged Ingot", "stormforged", "item/ruby.png"),
    "leviathan_scale": ("Leviathan Scale", "leviathan", "item/ruby.png"),
    "leviathan_ingot": ("Leviathan Ingot", "leviathan", "item/ruby.png"),
    "sovereign_heart": ("Sovereign Heart", "sovereign", "item/ruby.png"),
    "sovereign_ingot": ("Sovereign Ingot", "sovereign", "item/ruby.png"),
    "marine_badge": ("Marine Badge", "marine", "item/ruby.png"),
    "commodore_insignia": ("Commodore's Insignia", "sovereign", "item/ruby.png"),
}

ORES = {
    # block: (english, palette, fallback, drop item, min, max, fortune)
    "tidesteel_ore": ("Tidesteel Ore", "tidesteel", "block/ruby_ore.png", "raw_tidesteel", 1, 1, True),
    "deepslate_tidesteel_ore": ("Deepslate Tidesteel Ore", "tidesteel", "block/deepslate_ruby_ore.png", "raw_tidesteel", 1, 1, True),
    "abyssal_ore": ("Abyssal Ore", "abyssal", "block/deepslate_ruby_ore.png", "abyssal_shard", 1, 2, True),
    "stormglass_ore": ("Stormglass Ore", "stormforged", "block/ruby_ore.png", "stormglass_shard", 1, 3, True),
}


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)


def ramp(pal, lum):
    for i in range(1, len(pal)):
        if lum <= pal[i][0]:
            lo, hi = pal[i - 1], pal[i]
            t = (lum - lo[0]) / max(1, hi[0] - lo[0])
            return tuple(round(lo[k] + (hi[k] - lo[k]) * t) for k in (1, 2, 3))
    return pal[-1][1:]


def recolour_red(src, dst, pal):
    """Recolour the ruby-red pixels of one of our textures with a palette."""
    im = Image.open(os.path.join(A, "textures", src)).convert("RGBA")
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            mx, mn = max(r, g, b), min(r, g, b)
            reddish = r >= g and r >= b and (mx - mn) > 30 and (r - max(g, b)) > 20
            if reddish:
                lum = min(255, int((r * 0.6 + g * 0.3 + b * 0.1) * 1.25))
                nr, ng, nb = ramp(pal, lum)
                px[x, y] = (nr, ng, nb, a)
    out = os.path.join(A, "textures", dst)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    im.save(out)


def main():
    lang_path = os.path.join(A, "lang/en_us.json")
    lang = json.load(open(lang_path))

    # ---- gear
    for t in TIERS:
        pal = PALETTES[t]
        for piece, pname in PIECES.items():
            name = f"{t}_{piece}"
            handheld = piece in ("sword", "pickaxe", "axe")
            write(os.path.join(A, f"models/item/{name}.json"),
                  {"parent": "minecraft:item/handheld" if handheld else "minecraft:item/generated",
                   "textures": {"layer0": f"piratecrew:item/{name}"}})
            recolour_red(f"item/ruby_{piece}.png", f"item/{name}.png", pal)
            lang[f"item.piratecrew.{name}"] = f"{TIER_NAMES[t]} {pname}"
        recolour_red("models/armor/ruby_layer_1.png", f"models/armor/{t}_layer_1.png", pal)
        recolour_red("models/armor/ruby_layer_2.png", f"models/armor/{t}_layer_2.png", pal)

        ing = f"piratecrew:{INGOT[t]}"
        key = {"X": {"item": ing}, "S": {"item": "minecraft:stick"}}
        shapes = {
            "helmet": ["XXX", "X X"], "chestplate": ["X X", "XXX", "XXX"], "leggings": ["XXX", "X X", "X X"],
            "boots": ["X X", "X X"], "sword": ["X", "X", "S"], "pickaxe": ["XXX", " S ", " S "], "axe": ["XX", "XS", " S"],
        }
        for piece, pattern in shapes.items():
            k = {c: key[c] for c in "XS" if any(c in row for row in pattern)}
            write(os.path.join(D, f"recipes/{t}_{piece}.json"),
                  {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": pattern, "key": k,
                   "result": {"item": f"piratecrew:{t}_{piece}"}})
        # mining tag for this tier (filled below where needed)
        write(os.path.join(D, f"tags/blocks/needs_{t}_tool.json"), {"replace": False, "values": []})

    # ---- materials
    for name, (eng, pal, src) in MATERIALS.items():
        write(os.path.join(A, f"models/item/{name}.json"),
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"piratecrew:item/{name}"}})
        recolour_red(src, f"item/{name}.png", PALETTES[pal])
        lang[f"item.piratecrew.{name}"] = eng

    # ---- ores
    for name, (eng, pal, src, drop, lo, hi, fortune) in ORES.items():
        write(os.path.join(A, f"blockstates/{name}.json"), {"variants": {"": {"model": f"piratecrew:block/{name}"}}})
        write(os.path.join(A, f"models/block/{name}.json"),
              {"parent": "minecraft:block/cube_all", "textures": {"all": f"piratecrew:block/{name}"}})
        write(os.path.join(A, f"models/item/{name}.json"), {"parent": f"piratecrew:block/{name}"})
        recolour_red(src, f"block/{name}.png", PALETTES[pal])
        lang[f"block.piratecrew.{name}"] = eng
        funcs = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}, "add": False}]
        if fortune:
            funcs.append({"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"})
        funcs.append({"function": "minecraft:explosion_decay"})
        write(os.path.join(D, f"loot_tables/blocks/{name}.json"), {
            "type": "minecraft:block",
            "pools": [{"bonus_rolls": 0.0, "rolls": 1.0, "entries": [{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "name": f"piratecrew:{name}", "conditions": [{"condition": "minecraft:match_tool",
                 "predicate": {"enchantments": [{"enchantment": "minecraft:silk_touch", "levels": {"min": 1}}]}}]},
                {"type": "minecraft:item", "name": f"piratecrew:{drop}", "functions": funcs}]}]}]})

    # mining tags
    pick_path = os.path.join(MC, "tags/blocks/mineable/pickaxe.json")
    pick = json.load(open(pick_path))
    for name in ORES:
        if f"piratecrew:{name}" not in pick["values"]:
            pick["values"].append(f"piratecrew:{name}")
    write(pick_path, pick)
    write(os.path.join(MC, "tags/blocks/needs_diamond_tool.json"),
          {"replace": False, "values": ["piratecrew:tidesteel_ore", "piratecrew:deepslate_tidesteel_ore"]})
    write(os.path.join(D, "tags/blocks/needs_tidesteel_tool.json"), {"replace": False, "values": ["piratecrew:abyssal_ore"]})
    write(os.path.join(D, "tags/blocks/needs_abyssal_tool.json"), {"replace": False, "values": ["piratecrew:stormglass_ore"]})

    # ---- ingot recipes
    def cook(kind, inp, out, xp, t):
        write(os.path.join(D, f"recipes/{out}_from_{kind}.json"),
              {"type": f"minecraft:{kind}", "category": "misc", "ingredient": {"item": inp},
               "result": out if ":" in out else f"piratecrew:{out}", "experience": xp, "cookingtime": t})
    cook("smelting", "piratecrew:raw_tidesteel", "tidesteel_ingot", 1.0, 200)
    cook("blasting", "piratecrew:raw_tidesteel", "tidesteel_ingot", 1.0, 100)

    def shapeless(out, ingredients, count=1):
        write(os.path.join(D, f"recipes/{out}.json"),
              {"type": "minecraft:crafting_shapeless", "category": "misc",
               "ingredients": [{"item": i} for i in ingredients], "result": {"item": f"piratecrew:{out}", "count": count}})
    P = "piratecrew:"
    shapeless("abyssal_ingot", [P + "abyssal_shard"] * 4 + [P + "tidesteel_ingot"] * 4)
    shapeless("krakenbone_ingot", [P + "kraken_bone"] * 2 + [P + "abyssal_ingot"] * 2)
    shapeless("stormforged_ingot", [P + "storm_core"] + [P + "stormglass_shard"] * 4 + [P + "krakenbone_ingot"] * 2)
    shapeless("leviathan_ingot", [P + "leviathan_scale"] * 2 + [P + "stormforged_ingot"] * 2)
    shapeless("sovereign_ingot", [P + "sovereign_heart"] + [P + "leviathan_ingot"] * 4)

    json.dump(lang, open(lang_path, "w"), indent=2, ensure_ascii=False)
    print("generated", len(TIERS) * len(PIECES), "gear items,", len(MATERIALS), "materials,", len(ORES), "ores")


if __name__ == "__main__":
    main()
