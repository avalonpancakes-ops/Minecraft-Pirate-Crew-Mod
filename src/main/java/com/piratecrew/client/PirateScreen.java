package com.piratecrew.client;

import com.piratecrew.entity.PirateEntity;
import com.piratecrew.menu.PirateMenu;
import com.piratecrew.network.ModNetwork;
import com.piratecrew.network.PirateCommandPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Equipment + orders screen for a pirate in your crew. */
public class PirateScreen extends AbstractContainerScreen<PirateMenu> {
    private Button follow, hold, wander, dismiss;
    private boolean confirmDismiss = false;

    public PirateScreen(PirateMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 196;
        this.inventoryLabelY = PirateMenu.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        int bx = leftPos + 100, by = topPos + 62;
        follow = addRenderableWidget(Button.builder(Component.literal("Follow"), b -> send(PirateCommandPacket.Command.FOLLOW))
                .bounds(bx, by, 35, 16).build());
        hold = addRenderableWidget(Button.builder(Component.literal("Hold"), b -> send(PirateCommandPacket.Command.HOLD))
                .bounds(bx + 36, by, 35, 16).build());
        wander = addRenderableWidget(Button.builder(Component.literal("Roam"), b -> send(PirateCommandPacket.Command.WANDER))
                .bounds(bx, by + 18, 35, 16).build());
        dismiss = addRenderableWidget(Button.builder(Component.literal("Dismiss"), b -> {
            if (!confirmDismiss) {
                confirmDismiss = true;
                b.setMessage(Component.literal("Sure?"));
            } else {
                send(PirateCommandPacket.Command.DISMISS);
            }
        }).bounds(bx + 36, by + 18, 35, 16).build());
    }

    private void send(PirateCommandPacket.Command cmd) {
        PirateEntity p = menu.getPirate();
        if (p != null) ModNetwork.sendToServer(new PirateCommandPacket(p.getId(), cmd));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        PirateEntity p = menu.getPirate();
        if (p == null) return;
        PirateEntity.Orders o = p.getOrders();
        follow.active = o != PirateEntity.Orders.FOLLOW;
        hold.active = o != PirateEntity.Orders.HOLD;
        wander.active = o != PirateEntity.Orders.WANDER;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTicks, int mouseX, int mouseY) {
        GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (Slot s : menu.slots) GuiDraw.slot(g, leftPos + s.x, topPos + s.y);

        // Portrait
        GuiDraw.inset(g, leftPos + 26, topPos + 17, 50, 72);
        g.fill(leftPos + 27, topPos + 18, leftPos + 75, topPos + 88, 0xFF1B1B1B);
        PirateEntity p = menu.getPirate();
        if (p != null) {
            int cx = leftPos + 51, cy = topPos + 84;
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, cx, cy, 30, (float) (cx - mouseX), (float) (cy - 50 - mouseY), p);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, GuiDraw.TEXT, false);
        g.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, GuiDraw.TEXT, false);

        PirateEntity p = menu.getPirate();
        if (p == null) return;
        int x = 100, y = 18;
        g.drawString(font, Component.literal("Tier ").append(Component.literal(p.getTier().label).withStyle(p.getTier().color)), x, y, GuiDraw.TEXT, false);
        g.drawString(font, String.format("HP %.0f / %.0f", p.getHealth(), p.getMaxHealth()), x, y + 10, 0xFFAA0000, false);
        g.drawString(font, String.format("Dmg %.1f", attackDamage(p)), x, y + 20, GuiDraw.TEXT, false);
        g.drawString(font, "Armor " + p.getArmorValue(), x, y + 30, GuiDraw.TEXT, false);
    }

    /** Attack damage isn't synced to clients, so work it out: tier base + weapon bonus. */
    private static double attackDamage(PirateEntity p) {
        double dmg = p.getTier().attackDamage;
        for (AttributeModifier mod : p.getMainHandItem().getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) {
            if (mod.getOperation() == AttributeModifier.Operation.ADDITION) dmg += mod.getAmount();
        }
        return dmg;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTicks);
        this.renderTooltip(g, mouseX, mouseY);
    }
}
