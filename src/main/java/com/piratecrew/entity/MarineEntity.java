package com.piratecrew.entity;

import com.piratecrew.bounty.BountyManager;
import com.piratecrew.crew.CrewManager;
import com.piratecrew.item.GearTier;
import com.piratecrew.registry.ModItems;
import com.piratecrew.skin.PirateSkins;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * The Order of the Tide: the Sundered Sea's marines. They hunt pirates: any player in a crew or with
 * a bounty, and every pirate NPC (crew pirates, raiders, bar pirates). Even a recruit outclasses any
 * vanilla mob. They fight with the same weapons, shields, potions and building as pirates.
 */
public class MarineEntity extends PirateEntity implements Enemy {
    private static final ResourceLocation VP_PIRATE = new ResourceLocation("pirates", "pirate");

    public enum Rank {
        //          label        hp   dmg  armor tough  arrowTier       colour
        RECRUIT("Recruit",       40, 7.0, 8, 0, PirateTier.C, ChatFormatting.AQUA),
        RIFLEMAN("Rifleman",     40, 6.0, 8, 0, PirateTier.B, ChatFormatting.GREEN),
        SERGEANT("Sergeant",     60, 10.0, 14, 2, PirateTier.A, ChatFormatting.YELLOW),
        CAPTAIN("Captain",      120, 14.0, 20, 4, PirateTier.S, ChatFormatting.GOLD);

        public final String label;
        public final double health, damage, armor, toughness;
        public final PirateTier tier;
        public final ChatFormatting color;

        Rank(String label, double health, double damage, double armor, double toughness, PirateTier tier, ChatFormatting color) {
            this.label = label;
            this.health = health;
            this.damage = damage;
            this.armor = armor;
            this.toughness = toughness;
            this.tier = tier;
            this.color = color;
        }

        public String skinPrefix() {
            return "marine_" + name().toLowerCase() + "_";
        }

        public static Rank byId(int id) {
            Rank[] v = values();
            return id >= 0 && id < v.length ? v[id] : RECRUIT;
        }

        public static Rank random(RandomSource r) {
            int roll = r.nextInt(100);
            return roll < 45 ? RECRUIT : roll < 75 ? RIFLEMAN : roll < 94 ? SERGEANT : CAPTAIN;
        }
    }

    private static final String[] SURNAMES = {"Haskell", "Corvane", "Merrow", "Thale", "Brask", "Ostrand", "Velde", "Harlan", "Quill",
            "Strand", "Dorran", "Pike", "Morland", "Ashby", "Kestle", "Vance", "Rourke", "Talbot", "Wren", "Holt", "Garrick", "Seldon",
            "Marsh", "Teague", "Calder", "Fenn", "Rook", "Sterling", "Graye", "Lowell", "Barrow", "Hale", "Crane", "Daley", "Emmerich"};

    @Nullable private Rank rank;
    /** Patrol squads wander off (despawn) when nobody's near; outpost garrisons stay. */
    private boolean patrol;

    public MarineEntity(EntityType<? extends MarineEntity> type, Level level) {
        super(type, level);
    }

    public void setPatrol(boolean patrol) {
        this.patrol = patrol;
    }

    @Override
    public boolean isPersistenceRequired() {
        return !patrol && super.isPersistenceRequired();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return patrol && getTarget() == null;
    }

