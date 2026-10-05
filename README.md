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
- Look like players, each wearing a random skin from the set bundled in the mod (`src/main/resources/assets/piratecrew/textures/entity/pirate/`). No internet needed. Every skin can be given a tier: a pirate rolled as S tier always wears one of the S skins, so you can tell a pirate's rank by its look. Rarity odds don't change with the number of skins per tier. To add skins: `python3 tools/add_skins.py --tier S captain.png --tier F deckhand1.png deckhand2.png` (change one later with `--retier pirate_3 A`). Add `--hunter` first to add debt collector skins instead (`python3 tools/add_skins.py --hunter --tier S hunter.png`). Debt collectors wear pirate skins until debt collector skins are added. The script fixes common skin problems and detects thin (Alex) arms. A tier with no skins borrows untiered skins; with no skins at all, pirates wear the default Steve/Alex skins.
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

### Pirate packs, fighting styles and jobs
- Every pirate carries an **18-slot pack** (half a player's inventory), shown under its gear in its screen. Shift-click moves items between your inventory, its gear and its pack. Everything in the pack drops when it dies.
- **Fighting style:** each pirate is a **Brawler** (charges in and switches to melee early), **Balanced** (melee up close, ranged further out) or **Marksman** (keeps its distance and only draws a blade when cornered), shown next to its tier. Each pirate also gets its own slightly different switch-over distance, so no two fight quite alike.
- **Shields:** put a shield in a pirate's off hand and it blocks. In melee it raises the shield between its own swings, and it blocks arrows flying at it and enemies drawing a bow on it. Brawlers block less (they'd rather hit). Shields wear down as they take hits, and an axe knocks a pirate's shield aside for 5 seconds, just like a player's.
- **Potions and golden apples:** put potions, splash potions, golden apples, enchanted golden apples or a milk bucket in a pirate's pack (or off hand) and it uses them. Pirates have no hunger bar, so food is only eaten for its effects.
  - **Milk:** drinks it to clear harmful effects (poison, weakness, slowness...). Unlike a player's milk, it keeps its good effects.
  - **Healing potions (drink or splash) and golden apples:** used when badly hurt in a fight. Enchanted golden apples are saved for when it's nearly dead.
  - **Fire resistance:** drunk when it's on fire.
  - **Strength, speed and resistance potions:** drunk at the start of a fight.
  - **Harmful splash and lingering potions** (poison, harming, weakness, slowness): thrown at enemies 5 to 10 blocks away, never when a crewmate is close to the target.
  - Empty bottles and buckets go back in its pack. It lowers its shield to drink and doesn't attack while drinking.
- **Building in a fight:** give a pirate solid blocks (dirt, cobblestone, planks... anything full and non-falling, but never ore or ruby blocks) in its pack and it builds like a player:
  - **towers up** under itself to reach an enemy standing above it that it can't walk to;
  - **bridges** across gaps and water toward an enemy it can't otherwise reach;
  - **walls up for cover** (3 wide, 2 high) when it has a bow out and is being shot at from range, then steps out around the wall to shoot back (at most every 20 seconds).
  Pirates on **Hold** only build cover. Debt collectors carry endless cobblestone; raiders often carry some planks or cobblestone (not on ships). Hostile pirates only build if the `mobGriefing` gamerule is on; your crew always can.
- **Rushing builders:** when a pirate's target starts placing blocks (a player walling up or towering, or an enemy pirate building cover), it notices, draws its blade and rushes in around the wall with a burst of speed before the cover is finished.
- **Weapon switching:** give a pirate both a melee weapon and a bow, crossbow or trident (one in hand, one in its pack) and it swaps between them mid-fight depending on how close the enemy is and its style. It always grabs the hardest-hitting blade it has.
- **Tasks:** press **Tasks...** in a crew pirate's screen and pick a job. It works within about 12 blocks of where it's standing when you choose:
  - **Mine Ore**: mines ore it can see (cave walls, cliffs, exposed veins). Needs a pickaxe good enough for the ore.
  - **Farm Crops**: harvests fully grown wheat, carrots, potatoes, beetroot (and modded crops) and replants them.
  - **Go Fishing**: walks to the nearest shore and fishes (fish, junk and the odd bit of treasure). Needs a fishing rod; Lure and Luck of the Sea work.
  - **Chop Wood**: fells trees from the trunk up and replants saplings. Faster with an axe.
