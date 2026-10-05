package com.piratecrew.pact;

import com.piratecrew.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Who holds which Soul Pact, and players using their pact's power. */
public class SoulPacts {
    private static final String KEY = "piratecrew_pact";
    private static final String READY = "piratecrew_pact_ready";
    private static final double AIM_RANGE = 32.0;

    public static ItemStack randomPactItem(RandomSource random) {
        SoulPact[] all = SoulPact.values();
        return new ItemStack(ModItems.SOUL_PACTS.get(all[random.nextInt(all.length)]).get());
    }

    // ------------------------------------------------------------------ players

    /** Stored under the persisted tag so it survives death. */
    private static CompoundTag data(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    @Nullable
    public static SoulPact of(Player p) {
        return SoulPact.byId(data(p).getString(KEY));
    }

    @Nullable
    public static SoulPact of(LivingEntity e) {
        if (e instanceof Player p) return of(p);
        if (e instanceof com.piratecrew.entity.PirateEntity pe) return pe.getPact();
        return null;
    }

    public static void set(Player p, @Nullable SoulPact pact) {
        if (pact == null) data(p).remove(KEY);
        else data(p).putString(KEY, pact.id);
        data(p).remove(READY);
    }

    /** Tell the player's client their pact and its cooldown (for the HUD badge). */
    public static void sync(ServerPlayer p) {
        SoulPact pact = of(p);
        long ready = data(p).getLong(READY);
        com.piratecrew.network.ModNetwork.sendTo(p, new com.piratecrew.network.PactSyncPacket(pact == null ? "" : pact.id, ready,
                pact == null ? 1 : pact.cooldown));
    }

    /** Respawned players keep their pact (in case the persisted tag wasn't carried over). */
    public static void copy(Player from, Player to) {
        SoulPact p = of(from);
        if (p != null) data(to).putString(KEY, p.id);
    }

    /** The pact key: use the player's pact power at whatever they're looking at. */
    public static void activate(ServerPlayer player) {
        SoulPact pact = of(player);
        if (pact == null) {
            player.displayClientMessage(Component.literal("Your soul is bound to no pact. Find a Soul Pact in the Sundered Sea.").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (player.isSpectator()) return;
        if (PactPowers.drained(player)) {
            player.displayClientMessage(Component.literal("The sea silences your " + pact.title() + ".").withStyle(ChatFormatting.DARK_AQUA), true);
            return;
        }
        long now = player.level().getGameTime();
        long ready = data(player).getLong(READY);
        if (now < ready && !player.isCreative()) {
            player.displayClientMessage(Component.literal(pact.power + " ready in " + ((ready - now + 19) / 20) + "s").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(AIM_RANGE));
        var block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 aim = block.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, aim, new AABB(eye, aim).inflate(1.5),
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e != player, eye.distanceToSqr(aim));
        LivingEntity target = hit != null ? (LivingEntity) hit.getEntity() : null;
        if (target != null) aim = target.getEyePosition();
        if (!PactPowers.activate(player, pact, target, aim)) return;
        data(player).putLong(READY, now + pact.cooldown);
        sync(player);
        player.displayClientMessage(Component.literal("✦ " + pact.power + "!").withStyle(pact.color, ChatFormatting.BOLD), true);
    }

    /** Bind a pact to a player's soul. Replacing one needs a sneak, and the old one is lost. */
    public static boolean bind(ServerPlayer player, SoulPact pact) {
        SoulPact current = of(player);
        if (current == pact) {
            player.displayClientMessage(Component.literal("Your soul already holds the " + pact.title() + ".").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        if (current != null && !player.isShiftKeyDown()) {
            player.sendSystemMessage(Component.literal("Your soul is bound to the " + current.title() + ". Sneak and use this pact to tear the old one "
                    + "free and take the " + pact.title() + " instead. The " + current.title() + " will be lost.").withStyle(ChatFormatting.YELLOW));
            return false;
        }
        set(player, pact);
        player.sendSystemMessage(Component.literal("✦ Your soul is bound to the " + pact.title() + "!").withStyle(pact.color, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("  Gift: " + pact.passiveText).withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal("  Power (pact key, R): " + pact.power + ". " + pact.powerText).withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal("  The sea drains every pact: in water you are weak, slow and powerless.").withStyle(ChatFormatting.DARK_AQUA));
        return true;
    }
}
