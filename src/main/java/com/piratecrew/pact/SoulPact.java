package com.piratecrew.pact;

import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

/**
 * The ten Soul Pacts of the Sundered Sea. Each binds a spirit of the sea to its holder's soul: a
 * passive gift, one active power (the pact key, R by default, for players; used on its own by crew
 * pirates), and the price every pact carries: the sea drains it. While in water the holder is weak
 * and slow and the pact goes silent.
 */
public enum SoulPact {
    //        id         name        colour                     seal rgb  power             cooldown  ranged
    EMBER("ember", "Ember", ChatFormatting.GOLD, 0xE8641E, "Flame Burst", 160, true,
            "Immune to fire; your strikes set foes ablaze.", "Hurls a fan of fireballs and a gout of flame."),
    TEMPEST("tempest", "Tempest", ChatFormatting.AQUA, 0x5AC8FF, "Thunderstrike", 200, true,
            "Swift as a squall (Speed).", "Calls lightning where you aim, arcing to nearby foes."),
    FROST("frost", "Frost", ChatFormatting.WHITE, 0xBFEFFF, "Frost Nova", 240, false,
            "Your strikes chill foes; those who strike you are slowed.", "Freezes everything around you solid."),
    IRON("iron", "Iron", ChatFormatting.GRAY, 0x9AA0A6, "Iron Skin", 500, false,
            "Hard to hurt, hard to move (Resistance, knockback resistance).", "Turns your body to iron: Resistance III and Strength."),
    GALE("gale", "Gale", ChatFormatting.GREEN, 0x9CF5B0, "Gale Dash", 120, false,
            "Light as wind: high jumps, no fall damage.", "Dashes forward on the wind, bowling over anyone in the way."),
    SHADOW("shadow", "Shadow", ChatFormatting.DARK_PURPLE, 0x5A2A82, "Shadow Step", 200, false,
            "See in the dark; vanish while sneaking.", "Steps through the shadows behind your target; the next blow strikes double."),
    QUAKE("quake", "Quake", ChatFormatting.DARK_GREEN, 0x8A6A3A, "Quake", 200, false,
            "Your arms never tire (Haste).", "Shatters the ground around you, hurling foes into the air."),
    VENOM("venom", "Venom", ChatFormatting.DARK_GREEN, 0x6ED23C, "Venom Cloud", 240, true,
            "Immune to poison; your strikes poison.", "Spits a cloud of venom where you aim."),
    GRAVITY("gravity", "Gravity", ChatFormatting.BLUE, 0x3C3CC8, "Gravity Well", 280, true,
            "Fall slowly while sneaking.", "Drags everything near the aimed point together, lifts them and slams them down."),
    BLOOD("blood", "Blood", ChatFormatting.DARK_RED, 0xB4141E, "Crimson Drain", 240, false,
            "Your strikes steal life.", "Drains the life of everything around you to heal yourself.");

    public final String id, label;
    public final ChatFormatting color;
    public final int seal;
    public final String power;
    public final int cooldown;
    /** Crew pirates bound to a ranged pact fight from range; the rest charge in. */
    public final boolean ranged;
    public final String passiveText, powerText;

    SoulPact(String id, String label, ChatFormatting color, int seal, String power, int cooldown, boolean ranged, String passiveText, String powerText) {
        this.id = id;
        this.label = label;
        this.color = color;
        this.seal = seal;
        this.power = power;
        this.cooldown = cooldown;
        this.ranged = ranged;
        this.passiveText = passiveText;
        this.powerText = powerText;
    }

    public String title() {
        return label + " Pact";
    }

    @Nullable
    public static SoulPact byId(@Nullable String id) {
        if (id == null || id.isEmpty()) return null;
        for (SoulPact p : values()) if (p.id.equals(id)) return p;
        return null;
    }
}
