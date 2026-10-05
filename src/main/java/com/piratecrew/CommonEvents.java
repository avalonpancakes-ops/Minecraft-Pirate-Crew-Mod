package com.piratecrew;

import com.piratecrew.bank.LoanManager;
import com.piratecrew.bounty.BountyManager;
import com.piratecrew.entity.BountyHunterEntity;
import com.piratecrew.entity.CorpseEntity;
import com.piratecrew.crew.CrewCommands;
import com.piratecrew.crew.CrewManager;
import com.piratecrew.world.VillageBarHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = PirateCrew.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonEvents {

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        CrewCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            BountyManager.onLogin(sp);
            LoanManager.onLogin(sp);
            // First time aboard: a Captain's Log to learn the ropes.
            var tag = sp.getPersistentData();
            if (!tag.contains(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG)) tag.put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, new net.minecraft.nbt.CompoundTag());
            var keep = tag.getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
            if (!keep.getBoolean("piratecrew_got_log")) {
                keep.putBoolean("piratecrew_got_log", true);
                net.minecraft.world.item.ItemStack log = new net.minecraft.world.item.ItemStack(com.piratecrew.registry.ModItems.CAPTAINS_LOG.get());
                if (!sp.getInventory().add(log)) sp.drop(log, false);
                sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("\u2693 You've been given a Captain's Log. Open it (or press J) to learn the ways of the sea.")
                        .withStyle(net.minecraft.ChatFormatting.GOLD));
            }
            CrewManager.sync(sp, false);
        }
    }

    /**
     * No friendly fire, ever: crewmates (players and pirates) can't hurt each other by any weapon,
     * arrow, trident, thrown potion, TNT they lit, thorns...
     */
    @SubscribeEvent
    public static void friendlyFire(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide) return;
        Entity attacker = event.getSource().getEntity();
        if (attacker != null && attacker != event.getEntity() && CrewManager.areCrewmates(attacker, event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** Arrows, tridents and other projectiles fly straight through crewmates (so flame arrows can't set them alight either). */
    @SuppressWarnings({"deprecation", "removal"})
    @SubscribeEvent
    public static void friendlyProjectiles(net.minecraftforge.event.entity.ProjectileImpactEvent event) {
        if (event.getProjectile().level().isClientSide) return;
        if (!(event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult hit)) return;
        Entity owner = event.getProjectile().getOwner();
        if (owner != null && owner != hit.getEntity() && CrewManager.areCrewmates(owner, hit.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** Harmful effects from a crewmate's splash or lingering potion are stripped right away. */
    private static final java.util.List<java.util.Map.Entry<net.minecraft.world.entity.LivingEntity, net.minecraft.world.effect.MobEffect>> STRIP = new java.util.ArrayList<>();

    @SubscribeEvent
    public static void friendlyPotions(net.minecraftforge.event.entity.living.MobEffectEvent.Added event) {
        net.minecraft.world.entity.LivingEntity target = event.getEntity();
        Entity source = event.getEffectSource();
        var effect = event.getEffectInstance().getEffect();
        if (target.level().isClientSide || source == null || source == target) return;
        if (effect.getCategory() != net.minecraft.world.effect.MobEffectCategory.HARMFUL) return;
        if (CrewManager.areCrewmates(source, target)) STRIP.add(java.util.Map.entry(target, effect));
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        BountyManager.onDeath(event.getEntity(), event.getSource().getEntity());
        if (event.getEntity() instanceof ServerPlayer sp && event.getSource().getEntity() instanceof BountyHunterEntity h && h.isHunting(sp) && !h.isTestHunter()) {
            LoanManager.onHunterKill(sp, h);
        }
    }

    /**
     * Vanilla armor stops helping past 20 points (80% reduction). Sundered Sea sets go far beyond that,
     * so armor above 20 cuts the remaining damage further: 30 armor takes 80% of what 20 would,
     * 51 (full Leviathan) about 56%, 65 (full Sovereign) about 47%.
     */
    @SubscribeEvent
    public static void heavyArmor(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
        if (event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) return;
        double armor = event.getEntity().getArmorValue();
        if (armor <= 20) return;
        event.setAmount((float) (event.getAmount() / (1.0 + (armor - 20) / 40.0)));
    }

    /** Players leave a corpse holding their items instead of scattering them. */
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOW)
    public static void playerDrops(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || event.isCanceled()) return;
        java.util.List<net.minecraft.world.item.ItemStack> stacks = new java.util.ArrayList<>();
        for (net.minecraft.world.entity.item.ItemEntity ie : event.getDrops()) {
            if (!ie.getItem().isEmpty()) stacks.add(ie.getItem().copy());
        }
        if (stacks.isEmpty()) {
            LoanManager.onCorpse(sp, null);
            return;
        }
        event.setCanceled(true);
        CorpseEntity corpse = CorpseEntity.create(sp, stacks);
        sp.serverLevel().addFreshEntity(corpse);
        sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(String.format(
                "\u2620 Your belongings are on your corpse at %d, %d, %d. Right-click it to get them back: after 2 minutes anyone can loot it.",
                corpse.getBlockX(), corpse.getBlockY(), corpse.getBlockZ())).withStyle(net.minecraft.ChatFormatting.GRAY));
        LoanManager.onCorpse(sp, corpse);
    }

    /** The creative aggro stick acts before a mob's own right-click (trading, recruiting, the bank...). */
    @SubscribeEvent
    public static void aggroStick(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        net.minecraft.world.item.ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof com.piratecrew.item.AggroStickItem)) return;
        if (!(event.getTarget() instanceof net.minecraft.world.entity.LivingEntity target)) return;
        net.minecraft.world.InteractionResult r = com.piratecrew.item.AggroStickItem.useOnEntity(stack, event.getEntity(), target);
        event.setCanceled(true);
        event.setCancellationResult(r);
    }

    /** Fighters notice players walling up or towering (see BuildTracker). */
    @SubscribeEvent
    public static void blockPlaced(net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player p && !p.level().isClientSide) {
            com.piratecrew.entity.BuildTracker.record(p);
        }
    }

    @SubscribeEvent
    public static void entityJoin(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        com.piratecrew.compat.ValkyrienPiratesCompat.onJoin(event);
    }

    @SubscribeEvent
    public static void chunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            VillageBarHandler.onChunkLoad(level, chunk);
            com.piratecrew.world.RaiderCampHandler.onChunkLoad(level, chunk, event.isNewChunk());
            com.piratecrew.sundered.SunderedStructures.onChunkLoad(level, chunk, event.isNewChunk());
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!STRIP.isEmpty()) {
            for (var e : STRIP) if (e.getKey().isAlive()) e.getKey().removeEffect(e.getValue());
            STRIP.clear();
        }
        VillageBarHandler.tick(ServerLifecycleHooks.getCurrentServer());
        LoanManager.tick(ServerLifecycleHooks.getCurrentServer());
        com.piratecrew.compat.ValkyrienPiratesCompat.tick(ServerLifecycleHooks.getCurrentServer());
        com.piratecrew.world.RaiderCampHandler.tick(ServerLifecycleHooks.getCurrentServer());
        com.piratecrew.crew.EmperorManager.tick(ServerLifecycleHooks.getCurrentServer());
        com.piratecrew.sundered.SunderedStructures.tick(ServerLifecycleHooks.getCurrentServer());
        com.piratecrew.sundered.Marines.tick(ServerLifecycleHooks.getCurrentServer());
        com.piratecrew.pact.PactPowers.tick();
    }

    // ------------------------------------------------------------------ soul pacts

    /** Use a Soul Pact on one of your crew's pirates to bind it to them (before the pirate's own menu opens). */
    @SubscribeEvent
    public static void pactOnPirate(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        net.minecraft.world.item.ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof com.piratecrew.pact.SoulPactItem item)) return;
        if (!(event.getTarget() instanceof com.piratecrew.entity.PirateEntity pirate)) return;
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!pirate.isRecruited() || !CrewManager.isInSameCrew(sp, pirate)) {
            sp.displayClientMessage(net.minecraft.network.chat.Component.literal("Only a pirate of your own crew can take your pact.")
                    .withStyle(net.minecraft.ChatFormatting.RED), true);
            return;
        }
        var current = pirate.getPact();
        if (current == item.pact) {
            sp.displayClientMessage(net.minecraft.network.chat.Component.literal(pirate.getPirateName() + " already holds the " + item.pact.title() + "."), true);
            return;
        }
        if (current != null && !sp.isShiftKeyDown()) {
            sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(pirate.getPirateName() + " is bound to the " + current.title()
                    + ". Sneak and use the pact on them to replace it (the old pact will be lost).").withStyle(net.minecraft.ChatFormatting.YELLOW));
            return;
        }
        pirate.bindPact(item.pact);
        com.piratecrew.pact.SoulPactItem.bindEffects(sp.serverLevel(), pirate, item.pact);
        pirate.say("The " + item.pact.title() + " is mine now... I feel it. " + item.pact.power + "!");
        sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("\u2726 " + pirate.getPirateName() + " is bound to the " + item.pact.title()
                + " and now fights " + (item.pact.ranged ? "from range" : "up close") + ", using " + item.pact.power + " on their own.")
                .withStyle(item.pact.color));
        if (!sp.getAbilities().instabuild) stack.shrink(1);
    }

    @SubscribeEvent
    public static void pactHits(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;
        var source = event.getSource();
        if (!(source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker) || source.getDirectEntity() != attacker) return;
        var victim = event.getEntity();
        var pact = com.piratecrew.pact.SoulPacts.of(attacker);
        if (pact != null) event.setAmount(com.piratecrew.pact.PactPowers.onMeleeHit(attacker, pact, victim, event.getAmount()));
        var victimPact = com.piratecrew.pact.SoulPacts.of(victim);
        if (victimPact != null) com.piratecrew.pact.PactPowers.onStruck(victim, victimPact, attacker);
    }

    @SubscribeEvent
    public static void pactFall(net.minecraftforge.event.entity.living.LivingFallEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (com.piratecrew.pact.SoulPacts.of(event.getEntity()) == com.piratecrew.pact.SoulPact.GALE) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void pactPoison(net.minecraftforge.event.entity.living.MobEffectEvent.Applicable event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getEffectInstance().getEffect() == net.minecraft.world.effect.MobEffects.POISON
                && com.piratecrew.pact.SoulPacts.of(event.getEntity()) == com.piratecrew.pact.SoulPact.VENOM) {
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void pactPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || event.player.tickCount % 20 != 0) return;
        var pact = com.piratecrew.pact.SoulPacts.of(event.player);
        if (pact != null && !event.player.isSpectator()) com.piratecrew.pact.PactPowers.passives(event.player, pact);
    }

    @SubscribeEvent
    public static void pactClone(PlayerEvent.Clone event) {
        com.piratecrew.pact.SoulPacts.copy(event.getOriginal(), event.getEntity());
    }
}
