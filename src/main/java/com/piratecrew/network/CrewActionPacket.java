package com.piratecrew.network;

import com.piratecrew.crew.CrewManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Client -> server: a button press in the crew screen. */
public class CrewActionPacket {
    public enum Action {
        REQUEST_SYNC, CREATE, INVITE, ACCEPT, DECLINE, LEAVE, DISBAND,
        KICK, PROMOTE, DEMOTE, MAKE_CAPTAIN, RENAME, SET_ICON
    }

    private static final UUID NONE = new UUID(0, 0);

    public final Action action;
    public final String text;
    public final UUID target;

    public CrewActionPacket(Action action, String text, UUID target) {
        this.action = action;
        this.text = text == null ? "" : text;
        this.target = target == null ? NONE : target;
    }

    public static CrewActionPacket simple(Action action) {
        return new CrewActionPacket(action, "", null);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeUtf(text, 64);
        buf.writeUUID(target);
    }

    public static CrewActionPacket decode(FriendlyByteBuf buf) {
        return new CrewActionPacket(buf.readEnum(Action.class), buf.readUtf(64), buf.readUUID());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        switch (action) {
            case REQUEST_SYNC -> CrewManager.sync(player, true);
            case CREATE -> CrewManager.create(player, text);
            case INVITE -> CrewManager.inviteByName(player, text);
            case ACCEPT -> CrewManager.accept(player, target);
            case DECLINE -> CrewManager.decline(player, target);
            case LEAVE -> CrewManager.leave(player);
            case DISBAND -> CrewManager.disband(player);
            case KICK -> CrewManager.kick(player, target);
            case PROMOTE -> CrewManager.promote(player, target);
            case DEMOTE -> CrewManager.demote(player, target);
            case MAKE_CAPTAIN -> CrewManager.transferCaptain(player, target);
            case RENAME -> CrewManager.rename(player, text);
            case SET_ICON -> CrewManager.setIconFromHand(player);
        }
    }
}
