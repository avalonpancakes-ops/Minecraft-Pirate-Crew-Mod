package com.piratecrew.bank;

import com.piratecrew.Config;
import com.piratecrew.bounty.BountyData;
import com.piratecrew.crew.Crew;
import com.piratecrew.crew.CrewData;
import com.piratecrew.crew.CrewManager;
import com.piratecrew.entity.BountyHunterEntity;
import com.piratecrew.entity.CorpseEntity;
import net.minecraft.world.entity.Entity;
import com.piratecrew.entity.PirateTier;
import com.piratecrew.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ruby loans. A player borrows up to 500 rubies from the banker (paid into their bank account) and
 * owes it back with 25% interest within 15 Minecraft days. Miss the deadline and the bank sends a
 * bounty hunter every Minecraft day: F tier first, then D, C, B, A, S. A wave that fails to get the
 * player makes the next one stronger; after S come S+F, S+D ... S+S, S+S+F and so on. Paying the
 * debt off calls the hunters home.
 */
public class LoanManager {
    public static final long DAY = 24000L;

    public static long now(MinecraftServer server) {
        return server.overworld().getDayTime();
    }

    public static long owedFor(long amount) {
        return amount + (long) Math.ceil(amount * Config.LOAN_INTEREST.get() - 1e-9);
    }

    // ------------------------------------------------------------------ borrowing & repaying

