package com.piratecrew.client;

import com.piratecrew.network.BankActionPacket;
import com.piratecrew.network.ModNetwork;
import com.piratecrew.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Ruby bank: shows the balance as a number and moves rubies in and out of the inventory. */
public class BankScreen extends Screen {
    private static final int W = 220, H = 242;
    private static long balance;
    private static int inventoryRubies;
    private static long loanOwed;
    private static long loanTicksLeft;
    private static boolean loanOverdue;
    private static int loanMax = 500, interestPct = 25, loanDays = 15;

    private final java.util.List<Button> borrowButtons = new java.util.ArrayList<>();
    private final java.util.List<Button> repayButtons = new java.util.ArrayList<>();

    private int left, top;
    private EditBox amountBox;

    public BankScreen() {
        super(Component.literal("Ruby Bank"));
    }

    public static void update(com.piratecrew.network.BankSyncPacket p) {
        balance = p.balance;
        inventoryRubies = p.inventoryRubies;
        loanOwed = p.loanOwed;
        loanTicksLeft = p.loanTicksLeft;
        loanOverdue = p.loanOverdue;
        loanMax = p.loanMax;
        interestPct = p.interestPct;
        loanDays = p.loanDays;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static void send(boolean deposit, long amount) {
        ModNetwork.sendToServer(new BankActionPacket(deposit, amount));
    }

    private static void loan(BankActionPacket.Action action, long amount) {
        ModNetwork.sendToServer(new BankActionPacket(action, amount));
    }

    private long typedAmount() {
        try {
            return amountBox == null || amountBox.getValue().isEmpty() ? 0 : Long.parseLong(amountBox.getValue());
        } catch (NumberFormatException e) {
            return 0;
        }
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

        // Loans
        borrowButtons.clear();
        repayButtons.clear();
        y = top + 214;
        long[] borrowAmounts = {Math.max(1, loanMax / 5), Math.max(1, loanMax / 2), loanMax};
        for (int i = 0; i < 3; i++) {
            long amt = borrowAmounts[i];
            Component label = Component.literal(String.valueOf(amt));
            if (i == 2) label = Component.literal(String.valueOf(amt)).withStyle(ChatFormatting.GOLD);
            borrowButtons.add(addRenderableWidget(Button.builder(label, b -> loan(BankActionPacket.Action.BORROW, amt))
                    .tooltip(Tooltip.create(Component.literal("Borrow " + amt + " rubies, repay " + previewOwed(amt))))
                    .bounds(x0 + i * (bw + gap), y, bw, 18).build()));
        }
        borrowButtons.add(addRenderableWidget(Button.builder(Component.literal("Typed"), b -> {
                    long n = typedAmount();
                    if (n > 0) loan(BankActionPacket.Action.BORROW, Math.min(n, loanMax));
                }).tooltip(Tooltip.create(Component.literal("Borrow the amount typed in the box above (up to " + loanMax + ")")))
                .bounds(x0 + 3 * (bw + gap), y, bw, 18).build()));

        repayButtons.add(addRenderableWidget(Button.builder(Component.literal("10"), b -> loan(BankActionPacket.Action.REPAY, 10))
                .tooltip(Tooltip.create(Component.literal("Repay 10 rubies from your bank balance"))).bounds(x0, y, bw, 18).build()));
        repayButtons.add(addRenderableWidget(Button.builder(Component.literal("64"), b -> loan(BankActionPacket.Action.REPAY, 64))
                .tooltip(Tooltip.create(Component.literal("Repay 64 rubies from your bank balance"))).bounds(x0 + (bw + gap), y, bw, 18).build()));
        repayButtons.add(addRenderableWidget(Button.builder(Component.literal("All").withStyle(ChatFormatting.GREEN), b -> loan(BankActionPacket.Action.REPAY, -1))
                .tooltip(Tooltip.create(Component.literal("Repay as much as your bank balance covers"))).bounds(x0 + 2 * (bw + gap), y, bw, 18).build()));
        repayButtons.add(addRenderableWidget(Button.builder(Component.literal("Typed"), b -> {
                    long n = typedAmount();
                    if (n > 0) loan(BankActionPacket.Action.REPAY, n);
                }).tooltip(Tooltip.create(Component.literal("Repay the amount typed in the box above")))
                .bounds(x0 + 3 * (bw + gap), y, bw, 18).build()));
        updateLoanButtons();
    }

    private long previewOwed(long amt) {
        return amt + (long) Math.ceil(amt * interestPct / 100.0 - 1e-9);
    }

    private void updateLoanButtons() {
        boolean hasLoan = loanOwed > 0;
        for (Button b : borrowButtons) b.visible = !hasLoan;
        for (Button b : repayButtons) b.visible = hasLoan;
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
        updateLoanButtons();
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

        // Loan section
        g.fill(left + 10, top + 166, left + W - 10, top + 167, 0x60402000);
        g.drawString(font, Component.literal("Loan").withStyle(ChatFormatting.BOLD), left + 12, top + 172, 0xFF7A1F1F, false);
        if (loanOwed <= 0) {
            String terms = "up to " + String.format("%,d", loanMax) + " \u00b7 " + interestPct + "% \u00b7 " + loanDays + " days";
            g.drawString(font, terms, left + W - 12 - font.width(terms), top + 172, 0xFF606060, false);
            g.drawString(font, "Borrowed rubies go into your account.", left + 12, top + 186, 0xFF404040, false);
            g.drawString(font, "Miss the deadline and the bank sends", left + 12, top + 197, 0xFF8A2020, false);
            g.drawString(font, "bounty hunters after you.", left + 12, top + 206, 0xFF8A2020, false);
        } else {
            String owed = "Owed: " + String.format("%,d", loanOwed) + " rubies";
            g.drawString(font, owed, left + W - 12 - font.width(owed), top + 172, 0xFFB02020, false);
            String due;
            int color = 0xFF404040;
            if (loanOverdue || loanTicksLeft <= 0) {
                due = "OVERDUE: bounty hunters are after you!";
                color = 0xFFC01010;
            } else if (loanTicksLeft < 24000) {
                long hours = Math.max(1, (loanTicksLeft + 999) / 1000);
                due = "Due today: " + hours + " in-game hour" + (hours == 1 ? "" : "s") + " left";
                color = 0xFFB05000;
            } else {
                long days = (loanTicksLeft + 23999) / 24000;
                due = "Due in " + days + " Minecraft day" + (days == 1 ? "" : "s");
                if (days <= 3) color = 0xFFB05000;
            }
            g.drawString(font, due, left + 12, top + 186, color, false);
            g.drawString(font, "Repayments come from your bank balance.", left + 12, top + 199, 0xFF606060, false);
        }
        super.render(g, mouseX, mouseY, partialTicks);
    }
}
