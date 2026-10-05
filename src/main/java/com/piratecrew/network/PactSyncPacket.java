package com.piratecrew.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** The player's Soul Pact and when its power is ready again (game time), for the HUD. */
public class PactSyncPacket {
    public final String pact;
    public final long ready, duration;

    public PactSyncPacket(String pact, long ready, long duration) {
        this.pact = pact;
        this.ready = ready;
        this.duration = duration;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(pact, 32);
        buf.writeLong(ready);
        buf.writeLong(duration);
    }

    public static PactSyncPacket decode(FriendlyByteBuf buf) {
        return new PactSyncPacket(buf.readUtf(32), buf.readLong(), buf.readLong());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.piratecrew.client.PactHud.update(this));
        ctx.get().setPacketHandled(true);
    }
}
