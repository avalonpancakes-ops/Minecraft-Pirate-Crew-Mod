package com.piratecrew.compat;

import com.piratecrew.PirateCrew;
import com.piratecrew.entity.RaiderCrews;
import com.piratecrew.entity.RaiderPirateEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Valkyrien Pirates (with Valkyrien Skies + Eureka) sails pirate ships whose helmsmen and cannoneers
 * spawn once the ship starts moving. When that crew appears in the Overworld, this boards the ship
 * with an NPC pirate crew from this mod: a captain with the best gear and a crew with random gear.
 *
 * No code from either mod is used: the ship crew is recognised by its entity id and saved data, so
 * Pirate Crew works the same with or without them installed.
 */
public class ValkyrienPiratesCompat {
    public static final String MODID = "pirates";
    private static final ResourceLocation VP_PIRATE = new ResourceLocation(MODID, "pirate");
    /** Wait this long after a ship's crew starts appearing, so the whole crew is there. */
    private static final int GATHER_TICKS = 40;
    private static final double SAME_SHIP = 40.0;

    private record Sighting(ServerLevel level, UUID id, long time) {}

    private static final List<Sighting> PENDING = new ArrayList<>();
    private static Boolean loaded;

    public static boolean isLoaded() {
        if (loaded == null) loaded = ModList.get() != null && ModList.get().isLoaded(MODID);
        return loaded;
    }

    /**
     * With Valkyrien Pirates installed, switch on a built-in data pack that adds a second, denser
     * placement of its ships in the Sundered Sea (on top of the normal one its biome tags give).
     */
    public static void addPacks(net.minecraftforge.event.AddPackFindersEvent event) {
        if (event.getPackType() != net.minecraft.server.packs.PackType.SERVER_DATA || !isLoaded()) return;
        var file = ModList.get().getModFileById(PirateCrew.MODID);
        if (file == null) return;
        java.nio.file.Path root = file.getFile().findResource("resourcepacks/vp_sundered");
        net.minecraft.server.packs.repository.Pack pack = net.minecraft.server.packs.repository.Pack.readMetaAndCreate(
                "builtin/piratecrew_sundered_ships", net.minecraft.network.chat.Component.literal("Pirate Crew: Sundered Sea ships"), true,
                id -> new net.minecraft.server.packs.PathPackResources(id, root, true),
                net.minecraft.server.packs.PackType.SERVER_DATA, net.minecraft.server.packs.repository.Pack.Position.TOP,
                net.minecraft.server.packs.repository.PackSource.BUILT_IN);
        if (pack != null) event.addRepositorySource(consumer -> consumer.accept(pack));
    }

    public static void onJoin(EntityJoinLevelEvent event) {
        if (!isLoaded() || event.loadedFromDisk() || !(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD && level.dimension() != com.piratecrew.sundered.SunderedSea.LEVEL) return;
        Entity e = event.getEntity();
        if (!VP_PIRATE.equals(ForgeRegistries.ENTITY_TYPES.getKey(e.getType()))) return;
        PENDING.add(new Sighting(level, e.getUUID(), level.getGameTime()));
    }

    /** A Valkyrien Pirates crew member manning a cannon or the helm (wreck skeletons man nothing). */
    private static boolean mansAStation(Entity e) {
        CompoundTag tag = e.saveWithoutId(new CompoundTag());
        return tag.contains("BlockToDisableX")
                && (tag.getInt("BlockToDisableX") != 0 || tag.getInt("BlockToDisableY") != 0 || tag.getInt("BlockToDisableZ") != 0);
    }

    public static void tick(MinecraftServer server) {
        if (PENDING.isEmpty() || server.getTickCount() % 10 != 0) return;
        List<Sighting> ready = new ArrayList<>();
        for (Iterator<Sighting> it = PENDING.iterator(); it.hasNext(); ) {
            Sighting s = it.next();
            if (s.level.getGameTime() - s.time >= GATHER_TICKS) {
                ready.add(s);
                it.remove();
            }
        }
        // Group the crew members that turned up together into ships.
        List<Entity> crew = new ArrayList<>();
        for (Sighting s : ready) {
            Entity e = s.level.getEntity(s.id);
            if (e != null && e.isAlive()) crew.add(e);
        }
        while (!crew.isEmpty()) {
            Entity first = crew.remove(0);
            List<Entity> ship = new ArrayList<>();
            ship.add(first);
            for (Iterator<Entity> it = crew.iterator(); it.hasNext(); ) {
                Entity e = it.next();
                if (e.level() == first.level() && e.distanceToSqr(first) < SAME_SHIP * SAME_SHIP) {
                    ship.add(e);
                    it.remove();
                }
            }
            boardShip((ServerLevel) first.level(), ship);
        }
    }

    private static void boardShip(ServerLevel level, List<Entity> shipCrew) {
        if (shipCrew.stream().noneMatch(ValkyrienPiratesCompat::mansAStation)) return; // a wreck, not a sailing ship
        Entity anchor = shipCrew.get(0);
        // Already boarded (the crew respawned, or two sightings of the same ship)?
        if (!level.getEntitiesOfClass(RaiderPirateEntity.class, new AABB(anchor.blockPosition()).inflate(SAME_SHIP),
                RaiderPirateEntity::isCaptain).isEmpty()) return;
        List<Vec3> spots = new ArrayList<>();
        for (Entity e : shipCrew) spots.add(e.position());
        int crewSize = Math.max(3, Math.min(8, shipCrew.size() + 1 + level.getRandom().nextInt(3)));
        if (level.dimension() == com.piratecrew.sundered.SunderedSea.LEVEL) crewSize += 2;   // the Sundered Sea's crews are bigger
        List<RaiderPirateEntity> spawned = RaiderCrews.spawnCrew(level, spots, crewSize, true);
        if (!spawned.isEmpty()) {
            PirateCrew.LOGGER.debug("Pirate Crew: boarded a Valkyrien Pirates ship at {} with {} pirates", anchor.blockPosition(), spawned.size());
        }
    }
}
