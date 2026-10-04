package com.piratecrew.menu;

import com.piratecrew.entity.PirateEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** A Container view straight onto the pirate's equipment slots. */
public class PirateEquipmentContainer implements Container {
    public static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };

    private final PirateEntity pirate;

    public PirateEquipmentContainer(PirateEntity pirate) {
        this.pirate = pirate;
    }

    @Override
    public int getContainerSize() {
        return SLOTS.length;
    }

    @Override
    public boolean isEmpty() {
        for (EquipmentSlot s : SLOTS) if (!pirate.getItemBySlot(s).isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        return pirate.getItemBySlot(SLOTS[index]);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack current = getItem(index);
        if (current.isEmpty() || count <= 0) return ItemStack.EMPTY;
        ItemStack taken = current.split(count);
        setItem(index, current.isEmpty() ? ItemStack.EMPTY : current);
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack current = getItem(index);
        setItem(index, ItemStack.EMPTY);
        return current;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        pirate.setItemSlot(SLOTS[index], stack);
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return pirate.isAlive() && player.distanceToSqr(pirate) < 8 * 8;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < SLOTS.length; i++) setItem(i, ItemStack.EMPTY);
    }
}
