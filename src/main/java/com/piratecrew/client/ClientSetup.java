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
        }

        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_CREW);
        }

        @SubscribeEvent
        public static void packs(AddPackFindersEvent event) {
            RubyToolsPack.register(event);
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
        }
    }
}
