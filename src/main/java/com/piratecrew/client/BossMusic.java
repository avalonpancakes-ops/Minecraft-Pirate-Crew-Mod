package com.piratecrew.client;

import com.piratecrew.PirateCrew;
import com.piratecrew.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * While a Pirate Crew boss bar is on screen, the boss theme plays (on the Music slider) in place of
 * the vanilla soundtrack, fading in, and fading out a few seconds after the bar goes away.
 */
@Mod.EventBusSubscriber(modid = PirateCrew.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BossMusic {
    private static long lastSeen;
    private static Theme playing;

    /** Called by the boss bar renderer each frame a pirate boss bar is drawn. */
    public static void seen() {
        lastSeen = System.currentTimeMillis();
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (playing != null && playing.isStopped()) playing = null;
        boolean recent = System.currentTimeMillis() - lastSeen < 2500;
        // with the HUD hidden (F1) the bar isn't drawn, so keep whatever is already playing
        boolean active = mc.level != null && (recent || (mc.options.hideGui && playing != null && !playing.fading));
        if (active) {
            if (playing == null) {
                playing = new Theme();
                mc.getSoundManager().play(playing);
            }
            playing.fading = false;
            mc.getMusicManager().stopPlaying();
        } else if (playing != null) {
            playing.fading = true;
        }
    }

    private static class Theme extends AbstractTickableSoundInstance {
        boolean fading;

        Theme() {
            super(ModSounds.BOSS_THEME.get(), SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            looping = true;
            delay = 0;
            relative = true;
            attenuation = Attenuation.NONE;
            volume = 0.05F;
        }

        @Override
        public void tick() {
            if (fading) {
                volume -= 0.015F;
                if (volume <= 0.01F) stop();
            } else {
                volume = Math.min(0.85F, volume + 0.04F);
            }
        }
    }
}
