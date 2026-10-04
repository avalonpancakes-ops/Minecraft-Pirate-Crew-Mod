package com.piratecrew.crew;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.piratecrew.entity.PirateEntity;
import com.piratecrew.entity.PirateTier;
import com.piratecrew.registry.ModEntities;
import com.piratecrew.bank.BankManager;
import com.piratecrew.bank.LoanData;
import com.piratecrew.bank.LoanManager;
import com.piratecrew.entity.BountyHunterEntity;
import com.piratecrew.world.BankBuilder;
import com.piratecrew.world.BarBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;

import java.util.UUID;

public class CrewCommands {
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("crew")
                .executes(c -> { CrewManager.sync(c.getSource().getPlayerOrException(), true); return 1; })
                .then(Commands.literal("create").then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(c -> { CrewManager.create(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "name")); return 1; })))
                .then(Commands.literal("invite").then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> { CrewManager.invite(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "player")); return 1; })))
                .then(Commands.literal("accept").then(Commands.argument("id", StringArgumentType.word())
                        .executes(c -> { CrewManager.accept(c.getSource().getPlayerOrException(), parse(c)); return 1; })))
                .then(Commands.literal("decline").then(Commands.argument("id", StringArgumentType.word())
                        .executes(c -> { CrewManager.decline(c.getSource().getPlayerOrException(), parse(c)); return 1; })))
                .then(Commands.literal("leave")
                        .executes(c -> { CrewManager.leave(c.getSource().getPlayerOrException()); return 1; }))
                .then(Commands.literal("disband")
                        .executes(c -> { CrewManager.disband(c.getSource().getPlayerOrException()); return 1; }))
                .then(Commands.literal("icon")
                        .executes(c -> { CrewManager.setIconFromHand(c.getSource().getPlayerOrException()); return 1; }))
        );

        d.register(Commands.literal("bank").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            long bal = BankManager.balance(p.server, p.getUUID());
            c.getSource().sendSuccess(() -> Component.literal("Bank balance: " + String.format("%,d", bal) + " rubies. Visit a Bank Counter to deposit or withdraw.")
                    .withStyle(net.minecraft.ChatFormatting.GOLD), false);
            LoanData.Loan loan = LoanManager.loanOf(p.server, p.getUUID());
            if (loan != null) {
                c.getSource().sendSuccess(() -> Component.literal(LoanManager.status(p.server, loan))
                        .withStyle(loan.defaulted ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.YELLOW), false);
            }
            return 1;
        }));

        d.register(Commands.literal("piratecrew")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("spawnbar").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Direction facing = p.getDirection().getOpposite(); // door faces the player
                    BarBuilder.buildAt(p.serverLevel(), p.blockPosition().relative(p.getDirection(), 8), facing, true);
                    c.getSource().sendSuccess(() -> Component.literal("Built a pirate bar."), true);
                    return 1;
                }))
                .then(Commands.literal("spawnbank").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Direction facing = p.getDirection().getOpposite();
                    BankBuilder.buildAt(p.serverLevel(), p.blockPosition().relative(p.getDirection(), 7), facing, true);
                    c.getSource().sendSuccess(() -> Component.literal("Built a bank."), true);
                    return 1;
                }))
                .then(Commands.literal("loandue").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (!LoanManager.forceDue(p)) {
                        c.getSource().sendFailure(Component.literal("You don't have a loan."));
                        return 0;
                    }
                    c.getSource().sendSuccess(() -> Component.literal("Your loan is now overdue."), true);
                    return 1;
                }))
                .then(Commands.literal("spawnhunter").then(Commands.argument("tier", StringArgumentType.word())
                        .suggests((c, b) -> {
                            for (PirateTier t : PirateTier.values()) b.suggest(t.label);
                            return b.buildFuture();
                        })
                        .executes(c -> {
                            PirateTier tier = PirateTier.byLabel(StringArgumentType.getString(c, "tier"));
                            if (tier == null) {
                                c.getSource().sendFailure(Component.literal("Tier must be F, D, C, B, A or S"));
                                return 0;
                            }
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            ServerLevel level = p.serverLevel();
                            BountyHunterEntity h = ModEntities.BOUNTY_HUNTER.get().create(level);
                            if (h == null) return 0;
                            net.minecraft.world.phys.Vec3 pos = LoanManager.findSpot(level, p.blockPosition(), 8, 14, p.getRandom());
                            if (pos == null) pos = p.position();
                            h.moveTo(pos.x, pos.y, pos.z, p.getYRot(), 0);
                            h.setupHunter(tier, p.getUUID(), -1);
                            h.setTest(true);
                            h.finalizeSpawn(level, level.getCurrentDifficultyAt(p.blockPosition()), MobSpawnType.COMMAND, null, null);
                            level.addFreshEntity(h);
                            c.getSource().sendSuccess(() -> Component.literal("A test bounty hunter is coming for you (switch to survival)."), true);
                            return 1;
                        })))
                .then(Commands.literal("spawnpirate").then(Commands.argument("tier", StringArgumentType.word())
                        .suggests((c, b) -> {
                            for (PirateTier t : PirateTier.values()) b.suggest(t.label);
                            return b.buildFuture();
                        })
                        .executes(c -> {
                            PirateTier tier = PirateTier.byLabel(StringArgumentType.getString(c, "tier"));
                            if (tier == null) {
                                c.getSource().sendFailure(Component.literal("Tier must be F, D, C, B, A or S"));
                                return 0;
                            }
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            ServerLevel level = p.serverLevel();
                            PirateEntity pirate = ModEntities.PIRATE.get().create(level);
                            if (pirate == null) return 0;
                            pirate.moveTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0);
                            pirate.initPirate(tier);
                            pirate.finalizeSpawn(level, level.getCurrentDifficultyAt(p.blockPosition()), MobSpawnType.COMMAND, null, null);
                            level.addFreshEntity(pirate);
                            return 1;
                        })))
        );
    }

    private static UUID parse(CommandContext<CommandSourceStack> c) {
        try {
            return UUID.fromString(StringArgumentType.getString(c, "id"));
        } catch (IllegalArgumentException e) {
            return new UUID(0, 0);
        }
    }
}
