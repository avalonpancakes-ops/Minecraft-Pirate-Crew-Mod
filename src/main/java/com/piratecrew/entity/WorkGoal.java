package com.piratecrew.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A crew pirate's job: mining ore, farming, fishing or chopping wood near its work spot.
 * What it gathers goes into its pack; when the pack is full it empties it into the nearest
 * chest or barrel near the work spot. Fighting always takes priority over work.
 */
public class WorkGoal extends Goal {
    private static final double REACH = 4.5;

    private final PirateEntity pirate;
    @Nullable private BlockPos target;   // block to break / crop to harvest / water to fish in
    @Nullable private BlockPos stand;    // where to stand to fish
    @Nullable private BlockPos chest;
    private int progress, needed;
    private int stuck, searchCooldown, chestCooldown, fishTimer, actionCooldown, ticks;
    private final Set<BlockPos> skip = new HashSet<>();

    public WorkGoal(PirateEntity pirate) {
        this.pirate = pirate;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return pirate.isRecruited() && pirate.getOrders() == PirateEntity.Orders.WORK
                && pirate.getTask() != PirateTask.NONE && pirate.getWorkCenter() != null && pirate.getTarget() == null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        resetTarget();
        searchCooldown = 0;
    }

    @Override
    public void stop() {
        resetTarget();
        pirate.getNavigation().stop();
    }

    private ServerLevel level() {
        return (ServerLevel) pirate.level();
    }

    private void resetTarget() {
        if (target != null && progress > 0) level().destroyBlockProgress(pirate.getId(), target, -1);
        target = null;
        stand = null;
        progress = 0;
        needed = 0;
        stuck = 0;
    }

    private void giveUp() {
        if (target != null) skip.add(target.immutable());
        resetTarget();
    }

    @Override
    public void tick() {
        if (++ticks % 2400 == 0) skip.clear();
        if (actionCooldown > 0) actionCooldown--;

        if (pirate.packFull()) {
            resetTarget();
            tickDeposit();
            return;
        }
        switch (pirate.getTask()) {
            case MINE -> tickBreak(true);
            case WOOD -> tickBreak(false);
            case FARM -> tickFarm();
            case FISH -> tickFish();
            default -> {}
        }
    }

    // ------------------------------------------------------------------ movement helpers

    private void walkTo(BlockPos pos) {
        if (pirate.getNavigation().isDone() || ticks % 20 == 0) {
            Path path = pirate.getNavigation().createPath(pos, 1);
            if (path != null) pirate.getNavigation().moveTo(path, 1.0);
        }
        stuck++;
    }

