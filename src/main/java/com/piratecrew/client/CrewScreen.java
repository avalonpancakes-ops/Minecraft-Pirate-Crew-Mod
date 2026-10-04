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

import java.util.List;
import java.util.UUID;

/** Crew management: members, ranks, invites, icon, name. Opened with J (rebindable) or /crew. */
public class CrewScreen extends Screen {
    private static final int W = 260, H = 230;
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

    private void initCrew(CrewSyncPacket d) {
        CrewRole me = CrewRole.byId(d.myRole);
        boolean captain = me == CrewRole.CAPTAIN;
        boolean officer = captain || me == CrewRole.VICE_CAPTAIN;
        UUID self = minecraft != null && minecraft.player != null ? minecraft.player.getUUID() : null;

        int pages = Math.max(1, (d.members.size() + ROWS - 1) / ROWS);
        page = Math.min(page, pages - 1);
        long vices = d.members.stream().filter(m -> !m.npc() && m.role() == CrewRole.VICE_CAPTAIN.ordinal()).count();

        List<CrewSyncPacket.Member> members = d.members;
        for (int i = 0; i < ROWS; i++) {
            int idx = page * ROWS + i;
            if (idx >= members.size()) break;
            CrewSyncPacket.Member m = members.get(idx);
            int rowY = top + 39 + i * ROW_H;
            int bx = left + W - 12; // buttons are laid out right-to-left

            if (m.id().equals(self)) continue;

            if (m.npc()) {
                if (officer) {
                    bx -= 30;
                    addRenderableWidget(small("Kick", bx, rowY, 30, b -> { if (confirm("kick" + m.id(), b)) send(Action.KICK, "", m.id()); }));
                }
                continue;
            }

            CrewRole r = CrewRole.byId(m.role());
            boolean canKick = captain || (me == CrewRole.VICE_CAPTAIN && r == CrewRole.MEMBER);
            if (canKick) {
                bx -= 30;
                addRenderableWidget(small("Kick", bx, rowY, 30, b -> { if (confirm("kick" + m.id(), b)) send(Action.KICK, "", m.id()); }));
            }
            if (captain) {
                bx -= 34;
                addRenderableWidget(small("Capt.", bx, rowY, 34, b -> { if (confirm("capt" + m.id(), b)) send(Action.MAKE_CAPTAIN, "", m.id()); }));
                if (r == CrewRole.VICE_CAPTAIN) {
                    bx -= 40;
                    addRenderableWidget(small("Demote", bx, rowY, 40, b -> send(Action.DEMOTE, "", m.id())));
                } else if (vices < 2) {
                    bx -= 40;
                    addRenderableWidget(small("+Vice", bx, rowY, 40, b -> send(Action.PROMOTE, "", m.id())));
                }
            }
        }

        // Pager
        int pagerY = top + 39 + ROWS * ROW_H + 4;
        Button prev = addRenderableWidget(small("<", left + 10, pagerY, 20, b -> { page--; rebuild(); }));
        Button next = addRenderableWidget(small(">", left + 90, pagerY, 20, b -> { page++; rebuild(); }));
        prev.active = page > 0;
        next.active = page < pages - 1;

        int y1 = top + 182, y2 = top + 204;
        if (officer) {
            inviteBox = new EditBox(font, left + 10, y1, 120, 18, Component.literal("Player name"));
            inviteBox.setMaxLength(16);
            inviteBox.setValue(inviteText);
            inviteBox.setHint(Component.literal("Player to invite...").withStyle(ChatFormatting.DARK_GRAY));
            addRenderableWidget(inviteBox);
            addRenderableWidget(Button.builder(Component.literal("Invite"), b -> {
                send(Action.INVITE, inviteBox.getValue(), null);
                inviteBox.setValue("");
            }).bounds(left + 134, y1 - 1, 50, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Set Icon"), b -> send(Action.SET_ICON, "", null))
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Uses the item in your main hand as the crew icon")))
                    .bounds(left + 188, y1 - 1, 62, 20).build());
        }
        if (captain) {
            renameBox = new EditBox(font, left + 10, y2, 120, 18, Component.literal("New name"));
            renameBox.setMaxLength(24);
            renameBox.setValue(renameText);
            renameBox.setHint(Component.literal("Rename crew...").withStyle(ChatFormatting.DARK_GRAY));
            addRenderableWidget(renameBox);
            addRenderableWidget(Button.builder(Component.literal("Rename"), b -> send(Action.RENAME, renameBox.getValue(), null))
                    .bounds(left + 134, y2 - 1, 50, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Disband").withStyle(ChatFormatting.RED), b -> {
                if (confirm("disband", b)) send(Action.DISBAND, "", null);
            }).bounds(left + 188, y2 - 1, 62, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Leave Crew"), b -> {
            if (confirm("leave", b)) send(Action.LEAVE, "", null);
        }).bounds(left + W - 72, pagerY - 2, 62, 18).build());
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
        long players = d.members.stream().filter(m -> !m.npc()).count();
        CrewRole me = CrewRole.byId(d.myRole);
        String counts = "Players " + players + "/" + d.maxPlayers + "   Crew " + d.members.size() + "/" + d.maxSize;
        g.drawString(font, counts, left + 32, top + 20, GuiDraw.TEXT, false);
        g.drawString(font, Component.literal(me.title).withStyle(me.color == ChatFormatting.WHITE ? ChatFormatting.DARK_GRAY : ChatFormatting.DARK_RED),
                left + W - 10 - font.width(me.title), top + 9, GuiDraw.TEXT, false);

        // Member list
        GuiDraw.inset(g, left + 8, top + 37, W - 16, ROWS * ROW_H + 2);
        UUID self = minecraft != null && minecraft.player != null ? minecraft.player.getUUID() : null;
        for (int i = 0; i < ROWS; i++) {
            int idx = page * ROWS + i;
            int rowY = top + 38 + i * ROW_H;
            GuiDraw.row(g, left + 9, rowY, W - 18, ROW_H, i % 2 == 1);
            if (idx >= d.members.size()) continue;
            CrewSyncPacket.Member m = d.members.get(idx);

            Component tag;
            if (m.npc()) {
                PirateTier t = PirateTier.byId(m.tier());
                tag = Component.literal("[" + t.label + "]").withStyle(t.color, ChatFormatting.BOLD);
            } else {
                CrewRole r = CrewRole.byId(m.role());
                tag = Component.literal(r == CrewRole.CAPTAIN ? "★" : r == CrewRole.VICE_CAPTAIN ? "☆" : "•")
                        .withStyle(r == CrewRole.MEMBER ? ChatFormatting.DARK_GRAY : ChatFormatting.GOLD);
            }
            g.drawString(font, tag, left + 13, rowY + 4, 0xFFFFFFFF, true);

            int nameColor = m.npc() ? 0xFF3A3A3A : (m.online() ? 0xFF1E5A1E : 0xFF6A6A6A);
            String name = m.name() + (m.id().equals(self) ? " (you)" : "");
            g.drawString(font, font.plainSubstrByWidth(name, 96), left + 32, rowY + 4, nameColor, false);

            String sub = m.npc() ? "Pirate" : CrewRole.byId(m.role()).title + (m.online() ? "" : " - offline");
            g.drawString(font, font.plainSubstrByWidth(sub, 70), left + 132, rowY + 4, 0xFF555555, false);
        }

        int pages = Math.max(1, (d.members.size() + ROWS - 1) / ROWS);
        int pagerY = top + 39 + ROWS * ROW_H + 4;
        g.drawCenteredString(font, (page + 1) + "/" + pages, left + 60, pagerY + 4, 0xFFFFFF);

        if (inviteBox == null && renameBox == null) {
            g.drawString(font, "Only the captain and vice captains", left + 10, top + 186, 0xFF707070, false);
            g.drawString(font, "can invite, recruit and dismiss.", left + 10, top + 196, 0xFF707070, false);
        }

        if (mouseX >= left + 10 && mouseX < left + 26 && mouseY >= top + 9 && mouseY < top + 25 && !d.icon.isEmpty()) {
            g.renderTooltip(font, d.icon, mouseX, mouseY);
        }
    }
}
