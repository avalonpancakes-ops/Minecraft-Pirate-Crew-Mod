package com.piratecrew.entity;

import net.minecraft.util.RandomSource;

public class PirateNames {
    private static final String[] FIRST = {
            "Anne", "Barnaby", "Bartholomew", "Bess", "Calico", "Cutter", "Dagger", "Edward", "Esme", "Finn",
            "Flint", "Grace", "Gunner", "Hector", "Isla", "Jack", "Jonah", "Kidd", "Lottie", "Mary",
            "Morgan", "Ned", "Nell", "Olly", "Pearl", "Pete", "Quinn", "Rackham", "Ruby", "Sam",
            "Scarlet", "Silas", "Tess", "Tobias", "Ursula", "Vane", "Wren", "Will", "Zeb", "Rosa"
    };
    private static final String[] EPITHET = {
            "Salty", "One-Eye", "Black", "Red", "Mad", "Lucky", "Iron", "Stormy", "Crooked", "Bloody",
            "Silver", "Rusty", "Grim", "Quick", "Barnacle", "Gold-Tooth", "Peg-Leg", "Sly", "Dread", "Old"
    };
    private static final String[] SURNAME = {
            "Blackwater", "Bones", "Bonny", "Cutlass", "Drake", "Flintlock", "Galleon", "Gunwale", "Hook", "Keelhaul",
            "Kraken", "Marlowe", "Plank", "Read", "Rigger", "Saltbeard", "Seadog", "Sparrowhawk", "Teach", "Tidewell"
    };

    public static String random(RandomSource r) {
        String first = FIRST[r.nextInt(FIRST.length)];
        return switch (r.nextInt(3)) {
            case 0 -> EPITHET[r.nextInt(EPITHET.length)] + " " + first;
            case 1 -> first + " " + SURNAME[r.nextInt(SURNAME.length)];
            default -> EPITHET[r.nextInt(EPITHET.length)] + " " + first + " " + SURNAME[r.nextInt(SURNAME.length)];
        };
    }
}
