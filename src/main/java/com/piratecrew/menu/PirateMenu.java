package com.piratecrew.menu;

import com.mojang.datafixers.util.Pair;
import com.piratecrew.crew.CrewManager;
import com.piratecrew.entity.PirateEntity;
import com.piratecrew.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;

public class PirateMenu extends AbstractContainerMenu {
    public static final int PIRATE_SLOTS = 6;
    public static final int PACK_SLOTS = PirateEntity.PACK_SIZE;
    public static final int PACK_Y = 118;
    public static final int INV_Y = 170;

    @Nullable
    private final PirateEntity pirate;
    private final Container equipment;
    private final Container pack;

    public PirateMenu(int id, Inventory playerInv, @Nullable PirateEntity pirate) {
        super(ModMenus.PIRATE.get(), id);
        this.pirate = pirate;
        this.equipment = pirate != null ? new PirateEquipmentContainer(pirate) : new SimpleContainer(PIRATE_SLOTS);
        this.pack = pirate != null ? pirate.getPack() : new SimpleContainer(PACK_SLOTS);

        // Armor column
        addSlot(new GearSlot(equipment, 0, 8, 18, EquipmentSlot.HEAD, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET));
        addSlot(new GearSlot(equipment, 1, 8, 36, EquipmentSlot.CHEST, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE));
        addSlot(new GearSlot(equipment, 2, 8, 54, EquipmentSlot.LEGS, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS));
        addSlot(new GearSlot(equipment, 3, 8, 72, EquipmentSlot.FEET, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS));
        // Hands
        addSlot(new GearSlot(equipment, 4, 80, 18, EquipmentSlot.MAINHAND, new ResourceLocation("item/empty_slot_sword")));
        addSlot(new GearSlot(equipment, 5, 80, 36, EquipmentSlot.OFFHAND, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD));

        // Pirate's pack: 2 rows of 9
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(pack, col + row * 9, 8 + col * 18, PACK_Y + row * 18));
            }
        }

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, INV_Y + 58));
        }
    }

    public static PirateMenu fromNetwork(int id, Inventory inv, FriendlyByteBuf buf) {
        Entity e = inv.player.level().getEntity(buf.readVarInt());
        return new PirateMenu(id, inv, e instanceof PirateEntity p ? p : null);
    }

    @Nullable
    public PirateEntity getPirate() {
        return pirate;
    }

    @Override
    public boolean stillValid(Player player) {
        if (pirate == null || !equipment.stillValid(player)) return false;
        if (player instanceof ServerPlayer sp) return CrewManager.isInSameCrew(sp, pirate);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int packStart = PIRATE_SLOTS, packEnd = PIRATE_SLOTS + PACK_SLOTS;
        int invStart = packEnd, invEnd = this.slots.size();

        if (index < PIRATE_SLOTS) {
            // gear -> pack, then player
            if (!moveItemStackTo(stack, packStart, packEnd, false) && !moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
        } else if (index < packEnd) {
            // pack -> player
            if (!moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
        } else {
            // player -> armour slot / empty hands, otherwise the pack
            boolean moved = false;
            for (int i = 0; i < 4 && !moved; i++) {
                Slot target = this.slots.get(i);
                if (!target.hasItem() && target.mayPlace(stack)) moved = moveItemStackTo(stack, i, i + 1, false);
            }
            if (!moved && !this.slots.get(5).hasItem() && stack.canPerformAction(net.minecraftforge.common.ToolActions.SHIELD_BLOCK)) moved = moveItemStackTo(stack, 5, 6, false);
            if (!moved && !this.slots.get(4).hasItem() && stack.getMaxStackSize() == 1) moved = moveItemStackTo(stack, 4, 5, false);
            if (!moved) moved = moveItemStackTo(stack, packStart, packEnd, false);
            if (!moved) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    /** One of the pirate's equipment slots. */
    private class GearSlot extends Slot {
        private final EquipmentSlot type;
        private final ResourceLocation icon;

        GearSlot(Container container, int index, int x, int y, EquipmentSlot type, ResourceLocation icon) {
            super(container, index, x, y);
            this.type = type;
            this.icon = icon;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (type.getType() == EquipmentSlot.Type.ARMOR) {
                return pirate != null && stack.canEquip(type, pirate);
            }
            return true;
        }

        @Override
        public int getMaxStackSize() {
            return type.getType() == EquipmentSlot.Type.ARMOR ? 1 : super.getMaxStackSize();
        }

        @Override
        public boolean mayPickup(Player player) {
            ItemStack stack = getItem();
            if (!stack.isEmpty() && !player.isCreative() && EnchantmentHelper.hasBindingCurse(stack)) return false;
            return super.mayPickup(player);
        }

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return Pair.of(InventoryMenu.BLOCK_ATLAS, icon);
        }
    }
}
