package com.piratecrew.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Server -> client: everything the crew screen needs to draw. */
public class CrewSyncPacket {
    public record Member(UUID id, String name, boolean npc, int role, int tier, boolean online, int bounty, int playerKills, int pirateKills) {}
    public record InviteInfo(UUID crewId, String crewName, String inviter) {}

    public final boolean openScreen;
    public final boolean hasCrew;
    public final UUID crewId;
    public final String crewName;
    public final ItemStack icon;
    public final int myRole;
    public final int maxSize;
    public final int maxPlayers;
    public final List<Member> members;
    public final List<InviteInfo> invites;

    public CrewSyncPacket(boolean openScreen, boolean hasCrew, UUID crewId, String crewName, ItemStack icon,
                          int myRole, int maxSize, int maxPlayers, List<Member> members, List<InviteInfo> invites) {
        this.openScreen = openScreen;
        this.hasCrew = hasCrew;
        this.crewId = crewId;
        this.crewName = crewName;
        this.icon = icon;
        this.myRole = myRole;
        this.maxSize = maxSize;
        this.maxPlayers = maxPlayers;
        this.members = members;
        this.invites = invites;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(openScreen);
        buf.writeBoolean(hasCrew);
        buf.writeUUID(crewId);
        buf.writeUtf(crewName);
        buf.writeItem(icon);
        buf.writeVarInt(myRole);
        buf.writeVarInt(maxSize);
        buf.writeVarInt(maxPlayers);
        buf.writeVarInt(members.size());
        for (Member m : members) {
            buf.writeUUID(m.id());
            buf.writeUtf(m.name());
            buf.writeBoolean(m.npc());
            buf.writeVarInt(m.role());
            buf.writeVarInt(m.tier());
            buf.writeBoolean(m.online());
            buf.writeVarInt(m.bounty());
            buf.writeVarInt(m.playerKills());
            buf.writeVarInt(m.pirateKills());
        }
        buf.writeVarInt(invites.size());
        for (InviteInfo i : invites) {
            buf.writeUUID(i.crewId());
            buf.writeUtf(i.crewName());
            buf.writeUtf(i.inviter());
        }
    }

    public static CrewSyncPacket decode(FriendlyByteBuf buf) {
        boolean open = buf.readBoolean();
        boolean has = buf.readBoolean();
        UUID id = buf.readUUID();
        String name = buf.readUtf();
        ItemStack icon = buf.readItem();
        int role = buf.readVarInt();
        int maxSize = buf.readVarInt();
        int maxPlayers = buf.readVarInt();
        int n = buf.readVarInt();
        List<Member> members = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            members.add(new Member(buf.readUUID(), buf.readUtf(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        }
        int k = buf.readVarInt();
        List<InviteInfo> invites = new ArrayList<>(k);
        for (int i = 0; i < k; i++) {
            invites.add(new InviteInfo(buf.readUUID(), buf.readUtf(), buf.readUtf()));
        }
        return new CrewSyncPacket(open, has, id, name, icon, role, maxSize, maxPlayers, members, invites);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.piratecrew.client.ClientPacketHandler.handleCrewSync(this));
    }
}
