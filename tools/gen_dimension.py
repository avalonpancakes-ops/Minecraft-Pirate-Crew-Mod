#!/usr/bin/env python3
"""
Generates the Sundered Sea dimension data: dimension type, dimension, noise settings (an ocean
world with islands), biomes, ore features and the biome tags that let villages, shipwrecks and
ocean ruins (and Valkyrien Pirates ships, if installed) generate there.
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src/main/resources")
D = os.path.join(RES, "data/piratecrew")
MC = os.path.join(RES, "data/minecraft")


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)


def block(name, **props):
    s = {"Name": name}
    if props:
        s["Properties"] = {k: str(v) for k, v in props.items()}
    return s


# ---------------------------------------------------------------- dimension type & dimension
write(os.path.join(D, "dimension_type/sundered_sea.json"), {
    "ultrawarm": False, "natural": True, "piglin_safe": False, "respawn_anchor_works": False,
    "bed_works": True, "has_raids": True, "has_skylight": True, "has_ceiling": False,
    "coordinate_scale": 1.0, "ambient_light": 0.0, "logical_height": 384,
    "effects": "minecraft:overworld", "infiniburn": "#minecraft:infiniburn_overworld",
    "min_y": -64, "height": 384,
    "monster_spawn_light_level": {"type": "minecraft:uniform", "value": {"min_inclusive": 0, "max_inclusive": 7}},
    "monster_spawn_block_light_limit": 0,
})

FULL = [-1.0, 1.0]


def params(cont, temp=FULL):
    return {"temperature": temp, "humidity": FULL, "continentalness": cont, "erosion": FULL,
            "weirdness": FULL, "depth": 0.0, "offset": 0.0}


write(os.path.join(D, "dimension/sundered_sea.json"), {
    "type": "piratecrew:sundered_sea",
    "generator": {
        "type": "minecraft:noise",
        "settings": "piratecrew:sundered_sea",
        "biome_source": {"type": "minecraft:multi_noise", "biomes": [
            {"biome": "piratecrew:sundered_deep", "parameters": params([-1.0, 0.08])},
            {"biome": "piratecrew:sundered_shallows", "parameters": params([0.08, 0.29])},
            {"biome": "piratecrew:storm_isle", "parameters": params([0.29, 1.0], [-1.0, -0.22])},
            {"biome": "piratecrew:palm_isle", "parameters": params([0.29, 1.0], [-0.22, 0.42])},
            {"biome": "piratecrew:ember_isle", "parameters": params([0.29, 1.0], [0.42, 1.0])},
        ]},
    },
})

# ---------------------------------------------------------------- noise
write(os.path.join(D, "worldgen/noise/sundered_islands.json"), {"firstOctave": -8, "amplitudes": [1.0, 1.0, 0.6, 0.3, 0.15]})
write(os.path.join(D, "worldgen/noise/sundered_detail.json"), {"firstOctave": -5, "amplitudes": [1.0, 0.5, 0.25]})

islands = {"type": "minecraft:noise", "noise": "piratecrew:sundered_islands", "xz_scale": 1.0, "y_scale": 0.0}
# Surface height = 128 + 192 * offset: sea floor near y 32, islands rising where the island noise is high.
offset = {"type": "minecraft:add", "argument1": -0.5, "argument2": {
    "type": "minecraft:mul", "argument1": 1.1, "argument2": {
        "type": "minecraft:max", "argument1": 0.0, "argument2": {"type": "minecraft:add", "argument1": islands, "argument2": -0.15}}}}
gradient = {"type": "minecraft:y_clamped_gradient", "from_y": -64, "to_y": 320, "from_value": 1.0, "to_value": -1.0}
detail = {"type": "minecraft:mul", "argument1": 0.04,
          "argument2": {"type": "minecraft:noise", "noise": "piratecrew:sundered_detail", "xz_scale": 1.0, "y_scale": 1.0}}
density = {"type": "minecraft:interpolated", "argument": {
    "type": "minecraft:add", "argument1": {"type": "minecraft:add", "argument1": gradient, "argument2": offset}, "argument2": detail}}

surface = {"type": "minecraft:sequence", "sequence": [
    {"type": "minecraft:condition",
     "if_true": {"type": "minecraft:vertical_gradient", "random_name": "minecraft:bedrock_floor",
                 "true_at_and_below": {"above_bottom": 0}, "false_at_and_above": {"above_bottom": 5}},
     "then_run": {"type": "minecraft:block", "result_state": block("minecraft:bedrock")}},
    # Ember Isles: blackstone and basalt
    {"type": "minecraft:condition", "if_true": {"type": "minecraft:biome", "biome_is": ["piratecrew:ember_isle"]},
     "then_run": {"type": "minecraft:condition",
                  "if_true": {"type": "minecraft:stone_depth", "offset": 0, "surface_type": "floor", "add_surface_depth": True, "secondary_depth_range": 0},
                  "then_run": {"type": "minecraft:sequence", "sequence": [
                      {"type": "minecraft:condition",
                       "if_true": {"type": "minecraft:noise_threshold", "noise": "minecraft:surface", "min_threshold": 0.15, "max_threshold": 1.0},
                       "then_run": {"type": "minecraft:block", "result_state": block("minecraft:basalt", axis="y")}},
                      {"type": "minecraft:block", "result_state": block("minecraft:blackstone")}]}}},
    # Top block: grass on land, sand on beaches and the sea floor (gravel patches deep down)
    {"type": "minecraft:condition",
     "if_true": {"type": "minecraft:stone_depth", "offset": 0, "surface_type": "floor", "add_surface_depth": False, "secondary_depth_range": 0},
     "then_run": {"type": "minecraft:sequence", "sequence": [
         {"type": "minecraft:condition",
          "if_true": {"type": "minecraft:y_above", "anchor": {"absolute": 66}, "surface_depth_multiplier": 0, "add_stone_depth": False},
          "then_run": {"type": "minecraft:sequence", "sequence": [
              {"type": "minecraft:condition",
               "if_true": {"type": "minecraft:biome", "biome_is": ["piratecrew:storm_isle"]},
               "then_run": {"type": "minecraft:condition",
                            "if_true": {"type": "minecraft:noise_threshold", "noise": "minecraft:surface", "min_threshold": 0.25, "max_threshold": 1.0},
                            "then_run": {"type": "minecraft:block", "result_state": block("minecraft:podzol", snowy="false")}}},
              {"type": "minecraft:block", "result_state": block("minecraft:grass_block", snowy="false")}]}},
         {"type": "minecraft:condition",
          "if_true": {"type": "minecraft:y_above", "anchor": {"absolute": 52}, "surface_depth_multiplier": 0, "add_stone_depth": False},
          "then_run": {"type": "minecraft:block", "result_state": block("minecraft:sand")}},
         {"type": "minecraft:condition",
          "if_true": {"type": "minecraft:noise_threshold", "noise": "minecraft:surface", "min_threshold": 0.3, "max_threshold": 1.0},
          "then_run": {"type": "minecraft:block", "result_state": block("minecraft:gravel")}},
         {"type": "minecraft:block", "result_state": block("minecraft:sand")}]}},
    # Just below: dirt under land, sandstone under sand
    {"type": "minecraft:condition",
     "if_true": {"type": "minecraft:stone_depth", "offset": 0, "surface_type": "floor", "add_surface_depth": True, "secondary_depth_range": 2},
     "then_run": {"type": "minecraft:sequence", "sequence": [
         {"type": "minecraft:condition",
          "if_true": {"type": "minecraft:y_above", "anchor": {"absolute": 64}, "surface_depth_multiplier": 0, "add_stone_depth": False},
          "then_run": {"type": "minecraft:block", "result_state": block("minecraft:dirt")}},
         {"type": "minecraft:block", "result_state": block("minecraft:sandstone")}]}},
    {"type": "minecraft:condition",
     "if_true": {"type": "minecraft:vertical_gradient", "random_name": "minecraft:deepslate",
                 "true_at_and_below": {"absolute": 0}, "false_at_and_above": {"absolute": 8}},
     "then_run": {"type": "minecraft:block", "result_state": block("minecraft:deepslate", axis="y")}},
]}

shifted = lambda n, s: {"type": "minecraft:shifted_noise", "noise": n, "xz_scale": s, "y_scale": 0.0,
                        "shift_x": 0.0, "shift_y": 0.0, "shift_z": 0.0}
write(os.path.join(D, "worldgen/noise_settings/sundered_sea.json"), {
    "sea_level": 63,
    "disable_mob_generation": False,
    "aquifers_enabled": False,
    "ore_veins_enabled": False,
    "legacy_random_source": False,
    "default_block": block("minecraft:stone"),
    "default_fluid": block("minecraft:water", level=0),
    "noise": {"min_y": -64, "height": 384, "size_horizontal": 1, "size_vertical": 2},
    "noise_router": {
        "barrier": 0.0, "fluid_level_floodedness": 0.0, "fluid_level_spread": 0.0, "lava": 0.0,
        "temperature": shifted("minecraft:temperature", 0.25),
        "vegetation": shifted("minecraft:vegetation", 0.25),
        "continents": islands, "erosion": 0.0, "depth": 0.0, "ridges": 0.0,
        "initial_density_without_jaggedness": density,
        "final_density": density,
        "vein_toggle": 0.0, "vein_ridged": 0.0, "vein_gap": 0.0,
    },
    "spawn_target": [],
    "surface_rule": surface,
})

# ---------------------------------------------------------------- ores
def ore(name, size, targets, count, lo, hi, shape="uniform"):
    write(os.path.join(D, f"worldgen/configured_feature/{name}.json"), {
        "type": "minecraft:ore", "config": {"size": size, "discard_chance_on_air_exposure": 0.0, "targets": [
            {"target": {"predicate_type": "minecraft:tag_match", "tag": tag}, "state": block(state)} for tag, state in targets]}})
    write(os.path.join(D, f"worldgen/placed_feature/{name}.json"), {
        "feature": f"piratecrew:{name}", "placement": [
            {"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {"type": f"minecraft:{shape}",
                                                          "min_inclusive": {"absolute": lo}, "max_inclusive": {"absolute": hi}}},
            {"type": "minecraft:biome"}]})


STONE, DEEP = "minecraft:stone_ore_replaceables", "minecraft:deepslate_ore_replaceables"
ore("ore_ruby_sea", 8, [(STONE, "piratecrew:ruby_ore"), (DEEP, "piratecrew:deepslate_ruby_ore")], 16, -48, 110, "trapezoid")
ore("ore_tidesteel", 6, [(STONE, "piratecrew:tidesteel_ore"), (DEEP, "piratecrew:deepslate_tidesteel_ore")], 9, -40, 90)
ore("ore_abyssal", 4, [(DEEP, "piratecrew:abyssal_ore")], 6, -64, -8)
ore("ore_stormglass", 4, [(STONE, "piratecrew:stormglass_ore")], 8, 20, 140)

# ---------------------------------------------------------------- biomes
# One master order per generation step; every biome uses a subset in this order (Minecraft
# rejects dimensions whose biomes list shared features in different orders).
ORES = ["minecraft:ore_dirt", "minecraft:ore_gravel", "minecraft:ore_coal_upper", "minecraft:ore_coal_lower",
        "minecraft:ore_iron_upper", "minecraft:ore_iron_middle", "minecraft:ore_gold", "minecraft:ore_redstone",
        "minecraft:ore_diamond", "minecraft:ore_lapis", "minecraft:ore_copper",
        "piratecrew:ore_ruby_sea", "piratecrew:ore_tidesteel", "piratecrew:ore_abyssal", "piratecrew:ore_stormglass"]
VEG = ["minecraft:trees_sparse_jungle", "minecraft:trees_taiga", "minecraft:patch_large_fern",
       "minecraft:patch_grass_jungle", "minecraft:patch_grass_taiga", "minecraft:flower_default",
       "minecraft:patch_berry_common", "minecraft:patch_sugar_cane", "minecraft:patch_melon",
       "minecraft:patch_dead_bush", "minecraft:seagrass_warm", "minecraft:seagrass_deep",
       "minecraft:kelp_cold", "minecraft:sea_pickle", "minecraft:warm_ocean_vegetation"]
LAKES = ["minecraft:lake_lava_surface"]
SPRINGS = ["minecraft:spring_water", "minecraft:spring_lava"]


def subset(master, wanted):
    return [f for f in master if f in wanted]


def spawn(t, w, lo, hi):
    return {"type": t, "weight": w, "minCount": lo, "maxCount": hi}


NIGHT = [spawn("minecraft:zombie", 95, 4, 4), spawn("minecraft:skeleton", 100, 4, 4), spawn("minecraft:spider", 100, 4, 4),
         spawn("minecraft:creeper", 100, 4, 4), spawn("minecraft:enderman", 10, 1, 4), spawn("minecraft:witch", 5, 1, 1)]
SEA_LIFE = {"water_creature": [spawn("minecraft:dolphin", 2, 1, 2), spawn("minecraft:squid", 4, 1, 4)],
            "water_ambient": [spawn("minecraft:tropical_fish", 25, 8, 8), spawn("minecraft:cod", 10, 3, 6), spawn("minecraft:pufferfish", 5, 1, 3)]}


def biome(name, temp, downfall, effects, spawners, ores, veg, lakes=(), springs=(), precipitation=True):
    sp = {k: [] for k in ["monster", "creature", "ambient", "axolotls", "underground_water_creature",
                          "water_creature", "water_ambient", "misc"]}
    sp.update(spawners)
    features = [[] for _ in range(11)]
    features[1] = subset(LAKES, lakes)
    features[6] = subset(ORES, ores)
    features[8] = subset(SPRINGS, springs)
    features[9] = subset(VEG, veg)
    features[10] = ["minecraft:freeze_top_layer"]
    eff = {"mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0}}
    eff.update(effects)
    write(os.path.join(D, f"worldgen/biome/{name}.json"), {
        "has_precipitation": precipitation, "temperature": temp, "downfall": downfall, "effects": eff,
        "spawners": sp, "spawn_costs": {}, "carvers": {"air": []}, "features": features})


COMMON_ORES = ORES[:14]
biome("sundered_deep", 0.8, 0.5,
      {"sky_color": 0x78A7FF, "fog_color": 0xC0D8FF, "water_color": 0x1B6C7A, "water_fog_color": 0x05282E},
      dict(SEA_LIFE, monster=[spawn("minecraft:drowned", 5, 1, 1)]),
      COMMON_ORES, ["minecraft:seagrass_deep", "minecraft:kelp_cold"])
biome("sundered_shallows", 0.9, 0.5,
      {"sky_color": 0x78A7FF, "fog_color": 0xC0D8FF, "water_color": 0x2FB5B0, "water_fog_color": 0x0A3E3C},
      SEA_LIFE, COMMON_ORES, ["minecraft:seagrass_warm", "minecraft:sea_pickle", "minecraft:warm_ocean_vegetation"])
biome("palm_isle", 0.95, 0.9,
      {"sky_color": 0x77ADFF, "fog_color": 0xC0D8FF, "water_color": 0x3FD3C9, "water_fog_color": 0x0C4A46,
       "grass_color": 0x5DC83C, "foliage_color": 0x36C11A},
      {"monster": NIGHT, "creature": [spawn("minecraft:parrot", 40, 1, 2), spawn("minecraft:chicken", 10, 2, 4),
                                       spawn("minecraft:pig", 8, 2, 4), spawn("minecraft:ocelot", 2, 1, 1)], **SEA_LIFE},
      COMMON_ORES, ["minecraft:trees_sparse_jungle", "minecraft:patch_grass_jungle", "minecraft:flower_default",
                    "minecraft:patch_sugar_cane", "minecraft:patch_melon"], springs=["minecraft:spring_water"])
biome("storm_isle", 0.3, 0.9,
      {"sky_color": 0x6B7787, "fog_color": 0x8E99A6, "water_color": 0x2E5A63, "water_fog_color": 0x0A1E22,
       "grass_color": 0x3D6E4C, "foliage_color": 0x2F5E3E},
      {"monster": NIGHT, "creature": [spawn("minecraft:sheep", 12, 2, 4), spawn("minecraft:wolf", 6, 2, 4),
                                       spawn("minecraft:rabbit", 4, 2, 3)], **SEA_LIFE},
      ORES, ["minecraft:trees_taiga", "minecraft:patch_large_fern", "minecraft:patch_grass_taiga",
             "minecraft:patch_berry_common"], springs=["minecraft:spring_water"])
biome("ember_isle", 1.6, 0.0,
      {"sky_color": 0xB58A70, "fog_color": 0xC9957A, "water_color": 0x3A8C86, "water_fog_color": 0x123433,
       "grass_color": 0x8A7A3C, "foliage_color": 0x7C6A30,
       "particle": {"options": {"type": "minecraft:white_ash"}, "probability": 0.006}},
      {"monster": NIGHT, "creature": [], **SEA_LIFE},
      COMMON_ORES, ["minecraft:patch_dead_bush"], lakes=["minecraft:lake_lava_surface"],
      springs=["minecraft:spring_lava"], precipitation=False)

# ---------------------------------------------------------------- structures & tags
ISLES = ["piratecrew:palm_isle", "piratecrew:storm_isle", "piratecrew:ember_isle"]
SEAS = ["piratecrew:sundered_deep", "piratecrew:sundered_shallows"]


def tag(path, values):
    write(os.path.join(MC, f"tags/worldgen/biome/{path}.json"), {"replace": False, "values": values})


tag("has_structure/village_plains", ["piratecrew:palm_isle"])
tag("has_structure/village_taiga", ["piratecrew:storm_isle"])
tag("has_structure/village_desert", ["piratecrew:ember_isle"])
tag("has_structure/shipwreck", SEAS)
tag("has_structure/shipwreck_beached", ISLES)
tag("has_structure/ocean_ruin_warm", SEAS)
tag("has_structure/buried_treasure", ISLES)
tag("is_ocean", SEAS)
tag("is_deep_ocean", ["piratecrew:sundered_deep"])
write(os.path.join(D, "tags/worldgen/biome/sundered_sea.json"), {"replace": False, "values": SEAS + ISLES})
write(os.path.join(D, "tags/worldgen/biome/sundered_isles.json"), {"replace": False, "values": ISLES})
# Valkyrien Pirates ships sail here too (ignored if that mod isn't installed).
write(os.path.join(RES, "data/pirates/tags/worldgen/biome/deep_n_warm.json"),
      {"replace": False, "values": [{"id": b, "required": False} for b in SEAS]})
print("dimension data written")
