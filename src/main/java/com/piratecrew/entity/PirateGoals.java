package com.piratecrew.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

import java.util.EnumSet;

public class PirateGoals {

    /** Normal melee, used whenever the pirate isn't holding a bow, crossbow or trident. */
    public static class PirateMeleeGoal extends MeleeAttackGoal {
        private final PirateEntity pirate;

        public PirateMeleeGoal(PirateEntity pirate, double speed) {
            super(pirate, speed, true);
            this.pirate = pirate;
        }

        @Override
        public boolean canUse() {
            return pirate.getRangedType() == PirateEntity.Ranged.NONE && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return pirate.getRangedType() == PirateEntity.Ranged.NONE && super.canContinueToUse();
        }

        @Override
        public void tick() {
            super.tick();
            // Brawlers charge in faster, marksmen approach warily.
            if (!pirate.getNavigation().isDone()) {
                pirate.getNavigation().setSpeedModifier(pirate.getCombatStyle().chargeSpeed * (pirate.isRushing() ? 1.25 : 1.0));
            }
        }
    }

    /**
     * Bow / crossbow / trident combat: get within range and line of sight, back off if the target
     * is too close, draw (or load) the weapon and fire.
     */
    public static class RangedWeaponGoal extends Goal {
        private final PirateEntity pirate;
        private LivingEntity target;
        private int cooldown;
        private int unseenTicks;

        /** Stationary shooters (ship crews) never walk toward their target, so they don't step off the deck. */
        private final java.util.function.BooleanSupplier stationary;

        public RangedWeaponGoal(PirateEntity pirate) {
            this(pirate, () -> false);
        }

        public RangedWeaponGoal(PirateEntity pirate, java.util.function.BooleanSupplier stationary) {
            this.pirate = pirate;
            this.stationary = stationary;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = pirate.getTarget();
            if (t == null || !t.isAlive() || pirate.getRangedType() == PirateEntity.Ranged.NONE) return false;
            target = t;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            cooldown = 10;
            unseenTicks = 0;
        }

        @Override
        public void stop() {
            target = null;
            // Don't cancel a potion or golden apple: another goal (backing off to eat) may have taken over.
            if (pirate.isUsingItem() && !pirate.isConsuming()) pirate.stopUsingItem();
            pirate.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (target == null) return;
            PirateEntity.Ranged type = pirate.getRangedType();
            double dist = pirate.distanceToSqr(target);
            boolean canSee = pirate.getSensing().hasLineOfSight(target);
            unseenTicks = canSee ? 0 : unseenTicks + 1;
            float keep = pirate.getCombatStyle().keepDistance;
            double range = Math.max(type == PirateEntity.Ranged.TRIDENT ? 12.0 : 18.0, keep + 4.0);

            // Movement
            if (stationary.getAsBoolean()) {
                pirate.getNavigation().stop();
            } else if (!canSee || dist > range * range) {
                if (!pirate.getNavigation().moveTo(target, 1.1)) {
                    pirate.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.1);
                }
            } else {
                pirate.getNavigation().stop();
                // Too close for comfort: back off to this pirate's preferred distance.
                if (dist < (keep - 2.0) * (keep - 2.0)) pirate.getMoveControl().strafe(-0.6F, 0.0F);
            }
            pirate.getLookControl().setLookAt(target, 30.0F, 30.0F);
            pirate.lookAt(target, 30.0F, 30.0F);

            // Shield raised in the off hand: hold fire until it comes down.
            if (pirate.isUsingItem() && pirate.getUsedItemHand() != InteractionHand.MAIN_HAND) return;

            switch (type) {
                case BOW -> tickDrawAndRelease(canSee, 20, () -> {
                    int drawn = pirate.getTicksUsingItem();
                    pirate.stopUsingItem();
                    pirate.shootBow(target, BowItem.getPowerForTime(drawn));
                    cooldown = 15 + pirate.getRandom().nextInt(10);
                });
                case TRIDENT -> tickDrawAndRelease(canSee, 12, () -> {
                    pirate.stopUsingItem();
                    pirate.throwTrident(target);
                    cooldown = 30 + pirate.getRandom().nextInt(10);
                });
                case CROSSBOW -> tickCrossbow(canSee);
                default -> {}
            }
        }

        private void tickDrawAndRelease(boolean canSee, int drawTicks, Runnable fire) {
            if (pirate.isUsingItem()) {
                if (unseenTicks > 60) {
                    pirate.stopUsingItem();
                } else if (canSee && pirate.getTicksUsingItem() >= drawTicks) {
                    fire.run();
                }
            } else if (--cooldown <= 0 && canSee) {
                pirate.startUsingItem(InteractionHand.MAIN_HAND);
            }
        }

