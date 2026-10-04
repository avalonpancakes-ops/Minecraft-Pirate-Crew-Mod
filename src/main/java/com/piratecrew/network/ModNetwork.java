package com.piratecrew.network;

import com.piratecrew.PirateCrew;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String VERSION = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            PirateCrew.id("main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private static boolean registered = false;

    public static void register() {
        if (registered) return;
        registered = true;
        int id = 0;
        CHANNEL.messageBuilder(CrewActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CrewActionPacket::encode)
                .decoder(CrewActionPacket::decode)
                .consumerMainThread(CrewActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(PirateCommandPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PirateCommandPacket::encode)
                .decoder(PirateCommandPacket::decode)
                .consumerMainThread(PirateCommandPacket::handle)
                .add();
        CHANNEL.messageBuilder(BountyBoardPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BountyBoardPacket::encode)
                .decoder(BountyBoardPacket::decode)
                .consumerMainThread(BountyBoardPacket::handle)
                .add();
        CHANNEL.messageBuilder(CrewSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CrewSyncPacket::encode)
                .decoder(CrewSyncPacket::decode)
                .consumerMainThread(CrewSyncPacket::handle)
                .add();
    }

    public static void sendTo(ServerPlayer player, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }
}
