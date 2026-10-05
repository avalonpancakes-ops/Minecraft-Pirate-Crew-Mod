package com.piratecrew.entity;

import com.piratecrew.crew.Crew;
import com.piratecrew.crew.CrewData;
import com.piratecrew.crew.CrewManager;
import com.piratecrew.menu.PirateMenu;
import com.piratecrew.registry.ModItems;
import com.piratecrew.skin.PirateSkins;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ToolActions;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.alchemy.PotionUtils;
import java.util.UUID;

public class PirateEntity extends PathfinderMob {
    public enum Orders { FOLLOW, HOLD, WANDER, WORK }

    private static final EntityDataAccessor<Integer> DATA_TIER = SynchedEntityData.defineId(PirateEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_SKIN = SynchedEntityData.defineId(PirateEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Optional<UUID>> DATA_CREW = SynchedEntityData.defineId(PirateEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_ORDERS = SynchedEntityData.defineId(PirateEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STYLE = SynchedEntityData.defineId(PirateEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TASK = SynchedEntityData.defineId(PirateEntity.class, EntityDataSerializers.INT);

    /** Half of a player's 36-slot inventory. */
    public static final int PACK_SIZE = 18;
    public static final int WORK_RADIUS = 12;

    private static final int HOME_RADIUS = 6;

    private boolean initialized = false;
    protected String pirateName = "Pirate";
    @Nullable private UUID leaderId;
    @Nullable private BlockPos holdPos;
    @Nullable private BlockPos home;
    protected int lastCombatTick = -1000;
    private final SimpleContainer pack = new SimpleContainer(PACK_SIZE);
    /** This pirate's own distance for switching to melee (varies inside its combat style's range). */
    private float meleeRange = 5.0F;
    @Nullable private BlockPos workCenter;
    private int lastWeaponSwap = -1000;
    private int lastSwing = -1000;
    /** Ticks the shield stays down after an axe knocks it aside. */
    private int shieldCooldown = 0;
    private int lastSpeech = -100000;
    private String lastSpeechText = "";

    public PirateEntity(EntityType<? extends PirateEntity> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(false);
        this.setPersistenceRequired();
        if (this.getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) this.setDropChance(slot, 0.0F); // we drop gear ourselves
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_TIER, 0);
        this.entityData.define(DATA_SKIN, "");
        this.entityData.define(DATA_CREW, Optional.empty());
        this.entityData.define(DATA_ORDERS, Orders.WANDER.ordinal());
        this.entityData.define(DATA_STYLE, CombatStyle.BALANCED.ordinal());
        this.entityData.define(DATA_TASK, PirateTask.NONE.ordinal());
    }

    // ------------------------------------------------------------------ AI

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PirateGoals.RangedWeaponGoal(this));
        this.goalSelector.addGoal(1, new PirateGoals.PirateMeleeGoal(this, 1.2));
        this.goalSelector.addGoal(2, new PirateGoals.FollowLeaderGoal(this, 1.15, 5.0F, 2.5F));
        this.goalSelector.addGoal(3, new PirateGoals.HoldPositionGoal(this, 1.0));
        this.goalSelector.addGoal(3, new WorkGoal(this));
        this.goalSelector.addGoal(4, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.8));
        this.goalSelector.addGoal(6, new PirateGoals.IdleStrollGoal(this, 0.6));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new PirateGoals.DefendLeaderGoal(this));
        this.targetSelector.addGoal(3, new PirateGoals.AssistLeaderGoal(this));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
                e -> this.isRecruited() && e instanceof Enemy
                        && !(e instanceof Creeper) && !(e instanceof EnderMan) && !(e instanceof ZombifiedPiglin)));
    }

    /** Never target your own crew. TargetingConditions call this for every target goal. */
    @Override
    public boolean canAttack(LivingEntity target) {
        if (!this.level().isClientSide && CrewManager.areCrewmates(this, target)) return false;
        return super.canAttack(target);
    }

    // ------------------------------------------------------------------ ranged combat

    public enum Ranged { NONE, BOW, CROSSBOW, TRIDENT }

    /** What ranged weapon (if any) the pirate holds in its main hand. Modded bows/crossbows count too. */
    public Ranged getRangedType() {
        Item item = getMainHandItem().getItem();
        if (item instanceof CrossbowItem) return Ranged.CROSSBOW;
        if (item instanceof BowItem) return Ranged.BOW;
        if (item instanceof TridentItem) return Ranged.TRIDENT;
        return Ranged.NONE;
    }

    /**
     * Arrows to shoot: whatever arrows are in the off hand (tipped, spectral...), otherwise an
     * endless supply of plain arrows.
     */
    @Override
    public ItemStack getProjectile(ItemStack weapon) {
        if (weapon.getItem() instanceof ProjectileWeaponItem) {
            ItemStack off = getOffhandItem();
            if (off.getItem() instanceof ArrowItem) return off;
            return new ItemStack(Items.ARROW);
        }
        return ItemStack.EMPTY;
    }

    /** Better tiers hit harder and aim straighter. */
    private float inaccuracy() {
        return Math.max(1.0F, 10.0F - getTier().ordinal() * 1.7F);
    }

    /** Health and damage multiplier on top of the tier's stats (bounty hunters override this). */
    protected double statMultiplier() {
        return 1.0;
    }

    private double tierDamage() {
        return getTier().attackDamage * statMultiplier();
    }

    private void tierArrowBonus(AbstractArrow arrow) {
        arrow.setBaseDamage(arrow.getBaseDamage() + (tierDamage() - 1.0) * 0.25);
    }

    private void aim(Projectile projectile, LivingEntity target, float velocity, float yawOffsetDeg) {
        double dx = target.getX() - getX();
        double dy = target.getY(0.3333333333333333) - projectile.getY();
        double dz = target.getZ() - getZ();
        if (yawOffsetDeg != 0) {
            double r = Math.toRadians(yawOffsetDeg);
            double nx = dx * Math.cos(r) - dz * Math.sin(r);
            double nz = dx * Math.sin(r) + dz * Math.cos(r);
            dx = nx;
            dz = nz;
        }
        double flat = Math.sqrt(dx * dx + dz * dz);
        projectile.shoot(dx, dy + flat * 0.2, dz, velocity, inaccuracy());
    }

    private void useAmmo(ItemStack ammo, ItemStack weapon) {
        if (ammo == getOffhandItem() && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, weapon) == 0) {
            ammo.shrink(1);
        }
    }

    private void wearWeapon() {
        getMainHandItem().hurtAndBreak(1, this, e -> e.broadcastBreakEvent(InteractionHand.MAIN_HAND));
    }

    public void shootBow(LivingEntity target, float power) {
        ItemStack weapon = getMainHandItem();
        ItemStack ammo = getProjectile(weapon);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, ammo, power);
        if (weapon.getItem() instanceof BowItem bow) arrow = bow.customArrow(arrow);
        tierArrowBonus(arrow);
        aim(arrow, target, 1.6F + power * 0.4F, 0);
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(arrow);
        useAmmo(ammo, weapon);
        wearWeapon();
        lastCombatTick = this.tickCount;
    }

    public void shootCrossbow(LivingEntity target) {
        ItemStack weapon = getMainHandItem();
        ItemStack ammo = getProjectile(weapon);
        int pierce = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PIERCING, weapon);
        boolean multishot = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MULTISHOT, weapon) > 0;
        float[] angles = multishot ? new float[]{0F, -10F, 10F} : new float[]{0F};
        for (float angle : angles) {
            AbstractArrow arrow = ProjectileUtil.getMobArrow(this, ammo, 1.0F);
            arrow.setShotFromCrossbow(true);
            arrow.setSoundEvent(SoundEvents.CROSSBOW_HIT);
            if (pierce > 0) arrow.setPierceLevel((byte) pierce);
            if (angle != 0) arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            tierArrowBonus(arrow);
            arrow.setBaseDamage(arrow.getBaseDamage() + 1.0); // crossbows hit a bit harder than bows
            aim(arrow, target, 2.6F, angle);
            this.level().addFreshEntity(arrow);
        }
        this.playSound(SoundEvents.CROSSBOW_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
        useAmmo(ammo, weapon);
        wearWeapon();
        CrossbowItem.setCharged(weapon, false);
        // Forget any projectiles a player loaded before handing it over, so they aren't fired twice later.
        if (weapon.getTag() != null) weapon.getTag().remove("ChargedProjectiles");
        lastCombatTick = this.tickCount;
    }

    public void throwTrident(LivingEntity target) {
        ItemStack weapon = getMainHandItem();
        ThrownTrident trident = new ThrownTrident(this.level(), this, weapon.copy());
        trident.pickup = AbstractArrow.Pickup.DISALLOWED;
        trident.setBaseDamage(trident.getBaseDamage() + (tierDamage() - 1.0) * 0.5);
        aim(trident, target, 1.8F, 0);
        this.playSound(SoundEvents.TRIDENT_THROW, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(trident);
        wearWeapon();
        lastCombatTick = this.tickCount;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (consuming != null) return false; // busy drinking
        lastCombatTick = this.tickCount;
        lastSwing = this.tickCount;
        // Like a player: the shield comes down to swing.
        if (raisingShield()) stopUsingItem();
        return super.doHurtTarget(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt) lastCombatTick = this.tickCount;
        if (hurt && source.getDirectEntity() instanceof Projectile) lastShotAt = this.tickCount;
        return hurt;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // ------------------------------------------------------------------ spawning

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        if (!initialized) initPirate(PirateTier.random(this.random));
        return result;
    }

    /** Roll a name and skin and apply tier stats. Safe to call once per pirate. */
    public void initPirate(PirateTier tier) {
        this.initialized = true;
        this.entityData.set(DATA_TIER, tier.ordinal());
        this.pirateName = rollName();
        rollCombatStyle();
        applyTierStats();
        this.setHealth(this.getMaxHealth());
        this.entityData.set(DATA_SKIN, rollSkin(tier));
        updateDisplayName();
    }

    protected String rollName() {
        return PirateNames.random(this.random);
    }

    protected String rollSkin(PirateTier tier) {
        return PirateSkins.random(this.random, tier);
    }

    public boolean isInitialized() {
        return initialized;
    }

    /** Force a fighting style (bounty hunters with a fixed style). */
    protected void setCombatStyle(CombatStyle style) {
        this.entityData.set(DATA_STYLE, style.ordinal());
        this.meleeRange = style.meleeMin + this.random.nextFloat() * (style.meleeMax - style.meleeMin);
    }

    private void rollCombatStyle() {
        CombatStyle style = CombatStyle.random(this.random);
        this.entityData.set(DATA_STYLE, style.ordinal());
        this.meleeRange = style.meleeMin + this.random.nextFloat() * (style.meleeMax - style.meleeMin);
    }

    public CombatStyle getCombatStyle() {
        return CombatStyle.byId(this.entityData.get(DATA_STYLE));
    }

    public float getMeleeRange() {
        return meleeRange;
    }

    public PirateTask getTask() {
        return PirateTask.byId(this.entityData.get(DATA_TASK));
    }

    @Nullable
    public BlockPos getWorkCenter() {
        return workCenter;
    }

    public SimpleContainer getPack() {
        return pack;
    }

    protected void applyTierStats() {
        PirateTier tier = getTier();
        var hp = this.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(tier.maxHealth * statMultiplier());
        var dmg = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.setBaseValue(tier.attackDamage * statMultiplier());
    }

    public void setHome(BlockPos pos) {
        this.home = pos;
        applyRestriction();
    }

    /** Free pirates stay near their bar; crew pirates told to roam stay near where they were told. */
    private void applyRestriction() {
        if (isRecruited() && getOrders() == Orders.WORK && workCenter != null) {
            this.restrictTo(workCenter, WORK_RADIUS + 10);
        } else if (home != null && (!isRecruited() || getOrders() == Orders.WANDER)) {
            this.restrictTo(home, isRecruited() ? 12 : homeRadius());
        } else {
            this.clearRestriction();
        }
    }

    /** How far a free pirate strays from its home (bar pirates stay close; raider camps roam wider). */
    protected int homeRadius() {
        return HOME_RADIUS;
    }

    @Nullable
    public BlockPos getHome() {
        return home;
    }

    // ------------------------------------------------------------------ getters

    public PirateTier getTier() {
        return PirateTier.byId(this.entityData.get(DATA_TIER));
    }

    public String getSkinName() {
        return this.entityData.get(DATA_SKIN);
    }

    public String getPirateName() {
        return pirateName;
    }

    @Nullable
    public UUID getCrewId() {
        return this.entityData.get(DATA_CREW).orElse(null);
    }

    public boolean isRecruited() {
        return this.entityData.get(DATA_CREW).isPresent();
    }

    public Orders getOrders() {
        int o = this.entityData.get(DATA_ORDERS);
        return o >= 0 && o < Orders.values().length ? Orders.values()[o] : Orders.WANDER;
    }

    @Nullable
    public BlockPos getHoldPos() {
        return holdPos;
    }

    /** The crewmate this pirate follows/defends. Falls back to the captain if the leader left the crew. */
    @Nullable
    public Player getLeader() {
        if (this.level().isClientSide || !isRecruited()) return null;
        if (leaderId != null) {
            Player p = this.level().getPlayerByUUID(leaderId);
            if (p != null && CrewManager.areCrewmates(this, p)) return p;
        }
        Crew crew = CrewData.get(((ServerLevel) this.level()).getServer()).byId(getCrewId());
        if (crew != null && crew.captain != null) {
            Player captain = this.level().getPlayerByUUID(crew.captain);
            if (captain != null) {
                leaderId = captain.getUUID();
                return captain;
            }
        }
        return null;
    }

    /** How big this pirate is drawn (bosses tower over ordinary crews). */
    public float renderScale() {
        return 1.0F;
    }

    // ------------------------------------------------------------------ tier from bounty

    /**
     * Crew pirates climb tiers as their bounty grows: D at 100 rubies, C 250, B 500, A 1,000, S 2,500,
     * SS 5,000 and SSS 10,000. Never down: a pirate recruited at a high tier keeps it. Name and skin stay.
     */
    public void checkBountyPromotion() {
        if (!isRecruited() || !(this.level() instanceof ServerLevel sl)) return;
        int bounty = com.piratecrew.bounty.BountyManager.bountyOf(sl.getServer(), getUUID());
        PirateTier earned = PirateTier.promotionFor(bounty);
        PirateTier now = getTier();
        if (earned.ordinal() <= now.ordinal()) return;
        float missing = getMaxHealth() - getHealth();
        this.entityData.set(DATA_TIER, earned.ordinal());
        applyTierStats();
        setHealth(Math.max(1.0F, getMaxHealth() - missing));
        updateDisplayName();
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + 1, getZ(), 40, 0.4, 0.8, 0.4, 0.3);
        sl.playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 0.8F);
        CrewManager.onPirateTierUp(sl.getServer(), getCrewId(), this, now, earned, bounty);
    }

    // ------------------------------------------------------------------ soul pacts

    @Nullable
    private com.piratecrew.pact.SoulPact pact;
    /** Ticks until this pirate can use its pact's power again. */
    public int pactCooldown;

    @Nullable
    public com.piratecrew.pact.SoulPact getPact() {
        return pact;
    }

    /** Bind a Soul Pact to this pirate: its whole fighting style changes to suit the pact. */
    public void bindPact(com.piratecrew.pact.SoulPact pact) {
        this.pact = pact;
        this.pactCooldown = 40;
        setCombatStyle(pact.ranged ? CombatStyle.MARKSMAN : CombatStyle.BRAWLER);
        updateDisplayName();
    }

    /** A pact mark after the name, in the pact's colour. */
    protected void appendPactTag(MutableComponent name) {
        if (pact != null) name.append(Component.literal(" \u2726" + pact.label).withStyle(s -> s.withBold(false).withColor(pact.color)));
    }

    public void updateDisplayName() {
        PirateTier tier = getTier();
        MutableComponent name = Component.literal("[" + tier.label + "] ").withStyle(tier.color, ChatFormatting.BOLD);
        if (isViceCaptain()) name.append(Component.literal("\u2606 ").withStyle(s -> s.withBold(false).withColor(ChatFormatting.GOLD)));
        name.append(Component.literal(pirateName).withStyle(s -> s.withBold(false)
                        .withColor(isRecruited() ? ChatFormatting.WHITE : ChatFormatting.GRAY)));
        appendPactTag(name);
        this.setCustomName(name);
        this.setCustomNameVisible(true);
    }

    /** Server side: is this pirate one of its crew's vice captains? */
    public boolean isViceCaptain() {
        UUID crew = getCrewId();
        if (crew == null || this.level().isClientSide || !(this.level() instanceof ServerLevel sl)) return false;
        try {
            return CrewManager.isViceCaptain(sl.getServer(), crew, getUUID());
        } catch (Exception e) {
            return false;
        }
    }

    // ------------------------------------------------------------------ crew

    public void joinCrew(Crew crew, Player leader) {
        this.entityData.set(DATA_CREW, Optional.of(crew.id));
        this.leaderId = leader.getUUID();
        this.entityData.set(DATA_ORDERS, Orders.FOLLOW.ordinal());
        applyRestriction();
        this.setTarget(null);
        this.setLastHurtByMob(null);
        updateDisplayName();
    }

    /** Back to being a free pirate (kicked, crew disbanded...). Keeps the gear it was given. */
    public void leaveCrew() {
        this.entityData.set(DATA_CREW, Optional.empty());
        this.leaderId = null;
        this.holdPos = null;
        this.workCenter = null;
        this.entityData.set(DATA_TASK, PirateTask.NONE.ordinal());
        this.entityData.set(DATA_ORDERS, Orders.WANDER.ordinal());
        this.setTarget(null);
        this.setHome(this.blockPosition());
        updateDisplayName();
    }

    public void setOrders(Orders orders, Player by) {
        this.entityData.set(DATA_ORDERS, orders.ordinal());
        this.entityData.set(DATA_TASK, PirateTask.NONE.ordinal());
        this.leaderId = by.getUUID();
        this.getNavigation().stop();
        applyRestriction();
        switch (orders) {
            case FOLLOW -> by.displayClientMessage(Component.literal(pirateName + ": Aye, I'll follow ye!").withStyle(ChatFormatting.YELLOW), true);
            case HOLD -> {
                this.holdPos = this.blockPosition();
                by.displayClientMessage(Component.literal(pirateName + ": Holdin' this spot, Cap'n.").withStyle(ChatFormatting.YELLOW), true);
            }
            case WANDER -> {
                this.setHome(this.blockPosition());
                by.displayClientMessage(Component.literal(pirateName + ": I'll stretch me legs around here.").withStyle(ChatFormatting.YELLOW), true);
            }
            default -> {}
        }
    }

    /** Send the pirate to work around where it stands now. NONE stops work and has it follow. */
    public void startTask(PirateTask task, Player by) {
        if (task == PirateTask.NONE) {
            setOrders(Orders.FOLLOW, by);
            return;
        }
        this.leaderId = by.getUUID();
        this.workCenter = this.blockPosition();
        this.entityData.set(DATA_ORDERS, Orders.WORK.ordinal());
        this.entityData.set(DATA_TASK, task.ordinal());
        this.getNavigation().stop();
        applyRestriction();
        String line = switch (task) {
            case MINE -> "Aye, I'll dig out what ore I can find.";
            case FARM -> "I'll tend the crops, Cap'n.";
            case FISH -> "Off to catch us some supper!";
            case WOOD -> "Timber it is!";
            default -> "Aye.";
        };
        by.displayClientMessage(Component.literal(pirateName + ": " + line).withStyle(ChatFormatting.YELLOW), true);
    }

    /** Tell the crew leader something (rate-limited so the pirate doesn't nag). */
    public void say(String text) {
        if (this.tickCount - lastSpeech < 600 && text.equals(lastSpeechText)) return;
        if (this.tickCount - lastSpeech < 100) return;
        Player leader = getLeader();
        if (leader == null || leader.distanceToSqr(this) > 64 * 64) return;
        lastSpeech = this.tickCount;
        lastSpeechText = text;
        leader.sendSystemMessage(Component.literal(pirateName + ": ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(text).withStyle(ChatFormatting.YELLOW)));
    }

    // ------------------------------------------------------------------ pack & gear

    public static boolean isRangedWeapon(ItemStack s) {
        Item i = s.getItem();
        return i instanceof BowItem || i instanceof CrossbowItem || i instanceof TridentItem;
    }

    /** Extra melee damage an item gives in the main hand (0 for most items). */
    public static double meleeDamage(ItemStack s) {
        if (s.isEmpty()) return 0;
        double dmg = 0;
        for (AttributeModifier mod : s.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) {
            if (mod.getOperation() == AttributeModifier.Operation.ADDITION) dmg += mod.getAmount();
        }
        return dmg;
    }

    /** Swap the main-hand item with a pack slot. */
    public void swapWithPack(int slot) {
        ItemStack main = getMainHandItem().copy();
        setItemSlot(EquipmentSlot.MAINHAND, pack.getItem(slot).copy());
        pack.setItem(slot, main);
        if (isUsingItem()) stopUsingItem();
        lastWeaponSwap = this.tickCount;
    }

    /** Make sure the main hand holds an item matching {@code want}, taking one from the pack if needed. */
    public boolean equipFromPack(Predicate<ItemStack> want) {
        if (want.test(getMainHandItem())) return true;
        for (int i = 0; i < pack.getContainerSize(); i++) {
            if (want.test(pack.getItem(i))) {
                swapWithPack(i);
                return true;
            }
        }
        return false;
    }

    public boolean hasInHandOrPack(Predicate<ItemStack> want) {
        if (want.test(getMainHandItem())) return true;
        for (int i = 0; i < pack.getContainerSize(); i++) if (want.test(pack.getItem(i))) return true;
        return false;
    }

    public boolean packFull() {
        for (int i = 0; i < pack.getContainerSize(); i++) if (pack.getItem(i).isEmpty()) return false;
        return true;
    }

    /** Put an item in the pack; whatever doesn't fit is dropped at the pirate's feet. */
    public void addToPack(ItemStack stack) {
        ItemStack left = pack.addItem(stack);
        if (!left.isEmpty()) this.spawnAtLocation(left);
    }

    /**
     * In a fight, pick melee or ranged from what's in hand and in the pack, based on distance and
     * this pirate's combat style.
     */
    private void chooseWeapon() {
        LivingEntity target = getTarget();
        if (target == null || this.tickCount - lastWeaponSwap < 30 || isUsingItem()) return;
        ItemStack main = getMainHandItem();
        boolean holdingRanged = isRangedWeapon(main);
        int rangedSlot = -1, meleeSlot = -1;
        double bestMelee = holdingRanged ? 0 : meleeDamage(main);
        for (int i = 0; i < pack.getContainerSize(); i++) {
            ItemStack s = pack.getItem(i);
            if (s.isEmpty()) continue;
            if (isRangedWeapon(s)) {
                if (rangedSlot < 0) rangedSlot = i;
            } else {
                double dmg = meleeDamage(s);
                if (dmg > bestMelee) {
                    bestMelee = dmg;
                    meleeSlot = i;
                }
            }
        }
        boolean hasRanged = holdingRanged || rangedSlot >= 0;
        double dist = this.distanceTo(target);
        // A little stickiness so it doesn't flip back and forth at the boundary.
        float threshold = holdingRanged ? meleeRange : meleeRange + 1.5F;
        // Rushing an enemy who's walling up: blade out.
        boolean wantMelee = !hasRanged || dist < threshold || rushTicks > 0;
        if (wantMelee) {
            if (meleeSlot >= 0) swapWithPack(meleeSlot);
        } else if (!holdingRanged && rangedSlot >= 0) {
            swapWithPack(rangedSlot);
        }
    }

    // ------------------------------------------------------------------ interaction

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (this.level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        PirateTier tier = getTier();

        if (isRecruited()) {
            if (CrewManager.isInSameCrew(sp, this)) {
                openEquipment(sp);
            } else {
                Crew crew = CrewData.get(sp.server).byId(getCrewId());
                sp.sendSystemMessage(Component.literal(pirateName + " sails with " + (crew != null ? crew.name : "another crew") + ".")
                        .withStyle(ChatFormatting.GRAY));
            }
            return InteractionResult.CONSUME;
        }

        if (this.getTarget() == player) {
            sp.sendSystemMessage(Component.literal(pirateName + ": Ye think I'd sail with someone who struck me? Never!").withStyle(ChatFormatting.RED));
            return InteractionResult.CONSUME;
        }

        int cost = tier.cost();
        boolean holdingRuby = player.getItemInHand(hand).is(ModItems.RUBY.get());
        if (!holdingRuby) {
            sp.sendSystemMessage(Component.literal("[" + tier.label + " tier] ").withStyle(tier.color, ChatFormatting.BOLD)
                    .append(Component.literal(pirateName).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(String.format("  ❤ %.0f HP  ⚔ %.1f damage", tier.maxHealth, tier.attackDamage)).withStyle(ChatFormatting.GRAY)));
            sp.sendSystemMessage(Component.literal("Hold rubies and right-click to recruit for " + cost + " rubies.").withStyle(ChatFormatting.YELLOW));
            return InteractionResult.CONSUME;
        }

        // Check the crew before taking any rubies.
        Crew existing = CrewManager.crewOf(sp);
        if (existing != null) {
            if (!existing.isOfficer(sp.getUUID())) {
                sp.sendSystemMessage(Component.literal("Only your captain or vice captains can recruit pirates.").withStyle(ChatFormatting.RED));
                return InteractionResult.CONSUME;
            }
            if (existing.isFull()) {
                sp.sendSystemMessage(Component.literal(existing.name + " is full (" + existing.size() + " members).").withStyle(ChatFormatting.RED));
                return InteractionResult.CONSUME;
            }
        }

        boolean free = sp.getAbilities().instabuild;
        int have = countRubies(sp.getInventory());
        if (!free && have < cost) {
            sp.sendSystemMessage(Component.literal(pirateName + " wants " + cost + " rubies. You have " + have + ".").withStyle(ChatFormatting.RED));
            return InteractionResult.CONSUME;
        }

        Crew crew = existing != null ? existing : CrewManager.crewForRecruiting(sp);
        if (crew == null) return InteractionResult.CONSUME;

        if (!free) takeRubies(sp.getInventory(), cost);
        joinCrew(crew, sp);
        CrewManager.addPirate(sp.server, crew, this);
        com.piratecrew.goals.Goals.grant(sp, com.piratecrew.goals.Goal.RECRUIT);

        ServerLevel sl = (ServerLevel) this.level();
        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.0, getZ(), 12, 0.4, 0.6, 0.4, 0.0);
        sl.playSound(null, blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 0.9F);
        sp.sendSystemMessage(Component.literal("⚓ ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("[" + tier.label + "] ").withStyle(tier.color, ChatFormatting.BOLD))
                .append(Component.literal(pirateName + " joined " + crew.name + "! Right-click them to give gear and orders.").withStyle(ChatFormatting.YELLOW)));
        return InteractionResult.CONSUME;
    }

    public void openEquipment(ServerPlayer player) {
        if (consuming != null) stopUsingItem();
        NetworkHooks.openScreen(player,
                new SimpleMenuProvider((id, inv, p) -> new PirateMenu(id, inv, this), this.getDisplayName()),
                buf -> buf.writeVarInt(this.getId()));
    }

    private static int countRubies(Inventory inv) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ModItems.RUBY.get())) n += s.getCount();
        }
        return n;
    }

    private static void takeRubies(Inventory inv, int amount) {
        for (int i = 0; i < inv.getContainerSize() && amount > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ModItems.RUBY.get())) {
                int take = Math.min(amount, s.getCount());
                s.shrink(take);
                amount -= take;
            }
        }
        inv.setChanged();
    }

    // ------------------------------------------------------------------ building in a fight

    private int lastShotAt = -1000;
    @Nullable private BlockPos pillarBase;
    private int pillarStart;
    private int coverCooldown;
    private int rushTicks;
    private int lastReachCheck = -1000;
    private boolean lastReachable = true;
    private final java.util.ArrayDeque<BlockPos> buildQueue = new java.util.ArrayDeque<>();

    /** Can this pirate build right now? (Ship crews don't; hostile NPCs respect the mobGriefing rule.) */
    protected boolean canBuild() {
        if (isRecruited()) return true;
        return this.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
    }

    public boolean isRushing() {
        return rushTicks > 0;
    }

    /** Solid, full, non-falling blocks without a block entity, and nothing valuable (no ore or ruby blocks). */
    private static boolean isBuildBlock(ItemStack s) {
        if (!(s.getItem() instanceof net.minecraft.world.item.BlockItem bi)) return false;
        net.minecraft.world.level.block.Block b = bi.getBlock();
        if (b instanceof net.minecraft.world.level.block.FallingBlock || b instanceof net.minecraft.world.level.block.EntityBlock) return false;
        if (com.piratecrew.bank.RubyValues.unitValue(s) > 0) return false;
        return b.defaultBlockState().isCollisionShapeFullBlock(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    private int findBuildBlock() {
        for (int i = 0; i < pack.getContainerSize(); i++) if (isBuildBlock(pack.getItem(i))) return i;
        return -1;
    }

    /** Place one of its blocks at {@code pos} if the spot is free. */
    private boolean placeBuildBlock(BlockPos pos) {
        if (!canBuild()) return false;
        if (!this.level().getBlockState(pos).canBeReplaced()) return false;
        int slot = findBuildBlock();
        if (slot < 0) return false;
        ItemStack stack = pack.getItem(slot);
        BlockState state = ((net.minecraft.world.item.BlockItem) stack.getItem()).getBlock().defaultBlockState();
        if (!this.level().isUnobstructed(state, pos, net.minecraft.world.phys.shapes.CollisionContext.empty())) return false;
        this.level().setBlock(pos, state, 3);
        var sound = state.getSoundType();
        this.level().playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        this.swing(InteractionHand.MAIN_HAND);
        if (!infiniteConsumables()) {
            stack.shrink(1);
            pack.setChanged();
        }
        BuildTracker.record(this);
        return true;
    }

    private boolean canReach(LivingEntity t) {
        if (this.tickCount - lastReachCheck < 20) return lastReachable;
        lastReachCheck = this.tickCount;
        net.minecraft.world.level.pathfinder.Path path = this.getNavigation().createPath(t, 0);
        lastReachable = path != null && path.canReach();
        return lastReachable;
    }

    /**
     * In a fight, with blocks in its pack, a pirate builds like a player would: pillars up to an
     * enemy above it, bridges over gaps and water toward one it can't reach, and throws up a wall
     * for cover when it's being shot at from range. When its own target starts building, it rushes in.
     */
    private void tickBuilding() {
        if (rushTicks > 0) rushTicks--;
        if (coverCooldown > 0) coverCooldown--;
        LivingEntity t = getTarget();

        // Finishing a cover wall, a block at a time.
        if (!buildQueue.isEmpty()) {
            this.getNavigation().stop();
            if (t != null) this.getLookControl().setLookAt(t);
            if (this.tickCount % 2 == 0) placeBuildBlock(buildQueue.poll());
            return;
        }
        if (t == null || !t.isAlive()) {
            pillarBase = null;
            return;
        }

        // Spot the enemy walling up or towering: get in there before it's finished.
        if (this.tickCount % 10 == 0 && rushTicks == 0 && BuildTracker.isBuilding(t, 2) && this.distanceToSqr(t) < 24 * 24) {
            rushTicks = 100;
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0, false, false));
        }

        // Mid-pillar: once it's jumped clear of the block below its feet, put a block there.
        if (pillarBase != null) {
            if (!this.onGround() && this.getY() > pillarBase.getY() + 1.0) {
                placeBuildBlock(pillarBase);
                pillarBase = null;
            } else if (this.tickCount - pillarStart > 20) {
                pillarBase = null;
            }
            return;
        }

        if (this.tickCount % 5 != 0 || isUsingItem() || findBuildBlock() < 0 || !canBuild()) return;
        double dx = t.getX() - getX(), dz = t.getZ() - getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        double dy = t.getY() - getY();
        boolean holding = getOrders() == Orders.HOLD;

        // 1. Tower up to an enemy above that it can't walk to.
        if (!holding && dy >= 2.0 && flat < 5.0 && this.onGround() && !canReach(t)
                && this.level().getBlockState(blockPosition().above(2)).canBeReplaced()) {
            pillarBase = blockPosition();
            pillarStart = this.tickCount;
            this.getNavigation().stop();
            this.getJumpControl().jump();
            return;
        }

        // 2. Bridge over a gap or water toward an enemy it can't reach.
        if (!holding && flat > 2.0 && Math.abs(dy) < 4.0 && (this.onGround() || this.isInWater()) && !canReach(t)) {
            Direction d = Direction.getNearest(dx, 0, dz);
            BlockPos front = blockPosition().relative(d);
            BlockPos under = front.below();
            if (this.level().getBlockState(front).canBeReplaced() && this.level().getBlockState(front.above()).canBeReplaced()
                    && !this.level().getBlockState(under).isFaceSturdy(this.level(), under, Direction.UP)) {
                if (placeBuildBlock(under)) {
                    this.getMoveControl().setWantedPosition(front.getX() + 0.5, front.getY(), front.getZ() + 0.5, 1.0);
                }
            }
            return;
        }

        // 3. Being shot at from range with a bow out: wall up for cover (3 wide, 2 high, 2 blocks out).
        if (coverCooldown == 0 && isRangedWeapon(getMainHandItem()) && this.tickCount - lastShotAt < 40 && flat > 8.0 && flat < 32.0) {
            Direction d = Direction.getNearest(dx, 0, dz);
            Direction side = d.getClockWise();
            BlockPos centre = blockPosition().relative(d, 2);
            if (this.level().getBlockState(centre.below()).isFaceSturdy(this.level(), centre.below(), Direction.UP)) {
                for (int h = 0; h < 2; h++) {
                    for (int o = -1; o <= 1; o++) buildQueue.add(centre.relative(side, o).above(h));
                }
                coverCooldown = 400;
            }
        }
    }

    // ------------------------------------------------------------------ potions & food

    private static final int OFFHAND_SOURCE = -1, NONE = -2;
    /** The single potion / apple / milk being drunk or eaten from the off hand right now. */
    @Nullable private ItemStack consuming;
    /** Whatever was in the off hand (usually a shield) while drinking. */
    private ItemStack stashedOffhand = ItemStack.EMPTY;
    private int lastConsume = -1000;
    private int lastThrow = -1000;
    private int lastMilk = -1000;

    /** Bounty hunters never run out of potions. */
    protected boolean infiniteConsumables() {
        return false;
    }

    /** Ticks between offensive splash potions. */
    protected int throwCooldown() {
        return 80;
    }

    private static boolean hasEffect(ItemStack s, Predicate<MobEffectInstance> want) {
        for (MobEffectInstance e : PotionUtils.getMobEffects(s)) if (want.test(e)) return true;
        return false;
    }

    private static boolean isSplash(ItemStack s) {
        return s.is(Items.SPLASH_POTION) || s.is(Items.LINGERING_POTION);
    }

    private static boolean isHealing(MobEffectInstance e) {
        return e.getEffect() == MobEffects.HEAL || e.getEffect() == MobEffects.REGENERATION;
    }

    private static boolean isHarmfulPotion(ItemStack s) {
        List<MobEffectInstance> effects = PotionUtils.getMobEffects(s);
        if (effects.isEmpty()) return false;
        for (MobEffectInstance e : effects) if (e.getEffect().getCategory() != MobEffectCategory.HARMFUL) return false;
        return true;
    }

    private boolean isBuffWanted(MobEffectInstance e) {
        MobEffect m = e.getEffect();
        return (m == MobEffects.DAMAGE_BOOST || m == MobEffects.MOVEMENT_SPEED || m == MobEffects.DAMAGE_RESISTANCE
                || m == MobEffects.ABSORPTION) && !this.hasEffect(m);
    }

    private boolean hasHarmfulEffect() {
        for (MobEffectInstance e : this.getActiveEffects()) if (e.getEffect().getCategory() == MobEffectCategory.HARMFUL) return true;
        return false;
    }

    /** Off hand first, then the pack. */
    private int findConsumable(Predicate<ItemStack> want) {
        if (consuming == null && want.test(getOffhandItem())) return OFFHAND_SOURCE;
        for (int i = 0; i < pack.getContainerSize(); i++) if (want.test(pack.getItem(i))) return i;
        return NONE;
    }

    private ItemStack takeOne(int source) {
        ItemStack s = source == OFFHAND_SOURCE ? getOffhandItem() : pack.getItem(source);
        ItemStack one = s.copy();
        one.setCount(1);
        if (!infiniteConsumables()) {
            s.shrink(1);
            if (source == OFFHAND_SOURCE && s.isEmpty()) setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            pack.setChanged();
        }
        return one;
    }

    /** Drink or eat one item from the off hand (whatever was there is put back afterwards). */
    private void startConsuming(ItemStack one) {
        stashedOffhand = getOffhandItem().copy();
        consuming = one.copy();
        setItemSlot(EquipmentSlot.OFFHAND, one);
        startUsingItem(InteractionHand.OFF_HAND);
        lastConsume = this.tickCount;
    }

    private void throwPotion(ItemStack potion, @Nullable LivingEntity target) {
        ThrownPotion thrown = new ThrownPotion(this.level(), this);
        thrown.setItem(potion);
        if (target == null) {
            thrown.shoot(0, -1, 0, 0.3F, 0); // at its own feet
        } else {
            Vec3 move = target.getDeltaMovement();
            double dx = target.getX() + move.x - getX();
            double dy = target.getEyeY() - 1.1 - getY();
            double dz = target.getZ() + move.z - getZ();
            double flat = Math.sqrt(dx * dx + dz * dz);
            thrown.setXRot(thrown.getXRot() + 20.0F);
            thrown.shoot(dx, dy + flat * 0.2, dz, 0.75F, 8.0F);
        }
        this.swing(InteractionHand.MAIN_HAND);
        this.playSound(SoundEvents.WITCH_THROW, 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
        this.level().addFreshEntity(thrown);
        lastConsume = this.tickCount;
    }

    /**
     * Use potions, golden apples and milk from the off hand or pack: milk to purge debuffs, healing
     * potions and golden apples when hurt, fire resistance when burning, strength/speed/resistance
     * in a fight, and harmful splash potions thrown at the enemy. Pirates have no hunger, so food
     * is only eaten for its effects.
     */
    private void tickConsumables() {
        if (consuming != null || isUsingItem() || this.tickCount - lastConsume < 30) return;
        LivingEntity target = getTarget();
        boolean fighting = target != null && target.isAlive();
        float hp = this.getHealth() / this.getMaxHealth();
        int src;

        // 1. Milk purges debuffs (only the bad effects).
        if (hasHarmfulEffect() && this.tickCount - lastMilk > (infiniteConsumables() ? 200 : 40)
                && (src = findConsumable(st -> st.is(Items.MILK_BUCKET))) != NONE) {
            lastMilk = this.tickCount;
            startConsuming(takeOne(src));
            return;
        }

        // 2. Heal up when badly hurt.
        if (hp < 0.45F && (fighting || hp < 0.3F)) {
            if ((src = findConsumable(st -> st.is(Items.POTION) && hasEffect(st, PirateEntity::isHealing))) != NONE) {
                startConsuming(takeOne(src));
                return;
            }
            if ((src = findConsumable(st -> isSplash(st) && hasEffect(st, PirateEntity::isHealing))) != NONE) {
                throwPotion(takeOne(src), null);
                return;
            }
            boolean desperate = hp < 0.3F && fighting;
            src = findConsumable(st -> st.is(desperate ? Items.ENCHANTED_GOLDEN_APPLE : Items.GOLDEN_APPLE));
            if (src == NONE) src = findConsumable(st -> st.is(Items.GOLDEN_APPLE) || st.is(Items.ENCHANTED_GOLDEN_APPLE));
            if (src != NONE) {
                startConsuming(takeOne(src));
                return;
            }
        }

        // 3. Burning: fire resistance.
        if (this.isOnFire() && !this.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            Predicate<MobEffectInstance> fireRes = e -> e.getEffect() == MobEffects.FIRE_RESISTANCE;
            if ((src = findConsumable(st -> st.is(Items.POTION) && hasEffect(st, fireRes))) != NONE) {
                startConsuming(takeOne(src));
                return;
            }
            if ((src = findConsumable(st -> isSplash(st) && hasEffect(st, fireRes))) != NONE) {
                throwPotion(takeOne(src), null);
                return;
            }
        }

        if (!fighting) return;
        double dist = this.distanceTo(target);

        // 4. Buff up for the fight.
        if (dist < 16 && (src = findConsumable(st -> st.is(Items.POTION) && hasEffect(st, this::isBuffWanted))) != NONE) {
            startConsuming(takeOne(src));
            return;
        }

        // 5. Throw harmful splash potions at the enemy (never into a crewmate).
        if (dist >= 5.0 && dist <= 10.0 && this.tickCount - lastThrow > throwCooldown() && this.getSensing().hasLineOfSight(target)) {
            final LivingEntity t = target;
            src = findConsumable(st -> isSplash(st) && isHarmfulPotion(st)
                    && hasEffect(st, e -> !t.hasEffect(e.getEffect()) || e.getEffect().isInstantenous()));
            if (src != NONE) {
                boolean friendNear = !this.level().getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(4.0),
                        e -> e != t && (e == this || CrewManager.areCrewmates(this, e))).isEmpty();
                if (!friendNear) {
                    lastThrow = this.tickCount;
                    throwPotion(takeOne(src), t);
                }
            }
        }
    }

    private void applyConsumable(ItemStack item) {
        if (item.is(Items.MILK_BUCKET)) {
            for (MobEffectInstance e : new ArrayList<>(this.getActiveEffects())) {
                if (e.getEffect().getCategory() == MobEffectCategory.HARMFUL) this.removeEffect(e.getEffect());
            }
        } else if (item.is(Items.POTION)) {
            for (MobEffectInstance e : PotionUtils.getMobEffects(item)) {
                if (e.getEffect().isInstantenous()) e.getEffect().applyInstantenousEffect(this, this, this, e.getAmplifier(), 1.0);
                else this.addEffect(new MobEffectInstance(e));
            }
        } else {
            FoodProperties food = item.getFoodProperties(this);
            if (food != null) {
                for (Pair<MobEffectInstance, Float> p : food.getEffects()) {
                    if (p.getFirst() != null && this.random.nextFloat() < p.getSecond()) this.addEffect(new MobEffectInstance(p.getFirst()));
                }
                this.playSound(SoundEvents.PLAYER_BURP, 0.5F, this.random.nextFloat() * 0.1F + 0.9F);
            }
        }
    }

    @Override
    protected void completeUsingItem() {
        if (!this.level().isClientSide && consuming != null && isUsingItem()) {
            ItemStack item = consuming;
            this.triggerItemUseEffects(getUseItem(), 16);
            applyConsumable(item);
            consuming = null; // finished, so stopping doesn't hand it back
            stopUsingItem();
            setItemSlot(EquipmentSlot.OFFHAND, stashedOffhand);
            stashedOffhand = ItemStack.EMPTY;
            if (!infiniteConsumables()) {
                if (item.is(Items.POTION)) addToPack(new ItemStack(Items.GLASS_BOTTLE));
                else if (item.is(Items.MILK_BUCKET)) addToPack(new ItemStack(Items.BUCKET));
            }
            return;
        }
        super.completeUsingItem();
    }

    /** Interrupted while drinking or eating: put things back as they were. */
    @Override
    public void stopUsingItem() {
        ItemStack c = consuming;
        consuming = null;
        super.stopUsingItem();
        if (c != null && !this.level().isClientSide) {
            setItemSlot(EquipmentSlot.OFFHAND, stashedOffhand);
            stashedOffhand = ItemStack.EMPTY;
            if (!infiniteConsumables()) addToPack(c);
        }
    }

    // ------------------------------------------------------------------ shields

    private static boolean isShield(ItemStack s) {
        return !s.isEmpty() && s.canPerformAction(ToolActions.SHIELD_BLOCK);
    }

    /** Hand holding a shield: off hand normally, main hand if that's all it has. */
    @Nullable
    private InteractionHand shieldHand() {
        if (isShield(getOffhandItem())) return InteractionHand.OFF_HAND;
        if (isShield(getMainHandItem())) return InteractionHand.MAIN_HAND;
        return null;
    }

    private boolean raisingShield() {
        return isUsingItem() && isShield(getUseItem());
    }

    /** An arrow, trident or fireball flying at this pirate from someone who isn't a crewmate. */
    private boolean projectileIncoming() {
        for (Projectile p : this.level().getEntitiesOfClass(Projectile.class, this.getBoundingBox().inflate(10.0))) {
            if (p.getOwner() == this || (p.getOwner() != null && CrewManager.areCrewmates(this, p.getOwner()))) continue;
            if (p instanceof AbstractArrow a && a.isNoPhysics()) continue;
            Vec3 vel = p.getDeltaMovement();
            if (vel.lengthSqr() < 0.04) continue; // lying on the ground
            Vec3 toMe = this.position().add(0, this.getBbHeight() * 0.5, 0).subtract(p.position());
            if (vel.normalize().dot(toMe.normalize()) > 0.85) return true;
        }
        return false;
    }

    /**
     * Raise the shield against danger and lower it to strike. In melee the pirate blocks between its
     * own swings; it also blocks incoming arrows and enemies drawing a bow at it.
     */
    private void tickShield() {
        if (shieldCooldown > 0) shieldCooldown--;
        InteractionHand hand = shieldHand();
        LivingEntity target = getTarget();
        if (hand == null || shieldCooldown > 0 || target == null || !target.isAlive()) {
            if (raisingShield()) stopUsingItem();
            return;
        }
        // Busy drawing a bow or loading a crossbow: don't interrupt.
        if (isUsingItem() && !raisingShield()) return;

        boolean rangedMode = isRangedWeapon(getMainHandItem());
        double dist = this.distanceTo(target);
        boolean incoming = projectileIncoming();
        boolean aimedAt = target.isUsingItem() && isRangedWeapon(target.getUseItem()) && dist < 24
                && (!(target instanceof Mob m) || m.getTarget() == this) && this.getSensing().hasLineOfSight(target);
        boolean meleeDanger = !rangedMode && dist < 4.0 && this.tickCount - lastSwing >= 2 && this.tickCount - lastSwing < 14;
        // Brawlers would rather swing than turtle; they only block what's flying at them.
        if (getCombatStyle() == CombatStyle.BRAWLER) meleeDanger = meleeDanger && this.random.nextInt(3) == 0;

        boolean want = incoming || (!rangedMode && aimedAt) || meleeDanger;
        if (want && !raisingShield()) {
            startUsingItem(hand);
        } else if (!want && raisingShield()) {
            stopUsingItem();
        }
    }

    @Override
    protected void hurtCurrentlyUsedShield(float damage) {
        if (this.level().isClientSide || damage < 3.0F || !isShield(this.useItem)) return;
        InteractionHand hand = getUsedItemHand();
        int dmg = 1 + (int) Math.floor(damage);
        this.useItem.hurtAndBreak(dmg, this, e -> e.broadcastBreakEvent(hand));
        if (this.useItem.isEmpty()) {
            this.setItemInHand(hand, ItemStack.EMPTY);
            this.stopUsingItem();
            this.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + this.random.nextFloat() * 0.4F);
        }
    }

    /** Axes knock a pirate's shield aside for a few seconds, just like a player's. */
    @Override
    protected void blockUsingShield(LivingEntity attacker) {
        super.blockUsingShield(attacker);
        if (attacker.getMainHandItem().canDisableShield(this.useItem, this, attacker)) {
            shieldCooldown = 100;
            stopUsingItem();
            this.level().broadcastEntityEvent(this, (byte) 30);
        }
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) return;

        if (!initialized) initPirate(PirateTier.random(this.random));

        if (this.tickCount % 100 == 37 && isRecruited()) checkBountyPromotion();

        if (this.tickCount % 40 == 0) {
            UUID crewId = getCrewId();
            if (crewId != null && !CrewManager.crewStillHasPirate(((ServerLevel) level()).getServer(), crewId, getUUID())) {
                leaveCrew();
            }
        }

        // Pirates saved with an old or removed skin get a new one from the bundled set.
        if (this.tickCount % 100 == 0 && !PirateSkins.isValid(getSkinName()) && PirateSkins.count() > 0) {
            this.entityData.set(DATA_SKIN, rollSkin(getTier()));
        }

        if (this.tickCount % 10 == 0 && this.getTarget() != null) chooseWeapon();
        tickShield();
        if (this.tickCount % 10 == 0) tickConsumables();
        tickBuilding();
        if (pact != null) com.piratecrew.pact.PactPowers.pirateTick(this);

        // Players regenerate, so do pirates (slowly, out of combat).
        if (this.tickCount % 60 == 0 && this.getHealth() < this.getMaxHealth()
                && this.getTarget() == null && this.tickCount - lastCombatTick > 200) {
            this.heal(1.0F);
        }

        // Don't chase enemies too far from the leader when following.
        if (this.tickCount % 20 == 0 && getOrders() == Orders.FOLLOW && this.getTarget() != null) {
            Player leader = getLeader();
            if (leader != null && this.getTarget().distanceToSqr(leader) > 24 * 24) this.setTarget(null);
        }
    }

    @Override
    public void die(DamageSource source) {
        UUID crewId = getCrewId();
        super.die(source);
        if (!this.level().isClientSide && crewId != null) {
            CrewManager.onPirateGone(((ServerLevel) level()).getServer(), crewId, getUUID(),
                    pirateName + " has fallen: " + source.getLocalizedDeathMessage(this).getString());
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean hitByPlayer) {
        if (consuming != null) stopUsingItem();
        // A pact outlives its holder: the scroll returns when a pirate falls.
        if (pact != null) {
            this.spawnAtLocation(new ItemStack(com.piratecrew.registry.ModItems.SOUL_PACTS.get(pact).get()));
            pact = null;
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = this.getItemBySlot(slot);
            if (!stack.isEmpty() && !EnchantmentHelper.hasVanishingCurse(stack)) {
                this.spawnAtLocation(stack);
            }
            this.setItemSlot(slot, ItemStack.EMPTY);
        }
        for (int i = 0; i < pack.getContainerSize(); i++) {
            ItemStack stack = pack.removeItemNoUpdate(i);
            if (!stack.isEmpty() && !EnchantmentHelper.hasVanishingCurse(stack)) this.spawnAtLocation(stack);
        }
    }

    // ------------------------------------------------------------------ save / load

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("PirateInit", initialized);
        tag.putInt("Tier", this.entityData.get(DATA_TIER));
        tag.putString("Skin", getSkinName());
        tag.putString("PirateName", pirateName);
        tag.putInt("Orders", this.entityData.get(DATA_ORDERS));
        UUID crew = getCrewId();
        if (crew != null) tag.putUUID("Crew", crew);
        if (leaderId != null) tag.putUUID("Leader", leaderId);
        if (holdPos != null) tag.put("HoldPos", NbtUtils.writeBlockPos(holdPos));
        if (home != null) tag.put("Home", NbtUtils.writeBlockPos(home));
        tag.putInt("Style", this.entityData.get(DATA_STYLE));
        tag.putFloat("MeleeRange", meleeRange);
        tag.putInt("Task", this.entityData.get(DATA_TASK));
        if (workCenter != null) tag.put("WorkCenter", NbtUtils.writeBlockPos(workCenter));
        ListTag packTag = new ListTag();
        for (int i = 0; i < pack.getContainerSize(); i++) {
            ItemStack stack = pack.getItem(i);
            if (stack.isEmpty()) continue;
            CompoundTag t = new CompoundTag();
            t.putByte("Slot", (byte) i);
            stack.save(t);
            packTag.add(t);
        }
        tag.put("Pack", packTag);
        if (pact != null) tag.putString("SoulPact", pact.id);
        if (consuming != null) {
            tag.put("Consuming", consuming.save(new CompoundTag()));
            tag.put("StashedOffhand", stashedOffhand.save(new CompoundTag()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.pact = com.piratecrew.pact.SoulPact.byId(tag.getString("SoulPact"));
        this.initialized = tag.getBoolean("PirateInit");
        this.entityData.set(DATA_TIER, tag.getInt("Tier"));
        this.entityData.set(DATA_SKIN, tag.getString("Skin"));
        if (tag.contains("PirateName")) this.pirateName = tag.getString("PirateName");
        this.entityData.set(DATA_ORDERS, tag.getInt("Orders"));
        this.entityData.set(DATA_CREW, tag.hasUUID("Crew") ? Optional.of(tag.getUUID("Crew")) : Optional.empty());
        this.leaderId = tag.hasUUID("Leader") ? tag.getUUID("Leader") : null;
        this.holdPos = tag.contains("HoldPos") ? NbtUtils.readBlockPos(tag.getCompound("HoldPos")) : null;
        if (tag.contains("Style")) {
            this.entityData.set(DATA_STYLE, tag.getInt("Style"));
            this.meleeRange = tag.getFloat("MeleeRange");
        } else {
            rollCombatStyle(); // pirates from before combat styles existed
        }
        this.entityData.set(DATA_TASK, tag.getInt("Task"));
        this.workCenter = tag.contains("WorkCenter") ? NbtUtils.readBlockPos(tag.getCompound("WorkCenter")) : null;
        for (int i = 0; i < pack.getContainerSize(); i++) pack.setItem(i, ItemStack.EMPTY);
        for (Tag t : tag.getList("Pack", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            int slot = c.getByte("Slot") & 255;
            if (slot < pack.getContainerSize()) pack.setItem(slot, ItemStack.of(c));
        }
        if (tag.contains("Home")) setHome(NbtUtils.readBlockPos(tag.getCompound("Home")));
        if (tag.contains("Consuming")) {
            // Saved mid-drink: put the off hand back and the potion in the pack.
            setItemSlot(EquipmentSlot.OFFHAND, ItemStack.of(tag.getCompound("StashedOffhand")));
            if (!infiniteConsumables()) addToPack(ItemStack.of(tag.getCompound("Consuming")));
        }
        if (initialized) {
            applyTierStats();
            updateDisplayName();
        }
    }
}
