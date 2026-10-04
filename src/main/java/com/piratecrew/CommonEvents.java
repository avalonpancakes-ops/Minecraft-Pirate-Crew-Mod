package com.piratecrew;

import com.piratecrew.bank.LoanManager;
import com.piratecrew.bounty.BountyManager;
import com.piratecrew.entity.BountyHunterEntity;
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
            CrewManager.sync(sp, false);
        }
    }

    /** Crewmates (players and pirates) can't hurt each other unless friendly fire is on. */
    @SubscribeEvent
    public static void friendlyFire(LivingAttackEvent event) {
        if (Config.FRIENDLY_FIRE.get()) return;
        if (event.getEntity().level().isClientSide) return;
        Entity attacker = event.getSource().getEntity();
        if (attacker != null && attacker != event.getEntity() && CrewManager.areCrewmates(attacker, event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        BountyManager.onDeath(event.getEntity(), event.getSource().getEntity());
        if (event.getEntity() instanceof ServerPlayer sp && event.getSource().getEntity() instanceof BountyHunterEntity h && h.isHunting(sp) && !h.isTestHunter()) {
            LoanManager.onHunterKill(sp, h);
        }
    }

    @SubscribeEvent
    public static void chunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            VillageBarHandler.onChunkLoad(level, chunk);
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        VillageBarHandler.tick(ServerLifecycleHooks.getCurrentServer());
        LoanManager.tick(ServerLifecycleHooks.getCurrentServer());
    }
}
