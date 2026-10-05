package com.piratecrew.client;

import com.piratecrew.crew.CrewRole;
import com.piratecrew.entity.PirateTier;
import com.piratecrew.network.CrewActionPacket;
import com.piratecrew.network.CrewActionPacket.Action;
import com.piratecrew.network.CrewSyncPacket;
import com.piratecrew.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Crew management: members, ranks, invites, icon, name. Opened with J (rebindable) or /crew. */
public class CrewScreen extends Screen {
    private static final int W = 260, H = 246;
    private static final int LIST_Y = 37, PAGER_Y = LIST_Y + 8 * 16 + 5, ROW1_Y = PAGER_Y + 22, ROW2_Y = ROW1_Y + 24;
    private static final int ROWS = 8, ROW_H = 16;

    private int left, top;
    private int seenVersion = -1;
    private int page = 0;
    private String confirmKey = null;

    private EditBox nameBox, inviteBox, renameBox;
    private String nameText = "", inviteText = "", renameText = "";

    public CrewScreen() {
        super(Component.literal("Pirate Crew"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static void send(Action a, String text, UUID target) {
        ModNetwork.sendToServer(new CrewActionPacket(a, text, target));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        seenVersion = ClientCrewData.version();
        CrewSyncPacket d = ClientCrewData.get();
        if (d == null) return;
        if (d.hasCrew) initCrew(d);
        else initNoCrew(d);
    }

    private void rebuild() {
        if (nameBox != null) nameText = nameBox.getValue();
        if (inviteBox != null) inviteText = inviteBox.getValue();
        if (renameBox != null) renameText = renameBox.getValue();
        nameBox = inviteBox = renameBox = null;
        clearWidgets();
        init();
    }

    @Override
    public void tick() {
        super.tick();
        if (ClientCrewData.version() != seenVersion) rebuild();
        if (nameBox != null) nameBox.tick();
        if (inviteBox != null) inviteBox.tick();
        if (renameBox != null) renameBox.tick();
    }

    /** Two-click confirmation for drastic buttons. */
    private boolean confirm(String key, Button b) {
        if (key.equals(confirmKey)) {
            confirmKey = null;
            return true;
        }
        confirmKey = key;
        b.setMessage(Component.literal("Sure?").withStyle(ChatFormatting.RED));
        return false;
    }

    // ------------------------------------------------------------------ no crew

    private void initNoCrew(CrewSyncPacket d) {
        nameBox = new EditBox(font, left + 10, top + 52, 160, 18, Component.literal("Crew name"));
        nameBox.setMaxLength(24);
        nameBox.setValue(nameText);
        nameBox.setHint(Component.literal("Crew name...").withStyle(ChatFormatting.DARK_GRAY));
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(Component.literal("Create Crew"), b -> send(Action.CREATE, nameBox.getValue(), null))
                .bounds(left + 176, top + 51, 74, 20).build());

        int y = top + 106;
        for (int i = 0; i < Math.min(5, d.invites.size()); i++) {
            CrewSyncPacket.InviteInfo inv = d.invites.get(i);
            addRenderableWidget(Button.builder(Component.literal("Accept").withStyle(ChatFormatting.GREEN), b -> send(Action.ACCEPT, "", inv.crewId()))
                    .bounds(left + 146, y + i * 20, 50, 18).build());
            addRenderableWidget(Button.builder(Component.literal("Decline"), b -> send(Action.DECLINE, "", inv.crewId()))
                    .bounds(left + 200, y + i * 20, 50, 18).build());
        }
    }

    // ------------------------------------------------------------------ crew

    /** A button shown at the right end of a member row. */
    private record RowButton(String label, int width, Button.OnPress press) {}

    /** The buttons a member's row gets, given who is looking. Shared by init() and render(). */
    private List<RowButton> rowButtons(CrewSyncPacket d, CrewSyncPacket.Member m) {
        List<RowButton> out = new ArrayList<>();
        UUID self = minecraft != null && minecraft.player != null ? minecraft.player.getUUID() : null;
        if (m.id().equals(self)) return out;
        CrewRole me = CrewRole.byId(d.myRole);
        boolean captain = me == CrewRole.CAPTAIN;
        boolean officer = captain || me == CrewRole.VICE_CAPTAIN;
        CrewRole r = CrewRole.byId(m.role());
        if (r == CrewRole.CAPTAIN) return out;
        long vices = d.members.stream().filter(x -> x.role() == CrewRole.VICE_CAPTAIN.ordinal()).count();

        // Right-to-left order: Kick, Captain, Vice/Demote
        boolean canKick = captain || (officer && r == CrewRole.MEMBER);
        if (canKick) out.add(new RowButton("Kick", 28, b -> { if (confirm("kick" + m.id(), b)) send(Action.KICK, "", m.id()); }));
        if (captain && !m.npc()) out.add(new RowButton("Capt", 30, b -> { if (confirm("capt" + m.id(), b)) send(Action.MAKE_CAPTAIN, "", m.id()); }));
        if (captain) {
            if (r == CrewRole.VICE_CAPTAIN) out.add(new RowButton("Demote", 38, b -> send(Action.DEMOTE, "", m.id())));
            else if (vices < 2) out.add(new RowButton("+Vice", 34, b -> send(Action.PROMOTE, "", m.id())));
        }
        return out;
    }

    /** x where a row's buttons start (left edge of the leftmost one). */
    private int buttonsStart(List<RowButton> buttons) {
        int x = left + W - 12;
        for (RowButton rb : buttons) x -= rb.width() + 2;
        return x;
    }

    private void initCrew(CrewSyncPacket d) {
        CrewRole me = CrewRole.byId(d.myRole);
        boolean captain = me == CrewRole.CAPTAIN;
        boolean officer = captain || me == CrewRole.VICE_CAPTAIN;

        int pages = Math.max(1, (d.members.size() + ROWS - 1) / ROWS);
        page = Math.min(page, pages - 1);

        for (int i = 0; i < ROWS; i++) {
            int idx = page * ROWS + i;
            if (idx >= d.members.size()) break;
            CrewSyncPacket.Member m = d.members.get(idx);
            int rowY = top + LIST_Y + 1 + i * ROW_H;
            int bx = left + W - 12;
            for (RowButton rb : rowButtons(d, m)) {
                bx -= rb.width() + 2;
                addRenderableWidget(small(rb.label(), bx + 2, rowY, rb.width(), rb.press()));
            }
        }

        // Pager + leave
        int py = top + PAGER_Y;
        Button prev = addRenderableWidget(small("<", left + 10, py - 1, 20, b -> { page--; rebuild(); }));
        Button next = addRenderableWidget(small(">", left + 74, py - 1, 20, b -> { page++; rebuild(); }));
        prev.active = page > 0;
        next.active = page < pages - 1;
        addRenderableWidget(small("Leave Crew", left + W - 72, py - 1, 62, b -> {
            if (confirm("leave", b)) send(Action.LEAVE, "", null);
        }));

        int y1 = top + ROW1_Y, y2 = top + ROW2_Y;
        if (officer) {
            inviteBox = new EditBox(font, left + 10, y1, 124, 18, Component.literal("Player name"));
            inviteBox.setMaxLength(16);
            inviteBox.setValue(inviteText);
            inviteBox.setHint(Component.literal("Player to invite...").withStyle(ChatFormatting.DARK_GRAY));
            addRenderableWidget(inviteBox);
            addRenderableWidget(Button.builder(Component.literal("Invite"), b -> {
                send(Action.INVITE, inviteBox.getValue(), null);
                inviteBox.setValue("");
            }).bounds(left + 138, y1 - 1, 52, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Set Icon"), b -> send(Action.SET_ICON, "", null))
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Uses the item in your main hand as the crew icon")))
                    .bounds(left + 194, y1 + 1, 56, 16).build());
        }
        if (captain) {
            renameBox = new EditBox(font, left + 10, y2, 124, 18, Component.literal("New name"));
            renameBox.setMaxLength(24);
            renameBox.setValue(renameText);
            renameBox.setHint(Component.literal("Rename crew...").withStyle(ChatFormatting.DARK_GRAY));
            addRenderableWidget(renameBox);
            addRenderableWidget(Button.builder(Component.literal("Rename"), b -> send(Action.RENAME, renameBox.getValue(), null))
                    .bounds(left + 138, y2 - 1, 52, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Disband").withStyle(ChatFormatting.RED), b -> {
                if (confirm("disband", b)) send(Action.DISBAND, "", null);
            }).bounds(left + 194, y2 - 1, 56, 20).build());
        }
    }

    private Button small(String label, int x, int y, int w, Button.OnPress press) {
        return Button.builder(Component.literal(label), press).bounds(x, y + 1, w, 14).build();
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        renderBackground(g);
        GuiDraw.panel(g, left, top, W, H);
        CrewSyncPacket d = ClientCrewData.get();
        if (d == null) {
            g.drawCenteredString(font, "Loading crew...", left + W / 2, top + H / 2, 0xFFFFFF);
        } else if (d.hasCrew) {
            renderCrew(g, d, mouseX, mouseY);
        } else {
            renderNoCrew(g, d);
        }
        super.render(g, mouseX, mouseY, partialTicks);
    }

    private void renderNoCrew(GuiGraphics g, CrewSyncPacket d) {
        g.drawString(font, Component.literal("⚓ Pirate Crew").withStyle(ChatFormatting.BOLD), left + 10, top + 10, GuiDraw.TEXT, false);
        g.drawString(font, "You don't sail with a crew yet.", left + 10, top + 26, GuiDraw.TEXT, false);
        g.drawString(font, "Name one and become its Captain:", left + 10, top + 38, GuiDraw.TEXT, false);

        g.drawString(font, Component.literal("Invitations").withStyle(ChatFormatting.BOLD), left + 10, top + 92, GuiDraw.TEXT, false);
        if (d.invites.isEmpty()) {
            g.drawString(font, "None right now.", left + 10, top + 110, 0xFF707070, false);
        }
        for (int i = 0; i < Math.min(5, d.invites.size()); i++) {
            CrewSyncPacket.InviteInfo inv = d.invites.get(i);
            int y = top + 106 + i * 20;
            GuiDraw.row(g, left + 8, y - 1, 134, 20, i % 2 == 1);
            g.drawString(font, font.plainSubstrByWidth(inv.crewName(), 128), left + 11, y + 1, 0xFF5A3A00, false);
            g.drawString(font, font.plainSubstrByWidth("from " + inv.inviter(), 128), left + 11, y + 10, 0xFF555555, false);
        }

        g.drawString(font, "Tip: recruiting a pirate at a village bar", left + 10, top + H - 26, 0xFF707070, false);
        g.drawString(font, "with rubies creates a crew for you.", left + 10, top + H - 16, 0xFF707070, false);
    }

    private void renderCrew(GuiGraphics g, CrewSyncPacket d, int mouseX, int mouseY) {
        // Header: icon + name + counts
        GuiDraw.slot(g, left + 10, top + 9);
        g.renderItem(d.icon, left + 10, top + 9);
        g.drawString(font, Component.literal(d.crewName).withStyle(ChatFormatting.BOLD), left + 32, top + 9, 0xFF5A3A00, false);
        if (d.emperorRank > 0) {
            int nx = left + 32 + font.width(Component.literal(d.crewName).withStyle(ChatFormatting.BOLD)) + 5;
            Component title = Component.literal("\u265B Emperor #" + d.emperorRank).withStyle(ChatFormatting.GOLD);
            g.drawString(font, title, nx, top + 9, 0xFFC08000, false);
            if (mouseX >= nx && mouseX < nx + font.width(title) && mouseY >= top + 8 && mouseY < top + 18) {
                g.renderComponentTooltip(font, java.util.List.of(
                        Component.literal("Emperor of the Sea #" + d.emperorRank).withStyle(ChatFormatting.GOLD),
                        Component.literal("Every player: permanent Strength I and Resistance I").withStyle(ChatFormatting.GRAY),
                        Component.literal("Captain and vice captains: Strength III and Resistance II").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
            }
        }
        long players = d.members.stream().filter(m -> !m.npc()).count();
        CrewRole me = CrewRole.byId(d.myRole);
        String counts = "Players " + players + "/" + d.maxPlayers + "   Crew " + d.members.size() + "/" + d.maxSize;
        g.drawString(font, counts, left + 32, top + 20, GuiDraw.TEXT, false);
        int totalBounty = d.members.stream().mapToInt(CrewSyncPacket.Member::bounty).sum();
        if (totalBounty > 0) {
            String b = String.format("%,d", totalBounty);
            int bx = left + W - 10 - font.width(b);
            g.drawString(font, b, bx, top + 20, 0xFFAA0000, false);
            g.pose().pushPose();
            g.pose().translate(bx - 11, top + 19, 0);
            g.pose().scale(0.625F, 0.625F, 1);
            g.renderItem(new net.minecraft.world.item.ItemStack(com.piratecrew.registry.ModItems.RUBY.get()), 0, 0);
            g.pose().popPose();
        }
        g.drawString(font, Component.literal(me.title).withStyle(me == CrewRole.MEMBER ? ChatFormatting.DARK_GRAY : ChatFormatting.DARK_RED),
                left + W - 10 - font.width(me.title), top + 9, GuiDraw.TEXT, false);

        // Member list
        GuiDraw.inset(g, left + 8, top + LIST_Y, W - 16, ROWS * ROW_H + 2);
        UUID self = minecraft != null && minecraft.player != null ? minecraft.player.getUUID() : null;
        for (int i = 0; i < ROWS; i++) {
            int idx = page * ROWS + i;
            int rowY = top + LIST_Y + 1 + i * ROW_H;
            GuiDraw.row(g, left + 9, rowY, W - 18, ROW_H, i % 2 == 1);
            if (idx >= d.members.size()) continue;
            CrewSyncPacket.Member m = d.members.get(idx);
            CrewRole r = CrewRole.byId(m.role());

            // Left tag: tier for pirates, rank star for players
            int nameX;
            if (m.npc()) {
                PirateTier t = PirateTier.byId(m.tier());
                Component tag = Component.literal("[" + t.label + "]").withStyle(t.color, ChatFormatting.BOLD);
                g.drawString(font, tag, left + 12, rowY + 4, 0xFFFFFFFF, true);
                nameX = Math.max(left + 32, left + 12 + font.width(tag) + 3);
            } else {
                Component star = Component.literal(r == CrewRole.CAPTAIN ? "\u2605" : r == CrewRole.VICE_CAPTAIN ? "\u2606" : "\u2022")
                        .withStyle(r == CrewRole.MEMBER ? ChatFormatting.DARK_GRAY : ChatFormatting.GOLD);
                g.drawString(font, star, left + 14, rowY + 4, 0xFFFFFFFF, true);
                nameX = left + 26;
            }

            String sub = (m.npc() ? (r == CrewRole.VICE_CAPTAIN ? "Vice Captain" : "Pirate") : r.title)
                    + (!m.npc() && !m.online() ? " (offline)" : "");
            String name = m.name() + (m.id().equals(self) ? " (you)" : "");
            int right = buttonsStart(rowButtons(d, m)) - 4;
            int subW = font.width(sub);
            int nameMax = right - nameX - subW - 8;
            if (nameMax < 50) { sub = ""; nameMax = right - nameX; }

            int nameColor = m.npc() ? 0xFF3A3A3A : (m.online() ? 0xFF1E5A1E : 0xFF6A6A6A);
            if (m.npc() && r == CrewRole.VICE_CAPTAIN) nameColor = 0xFF7A4A00;
            g.drawString(font, font.plainSubstrByWidth(name, nameMax), nameX, rowY + 4, nameColor, false);
            if (!sub.isEmpty()) g.drawString(font, sub, right - font.width(sub), rowY + 4, 0xFF555555, false);
        }

        int pages = Math.max(1, (d.members.size() + ROWS - 1) / ROWS);
        g.drawCenteredString(font, (page + 1) + "/" + pages, left + 52, top + PAGER_Y + 3, 0xFFFFFF);

        // Hovering a member's name shows their bounty and kills
        for (int i = 0; i < ROWS; i++) {
            int idx = page * ROWS + i;
            if (idx >= d.members.size()) break;
            int rowY = top + LIST_Y + 1 + i * ROW_H;
            CrewSyncPacket.Member m = d.members.get(idx);
            int right = buttonsStart(rowButtons(d, m)) - 4;
            if (mouseX >= left + 9 && mouseX < right && mouseY >= rowY && mouseY < rowY + ROW_H) {
                List<Component> lines = new ArrayList<>();
                lines.add(Component.literal(m.name()).withStyle(ChatFormatting.GOLD));
                lines.add(Component.literal(m.bounty() > 0 ? "Bounty: " + String.format("%,d", m.bounty()) + " rubies" : "No bounty")
                        .withStyle(m.bounty() > 0 ? ChatFormatting.RED : ChatFormatting.GRAY));
                lines.add(Component.literal("Kills: " + m.playerKills() + " players, " + m.pirateKills() + " pirates").withStyle(ChatFormatting.GRAY));
                g.renderComponentTooltip(font, lines, mouseX, mouseY);
            }
        }

        if (inviteBox == null && renameBox == null) {
            g.drawString(font, "Only the captain and vice captains", left + 10, top + ROW1_Y + 2, 0xFF707070, false);
            g.drawString(font, "can invite, recruit and dismiss.", left + 10, top + ROW1_Y + 12, 0xFF707070, false);
        }

        if (mouseX >= left + 10 && mouseX < left + 26 && mouseY >= top + 9 && mouseY < top + 25 && !d.icon.isEmpty()) {
            g.renderTooltip(font, d.icon, mouseX, mouseY);
        }
    }
}