        private void tickCrossbow(boolean canSee) {
            ItemStack crossbow = pirate.getMainHandItem();
            if (!CrossbowItem.isCharged(crossbow)) {
                if (!pirate.isUsingItem()) {
                    pirate.startUsingItem(InteractionHand.MAIN_HAND);
                } else if (pirate.getTicksUsingItem() >= CrossbowItem.getChargeDuration(crossbow)) {
                    pirate.stopUsingItem();
                    CrossbowItem.setCharged(crossbow, true);
                    pirate.playSound(SoundEvents.CROSSBOW_LOADING_END, 1.0F, 1.0F);
                    cooldown = 10 + pirate.getRandom().nextInt(15);
                }
            } else if (--cooldown <= 0 && canSee) {
                pirate.shootCrossbow(target);
            }
        }
    }

    /** Follows the crew leader like a tamed wolf; teleports if left far behind. */
    public static class FollowLeaderGoal extends Goal {
        private final PirateEntity pirate;
        private final double speed;
        private final float startDist;
        private final float stopDist;
        private Player leader;
        private int recalc;

        public FollowLeaderGoal(PirateEntity pirate, double speed, float startDist, float stopDist) {
            this.pirate = pirate;
            this.speed = speed;
            this.startDist = startDist;
            this.stopDist = stopDist;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (pirate.getOrders() != PirateEntity.Orders.FOLLOW) return false;
            Player p = pirate.getLeader();
            if (p == null || p.isSpectator()) return false;
            if (pirate.distanceToSqr(p) < startDist * startDist) return false;
            leader = p;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (leader == null || !leader.isAlive() || pirate.getOrders() != PirateEntity.Orders.FOLLOW) return false;
            if (pirate.getNavigation().isDone() && pirate.distanceToSqr(leader) > 12 * 12) return true; // will teleport
            return pirate.distanceToSqr(leader) > stopDist * stopDist;
        }

        @Override
        public void start() {
            recalc = 0;
        }

        @Override
        public void stop() {
            leader = null;
            pirate.getNavigation().stop();
        }

        @Override
        public void tick() {
            pirate.getLookControl().setLookAt(leader, 10.0F, pirate.getMaxHeadXRot());
            if (--recalc > 0) return;
            recalc = adjustedTickDelay(10);
            if (pirate.isLeashed() || pirate.isPassenger()) return;
            if (pirate.distanceToSqr(leader) >= 20 * 20) {
                teleportNearLeader();
            } else {
                pirate.getNavigation().moveTo(leader, speed);
            }
        }

        private void teleportNearLeader() {
            BlockPos base = leader.blockPosition();
            for (int i = 0; i < 10; i++) {
                int dx = pirate.getRandom().nextIntBetweenInclusive(-3, 3);
                int dy = pirate.getRandom().nextIntBetweenInclusive(-1, 1);
                int dz = pirate.getRandom().nextIntBetweenInclusive(-3, 3);
                if (Math.abs(dx) < 2 && Math.abs(dz) < 2) continue;
                BlockPos pos = base.offset(dx, dy, dz);
                if (WalkNodeEvaluator.getBlockPathTypeStatic(pirate.level(), pos.mutable()) != BlockPathTypes.WALKABLE) continue;
                double mx = pos.getX() + 0.5 - pirate.getX();
                double my = pos.getY() - pirate.getY();
                double mz = pos.getZ() + 0.5 - pirate.getZ();
                if (!pirate.level().noCollision(pirate, pirate.getBoundingBox().move(mx, my, mz))) continue;
                pirate.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, pirate.getYRot(), pirate.getXRot());
                pirate.getNavigation().stop();
                return;
            }
        }
    }

    /** Walk back to the spot the pirate was told to guard. */
    public static class HoldPositionGoal extends Goal {
        private final PirateEntity pirate;
        private final double speed;

        public HoldPositionGoal(PirateEntity pirate, double speed) {
            this.pirate = pirate;
            this.speed = speed;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            BlockPos pos = pirate.getHoldPos();
            return pirate.getOrders() == PirateEntity.Orders.HOLD && pos != null
                    && pirate.blockPosition().distSqr(pos) > 2 * 2;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() && !pirate.getNavigation().isDone();
        }

        @Override
        public void start() {
            BlockPos pos = pirate.getHoldPos();
            if (pos != null) pirate.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, speed);
        }
    }

    /** Random strolling, except when following or holding a position. */
    public static class IdleStrollGoal extends WaterAvoidingRandomStrollGoal {
        private final PirateEntity pirate;

        public IdleStrollGoal(PirateEntity pirate, double speed) {
            super(pirate, speed);
            this.pirate = pirate;
        }

        @Override
        public boolean canUse() {
            PirateEntity.Orders o = pirate.getOrders();
            return (!pirate.isRecruited() || o == PirateEntity.Orders.WANDER) && super.canUse();
        }
    }

    /** Attack whatever hurt the leader. */
    public static class DefendLeaderGoal extends TargetGoal {
        private final PirateEntity pirate;
        private LivingEntity attacker;
        private int timestamp;

        public DefendLeaderGoal(PirateEntity pirate) {
            super(pirate, false);
            this.pirate = pirate;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (!pirate.isRecruited()) return false;
            Player leader = pirate.getLeader();
            if (leader == null) return false;
            attacker = leader.getLastHurtByMob();
            int ts = leader.getLastHurtByMobTimestamp();
            return ts != timestamp && attacker != null && canAttack(attacker, TargetingConditions.DEFAULT);
        }

        @Override
        public void start() {
            mob.setTarget(attacker);
            Player leader = pirate.getLeader();
            if (leader != null) timestamp = leader.getLastHurtByMobTimestamp();
            super.start();
        }
    }

    /** Attack whatever the leader attacks. */
    public static class AssistLeaderGoal extends TargetGoal {
        private final PirateEntity pirate;
        private LivingEntity victim;
        private int timestamp;

        public AssistLeaderGoal(PirateEntity pirate) {
            super(pirate, false);
            this.pirate = pirate;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (!pirate.isRecruited()) return false;
            Player leader = pirate.getLeader();
            if (leader == null) return false;
            victim = leader.getLastHurtMob();
            int ts = leader.getLastHurtMobTimestamp();
            return ts != timestamp && victim != null && canAttack(victim, TargetingConditions.DEFAULT);
        }

        @Override
        public void start() {
            mob.setTarget(victim);
            Player leader = pirate.getLeader();
            if (leader != null) timestamp = leader.getLastHurtMobTimestamp();
            super.start();
        }
    }

    /**
     * While drinking a potion or eating a golden apple mid-fight, back away from the enemy to make
     * room instead of standing in front of it. Ship crews stay on deck and eat where they stand.
     */
    public static class RetreatToEatGoal extends Goal {
        private final PirateEntity pirate;
        private int repath;

        public RetreatToEatGoal(PirateEntity pirate) {
            this.pirate = pirate;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = pirate.getTarget();
            return pirate.isConsuming() && pirate.canRetreat() && t != null && t.isAlive() && pirate.distanceToSqr(t) < 14 * 14;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            repath = 0;
        }

        @Override
        public void stop() {
            pirate.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity t = pirate.getTarget();
            if (t == null) return;
            pirate.getLookControl().setLookAt(t, 30.0F, 30.0F);
            if (--repath > 0 && !pirate.getNavigation().isDone()) return;
            repath = 10;
            net.minecraft.world.phys.Vec3 away = net.minecraft.world.entity.ai.util.DefaultRandomPos.getPosAway(pirate, 10, 4, t.position());
            if (away != null) {
                pirate.getNavigation().moveTo(away.x, away.y, away.z, 1.35);
            } else {
                // Cornered: at least step straight back.
                net.minecraft.world.phys.Vec3 back = pirate.position().subtract(t.position()).normalize().scale(4.0).add(pirate.position());
                pirate.getMoveControl().setWantedPosition(back.x, pirate.getY(), back.z, 1.35);
            }
        }
    }

    /**
     * Fallback melee: when the normal melee goal can't find a path (water, a ledge, a gap, or the
     * target just outside a home area), walk straight at the target and swing when in reach, so
     * two pirates never end up just staring at each other.
     */
    public static class ChargeGoal extends Goal {
        private final PirateEntity pirate;
        private int attackCooldown;
        private int stuckTicks;
        private double lastDist;

        public ChargeGoal(PirateEntity pirate) {
            this.pirate = pirate;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean valid() {
            LivingEntity t = pirate.getTarget();
            return t != null && t.isAlive() && !pirate.isConsuming() && pirate.getRangedType() == PirateEntity.Ranged.NONE
                    && pirate.canRetreat() && pirate.distanceToSqr(t) < 24 * 24;
        }

        @Override
        public boolean canUse() {
            // Only steps in when nothing else is moving the pirate toward its target.
            return valid() && pirate.getNavigation().isDone();
        }

        @Override
        public boolean canContinueToUse() {
            return valid();
        }

        @Override
        public void start() {
            attackCooldown = 0;
            stuckTicks = 0;
            lastDist = Double.MAX_VALUE;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity t = pirate.getTarget();
            if (t == null) return;
            pirate.getLookControl().setLookAt(t, 30.0F, 30.0F);
            double dist = pirate.distanceToSqr(t);
            double reach = pirate.getBbWidth() * 2.0F * pirate.getBbWidth() * 2.0F + t.getBbWidth() + 1.0;
            if (attackCooldown > 0) attackCooldown--;
            if (dist <= reach) {
                pirate.getNavigation().stop();
                if (attackCooldown <= 0 && pirate.getSensing().hasLineOfSight(t)) {
                    pirate.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    pirate.doHurtTarget(t);
                    attackCooldown = 20;
                }
                return;
            }
            // Try a real path first, then just walk straight at it (jumping over small steps).
            if (!pirate.getNavigation().moveTo(t, 1.2)) {
                pirate.getMoveControl().setWantedPosition(t.getX(), t.getY(), t.getZ(), 1.2);
                if (pirate.horizontalCollision && pirate.onGround()) pirate.getJumpControl().jump();
            }
            // Getting nowhere for 3 seconds: draw a bow if there's one in the pack.
            stuckTicks = dist < lastDist - 0.05 ? 0 : stuckTicks + 1;
            lastDist = Math.min(lastDist, dist);
            if (stuckTicks > 60) {
                pirate.equipFromPack(PirateEntity::isRangedWeapon);
                stuckTicks = 0;
                lastDist = Double.MAX_VALUE;
            }
        }
    }
}