- Tools come out of the pack automatically and wear down like a player's. What it gathers goes into its pack; when the pack is full it empties it into the **nearest chest or barrel** near its work spot, keeping its gear and up to 16 seeds or saplings for replanting. With no chest nearby it stops and tells you.
- Pirates stop working to fight, then go back to their job. **Stop Working** (or Follow, Hold or Roam) ends the job.

### Crews
- Press **I** (rebindable under Controls → Pirate Crew) or type `/crew` to open the crew screen.
- Whoever creates the crew is **Captain**. The captain can appoint **2 Vice Captains**, either players or recruited pirates (use **+Vice** / **Demote** on their row). Pirate vice captains get a ☆ on their name tag. Only player vice captains can recruit and invite.
- **Max 20 members**, of which at most **7 can be real players** — the rest have to be pirates.
- Captain and vice captains can invite players, recruit and dismiss pirates, and set the crew icon. Vice captains can kick deckhands; the captain can kick anyone, promote/demote, hand over the captaincy, rename and disband.
- **Crew icon:** hold any item and press **Set Icon**.
- **No friendly fire, ever:** crewmates (players and pirates) can't hurt or kill each other. Swords, axes and fists do nothing, arrows and tridents fly straight through crewmates (flame arrows don't light them up), a crewmate's splash or lingering potions can't harm or poison you, and TNT a crewmate lit can't hurt you.