    public Rank getRank() {
        return rank == null ? Rank.RECRUIT : rank;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PirateGoals.RangedWeaponGoal(this));
        this.goalSelector.addGoal(1, new PirateGoals.PirateMeleeGoal(this, 1.2));
        this.goalSelector.addGoal(4, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.8));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.55) {
            @Override
            public boolean canUse() {
                return getTarget() == null && super.canUse();
            }
        });
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        addCombatFallbacks();

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, MarineEntity.class).setAlertOthers(MarineEntity.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, e -> isPirate((Player) e)));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, PirateEntity.class, 10, true, false,
                e -> !(e instanceof MarineEntity) && !(e instanceof BountyHunterEntity)));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
                e -> VP_PIRATE.equals(ForgeRegistries.ENTITY_TYPES.getKey(e.getType()))));
    }

    /** A player the Order hunts: in a crew, or carrying a bounty. */
    public static boolean isPirate(Player p) {
        if (p.isCreative() || p.isSpectator() || p.level().isClientSide || p.getServer() == null) return false;
        return CrewManager.crewIdOf(p) != null || BountyManager.bountyOf(p.getServer(), p.getUUID()) > 0;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof MarineEntity) return false;
        return super.canAttack(target);
    }

    @Override
    protected int homeRadius() {
        return 16;
    }

    // ------------------------------------------------------------------ setup

    public void setupMarine(Rank r) {
        this.rank = r;
        initPirate(r.tier);
        var follow = this.getAttribute(Attributes.FOLLOW_RANGE);
        if (follow != null) follow.setBaseValue(40.0);
        var armor = this.getAttribute(Attributes.ARMOR);
        if (armor != null) armor.setBaseValue(r.armor);
        var tough = this.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (tough != null) tough.setBaseValue(r.toughness);
        var kb = this.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb != null) kb.setBaseValue(r == Rank.CAPTAIN ? 0.4 : 0.1);
        this.xpReward = switch (r) {
            case RECRUIT, RIFLEMAN -> 12;
            case SERGEANT -> 20;
            case CAPTAIN -> 45;
        };
        equip(r);
        updateDisplayName();
    }

    private void equip(Rank r) {
        RandomSource rnd = this.random;
        ItemStack tidesteelSword = new ItemStack(ModItems.GEAR.get(GearTier.TIDESTEEL).sword().get());
        switch (r) {
            case RECRUIT -> {
                setItemSlot(EquipmentSlot.MAINHAND, rnd.nextFloat() < 0.3F ? tidesteelSword : new ItemStack(Items.IRON_SWORD));
                if (rnd.nextFloat() < 0.3F) setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            }
            case RIFLEMAN -> {
                setCombatStyle(CombatStyle.MARKSMAN);
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
                getPack().setItem(0, new ItemStack(Items.IRON_SWORD));
            }
            case SERGEANT -> {
                setItemSlot(EquipmentSlot.MAINHAND, tidesteelSword);
                setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                if (rnd.nextFloat() < 0.4F) getPack().setItem(0, new ItemStack(Items.CROSSBOW));
                getPack().setItem(2, new ItemStack(Items.STONE_BRICKS, 16));
            }
            case CAPTAIN -> {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.GEAR.get(GearTier.ABYSSAL).sword().get()));
                setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                getPack().setItem(0, new ItemStack(Items.CROSSBOW));
                getPack().setItem(1, PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.STRONG_HEALING));
                getPack().setItem(2, new ItemStack(Items.STONE_BRICKS, 24));
                getPack().setItem(3, new ItemStack(Items.GOLDEN_APPLE, 2));
            }
        }
    }

    @Override
    protected void applyTierStats() {
        Rank r = getRank();
        var hp = this.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(r.health);
        var dmg = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.setBaseValue(r.damage);
    }

    @Override
    protected String rollSkin(PirateTier tier) {
        String s = PirateSkins.randomWithPrefix(this.random, getRank().skinPrefix());
        return s.isEmpty() ? super.rollSkin(tier) : s;
    }

    @Override
    protected String rollName() {
        return SURNAMES[this.random.nextInt(SURNAMES.length)];
    }

    @Override
    public void updateDisplayName() {
        Rank r = getRank();
        MutableComponent name = Component.literal("[" + r.label + "] ").withStyle(r.color, ChatFormatting.BOLD);
        name.append(Component.literal("⚓ " + pirateName).withStyle(s -> s.withBold(false).withColor(ChatFormatting.DARK_AQUA)));
        this.setCustomName(name);
        this.setCustomNameVisible(true);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        if (!isInitialized()) setupMarine(Rank.random(this.random));
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    @Override
    public void aiStep() {
        if (!this.level().isClientSide && !isInitialized()) setupMarine(Rank.random(this.random));
        super.aiStep();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (this.level().isClientSide) return InteractionResult.SUCCESS;
        String line = isPirate(player) ? "Pirate scum! You'll hang for your crimes!" : "Move along, citizen. The Order of the Tide keeps these waters.";
        player.sendSystemMessage(Component.literal(getRank().label + " " + pirateName + ": ").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.literal(line).withStyle(ChatFormatting.AQUA)));
        return InteractionResult.CONSUME;
    }

    /** Badges (for summoning Commodore Graves), rubies, and sometimes tidesteel or a Soul Pact from captains. */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean hitByPlayer) {
        Rank r = getRank();
        int badges = (r == Rank.CAPTAIN ? 3 : r == Rank.SERGEANT ? 2 : 1) + this.random.nextInt(2 + looting);
        this.spawnAtLocation(new ItemStack(ModItems.MARINE_BADGE.get(), badges));
        this.spawnAtLocation(new ItemStack(ModItems.RUBY.get(), 1 + this.random.nextInt(r == Rank.CAPTAIN ? 8 : 3)));
        if (hitByPlayer && (r == Rank.SERGEANT || r == Rank.CAPTAIN) && this.random.nextFloat() < (r == Rank.CAPTAIN ? 0.5F : 0.2F)) {
            this.spawnAtLocation(new ItemStack(ModItems.TIDESTEEL_INGOT.get(), 1 + this.random.nextInt(2)));
        }
        if (hitByPlayer && r == Rank.CAPTAIN && this.random.nextFloat() < 0.04F + looting * 0.01F) {
            ItemStack pact = com.piratecrew.pact.SoulPacts.randomPactItem(this.random);
            if (!pact.isEmpty()) this.spawnAtLocation(pact);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("MarineRank", getRank().ordinal());
        tag.putBoolean("Patrol", patrol);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        this.rank = Rank.byId(tag.getInt("MarineRank"));
        this.patrol = tag.getBoolean("Patrol");
        super.readAdditionalSaveData(tag);
    }
}
