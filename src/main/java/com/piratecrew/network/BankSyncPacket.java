package com.piratecrew.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: bank balance and loan (and opens the bank screen if asked). */
public class BankSyncPacket {
    public final long balance;
    public final int inventoryRubies;
    public final boolean open;
    /** Rubies owed on the current loan, 0 = no loan. */
    public final long loanOwed;
    /** Ticks until the loan is due (negative once overdue). */
    public final long loanTicksLeft;
    public final boolean loanOverdue;
    public final int loanMax;
    public final int interestPct;
    public final int loanDays;

    public BankSyncPacket(long balance, int inventoryRubies, boolean open, long loanOwed, long loanTicksLeft,
                          boolean loanOverdue, int loanMax, int interestPct, int loanDays) {
        this.balance = balance;
        this.inventoryRubies = inventoryRubies;
        this.open = open;
        this.loanOwed = loanOwed;
        this.loanTicksLeft = loanTicksLeft;
        this.loanOverdue = loanOverdue;
        this.loanMax = loanMax;
        this.interestPct = interestPct;
        this.loanDays = loanDays;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeLong(balance);
        buf.writeVarInt(inventoryRubies);
        buf.writeBoolean(open);
        buf.writeLong(loanOwed);
        buf.writeLong(loanTicksLeft);
        buf.writeBoolean(loanOverdue);
        buf.writeVarInt(loanMax);
        buf.writeVarInt(interestPct);
        buf.writeVarInt(loanDays);
    }

    public static BankSyncPacket decode(FriendlyByteBuf buf) {
        return new BankSyncPacket(buf.readLong(), buf.readVarInt(), buf.readBoolean(), buf.readLong(), buf.readLong(),
                buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.piratecrew.client.ClientPacketHandler.handleBank(this));
    }
}
