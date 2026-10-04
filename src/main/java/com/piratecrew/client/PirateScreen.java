package com.piratecrew.client;

import com.piratecrew.entity.CombatStyle;
import com.piratecrew.entity.PirateEntity;
import com.piratecrew.entity.PirateTask;
import com.piratecrew.menu.PirateMenu;
import com.piratecrew.network.ModNetwork;
import com.piratecrew.network.PirateCommandPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Equipment, pack, orders and tasks for a pirate in your crew. */
public class PirateScreen extends AbstractContainerScreen<PirateMenu> {
    private Button follow, hold, wander, dismiss, tasks;
    private boolean confirmDismiss = false;

    public PirateScreen(PirateMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = PirateMenu.INV_Y + 58 + 18 + 6;
        this.inventoryLabelY = PirateMenu.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        int bx = leftPos + 100, by = topPos + 60;
        follow = addRenderableWidget(Button.builder(Component.literal("Follow"), b -> send(PirateCommandPacket.Command.FOLLOW))
                .bounds(bx, by, 35, 16).build());
        hold = addRenderableWidget(Button.builder(Component.literal("Hold"), b -> send(PirateCommandPacket.Command.HOLD))
                .bounds(bx + 36, by, 35, 16).build());
        wander = addRenderableWidget(Button.builder(Component.literal("Roam"), b -> send(PirateCommandPacket.Command.WANDER))
                .bounds(bx, by + 17, 35, 16).build());
        dismiss = addRenderableWidget(Button.builder(Component.literal("Dismiss"), b -> {
            if (!confirmDismiss) {
                confirmDismiss = true;
                b.setMessage(Component.literal("Sure?"));
            } else {
                send(PirateCommandPacket.Command.DISMISS);
            }
        }).bounds(bx + 36, by + 17, 35, 16).build());
        tasks = addRenderableWidget(Button.builder(Component.literal("Tasks..."), b -> openTasks())
                .tooltip(Tooltip.create(Component.literal("Send this pirate to mine, farm, fish or chop wood")))
                .bounds(bx, by + 34, 71, 16).build());
    }

    private void send(PirateCommandPacket.Command cmd) {
        PirateEntity p = menu.getPirate();
        if (p != null) ModNetwork.sendToServer(new PirateCommandPacket(p.getId(), cmd));
    }

    private void openTasks() {
        PirateEntity p = menu.getPirate();
        if (p == null || minecraft == null || minecraft.player == null) return;
        minecraft.player.closeContainer();
        minecraft.setScreen(new TaskScreen(p.getId(), p.getPirateName(), p.getTask()));
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
        g.drawString(this.font, "Pack", 8, PirateMenu.PACK_Y - 11, GuiDraw.TEXT, false);

        PirateEntity p = menu.getPirate();
        if (p == null) return;
        int x = 100, y = 17;
        CombatStyle style = p.getCombatStyle();
        g.drawString(font, Component.literal(p.getTier().label).withStyle(p.getTier().color, ChatFormatting.BOLD)
                .append(Component.literal(" " + style.label).withStyle(s -> s.withBold(false).withColor(styleColor(style)))), x, y, GuiDraw.TEXT, false);
        g.drawString(font, String.format("HP %.0f/%.0f", p.getHealth(), p.getMaxHealth()), x, y + 10, 0xFFAA0000, false);
        g.drawString(font, String.format("Dmg %.1f  Arm %d", attackDamage(p), p.getArmorValue()), x, y + 20, GuiDraw.TEXT, false);
        PirateTask task = p.getTask();
        String doing = p.getOrders() == PirateEntity.Orders.WORK ? task.label
                : switch (p.getOrders()) { case FOLLOW -> "Following"; case HOLD -> "Holding"; default -> "Roaming"; };
        g.drawString(font, font.plainSubstrByWidth(doing, 72), x, y + 31, 0xFF2E5A2E, false);
    }

    private static int styleColor(CombatStyle s) {
        return switch (s) {
            case BRAWLER -> 0xB02020;
            case MARKSMAN -> 0x1E6B8A;
            default -> 0x8A6A00;
        };
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
        PirateEntity p = menu.getPirate();
        if (p != null && mouseX >= leftPos + 100 && mouseX < leftPos + 172 && mouseY >= topPos + 16 && mouseY < topPos + 26) {
            g.renderTooltip(font, Component.literal(switch (p.getCombatStyle()) {
                case BRAWLER -> "Brawler: rushes in and switches to melee early.";
                case MARKSMAN -> "Marksman: prefers ranged weapons and keeps its distance.";
                default -> "Balanced: melee up close, ranged at a distance.";
            }), mouseX, mouseY);
        }
    }
}
