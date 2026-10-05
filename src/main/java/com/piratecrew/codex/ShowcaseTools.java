package com.piratecrew.codex;

import com.piratecrew.bank.LoanManager;
import com.piratecrew.entity.MarineEntity;
import com.piratecrew.entity.RaiderPirateEntity;
import com.piratecrew.entity.boss.BountyBoss;
import com.piratecrew.item.GearTier;
import com.piratecrew.pact.SoulPact;
import com.piratecrew.registry.ModEntities;
import com.piratecrew.registry.ModItems;
import com.piratecrew.sundered.SirenTeleporter;
import com.piratecrew.sundered.SunderedSea;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** The Captain's Log showcase page: one-click tools for operators (permission level 2). */
public class ShowcaseTools {
    public enum Action {
        SOVEREIGN_KIT, ALL_TIERS, BOSS_SUMMONS, ALL_PACTS,
        BOSS_COMMODORE, BOSS_KRAKEN, BOSS_TEMPEST, BOSS_LEVIATHAN, BOSS_VANE,
        BUILD_PORTAL, TO_SEA, OUTPOST, SHRINE, CAMP, MARINES, RAIDERS,
        HEAL, CLEAR, DAY, NIGHT, RUBIES;

        public static Action byId(int id) {
            Action[] v = values();
            return id >= 0 && id < v.length ? v[id] : HEAL;
        }
    }

