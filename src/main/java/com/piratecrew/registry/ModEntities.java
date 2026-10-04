package com.piratecrew.registry;

import com.piratecrew.PirateCrew;
import com.piratecrew.entity.BankerEntity;
import com.piratecrew.entity.BountyHunterEntity;
import com.piratecrew.entity.PirateEntity;
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

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(PIRATE.get(), PirateEntity.createAttributes().build());
        event.put(BANKER.get(), BankerEntity.createAttributes().build());
        event.put(BOUNTY_HUNTER.get(), PirateEntity.createAttributes().build());
    }
}
