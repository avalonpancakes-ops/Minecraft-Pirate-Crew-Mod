package com.piratecrew.entity;

import com.piratecrew.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * A hostile NPC pirate: part of an enemy crew sailing the ocean (on Valkyrien Pirates ships) under
 * an NPC captain. Attacks players and recruited pirates, can't be recruited. Crews on a ship hold
 * their deck: they shoot from where they stand and only fight hand to hand with boarders.
 */
public class RaiderPirateEntity extends PirateEntity implements Enemy {
    private static final ResourceLocation VP_PIRATE = new ResourceLocation("pirates", "pirate");

    private boolean captain;
    /** Crews spawned on a ship stay put instead of walking off the deck. */
    private boolean onShip;
    /** Gear level 0-6: none, leather, gold, chainmail, iron, ruby, diamond. */
    private int gearLevel;

    public RaiderPirateEntity(EntityType<? extends RaiderPirateEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PirateGoals.RangedWeaponGoal(this, () -> onShip));
        this.goalSelector.addGoal(1, new PirateGoals.PirateMeleeGoal(this, 1.2) {
            @Override
            public boolean canUse() {
                return super.canUse() && withinReachIfOnShip();
            }

            @Override
            public boolean canContinueToUse() {
                return super.canContinueToUse() && withinReachIfOnShip();
            }
        });
        this.goalSelector.addGoal(4, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return !onShip && super.canUse();
            }
        });
        // Camp crews mill about their camp.
        this.goalSelector.addGoal(6, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 0.5) {
            @Override
            public boolean canUse() {
                return !onShip && getTarget() == null && super.canUse();
            }
        });
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, RaiderPirateEntity.class).setAlertOthers(RaiderPirateEntity.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                e -> !((Player) e).isCreative() && !e.isSpectator()));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, PirateEntity.class, 10, true, false,
                e -> e instanceof PirateEntity p && p.isRecruited()));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, MarineEntity.class, 10, true, false, e -> true));
    }

    @Override
    protected boolean canBuild() {
        return !onShip && super.canBuild();
    }

    @Override
    protected int homeRadius() {
        return 14;
    }

    private boolean withinReachIfOnShip() {
        LivingEntity t = getTarget();
        return !onShip || (t != null && this.distanceToSqr(t) < 4.0 * 4.0);
    }

    /** Never turn on their own side: other raiders or Valkyrien Pirates' own crew. */
    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof RaiderPirateEntity) return false;
        if (VP_PIRATE.equals(ForgeRegistries.ENTITY_TYPES.getKey(target.getType()))) return false;
        return super.canAttack(target);
    }

    // ------------------------------------------------------------------ setup

    /**
     * Roll this pirate's tier, name and gear. Crew get random gear up to diamond, with pieces missing
     * here and there; the captain wears a full set at the crew's best level (or one better).
     */
    public void setupRaider(PirateTier tier, int gearLevel, boolean captain, boolean onShip) {
        this.captain = captain;
        this.onShip = onShip;
        this.gearLevel = Math.max(0, Math.min(6, gearLevel));
        initPirate(tier);
        var follow = this.getAttribute(Attributes.FOLLOW_RANGE);
        if (follow != null) follow.setBaseValue(40.0);
        this.xpReward = captain ? 40 : 5 + 3 * tier.ordinal();
        for (EquipmentSlot slot : EquipmentSlot.values()) this.setDropChance(slot, 0.0F);
        equip();
        updateDisplayName();
    }

    private static final Item[][] ARMOR = {
            {},
            {Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS},
            {Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS},
            {Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS},
            {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS},
            null, // ruby, filled in at runtime (registry objects)
            {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS}};

    private static Item[] armorFor(int level) {
        if (level == 5) return new Item[]{ModItems.RUBY_HELMET.get(), ModItems.RUBY_CHESTPLATE.get(), ModItems.RUBY_LEGGINGS.get(), ModItems.RUBY_BOOTS.get()};
        return ARMOR[level];
    }

    private static Item swordFor(int level) {
        return switch (level) {
            case 0 -> Items.WOODEN_SWORD;
            case 1, 3 -> Items.STONE_SWORD;
            case 2 -> Items.GOLDEN_SWORD;
            case 4 -> Items.IRON_SWORD;
            case 5 -> ModItems.RUBY_SWORD.get();
            default -> Items.DIAMOND_SWORD;
        };
    }

    /** Sundered Sea crews wear the sea's own gear: tidesteel up to stormforged, captains up to leviathan. */
    private com.piratecrew.item.GearTier seaTier() {
        if (captain) {
            if (gearLevel >= 6) return this.random.nextFloat() < 0.35F ? com.piratecrew.item.GearTier.LEVIATHAN : com.piratecrew.item.GearTier.STORMFORGED;
            return gearLevel >= 4 ? com.piratecrew.item.GearTier.KRAKENBONE : com.piratecrew.item.GearTier.ABYSSAL;
        }
        if (gearLevel >= 6) return com.piratecrew.item.GearTier.STORMFORGED;
        if (gearLevel == 5) return com.piratecrew.item.GearTier.KRAKENBONE;
        return gearLevel >= 3 ? com.piratecrew.item.GearTier.ABYSSAL : com.piratecrew.item.GearTier.TIDESTEEL;
    }

    private boolean atSea() {
        return level().dimension() == com.piratecrew.sundered.SunderedSea.LEVEL;
    }

    private void equip() {
        boolean sea = atSea();
        var seaGear = sea ? ModItems.GEAR.get(seaTier()) : null;
        Item[] armor = sea ? new Item[]{seaGear.helmet().get(), seaGear.chestplate().get(), seaGear.leggings().get(), seaGear.boots().get()}
                : armorFor(gearLevel);
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < armor.length; i++) {
            if (captain || this.random.nextFloat() < 0.7F) setItemSlot(slots[i], new ItemStack(armor[i]));
        }

        ItemStack sword = new ItemStack(sea ? seaGear.sword().get() : swordFor(gearLevel));
        ItemStack ranged = new ItemStack(this.random.nextInt(3) == 0 ? Items.CROSSBOW : Items.BOW);
        if (captain && (gearLevel >= 5 || sea)) sword.enchant(Enchantments.SHARPNESS, 1 + this.random.nextInt(3));
        if (captain && (gearLevel >= 5 || sea)) ranged.enchant(ranged.is(Items.BOW) ? Enchantments.POWER_ARROWS : Enchantments.QUICK_CHARGE, 1 + this.random.nextInt(2));

        // Ship crews mostly shoot; up close they draw their blade. Brawlers and the captain lead with the blade.
        boolean bladeFirst = captain || getCombatStyle() == CombatStyle.BRAWLER || (!onShip && this.random.nextBoolean());
        boolean hasRanged = captain || onShip || this.random.nextFloat() < 0.6F;
        if (hasRanged) {
            setItemSlot(EquipmentSlot.MAINHAND, bladeFirst ? sword : ranged);
            getPack().setItem(0, bladeFirst ? ranged : sword);
        } else {
            setItemSlot(EquipmentSlot.MAINHAND, sword);
        }
        if (captain && (gearLevel >= 4 || sea)) setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        // A Sundered Sea captain sometimes sails with a Soul Pact (its scroll drops when it dies).
        if (sea && captain && this.random.nextFloat() < 0.2F) {
            var pacts = com.piratecrew.pact.SoulPact.values();
            bindPact(pacts[this.random.nextInt(pacts.length)]);
        }
        if (captain) getPack().setItem(1, new ItemStack(Items.GOLDEN_APPLE, 1 + this.random.nextInt(2)));
        // Blocks for bridging, towering and cover.
        if (captain) getPack().setItem(2, new ItemStack(Items.COBBLESTONE, 32));
        else if (this.random.nextFloat() < 0.6F) getPack().setItem(2, new ItemStack(this.random.nextBoolean() ? Items.OAK_PLANKS : Items.COBBLESTONE, 8 + this.random.nextInt(17)));
    }

    public boolean isCaptain() {
        return captain;
    }

    @Override
    protected String rollName() {
        String n = super.rollName();
        return captain ? "Captain " + n : n;
    }

    @Override
    public void updateDisplayName() {
        PirateTier tier = getTier();
        MutableComponent name = Component.literal("[" + tier.label + "] ").withStyle(tier.color, ChatFormatting.BOLD);
        name.append(Component.literal("☠ " + pirateName).withStyle(s -> s.withBold(false)
                .withColor(captain ? ChatFormatting.GOLD : ChatFormatting.RED)));
        this.setCustomName(name);
        this.setCustomNameVisible(true);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (this.level().isClientSide) return InteractionResult.SUCCESS;
        player.sendSystemMessage(Component.literal(pirateName + ": ").withStyle(ChatFormatting.DARK_RED)
                .append(Component.literal(captain ? "Ye'll walk the plank before ye give orders on MY ship!"
                        : "Sail with ye? I'd sooner feed ye to the sharks!").withStyle(ChatFormatting.RED)));
        return InteractionResult.CONSUME;
    }

    /** Some rubies (more from the captain) and the occasional piece of gear, like a vanilla mob. */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean hitByPlayer) {
        int rubies = captain ? 8 + this.random.nextInt(13) : 1 + this.random.nextInt(4);
        rubies += this.random.nextInt(looting + 1);
        this.spawnAtLocation(new ItemStack(ModItems.RUBY.get(), rubies));
        if (!hitByPlayer) return;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack s = this.getItemBySlot(slot);
            if (!s.isEmpty() && this.random.nextFloat() < (captain ? 0.25F : 0.085F) + looting * 0.01F) {
                ItemStack drop = s.copy();
                if (drop.isDamageableItem()) drop.setDamageValue(this.random.nextInt(Math.max(1, drop.getMaxDamage() / 2)));
                this.spawnAtLocation(drop);
            }
        }
    }

    // ------------------------------------------------------------------ save / load

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Captain", captain);
        tag.putBoolean("OnShip", onShip);
        tag.putInt("GearLevel", gearLevel);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        this.captain = tag.getBoolean("Captain");
        this.onShip = tag.getBoolean("OnShip");
        this.gearLevel = tag.getInt("GearLevel");
        super.readAdditionalSaveData(tag);
    }
}
