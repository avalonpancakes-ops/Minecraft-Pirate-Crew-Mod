package com.piratecrew.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Server -> client: the wanted posters to show on a bounty board (opens the screen). */
public class BountyBoardPacket {
    public record Poster(UUID id, boolean npc, String name, String skin, int tier, String textures, String texturesSig,
                         String crewName, int amount, int playerKills, int pirateKills) {}

    public final List<Poster> posters;

    public BountyBoardPacket(List<Poster> posters) {
        this.posters = posters;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(posters.size());
        for (Poster p : posters) {
            buf.writeUUID(p.id());
            buf.writeBoolean(p.npc());
            buf.writeUtf(p.name());
            buf.writeUtf(p.skin());
            buf.writeVarInt(p.tier());
            buf.writeUtf(p.textures(), 32767);
            buf.writeUtf(p.texturesSig(), 32767);
            buf.writeUtf(p.crewName());
            buf.writeVarInt(p.amount());
            buf.writeVarInt(p.playerKills());
            buf.writeVarInt(p.pirateKills());
        }
    }

    public static BountyBoardPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<Poster> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(new Poster(buf.readUUID(), buf.readBoolean(), buf.readUtf(), buf.readUtf(), buf.readVarInt(),
                    buf.readUtf(32767), buf.readUtf(32767), buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        }
        return new BountyBoardPacket(list);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.piratecrew.client.ClientPacketHandler.openBountyBoard(this));
    }
}
