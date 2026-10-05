package com.piratecrew.registry;

import com.piratecrew.PirateCrew;
import com.piratecrew.entity.BankerEntity;
import com.piratecrew.entity.BountyHunterEntity;
import com.piratecrew.entity.CorpseEntity;
import com.piratecrew.entity.RaiderPirateEntity;
import com.piratecrew.entity.MarineEntity;
import com.piratecrew.entity.PirateEntity;
import com.piratecrew.entity.boss.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, PirateCrew.MODID);

    public static final RegistryObject<EntityType<PirateEntity>> PIRATE = ENTITIES.register("pirate",
            () -> EntityType.Builder.<PirateEntity>of(PirateEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build(PirateCrew.id("pirate").toString()));

    public static final RegistryObject<EntityType<BankerEntity>> BANKER = ENTITIES.register("banker",
            () -> EntityType.Builder.<BankerEntity>of(BankerEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build(PirateCrew.id("banker").toString()));

    public static final RegistryObject<EntityType<BountyHunterEntity>> BOUNTY_HUNTER = ENTITIES.register("bounty_hunter",
            () -> EntityType.Builder.<BountyHunterEntity>of(BountyHunterEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build(PirateCrew.id("bounty_hunter").toString()));

    public static final RegistryObject<EntityType<RaiderPirateEntity>> RAIDER_PIRATE = ENTITIES.register("raider_pirate",
            () -> EntityType.Builder.<RaiderPirateEntity>of(RaiderPirateEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build(PirateCrew.id("raider_pirate").toString()));

    public static final RegistryObject<EntityType<MarineEntity>> MARINE = ENTITIES.register("marine",
            () -> EntityType.Builder.<MarineEntity>of(MarineEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build(PirateCrew.id("marine").toString()));

    public static final RegistryObject<EntityType<CommodoreEntity>> COMMODORE = boss("commodore", CommodoreEntity::new);
    public static final RegistryObject<EntityType<TempestAdmiralEntity>> TEMPEST_ADMIRAL = boss("tempest_admiral", TempestAdmiralEntity::new);
    public static final RegistryObject<EntityType<FleetAdmiralEntity>> FLEET_ADMIRAL = boss("fleet_admiral", FleetAdmiralEntity::new);

    public static final RegistryObject<EntityType<KrakenEntity>> KRAKEN = ENTITIES.register("kraken",
            () -> EntityType.Builder.<KrakenEntity>of(KrakenEntity::new, MobCategory.MONSTER)
                    .sized(3.0F, 6.0F)
                    .clientTrackingRange(16)
                    .build(PirateCrew.id("kraken").toString()));

    public static final RegistryObject<EntityType<LeviathanEntity>> LEVIATHAN = ENTITIES.register("leviathan",
            () -> EntityType.Builder.<LeviathanEntity>of(LeviathanEntity::new, MobCategory.MONSTER)
                    .sized(5.0F, 5.0F)
                    .clientTrackingRange(16)
                    .build(PirateCrew.id("leviathan").toString()));

    private static <T extends MarineBossEntity> RegistryObject<EntityType<T>> boss(String name, EntityType.EntityFactory<T> factory) {
        return ENTITIES.register(name, () -> EntityType.Builder.of(factory, MobCategory.MONSTER)
                .sized(0.6F, 1.8F)
                .fireImmune()
                .clientTrackingRange(12)
                .build(PirateCrew.id(name).toString()));
    }

    public static final RegistryObject<EntityType<CorpseEntity>> CORPSE = ENTITIES.register("corpse",
            () -> EntityType.Builder.<CorpseEntity>of(CorpseEntity::new, MobCategory.MISC)
                    .sized(1.2F, 0.5F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build(PirateCrew.id("corpse").toString()));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(PIRATE.get(), PirateEntity.createAttributes().build());
        event.put(BANKER.get(), BankerEntity.createAttributes().build());
        event.put(BOUNTY_HUNTER.get(), PirateEntity.createAttributes().build());
        event.put(RAIDER_PIRATE.get(), PirateEntity.createAttributes().build());
        event.put(MARINE.get(), PirateEntity.createAttributes().build());
        event.put(COMMODORE.get(), PirateEntity.createAttributes().build());
        event.put(TEMPEST_ADMIRAL.get(), PirateEntity.createAttributes().build());
        event.put(FLEET_ADMIRAL.get(), PirateEntity.createAttributes().build());
        event.put(KRAKEN.get(), KrakenEntity.createAttributes().build());
        event.put(LEVIATHAN.get(), LeviathanEntity.createAttributes().build());
    }
}
