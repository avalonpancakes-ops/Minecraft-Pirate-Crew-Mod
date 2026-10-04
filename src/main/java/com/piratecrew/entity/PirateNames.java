package com.piratecrew.entity;

import net.minecraft.util.RandomSource;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Random pirate names. With ~230 first names, ~130 epithets, ~70 titles and ~170 surnames (plus
 * thousands of built-up surnames like "Grimwake" or "Saltmarrow") there are millions of
 * combinations, and the last few hundred names handed out are never reused.
 */
public class PirateNames {
    private static final String[] FIRST = {
            // classic & seafaring
            "Anne", "Barnaby", "Bartholomew", "Bess", "Calico", "Cutter", "Dagger", "Edward", "Esme", "Finn",
            "Flint", "Grace", "Gunner", "Hector", "Isla", "Jack", "Jonah", "Kidd", "Lottie", "Mary",
            "Morgan", "Ned", "Nell", "Olly", "Pearl", "Pete", "Quinn", "Rackham", "Ruby", "Sam",
            "Scarlet", "Silas", "Tess", "Tobias", "Ursula", "Vane", "Wren", "Will", "Zeb", "Rosa",
            "Abel", "Ada", "Agnes", "Alaric", "Albin", "Alma", "Amos", "Angus", "Archie", "Arlo",
            "Astrid", "August", "Barrow", "Basil", "Bea", "Benedict", "Bertie", "Birdie", "Blaise", "Bonnie",
            "Bram", "Briony", "Caius", "Caleb", "Callum", "Cass", "Cecil", "Celia", "Clem", "Cora",
            "Cormac", "Cyrus", "Dacey", "Darby", "Delia", "Dell", "Dex", "Dorian", "Dot", "Duncan",
            "Eben", "Edda", "Edgar", "Effie", "Elias", "Elsa", "Emmett", "Enid", "Ezra", "Fen",
            "Fergus", "Fia", "Fletcher", "Flora", "Freya", "Gale", "Garrick", "Gideon", "Gilly", "Greer",
            "Griff", "Gus", "Hal", "Hattie", "Hazel", "Helga", "Hob", "Hollis", "Hugo", "Ida",
            "Ignatius", "Ike", "Inga", "Ira", "Ivo", "Ivy", "Jago", "Jasper", "Jem", "Jessamy",
            "Jory", "Jude", "Juno", "Kai", "Kester", "Kit", "Lark", "Leif", "Lem", "Lenna",
            "Lior", "Lorcan", "Lou", "Lucius", "Lyra", "Mabel", "Mack", "Maddox", "Magda", "Malachi",
            "Mara", "Marlow", "Matilda", "Merrick", "Milo", "Mina", "Moll", "Mungo", "Nan", "Nico",
            "Nils", "Noor", "Obadiah", "Odette", "Ogden", "Olaf", "Orla", "Oswin", "Otto", "Peg",
            "Percival", "Phineas", "Pip", "Piper", "Prudence", "Quill", "Ramsey", "Reuben", "Rhea", "Roderick",
            "Roisin", "Rook", "Rory", "Rufus", "Sable", "Sadie", "Saoirse", "Saul", "Selma", "Seth",
            "Shay", "Sigrid", "Sol", "Sorrel", "Stellan", "Sven", "Tabitha", "Talon", "Thaddeus", "Thea",
            "Thorne", "Tilda", "Tobin", "Tova", "Uriah", "Valka", "Vera", "Vesper", "Vic", "Vivian",
            "Wade", "Walt", "Wendel", "Willa", "Wolf", "Xander", "Yara", "Yorick", "Yusuf", "Zara",
            "Zelda", "Zora", "Ambrose", "Constance", "Fenwick", "Horatio", "Lazarus", "Octavia", "Ronan", "Seraphina"
    };

