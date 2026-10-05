package com.piratecrew.client;

import com.piratecrew.bank.ShopCatalog;
import com.piratecrew.network.ModNetwork;
import com.piratecrew.network.ShopBuyPacket;
import com.piratecrew.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** The banker's shop: items for rubies, paid from the bank balance first, then rubies carried. */
public class ShopScreen extends Screen {
    private static final int W = 240, H = 194;
    private static final int COLS = 7, ROWS = 3, CELL_W = 30, CELL_H = 34, GAP = 2;
    private static ShopCatalog.Category tab = ShopCatalog.Category.FOOD;

    private int left, top;
    private final List<Button> tabButtons = new ArrayList<>();
    private List<ShopCatalog.Entry> shown = List.of();
    private final List<ItemStack> stacks = new ArrayList<>();

    public ShopScreen() {
        super(Component.literal("Banker's Shop"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.literal("Bank"), b -> minecraft.setScreen(new BankScreen()))
                .bounds(left + W - 54, top + 5, 46, 16).build(PirateButton::new));
        tabButtons.clear();
        ShopCatalog.Category[] cats = ShopCatalog.Category.values();
        int tw = (W - 20) / cats.length;
        for (int i = 0; i < cats.length; i++) {
            ShopCatalog.Category c = cats[i];
            tabButtons.add(addRenderableWidget(Button.builder(Component.literal(c.label), b -> selectTab(c))
                    .bounds(left + 10 + i * tw, top + 40, tw - 1, 16).build(PirateButton::new)));
        }
        selectTab(tab);
    }

    private void selectTab(ShopCatalog.Category c) {
        tab = c;
        ShopCatalog.Category[] cats = ShopCatalog.Category.values();
        for (int i = 0; i < tabButtons.size(); i++) tabButtons.get(i).active = cats[i] != c;
        shown = ShopCatalog.in(c);
        if (shown.size() > COLS * ROWS) shown = shown.subList(0, COLS * ROWS);
        stacks.clear();
        for (ShopCatalog.Entry e : shown) stacks.add(e.make());
    }

    private int gridX() {
        return left + (W - (COLS * CELL_W + (COLS - 1) * GAP)) / 2;
    }

    private int gridY() {
        return top + 62;
    }

    private int cellAt(double mx, double my) {
        for (int i = 0; i < shown.size(); i++) {
            int x = gridX() + (i % COLS) * (CELL_W + GAP);
            int y = gridY() + (i / COLS) * (CELL_H + GAP);
            if (mx >= x && mx < x + CELL_W && my >= y && my < y + CELL_H) return i;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int i = cellAt(mx, my);
            if (i >= 0) {
                ModNetwork.sendToServer(new ShopBuyPacket(shown.get(i).index(), hasShiftDown() ? 5 : 1));
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        renderBackground(g);
        GuiDraw.panel(g, left, top, W, H);
        g.drawString(font, Component.literal("⚓ Banker's Shop").withStyle(ChatFormatting.BOLD), left + 10, top + 9, 0xFF5A3A00, false);

        long bank = BankScreen.balance();
        int carried = BankScreen.inventoryRubies();
        long funds = bank + carried;
        g.renderItem(new ItemStack(ModItems.RUBY.get()), left + 9, top + 21);
        g.drawString(font, String.format("Bank %,d  ·  Carried %,d", bank, carried), left + 28, top + 26, 0xFF404040, false);

        int hovered = cellAt(mouseX, mouseY);
        for (int i = 0; i < shown.size(); i++) {
            int x = gridX() + (i % COLS) * (CELL_W + GAP);
            int y = gridY() + (i / COLS) * (CELL_H + GAP);
            GuiDraw.inset(g, x, y, CELL_W, CELL_H);
            if (i == hovered) g.fill(x + 1, y + 1, x + CELL_W - 1, y + CELL_H - 1, 0x40FFFFFF);
            ItemStack s = stacks.get(i);
            g.renderItem(s, x + 7, y + 3);
            g.renderItemDecorations(font, s, x + 7, y + 3);
            String price = String.valueOf(shown.get(i).price());
            int color = funds >= shown.get(i).price() ? 0xFF2E7D2E : 0xFFB02020;
            g.drawString(font, price, x + (CELL_W - font.width(price)) / 2, y + 23, color, false);
        }

        g.drawString(font, "Click to buy, shift-click for 5.", left + 10, top + H - 22, 0xFF606060, false);
        g.drawString(font, "Paid from your bank, then rubies on you.", left + 10, top + H - 12, 0xFF606060, false);
        super.render(g, mouseX, mouseY, partialTicks);

        if (hovered >= 0) {
            ShopCatalog.Entry e = shown.get(hovered);
            List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(minecraft, stacks.get(hovered)));
            lines.add(Component.empty());
            lines.add(Component.literal(String.format("Price: %,d rubies", e.price())).withStyle(funds >= e.price() ? ChatFormatting.GREEN : ChatFormatting.RED));
            lines.add(Component.literal(String.format("Shift-click: 5 for %,d", e.price() * 5L)).withStyle(ChatFormatting.GRAY));
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }
}
