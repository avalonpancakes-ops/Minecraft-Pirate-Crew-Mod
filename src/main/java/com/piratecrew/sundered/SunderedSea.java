package com.piratecrew.sundered;

import com.piratecrew.PirateCrew;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** The Sundered Sea: an ocean world of islands, reached through a ruby portal lit with a Siren Conch. */
public class SunderedSea {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, PirateCrew.id("sundered_sea"));

    public static boolean isSunderedSea(Level level) {
        return level.dimension() == LEVEL;
    }
}
