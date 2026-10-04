package com.piratecrew.client;

import com.piratecrew.network.CrewSyncPacket;
import net.minecraft.client.Minecraft;

public class ClientPacketHandler {
    public static void handleCrewSync(CrewSyncPacket packet) {
        ClientCrewData.update(packet);
        Minecraft mc = Minecraft.getInstance();
        if (packet.openScreen && !(mc.screen instanceof CrewScreen)) {
            mc.setScreen(new CrewScreen());
        }
    }
}