    private double eyeDistSqr(BlockPos pos) {
        return pirate.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos));
    }

    private void lookAt(BlockPos pos) {
        pirate.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    // ------------------------------------------------------------------ mining & chopping

    private static boolean isOre(BlockState s) {
        return s.is(Tags.Blocks.ORES);
    }

    private static boolean isLog(BlockState s) {
        return s.is(BlockTags.LOGS);
    }

    private boolean exposed(BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockState n = level().getBlockState(pos.relative(d));
            if (n.isAir() || !n.getFluidState().isEmpty()) return true;
        }
        return false;
    }

    private boolean validBreakTarget(BlockPos pos, boolean mining) {
        BlockState s = level().getBlockState(pos);
        return mining ? isOre(s) && exposed(pos) : isLog(s);
    }

    @Nullable
    private BlockPos findBreakTarget(boolean mining) {
        BlockPos c = pirate.getWorkCenter();
        if (c == null) return null;
        int r = PirateEntity.WORK_RADIUS;
        int yMin = mining ? -8 : -3, yMax = mining ? 8 : 12;
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, yMin, -r), c.offset(r, yMax, r))) {
            if (skip.contains(p) || !validBreakTarget(p, mining)) continue;
            double score = pirate.distanceToSqr(Vec3.atCenterOf(p));
            if (!mining) score += Math.max(0, p.getY() - pirate.getY() - 2) * 40; // trunk first
            if (score < bestScore) {
                bestScore = score;
                best = p.immutable();
            }
        }
        return best;
    }

    private void tickBreak(boolean mining) {
        if (target == null || !validBreakTarget(target, mining)) {
            resetTarget();
            if (--searchCooldown > 0) return;
            searchCooldown = 40;
            target = findBreakTarget(mining);
            if (target == null) {
                pirate.say(mining ? "No ore I can get at round here. Try a cave or a cliff." : "No trees I can reach round here.");
                return;
            }
        }

        BlockState state = level().getBlockState(target);
        if (mining) {
            if (!pirate.equipFromPack(s -> s.getItem() instanceof PickaxeItem && s.isCorrectToolForDrops(state))) {
                boolean anyPick = pirate.hasInHandOrPack(s -> s.getItem() instanceof PickaxeItem);
                pirate.say(anyPick ? "Me pickaxe can't break that ore. I need a better one." : "I need a pickaxe to mine, Cap'n.");
                giveUp();
                return;
            }
        } else {
            pirate.equipFromPack(s -> s.getItem() instanceof AxeItem);
        }

        if (eyeDistSqr(target) > REACH * REACH) {
            walkTo(target);
            if (stuck > 200) giveUp();
            return;
        }
        pirate.getNavigation().stop();
        lookAt(target);

        ItemStack tool = pirate.getMainHandItem();
        if (needed == 0) {
            float hardness = state.getDestroySpeed(level(), target);
            if (hardness < 0) { giveUp(); return; } // unbreakable
            float speed = Math.max(1.0F, tool.getDestroySpeed(state));
            int eff = EnchantmentHelper.getBlockEfficiency(pirate);
            if (eff > 0 && speed > 1.0F) speed += eff * eff + 1;
            boolean correct = !state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state);
            needed = Math.max(4, Math.min(400, (int) Math.ceil(hardness * (correct ? 30 : 100) / speed)));
        }
        progress++;
        if (progress % 5 == 1) pirate.swing(InteractionHand.MAIN_HAND);
        level().destroyBlockProgress(pirate.getId(), target, Math.min(9, progress * 10 / needed));
        if (progress >= needed) {
            BlockPos pos = target;
            level().destroyBlockProgress(pirate.getId(), pos, -1);
            List<ItemStack> drops = Block.getDrops(state, level(), pos, level().getBlockEntity(pos), pirate, tool);
            level().destroyBlock(pos, false, pirate);
            for (ItemStack d : drops) pirate.addToPack(d);
            if (tool.isDamageableItem()) tool.hurtAndBreak(1, pirate, e -> e.broadcastBreakEvent(InteractionHand.MAIN_HAND));
            if (!mining) replantSapling(pos);
            target = null;
            progress = 0;
            needed = 0;
            stuck = 0;
        }
    }

    private void replantSapling(BlockPos pos) {
        if (!level().getBlockState(pos).isAir() || !level().getBlockState(pos.below()).is(BlockTags.DIRT)) return;
        SimpleContainerView pack = new SimpleContainerView(pirate);
        for (int i = 0; i < pack.size(); i++) {
            ItemStack s = pack.get(i);
            if (s.getItem() instanceof BlockItem bi && bi.getBlock() instanceof SaplingBlock) {
                BlockState sapling = bi.getBlock().defaultBlockState();
                if (sapling.canSurvive(level(), pos)) {
                    level().setBlock(pos, sapling, 3);
                    s.shrink(1);
                    return;
                }
            }
        }
    }

    // ------------------------------------------------------------------ farming

    private boolean ripe(BlockPos pos) {
        BlockState s = level().getBlockState(pos);
        return s.getBlock() instanceof CropBlock crop && crop.isMaxAge(s);
    }

    @Nullable
    private BlockPos findRipeCrop() {
        BlockPos c = pirate.getWorkCenter();
        if (c == null) return null;
        int r = PirateEntity.WORK_RADIUS;
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -3, -r), c.offset(r, 3, r))) {
            if (skip.contains(p) || !ripe(p)) continue;
            double d = pirate.distanceToSqr(Vec3.atCenterOf(p));
            if (d < bestDist) {
                bestDist = d;
                best = p.immutable();
            }
        }
        return best;
    }

    private void tickFarm() {
        if (target == null || !ripe(target)) {
            resetTarget();
            if (--searchCooldown > 0) return;
            searchCooldown = 40;
            target = findRipeCrop();
            if (target == null) {
                pirate.say("No ripe crops yet. I'll keep watch on 'em.");
                return;
            }
        }
        if (pirate.distanceToSqr(Vec3.atBottomCenterOf(target)) > 2.5 * 2.5) {
            walkTo(target);
            if (stuck > 200) giveUp();
            return;
        }
        pirate.getNavigation().stop();
        lookAt(target);
        if (actionCooldown > 0) return;
        actionCooldown = 8;

        BlockState state = level().getBlockState(target);
        CropBlock crop = (CropBlock) state.getBlock();
        ItemStack seed = crop.getCloneItemStack(level(), target, state);
        List<ItemStack> drops = Block.getDrops(state, level(), target, null, pirate, pirate.getMainHandItem());
        pirate.swing(InteractionHand.MAIN_HAND);
        level().destroyBlock(target, false, pirate);
        for (ItemStack d : drops) pirate.addToPack(d);
        // Replant with a seed from the pack
        SimpleContainerView pack = new SimpleContainerView(pirate);
        for (int i = 0; i < pack.size(); i++) {
            ItemStack s = pack.get(i);
            if (!seed.isEmpty() && s.is(seed.getItem())) {
                BlockState planted = crop.defaultBlockState();
                if (planted.canSurvive(level(), target)) {
                    level().setBlock(target, planted, 3);
                    s.shrink(1);
                }
                break;
            }
        }
        target = null;
        stuck = 0;
    }

    // ------------------------------------------------------------------ fishing

    private boolean fishable(BlockPos water) {
        return level().getFluidState(water).is(Fluids.WATER) && level().getFluidState(water).isSource()
                && level().getBlockState(water.above()).isAir();
    }

    private boolean standable(BlockPos feet) {
        return level().getBlockState(feet.below()).isFaceSturdy(level(), feet.below(), Direction.UP)
                && level().getBlockState(feet).getCollisionShape(level(), feet).isEmpty()
                && level().getFluidState(feet).isEmpty()
                && level().getBlockState(feet.above()).getCollisionShape(level(), feet.above()).isEmpty();
    }

    private boolean findFishingSpot() {
        BlockPos c = pirate.getWorkCenter();
        if (c == null) return false;
        int r = PirateEntity.WORK_RADIUS;
        double bestDist = Double.MAX_VALUE;
        BlockPos bestWater = null, bestStand = null;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -4, -r), c.offset(r, 2, r))) {
            if (skip.contains(p) || !fishable(p)) continue;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos feet = p.relative(d).above();
                if (!standable(feet)) {
                    feet = p.relative(d);
                    if (!standable(feet)) continue;
                }
                double dist = pirate.distanceToSqr(Vec3.atBottomCenterOf(feet));
                if (dist < bestDist) {
                    bestDist = dist;
                    bestWater = p.immutable();
                    bestStand = feet.immutable();
                }
            }
        }
        target = bestWater;
        stand = bestStand;
        return target != null;
    }

    private void tickFish() {
        if (!pirate.equipFromPack(s -> s.getItem() instanceof FishingRodItem)) {
            pirate.say("I need a fishing rod for that, Cap'n.");
            return;
        }
        if (target == null || stand == null || !fishable(target)) {
            resetTarget();
            if (--searchCooldown > 0) return;
            searchCooldown = 40;
            if (!findFishingSpot()) {
                pirate.say("There's no water to fish in near here.");
                return;
            }
            fishTimer = 0;
        }
        if (pirate.distanceToSqr(Vec3.atBottomCenterOf(stand)) > 1.6 * 1.6) {
            walkTo(stand);
            if (stuck > 300) giveUp();
            return;
        }
        pirate.getNavigation().stop();
        lookAt(target);

        ItemStack rod = pirate.getMainHandItem();
        if (fishTimer <= 0) {
            int lure = EnchantmentHelper.getFishingSpeedBonus(rod);
            fishTimer = Math.max(60, 300 + pirate.getRandom().nextInt(500) - lure * 100);
            pirate.swing(InteractionHand.MAIN_HAND);
            level().playSound(null, pirate.blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F);
            return;
        }
        if (--fishTimer > 0) {
            if (fishTimer % 40 == 0) level().sendParticles(ParticleTypes.FISHING, target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5, 2, 0.1, 0, 0.1, 0);
            return;
        }
        // A bite!
        int luck = EnchantmentHelper.getFishingLuckBonus(rod);
        LootParams params = new LootParams.Builder(level())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(target))
                .withParameter(LootContextParams.TOOL, rod)
                .withLuck(luck)
                .create(LootContextParamSets.FISHING);
        LootTable table = level().getServer().getLootData().getLootTable(BuiltInLootTables.FISHING);
        for (ItemStack loot : table.getRandomItems(params)) pirate.addToPack(loot);
        level().sendParticles(ParticleTypes.SPLASH, target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5, 12, 0.3, 0.1, 0.3, 0);
        level().playSound(null, target, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.6F, 1.0F);
        pirate.swing(InteractionHand.MAIN_HAND);
        if (rod.isDamageableItem()) rod.hurtAndBreak(1, pirate, e -> e.broadcastBreakEvent(InteractionHand.MAIN_HAND));
        fishTimer = 0;
    }

    // ------------------------------------------------------------------ emptying the pack

    /** Weapons, tools, armour and ammo stay with the pirate. */
    private static boolean isGear(ItemStack s) {
        Item i = s.getItem();
        return i instanceof TieredItem || i instanceof ProjectileWeaponItem || i instanceof TridentItem
                || i instanceof FishingRodItem || i instanceof ShearsItem || i instanceof ArmorItem
                || i instanceof ShieldItem || i instanceof ArrowItem;
    }

    private static boolean isPlantable(ItemStack s) {
        return s.getItem() instanceof BlockItem bi && (bi.getBlock() instanceof CropBlock || bi.getBlock() instanceof SaplingBlock);
    }

    @Nullable
    private BlockPos findChest() {
        BlockPos c = pirate.getWorkCenter();
        if (c == null) return null;
        int r = PirateEntity.WORK_RADIUS + 4;
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -4, -r), c.offset(r, 4, r))) {
            BlockEntity be = level().getBlockEntity(p);
            if (!(be instanceof Container)) continue;
            BlockState s = level().getBlockState(p);
            if (!(s.is(Tags.Blocks.CHESTS) || s.is(Tags.Blocks.BARRELS))) continue;
            double d = pirate.distanceToSqr(Vec3.atCenterOf(p));
            if (d < bestDist) {
                bestDist = d;
                best = p.immutable();
            }
        }
        return best;
    }

    private void tickDeposit() {
        if (chest == null || !(level().getBlockEntity(chest) instanceof Container)) {
            chest = null;
            if (--chestCooldown > 0) return;
            chestCooldown = 60;
            chest = findChest();
            if (chest == null) {
                pirate.say("Me pack's full! Put a chest or barrel near me work spot, or take what I've got.");
                return;
            }
        }
        if (pirate.distanceToSqr(Vec3.atCenterOf(chest)) > 2.8 * 2.8) {
            walkTo(chest);
            if (stuck > 300) { chest = null; stuck = 0; }
            return;
        }
        pirate.getNavigation().stop();
        lookAt(chest);
        if (actionCooldown > 0) return;
        actionCooldown = 10;

        Container box = (Container) level().getBlockEntity(chest);
        Map<Item, Integer> kept = new HashMap<>();
        boolean movedAny = false;
        for (int i = 0; i < pirate.getPack().getContainerSize(); i++) {
            ItemStack s = pirate.getPack().getItem(i);
            if (s.isEmpty() || isGear(s)) continue;
            if (isPlantable(s)) {
                // keep up to 16 seeds/saplings of each kind for replanting
                int have = kept.getOrDefault(s.getItem(), 0);
                if (have < 16) {
                    int keep = Math.min(16 - have, s.getCount());
                    kept.put(s.getItem(), have + keep);
                    if (keep == s.getCount()) continue;
                    ItemStack extra = s.split(s.getCount() - keep);
                    ItemStack left = insert(box, extra);
                    if (left.getCount() != extra.getCount()) movedAny = true;
                    s.grow(left.getCount());
                    continue;
                }
            }
            int before = s.getCount();
            ItemStack left = insert(box, s.copy());
            if (left.getCount() != before) movedAny = true;
            pirate.getPack().setItem(i, left);
        }
        box.setChanged();
        if (movedAny) {
            pirate.swing(InteractionHand.MAIN_HAND);
            level().playSound(null, chest, SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 0.5F, 1.1F);
        }
        if (pirate.packFull()) {
            pirate.say("That chest's full too, Cap'n. I've nowhere to put me haul.");
            chest = null;
            chestCooldown = 200;
        }
    }

    private static ItemStack insert(Container box, ItemStack stack) {
        for (int i = 0; i < box.getContainerSize() && !stack.isEmpty(); i++) {
            ItemStack s = box.getItem(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameTags(s, stack) && s.getCount() < s.getMaxStackSize() && box.canPlaceItem(i, stack)) {
                int move = Math.min(stack.getCount(), s.getMaxStackSize() - s.getCount());
                s.grow(move);
                stack.shrink(move);
            }
        }
        for (int i = 0; i < box.getContainerSize() && !stack.isEmpty(); i++) {
            if (box.getItem(i).isEmpty() && box.canPlaceItem(i, stack)) {
                box.setItem(i, stack.copy());
                stack.setCount(0);
            }
        }
        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    /** Tiny helper for walking the pirate's pack. */
    private record SimpleContainerView(PirateEntity pirate) {
        int size() {
            return pirate.getPack().getContainerSize();
        }

        ItemStack get(int i) {
            return pirate.getPack().getItem(i);
        }
    }
}
