package com.piratecrew.client;

import com.piratecrew.entity.PirateTier;
import com.piratecrew.network.BountyBoardPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The bounty board: wanted posters, six to a page, biggest bounty first. */
public class BountyBoardScreen extends Screen {
    private static final int COLS = 3, ROWS = 2, PER_PAGE = COLS * ROWS;
    private static final int PW = 84, PH = 122, GAP = 10, PAD = 12, HEADER = 22, FOOTER = 24;
    private static final int W = PAD * 2 + COLS * PW + (COLS - 1) * GAP;
    private static final int H = HEADER + PAD + ROWS * PH + (ROWS - 1) * GAP + FOOTER;

    private final List<BountyBoardPacket.Poster> posters;
    private final Map<UUID, PosterArt.Face> faces = new HashMap<>();
    private int page = 0;
    private int left, top;
    private Button prev, next;

    public BountyBoardScreen(List<BountyBoardPacket.Poster> posters) {
        super(Component.literal("Bounty Board"));
        this.posters = posters;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int pages() {
        return Math.max(1, (posters.size() + PER_PAGE - 1) / PER_PAGE);
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = Math.max(2, (height - H) / 2);
        int by = top + H - FOOTER + 4;
        prev = addRenderableWidget(Button.builder(Component.literal("<"), b -> { page--; update(); })
                .bounds(left + PAD, by, 20, 16).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> { page++; update(); })
                .bounds(left + W - PAD - 20, by, 20, 16).build());
        update();
    }

    private void update() {
        page = Math.max(0, Math.min(page, pages() - 1));
        prev.active = page > 0;
        next.active = page < pages() - 1;
    }

    private int posterX(int i) {
        return left + PAD + (i % COLS) * (PW + GAP);
    }

    private int posterY(int i) {
        return top + HEADER + (i / COLS) * (PH + GAP);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        renderBackground(g);
        // Wooden frame and cork board
        g.fill(left - 3, top - 3, left + W + 3, top + H + 3, 0xFF2E1B0C);
        g.fill(left, top, left + W, top + H, 0xFF6B4A2B);
        g.fill(left + 4, top + HEADER - 4, left + W - 4, top + H - FOOTER, 0xFFA07A4C);
        g.drawCenteredString(font, Component.literal("☠ BOUNTY BOARD ☠").withStyle(ChatFormatting.BOLD), left + W / 2, top + 6, 0xFFE8C46A);

        if (posters.isEmpty()) {
            g.drawCenteredString(font, "No bounties yet.", left + W / 2, top + H / 2 - 10, 0xFFFFFFFF);
            g.drawCenteredString(font, "Crews earn them by sinking rival pirates.", left + W / 2, top + H / 2 + 2, 0xFFE0D0B0);
        }

        BountyBoardPacket.Poster hovered = null;
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < posters.size(); i++) {
            BountyBoardPacket.Poster p = posters.get(start + i);
            int x = posterX(i), y = posterY(i);
            PosterArt.Face face = faces.computeIfAbsent(p.id(), k -> PosterArt.faceFor(p));
            PosterArt.draw(g, font, p, face, x, y, PW, PH);
            // pin
            g.fill(x + PW / 2 - 2, y + 1, x + PW / 2 + 2, y + 5, 0xFFB01E1E);
            g.fill(x + PW / 2 - 1, y + 2, x + PW / 2, y + 3, 0xFFFF8080);
            if (mouseX >= x && mouseX < x + PW && mouseY >= y && mouseY < y + PH) hovered = p;
        }

        g.drawCenteredString(font, (page + 1) + " / " + pages(), left + W / 2, top + H - FOOTER + 8, 0xFFE0D0B0);
        super.render(g, mouseX, mouseY, partialTicks);

        if (hovered != null) g.renderComponentTooltip(font, tooltip(hovered), mouseX, mouseY);
    }

    private List<Component> tooltip(BountyBoardPacket.Poster p) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(p.name()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        lines.add(Component.literal("Bounty: " + String.format("%,d", p.amount()) + " rubies").withStyle(ChatFormatting.RED));
        if (p.npc()) {
            PirateTier t = PirateTier.byId(p.tier());
            lines.add(Component.literal(t.label + "-tier pirate").withStyle(t.color));
        } else {
            lines.add(Component.literal("Player").withStyle(ChatFormatting.AQUA));
        }
        lines.add(Component.literal(p.crewName().isEmpty() ? "No crew" : "Crew: " + p.crewName()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Kills: " + p.playerKills() + " players, " + p.pirateKills() + " pirates").withStyle(ChatFormatting.GRAY));
        return lines;
    }
}
