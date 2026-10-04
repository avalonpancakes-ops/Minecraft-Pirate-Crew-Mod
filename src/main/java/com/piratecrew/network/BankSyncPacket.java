package com.piratecrew.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: bank balance (and opens the bank screen if asked). */
public class BankSyncPacket {
    public final long balance;
    public final int inventoryRubies;
    public final boolean open;

    public BankSyncPacket(long balance, int inventoryRubies, boolean open) {
        this.balance = balance;
        this.inventoryRubies = inventoryRubies;
        this.open = open;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeLong(balance);
        buf.writeVarInt(inventoryRubies);
        buf.writeBoolean(open);
    }

    public static BankSyncPacket decode(FriendlyByteBuf buf) {
        return new BankSyncPacket(buf.readLong(), buf.readVarInt(), buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.piratecrew.client.ClientPacketHandler.handleBank(this));
    }
}
