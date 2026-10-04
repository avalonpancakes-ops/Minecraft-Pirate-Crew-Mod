package com.piratecrew.entity;

import com.piratecrew.Config;
import com.piratecrew.bank.LoanManager;
import com.piratecrew.registry.ModItems;
import com.piratecrew.skin.PirateSkins;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import com.piratecrew.PirateCrew;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Vector3f;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Sent by the bank after a player who didn't repay a loan. Fights like a pirate (same weapons,
 * styles and shield use) but with five times the health and damage of a pirate of its tier.
 * Can't be recruited, only hunts its debtor (and whoever attacks it), and goes home when the debt is
 * paid, a newer wave replaces it, or the debtor leaves.
 */
public class BountyHunterEntity extends PirateEntity {
    @Nullable private UUID debtorId;
    private int waveSerial;
    /** Spawned by an op command: hunts until the debtor leaves, ignores loans. */
    private boolean test;
    private int debtorMissing;
    private int unseenTicks;
    private boolean greeted;
    /** A pirate summoned by an A-tier hunter rather than a hunter itself (pirate stats, diamond gear). */
    private boolean minion;
    @Nullable private UUID summonerId;
    /** Ticks until an A-tier hunter can summon his crew again. */
    private int summonCooldown;
    public static final int SUMMON_COOLDOWN = 6000; // 5 minutes
    public static final int SUMMON_COUNT = 4;
    /** The corpse this hunter is walking to / searching after killing its debtor. */
    @Nullable private UUID lootCorpse;
    private int lootTicks;
    private int lootTravel;

    private static final String[] FIRST = {
            "Vex", "Morrow", "Silas", "Grell", "Kade", "Thorne", "Ruthven", "Mordecai", "Jago", "Corvin",
            "Hask", "Brannoc", "Severin", "Draven", "Isolde", "Mag", "Nyx", "Ragna", "Sable", "Wren",
            "Osric", "Varga", "Lucan", "Dace", "Ebon", "Fenwick", "Grimsby", "Hollis", "Ivo", "Jory",
            "Kestrel", "Lorne", "Malachai", "Nash", "Orla", "Quill", "Roan", "Slade", "Tamsin", "Ulric"};
    private static final String[] TITLE = {
            "the Collector", "the Debt-Taker", "Coin-Hound", "the Bloodhound", "the Tracker", "No-Mercy",
            "the Reaper", "the Ledger", "Blackmark", "the Taxman", "Iron-Purse", "the Repossessor",
            "Red-Ink", "the Foreclosure", "Cold-Hand", "the Bailiff", "Last-Notice", "the Usurer's Blade",
            "Grimtally", "the Settler", "Dead-Debt", "the Interest", "Nightcall", "the Lien"};

    public BountyHunterEntity(EntityType<? extends BountyHunterEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PirateGoals.RangedWeaponGoal(this));
        this.goalSelector.addGoal(1, new PirateGoals.PirateMeleeGoal(this, 1.25));
        this.goalSelector.addGoal(4, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected boolean infiniteConsumables() {
        return true;
    }

    @Override
    protected int throwCooldown() {
        return 140;
    }

    @Override
    protected double statMultiplier() {
        if (minion) return 1.0;
        try {
            return Config.HUNTER_STRENGTH.get();
        } catch (Exception e) {
            return 5.0;
        }
    }

    @Override
    protected String rollSkin(PirateTier tier) {
        return minion ? PirateSkins.random(this.random, tier) : PirateSkins.randomHunter(this.random, tier);
    }

    @Override
    protected String rollName() {
        if (minion) return PirateNames.random(this.random);
        return FIRST[this.random.nextInt(FIRST.length)] + " " + TITLE[this.random.nextInt(TITLE.length)];
    }

    @Override
    public void updateDisplayName() {
        PirateTier tier = getTier();
        MutableComponent name = Component.literal("[" + tier.label + "] ").withStyle(tier.color, ChatFormatting.BOLD);
        name.append(Component.literal("☠ " + pirateName).withStyle(s -> s.withBold(false).withColor(minion ? ChatFormatting.DARK_RED : ChatFormatting.RED)));
        this.setCustomName(name);
        this.setCustomNameVisible(true);
    }

    // ------------------------------------------------------------------ setup

    /** Called before the hunter is added to the world. */
    public void setupHunter(PirateTier tier, UUID debtor, int serial) {
        this.debtorId = debtor;
        this.waveSerial = serial;
        initPirate(tier);
        var follow = this.getAttribute(Attributes.FOLLOW_RANGE);
        if (follow != null) follow.setBaseValue(64.0);
        var kb = this.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb != null) kb.setBaseValue(0.1 * tier.ordinal());
        this.xpReward = 0; // a punishment, not a mob to farm: no XP, no drops
        equip(tier);
    }

