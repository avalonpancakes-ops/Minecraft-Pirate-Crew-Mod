package com.piratecrew.network;

import com.piratecrew.codex.ShowcaseTools;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** A showcase tool was clicked in the Captain's Log (checked for operator permission on the server). */
public class CodexActionPacket {
    private final int action;

    public CodexActionPacket(ShowcaseTools.Action action) {
        this.action = action.ordinal();
    }

    private CodexActionPacket(int action) {
        this.action = action;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(action);
    }

    public static CodexActionPacket decode(FriendlyByteBuf buf) {
        return new CodexActionPacket(buf.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer p = ctx.get().getSender();
        if (p != null) ShowcaseTools.run(p, ShowcaseTools.Action.byId(action));
        ctx.get().setPacketHandled(true);
    }
}
