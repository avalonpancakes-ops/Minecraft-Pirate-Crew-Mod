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
        try {
            return Config.HUNTER_STRENGTH.get();
        } catch (Exception e) {
            return 5.0;
        }
    }

    @Override
    protected String rollSkin(PirateTier tier) {
        return PirateSkins.randomHunter(this.random, tier);
    }

    @Override
    protected String rollName() {
        return FIRST[this.random.nextInt(FIRST.length)] + " " + TITLE[this.random.nextInt(TITLE.length)];
    }

    @Override
    public void updateDisplayName() {
        PirateTier tier = getTier();
        MutableComponent name = Component.literal("[" + tier.label + "] ").withStyle(tier.color, ChatFormatting.BOLD);
        name.append(Component.literal("☠ " + pirateName).withStyle(s -> s.withBold(false).withColor(ChatFormatting.RED)));
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

        if (tier.ordinal() <= PirateTier.C.ordinal()) {
            // Sword (wooden at F, stone at D, ruby at C), bow and shield: bow in hand for marksmen, sword for everyone else.
            boolean marksman = getCombatStyle() == CombatStyle.MARKSMAN;
            setItemSlot(EquipmentSlot.MAINHAND, gear(marksman ? Items.BOW : sword));
            getPack().setItem(0, gear(marksman ? sword : Items.BOW));
            setItemSlot(EquipmentSlot.OFFHAND, gear(Items.SHIELD));
            if (tier == PirateTier.C) {
                // Poison splash potions to throw, and milk to purge debuffs. Hunters never run out.
                getPack().setItem(1, PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.POISON));
                getPack().setItem(2, new ItemStack(Items.MILK_BUCKET));
            }
            return;
        }

        // Marksmen open with a crossbow, balanced hunters carry one in reserve, brawlers bring a shield.
        ItemStack ranged = gear(this.random.nextInt(3) == 0 ? Items.BOW : Items.CROSSBOW);
        switch (getCombatStyle()) {
            case MARKSMAN -> {
                setItemSlot(EquipmentSlot.MAINHAND, ranged);
                getPack().setItem(0, gear(sword));
            }
            case BALANCED -> {
                setItemSlot(EquipmentSlot.MAINHAND, gear(sword));
                getPack().setItem(0, ranged);
                if (t >= PirateTier.A.ordinal()) setItemSlot(EquipmentSlot.OFFHAND, gear(Items.SHIELD));
            }
            default -> {
                setItemSlot(EquipmentSlot.MAINHAND, gear(sword));
                if (t >= PirateTier.C.ordinal()) setItemSlot(EquipmentSlot.OFFHAND, gear(Items.SHIELD));
            }
        }
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.debtorId = tag.hasUUID("Debtor") ? tag.getUUID("Debtor") : null;
        this.waveSerial = tag.getInt("WaveSerial");
        this.test = tag.getBoolean("TestHunter");
        this.greeted = tag.getBoolean("Greeted");
    }
}