    public static void borrow(ServerPlayer player, long amount) {
        if (!BankManager.atCounter(player)) return;
        LoanData data = LoanData.get(player.server);
        if (data.get(player.getUUID()) != null) {
            player.displayClientMessage(Component.literal("Pay off your current loan before borrowing again.").withStyle(ChatFormatting.RED), true);
            return;
        }
        long max = Config.LOAN_MAX.get();
        long amt = Math.min(Math.max(amount, 0), max);
        if (amt <= 0) return;
        LoanData.Loan l = data.create(player.getUUID());
        l.principal = amt;
        l.owed = owedFor(amt);
        l.deadline = now(player.server) + Config.LOAN_DAYS.get() * DAY;
        data.setDirty();
        BankData.get(player.server).add(player.getUUID(), amt);
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.sendSystemMessage(Component.literal("Banker: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(String.format("%,d rubies are in your account. You owe me %,d rubies within %d days. Don't make me send someone.",
                        amt, l.owed, Config.LOAN_DAYS.get())).withStyle(ChatFormatting.YELLOW)));
        BankManager.sync(player, false);
    }

    /** Repay from the bank balance. amount < 0 = as much as possible. */
    public static void repay(ServerPlayer player, long amount) {
        if (!BankManager.atCounter(player)) return;
        LoanData data = LoanData.get(player.server);
        LoanData.Loan l = data.get(player.getUUID());
        if (l == null) return;
        BankData bank = BankData.get(player.server);
        long want = amount < 0 ? l.owed : Math.min(amount, l.owed);
        long paid = bank.take(player.getUUID(), want);
        if (paid <= 0) {
            player.displayClientMessage(Component.literal("Deposit rubies first: repayments come out of your bank balance.").withStyle(ChatFormatting.RED), true);
            return;
        }
        l.owed -= paid;
        data.setDirty();
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.8F);
        if (l.owed <= 0) {
            boolean hunted = l.defaulted;
            data.remove(player.getUUID());
            player.sendSystemMessage(Component.literal("Banker: ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(hunted ? "Paid in full, at last. I'll call off the collectors." : "Paid in full. A pleasure doing business.")
                            .withStyle(ChatFormatting.GREEN)));
        } else {
            player.displayClientMessage(Component.literal(String.format("Paid %,d rubies. You still owe %,d.", paid, l.owed)).withStyle(ChatFormatting.YELLOW), true);
        }
        BankManager.sync(player, false);
    }

    // ------------------------------------------------------------------ hunters

    /** Hunters of this wave keep hunting only while their wave is the current one. */
    public static boolean huntActive(MinecraftServer server, UUID debtor, int serial) {
        LoanData.Loan l = LoanData.get(server).get(debtor);
        return l != null && l.defaulted && l.serial == serial && !l.waveDone;
    }

    @Nullable
    public static LoanData.Loan loanOf(MinecraftServer server, UUID player) {
        return LoanData.get(server).get(player);
    }

    /** Tiers in wave n: one more S hunter for every six waves, plus F, D, C, B, A or S. */
    public static List<PirateTier> waveTiers(int wave) {
        List<PirateTier> tiers = new ArrayList<>();
        PirateTier[] all = PirateTier.rolled();
        for (int i = 0; i < wave / all.length; i++) tiers.add(PirateTier.S);
        tiers.add(all[wave % all.length]);
        int max = Config.HUNTER_MAX_PER_WAVE.get();
        while (tiers.size() > max) tiers.remove(tiers.size() - 1);
        return tiers;
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        LoanData data = LoanData.get(server);
        if (data.all().isEmpty()) return;
        long now = now(server);
        for (LoanData.Loan l : new ArrayList<>(data.all())) {
            ServerPlayer p = server.getPlayerList().getPlayer(l.player);
            if (!l.defaulted) {
                if (now >= l.deadline) {
                    l.defaulted = true;
                    l.nextWaveAt = now;
                    data.setDirty();
                } else if (p != null) {
                    int daysLeft = (int) Math.ceil((l.deadline - now) / (double) DAY);
                    if (daysLeft <= 3 && daysLeft < l.lastReminder) {
                        l.lastReminder = daysLeft;
                        data.setDirty();
                        p.sendSystemMessage(Component.literal("⚠ ").withStyle(ChatFormatting.GOLD)
                                .append(Component.literal(String.format("Your loan of %,d rubies is due in %d day%s. Repay it at a bank or the bank sends debt collectors.",
                                        l.owed, daysLeft, daysLeft == 1 ? "" : "s")).withStyle(ChatFormatting.YELLOW)));
                    }
                }
            }
            if (l.defaulted && p != null && now >= l.nextWaveAt && p.isAlive() && !p.isSpectator()) {
                sendWave(p, l, now);
                data.setDirty();
            } else if (l.defaulted && l.resumeWave && p != null && p.tickCount > 200 && p.isAlive() && !p.isSpectator() && !l.waveDone) {
                resumeWave(p, l);
                data.setDirty();
            }
        }
    }

    private static List<BountyHunterEntity> spawnHunters(ServerPlayer p, List<PirateTier> tiers, int serial) {
        ServerLevel level = p.serverLevel();
        List<BountyHunterEntity> sent = new ArrayList<>();
        for (PirateTier tier : tiers) {
            Vec3 pos = findSpot(level, p.blockPosition(), 20, 32, p.getRandom());
            if (pos == null) pos = findSpot(level, p.blockPosition(), 8, 20, p.getRandom());
            if (pos == null) pos = findSpot(level, p.blockPosition(), 3, 8, p.getRandom());
            if (pos == null) continue;
            BountyHunterEntity h = ModEntities.BOUNTY_HUNTER.get().create(level);
            if (h == null) continue;
            h.moveTo(pos.x, pos.y, pos.z, p.getRandom().nextFloat() * 360F, 0);
            h.setupHunter(tier, p.getUUID(), serial);
            h.finalizeSpawn(level, level.getCurrentDifficultyAt(h.blockPosition()), MobSpawnType.EVENT, null, null);
            level.addFreshEntity(h);
            sent.add(h);
        }
        return sent;
    }

    /** Today's hunters gave up because the player logged off: note it so they return with the player. */
    public static void huntersLeftForLogout(MinecraftServer server, UUID debtor, int serial) {
        LoanData data = LoanData.get(server);
        LoanData.Loan l = data.get(debtor);
        if (l != null && l.defaulted && l.serial == serial && !l.waveDone && !l.resumeWave) {
            l.resumeWave = true;
            data.setDirty();
        }
    }

    /** The player came back online during the same day: today's hunters pick up the trail again. */
    private static void resumeWave(ServerPlayer p, LoanData.Loan l) {
        List<BountyHunterEntity> sent = spawnHunters(p, waveTiers(l.wave), l.serial);
        if (sent.isEmpty()) return;
        l.resumeWave = false;
        p.sendSystemMessage(Component.literal("\u2620 Logging off won't save you. The bank's collectors have picked up your trail again!").withStyle(ChatFormatting.DARK_RED));
        p.playNotifySound(SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.0F, 0.7F);
    }

    private static void sendWave(ServerPlayer p, LoanData.Loan l, long now) {
        boolean first = l.serial == 0;
        // The last wave failed to get the player: tomorrow's is stronger.
        int wave = first || l.waveDone ? l.wave : l.wave + 1;
        List<PirateTier> tiers = waveTiers(wave);
        ServerLevel level = p.serverLevel();
        int serial = l.serial + 1;

        List<BountyHunterEntity> sent = spawnHunters(p, tiers, serial);
        if (sent.isEmpty()) {
            l.nextWaveAt = now + 600; // nowhere to stand right now; try again in 30 seconds
            return;
        }
        l.wave = wave;
        l.serial = serial;
        l.waveDone = false;
        l.resumeWave = false;
        l.nextWaveAt = now + DAY;

        if (first) {
            p.sendSystemMessage(Component.literal("Banker: ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(String.format("Your %,d rubies are overdue. I warned you.", l.owed)).withStyle(ChatFormatting.RED)));
        }
        MutableComponent msg = Component.literal("☠ The bank has sent ").withStyle(ChatFormatting.DARK_RED);
        for (int i = 0; i < sent.size(); i++) {
            BountyHunterEntity h = sent.get(i);
            if (i > 0) msg.append(Component.literal(i == sent.size() - 1 ? " and " : ", ").withStyle(ChatFormatting.DARK_RED));
            msg.append(Component.literal("[" + h.getTier().label + "] ").withStyle(h.getTier().color, ChatFormatting.BOLD))
                    .append(Component.literal(h.getPirateName()).withStyle(ChatFormatting.RED));
        }
        msg.append(Component.literal(String.format(" to collect your debt of %,d rubies!", l.owed)).withStyle(ChatFormatting.DARK_RED));
        p.sendSystemMessage(msg);
        p.playNotifySound(SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.0F, 0.7F);
    }

    /** A spot a hunter can stand on, between minD and maxD blocks from center, near center's height. */
    @Nullable
    public static Vec3 findSpot(ServerLevel level, BlockPos center, double minD, double maxD, RandomSource r) {
        for (int attempt = 0; attempt < 40; attempt++) {
            double ang = r.nextDouble() * Math.PI * 2;
            double d = minD + r.nextDouble() * (maxD - minD);
            int x = center.getX() + (int) Math.round(Math.cos(ang) * d);
            int z = center.getZ() + (int) Math.round(Math.sin(ang) * d);
            if (!level.isLoaded(new BlockPos(x, center.getY(), z))) continue;
            for (int dy = 6; dy >= -8; dy--) {
                BlockPos feet = new BlockPos(x, center.getY() + dy, z);
                BlockPos below = feet.below();
                BlockState floor = level.getBlockState(below);
                if (!floor.isFaceSturdy(level, below, Direction.UP)) continue;
                if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()) continue;
                AABB box = new AABB(feet.getX() + 0.2, feet.getY(), feet.getZ() + 0.2, feet.getX() + 0.8, feet.getY() + 1.8, feet.getZ() + 0.8);
                if (!level.noCollision(box)) continue;
                return Vec3.atBottomCenterOf(feet);
            }
        }
        return null;
    }

    /** Debts still to be taken from a dead player's corpse: player -> the hunter who'll search it. */
    private static final Map<UUID, UUID> PENDING = new HashMap<>();

    /**
     * A hunter got the player. The bank claims the player's bounty and puts it toward the loan, then
     * takes what it can from their bank account. Whatever is still owed, the hunter takes in items
     * from the player's corpse (rubies first, then valuables), destroyed for good.
     */
    public static void onHunterKill(ServerPlayer victim, BountyHunterEntity hunter) {
        MinecraftServer server = victim.server;
        LoanData data = LoanData.get(server);
        LoanData.Loan l = data.get(victim.getUUID());
        if (l == null) return;
        l.waveDone = true;

        // 1. The bank takes what's owed out of the bounty on the player's head; any bounty left over stays.
        long fromBounty = 0;
        BountyData bounties = BountyData.get(server);
        BountyData.Entry be = bounties.get(victim.getUUID());
        if (be != null && be.amount > 0) {
            fromBounty = Math.min(be.amount, l.owed);
            be.amount -= (int) fromBounty;
            bounties.setDirty();
            l.owed -= fromBounty;
            server.getPlayerList().broadcastSystemMessage(Component.literal("\u2620 ").withStyle(ChatFormatting.DARK_RED)
                    .append(Component.literal("The bank").withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" claimed ").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(String.format("%,d rubies", fromBounty)).withStyle(ChatFormatting.RED))
                    .append(Component.literal(" of the bounty on ").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(victim.getGameProfile().getName()).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal("!").withStyle(ChatFormatting.YELLOW)), false);
            UUID crewId = CrewManager.crewIdOf(victim);
            Crew crew = crewId == null ? null : CrewData.get(server).byId(crewId);
            if (crew != null) CrewManager.syncCrew(server, crew);
        }

        // 2. Then the bank account.
        long fromBank = l.owed > 0 ? BankData.get(server).take(victim.getUUID(), l.owed) : 0;
        l.owed -= fromBank;
        data.setDirty();

        victim.sendSystemMessage(Component.literal("\u2620 ").withStyle(ChatFormatting.DARK_RED)
                .append(Component.literal(hunter.getPirateName()).withStyle(ChatFormatting.RED))
                .append(Component.literal(" collected for the bank:").withStyle(ChatFormatting.YELLOW)));
        if (fromBounty > 0) victim.sendSystemMessage(line(String.format("%,d rubies from the bounty on your head (%,d left on it)", fromBounty, be.amount)));
        if (fromBank > 0) victim.sendSystemMessage(line(String.format("%,d rubies from your bank account", fromBank)));

        if (l.owed <= 0) {
            data.remove(victim.getUUID());
            victim.sendSystemMessage(Component.literal("Your debt is settled.").withStyle(ChatFormatting.GREEN));
            return;
        }

        // 3. Then items. Normally from the corpse; with keepInventory on there's no corpse, so straight from the inventory.
        if (victim.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)) {
            Map<String, Integer> taken = new LinkedHashMap<>();
            Inventory inv = victim.getInventory();
            long[] result = seizeItems(inv, l.owed, taken, c -> {
                if (!inv.add(c) && !c.isEmpty()) victim.drop(c, false);
            });
            finishItems(victim, l, data, taken, result);
        } else {
            PENDING.put(victim.getUUID(), hunter.getUUID());
            victim.sendSystemMessage(line(String.format("...and %s is searching your corpse for the other %,d rubies", hunter.getPirateName(), l.owed)));
        }
    }

    /** The dead player's corpse was made (or there was nothing to make one from). */
    public static void onCorpse(ServerPlayer player, @Nullable CorpseEntity corpse) {
        UUID hunterId = PENDING.remove(player.getUUID());
        if (hunterId == null) return;
        LoanData data = LoanData.get(player.server);
        LoanData.Loan l = data.get(player.getUUID());
        if (l == null) return;
        if (corpse == null) {
            finishItems(player, l, data, new LinkedHashMap<>(), new long[]{0, 0});
            return;
        }
        Entity e = player.serverLevel().getEntity(hunterId);
        if (e instanceof BountyHunterEntity hunter && hunter.isAlive()) {
            corpse.lockFor(hunter);
            hunter.startLooting(corpse);
        } else {
            // The hunter is already gone: the bank's men collect it themselves.
            seizeFromCorpse(corpse, null);
        }
    }

    /** The hunter finished searching the corpse (or took too long and the bank stepped in). */
    public static void seizeFromCorpse(CorpseEntity corpse, @Nullable BountyHunterEntity hunter) {
        MinecraftServer server = corpse.getServer();
        UUID owner = corpse.getOwner();
        corpse.unlock();
        if (server == null || owner == null) return;
        LoanData data = LoanData.get(server);
        LoanData.Loan l = data.get(owner);
        if (l == null || l.owed <= 0) return;
        Map<String, Integer> taken = new LinkedHashMap<>();
        long[] result = seizeItems(corpse.getItems(), l.owed, taken, c -> {
            ItemStack left = corpse.getItems().addItem(c);
            if (!left.isEmpty()) corpse.spawnAtLocation(left);
        });
        if (hunter != null) {
            hunter.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            hunter.playSound(SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, 0.8F);
        }
        ServerPlayer p = server.getPlayerList().getPlayer(owner);
        if (p != null) {
            p.sendSystemMessage(Component.literal("\u2620 ").withStyle(ChatFormatting.DARK_RED)
                    .append(Component.literal(hunter != null ? hunter.getPirateName() : "The bank").withStyle(ChatFormatting.RED))
                    .append(Component.literal(" went through your corpse:").withStyle(ChatFormatting.YELLOW)));
            finishItems(p, l, data, taken, result);
        } else {
            l.owed = Math.max(0, l.owed - result[0]);
            if (l.owed <= 0) data.remove(owner);
            data.setDirty();
        }
    }

    /** The hunter died before searching the corpse. */
    public static void cancelSeizure(CorpseEntity corpse) {
    }

    private static void finishItems(ServerPlayer victim, LoanData.Loan l, LoanData data, Map<String, Integer> taken, long[] result) {
        l.owed = Math.max(0, l.owed - result[0]);
        data.setDirty();
        if (!taken.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            int shown = 0;
            for (var e : taken.entrySet()) {
                if (shown == 8) {
                    sb.append(", and ").append(taken.size() - 8).append(" more");
                    break;
                }
                if (shown > 0) sb.append(", ");
                sb.append(e.getValue() > 1 ? e.getValue() + "x " : "").append(e.getKey());
                shown++;
            }
            victim.sendSystemMessage(line(String.format("%,d rubies' worth of your items, destroyed: ", result[1]) + sb));
        } else {
            victim.sendSystemMessage(line("no items: you had nothing worth taking"));
        }
        if (l.owed <= 0) {
            data.remove(victim.getUUID());
            victim.sendSystemMessage(Component.literal("Your debt is settled.").withStyle(ChatFormatting.GREEN));
        } else {
            victim.sendSystemMessage(Component.literal(String.format("You still owe %,d rubies. More collectors come tomorrow.", l.owed)).withStyle(ChatFormatting.RED));
        }
    }

    private static Component line(String text) {
        return Component.literal("  - " + text).withStyle(ChatFormatting.GRAY);
    }

    /**
     * Takes items worth at least {@code owed} rubies from the inventory (or everything of value if
     * that's not enough): rubies first, then valuables, avoiding overshooting where it can.
     * Returns {debt covered, value taken}.
     */
    static long[] seizeItems(net.minecraft.world.Container inv, long owed, Map<String, Integer> taken,
                             java.util.function.Consumer<ItemStack> giveChange) {
        double remaining = owed;
        double value = 0;
        int rubies = BankManager.countRubies(inv);
        if (rubies > 0) {
            int t = BankManager.takeRubies(inv, (int) Math.min(rubies, owed), giveChange);
            remaining -= t;
            value += t;
            if (t > 0) taken.merge("Ruby", t, Integer::sum);
        }
        if (remaining > 0.5) {
            List<Integer> slots = new ArrayList<>();
            for (int i = 0; i < inv.getContainerSize(); i++) if (RubyValues.unitValue(inv.getItem(i)) > 0) slots.add(i);
            slots.sort((a, b) -> Double.compare(RubyValues.unitValue(inv.getItem(b)), RubyValues.unitValue(inv.getItem(a))));
            // Most valuable items first, as long as each one fits in what's still owed.
            for (int slot : slots) {
                ItemStack s = inv.getItem(slot);
                double uv = RubyValues.unitValue(s);
                while (!s.isEmpty() && remaining > 0.5 && uv <= remaining + 1e-6) {
                    taken.merge(s.getHoverName().getString(), 1, Integer::sum);
                    s.shrink(1);
                    remaining -= uv;
                    value += uv;
                }
                if (remaining <= 0.5) break;
            }
            // Still owed but everything left is worth more than that: take the cheapest one.
            if (remaining > 0.5) {
                int best = -1;
                double bestV = Double.MAX_VALUE;
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    double uv = RubyValues.unitValue(inv.getItem(i));
                    if (uv > 0 && uv < bestV) {
                        bestV = uv;
                        best = i;
                    }
                }
                if (best >= 0) {
                    ItemStack s = inv.getItem(best);
                    taken.merge(s.getHoverName().getString(), 1, Integer::sum);
                    s.shrink(1);
                    remaining -= bestV;
                    value += bestV;
                }
            }
            inv.setChanged();
        }
        long covered = remaining <= 0.5 ? owed : owed - (long) Math.ceil(remaining);
        return new long[]{covered, Math.round(value)};
    }

    /** The last hunter of a wave fell. */
    public static void onHunterDeath(BountyHunterEntity hunter) {
        MinecraftServer server = hunter.getServer();
        UUID debtor = hunter.getDebtor();
        if (server == null || debtor == null || hunter.isTestHunter()) return;
        if (!huntActive(server, debtor, hunter.getWaveSerial())) return;
        boolean othersLeft = !hunter.level().getEntitiesOfClass(BountyHunterEntity.class, hunter.getBoundingBox().inflate(160),
                h -> h != hunter && h.isAlive() && debtor.equals(h.getDebtor()) && h.getWaveSerial() == hunter.getWaveSerial()).isEmpty();
        ServerPlayer p = server.getPlayerList().getPlayer(debtor);
        if (othersLeft || p == null) return;
        LoanData.Loan l = loanOf(server, debtor);
        if (l == null) return;
        PirateTier next = waveTiers(l.wave + 1).get(waveTiers(l.wave + 1).size() - 1);
        p.sendSystemMessage(Component.literal("You fought off the bank's collectors... for today. Tomorrow a ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(next.label + "-tier").withStyle(next.color, ChatFormatting.BOLD))
                .append(Component.literal(String.format(" collector comes. Repay the %,d rubies to stop them.", l.owed)).withStyle(ChatFormatting.GOLD)));
    }

    public static void onLogin(ServerPlayer p) {
        LoanData.Loan l = loanOf(p.server, p.getUUID());
        if (l == null) return;
        p.sendSystemMessage(Component.literal(status(p.server, l)).withStyle(l.defaulted ? ChatFormatting.RED : ChatFormatting.YELLOW));
    }

    public static String status(MinecraftServer server, LoanData.Loan l) {
        if (l.defaulted) return String.format("You owe the bank %,d rubies and it's overdue: debt collectors are after you. Repay it at a bank to call them off.", l.owed);
        long left = l.deadline - now(server);
        int days = (int) Math.ceil(left / (double) DAY);
        return String.format("You owe the bank %,d rubies, due in %d day%s.", l.owed, days, days == 1 ? "" : "s");
    }

    /** Op testing: make the caller's loan due right now. */
    public static boolean forceDue(ServerPlayer p) {
        LoanData data = LoanData.get(p.server);
        LoanData.Loan l = data.get(p.getUUID());
        if (l == null) return false;
        l.deadline = now(p.server);
        data.setDirty();
        return true;
    }
}
