package com.piratecrew.network;

import com.piratecrew.crew.CrewManager;
import com.piratecrew.entity.PirateEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: order a crew pirate (from its equipment screen). */
public class PirateCommandPacket {
    public enum Command { FOLLOW, HOLD, WANDER, DISMISS }

    public final int entityId;
    public final Command command;

    public PirateCommandPacket(int entityId, Command command) {
        this.entityId = entityId;
        this.command = command;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeEnum(command);
    }

    public static PirateCommandPacket decode(FriendlyByteBuf buf) {
        return new PirateCommandPacket(buf.readVarInt(), buf.readEnum(Command.class));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        Entity e = player.level().getEntity(entityId);
        if (!(e instanceof PirateEntity pirate) || pirate.distanceToSqr(player) > 16 * 16) return;
        if (!CrewManager.isInSameCrew(player, pirate)) return;

        switch (command) {
            case FOLLOW -> pirate.setOrders(PirateEntity.Orders.FOLLOW, player);
            case HOLD -> pirate.setOrders(PirateEntity.Orders.HOLD, player);
            case WANDER -> pirate.setOrders(PirateEntity.Orders.WANDER, player);
            case DISMISS -> {
                player.closeContainer();
                CrewManager.dismissPirate(player, pirate);
            }
        }
    }
}
