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

    public static void openBountyBoard(com.piratecrew.network.BountyBoardPacket packet) {
        Minecraft.getInstance().setScreen(new BountyBoardScreen(packet.posters));
    }

    public static void handleBank(com.piratecrew.network.BankSyncPacket packet) {
        BankScreen.update(packet.balance, packet.inventoryRubies);
        Minecraft mc = Minecraft.getInstance();
        if (packet.open && !(mc.screen instanceof BankScreen)) mc.setScreen(new BankScreen());
    }
}
