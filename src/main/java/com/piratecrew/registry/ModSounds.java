package com.piratecrew.registry;

import com.piratecrew.PirateCrew;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** The mod's own sounds, synthesized from scratch by tools/gen_sounds.py. */
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, PirateCrew.MODID);

    public static final RegistryObject<SoundEvent> GOAL_COMPLETE = reg("goal_complete");
    public static final RegistryObject<SoundEvent> BOUNTY_UP = reg("bounty_up");
    public static final RegistryObject<SoundEvent> RANK_UP = reg("rank_up");
    public static final RegistryObject<SoundEvent> BOSS_DEFEAT = reg("boss_defeat");
    public static final RegistryObject<SoundEvent> BOSS_HORN = reg("boss_horn");
    public static final RegistryObject<SoundEvent> PACT_BIND = reg("pact_bind");
    public static final RegistryObject<SoundEvent> COINS = reg("coins");
    public static final RegistryObject<SoundEvent> SEA_AMBIENT = reg("sea_ambient");
    public static final RegistryObject<SoundEvent> SEA_ADDITIONS = reg("sea_additions");
    public static final RegistryObject<SoundEvent> BOSS_THEME = reg("boss_theme");

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(PirateCrew.MODID, name)));
    }
}
