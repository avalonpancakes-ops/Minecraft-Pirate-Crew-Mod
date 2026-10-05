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
            for (var check : CHECKS) check.accept(sea);
            PirateCrew.LOGGER.info("PIRATECREW SMOKETEST OK");
        } catch (Throwable t) {
            PirateCrew.LOGGER.error("PIRATECREW SMOKETEST FAIL", t);
        }
        server.halt(false);
    }
}
