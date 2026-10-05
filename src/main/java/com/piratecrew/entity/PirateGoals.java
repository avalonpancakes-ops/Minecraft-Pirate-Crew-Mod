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
            if (pirate.isUsingItem()) pirate.stopUsingItem();
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
                pirate.getNavigation().moveTo(target, 1.1);
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
}
