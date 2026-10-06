package com.piratecrew.network;

import com.piratecrew.pact.SoulPacts;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** A pact key was pressed: the power, the technique or the ultimate. */
public class PactAbilityPacket {
    public static final int POWER = 0, TECHNIQUE = 1, ULTIMATE = 2;
    private final int move;

    public PactAbilityPacket() {
        this(POWER);
    }

    public PactAbilityPacket(int move) {
        this.move = move;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(move);
    }

    public static PactAbilityPacket decode(FriendlyByteBuf buf) {
        return new PactAbilityPacket(buf.readByte());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            switch (move) {
                case TECHNIQUE -> SoulPacts.technique(player);
                case ULTIMATE -> SoulPacts.ultimate(player);
                default -> SoulPacts.activate(player);
            }
        }
        ctx.get().setPacketHandled(true);
    }
}
