package com.piratecrew.client;

import com.piratecrew.entity.PirateTask;
import com.piratecrew.network.ModNetwork;
import com.piratecrew.network.PirateCommandPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Pick a job for a pirate. It works around the spot where it's standing when you choose. */
public class TaskScreen extends Screen {
    private static final int W = 200, H = 168;

    private record Choice(PirateTask task, PirateCommandPacket.Command cmd, ItemStack icon) {}

    private static final Choice[] CHOICES = {
            new Choice(PirateTask.MINE, PirateCommandPacket.Command.TASK_MINE, new ItemStack(Items.IRON_PICKAXE)),
            new Choice(PirateTask.FARM, PirateCommandPacket.Command.TASK_FARM, new ItemStack(Items.WHEAT)),
            new Choice(PirateTask.FISH, PirateCommandPacket.Command.TASK_FISH, new ItemStack(Items.FISHING_ROD)),
            new Choice(PirateTask.WOOD, PirateCommandPacket.Command.TASK_WOOD, new ItemStack(Items.OAK_LOG)),
    };

    private final int entityId;
    private final String pirateName;
    private final PirateTask current;
    private int left, top;

    public TaskScreen(int entityId, String pirateName, PirateTask current) {
        super(Component.literal("Tasks"));
        this.entityId = entityId;
        this.pirateName = pirateName;
        this.current = current;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int y = top + 30;
        for (Choice c : CHOICES) {
            String label = (c.task() == current ? "▶ " : "") + switch (c.task()) {
                case MINE -> "Mine Ore";
                case FARM -> "Farm Crops";
                case FISH -> "Go Fishing";
                case WOOD -> "Chop Wood";
                default -> c.task().label;
            };
            addRenderableWidget(Button.builder(Component.literal(label), b -> choose(c.cmd()))
                    .tooltip(Tooltip.create(Component.literal(c.task().description)))
                    .bounds(left + 34, y, W - 46, 20).build());
            y += 24;
        }
        addRenderableWidget(Button.builder(Component.literal("Stop Working").withStyle(ChatFormatting.RED), b -> choose(PirateCommandPacket.Command.TASK_STOP))
                .tooltip(Tooltip.create(Component.literal("Stop the current job and follow you")))
                .bounds(left + 12, y + 4, 84, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(left + W - 96, y + 4, 84, 20).build());
    }

    private void choose(PirateCommandPacket.Command cmd) {
        ModNetwork.sendToServer(new PirateCommandPacket(entityId, cmd));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        renderBackground(g);
        GuiDraw.panel(g, left, top, W, H);
        g.drawString(font, Component.literal("Give " + pirateName + " a task").withStyle(ChatFormatting.BOLD), left + 10, top + 9, 0xFF5A3A00, false);
        g.drawString(font, "Works around where it stands now.", left + 10, top + 19, 0xFF606060, false);
        int y = top + 32;
        for (Choice c : CHOICES) {
            g.renderItem(c.icon(), left + 12, y);
            y += 24;
        }
        super.render(g, mouseX, mouseY, partialTicks);
    }
}
