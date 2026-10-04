package com.piratecrew.network;

import com.piratecrew.bank.BankManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: buy from the banker's shop (index into ShopCatalog, number of bundles). */
public class ShopBuyPacket {
    public final int index;
    public final int bundles;

    public ShopBuyPacket(int index, int bundles) {
        this.index = index;
        this.bundles = bundles;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(index);
        buf.writeVarInt(bundles);
    }

    public static ShopBuyPacket decode(FriendlyByteBuf buf) {
        return new ShopBuyPacket(buf.readVarInt(), buf.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) BankManager.buy(player, index, bundles);
    }
}
