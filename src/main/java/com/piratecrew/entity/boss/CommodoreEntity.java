package com.piratecrew.entity.boss;

import com.piratecrew.entity.CombatStyle;
import com.piratecrew.item.GearTier;
import com.piratecrew.pact.SoulPacts;
import com.piratecrew.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

/**
 * Commodore Graves, first of the Order's bosses (summoned with a Signal Flare). A duelist with a
 * fleet behind him: he calls down broadsides on whoever he's fighting, leaps in with a crushing
 * landing, and calls marines to his side as he weakens.
 */
public class CommodoreEntity extends MarineBossEntity {
    private static final String[] BROADSIDE = {"Fire the broadside!", "Gunners, on my mark!", "Rake them with grapeshot!", "Let them taste iron!"};
    private boolean leaping;
    private int leapTick;

    public CommodoreEntity(EntityType<? extends CommodoreEntity> type, Level level) {
        super(type, level);
    }

    @Override protected String bossName() { return "Graves"; }
    @Override protected String bossTitle() { return "Commodore"; }
    @Override protected String bossSkin() { return "boss_commodore"; }
    @Override protected ChatFormatting bossColor() { return ChatFormatting.AQUA; }
    @Override protected BossEvent.BossBarColor barColor() { return BossEvent.BossBarColor.BLUE; }
    @Override protected double bossHealth() { return 600; }
    @Override protected double bossDamage() { return 9; }
    @Override protected double bossArmor() { return 6; }
    @Override protected double bossToughness() { return 0; }
    @Override protected float bossScale() { return 1.15F; }
    @Override public String epithet() { return "Hound of the Order"; }
    @Override public int ribbonColor() { return 0x5AC8FF; }
    @Override public int bountyValue() { return 150; }

    @Override
    protected void equipBoss() {
        setCombatStyle(CombatStyle.BRAWLER);
        setItemSlot(EquipmentSlot.MAINHAND, gear(ModItems.GEAR.get(GearTier.ABYSSAL).sword().get(), Enchantments.SHARPNESS, 3));
        setItemSlot(EquipmentSlot.OFFHAND, gear(Items.SHIELD));
        getPack().setItem(0, PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.STRONG_HEALING));
        getPack().setItem(1, PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.STRONG_HEALING));
    }

    @Override
    protected void tickBoss(ServerLevel level, LivingEntity target) {
        if (leaping && onGround() && tickCount - leapTick > 5) {
            leaping = false;
            BossFx.shockwave(level, this, position(), 4.0, 14.0F, 0.6);
        }
        if (crossed(0.66F, 1)) {
            shout("To me, marines! Take this rabble alive!");
            callMarines(level, target, 3, Rank.RECRUIT, Rank.RIFLEMAN, Rank.SERGEANT);
        }
        if (crossed(0.33F, 2)) {
            shout("Enough! I'll see you hanged from my yardarm!");
            callMarines(level, target, 3, Rank.SERGEANT, Rank.RIFLEMAN, Rank.SERGEANT);
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, MobEffectInstance.INFINITE_DURATION, 0, false, true));
        }
        if (abilityCooldown > 0) return;
        double dist = distanceTo(target);
        boolean enraged = healthFraction() < 0.5F;
        if (dist > 7 && dist < 18 && onGround() && canSeeTarget(target) && random.nextFloat() < 0.45F) {
            leapAt(target.position(), 0.75);
            leaping = true;
            leapTick = tickCount;
            abilityCooldown = 90;
        } else {
            shout(BROADSIDE[random.nextInt(BROADSIDE.length)]);
            fx.barrage(target, BossFx.Kind.CANNON, enraged ? 8 : 5, 4.5, 16.0F, 30);
            abilityCooldown = enraged ? 110 : 150;
        }
    }

    @Override
    protected void dropBossLoot(DamageSource source, int looting) {
        spawnAtLocation(new ItemStack(ModItems.KRAKEN_LURE.get()));
        spawnAtLocation(new ItemStack(ModItems.TREASURE_MAP.get()));
        if (random.nextFloat() < 0.2F + looting * 0.05F) spawnAtLocation(ModItems.legend(com.piratecrew.item.LegendaryWeaponItem.Legend.BROADSIDE));
        spawnAtLocation(new ItemStack(ModItems.COMMODORE_INSIGNIA.get(), 1 + random.nextInt(2)));
        spawnAtLocation(new ItemStack(ModItems.ABYSSAL_SHARD.get(), 4 + random.nextInt(5 + looting)));
        spawnAtLocation(new ItemStack(ModItems.TIDESTEEL_INGOT.get(), 3 + random.nextInt(4)));
        spawnAtLocation(new ItemStack(ModItems.MARINE_BADGE.get(), 4 + random.nextInt(5)));
        spawnAtLocation(new ItemStack(ModItems.RUBY.get(), 20 + random.nextInt(21)));
        if (random.nextFloat() < 0.25F + looting * 0.03F) spawnAtLocation(SoulPacts.randomPactItem(random));
    }
}
