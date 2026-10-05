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
- Crewmates can't hurt each other (toggle in the config).

### Bounties
- Crew members (players **and** recruited pirates) earn a ruby bounty by killing players and pirates outside their crew: **10 rubies per player**, **2 rubies per F-tier pirate +1 per tier above F** (doubled if the pirate belonged to a crew).
- **A player** who kills a wanted target from another crew claims the **whole bounty, paid into their bank account**, and adds 25% of it to their own bounty.
- **Pirates have no bank account.** When a crew pirate kills a wanted player or pirate, that crew's **captain gets 25% of the bounty** in their bank.
- The whole server is told who claimed what. Killing the same target again within 10 minutes doesn't count, so friends can't farm each other.
- A dead pirate's poster comes down for good; a player's bounty resets to 0 once claimed.
- Every pirate bar has a **Bounty Board** on its front wall (bars built before this update get one the next time they're loaded). Right-click it to see WANTED posters for everyone with a bounty, biggest first, with their face, name and bounty. Hover a poster for crew, tier and kills.
- Craft your own board: planks top and bottom, paper-ruby-paper in the middle.
- The crew screen shows your crew's total bounty, and hovering a member shows theirs.

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
- Now and then, out in the Overworld, you'll stumble on a **raider camp**: a clearing with a campfire ringed by log seats, three wool tents with bedrolls, barrels, a black flag on a pole, lanterns and a **loot chest** (rubies, food, arrows, iron, gold, the odd emerald, enchanted book, golden apple or diamond).
- An enemy pirate crew is lounging around the fire: a **captain** and 3 to 5 crew, geared exactly like the ship crews (random gear up to diamond, captain always best equipped). They wander about their camp, attack players and recruited pirates who come close, and chase intruders up to about 14 blocks from camp (archers keep shooting further out).
- Camps only appear in newly generated land, on flat dry ground (not oceans, rivers or beaches), well away from villages, bars, banks and each other. By default a camp is tried in 1 of every 250 new chunks and camps are at least 320 blocks apart; both are in the config (`generateCamps`, `campChance`, `campSpacing`). A camp's crew doesn't come back once it's been wiped out.
- `/piratecrew spawncamp` (op) builds one in front of you.

### Enemy pirate crews at sea (with Valkyrien Pirates)
- Optional: install **[Valkyrien Pirates](https://modrinth.com/mod/valkyrien-pirates)** (it needs **Valkyrien Skies** and **Eureka! Ships!**) and its pirate ships that sail the oceans get boarded by an **enemy NPC pirate crew** from this mod, in the Overworld only. Without those mods, Pirate Crew works exactly as before.
- Each ship gets a **captain** plus 3 to 8 crew (bigger ships, bigger crews), alongside Valkyrien Pirates' own helmsman and cannoneers, who still sail the ship and fire its cannons.
- **Gear is random per crew member**, from nothing up to **diamond** (leather, gold, chainmail, iron, ruby or diamond, with pieces missing here and there), swords to match, and bows or crossbows. The **captain is always the best equipped**: a full set at the crew's best level, or one level better (so a captain isn't always in diamond). Captains are B, A or S tier, carry a shield from iron gear up and a golden apple or two, and their blade and bow are enchanted at ruby/diamond level.
- They attack **players and recruited pirates** (your crew fights back and helps you), and they look out for each other: hit one and its crewmates come for you. They can't be recruited.
- On their ship they **hold the deck**: they shoot from where they stand and fight hand to hand with anyone who boards, instead of walking off into the sea.
- They drop a few rubies (the captain drops 8 to 20) and sometimes a piece of their gear. Killing one raises your bounty just like killing any other pirate of its tier (if you're in a crew).
- `/piratecrew spawnraiders` (op) brings an enemy crew to you on land for testing.

### Commands
- `/crew` — open the crew screen
- `/crew create <name>`, `/crew invite <player>`, `/crew leave`, `/crew disband`, `/crew icon`
- `/bank` — show your bank balance
- `/piratecrew spawnbar` / `/piratecrew spawnbank` (op) — build a bar or bank in front of you
- `/piratecrew spawnpirate <F|D|C|B|A|S>` (op) — spawn a pirate of a given tier
- `/piratecrew spawncollector <F|D|C|B|A|S>` (op) — send a test debt collector after yourself (doesn't touch your loan)
- **Debt collector spawn eggs** (creative tab, one per tier F to S): use one on a block to spawn a fully equipped test debt collector of that tier that hunts you. Like `spawncollector`, it never touches loans; fight it in survival.
- **Aggro Stick** (creative tab, works in creative mode only): right-click a mob to pick it (it glows), then right-click another mob and the two fight. Sneak + right-click a mob to set every mob within 16 blocks on it; sneak + right-click the air to clear your pick. Works on any mob, including pirates, debt collectors, villagers and vanilla monsters (mobs with no way to attack, like cows, just get chased).
- `/piratecrew loandue` (op) — make your loan overdue now, to test the collectors

## Config

`config/piratecrew-common.toml` (created on first launch): recruit cost per tier, crew size limits, friendly fire, bar restocking, bounty values, and loan size, interest, days and collector strength.
