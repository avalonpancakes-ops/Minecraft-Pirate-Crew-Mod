package com.piratecrew.client;

import com.piratecrew.goals.Goal;
import com.piratecrew.network.GoalSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/** The player's Voyage Goals as last sent by the server. */
public class ClientGoals {
    private static long mask;

    public static void update(GoalSyncPacket p) {
        mask = p.mask;
        Goal g = Goal.byId(p.reached);
        if (g != null) {
            PirateToast.show(g.icon.get(), "Goal Complete!", g.title, 0xE8B84A);
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F));
        }
    }

    public static boolean has(Goal g) {
        return (mask & g.bit()) != 0;
    }

    public static int count() {
        return Long.bitCount(mask & ((1L << Goal.values().length) - 1));
    }

    /** For screenshots. */
    public static void set(long m) {
        mask = m;
    }
}
