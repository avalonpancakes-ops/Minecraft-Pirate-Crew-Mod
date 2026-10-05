package com.piratecrew.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** The player's Voyage Goals, plus the one just reached (for the toast), or -1. */
public class GoalSyncPacket {
    public final long mask;
    public final int reached;

    public GoalSyncPacket(long mask, int reached) {
        this.mask = mask;
        this.reached = reached;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeLong(mask);
        buf.writeVarInt(reached + 1);
    }

    public static GoalSyncPacket decode(FriendlyByteBuf buf) {
        return new GoalSyncPacket(buf.readLong(), buf.readVarInt() - 1);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.piratecrew.client.ClientGoals.update(this));
        ctx.get().setPacketHandled(true);
    }
}