    public void setTest(boolean test) {
        this.test = test;
    }

    /** A pirate summoned by an A-tier hunter: normal pirate stats, full diamond gear, hunts the same debtor. */
    public void setupMinion(BountyHunterEntity leader) {
        this.minion = true;
        this.summonerId = leader.getUUID();
        this.debtorId = leader.debtorId;
        this.waveSerial = leader.waveSerial;
        this.test = leader.test;
        this.greeted = true;
        initPirate(PirateTier.random(this.random));
        var follow = this.getAttribute(Attributes.FOLLOW_RANGE);
        if (follow != null) follow.setBaseValue(64.0);
        this.xpReward = 0;
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        boolean marksman = getCombatStyle() == CombatStyle.MARKSMAN;
        setItemSlot(EquipmentSlot.MAINHAND, gear(marksman ? Items.BOW : Items.DIAMOND_SWORD));
        getPack().setItem(0, gear(marksman ? Items.DIAMOND_SWORD : Items.BOW));
    }

    public boolean isMinion() {
        return minion;
    }

    /**
     * Hunters wear no armor (so their skins show) but get the armor points of a full set for their
     * tier built into their stats. All their gear is unbreakable and none of it ever drops.
     */
    private void equip(PirateTier tier) {
        // Armor and toughness of a full set: leather, chainmail, iron, ruby, diamond, netherite.
        double[] armor = {7, 12, 15, 18, 20, 20};
        double[] toughness = {0, 0, 0, 1, 8, 12};
        int t = tier.ordinal();
        var a = this.getAttribute(Attributes.ARMOR);
        if (a != null) a.setBaseValue(armor[t]);
        var tough = this.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (tough != null) tough.setBaseValue(toughness[t]);
        if (tier == PirateTier.S) {
            var kb = this.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (kb != null) kb.setBaseValue(Math.max(kb.getBaseValue(), 0.4)); // like full netherite
        }

        Item sword = switch (tier) {
            case F -> Items.WOODEN_SWORD;
            case D -> Items.STONE_SWORD;
            case C, B -> ModItems.RUBY_SWORD.get();
            case A -> Items.DIAMOND_SWORD;
            default -> Items.NETHERITE_SWORD;
        };

        if (tier.ordinal() <= PirateTier.B.ordinal()) {
            // Sword (wooden at F, stone at D, ruby at C, ruby with Sharpness II at B), bow and shield:
            // bow in hand for marksmen, sword for everyone else.
            ItemStack blade = gear(sword);
            if (tier == PirateTier.B) blade.enchant(Enchantments.SHARPNESS, 2);
            ItemStack bow = gear(Items.BOW);
            boolean marksman = getCombatStyle() == CombatStyle.MARKSMAN;
            setItemSlot(EquipmentSlot.MAINHAND, marksman ? bow : blade);
            getPack().setItem(0, marksman ? blade : bow);
            setItemSlot(EquipmentSlot.OFFHAND, gear(Items.SHIELD));
            if (tier.ordinal() >= PirateTier.C.ordinal()) addPotions(false);
            return;
        }

        if (tier == PirateTier.A) {
            // Prefers range: a fully enchanted bow, a Sharpness II diamond sword only when cornered.
            setCombatStyle(CombatStyle.MARKSMAN);
            ItemStack bow = gear(Items.BOW);
            bow.enchant(Enchantments.POWER_ARROWS, 5);
            bow.enchant(Enchantments.PUNCH_ARROWS, 2);
            bow.enchant(Enchantments.FLAMING_ARROWS, 1);
            bow.enchant(Enchantments.INFINITY_ARROWS, 1);
            bow.enchant(Enchantments.UNBREAKING, 3);
            ItemStack blade = gear(sword);
            blade.enchant(Enchantments.SHARPNESS, 2);
            setItemSlot(EquipmentSlot.MAINHAND, bow);
            getPack().setItem(0, blade);
            setItemSlot(EquipmentSlot.OFFHAND, gear(Items.SHIELD));
            addPotions(true);
            applyPermanentEffects();
            summonCooldown = 0;
            return;
        }

        // S tier: charges in with a fully enchanted netherite sword, fires eye lasers at range,
        // and is permanently buffed to the max.
        setCombatStyle(CombatStyle.BRAWLER);
        ItemStack blade = gear(sword);
        blade.enchant(Enchantments.SHARPNESS, 5);
        blade.enchant(Enchantments.FIRE_ASPECT, 2);
        blade.enchant(Enchantments.KNOCKBACK, 2);
        blade.enchant(Enchantments.SWEEPING_EDGE, 3);
        blade.enchant(Enchantments.UNBREAKING, 3);
        setItemSlot(EquipmentSlot.MAINHAND, blade);
        setItemSlot(EquipmentSlot.OFFHAND, gear(Items.SHIELD));
        addPotions(false);
        applyPermanentEffects();
    }

