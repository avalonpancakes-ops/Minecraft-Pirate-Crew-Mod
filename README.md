# Pirate Crew — Forge 1.20.1 mod

Recruit player-like pirate NPCs from village bars with rubies, run a crew of up to 20, and arm them for battle.

## Building the jar

You need **Java 17** (the JDK, not just the JRE) — e.g. Eclipse Temurin 17.

1. Unzip this folder somewhere.
2. Open a terminal in the folder and run:
   - Windows: `gradlew.bat build`
   - Mac/Linux: `./gradlew build`
3. The first build downloads Minecraft and Forge (takes a few minutes).
4. Your mod is at `build/libs/piratecrew-1.0.0.jar`. Drop it into your `.minecraft/mods` folder with Forge 1.20.1 (47.x) installed.

To test without installing: `gradlew runClient` launches Minecraft with the mod.

## What's in it

### Rubies
- **Ruby Ore** and **Deepslate Ruby Ore** generate underground in every Overworld biome (Y −48 to 72, fairly common since rubies are currency).
- Needs a **stone pickaxe or better**. Drops 1–2 rubies (Fortune works), or the ore itself with Silk Touch. Smelts to rubies too.
- **Ruby tools** (Cutlass, Pickaxe, Axe, Shovel, Hoe) and **Ruby armor** sit between iron and diamond:

| | Iron | **Ruby** | Diamond |
|---|---|---|---|
| Tool durability | 250 | **900** | 1561 |
| Mining speed | 6 | **7** | 8 |
| Sword damage | 6 | **6.5** | 7 |
| Armor points (full set) | 15 | **18** | 20 |
| Armor toughness | 0 | **1** | 2 |

### Village bars
- Every village gets **exactly one** pirate tavern, built just outside the village on the flattest dry ground, with its door facing the village and a dirt path leading in.
- Each bar starts with 6 pirates and restocks to at least 3 free pirates every half Minecraft-day while a player is nearby.
- Bars appear as villages load, a second or so after you arrive. Admins can also place one with `/piratecrew spawnbar`.

### Pirates
- Look like players, wearing random skins from **The Skindex** (minecraftskins.com). The server reads a few pages of the site's "Top" list at startup and caches them in `config/piratecrew_skin_pool.txt`. If the site can't be reached it falls back to skins from **mc-heads.net**, then to vanilla Steve/Alex skins.
- Rarity tiers (stats before weapons/armor):

| Tier | Health | Base damage | How common | Cost (rubies) |
|---|---|---|---|---|
| F | 20 (same as a player) | 1 | 35% | 5 |
| D | 24 | 2 | 25% | 10 |
| C | 30 | 3 | 18% | 18 |
| B | 36 | 4 | 12% | 30 |
| A | 44 | 5.5 | 7% | 48 |
| S | 56 | 7 | 3% | 72 |

- **Right-click** a pirate to see its tier and price. **Hold rubies and right-click** to recruit (rubies are taken from anywhere in your inventory). If you're not in a crew yet, one is created for you.
- **Hit a pirate you didn't recruit and it fights back.** It won't join you afterwards while it's angry at you.
- Right-click **your own crew's** pirate to open its gear screen: armor slots, main hand and off hand, plus orders — **Follow**, **Hold** (guard this spot), **Roam** (wander nearby) and **Dismiss**.
- Crew pirates defend whoever they follow, attack what that player attacks, and fight hostile mobs (they leave creepers alone). They teleport to you if left far behind, regenerate slowly out of combat, and drop all their gear when they die.

### Crews
- Press **J** (rebindable under Controls → Pirate Crew) or type `/crew` to open the crew screen.
- Whoever creates the crew is **Captain**. The captain can appoint **2 Vice Captains**.
- **Max 20 members**, of which at most **7 can be real players** — the rest have to be pirates.
- Captain and vice captains can invite players, recruit and dismiss pirates, and set the crew icon. Vice captains can kick deckhands; the captain can kick anyone, promote/demote, hand over the captaincy, rename and disband.
- **Crew icon:** hold any item and press **Set Icon**.
- Crewmates can't hurt each other (toggle in the config).

### Commands
- `/crew` — open the crew screen
- `/crew create <name>`, `/crew invite <player>`, `/crew leave`, `/crew disband`, `/crew icon`
- `/piratecrew spawnbar` (op) — build a bar in front of you
- `/piratecrew spawnpirate <F|D|C|B|A|S>` (op) — spawn a pirate of a given tier

## Config

`config/piratecrew-common.toml` (created on first launch): recruit cost per tier, crew size limits, friendly fire, bar restocking, and skin settings (switch the skin list to `latest`, change how many pages are read, or edit the fallback usernames).
