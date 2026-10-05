package com.piratecrew.entity;

import com.piratecrew.bank.LoanManager;
import com.piratecrew.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A dead player's body, holding everything they were carrying instead of scattering it on the
 * ground. Only its owner can take the items back (right-click). When a bounty hunter made the kill
 * the corpse is locked while the hunter searches it for valuables worth the rest of the debt.
 */
public class CorpseEntity extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER = SynchedEntityData.defineId(CorpseEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> DATA_NAME = SynchedEntityData.defineId(CorpseEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_LOCKED = SynchedEntityData.defineId(CorpseEntity.class, EntityDataSerializers.BOOLEAN);

    /** After this long a stuck search is finished by the bank anyway. */
    private static final int LOCK_TIMEOUT = 1200;
    /** Only the dead player can loot their corpse for this long (2 minutes); then anyone can. */
    public static final int OWNER_ONLY_TICKS = 2400;

    private SimpleContainer items = new Contents(54);
    private long createdAt = -1;
    private boolean publicLoot;
    @Nullable private UUID looter;
    private int lockedTicks;

    public CorpseEntity(EntityType<? extends CorpseEntity> type, Level level) {
        super(type, level);
    }

    /** Put a dead player's drops in a new corpse where they died. */
    public static CorpseEntity create(ServerPlayer player, List<ItemStack> drops) {
        CorpseEntity c = new CorpseEntity(ModEntities.CORPSE.get(), player.level());
        double y = player.getY();
        boolean inVoid = y < player.level().getMinBuildHeight();
        if (inVoid) {
            y = player.level().getMinBuildHeight() + 1;
            c.setNoGravity(true);
        }
        c.moveTo(player.getX(), y, player.getZ(), player.getYRot(), 0);
        c.entityData.set(DATA_OWNER, Optional.of(player.getUUID()));
        c.entityData.set(DATA_NAME, player.getGameProfile().getName());
        c.items = c.new Contents(Math.max(54, drops.size()));
        c.createdAt = player.level().getGameTime();
        for (ItemStack s : drops) c.items.addItem(s);
        c.updateName();
        return c;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_OWNER, Optional.empty());
        this.entityData.define(DATA_NAME, "");
        this.entityData.define(DATA_LOCKED, false);
    }

    @Nullable
    public UUID getOwner() {
        return this.entityData.get(DATA_OWNER).orElse(null);
    }

    public String getOwnerName() {
        return this.entityData.get(DATA_NAME);
    }

    public SimpleContainer getItems() {
        return items;
    }

    public boolean isLocked() {
        return this.entityData.get(DATA_LOCKED);
    }

    public boolean isLockedBy(UUID hunter) {
        return isLocked() && hunter.equals(looter);
    }

    /** A bounty hunter is coming to search this corpse; the owner can't take anything until he's done. */
    public void lockFor(BountyHunterEntity hunter) {
        this.looter = hunter.getUUID();
        this.lockedTicks = 0;
        this.entityData.set(DATA_LOCKED, true);
        updateName();
    }

    public void unlock() {
        this.looter = null;
        this.entityData.set(DATA_LOCKED, false);
        updateName();
    }

    private void updateName() {
        String n = "☠ " + getOwnerName() + "'s corpse";
        var name = Component.literal(n).withStyle(ChatFormatting.GRAY);
        if (isLocked()) name.append(Component.literal(" (being searched)").withStyle(ChatFormatting.RED));
        else if (publicLoot) name.append(Component.literal(" (free loot)").withStyle(ChatFormatting.GOLD));
        this.setCustomName(name);
        this.setCustomNameVisible(true);
    }

    /** Ticks left before anyone can loot this corpse (0 = anyone can now). */
    private long ownerOnlyLeft() {
        if (createdAt < 0) return 0;
        return Math.max(0, OWNER_ONLY_TICKS - (this.level().getGameTime() - createdAt));
    }

    /** The corpse's items, viewable like a chest; the view closes when the corpse is gone or out of reach. */
    private class Contents extends SimpleContainer {
        Contents(int size) {
            super(size);
        }

        @Override
        public boolean stillValid(Player player) {
            return CorpseEntity.this.isAlive() && !CorpseEntity.this.isLocked() && player.distanceToSqr(CorpseEntity.this) < 8 * 8;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.isNoGravity()) {
            Vec3 v = this.getDeltaMovement();
            this.setDeltaMovement(v.x * 0.5, this.onGround() ? 0 : Math.max(v.y - 0.04, -1.0), v.z * 0.5);
            this.move(MoverType.SELF, this.getDeltaMovement());
        }
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel sl)) return;
        if (createdAt < 0) createdAt = sl.getGameTime();
        if (!publicLoot && this.tickCount % 20 == 0 && ownerOnlyLeft() == 0) {
            publicLoot = true;
            updateName();
        }
        if (isLocked()) {
            lockedTicks++;
            if (lockedTicks % 20 == 0) {
                Entity h = looter == null ? null : sl.getEntity(looter);
                if (!(h instanceof BountyHunterEntity hunter) || !hunter.isAlive()) {
                    // The hunter was killed (or left) before he got to it: the items are safe.
                    unlock();
                    ServerPlayer owner = ownerPlayer(sl);
                    if (owner != null) owner.sendSystemMessage(Component.literal("The collector never finished searching your corpse. Your items are safe.").withStyle(ChatFormatting.GREEN));
                    LoanManager.cancelSeizure(this);
                } else if (lockedTicks >= LOCK_TIMEOUT) {
                    LoanManager.seizeFromCorpse(this, hunter);
                }
            }
        }
        if (isEmpty() && !isLocked()) this.discard();
    }

    private boolean isEmpty() {
        return items.isEmpty();
    }

    @Nullable
    private ServerPlayer ownerPlayer(ServerLevel sl) {
        UUID o = getOwner();
        return o == null ? null : sl.getServer().getPlayerList().getPlayer(o);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (this.level().isClientSide) return InteractionResult.SUCCESS;
        boolean owner = player.getUUID().equals(getOwner());
        if (isLocked()) {
            player.displayClientMessage(Component.literal(owner ? "A debt collector is searching your corpse. Stop him or wait."
                    : "A debt collector is searching this corpse.").withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }
        if (!owner) {
            long left = ownerOnlyLeft();
            if (left > 0) {
                long secs = (left + 19) / 20;
                player.displayClientMessage(Component.literal(String.format("This is %s's corpse. Anyone can loot it in %d:%02d.",
                        getOwnerName(), secs / 60, secs % 60)).withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.CONSUME;
            }
            openLoot(player);
            return InteractionResult.CONSUME;
        }
        // The owner can sneak-click to pick items out instead of taking everything.
        if (player.isShiftKeyDown()) {
            openLoot(player);
            return InteractionResult.CONSUME;
        }
        // Armor and off-hand items go back where they were worn if the slot is free; the rest into the inventory.
        for (int i = 0; i < items.getContainerSize(); i++) {
            ItemStack s = items.removeItemNoUpdate(i);
            if (s.isEmpty()) continue;
            EquipmentSlot slot = Mob.getEquipmentSlotForItem(s);
            if (slot.getType() == EquipmentSlot.Type.ARMOR && player.getItemBySlot(slot).isEmpty()) {
                player.setItemSlot(slot, s);
                continue;
            }
            if (!player.getInventory().add(s) && !s.isEmpty()) player.drop(s, false);
        }
        this.level().playSound(null, blockPosition(), SoundEvents.ARMOR_EQUIP_GENERIC, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (this.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.SOUL, getX(), getY() + 0.3, getZ(), 12, 0.4, 0.1, 0.4, 0.02);
        player.displayClientMessage(Component.literal("You recovered your belongings.").withStyle(ChatFormatting.GREEN), true);
        this.discard();
        return InteractionResult.CONSUME;
    }

    /** A chest-style view of the first 54 slots, to pick items out one by one. */
    private void openLoot(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        net.minecraft.world.Container view = items;
        sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new net.minecraft.world.inventory.ChestMenu(
                net.minecraft.world.inventory.MenuType.GENERIC_9x6, id, inv, view, 6), Component.literal(getOwnerName() + "'s corpse")));
        this.level().playSound(null, blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 0.8F, 0.8F);
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return !this.isRemoved();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        UUID o = getOwner();
        if (o != null) tag.putUUID("Owner", o);
        tag.putString("OwnerName", getOwnerName());
        tag.putBoolean("Locked", isLocked());
        if (looter != null) tag.putUUID("Looter", looter);
        tag.putInt("LockedTicks", lockedTicks);
        tag.putLong("CreatedAt", createdAt);
        tag.putBoolean("PublicLoot", publicLoot);
        tag.putInt("Size", items.getContainerSize());
        ListTag list = new ListTag();
        for (int i = 0; i < items.getContainerSize(); i++) {
            ItemStack s = items.getItem(i);
            if (s.isEmpty()) continue;
            CompoundTag t = new CompoundTag();
            t.putShort("Slot", (short) i);
            s.save(t);
            list.add(t);
        }
        tag.put("Items", list);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(DATA_OWNER, tag.hasUUID("Owner") ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
        this.entityData.set(DATA_NAME, tag.getString("OwnerName"));
        this.looter = tag.hasUUID("Looter") ? tag.getUUID("Looter") : null;
        this.lockedTicks = tag.getInt("LockedTicks");
        this.entityData.set(DATA_LOCKED, tag.getBoolean("Locked") && looter != null);
        this.createdAt = tag.contains("CreatedAt") ? tag.getLong("CreatedAt") : -1;
        this.publicLoot = tag.getBoolean("PublicLoot");
        this.items = new Contents(Math.max(54, tag.getInt("Size")));
        for (Tag t : tag.getList("Items", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            int slot = c.getShort("Slot");
            if (slot >= 0 && slot < items.getContainerSize()) items.setItem(slot, ItemStack.of(c));
        }
        updateName();
    }
}
