package com.piratecrew.entity.boss;

import com.piratecrew.entity.MarineEntity;
import com.piratecrew.entity.PirateTier;
import com.piratecrew.sundered.Marines;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A named officer of the Order of the Tide fought as a boss: boss bar, huge health, telegraphed
 * special attacks, no gear drops (only its own loot), never despawns, never leaves the dimension.
 * Unlike regular marines, a boss fights anyone who challenges it, pirate or not.
 */
public abstract class MarineBossEntity extends MarineEntity implements BountyBoss {
    protected final ServerBossEvent bossEvent = new ServerBossEvent(Component.literal("Boss"), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
    protected final BossFx fx = new BossFx();
    /** Ticks until the next special attack. */
    protected int abilityCooldown = 60;
    /** Which health thresholds have already triggered (bit flags, subclasses decide). */
    protected int phaseFlags;

    protected MarineBossEntity(EntityType<? extends MarineBossEntity> type, Level level) {
        super(type, level);
        this.xpReward = 250;
        this.setPersistenceRequired();
    }

    // ------------------------------------------------------------------ what each boss defines

    protected abstract String bossName();

    protected abstract String bossTitle();

    protected abstract String bossSkin();

    protected abstract ChatFormatting bossColor();

    protected abstract BossEvent.BossBarColor barColor();

    protected abstract double bossHealth();

    protected abstract double bossDamage();

    protected abstract double bossArmor();

    protected abstract double bossToughness();

    protected abstract void equipBoss();

    /** Runs every server tick while the boss has a living target. */
    protected abstract void tickBoss(ServerLevel level, LivingEntity target);

    protected abstract void dropBossLoot(DamageSource source, int looting);

    // ------------------------------------------------------------------ setup

    public void setupBoss() {
        setupMarine(Rank.CAPTAIN);
        for (int i = 0; i < getPack().getContainerSize(); i++) getPack().setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
        var armor = getAttribute(Attributes.ARMOR);
        if (armor != null) armor.setBaseValue(bossArmor());
        var tough = getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (tough != null) tough.setBaseValue(bossToughness());
        var kb = getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb != null) kb.setBaseValue(0.9);
        var follow = getAttribute(Attributes.FOLLOW_RANGE);
        if (follow != null) follow.setBaseValue(48.0);
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(0.32);
        setPatrol(false);
        equipBoss();
        for (var slot : net.minecraft.world.entity.EquipmentSlot.values()) setDropChance(slot, 0.0F);
        setHealth(getMaxHealth());
        updateDisplayName();
    }

    @Override
    protected void applyTierStats() {
        var hp = getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(bossHealth());
        var dmg = getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.setBaseValue(bossDamage());
    }

    @Override
    protected String rollSkin(PirateTier tier) {
        return bossSkin();
    }

    @Override
    protected String rollName() {
        return bossName();
    }

    @Override
    public void updateDisplayName() {
        MutableComponent name = Component.literal("☠ " + bossTitle() + " " + bossName()).withStyle(bossColor(), ChatFormatting.BOLD);
        setCustomName(name);
        setCustomNameVisible(true);
        bossEvent.setName(name);
        bossEvent.setColor(barColor());
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Bosses fight anyone who comes for them, not only pirates.
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        if (!isInitialized()) setupBoss();
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    @Override
    public void aiStep() {
        if (!level().isClientSide && !isInitialized()) setupBoss();
        super.aiStep();
    }

    @Override
    protected boolean canBuild() {
        return false;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    // ------------------------------------------------------------------ boss bar

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        ServerLevel level = (ServerLevel) level();
        fx.tick(level, this);
        if (abilityCooldown > 0) abilityCooldown--;
        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && target.level() == level) tickBoss(level, target);
        // Too far from anything to fight: drift back to full health slowly.
        if (target == null && tickCount % 20 == 0 && getHealth() < getMaxHealth()) heal(getMaxHealth() * 0.01F);
    }

    // ------------------------------------------------------------------ helpers for subclasses

    protected float healthFraction() {
        return getHealth() / getMaxHealth();
    }

    /** True once, the first time health drops below {@code fraction} (flag = a unique bit). */
    protected boolean crossed(float fraction, int flag) {
        if ((phaseFlags & flag) != 0 || healthFraction() > fraction) return false;
        phaseFlags |= flag;
        return true;
    }

    /** Say something to everyone within 48 blocks. */
    protected void shout(String line) {
        Component msg = Component.literal(bossName() + ": ").withStyle(bossColor(), ChatFormatting.BOLD)
                .append(Component.literal(line).withStyle(s -> s.withBold(false).withColor(ChatFormatting.WHITE)));
        for (Player p : level().players()) if (p.distanceToSqr(this) < 48 * 48) p.sendSystemMessage(msg);
    }

    /** Call in marines around the boss to join the fight. */
    protected void callMarines(ServerLevel level, LivingEntity target, int count, Rank... ranks) {
        for (int i = 0; i < count; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            Vec3 at = position().add(Math.cos(a) * 4, 0.5, Math.sin(a) * 4);
            MarineEntity m = Marines.spawn(level, at, ranks[i % ranks.length], true);
            if (m != null) m.setTarget(target);
        }
    }

    /** Boss gear never breaks (and never drops). */
    protected static net.minecraft.world.item.ItemStack gear(net.minecraft.world.item.Item item, Object... enchants) {
        net.minecraft.world.item.ItemStack s = new net.minecraft.world.item.ItemStack(item);
        s.getOrCreateTag().putBoolean("Unbreakable", true);
        for (int i = 0; i + 1 < enchants.length; i += 2) {
            s.enchant((net.minecraft.world.item.enchantment.Enchantment) enchants[i], (Integer) enchants[i + 1]);
        }
        return s;
    }

    /** Blocks between the boss and its target, for abilities that shouldn't go through walls. */
    protected boolean canSeeTarget(LivingEntity target) {
        return getSensing().hasLineOfSight(target);
    }

    /** Leap toward a point (sets velocity; landing handled by the caller). */
    protected void leapAt(Vec3 to, double up) {
        Vec3 d = to.subtract(position());
        Vec3 h = new Vec3(d.x, 0, d.z);
        double dist = h.length();
        if (dist < 0.01) return;
        Vec3 v = h.normalize().scale(Math.min(2.2, dist * 0.16));
        setDeltaMovement(v.x, up, v.z);
        hasImpulse = true;
    }

    // ------------------------------------------------------------------ death and loot

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide && level().getServer() != null) {
            Entity killer = source.getEntity();
            String by = killer instanceof Player p ? " by " + p.getGameProfile().getName()
                    : killer instanceof com.piratecrew.entity.PirateEntity pe ? " by " + pe.getPirateName() : "";
            level().getServer().getPlayerList().broadcastSystemMessage(Component.literal("⚓ " + bossTitle() + " " + bossName()
                    + " has been defeated" + by + "!").withStyle(bossColor(), ChatFormatting.BOLD), false);
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean hitByPlayer) {
        dropBossLoot(source, looting);
    }

    // ------------------------------------------------------------------ save

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BossPhase", phaseFlags);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        this.phaseFlags = tag.getInt("BossPhase");
        super.readAdditionalSaveData(tag);
        if (hasCustomName()) bossEvent.setName(getDisplayName());
        bossEvent.setColor(barColor());
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        if (name != null) bossEvent.setName(name);
    }
}
