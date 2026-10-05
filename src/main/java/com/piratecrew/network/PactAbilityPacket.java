package com.piratecrew.network;

import com.piratecrew.pact.SoulPacts;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** The pact key was pressed. */
public class PactAbilityPacket {
    public void encode(FriendlyByteBuf buf) {
    }

    public static PactAbilityPacket decode(FriendlyByteBuf buf) {
        return new PactAbilityPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) SoulPacts.activate(player);
        ctx.get().setPacketHandled(true);
    }
}
