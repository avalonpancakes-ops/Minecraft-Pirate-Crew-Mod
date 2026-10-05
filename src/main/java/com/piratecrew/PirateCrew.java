package com.piratecrew;

import com.mojang.logging.LogUtils;
import com.piratecrew.network.ModNetwork;
import com.piratecrew.registry.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(PirateCrew.MODID)
public class PirateCrew {
    public static final String MODID = "piratecrew";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PirateCrew() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModPoi.POI.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModTabs.TABS.register(modBus);
        com.piratecrew.registry.ModParticles.PARTICLES.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(ModEntities::registerAttributes);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
        event.enqueueWork(com.piratecrew.util.AttributeCaps::raise);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
