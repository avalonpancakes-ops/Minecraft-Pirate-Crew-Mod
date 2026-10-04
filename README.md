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
- Look like players, each wearing a random skin from the set bundled in the mod (`src/main/resources/assets/piratecrew/textures/entity/pirate/`). No internet needed. Every skin can be given a tier: a pirate rolled as S tier always wears one of the S skins, so you can tell a pirate's rank by its look. Rarity odds don't change with the number of skins per tier. To add skins: `python3 tools/add_skins.py --tier S captain.png --tier F deckhand1.png deckhand2.png` (change one later with `--retier pirate_3 A`). The script fixes common skin problems and detects thin (Alex) arms. A tier with no skins borrows untiered skins; with no skins at all, pirates wear the default Steve/Alex skins.
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
- Crew pirates defend whoever they follow, attack what that player attacks, and fight hostile mobs (they leave creepers alone). Give them a **bow, crossbow or trident** and they fight at range: they keep their distance, draw or load, and fire. Better tiers aim straighter and hit harder. Arrows are unlimited; put tipped or spectral arrows in a pirate's off hand and it shoots those (and uses them up). Power, Punch, Flame, Piercing and Multishot all work. They teleport to you if left far behind, regenerate slowly out of combat, and drop all their gear when they die.

### Crews
- Press **I** (rebindable under Controls → Pirate Crew) or type `/crew` to open the crew screen.
- Whoever creates the crew is **Captain**. The captain can appoint **2 Vice Captains**, either players or recruited pirates (use **+Vice** / **Demote** on their row). Pirate vice captains get a ☆ on their name tag. Only player vice captains can recruit and invite.
- **Max 20 members**, of which at most **7 can be real players** — the rest have to be pirates.
- Captain and vice captains can invite players, recruit and dismiss pirates, and set the crew icon. Vice captains can kick deckhands; the captain can kick anyone, promote/demote, hand over the captaincy, rename and disband.
- **Crew icon:** hold any item and press **Set Icon**.
- Crewmates can't hurt each other (toggle in the config).

### Bounties
- Crew members (players **and** recruited pirates) earn a ruby bounty by killing players and pirates outside their crew: **10 rubies per player**, **2 rubies per F-tier pirate +1 per tier above F** (doubled if the pirate belonged to a crew).
- Kill a wanted crew member and you **claim their whole bounty in rubies**. Players get the rubies straight into their inventory; if a pirate gets the kill, the rubies drop where the target fell. The whole server is told who claimed what. The killer also adds 25% of the claimed bounty to their own.
- Killing the same target again within 10 minutes doesn't count, so friends can't farm each other.
- A dead pirate's poster comes down for good; a player's bounty resets to 0 when claimed.
- Every pirate bar has a **Bounty Board** on its front wall (bars built before this update get one the next time they're loaded). Right-click it to see WANTED posters for everyone with a bounty, biggest first, with their face, name and bounty. Hover a poster for crew, tier and kills.
- Craft your own board: planks top and bottom, paper-ruby-paper in the middle.
- The crew screen shows your crew's total bounty, and hovering a member shows theirs.

### Commands
- `/crew` — open the crew screen
- `/crew create <name>`, `/crew invite <player>`, `/crew leave`, `/crew disband`, `/crew icon`
- `/piratecrew spawnbar` (op) — build a bar in front of you
- `/piratecrew spawnpirate <F|D|C|B|A|S>` (op) — spawn a pirate of a given tier

## Config

`config/piratecrew-common.toml` (created on first launch): recruit cost per tier, crew size limits, friendly fire, bar restocking and bounty values.