### Bounties
- Crew members (players **and** recruited pirates) earn a ruby bounty by killing players and pirates outside their crew: **10 rubies per player**, **2 rubies per F-tier pirate +1 per tier above F** (doubled if the pirate belonged to a crew).
- **A player** who kills a wanted target from another crew claims the **whole bounty, paid into their bank account**, and adds 25% of it to their own bounty.
- **Pirates have no bank account.** When a crew pirate kills a wanted player or pirate, that crew's **captain gets 25% of the bounty** in their bank.
- The whole server is told who claimed what. Killing the same target again within 10 minutes doesn't count, so friends can't farm each other.
- A dead pirate's poster comes down for good; a player's bounty resets to 0 once claimed.
- **A player's bounty is only ever lost two ways:** killed by a member of a **rival crew** (a player, or a recruited pirate of another crew), or by the bank's **debt collectors**. Dying to zombies, other mobs, enemy raiders, free bar pirates, falling, lava or a player who isn't in a crew leaves your bounty untouched.
- Every pirate bar has a **Bounty Board** on its front wall (bars built before this update get one the next time they're loaded). Right-click it to see WANTED posters for everyone with a bounty, biggest first, with their face, name and bounty. Hover a poster for crew, tier and kills.
- Craft your own board: planks top and bottom, paper-ruby-paper in the middle.
- The crew screen shows your crew's total bounty, and hovering a member shows theirs.

### The Four Emperors of the Sea
- A crew's **total bounty** is every member's bounty added up, players and pirates alike (shown at the top right of the crew screen).
- The **4 crews with the highest total bounty** become the **Emperors of the Sea**, as long as they have at least **10,000 rubies** of combined bounty. The whole server is told when a crew claims a seat or is toppled from one. `/crew emperors` lists the current Emperors.
- Every **player** in an Emperor crew has permanent **Strength I and Resistance I**. The **captain and vice captains** get **Strength III and Resistance II**.
- The buffs go as soon as a player **leaves or is kicked** from the crew, or the crew is **overtaken** and drops out of the top four. They come back after death or drinking milk. A stronger potion still works, and the Emperor buff returns when it wears off.
- Emperor crews get a **♛ Emperor #1-4** title next to their name in the crew screen (hover it for the perks).
- The bounty needed and the number of Emperors are in the config (`emperorMinimumBounty`, `emperorCount`).

### Banks
- Every village also gets **one bank**, a stone hall with a teller counter, built on its own spot at least 20 blocks from the bar so they never overlap. Villages you've already visited get theirs the next time you go there.
- A **Banker** stands behind the counter of every bank (banks built before this update get one when you next visit). He can't be hurt, pushed or led away. Right-click him, or a **Bank Counter**, to open your account. Your balance is just a number (1,200, 1,700...), so rubies stop taking up inventory space.
- Deposit +1 / +10 / +64 / All, withdraw -1 / -10 / -64 / Max (Max = as much as fits in your inventory), or type an exact amount. Ruby blocks count as 9 rubies when depositing.
- **Shop:** press **Shop** on the bank screen to buy items with rubies: food, materials (iron, gold, diamonds, netherite...), weapons and totems, iron and diamond gear, enchanted books (Mending, Sharpness V, Fortune III...) and odds and ends like ender pearls, saddles and name tags. Click to buy, shift-click to buy 5. It's paid from your bank balance first, then rubies you're carrying.
- Bounty rewards go straight into the bank. `/bank` shows your balance anywhere; deposits and withdrawals need a counter.

### Loans and debt collectors
- The banker lends rubies: borrow **up to 500** (quick buttons for 100 / 250 / 500, or type an amount). The rubies go into your bank account. One loan at a time.
- You owe it back with **25% interest** (borrow 500, owe 625) within **7 Minecraft days**. That's in-game days, so sleeping through the night counts as a day passing. Repay from your bank balance at any bank; the bank screen shows what you owe and how many days are left. You're warned in chat when 3, 2 and 1 days are left.
- **Miss the deadline and the banker sends a debt collector after you.** A new one comes every Minecraft day until the debt is paid:
  - Day 1 an **F**-tier collector, then **D**, **C**, **B**, **A**, **S**, but only if the last one failed to get you.
  - After S: **S + F**, then S + D ... S + S, then S + S + F, and so on (up to 6 collectors a day).
- **Debt collectors are 5 times stronger than a pirate of the same tier** (an F collector has 100 HP, an S collector 280 HP and 35 base damage) They wear no armor, so you can see their skins, but they get the armor of a full set built into their stats: leather at F, then chainmail, iron, ruby, diamond and netherite at S. An F collector carries a wooden sword, a bow and a shield; a D collector a stone sword, a bow and a shield; a C collector a ruby cutlass, a bow, a shield, poison splash potions to throw at you and a milk bucket to wash off anything you throw at him; a B collector the same with a Sharpness II ruby cutlass. An **A collector** is a marksman who prefers to keep his distance: a bow with Power V, Punch II, Flame and Infinity (fire arrows), and a Sharpness II diamond sword he only draws when you get close. He has **permanent Strength**, plus a shield, poison splash potions, milk, and fire resistance potions for when he's burning. Every 5 minutes he **summons a crew of 4 pirates** in full diamond gear to help him (never more than 8 at once). They have normal pirate stats, can't be recruited, drop nothing, and leave along with him. An **S collector** charges straight at you with a netherite sword (Sharpness V, Fire Aspect II, Knockback II, Sweeping Edge III) and a shield, with permanent **Strength II, Speed II, Resistance II, Regeneration II, Fire Resistance and Water Breathing**. He doesn't summon a crew. Instead he **shoots laser beams from his eyes**: his eyes glow red for a second and a half while he aims, he locks on just before firing, and the beam does 16 damage (8 hearts) that **ignores armor** and sets you on fire. Get behind a wall or raise a shield to stop it, or sidestep at the last moment. He can fire every 8 seconds, from 4 to 32 blocks away. He also has poison splash potions and milk like the tiers below. Their weapons never break and their potions and milk never run out. Their weapons never break. They fight with the same styles as pirates, track you down if you run, and appear 20 to 30 blocks away so you see them coming.
- They can't be recruited, only hunt the player who owes the debt (and anyone who attacks them, so your crew can help), and **drop nothing**: no gear, no XP. They're a punishment, not something to farm.
- **If a collector kills you**, the bank collects, in this order:
  1. **Your bounty:** the bank claims the whole bounty on your head (announced to the server) and takes it off your debt.
  2. **Your bank account:** as much as is still owed.
  3. **Your items:** if you still owe more, the collector walks over to your corpse, crouches over it and goes through your things for a few seconds. He takes items worth what you still owe, and they're **destroyed for good**. Rubies go first, then your most valuable things (diamonds, netherite, enchanted gear, elytra, totems...) until the debt is covered. Items are valued at the banker's shop prices; everyday blocks are worthless to him. While he searches, the corpse is locked (you can't grab your stuff), but if you or your crew **kill him before he finishes, your items are safe**. When he's done he vanishes, and whatever he didn't take is still on your corpse (and the 2-minute timer keeps running, so get back fast).
  - If that still doesn't cover it, more collectors come the next day, at the same strength.
- **Paying off the debt calls the collectors off** at once.
- **No escaping through portals:** if you go to the Nether or the End (or any other dimension), the collectors follow you a few seconds later and turn up nearby.
- **No escaping by logging off:** collectors leave 30 seconds after you log out, but if you come back the same Minecraft day, the same collectors come straight back after you.

### Corpses
- When a player dies, their items don't scatter on the ground: they stay on the player's **corpse**, which lies where they died wearing their skin, with a name tag. Chat tells you its coordinates.
- For the first **2 minutes** only the dead player can loot it: **right-click the corpse** and everything comes back (armor straight back on if the slot is free) and the corpse disappears. Sneak + right-click opens it like a chest to pick items out instead.
- After 2 minutes the corpse's name tag says **(free loot)** and **anyone** can right-click it to open it like a chest and take what they want. Other players who try earlier are told how long is left. The owner can still take everything back at any time.
- Corpses can't be hurt, don't burn in lava, and never despawn while they hold items. If you die in the void, your corpse floats just above it.
- With the `keepInventory` gamerule on there's no corpse (you keep your items, and a collector takes his share straight from your inventory).

### Raider camps
- Now and then, out in the Overworld (and on Sundered Sea islands), you'll stumble on a **raider camp**: a clearing with a campfire ringed by log seats, three wool tents with bedrolls, barrels, a black flag on a pole, lanterns and a **loot chest** (rubies, food, arrows, iron, gold, the odd emerald, enchanted book, golden apple or diamond).
- An enemy pirate crew is lounging around the fire: a **captain** and 3 to 5 crew, geared exactly like the ship crews (random gear up to diamond, captain always best equipped). They wander about their camp, attack players and recruited pirates who come close, and chase intruders up to about 14 blocks from camp (archers keep shooting further out).
- Camps only appear in newly generated land, on flat dry ground (not oceans, rivers or beaches), well away from villages, bars, banks and each other. By default a camp is tried in 1 of every 250 new chunks and camps are at least 320 blocks apart; both are in the config (`generateCamps`, `campChance`, `campSpacing`). A camp's crew doesn't come back once it's been wiped out.
- `/piratecrew spawncamp` (op) builds one in front of you.

### Enemy pirate crews at sea (with Valkyrien Pirates)
- Optional: install **[Valkyrien Pirates](https://modrinth.com/mod/valkyrien-pirates)** (it needs **Valkyrien Skies** and **Eureka! Ships!**) and its pirate ships that sail the oceans get boarded by an **enemy NPC pirate crew** from this mod, in the Overworld and the Sundered Sea. Without those mods, Pirate Crew works exactly as before.
- Each ship gets a **captain** plus 3 to 8 crew (bigger ships, bigger crews), alongside Valkyrien Pirates' own helmsman and cannoneers, who still sail the ship and fire its cannons.
- **Gear is random per crew member**, from nothing up to **diamond** (leather, gold, chainmail, iron, ruby or diamond, with pieces missing here and there), swords to match, and bows or crossbows. The **captain is always the best equipped**: a full set at the crew's best level, or one level better (so a captain isn't always in diamond). Captains are B, A or S tier, carry a shield from iron gear up and a golden apple or two, and their blade and bow are enchanted at ruby/diamond level.
- They attack **players and recruited pirates** (your crew fights back and helps you), and they look out for each other: hit one and its crewmates come for you. They can't be recruited.
- On their ship they **hold the deck**: they shoot from where they stand and fight hand to hand with anyone who boards, instead of walking off into the sea.
- They drop a few rubies (the captain drops 8 to 20) and sometimes a piece of their gear. Killing one raises your bounty just like killing any other pirate of its tier (if you're in a crew).
- `/piratecrew spawnraiders` (op) brings an enemy crew to you on land for testing.

### The Sundered Sea (new dimension)
A lawless ocean world of scattered islands, ruled by pirates and hunted by the Order of the Tide.

**Getting there**
- Build a frame of **ruby blocks** exactly like a nether portal (inside 2–21 wide, 3–21 tall; corners optional).
- Buy a **Siren Conch** from any banker's shop (*Misc*, **1,000 rubies**) and use it on the frame. The conch is used up and the portal fills with sea-green swirl. Stand in it for 4 seconds to cross.
- Coordinates carry over 1:1. If there's no portal nearby on the other side, one is built for you on dry land, or on a small spruce raft out at sea. Walk back through it to return to the Overworld.

**The world**
- Open ocean (deep sea and warm shallows) dotted with islands: **Palm Isles** (grassy jungle islands), **Storm Isles** (bare rock, gravel and spruce) and **Ember Isles** (blackstone and basalt).
- Normal **villages** spawn on the islands, and they get **bars and banks** like Overworld villages. You'll also find shipwrecks, warm ocean ruins and buried treasure.
- **Raider camps** turn up on the islands (twice as often as in the Overworld). With Valkyrien Pirates installed, its ships carry enemy crews here too.
- New ores:
  - **Tidesteel Ore**, everywhere in stone and deepslate. Needs a diamond pickaxe or better.
  - **Abyssal Ore**, deep in the deepslate (below y −8). Needs a tidesteel pickaxe or better.
  - **Stormglass Ore**, on Storm Isles only. Needs an abyssal pickaxe or better.
  - Ruby ore is common here too.
- **Pact Shrines**: rare ruined rings of mossy pillars around an altar lit by soul lanterns. The altar chest always holds a **Soul Pact**, plus treasure.

**The Order of the Tide (marines)**
- Navy longcoats, teal sashes, brass buttons and black tricornes, with four ranks:

  | Rank | Health | Damage | Armor | Fights with |
  |---|---|---|---|---|
  | Recruit | 40 | 7 | 8 | Iron or tidesteel swords, sometimes a shield |
  | Rifleman | 40 | 6 | 8 | Crossbows from range |
  | Sergeant | 60 | 10 | 14 + toughness | Tidesteel sword, shield, crossbow; builds cover |
  | Captain | 120 | 14 | 20 + toughness | Abyssal sword, shield, crossbow, healing potions, golden apples |

- Every marine is stronger than any vanilla Overworld mob.
- **Who they hunt:** any player in a crew or with a bounty, every pirate NPC (crew pirates, raiders, bar pirates) and Valkyrien Pirates crews. Players with no crew and no bounty are left alone unless they attack.
- **Patrols:** squads of 2–4 wander the islands near players. **Outposts** (stone-brick yards with walls, a watchtower and a loot chest) are guarded by a garrison.
- **Drops:**
  - Marine Badges, which summon the first boss.
  - Rubies, and sometimes tidesteel.
  - Captains have a small chance to drop a Soul Pact.
- Killing marines raises your bounty: 5 for a recruit, up to 17 for a captain. Marines never claim bounties, they just hang pirates.

**Gear progression**
Ruby gear is the starting point. Each new tier is crafted from its ingot, in the usual shapes; the "cutlass" is the sword.

| Tier | Armor (helmet/chest/legs/boots) | Toughness per piece | Cutlass damage | Durability | Where the material comes from |
|---|---|---|---|---|---|
| Tidesteel | 3 / 8 / 7 / 3 | 2.5 | 8 | 1800 | Tidesteel ore, smelted |
| Abyssal | 4 / 9 / 7 / 4 | 3.5 | 10 | 2400 | 3 abyssal shards + 1 tidesteel ingot |
| Krakenbone | 5 / 11 / 9 / 5 | 5 | 13 | 3000 | 2 kraken bones + 1 abyssal ingot → 2 |
| Stormforged | 7 / 14 / 11 / 6 | 7 | 18 | 3800 | storm core + 2 stormglass + 1 krakenbone ingot → 2 |
| Leviathan | 10 / 17 / 15 / 9 | 9 | 24 | 5000 | 2 leviathan scales + 1 stormforged ingot → 2 |
| Sovereign | 14 / **20** / 18 / 13 | 12 | **31** | 8000 | 1 sovereign heart + 1 leviathan ingot → 2 |

- A full Sovereign set is **65 armor**. The vanilla armor cap of 30 is lifted, and armor above 20 now keeps reducing damage. With 30 armor you take 80% of what you'd take at 20; full Leviathan takes about 56%, full Sovereign about 47%.
- Abyssal and later gear is fireproof, and every piece adds a little knockback resistance (a full Sovereign set is immune to knockback).

**Bosses**
Each boss has a boss bar and telegraphed special attacks: flames, sparks or ink mark the ground a moment before a strike lands. Each one drops the item that summons the next.

- Summon items only work in the Sundered Sea.
- Use a land boss's item while looking at the ground. Use a sea boss's item while looking out over water at least 5 blocks deep.
- Only one of each boss can be around at a time.
- Bosses never despawn and slowly heal if left alone.
- Whoever lands the killing blow gets the boss's value added to their **bounty** (crew members only).

| # | Boss | Health | Summoned with (recipe) | Fights with | Drops |
|---|---|---|---|---|---|
| 1 | **Commodore Graves** | 600 | Signal Flare (7 marine badges + gunpowder + tidesteel ingot) | Abyssal Sharpness III cutlass and shield. Calls cannon broadsides on you. Leaps in with a crushing landing. Calls marines at 2/3 and 1/3 health, and enrages. | Kraken Lure, Commodore's Insignia, abyssal shards, tidesteel, badges, rubies, 25% Soul Pact. Bounty +150 |
| 2 | **The Kraken** | 1000 | Kraken Lure (insignia + 4 abyssal shards + 2 tropical fish + ink sac) | A ship-sized squid that hunts from below. Tentacle slams, a blinding ink cloud, a tentacle that drags you off boats and shores, geysers. Below half health: a whirlpool. | Storm Sigil, 8–14 kraken bones, abyssal ingots, ink, rubies, 30% Soul Pact. Bounty +250 |
| 3 | **Tempest Admiral Sorel** | 1600 | Storm Sigil (4 kraken bones + 4 stormglass + eye of ender) | Krakenbone Sharpness IV cutlass and multishot crossbow. Marked lightning strikes, a wind gust that throws you into the air, blinks behind archers. Below half health: a thunderstorm where lightning keeps falling on everyone, plus riflemen and sergeants. | Leviathan Horn, 4–7 storm cores, stormglass, krakenbone ingots, rubies, 35% Soul Pact. Bounty +400 |
| 4 | **The Leviathan** | 2800 | Leviathan Horn (4 storm cores + 4 prismarine crystals + nautilus shell) | A ship-sized elder guardian. Its beam charges faster as it weakens. Tail slam up close, whirlpool pull, geysers, and guardian broods at 75/50/25%. | Admiral's Warrant, 8–14 leviathan scales, stormforged ingots, heart of the sea, prismarine, rubies, 40% Soul Pact. Bounty +800 |
| 5 | **Fleet Admiral Vane, the Iron Tide** | 4500 | Admiral's Warrant (4 leviathan scales + 4 badges + insignia) | Sovereign Sharpness V / Fire Aspect II cutlass. Phase 1: dash strikes that cut through everything in a line, plus cannon fire. Phase 2 (below 2/3): two marine captains join and the whole fleet opens fire. Phase 3 (below 1/3): the Iron Tide, with resistance, speed, strength, ground-shaking shockwaves and lightning. | 3–5 sovereign hearts, leviathan ingots, netherite, 100+ rubies, a **guaranteed** Soul Pact. Bounty +1500 |

Bosses are built for crews. Bring your pirates (and their pacts).

**Soul Pacts**
Soul Pacts are found in Pact Shrines (always), sometimes in outpost chests, from boss kills and, rarely, from marine captains. There are ten:

| Pact | Gift (always on) | Power (R key) | Cooldown |
|---|---|---|---|
| Ember | Fire immunity; your hits set foes on fire | **Flame Burst**: a fan of five fireballs and a gout of flame | 8s |
| Tempest | Speed | **Thunderstrike**: lightning where you aim, arcing to 3 more foes | 10s |
| Frost | Your hits slow and chill; attackers are slowed | **Frost Nova**: freezes everything around you and ices over the sea | 12s |
| Iron | Resistance | **Iron Skin**: Resistance III and Strength for 8s | 25s |
| Gale | High jumps, no fall damage | **Gale Dash**: dash 10 blocks on the wind, knocking aside anyone in the way | 6s |
| Shadow | Night vision; invisible while sneaking | **Shadow Step**: teleport behind your target; your next hit deals double | 10s |
| Quake | Haste II | **Quake**: shatter the ground, launching everyone around you | 10s |
| Venom | Poison immunity; your hits poison | **Venom Cloud**: a lingering cloud of poison where you aim | 12s |
| Gravity | Slow fall while sneaking | **Gravity Well**: drag everything near a point together, lift it and slam it down | 14s |
| Blood | Your hits heal you for 15% of the damage | **Crimson Drain**: drain life from everyone around you to heal | 12s |

- **Binding:** use a pact to bind it to your soul, one per soul. To swap, sneak and use a different pact; the old one is lost. Your pact stays with you through death. The power key is **R** by default (Controls → Pirate Crew).
- **The price:** the sea drains every pact. In water you're weakened and slowed, your gift fades, and your power won't work.
- **Damage scaling:** power damage grows with your weapon, so a pact hits harder with a Sovereign cutlass than a ruby sword. Powers never hurt your crewmates, your crew's pirates, villagers or bankers.
- **Crew pirates:**
  - Use a pact on one of your crew's pirates to give it to them. Their whole fighting style changes:
    - Ember, Tempest, Venom and Gravity pirates fight from range.
    - The rest charge in.
  - They use the power on their own when it fits the fight. For example, an Iron pirate hardens when hurt and a Shadow pirate steps behind its target.
  - Their name shows their pact.
  - If a pact pirate dies, its pact scroll drops so it isn't lost.
- `/piratecrew clearpact` (op) frees your soul for testing.

### Commands
- `/crew` — open the crew screen
- `/crew create <name>`, `/crew invite <player>`, `/crew leave`, `/crew disband`, `/crew icon`, `/crew emperors`
- `/bank` — show your bank balance
- `/piratecrew spawnbar` / `/piratecrew spawnbank` (op) — build a bar or bank in front of you
- `/piratecrew spawnpirate <F|D|C|B|A|S>` (op) — spawn a pirate of a given tier
- `/piratecrew spawncollector <F|D|C|B|A|S>` (op) — send a test debt collector after yourself (doesn't touch your loan)
- **Debt collector spawn eggs** (creative tab, one per tier F to S): use one on a block to spawn a fully equipped test debt collector of that tier that hunts you. Like `spawncollector`, it never touches loans; fight it in survival.
- **Aggro Stick** (creative tab, works in creative mode only): right-click a mob to pick it (it glows), then right-click another mob and the two fight. Sneak + right-click a mob to set every mob within 16 blocks on it; sneak + right-click the air to clear your pick. Works on any mob, including pirates, debt collectors, villagers and vanilla monsters (mobs with no way to attack, like cows, just get chased).
- `/piratecrew loandue` (op) — make your loan overdue now, to test the collectors
- `/piratecrew spawnoutpost`, `/piratecrew spawnmarines`, `/piratecrew spawnshrine` (op) — build a marine outpost, call a marine squad, or build a Pact Shrine in front of you
- **Boss and marine spawn eggs** (creative tab) for testing; boss summon items also work anywhere in creative mode

## Config

`config/piratecrew-common.toml` (created on first launch): recruit cost per tier, crew size limits, bar restocking, bounty values, and loan size, interest, days and collector strength.
