package com.piratecrew.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.util.RandomSource;

/**
 * How a pirate likes to fight. Each pirate also gets its own random twist on its style's ranges,
 * so no two fight quite alike.
 */
public enum CombatStyle {
    /** Charges in: switches to melee early and runs at the enemy. */
    BRAWLER("Brawler", ChatFormatting.RED, 7.0F, 11.0F, 5.0F, 1.35, 35),
    /** Uses whatever fits: melee up close, ranged at a distance. */
    BALANCED("Balanced", ChatFormatting.YELLOW, 4.0F, 6.5F, 8.0F, 1.2, 40),
    /** Hangs back: only draws a blade when cornered and keeps its distance. */
    MARKSMAN("Marksman", ChatFormatting.AQUA, 1.8F, 3.2F, 12.0F, 1.05, 25);

    public final String label;
    public final ChatFormatting color;
    /** Range of the distance below which the pirate switches to melee. */
    public final float meleeMin, meleeMax;
    /** Distance the pirate tries to keep while shooting. */
    public final float keepDistance;
    /** Speed multiplier when closing in for melee. */
    public final double chargeSpeed;
    public final int weight;

    CombatStyle(String label, ChatFormatting color, float meleeMin, float meleeMax, float keepDistance, double chargeSpeed, int weight) {
        this.label = label;
        this.color = color;
        this.meleeMin = meleeMin;
        this.meleeMax = meleeMax;
        this.keepDistance = keepDistance;
        this.chargeSpeed = chargeSpeed;
        this.weight = weight;
    }

    public static CombatStyle byId(int id) {
        CombatStyle[] v = values();
        return id >= 0 && id < v.length ? v[id] : BALANCED;
    }

    public static CombatStyle random(RandomSource r) {
        int total = 0;
        for (CombatStyle s : values()) total += s.weight;
        int roll = r.nextInt(total);
        for (CombatStyle s : values()) {
            roll -= s.weight;
            if (roll < 0) return s;
        }
        return BALANCED;
    }
}