    private void permanent(net.minecraft.world.effect.MobEffect effect, int amplifier) {
        MobEffectInstance cur = this.getEffect(effect);
        if (cur == null || cur.getAmplifier() < amplifier || !cur.isInfiniteDuration()) {
            this.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, amplifier, false, true));
        }
    }

    /** A tier: permanent Strength. S tier: Strength II, Speed II, Resistance II, Regeneration II, Fire Resistance, Water Breathing. */
    private void applyPermanentEffects() {
        if (minion) return;
        if (getTier() == PirateTier.A) {
            permanent(MobEffects.DAMAGE_BOOST, 0);
        } else if (getTier() == PirateTier.S) {
            permanent(MobEffects.DAMAGE_BOOST, 1);
            permanent(MobEffects.MOVEMENT_SPEED, 1);
            permanent(MobEffects.DAMAGE_RESISTANCE, 1);
            permanent(MobEffects.REGENERATION, 1);
            permanent(MobEffects.FIRE_RESISTANCE, 0);
            permanent(MobEffects.WATER_BREATHING, 0);
        }
    }

    // ------------------------------------------------------------------ S tier: eye lasers

    private static final int LASER_CHARGE = 30, LASER_BEAM = 8, LASER_COOLDOWN = 160;
    private static final float LASER_DAMAGE = 16.0F;
    private static final ResourceKey<DamageType> LASER = ResourceKey.create(Registries.DAMAGE_TYPE, PirateCrew.id("laser"));
    private int laserCooldown = 60;
    private int laserCharge = -1;
    @Nullable private Vec3 laserAim;
    @Nullable private Vec3 beamEnd;

    private boolean isLaserUser() {
        return !minion && getTier() == PirateTier.S;
    }

    private Vec3[] eyes() {
        Vec3 look = Vec3.directionFromRotation(this.getXRot(), this.getYHeadRot());
        Vec3 side = look.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 c = this.getEyePosition().add(look.scale(0.3));
        return new Vec3[]{c.add(side.scale(0.13)), c.add(side.scale(-0.13))};
    }

    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(1.0F, 0.05F, 0.05F), 1.4F);

    /**
     * Eyes glow red for a moment (tracking the target), lock on just before firing, then a beam hits
     * the first thing in its path for heavy damage that ignores armor and sets it alight. Walls and
     * shields stop it; dodging at the last moment works.
     */
    private void tickLaser(ServerLevel sl) {
        if (!isLaserUser()) return;
        if (laserCooldown > 0) laserCooldown--;
        LivingEntity t = getTarget();
        if (laserCharge < 0) {
            if (laserCooldown == 0 && t != null && t.isAlive() && !isUsingItem()) {
                double d = this.distanceTo(t);
                if (d >= 4 && d <= 32 && this.getSensing().hasLineOfSight(t)) {
                    laserCharge = 0;
                    this.playSound(SoundEvents.GUARDIAN_ATTACK, 2.0F, 0.6F);
                }
            }
            return;
        }
        laserCharge++;
        if (laserCharge <= LASER_CHARGE) {
            if (t == null || !t.isAlive()) {
                laserCharge = -1;
                laserCooldown = 40;
                return;
            }
            this.getLookControl().setLookAt(t, 90.0F, 90.0F);
            if (laserCharge <= LASER_CHARGE - 8) laserAim = t.getBoundingBox().getCenter();
            if (laserCharge % 2 == 0) for (Vec3 e : eyes()) sl.sendParticles(RED, e.x, e.y, e.z, 2, 0.02, 0.02, 0.02, 0);
            if (laserCharge == LASER_CHARGE && laserAim != null) fireLaser(sl);
            return;
        }
        if (beamEnd != null && laserCharge <= LASER_CHARGE + LASER_BEAM) {
            for (Vec3 e : eyes()) drawBeam(sl, e, beamEnd);
            return;
        }
        laserCharge = -1;
        beamEnd = null;
        laserCooldown = LASER_COOLDOWN;
    }

    private void fireLaser(ServerLevel sl) {
        Vec3 eye = this.getEyePosition();
        Vec3 dir = laserAim.subtract(eye).normalize();
        Vec3 end = eye.add(dir.scale(40));
        BlockHitResult block = sl.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(sl, this, eye, end, new AABB(eye, end).inflate(1.0),
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e != this && !(e instanceof BountyHunterEntity));
        if (hit != null && hit.getEntity() instanceof LivingEntity victim) {
            end = victim.getBoundingBox().getCenter();
            victim.hurt(laserDamage(sl), LASER_DAMAGE);
            victim.setSecondsOnFire(4);
        }
        beamEnd = end;
        this.playSound(SoundEvents.FIRECHARGE_USE, 2.0F, 0.5F);
        sl.sendParticles(ParticleTypes.LAVA, end.x, end.y, end.z, 6, 0.2, 0.2, 0.2, 0);
        sl.sendParticles(ParticleTypes.SMOKE, end.x, end.y, end.z, 10, 0.2, 0.2, 0.2, 0.02);
        for (Vec3 e : eyes()) drawBeam(sl, e, end);
    }

    private void drawBeam(ServerLevel sl, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        int steps = (int) Math.min(160, len * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(d.scale(i / (double) Math.max(1, steps)));
            sl.sendParticles(RED, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    private DamageSource laserDamage(ServerLevel sl) {
        var reg = sl.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        var holder = reg.getHolder(LASER);
        return holder.isPresent() ? new DamageSource(holder.get(), this) : this.damageSources().indirectMagic(this, this);
    }

    private boolean isSummoner() {
        return !minion && getTier() == PirateTier.A;
    }

    /** Call in a crew of pirates in diamond gear to help. */
    private void summonCrew(ServerLevel sl, ServerPlayer debtor) {
        int alive = sl.getEntitiesOfClass(BountyHunterEntity.class, this.getBoundingBox().inflate(96),
                h -> h.isAlive() && h.minion && getUUID().equals(h.summonerId)).size();
        int count = Math.min(SUMMON_COUNT, 2 * SUMMON_COUNT - alive);
        if (count <= 0) return;
        int made = 0;
        for (int i = 0; i < count; i++) {
            Vec3 spot = LoanManager.findSpot(sl, this.blockPosition(), 2, 5, this.random);
            if (spot == null) continue;
            BountyHunterEntity m = com.piratecrew.registry.ModEntities.BOUNTY_HUNTER.get().create(sl);
            if (m == null) continue;
            m.moveTo(spot.x, spot.y, spot.z, this.getYRot(), 0);
            m.setupMinion(this);
            m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(m.blockPosition()), net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED, null, null);
            m.setTarget(debtor);
            sl.addFreshEntity(m);
            sl.sendParticles(ParticleTypes.CLOUD, spot.x, spot.y + 1.0, spot.z, 12, 0.3, 0.6, 0.3, 0.02);
            made++;
        }
        if (made == 0) return;
        summonCooldown = SUMMON_COOLDOWN;
        this.swing(InteractionHand.MAIN_HAND);
        this.playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.5F, 0.9F);
        debtor.sendSystemMessage(Component.literal(pirateName + ": ").withStyle(ChatFormatting.DARK_RED)
                .append(Component.literal("Lads! The bank pays double for this one. Take 'em!").withStyle(ChatFormatting.RED)));
    }

    /** Poison splash potions to throw and milk to purge debuffs (plus fire resistance from A tier). Never run out. */
    private void addPotions(boolean fireResistance) {
        getPack().setItem(1, PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.POISON));
        getPack().setItem(2, new ItemStack(Items.MILK_BUCKET));
        if (fireResistance) getPack().setItem(3, PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.FIRE_RESISTANCE));
    }

    private static ItemStack gear(Item item) {
        ItemStack s = new ItemStack(item);
        s.getOrCreateTag().putBoolean("Unbreakable", true);
        return s;
    }

    // ------------------------------------------------------------------ getters

    @Nullable
    public UUID getDebtor() {
        return debtorId;
    }

    public int getWaveSerial() {
        return waveSerial;
    }

    public boolean isTestHunter() {
        return test;
    }

    public boolean isHunting(Player p) {
        return p.getUUID().equals(debtorId);
    }

    // ------------------------------------------------------------------ behaviour

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel sl)) return;
        if (lootCorpse != null) {
            if (this.tickCount % 10 == 0) tickLooting(sl);
            return;
        }
        tickLaser(sl);
        if (this.tickCount % 20 != 0) return;

        if (debtorId == null || (!test && !LoanManager.huntActive(sl.getServer(), debtorId, waveSerial))) {
            vanish();
            return;
        }
        ServerPlayer debtor = sl.getServer().getPlayerList().getPlayer(debtorId);
        if (debtor == null || debtor.level() != this.level()) {
            // Debtor logged off or left the dimension: wait a little, then give up for today.
            if (++debtorMissing > 30) vanish();
            return;
        }
        debtorMissing = 0;
        applyPermanentEffects();
        if (isSummoner()) {
            if (summonCooldown > 0) summonCooldown -= 20;
            if (summonCooldown <= 0 && getTarget() == debtor && this.distanceToSqr(debtor) < 24 * 24
                    && this.getSensing().hasLineOfSight(debtor) && !debtor.isCreative()) {
                summonCrew(sl, debtor);
            }
        }
        if (!greeted && this.distanceToSqr(debtor) < 40 * 40) {
            greeted = true;
            var loan = LoanManager.loanOf(sl.getServer(), debtorId);
            String line = loan != null
                    ? String.format("%s. The bank wants its %,d rubies, and I'm here to collect.", debtor.getGameProfile().getName(), loan.owed)
                    : "Nothing personal. Just business.";
            debtor.sendSystemMessage(Component.literal(pirateName + ": ").withStyle(ChatFormatting.DARK_RED)
                    .append(Component.literal(line).withStyle(ChatFormatting.RED)));
        }

        LivingEntity target = getTarget();
        if ((target == null || !target.isAlive() || target.distanceToSqr(this) > 32 * 32)
                && debtor.isAlive() && !debtor.isSpectator() && !debtor.isCreative()) {
            setTarget(debtor);
        }

        // Lost the trail: catch up out of sight like a real tracker.
        boolean sees = this.getSensing().hasLineOfSight(debtor);
        unseenTicks = sees ? 0 : unseenTicks + 20;
        double d = this.distanceToSqr(debtor);
        if (d > 56 * 56 || (d > 28 * 28 && unseenTicks > 200)) {
            Vec3 spot = LoanManager.findSpot(sl, debtor.blockPosition(), 12, 20, this.random);
            if (spot != null) {
                this.teleportTo(spot.x, spot.y, spot.z);
                this.getNavigation().stop();
                unseenTicks = 0;
            }
        }
    }

    /** Walk over to the debtor's corpse and search it for valuables. */
    public void startLooting(CorpseEntity corpse) {
        this.lootCorpse = corpse.getUUID();
        this.lootTicks = 0;
        this.lootTravel = 0;
        this.setTarget(null);
        this.laserCharge = -1;
        if (isUsingItem()) stopUsingItem();
    }

    private void tickLooting(ServerLevel sl) {
        Entity e = sl.getEntity(lootCorpse);
        if (!(e instanceof CorpseEntity corpse) || !corpse.isAlive() || !corpse.isLockedBy(getUUID())) {
            lootCorpse = null;
            this.setPose(net.minecraft.world.entity.Pose.STANDING);
            return;
        }
        this.setTarget(null);
        double d = this.distanceToSqr(corpse);
        if (d > 2.2 * 2.2) {
            this.setPose(net.minecraft.world.entity.Pose.STANDING);
            lootTravel += 10;
            if (d > 48 * 48 || lootTravel > 600) {
                Vec3 spot = LoanManager.findSpot(sl, corpse.blockPosition(), 1, 3, this.random);
                if (spot != null) this.teleportTo(spot.x, spot.y, spot.z);
                lootTravel = 0;
            } else {
                this.getNavigation().moveTo(corpse.getX(), corpse.getY(), corpse.getZ(), 1.2);
            }
            return;
        }
        // Crouch over the body and go through the pockets for a few seconds.
        this.getNavigation().stop();
        this.getLookControl().setLookAt(corpse.getX(), corpse.getY(), corpse.getZ());
        this.setPose(net.minecraft.world.entity.Pose.CROUCHING);
        lootTicks += 10;
        if (lootTicks % 20 == 0) {
            this.swing(InteractionHand.MAIN_HAND);
            this.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 1.0F, 0.8F + this.random.nextFloat() * 0.3F);
        }
        if (lootTicks >= 80) {
            this.setPose(net.minecraft.world.entity.Pose.STANDING);
            lootCorpse = null;
            LoanManager.seizeFromCorpse(corpse, this);
            vanish();
        }
    }

    /** Leave in a puff of smoke (debt paid, wave replaced, or debtor gone). */
    private void vanish() {
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 20, 0.3, 0.6, 0.3, 0.02);
        }
        this.playSound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 0.8F);
        this.discard();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (this.level().isClientSide) return InteractionResult.SUCCESS;
        String line = isHunting(player) ? "Pay the bank or pay with your life." : "Not you. Stay out of my way.";
        player.sendSystemMessage(Component.literal(pirateName + ": ").withStyle(ChatFormatting.DARK_RED)
                .append(Component.literal(line).withStyle(ChatFormatting.RED)));
        return InteractionResult.CONSUME;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide) LoanManager.onHunterDeath(this);
    }

    /** Hunters are a punishment, not loot: they drop nothing at all. */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean hitByPlayer) {
    }

    @Override
    protected void dropFromLootTable(DamageSource source, boolean hitByPlayer) {
    }

    @Override
    public int getExperienceReward() {
        return 0;
    }

    // ------------------------------------------------------------------ save / load

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (debtorId != null) tag.putUUID("Debtor", debtorId);
        tag.putInt("WaveSerial", waveSerial);
        tag.putBoolean("TestHunter", test);
        tag.putBoolean("Greeted", greeted);
        tag.putBoolean("Minion", minion);
        if (summonerId != null) tag.putUUID("Summoner", summonerId);
        tag.putInt("SummonCooldown", summonCooldown);
        if (lootCorpse != null) tag.putUUID("LootCorpse", lootCorpse);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        // Read the minion flag first: it decides the stats the pirate code applies while loading.
        this.minion = tag.getBoolean("Minion");
        this.summonerId = tag.hasUUID("Summoner") ? tag.getUUID("Summoner") : null;
        this.summonCooldown = tag.getInt("SummonCooldown");
        this.lootCorpse = tag.hasUUID("LootCorpse") ? tag.getUUID("LootCorpse") : null;
        super.readAdditionalSaveData(tag);
        this.debtorId = tag.hasUUID("Debtor") ? tag.getUUID("Debtor") : null;
        this.waveSerial = tag.getInt("WaveSerial");
        this.test = tag.getBoolean("TestHunter");
        this.greeted = tag.getBoolean("Greeted");
    }
}
