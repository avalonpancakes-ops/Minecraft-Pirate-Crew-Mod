package com.piratecrew.registry;

import com.piratecrew.PirateCrew;
import com.piratecrew.block.BankCounterBlock;
import com.piratecrew.block.BountyBoardBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, PirateCrew.MODID);

    // Mineable with stone pickaxe or better (see data/minecraft/tags/blocks/needs_stone_tool.json)
    public static final RegistryObject<Block> RUBY_ORE = BLOCKS.register("ruby_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 3.0F),
                    UniformInt.of(3, 7)));

    public static final RegistryObject<Block> DEEPSLATE_RUBY_ORE = BLOCKS.register("deepslate_ruby_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(4.5F, 3.0F)
                    .sound(SoundType.DEEPSLATE),
                    UniformInt.of(3, 7)));

    public static final RegistryObject<Block> RUBY_BLOCK = BLOCKS.register("ruby_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .instrument(NoteBlockInstrument.BIT)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Block> BOUNTY_BOARD = BLOCKS.register("bounty_board",
            () -> new BountyBoardBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(1.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .ignitedByLava()));

    /** Tough like obsidian so village banks don't get carried off easily. */
    public static final RegistryObject<Block> BANK_COUNTER = BLOCKS.register("bank_counter",
            () -> new BankCounterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .instrument(NoteBlockInstrument.BASS)
                    .requiresCorrectToolForDrops()
                    .strength(50.0F, 1200.0F)
                    .sound(SoundType.WOOD)));

    // ------------------------------------------------------------------ Sundered Sea ores

    private static RegistryObject<Block> ore(String name, MapColor color, float hardness, SoundType sound, int minXp, int maxXp) {
        return BLOCKS.register(name, () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                .mapColor(color)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(hardness, hardness + 3.0F)
                .sound(sound), UniformInt.of(minXp, maxXp)));
    }

    /** Tidesteel: diamond pickaxe or better. */
    public static final RegistryObject<Block> TIDESTEEL_ORE = ore("tidesteel_ore", MapColor.STONE, 4.0F, SoundType.STONE, 3, 7);
    public static final RegistryObject<Block> DEEPSLATE_TIDESTEEL_ORE = ore("deepslate_tidesteel_ore", MapColor.DEEPSLATE, 5.5F, SoundType.DEEPSLATE, 3, 7);
    /** Abyssal: deep down, tidesteel pickaxe or better. */
    public static final RegistryObject<Block> ABYSSAL_ORE = ore("abyssal_ore", MapColor.DEEPSLATE, 7.0F, SoundType.DEEPSLATE, 5, 10);
    /** Stormglass: Storm Isles only, abyssal pickaxe or better. */
    public static final RegistryObject<Block> STORMGLASS_ORE = ore("stormglass_ore", MapColor.STONE, 8.0F, SoundType.AMETHYST, 6, 12);

    /** Inside a lit ruby frame: the way to (and from) the Sundered Sea. */
    public static final RegistryObject<Block> SIREN_PORTAL = BLOCKS.register("siren_portal",
            () -> new com.piratecrew.sundered.SirenPortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .noCollission()
                    .strength(-1.0F)
                    .sound(SoundType.GLASS)
                    .lightLevel(s -> 11)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)
                    .noLootTable()));
}
