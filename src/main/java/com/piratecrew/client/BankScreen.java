package com.piratecrew.client;

import com.piratecrew.network.BankActionPacket;
import com.piratecrew.network.ModNetwork;
import com.piratecrew.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Ruby bank: shows the balance as a number and moves rubies in and out of the inventory. */
public class BankScreen extends Screen {
    private static final int W = 220, H = 170;
    private static long balance;
    private static int inventoryRubies;

    private int left, top;
    private EditBox amountBox;

    public BankScreen() {
        super(Component.literal("Ruby Bank"));
    }

    public static void update(long bal, int inv) {
        balance = bal;
        inventoryRubies = inv;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static void send(boolean deposit, long amount) {
        ModNetwork.sendToServer(new BankActionPacket(deposit, amount));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;

        int y = top + 81;
        int bw = 44, gap = 4, x0 = left + 12;
        addRenderableWidget(Button.builder(Component.literal("+1"), b -> send(true, 1)).bounds(x0, y, bw, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+10"), b -> send(true, 10)).bounds(x0 + (bw + gap), y, bw, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+64"), b -> send(true, 64)).bounds(x0 + 2 * (bw + gap), y, bw, 18).build());
        addRenderableWidget(Button.builder(Component.literal("All").withStyle(ChatFormatting.GREEN), b -> send(true, -1))
                .bounds(x0 + 3 * (bw + gap), y, bw, 18).build());

        y = top + 113;
        addRenderableWidget(Button.builder(Component.literal("-1"), b -> send(false, 1)).bounds(x0, y, bw, 18).build());
        addRenderableWidget(Button.builder(Component.literal("-10"), b -> send(false, 10)).bounds(x0 + (bw + gap), y, bw, 18).build());
        addRenderableWidget(Button.builder(Component.literal("-64"), b -> send(false, 64)).bounds(x0 + 2 * (bw + gap), y, bw, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Max").withStyle(ChatFormatting.GOLD), b -> send(false, -1))
                .bounds(x0 + 3 * (bw + gap), y, bw, 18).build());

        y = top + 141;
        amountBox = new EditBox(font, x0, y + 1, 80, 16, Component.literal("Amount"));
        amountBox.setMaxLength(9);
        amountBox.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        amountBox.setHint(Component.literal("Amount...").withStyle(ChatFormatting.DARK_GRAY));
        addRenderableWidget(amountBox);
        addRenderableWidget(Button.builder(Component.literal("Deposit"), b -> sendAmount(true)).bounds(x0 + 84, y, 52, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Withdraw"), b -> sendAmount(false)).bounds(x0 + 140, y, 56, 18).build());
    }

    private void sendAmount(boolean deposit) {
        String v = amountBox.getValue();
        if (v.isEmpty()) return;
        try {
            long n = Long.parseLong(v);
            if (n > 0) send(deposit, n);
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (amountBox != null) amountBox.tick();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        renderBackground(g);
        GuiDraw.panel(g, left, top, W, H);
        g.drawString(font, Component.literal("⚓ Ruby Bank").withStyle(ChatFormatting.BOLD), left + 10, top + 9, 0xFF5A3A00, false);

        // Balance counter
        GuiDraw.inset(g, left + 10, top + 24, W - 20, 30);
        g.fill(left + 11, top + 25, left + W - 10, top + 53, 0xFF1E140C);
        String bal = String.format("%,d", balance);
        float scale = 2.0F;
        int textW = (int) (font.width(bal) * scale);
        int totalW = 18 + textW;
        int bx = left + (W - totalW) / 2;
        g.pose().pushPose();
        g.pose().translate(bx, top + 30, 0);
        g.renderItem(new ItemStack(ModItems.RUBY.get()), 0, 1);
        g.pose().translate(20, 2, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, bal, 0, 0, 0xFFFF5566, true);
        g.pose().popPose();

        g.drawString(font, "In your inventory: " + String.format("%,d", inventoryRubies) + " rubies", left + 12, top + 58, 0xFF404040, false);
        g.drawString(font, Component.literal("Deposit").withStyle(ChatFormatting.BOLD), left + 12, top + 71, 0xFF2E6B2E, false);
        g.drawString(font, Component.literal("Withdraw").withStyle(ChatFormatting.BOLD), left + 12, top + 103, 0xFF7A4A00, false);
        super.render(g, mouseX, mouseY, partialTicks);
    }
}
