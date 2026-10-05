package com.piratecrew.debug;

import com.piratecrew.PirateCrew;
import com.piratecrew.sundered.SunderedSea;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Build check only (never active in normal play): when the server is started with
 * -Dpiratecrew.smoketest=true, it generates terrain in the Sundered Sea and spawns the mod's mobs,
 * logs the result and shuts down. CI uses it to catch broken worldgen data that compiling can't.
 */
@Mod.EventBusSubscriber(modid = PirateCrew.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SmokeTest {
    public static final java.util.List<java.util.function.Consumer<ServerLevel>> CHECKS = new java.util.ArrayList<>();

    /** A top-down picture of the islands around 0,0 (one pixel per block), for checking the terrain by eye. */
    private static void renderMap(ServerLevel sea) throws java.io.IOException {
        int chunksAcross = 32, size = chunksAcross * 16, origin = -size / 2;
        java.util.Map<String, Integer> biomes = new java.util.TreeMap<>();
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_RGB);
        for (int cx = 0; cx < chunksAcross; cx++) {
            for (int cz = 0; cz < chunksAcross; cz++) {
                var chunk = sea.getChunk((origin >> 4) + cx, (origin >> 4) + cz, ChunkStatus.SURFACE, true);
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        int h = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                        var biome = chunk.getNoiseBiome(x >> 2, Math.max(h, sea.getSeaLevel()) >> 2, z >> 2);
                        String b = biome.unwrapKey().map(k -> k.location().getPath()).orElse("?");
                        biomes.merge(b, 1, Integer::sum);
                        int rgb;
                        if (h <= sea.getSeaLevel()) {
                            int depth = Math.min(40, sea.getSeaLevel() - h);
                            rgb = rgb(20, 120 - depth * 2, 190 - depth * 2);
                        } else if (h <= 66) {
                            rgb = rgb(222, 206, 140);
                        } else {
                            float shade = Math.min(1.0F, 0.55F + (h - 66) / 80.0F);
                            int[] base = b.contains("storm") ? new int[]{50, 100, 70} : b.contains("ember") ? new int[]{90, 70, 70} : new int[]{80, 180, 60};
                            rgb = rgb((int) (base[0] * shade), (int) (base[1] * shade), (int) (base[2] * shade));
                        }
                        img.setRGB(cx * 16 + x, cz * 16 + z, rgb);
                    }
                }
            }
        }
        PirateCrew.LOGGER.info("PIRATECREW SMOKETEST biome columns: {}", biomes);
        java.io.File dir = new java.io.File("smoke");
        dir.mkdirs();
        javax.imageio.ImageIO.write(img, "png", new java.io.File(dir, "sundered_map.png"));
        PirateCrew.LOGGER.info("PIRATECREW SMOKETEST map written to {}", new java.io.File(dir, "sundered_map.png").getAbsolutePath());
    }

    private static int rgb(int r, int g, int b) {
        return (Math.max(0, Math.min(255, r)) << 16) | (Math.max(0, Math.min(255, g)) << 8) | Math.max(0, Math.min(255, b));
    }

    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        if (!Boolean.getBoolean("piratecrew.smoketest")) return;
        MinecraftServer server = event.getServer();
        try {
            ServerLevel sea = server.getLevel(SunderedSea.LEVEL);
            if (sea == null) throw new IllegalStateException("Sundered Sea dimension is missing");
            int land = 0, chunks = 0;
            int[][] spots = {{0, 0}, {40, 0}, {0, 40}, {-60, -30}, {90, 70}, {-120, 110}};
            for (int[] s : spots) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        var chunk = sea.getChunk(s[0] + dx, s[1] + dz, ChunkStatus.FULL, true);
                        chunks++;
                        int x = ((s[0] + dx) << 4) + 8, z = ((s[1] + dz) << 4) + 8;
                        int h = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, x & 15, z & 15);
                        if (h > sea.getSeaLevel()) land++;
                    }
                }
            }
            PirateCrew.LOGGER.info("PIRATECREW SMOKETEST generated {} Sundered Sea chunks ({} with land above the sea)", chunks, land);
            renderMap(sea);
            // Marines and an outpost on the first flat island ground found.
            net.minecraft.core.BlockPos outpost = null;
            int tried = 0;
            for (int x = -240; x <= 240 && outpost == null && tried < 40; x += 16) {
                for (int z = -240; z <= 240 && outpost == null && tried < 40; z += 16) {
                    var rough = sea.getChunk(x >> 4, z >> 4, ChunkStatus.SURFACE, true);
                    int h = rough.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, x & 15, z & 15);
                    if (h <= sea.getSeaLevel() + 3) continue;
                    tried++;
                    for (int cx = (x - 8) >> 4; cx <= (x + 8) >> 4; cx++) for (int cz = (z - 8) >> 4; cz <= (z + 8) >> 4; cz++) sea.getChunk(cx, cz);
                    outpost = com.piratecrew.sundered.SunderedStructures.flatIslandGround(sea, x, z, 7);
                }
            }
            PirateCrew.LOGGER.info("PIRATECREW SMOKETEST checked {} island spots for an outpost", tried);
            if (outpost != null) {
                com.piratecrew.sundered.OutpostBuilder.build(sea, outpost);
                int marines = sea.getEntitiesOfClass(com.piratecrew.entity.MarineEntity.class, new net.minecraft.world.phys.AABB(outpost).inflate(16)).size();
                PirateCrew.LOGGER.info("PIRATECREW SMOKETEST outpost built at {} with {} marines", outpost, marines);
            } else {
                PirateCrew.LOGGER.info("PIRATECREW SMOKETEST no flat island ground found for an outpost test");
            }
            // Every boss spawns, sets itself up and survives a few ticks of AI.
            net.minecraft.core.BlockPos bossAt = outpost != null ? outpost.above(2) : new net.minecraft.core.BlockPos(0, sea.getSeaLevel() - 6, 0);
            for (var type : java.util.List.<net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>>of(
                    com.piratecrew.registry.ModEntities.COMMODORE.get(), com.piratecrew.registry.ModEntities.KRAKEN.get(),
                    com.piratecrew.registry.ModEntities.TEMPEST_ADMIRAL.get(), com.piratecrew.registry.ModEntities.LEVIATHAN.get(),
                    com.piratecrew.registry.ModEntities.FLEET_ADMIRAL.get())) {
                net.minecraft.world.entity.Mob boss = type.create(sea);
                if (boss == null) throw new IllegalStateException("couldn't create " + type);
                boss.moveTo(bossAt.getX() + 0.5, bossAt.getY(), bossAt.getZ() + 0.5, 0, 0);
                boss.finalizeSpawn(sea, sea.getCurrentDifficultyAt(bossAt), net.minecraft.world.entity.MobSpawnType.EVENT, null, null);
                sea.addFreshEntity(boss);
                for (int i = 0; i < 40; i++) boss.tick();
                PirateCrew.LOGGER.info("PIRATECREW SMOKETEST boss {} health {}/{} armor {} name {}", type.getDescriptionId(), boss.getHealth(),
                        boss.getMaxHealth(), boss.getArmorValue(), boss.getDisplayName().getString());
                if (boss.getMaxHealth() < 500) throw new IllegalStateException("boss health cap not raised: " + boss.getMaxHealth());
                boss.discard();
            }
            // Every Soul Pact power fires from a crew pirate at a target without errors.
            var dummy = net.minecraft.world.entity.EntityType.ZOMBIE.create(sea);
            var caster = com.piratecrew.registry.ModEntities.PIRATE.get().create(sea);
            if (dummy != null && caster != null) {
                dummy.moveTo(bossAt.getX() + 4.5, bossAt.getY(), bossAt.getZ() + 0.5, 0, 0);
                caster.moveTo(bossAt.getX() + 0.5, bossAt.getY(), bossAt.getZ() + 0.5, 0, 0);
                caster.finalizeSpawn(sea, sea.getCurrentDifficultyAt(bossAt), net.minecraft.world.entity.MobSpawnType.EVENT, null, null);
                sea.addFreshEntity(dummy);
                sea.addFreshEntity(caster);
                StringBuilder fired = new StringBuilder();
                for (var pact : com.piratecrew.pact.SoulPact.values()) {
                    caster.bindPact(pact);
                    boolean ok = com.piratecrew.pact.PactPowers.activate(caster, pact, dummy, dummy.getEyePosition());
                    com.piratecrew.pact.PactPowers.passives(caster, pact);
                    fired.append(pact.id).append(ok ? "+ " : "- ");
                }
                PirateCrew.LOGGER.info("PIRATECREW SMOKETEST pact powers: {} (pirate now '{}')", fired.toString().trim(), caster.getDisplayName().getString());
                dummy.discard();
                caster.discard();
            }
            if (outpost != null) {
                int sx = outpost.getX() + 24, sz = outpost.getZ();
                for (int cx = (sx - 8) >> 4; cx <= (sx + 8) >> 4; cx++) for (int cz = (sz - 8) >> 4; cz <= (sz + 8) >> 4; cz++) sea.getChunk(cx, cz);
                int sh = sea.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz);
                com.piratecrew.sundered.ShrineBuilder.build(sea, new net.minecraft.core.BlockPos(sx, sh - 1, sz));
                PirateCrew.LOGGER.info("PIRATECREW SMOKETEST pact shrine built at {} {} {}", sx, sh - 1, sz);
            }
            for (var check : CHECKS) check.accept(sea);
            PirateCrew.LOGGER.info("PIRATECREW SMOKETEST OK");
        } catch (Throwable t) {
            PirateCrew.LOGGER.error("PIRATECREW SMOKETEST FAIL", t);
        }
        server.halt(false);
    }
}
