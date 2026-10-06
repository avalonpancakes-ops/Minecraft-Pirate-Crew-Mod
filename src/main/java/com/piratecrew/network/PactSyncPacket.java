package com.piratecrew.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** The player's Soul Pact, its mastery, and when each move is ready again (game time), for the HUD. */
public class PactSyncPacket {
    public final String pact;
    public final long ready, duration;
    public final int points;
    public final long techReady, techDuration, ultUntil, ultReady;

    public PactSyncPacket(String pact, long ready, long duration) {
        this(pact, ready, duration, 0, 0, 1, 0, 0);
    }

    public PactSyncPacket(String pact, long ready, long duration, int points, long techReady, long techDuration, long ultUntil, long ultReady) {
        this.pact = pact;
        this.ready = ready;
        this.duration = duration;
        this.points = points;
        this.techReady = techReady;
        this.techDuration = techDuration;
        this.ultUntil = ultUntil;
        this.ultReady = ultReady;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(pact, 32);
        buf.writeLong(ready);
        buf.writeLong(duration);
        buf.writeVarInt(points);
        buf.writeLong(techReady);
        buf.writeLong(techDuration);
        buf.writeLong(ultUntil);
        buf.writeLong(ultReady);
    }

    public static PactSyncPacket decode(FriendlyByteBuf buf) {
        return new PactSyncPacket(buf.readUtf(32), buf.readLong(), buf.readLong(), buf.readVarInt(),
                buf.readLong(), buf.readLong(), buf.readLong(), buf.readLong());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.piratecrew.client.PactHud.update(this));
        ctx.get().setPacketHandled(true);
    }
}
