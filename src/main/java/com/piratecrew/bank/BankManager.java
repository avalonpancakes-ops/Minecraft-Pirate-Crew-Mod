package com.piratecrew.bank;

import com.piratecrew.entity.BankerEntity;
import com.piratecrew.network.BankSyncPacket;
import net.minecraft.world.phys.AABB;
import com.piratecrew.network.ModNetwork;
import com.piratecrew.registry.ModBlocks;
import com.piratecrew.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ruby bank. Players deposit and withdraw at a Bank Counter; the balance is just a number, so
 * rubies stop eating inventory space. Bounty rewards are paid straight into the bank.
 */
public class BankManager {
    /** Counter each player last opened, so deposit/withdraw only work while standing at one. */
    private static final Map<UUID, BlockPos> OPEN_AT = new HashMap<>();
    public static final int BLOCK_VALUE = 9;

    public static long balance(MinecraftServer server, UUID player) {
        return BankData.get(server).balance(player);
    }

    public static void credit(MinecraftServer server, UUID player, long amount) {
        BankData.get(server).add(player, amount);
        ServerPlayer online = server.getPlayerList().getPlayer(player);
        if (online != null && OPEN_AT.containsKey(player)) sync(online, false);
    }

    /** Take up to {@code amount} from the bank; returns how much was taken. */
    public static long debit(MinecraftServer server, UUID player, long amount) {
        return BankData.get(server).take(player, amount);
    }

    // ------------------------------------------------------------------ counter

    public static void open(ServerPlayer player, BlockPos counter) {
        OPEN_AT.put(player.getUUID(), counter.immutable());
        sync(player, true);
    }

    public static boolean atCounter(ServerPlayer player) {
        BlockPos pos = OPEN_AT.get(player.getUUID());
        if (pos == null) return false;
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 8 * 8) return false;
        if (player.level().getBlockState(pos).is(ModBlocks.BANK_COUNTER.get())) return true;
        return !player.level().getEntitiesOfClass(BankerEntity.class, new AABB(pos).inflate(2.0)).isEmpty();
    }

    public static void sync(ServerPlayer player, boolean open) {
        LoanData.Loan loan = LoanData.get(player.server).get(player.getUUID());
        long owed = loan == null ? 0 : loan.owed;
        long ticksLeft = loan == null ? 0 : loan.deadline - LoanManager.now(player.server);
        boolean overdue = loan != null && loan.defaulted;
        ModNetwork.sendTo(player, new BankSyncPacket(balance(player.server, player.getUUID()), countRubies(player.getInventory()), open,
                owed, ticksLeft, overdue, com.piratecrew.Config.LOAN_MAX.get(),
                (int) Math.round(com.piratecrew.Config.LOAN_INTEREST.get() * 100), com.piratecrew.Config.LOAN_DAYS.get()));
    }

    /** amount < 0 means "everything". Ruby blocks in the inventory count as 9 each. */
    public static void deposit(ServerPlayer player, long amount) {
        if (!atCounter(player)) return;
        int have = countRubies(player.getInventory());
        long want = amount < 0 ? have : Math.min(amount, have);
        if (want <= 0) {
            player.displayClientMessage(Component.literal("You have no rubies to deposit.").withStyle(ChatFormatting.RED), true);
            return;
        }
        int taken = takeRubies(player.getInventory(), (int) want);
        BankData.get(player.server).add(player.getUUID(), taken);
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
        sync(player, false);
    }

    /** amount < 0 means "as much as fits in the inventory". */
    public static void withdraw(ServerPlayer player, long amount) {
        if (!atCounter(player)) return;
        BankData data = BankData.get(player.server);
        long bal = data.balance(player.getUUID());
        long room = roomForRubies(player.getInventory());
        long want = amount < 0 ? Math.min(bal, room) : Math.min(amount, bal);
        if (want <= 0) {
            player.displayClientMessage(Component.literal(bal <= 0 ? "Your account is empty." : "No room in your inventory.")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        data.take(player.getUUID(), want);
        long left = want;
        while (left > 0) {
            int n = (int) Math.min(64, left);
            ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(ModItems.RUBY.get(), n));
            left -= n;
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.9F);
        sync(player, false);
    }

    // ------------------------------------------------------------------ inventory helpers

    /** Rubies in an inventory, counting ruby blocks as 9. */
    public static int countRubies(Inventory inv) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ModItems.RUBY.get())) n += s.getCount();
            else if (s.is(ModItems.RUBY_BLOCK.get())) n += s.getCount() * BLOCK_VALUE;
        }
        return n;
    }

    /**
     * Removes {@code amount} rubies, loose rubies first, then breaking ruby blocks (change is given
     * back as rubies). Returns how many were taken.
     */
    public static int takeRubies(Inventory inv, int amount) {
        int left = amount;
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ModItems.RUBY.get())) {
                int t = Math.min(left, s.getCount());
                s.shrink(t);
                left -= t;
            }
        }
        int change = 0;
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack s = inv.getItem(i);
            while (s.is(ModItems.RUBY_BLOCK.get()) && !s.isEmpty() && left > 0) {
                s.shrink(1);
                int used = Math.min(left, BLOCK_VALUE);
                left -= used;
                change += BLOCK_VALUE - used;
            }
        }
        inv.setChanged();
        if (change > 0) {
            ItemStack c = new ItemStack(ModItems.RUBY.get(), change);
            if (!inv.add(c) && !c.isEmpty()) inv.player.drop(c, false);
        }
        return amount - left;
    }

    /** How many loose rubies the main inventory can still take. */
    public static long roomForRubies(Inventory inv) {
        long room = 0;
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack s = inv.items.get(i);
            if (s.isEmpty()) room += 64;
            else if (s.is(ModItems.RUBY.get())) room += 64 - s.getCount();
        }
        return room;
    }
}