    private static final String[] EPITHET = {
            "Salty", "One-Eye", "Black", "Red", "Mad", "Lucky", "Iron", "Stormy", "Crooked", "Bloody",
            "Silver", "Rusty", "Grim", "Quick", "Barnacle", "Gold-Tooth", "Peg-Leg", "Sly", "Dread", "Old",
            "Hook-Hand", "Scurvy", "Smiling", "Laughing", "Whistling", "Mumbling", "Gentle", "Wicked", "Two-Pistol", "Barefoot",
            "Bilge-Rat", "Brine", "Calm", "Cannonball", "Cinder", "Cold", "Copper", "Cutthroat", "Dapper", "Deadeye",
            "Dirty", "Dizzy", "Drowned", "Dusty", "Fancy", "Fearless", "Fiery", "Foggy", "Gallant", "Ghostly",
            "Gloomy", "Greasy", "Grumpy", "Gunpowder", "Handsome", "Hairy", "Hollow", "Honest", "Howling", "Hungry",
            "Jolly", "Keelhaul", "Knife-Ear", "Lanky", "Limping", "Little", "Long", "Loud", "Mangy", "Merry",
            "Midnight", "Mossy", "Muddy", "Noisy", "Nimble", "Pale", "Patchy", "Pickled", "Plucky", "Poor",
            "Pretty", "Proud", "Quiet", "Ragged", "Reckless", "Ripper", "Rum-Soaked", "Sandy", "Scar-Face", "Scrappy",
            "Shady", "Sharp", "Shifty", "Short", "Sleepy", "Slippery", "Smoky", "Sneaky", "Soggy", "Squinty",
            "Steady", "Stinky", "Stone", "Stubborn", "Swift", "Tall", "Tattooed", "Thunder", "Tidal", "Tipsy",
            "Toothless", "Tough", "Twitchy", "Ugly", "Velvet", "Wailing", "Weary", "Whiskered", "Wild", "Windy",
            "Wobbly", "Young", "Lucky-Seven", "Cracked", "Hazard", "Squall", "Tar-Hand", "Chain", "Bone", "Ember"
    };

    /** "Mara the Bold" */
    private static final String[] TITLE = {
            "Bold", "Brave", "Cruel", "Cunning", "Fierce", "Grim", "Hungry", "Mad", "Merciless", "Patient",
            "Quiet", "Red", "Restless", "Ruthless", "Sly", "Swift", "Terrible", "Unlucky", "Wise", "Wretched",
            "Butcher", "Kraken", "Shark", "Serpent", "Gull", "Albatross", "Barracuda", "Eel", "Viper", "Wolf",
            "Crow", "Raven", "Magpie", "Hound", "Storm", "Tempest", "Hurricane", "Squall", "Tide", "Wave",
            "Anchor", "Cannon", "Cutlass", "Dagger", "Hook", "Plank", "Powder Monkey", "Navigator", "Lookout", "Quartermaster",
            "Gunner", "Bosun", "Cook", "Carpenter", "Surgeon", "Smuggler", "Wrecker", "Drifter", "Castaway", "Mutineer",
            "Unsinkable", "Undying", "Forgotten", "Damned", "Fearsome", "Lucky", "Silent", "Shipless", "Thrice-Drowned", "Hanged"
    };

