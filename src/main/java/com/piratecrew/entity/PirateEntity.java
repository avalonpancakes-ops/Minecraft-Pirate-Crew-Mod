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
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class PirateEntity extends PathfinderMob {
    public enum Orders { FOLLOW, HOLD, WANDER }

    private static final EntityDataAccessor<Integer> DATA_TIER = SynchedEntityData.defineId(PirateEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_SKIN = SynchedEntityData.defineId(PirateEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Optional<UUID>> DATA_CREW = SynchedEntityData.defineId(PirateEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_ORDERS = SynchedEntityData.defineId(PirateEntity.class, EntityDataSerializers.INT);

    private static final int HOME_RADIUS = 6;

    private boolean initialized = false;
    private String pirateName = "Pirate";
    @Nullable private UUID leaderId;
    @Nullable private BlockPos holdPos;
    @Nullable private BlockPos home;
    private int lastCombatTick = -1000;

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
    }

    // ------------------------------------------------------------------ AI

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PirateGoals.RangedWeaponGoal(this));
        this.goalSelector.addGoal(1, new PirateGoals.PirateMeleeGoal(this, 1.2));
        this.goalSelector.addGoal(2, new PirateGoals.FollowLeaderGoal(this, 1.15, 5.0F, 2.5F));
        this.goalSelector.addGoal(3, new PirateGoals.HoldPositionGoal(this, 1.0));
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

    private void tierArrowBonus(AbstractArrow arrow) {
        arrow.setBaseDamage(arrow.getBaseDamage() + (getTier().attackDamage - 1.0) * 0.25);
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
        trident.setBaseDamage(trident.getBaseDamage() + (getTier().attackDamage - 1.0) * 0.5);
        aim(trident, target, 1.8F, 0);
        this.playSound(SoundEvents.TRIDENT_THROW, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(trident);
        wearWeapon();
        lastCombatTick = this.tickCount;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        lastCombatTick = this.tickCount;
        return super.doHurtTarget(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt) lastCombatTick = this.tickCount;
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
        this.pirateName = PirateNames.random(this.random);
        applyTierStats();
        this.setHealth(this.getMaxHealth());
        this.entityData.set(DATA_SKIN, PirateSkins.random(this.random, tier));
        updateDisplayName();
    }

    private void applyTierStats() {
        PirateTier tier = getTier();
        var hp = this.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(tier.maxHealth);
        var dmg = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.setBaseValue(tier.attackDamage);
    }

    public void setHome(BlockPos pos) {
        this.home = pos;
        applyRestriction();
    }

    /** Free pirates stay near their bar; crew pirates told to roam stay near where they were told. */
    private void applyRestriction() {
        if (home != null && (!isRecruited() || getOrders() == Orders.WANDER)) {
            this.restrictTo(home, isRecruited() ? 12 : HOME_RADIUS);
        } else {
            this.clearRestriction();
        }
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

    public void updateDisplayName() {
        PirateTier tier = getTier();
        MutableComponent name = Component.literal("[" + tier.label + "] ").withStyle(tier.color, ChatFormatting.BOLD);
        if (isViceCaptain()) name.append(Component.literal("\u2606 ").withStyle(s -> s.withBold(false).withColor(ChatFormatting.GOLD)));
        name.append(Component.literal(pirateName).withStyle(s -> s.withBold(false)
                        .withColor(isRecruited() ? ChatFormatting.WHITE : ChatFormatting.GRAY)));
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
        this.entityData.set(DATA_ORDERS, Orders.WANDER.ordinal());
        this.setTarget(null);
        this.setHome(this.blockPosition());
        updateDisplayName();
    }

    public void setOrders(Orders orders, Player by) {
        this.entityData.set(DATA_ORDERS, orders.ordinal());
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

        ServerLevel sl = (ServerLevel) this.level();
        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.0, getZ(), 12, 0.4, 0.6, 0.4, 0.0);
        sl.playSound(null, blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 0.9F);
        sp.sendSystemMessage(Component.literal("⚓ ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("[" + tier.label + "] ").withStyle(tier.color, ChatFormatting.BOLD))
                .append(Component.literal(pirateName + " joined " + crew.name + "! Right-click them to give gear and orders.").withStyle(ChatFormatting.YELLOW)));
        return InteractionResult.CONSUME;
    }

    private void openEquipment(ServerPlayer player) {
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

    // ------------------------------------------------------------------ ticking

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) return;

        if (!initialized) initPirate(PirateTier.random(this.random));

        if (this.tickCount % 40 == 0) {
            UUID crewId = getCrewId();
            if (crewId != null && !CrewManager.crewStillHasPirate(((ServerLevel) level()).getServer(), crewId, getUUID())) {
                leaveCrew();
            }
        }

        // Pirates saved with an old or removed skin get a new one from the bundled set.
        if (this.tickCount % 100 == 0 && !PirateSkins.isValid(getSkinName()) && PirateSkins.count() > 0) {
            this.entityData.set(DATA_SKIN, PirateSkins.random(this.random, getTier()));
        }

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
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = this.getItemBySlot(slot);
            if (!stack.isEmpty() && !EnchantmentHelper.hasVanishingCurse(stack)) {
                this.spawnAtLocation(stack);
            }
            this.setItemSlot(slot, ItemStack.EMPTY);
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.initialized = tag.getBoolean("PirateInit");
        this.entityData.set(DATA_TIER, tag.getInt("Tier"));
        this.entityData.set(DATA_SKIN, tag.getString("Skin"));
        if (tag.contains("PirateName")) this.pirateName = tag.getString("PirateName");
        this.entityData.set(DATA_ORDERS, tag.getInt("Orders"));
        this.entityData.set(DATA_CREW, tag.hasUUID("Crew") ? Optional.of(tag.getUUID("Crew")) : Optional.empty());
        this.leaderId = tag.hasUUID("Leader") ? tag.getUUID("Leader") : null;
        this.holdPos = tag.contains("HoldPos") ? NbtUtils.readBlockPos(tag.getCompound("HoldPos")) : null;
        if (tag.contains("Home")) setHome(NbtUtils.readBlockPos(tag.getCompound("Home")));
        if (initialized) {
            applyTierStats();
            updateDisplayName();
        }
    }
}
