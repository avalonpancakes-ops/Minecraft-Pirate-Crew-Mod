package com.piratecrew.network;

import com.piratecrew.bank.BankManager;
import com.piratecrew.bank.LoanManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: deposit, withdraw, borrow or repay at a bank. amount < 0 = all / as much as possible. */
public class BankActionPacket {
    public enum Action { DEPOSIT, WITHDRAW, BORROW, REPAY }

    public final Action action;
    public final long amount;

    public BankActionPacket(Action action, long amount) {
        this.action = action;
        this.amount = amount;
    }

    public BankActionPacket(boolean deposit, long amount) {
        this(deposit ? Action.DEPOSIT : Action.WITHDRAW, amount);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeLong(amount);
    }

    public static BankActionPacket decode(FriendlyByteBuf buf) {
        return new BankActionPacket(buf.readEnum(Action.class), buf.readLong());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        switch (action) {
            case DEPOSIT -> BankManager.deposit(player, amount);
            case WITHDRAW -> BankManager.withdraw(player, amount);
            case BORROW -> LoanManager.borrow(player, amount);
            case REPAY -> LoanManager.repay(player, amount);
        }
    }
}
