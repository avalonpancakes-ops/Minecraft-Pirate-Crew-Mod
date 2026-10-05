package com.piratecrew.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.piratecrew.PirateCrew;
import com.piratecrew.network.CrewActionPacket;
import com.piratecrew.network.ModNetwork;
import com.piratecrew.registry.ModEntities;
import com.piratecrew.registry.ModMenus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

public class ClientSetup {
    public static final KeyMapping OPEN_CREW = new KeyMapping("key.piratecrew.crew_menu", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I, "key.categories.piratecrew");
    public static final KeyMapping OPEN_LOG = new KeyMapping("key.piratecrew.captains_log", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.piratecrew");
    public static final KeyMapping PACT_POWER = new KeyMapping("key.piratecrew.pact_power", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.piratecrew");

    @Mod.EventBusSubscriber(modid = PirateCrew.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBus {
        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> MenuScreens.register(ModMenus.PIRATE.get(), PirateScreen::new));
        }

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.PIRATE.get(), PirateRenderer::new);
            event.registerEntityRenderer(ModEntities.BANKER.get(), BankerRenderer::new);
            event.registerEntityRenderer(ModEntities.BOUNTY_HUNTER.get(), PirateRenderer::new);
            event.registerEntityRenderer(ModEntities.CORPSE.get(), CorpseRenderer::new);
            event.registerEntityRenderer(ModEntities.RAIDER_PIRATE.get(), PirateRenderer::new);
            event.registerEntityRenderer(ModEntities.MARINE.get(), PirateRenderer::new);
            event.registerEntityRenderer(ModEntities.COMMODORE.get(), PirateRenderer::new);
            event.registerEntityRenderer(ModEntities.TEMPEST_ADMIRAL.get(), PirateRenderer::new);
            event.registerEntityRenderer(ModEntities.FLEET_ADMIRAL.get(), PirateRenderer::new);
            event.registerEntityRenderer(ModEntities.KRAKEN.get(), KrakenRenderer::new);
            event.registerEntityRenderer(ModEntities.LEVIATHAN.get(), LeviathanRenderer::new);
        }

        @SubscribeEvent
        public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(com.piratecrew.client.model.KrakenModel.LAYER, com.piratecrew.client.model.KrakenModel::createBodyLayer);
        }

        @SubscribeEvent
        public static void overlays(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event) {
            event.registerAbove(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.HOTBAR.id(), "soul_pact", PactHud::render);
        }

        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_CREW);
            event.register(PACT_POWER);
            event.register(OPEN_LOG);
        }

        @SubscribeEvent
        public static void packs(AddPackFindersEvent event) {
            RubyToolsPack.register(event);
            SunderedTexturesPack.register(event);
        }
    }

    @Mod.EventBusSubscriber(modid = PirateCrew.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeBus {
        @SubscribeEvent
        public static void clientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            while (OPEN_CREW.consumeClick()) {
                if (mc.player != null && mc.screen == null) {
                    ModNetwork.sendToServer(CrewActionPacket.simple(CrewActionPacket.Action.REQUEST_SYNC));
                }
            }
            while (OPEN_LOG.consumeClick()) {
                if (mc.player != null && mc.screen == null) com.piratecrew.client.codex.CodexScreen.open();
            }
            while (PACT_POWER.consumeClick()) {
                if (mc.player != null && mc.screen == null) ModNetwork.sendToServer(new com.piratecrew.network.PactAbilityPacket());
            }
        }
    }
}
