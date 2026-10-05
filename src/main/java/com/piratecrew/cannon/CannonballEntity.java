package com.piratecrew.cannon;

import com.piratecrew.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** An iron ball that arcs from a cannon and bursts where it lands (entities hurt, blocks spared). */
public class CannonballEntity extends ThrowableItemProjectile {
    public CannonballEntity(EntityType<? extends CannonballEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.CANNONBALL.get();
    }

    @Override
    protected float getGravity() {
        return 0.035F;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0, 0.01, 0);
        if (tickCount > 200) discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        hit.getEntity().hurt(damageSources().thrown(this, getOwner()), 10.0F);
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide) {
            level().explode(this, getX(), getY(), getZ(), 2.2F, Level.ExplosionInteraction.NONE);
            discard();
        }
    }
}
