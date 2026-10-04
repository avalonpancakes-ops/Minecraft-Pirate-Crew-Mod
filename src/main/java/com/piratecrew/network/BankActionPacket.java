package com.piratecrew.network;

import com.piratecrew.bank.BankManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: deposit or withdraw at a Bank Counter. amount < 0 = all / as much as fits. */
public class BankActionPacket {
    public final boolean deposit;
    public final long amount;

    public BankActionPacket(boolean deposit, long amount) {
        this.deposit = deposit;
        this.amount = amount;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(deposit);
        buf.writeLong(amount);
    }

    public static BankActionPacket decode(FriendlyByteBuf buf) {
        return new BankActionPacket(buf.readBoolean(), buf.readLong());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        if (deposit) BankManager.deposit(player, amount);
        else BankManager.withdraw(player, amount);
    }
}
