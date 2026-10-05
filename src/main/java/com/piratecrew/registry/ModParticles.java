package com.piratecrew.registry;

import com.piratecrew.PirateCrew;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** The mod's own particles: embers, frost shards, storm sparks, shadow wisps, blood drops and pact glyphs. */
public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, PirateCrew.MODID);

    public static final RegistryObject<SimpleParticleType> EMBER = PARTICLES.register("ember", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> FROST = PARTICLES.register("frost", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> SPARK = PARTICLES.register("spark", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> WISP = PARTICLES.register("wisp", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> BLOOD = PARTICLES.register("blood", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> MOTE = PARTICLES.register("mote", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> GLYPH = PARTICLES.register("glyph", () -> new SimpleParticleType(false));
}
