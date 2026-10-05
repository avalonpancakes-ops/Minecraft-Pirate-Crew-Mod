package com.piratecrew.client;

import com.piratecrew.PirateCrew;
import com.piratecrew.client.codex.CodexScreen;
import com.piratecrew.entity.MarineEntity;
import com.piratecrew.entity.PirateEntity;
import com.piratecrew.item.GearTier;
import com.piratecrew.registry.ModEntities;
import com.piratecrew.registry.ModItems;
import com.piratecrew.sundered.SirenTeleporter;
import com.piratecrew.sundered.SunderedSea;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * CI only (-Dpiratecrew.clientshots=true): starts the client under a virtual display, opens the mod's
 * screens, builds a world with the mod's gear, mobs and bosses on show, takes screenshots of it all
 * into run/screenshots, then quits. Lets the art and UI be checked without playing.
 */
@Mod.EventBusSubscriber(modid = PirateCrew.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientShots {
    private static final boolean ON = Boolean.getBoolean("piratecrew.clientshots");

    private record Step(String name, int delay, Consumer<Minecraft> setup) {}

    private static final List<Step> STEPS = new ArrayList<>();
    private static final int WORLD_FROM = 9;   // steps from here on need the world loaded
    private static int index = -1, frames, idle;
    private static boolean started, setupDone;

    private static void build() {
        // --- title screen: guidebook and item art
        STEPS.add(new Step("codex_start", 40, mc -> CodexScreen.openAt(0, 0, false)));
        STEPS.add(new Step("codex_boss", 30, mc -> CodexScreen.openAt(3, 4, false)));
        STEPS.add(new Step("codex_kraken", 30, mc -> CodexScreen.openAt(3, 1, false)));
        STEPS.add(new Step("codex_gear", 30, mc -> CodexScreen.openAt(5, 5, false)));
        STEPS.add(new Step("codex_pact", 30, mc -> CodexScreen.openAt(4, 0, false)));
        STEPS.add(new Step("codex_showcase", 30, mc -> CodexScreen.openAt(0, 0, true)));
        STEPS.add(new Step("items_a", 30, mc -> mc.setScreen(new GalleryScreen())));
        STEPS.add(new Step("items_b", 9, mc -> { }));
        // --- a world to look at
        STEPS.add(new Step("", 0, ClientShots::createWorld));
        STEPS.add(new Step("", 200, mc -> { }));   // let the world settle
        STEPS.add(new Step("bank", 40, mc -> mc.setScreen(new BankScreen())));
        STEPS.add(new Step("shop", 30, mc -> mc.setScreen(new ShopScreen())));
        STEPS.add(new Step("crew", 30, mc -> mc.setScreen(new CrewScreen())));
        STEPS.add(new Step("armor_lineup", 120, mc -> lineup(mc, true)));
        STEPS.add(new Step("boss_lineup", 120, mc -> lineup(mc, false)));
        STEPS.add(new Step("sea_beasts", 120, ClientShots::beasts));
        STEPS.add(new Step("held_sword", 40, mc -> held(mc, ModItems.GEAR.get(GearTier.SOVEREIGN).sword())));
        STEPS.add(new Step("held_storm", 20, mc -> held(mc, ModItems.GEAR.get(GearTier.STORMFORGED).sword())));
        STEPS.add(new Step("portal", 80, ClientShots::portal));
        STEPS.add(new Step("", 60, ClientShots::sea));
        STEPS.add(new Step("sundered_sea", 400, ClientShots::hover));
    }

    @SubscribeEvent
    public static void renderEnd(TickEvent.RenderTickEvent event) {
        if (!ON || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (!started) {
            if (!(mc.screen instanceof TitleScreen) || mc.getOverlay() != null) return;
            if (++idle < 60) return;
            started = true;
            mc.options.renderDistance().set(6);
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
            build();
            next(mc);
            return;
        }
        if (index >= STEPS.size()) return;
        Step s = STEPS.get(index);
        // world steps wait for the world to be ready
        if (index >= WORLD_FROM) {
            if (mc.level == null || mc.player == null || (!setupDone && mc.screen != null && index == WORLD_FROM)) return;
            if (!setupDone) {
                setupDone = true;
                run(mc);
            }
        }
        if (++frames < s.delay()) return;
        if (!s.name().isEmpty()) {
            Screenshot.grab(mc.gameDirectory, s.name() + ".png", mc.getMainRenderTarget(), msg -> { });
            PirateCrew.LOGGER.info("PIRATECREW CLIENTSHOT {}", s.name());
        }
        next(mc);
    }

    private static void next(Minecraft mc) {
        index++;
        frames = 0;
        if (index >= STEPS.size()) {
            PirateCrew.LOGGER.info("PIRATECREW CLIENTSHOTS DONE");
            mc.stop();
            return;
        }
        setupDone = false;
        if (index < WORLD_FROM) {
            setupDone = true;
            run(mc);
        }
    }

    private static void run(Minecraft mc) {
        try {
            STEPS.get(index).setup().accept(mc);
        } catch (Throwable t) {
            PirateCrew.LOGGER.error("PIRATECREW CLIENTSHOT step {} failed", STEPS.get(index).name(), t);
        }
    }

    // ------------------------------------------------------------------ world

    private static void createWorld(Minecraft mc) {
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        LevelSettings settings = new LevelSettings("shots", GameType.CREATIVE, false, Difficulty.EASY, true, rules, WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel("shots", settings, new WorldOptions(20261005L, false, false),
                ra -> ra.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
    }

    private static void onServer(Minecraft mc, Consumer<ServerPlayer> task) {
        var server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) return;
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayer(mc.player.getUUID());
            if (sp != null) task.accept(sp);
        });
    }

    /** Height of the ground, loading the chunk first (unloaded chunks report the bottom of the world). */
    private static int ground(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    private static void clearBosses(ServerLevel level) {
        for (var e : level.getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(-200, -100, -200, 200, 400, 200),
                e -> e instanceof com.piratecrew.entity.boss.BountyBoss || e instanceof MarineEntity || e instanceof PirateEntity)) e.discard();
    }

    private static void look(ServerPlayer sp, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        sp.getAbilities().flying = true;
        sp.onUpdateAbilities();
        sp.teleportTo(level, x, y, z, yaw, pitch);
    }

    private static <T extends Mob> T place(ServerLevel level, EntityType<T> type, double x, double y, double z, float yaw) {
        T m = type.create(level);
        if (m == null) return null;
        m.moveTo(x, y, z, yaw, 0);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(x, y, z)), MobSpawnType.COMMAND, null, null);
        m.setNoAi(true);
        m.setYHeadRot(yaw);
        m.yBodyRot = yaw;
        level.addFreshEntity(m);
        return m;
    }

    private static void lineup(Minecraft mc, boolean armor) {
        mc.setScreen(null);
        mc.options.hideGui = true;
        onServer(mc, sp -> {
            ServerLevel level = sp.serverLevel();
            level.setDayTime(6000);
            int z0 = armor ? 0 : 40;
            int ground = ground(level, 0, z0 + 6);
            if (armor) {
                int i = 0;
                for (GearTier t : GearTier.values()) {
                    PirateEntity p = place(level, ModEntities.PIRATE.get(), -6.25 + i * 2.5, ground, z0 + 6.5, 180F);
                    if (p != null) {
                        var set = ModItems.GEAR.get(t);
                        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(set.helmet().get()));
                        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(set.chestplate().get()));
                        p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(set.leggings().get()));
                        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(set.boots().get()));
                        p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(set.sword().get()));
                    }
                    i++;
                }
            } else {
                place(level, ModEntities.COMMODORE.get(), -4.5, ground, z0 + 6.5, 180F);
                place(level, ModEntities.TEMPEST_ADMIRAL.get(), -1.5, ground, z0 + 6.5, 180F);
                place(level, ModEntities.FLEET_ADMIRAL.get(), 1.5, ground, z0 + 6.5, 180F);
                int i = 0;
                for (MarineEntity.Rank r : MarineEntity.Rank.values()) {
                    MarineEntity m = ModEntities.MARINE.get().create(level);
                    if (m == null) continue;
                    m.moveTo(4.5 + (i % 2) * 1.6, ground, z0 + 5.5 + (i / 2) * 2.0, 180F, 0);
                    m.setupMarine(r);
                    m.setNoAi(true);
                    m.setYHeadRot(180F);
                    m.yBodyRot = 180F;
                    level.addFreshEntity(m);
                    i++;
                }
            }
            look(sp, level, 0.5, ground + (armor ? 1.2 : 1.4), z0 + (armor ? 1.0 : -0.5), 0F, armor ? 10F : 8F);
        });
    }

    private static void beasts(Minecraft mc) {
        onServer(mc, sp -> {
            ServerLevel level = sp.serverLevel();
            clearBosses(level);
            int ground = ground(level, 0, 100);
            place(level, ModEntities.KRAKEN.get(), -5, ground + 1, 112, 200F);
            place(level, ModEntities.LEVIATHAN.get(), 6, ground + 2, 114, 160F);
            look(sp, level, 0.5, ground + 4, 96, 0F, 5F);
        });
    }

    private static void held(Minecraft mc, RegistryObject<Item> item) {
        mc.options.hideGui = false;
        onServer(mc, sp -> {
            sp.getInventory().selected = 0;
            sp.getInventory().setItem(0, new ItemStack(item.get()));
            sp.getInventory().setItem(1, new ItemStack(ModItems.GEAR.get(GearTier.LEVIATHAN).sword().get()));
            sp.getInventory().setItem(2, new ItemStack(ModItems.GEAR.get(GearTier.KRAKENBONE).pickaxe().get()));
            sp.getInventory().setItem(3, new ItemStack(ModItems.SOVEREIGN_HEART.get()));
            sp.getInventory().setItem(4, new ItemStack(ModItems.STORM_CORE.get()));
            sp.getInventory().setItem(5, new ItemStack(ModItems.ADMIRALS_WARRANT.get()));
            sp.getInventory().setItem(6, new ItemStack(ModItems.SIREN_CONCH.get()));
            sp.getInventory().setItem(7, new ItemStack(ModItems.SOUL_PACTS.get(com.piratecrew.pact.SoulPact.TEMPEST).get()));
            sp.getInventory().setItem(8, new ItemStack(ModItems.CAPTAINS_LOG.get()));
            ServerLevel level = sp.serverLevel();
            clearBosses(level);
            int ground = ground(level, 0, -40);
            look(sp, level, 0.5, ground + 1.6, -40, 0F, 0F);
        });
    }

    private static void portal(Minecraft mc) {
        mc.options.hideGui = true;
        onServer(mc, sp -> {
            ServerLevel level = sp.serverLevel();
            int ground = ground(level, 0, -80);
            SirenTeleporter.build(level, new BlockPos(0, ground - 1, -74));
            look(sp, level, 0.5, ground + 1.5, -82, 0F, -5F);
        });
    }

    private static void sea(Minecraft mc) {
        onServer(mc, sp -> {
            ServerLevel sea = sp.server.getLevel(SunderedSea.LEVEL);
            if (sea == null) return;
            sea.setDayTime(6000);
            look(sp, sea, 0.5, 120, 0.5, 30F, 25F);
        });
    }

    /** Flying is reset by the dimension change: turn it back on and hold position over the islands. */
    private static void hover(Minecraft mc) {
        onServer(mc, sp -> look(sp, sp.serverLevel(), 0.5, 120, 0.5, 30F, 25F));
    }

    // ------------------------------------------------------------------ item gallery

    /** Every Pirate Crew item on parchment, for the screenshot. */
    public static class GalleryScreen extends Screen {
        public GalleryScreen() {
            super(Component.literal("Gallery"));
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
            renderBackground(g);
            int cols = 14, size = 26;
            List<ItemStack> items = new ArrayList<>();
            for (RegistryObject<Item> r : ModItems.ITEMS.getEntries()) items.add(new ItemStack(r.get()));
            int rowsN = (items.size() + cols - 1) / cols;
            int w = cols * size + 16, h = rowsN * size + 16;
            int x0 = (width - w) / 2, y0 = (height - h) / 2;
            GuiDraw.panel(g, x0, y0, w, h);
            for (int i = 0; i < items.size(); i++) {
                int x = x0 + 8 + (i % cols) * size, y = y0 + 8 + (i / cols) * size;
                GuiDraw.slot(g, x + 3, y + 3);
                g.renderItem(items.get(i), x + 3, y + 3);
            }
        }
    }
}
