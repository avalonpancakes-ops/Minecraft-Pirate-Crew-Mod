package com.piratecrew.registry;

import com.google.common.collect.ImmutableSet;
import com.piratecrew.PirateCrew;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Lets Siren portals be found quickly on the far side, the way vanilla finds nether portals. */
public class ModPoi {
    public static final DeferredRegister<PoiType> POI = DeferredRegister.create(ForgeRegistries.POI_TYPES, PirateCrew.MODID);

    public static final RegistryObject<PoiType> SIREN_PORTAL = POI.register("siren_portal",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.SIREN_PORTAL.get().getStateDefinition().getPossibleStates()), 0, 1));

    public static final ResourceKey<PoiType> SIREN_PORTAL_KEY = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, PirateCrew.id("siren_portal"));
}