    public static void run(ServerPlayer p, Action a) {
        if (!p.hasPermissions(2)) {
            p.displayClientMessage(Component.literal("The showcase tools need operator permissions.").withStyle(ChatFormatting.RED), true);
            return;
        }
        ServerLevel level = p.serverLevel();
        String done = switch (a) {
            case SOVEREIGN_KIT -> {
                giveSet(p, GearTier.SOVEREIGN, true);
                yield "A full Sovereign kit is yours.";
            }
            case ALL_TIERS -> {
                for (GearTier t : GearTier.values()) giveSet(p, t, false);
                yield "One of every Sundered Sea set.";
            }
            case BOSS_SUMMONS -> {
                for (var item : List.of(ModItems.SIGNAL_FLARE, ModItems.KRAKEN_LURE, ModItems.STORM_SIGIL, ModItems.LEVIATHAN_HORN, ModItems.ADMIRALS_WARRANT)) {
                    give(p, new ItemStack(item.get(), 4));
                }
                give(p, new ItemStack(ModItems.SIREN_CONCH.get(), 4));
                yield "Boss summons and Siren Conches.";
            }
            case ALL_PACTS -> {
                for (SoulPact pact : SoulPact.values()) give(p, new ItemStack(ModItems.SOUL_PACTS.get(pact).get()));
                yield "All ten Soul Pacts.";
            }
            case BOSS_COMMODORE -> boss(p, ModEntities.COMMODORE.get(), false);
            case BOSS_KRAKEN -> boss(p, ModEntities.KRAKEN.get(), true);
            case BOSS_TEMPEST -> boss(p, ModEntities.TEMPEST_ADMIRAL.get(), false);
            case BOSS_LEVIATHAN -> boss(p, ModEntities.LEVIATHAN.get(), true);
            case BOSS_VANE -> boss(p, ModEntities.FLEET_ADMIRAL.get(), false);
            case BUILD_PORTAL -> {
                BlockPos at = ahead(level, p, 5);
                SirenTeleporter.build(level, at);
                yield "A lit Siren portal stands before you.";
            }
            case TO_SEA -> {
                boolean inSea = level.dimension() == SunderedSea.LEVEL;
                ServerLevel to = level.getServer().getLevel(inSea ? Level.OVERWORLD : SunderedSea.LEVEL);
                if (to == null) yield "That world couldn't be reached.";
                BlockPos at = BlockPos.containing(p.getX(), 0, p.getZ());
                to.getChunk(at);
                int y = Math.max(to.getSeaLevel() + 1, to.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ()));
                p.teleportTo(to, p.getX(), y + 1, p.getZ(), p.getYRot(), p.getXRot());
                yield inSea ? "Back to the Overworld." : "Welcome to the Sundered Sea.";
            }
            case OUTPOST -> {
                com.piratecrew.sundered.OutpostBuilder.build(level, ahead(level, p, 14).below());
                yield "A marine outpost.";
            }
            case SHRINE -> {
                com.piratecrew.sundered.ShrineBuilder.build(level, ahead(level, p, 10).below());
                yield "A Pact Shrine.";
            }
            case CAMP -> {
                BlockPos floor = ahead(level, p, 14).below();
                com.piratecrew.world.CampBuilder.build(level, floor);
                com.piratecrew.world.CampData.get(level).add(floor);
                yield "A raider camp.";
            }
            case MARINES -> {
                int n = com.piratecrew.sundered.Marines.spawnSquad(level, p.blockPosition().relative(p.getDirection(), 10), 4, false).size();
                yield "A marine squad of " + n + ".";
            }
            case RAIDERS -> {
                List<Vec3> spots = new ArrayList<>();
                for (int i = 0; i < 6; i++) {
                    Vec3 s = LoanManager.findSpot(level, p.blockPosition(), 6, 12, p.getRandom());
                    if (s != null) spots.add(s);
                }
                int n = spots.isEmpty() ? 0 : com.piratecrew.entity.RaiderCrews.spawnCrew(level, spots, 4 + p.getRandom().nextInt(3), false).size();
                yield n == 0 ? "No room for raiders here." : "An enemy crew of " + n + ".";
            }
            case HEAL -> {
                p.setHealth(p.getMaxHealth());
                p.getFoodData().setFoodLevel(20);
                p.getFoodData().setSaturation(20);
                p.clearFire();
                p.removeAllEffects();
                yield "Healed and fed.";
            }
            case CLEAR -> {
                int n = 0;
                for (Entity e : level.getEntities((Entity) null, p.getBoundingBox().inflate(96),
                        e -> e instanceof MarineEntity || e instanceof RaiderPirateEntity || e instanceof BountyBoss)) {
                    e.discard();
                    n++;
                }
                yield "Cleared " + n + " marines, raiders and bosses.";
            }
            case DAY -> {
                level.setDayTime(1000);
                yield "Morning.";
            }
            case NIGHT -> {
                level.setDayTime(13000);
                yield "Night falls.";
            }
            case RUBIES -> {
                give(p, new ItemStack(ModItems.RUBY_BLOCK.get(), 64));
                yield "A stack of ruby blocks (576 rubies).";
            }
        };
        p.displayClientMessage(Component.literal("✦ " + done).withStyle(ChatFormatting.GOLD), true);
    }

    private static BlockPos ahead(ServerLevel level, ServerPlayer p, int dist) {
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0E-4) flat = new Vec3(0, 0, 1);
        flat = flat.normalize();
        int x = (int) Math.floor(p.getX() + flat.x * dist), z = (int) Math.floor(p.getZ() + flat.z * dist);
        level.getChunk(x >> 4, z >> 4);
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    private static String boss(ServerPlayer p, EntityType<? extends Mob> type, boolean water) {
        ServerLevel level = p.serverLevel();
        Mob mob = type.create(level);
        if (mob == null) return "Couldn't summon that boss.";
        BlockPos at = ahead(level, p, water ? 14 : 8);
        if (water) at = new BlockPos(at.getX(), Math.min(at.getY(), level.getSeaLevel() - 6), at.getZ());
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getYRot() + 180F, 0);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
        level.addFreshEntity(mob);
        return mob.getDisplayName().getString() + " has arrived (switch to survival to fight).";
    }

    private static void giveSet(ServerPlayer p, GearTier t, boolean enchanted) {
        for (var item : ModItems.GEAR.get(t).all()) {
            ItemStack s = new ItemStack(item.get());
            if (enchanted) {
                s.enchant(Enchantments.UNBREAKING, 3);
                s.enchant(Enchantments.MENDING, 1);
                if (s.getItem() instanceof net.minecraft.world.item.SwordItem) s.enchant(Enchantments.SHARPNESS, 5);
                if (s.getItem() instanceof net.minecraft.world.item.ArmorItem) s.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
            }
            give(p, s);
        }
    }

    private static void give(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s)) p.drop(s, false);
    }
}
