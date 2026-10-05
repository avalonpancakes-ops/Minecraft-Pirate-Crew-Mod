package com.piratecrew.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/** One class drives all the mod's particles; each kind has its own motion, life, size and glow. */
public class PactParticle extends TextureSheetParticle {
    public enum Kind { EMBER, FROST, SPARK, WISP, BLOOD, GLYPH }

    private final SpriteSet sprites;
    private final Kind kind;
    private final float startSize;

    protected PactParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites, Kind kind) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        this.kind = kind;
        this.xd = vx + (random.nextDouble() - 0.5) * 0.02;
        this.yd = vy + (random.nextDouble() - 0.5) * 0.02;
        this.zd = vz + (random.nextDouble() - 0.5) * 0.02;
        this.hasPhysics = kind == Kind.BLOOD;
        switch (kind) {
            case EMBER -> { lifetime = 18 + random.nextInt(14); gravity = -0.04F; quadSize *= 0.9F; yd += 0.03; }
            case FROST -> { lifetime = 30 + random.nextInt(20); gravity = 0.02F; quadSize *= 1.1F; roll = random.nextFloat() * Mth.TWO_PI; }
            case SPARK -> { lifetime = 6 + random.nextInt(6); gravity = 0; quadSize *= 0.8F; }
            case WISP -> { lifetime = 24 + random.nextInt(16); gravity = -0.015F; quadSize *= 1.6F; }
            case BLOOD -> { lifetime = 20 + random.nextInt(10); gravity = 0.9F; quadSize *= 0.7F; }
            case GLYPH -> { lifetime = 30; gravity = -0.01F; quadSize *= 1.5F; xd *= 0.2; zd *= 0.2; yd = 0.04; }
        }
        this.startSize = quadSize;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!isAlive()) return;
        setSpriteFromAge(sprites);
        float life = age / (float) lifetime;
        switch (kind) {
            case EMBER -> { quadSize = startSize * (1 - life * 0.7F); alpha = 1 - life * life; xd += (random.nextDouble() - 0.5) * 0.01; }
            case FROST -> { oRoll = roll; roll += 0.08F; alpha = 1 - life * 0.8F; }
            case SPARK -> { xd += (random.nextDouble() - 0.5) * 0.1; zd += (random.nextDouble() - 0.5) * 0.1; alpha = 1 - life; }
            case WISP -> { quadSize = startSize * (1 + life); alpha = 0.8F * (1 - life); xd *= 0.9; zd *= 0.9; }
            case BLOOD -> alpha = 1 - life * 0.5F;
            case GLYPH -> { alpha = life < 0.2F ? life * 5 : 1 - (life - 0.2F) / 0.8F; quadSize = startSize * (1 + life * 0.3F); }
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return kind == Kind.BLOOD || kind == Kind.WISP ? super.getLightColor(partialTick) : 0xF000F0;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Kind kind;

        public Provider(SpriteSet sprites, Kind kind) {
            this.sprites = sprites;
            this.kind = kind;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new PactParticle(level, x, y, z, vx, vy, vz, sprites, kind);
        }
    }
}
