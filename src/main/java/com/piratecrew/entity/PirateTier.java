package com.piratecrew.entity;

import com.piratecrew.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.util.RandomSource;

/**
 * Rarity tiers. F tier has exactly player stats (20 HP, 1 damage = a bare fist).
 * Stats are before any weapon or armor is given.
 */
public enum PirateTier {
    F("F", 20.0, 1.0, 35, ChatFormatting.GRAY),
    D("D", 24.0, 2.0, 25, ChatFormatting.GREEN),
    C("C", 30.0, 3.0, 18, ChatFormatting.AQUA),
    B("B", 36.0, 4.0, 12, ChatFormatting.BLUE),
    A("A", 44.0, 5.5, 7, ChatFormatting.LIGHT_PURPLE),
    S("S", 56.0, 7.0, 3, ChatFormatting.GOLD);

    public final String label;
    public final double maxHealth;
    public final double attackDamage;
    public final int spawnWeight;
    public final ChatFormatting color;

    PirateTier(String label, double maxHealth, double attackDamage, int spawnWeight, ChatFormatting color) {
        this.label = label;
        this.maxHealth = maxHealth;
        this.attackDamage = attackDamage;
        this.spawnWeight = spawnWeight;
        this.color = color;
    }

    public int cost() {
        return switch (this) {
            case F -> Config.COST_F.get();
            case D -> Config.COST_D.get();
            case C -> Config.COST_C.get();
            case B -> Config.COST_B.get();
            case A -> Config.COST_A.get();
            case S -> Config.COST_S.get();
        };
    }

    public static PirateTier byId(int id) {
        PirateTier[] v = values();
        return id >= 0 && id < v.length ? v[id] : F;
    }

    public static PirateTier random(RandomSource random) {
        int total = 0;
        for (PirateTier t : values()) total += t.spawnWeight;
        int roll = random.nextInt(total);
        for (PirateTier t : values()) {
            roll -= t.spawnWeight;
            if (roll < 0) return t;
        }
        return F;
    }

    public static PirateTier byLabel(String label) {
        for (PirateTier t : values()) if (t.label.equalsIgnoreCase(label)) return t;
        return null;
    }
}