    private static final String[] SURNAME = {
            "Blackwater", "Bones", "Bonny", "Cutlass", "Drake", "Flintlock", "Galleon", "Gunwale", "Hook", "Keelhaul",
            "Kraken", "Marlowe", "Plank", "Read", "Rigger", "Saltbeard", "Seadog", "Sparrowhawk", "Teach", "Tidewell",
            "Abernathy", "Ashdown", "Avery", "Barlow", "Bellamy", "Blackwood", "Blight", "Brackish", "Bramble", "Brennan",
            "Brigg", "Bristow", "Calloway", "Carver", "Chandler", "Cobb", "Cogsworth", "Cole", "Corvin", "Crane",
            "Crowe", "Culpepper", "Dampier", "Darkmoor", "Davenport", "Deverell", "Dregs", "Duskwell", "Everard", "Fairweather",
            "Farrow", "Fathom", "Fenwick", "Finch", "Fletcher", "Foxglove", "Gallows", "Garrow", "Gilbert", "Gloom",
            "Goodfellow", "Gorse", "Graves", "Greaves", "Grimshaw", "Hale", "Halyard", "Hardtack", "Harlow", "Hawkins",
            "Heron", "Hollowell", "Holt", "Hornblower", "Hull", "Ironside", "Jetsam", "Kettle", "Kingsley", "Lark",
            "Latch", "Lockjaw", "Longshanks", "Lowe", "Mainsail", "Malloy", "Marsh", "Mayhew", "Merrow", "Mizzen",
            "Moorcock", "Morrow", "Mudge", "Nettle", "Nightingale", "Oakum", "Pellew", "Penhallow", "Pike", "Quarrel",
            "Quickwater", "Ragwort", "Ratcliffe", "Raven", "Redfern", "Reef", "Roberts", "Rook", "Rudder", "Rumbold",
            "Sable", "Saltmarsh", "Scuttle", "Shanty", "Sharkey", "Shipley", "Shoal", "Silvertongue", "Skerry", "Slate",
            "Starboard", "Stern", "Stormcrow", "Strand", "Swale", "Swift", "Tarrant", "Thatch", "Thorne", "Tiller",
            "Torrent", "Trelawney", "Trident", "Tuck", "Vance", "Varrow", "Wakefield", "Warrick", "Weatherby", "Whelk",
            "Whitlock", "Wick", "Winch", "Windlass", "Wolfe", "Wrack", "Yardarm", "Yorke", "Barrowclough", "Carrack",
            "Corsair", "Doubloon", "Flotsam", "Grogan", "Lantern", "Marooner", "Rumrunner", "Scrimshaw", "Tarbrush", "Wavecrest"
    };

    /** Built-up surnames: "Grim" + "wake" = "Grimwake". */
    private static final String[] SUR_START = {
            "Ash", "Black", "Blood", "Bone", "Brine", "Cold", "Coral", "Crow", "Dark", "Dead",
            "Deep", "Drift", "Dusk", "Fog", "Gold", "Grey", "Grim", "Gull", "Hollow", "Iron",
            "Kelp", "Mist", "Moon", "Night", "Pearl", "Rain", "Raven", "Red", "Reef", "Rust",
            "Salt", "Sand", "Shark", "Silver", "Squall", "Star", "Storm", "Thorn", "Tide", "Wave"
    };
    private static final String[] SUR_END = {
            "beard", "bone", "born", "brook", "burn", "bury", "by", "combe", "crest", "fall",
            "fang", "field", "fin", "fist", "ford", "gale", "grave", "hand", "hart", "haven",
            "helm", "hook", "keel", "lock", "mane", "marrow", "mast", "moor", "port", "rider",
            "ridge", "rock", "sail", "shore", "skull", "tooth", "wake", "water", "well", "wood"
    };

    private static final int REMEMBER = 400;
    private static final Deque<String> RECENT = new ArrayDeque<>();
    private static final Set<String> RECENT_SET = new HashSet<>();

    private static String pick(RandomSource r, String[] list) {
        return list[r.nextInt(list.length)];
    }

    private static String surname(RandomSource r) {
        if (r.nextInt(5) < 2) {
            String start = pick(r, SUR_START), end = pick(r, SUR_END);
            // avoid ugly doubles like "Reefford" -> fine, but skip "Ironiron"-style repeats
            if (!start.toLowerCase().endsWith(end)) return start + end;
        }
        return pick(r, SURNAME);
    }

    private static String generate(RandomSource r) {
        String first = pick(r, FIRST);
        return switch (r.nextInt(10)) {
            case 0, 1 -> pick(r, EPITHET) + " " + first;
            case 2, 3, 4 -> first + " " + surname(r);
            case 5, 6 -> pick(r, EPITHET) + " " + first + " " + surname(r);
            case 7 -> first + " the " + pick(r, TITLE);
            case 8 -> first + " \"" + pick(r, EPITHET) + "\" " + surname(r);
            default -> first + " " + surname(r) + " the " + pick(r, TITLE);
        };
    }

    /** A random name that hasn't been handed out recently. */
    public static synchronized String random(RandomSource r) {
        String name = generate(r);
        for (int i = 0; i < 20 && (RECENT_SET.contains(name) || name.length() > 30); i++) name = generate(r);
        RECENT.addLast(name);
        RECENT_SET.add(name);
        while (RECENT.size() > REMEMBER) RECENT_SET.remove(RECENT.removeFirst());
        return name;
    }
}
