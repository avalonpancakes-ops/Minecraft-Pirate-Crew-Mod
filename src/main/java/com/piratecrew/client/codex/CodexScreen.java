package com.piratecrew.client.codex;

import com.piratecrew.client.GuiDraw;
import com.piratecrew.client.PirateButton;
import com.piratecrew.codex.ShowcaseTools;
import com.piratecrew.network.CodexActionPacket;
import com.piratecrew.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The Captain's Log: a night-chart guidebook to everything in Pirate Crew. Sections down the left
 * (gear, places, foes, bosses, pacts), the chosen page on the right, and for operators a Showcase
 * page of one-click tools. Animated: rolling waves on the header, a shimmering title, bobbing icons
 * and pulsing brass highlights.
 */
public class CodexScreen extends Screen {
    private static final int SIDEBAR_W = 124;
    private static final int ROW_H = 13;
    private static final int HEADER_H = 24;

    // remembered between openings
    private static int lastSection = 0, lastEntry = 0;
    private static int lastMode = 0;   // 0 an entry, 1 showcase, 2 voyage goals

    private int left, top, w, h;
    private int section = lastSection, entry = lastEntry;
    private int mode = lastMode;
    private double sideScroll, pageScroll;
    private int pageContentH;

    private record Row(int section, int entry, boolean header, int kind) {}

    private final List<Row> rows = new ArrayList<>();

    public CodexScreen() {
        super(Component.literal("Captain's Log"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new CodexScreen());
    }

    /** Open on a given page (used for screenshots); showcase pages are forced visible. */
    public static void openAt(int section, int entry, boolean showcase) {
        lastSection = section;
        lastEntry = entry;
        lastMode = showcase ? 1 : 0;
        forceShowcase = true;
        open();
    }

    /** Open on the Voyage Goals page. */
    public static void openGoals() {
        lastMode = 2;
        open();
    }

    private static boolean forceShowcase;

    private boolean isOp() {
        return forceShowcase || (minecraft != null && minecraft.player != null && minecraft.player.hasPermissions(2));
    }

    @Override
    protected void init() {
        w = Math.min(width - 12, 430);
        h = Math.min(height - 12, 256);
        left = (width - w) / 2;
        top = (height - h) / 2;
        rows.clear();
        var secs = CodexContent.sections();
        rows.add(new Row(-2, -1, true, 2));
        rows.add(new Row(-2, 0, false, 2));
        for (int s = 0; s < secs.size(); s++) {
            rows.add(new Row(s, -1, true, 0));
            for (int e = 0; e < secs.get(s).entries().size(); e++) rows.add(new Row(s, e, false, 0));
        }
        if (isOp()) {
            rows.add(new Row(-1, -1, true, 1));
            rows.add(new Row(-1, 0, false, 1));
        } else if (mode == 1) {
            mode = 0;
        }
        if (section >= secs.size()) section = 0;
        if (entry >= secs.get(section).entries().size()) entry = 0;
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            boolean sel = !r.header && (r.kind != 0 ? mode == r.kind : mode == 0 && r.section == section && r.entry == entry);
            if (sel) {
                int sh = h - HEADER_H - 10;
                sideScroll = Math.max(0, i * ROW_H - sh / 2);
            }
        }
        addRenderableWidget(Button.builder(Component.literal("✕"), b -> onClose())
                .bounds(left + w - 20, top + 5, 14, 14).build(PirateButton::new));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        lastSection = section;
        lastEntry = entry;
        lastMode = mode;
        super.onClose();
    }

    private float time(float partial) {
        return (System.currentTimeMillis() % 1_000_000L) / 50F;
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        float t = time(partial);
        drawFrame(g, t);
        drawSidebar(g, mouseX, mouseY, t);
        if (mode == 1) drawShowcase(g, mouseX, mouseY, t);
        else if (mode == 2) drawGoals(g, mouseX, mouseY, t);
        else drawEntry(g, t);
        super.render(g, mouseX, mouseY, partial);
        if (mode == 1) drawToolTooltip(g, mouseX, mouseY);
        else if (mode == 0) drawStripTooltip(g, mouseX, mouseY);
    }

    private void drawFrame(GuiGraphics g, float t) {
        GuiDraw.tile(g, GuiDraw.WOOD, left, top, w, h, 64);
        g.fill(left, top, left + w, top + 1, 0xFF1A0F08);
        g.fill(left, top + h - 1, left + w, top + h, 0xFF1A0F08);
        g.fill(left, top, left + 1, top + h, 0xFF1A0F08);
        g.fill(left + w - 1, top, left + w, top + h, 0xFF1A0F08);
        // header: waves rolling along the bottom of the plank
        for (int x = left + 4; x < left + w - 4; x += 2) {
            int y = top + HEADER_H - 6 + Math.round(Mth.sin((x - left + t * 1.6F) / 7F) * 1.5F);
            g.fill(x, y, x + 2, y + 2, 0xFF2FB3B5);
            g.fill(x, y + 2, x + 2, top + HEADER_H - 2, 0xFF125A66);
            if (((x - left) / 2 + (int) (t / 3)) % 23 == 0) g.fill(x, y - 1, x + 2, y, 0xFFE8FFFA);
        }
        // shimmering title
        String title = "⚓ CAPTAIN'S LOG ⚓";
        int tw = font.width(title) * 3 / 2;
        int tx = left + (w - tw) / 2, ty = top + 4;
        g.pose().pushPose();
        g.pose().translate(tx, ty, 0);
        g.pose().scale(1.5F, 1.5F, 1F);
        int cx = 0;
        for (int i = 0; i < title.length(); i++) {
            String ch = String.valueOf(title.charAt(i));
            float k = 0.5F + 0.5F * Mth.sin(t * 0.15F - i * 0.45F);
            int col = lerp(0xFFC8901E, 0xFFFFF0B0, k);
            g.drawString(font, Component.literal(ch).withStyle(ChatFormatting.BOLD), cx, 0, col, true);
            cx += font.width(Component.literal(ch).withStyle(ChatFormatting.BOLD));
        }
        g.pose().popPose();
        // the night-chart page
        int px = left + 4, py = top + HEADER_H, pw = w - 8, ph = h - HEADER_H - 4;
        g.fill(px, py, px + pw, py + ph, 0xFF0E1C28);
        for (int gx = px + 8; gx < px + pw; gx += 16) g.fill(gx, py, gx + 1, py + ph, 0x1830C0D0);
        for (int gy = py + 8; gy < py + ph; gy += 16) g.fill(px, gy, px + pw, gy + 1, 0x1830C0D0);
        g.fill(px, py, px + pw, py + 1, 0xFF2A1A0C);
        compassRose(g, left + w - 40, top + h - 40, t);
        // divider between sidebar and page
        g.fill(left + 4 + SIDEBAR_W, py, left + 5 + SIDEBAR_W, py + ph, 0xFF6A4A2A);
    }

    /** A faint eight-point compass rose watermarked on the chart, its north needle glinting. */
    private void compassRose(GuiGraphics g, int cx, int cy, float t) {
        int col = 0x2830C0D0, dim = 0x1830C0D0;
        // ring
        for (int a = 0; a < 360; a += 4) {
            double r = Math.toRadians(a);
            int x = cx + (int) Math.round(Math.cos(r) * 19), y = cy + (int) Math.round(Math.sin(r) * 19);
            g.fill(x, y, x + 1, y + 1, col);
        }
        // cardinal points: tapering diamonds
        for (int d = 0; d <= 26; d++) {
            int half = Math.max(0, 3 - d * 3 / 26);
            g.fill(cx - half, cy - d, cx + half + 1, cy - d + 1, d > 2 ? lerp(0x40E8B84A, 0x90FFE070, 0.5F + 0.5F * Mth.sin(t * 0.1F)) : col);
            g.fill(cx - half, cy + d, cx + half + 1, cy + d + 1, col);
            g.fill(cx + d, cy - half, cx + d + 1, cy + half + 1, col);
            g.fill(cx - d, cy - half, cx - d + 1, cy + half + 1, col);
        }
        // diagonal points
        for (int d = 0; d <= 14; d++) {
            g.fill(cx + d, cy + d, cx + d + 1, cy + d + 1, dim);
            g.fill(cx - d, cy + d, cx - d + 1, cy + d + 1, dim);
            g.fill(cx + d, cy - d, cx + d + 1, cy - d + 1, dim);
            g.fill(cx - d, cy - d, cx - d + 1, cy - d + 1, dim);
        }
        g.drawString(font, "N", cx - 2, cy - 38, 0x60E8B84A, false);
    }

    private void drawSidebar(GuiGraphics g, int mouseX, int mouseY, float t) {
        int x0 = left + 6, y0 = top + HEADER_H + 3, sh = h - HEADER_H - 10;
        int contentH = rows.size() * ROW_H;
        sideScroll = Mth.clamp(sideScroll, 0, Math.max(0, contentH - sh));
        g.enableScissor(x0, y0, x0 + SIDEBAR_W - 4, y0 + sh);
        var secs = CodexContent.sections();
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            int y = y0 + i * ROW_H - (int) sideScroll;
            if (y < y0 - ROW_H || y > y0 + sh) continue;
            if (r.header) {
                int color = r.kind == 1 ? 0xFFE8B84A : r.kind == 2 ? 0xFF7FE0C8 : (0xFF000000 | secs.get(r.section).color());
                String label = r.kind == 1 ? "SHOWCASE" : r.kind == 2 ? "VOYAGE" : secs.get(r.section).title();
                g.drawString(font, "◆", x0 + 1, y + 3, color, false);
                g.drawString(font, Component.literal(label).withStyle(ChatFormatting.BOLD), x0 + 9, y + 3, color, false);
                int lx = x0 + 12 + font.width(Component.literal(label).withStyle(ChatFormatting.BOLD));
                if (lx < x0 + SIDEBAR_W - 8) g.fill(lx, y + 7, x0 + SIDEBAR_W - 8, y + 8, (color & 0x00FFFFFF) | 0x80000000);
                continue;
            }
            boolean selected = r.kind != 0 ? mode == r.kind : (mode == 0 && r.section == section && r.entry == entry);
            boolean hovered = mouseX >= x0 && mouseX < x0 + SIDEBAR_W - 6 && mouseY >= y && mouseY < y + ROW_H && mouseY >= y0 && mouseY < y0 + sh;
            if (selected) {
                float k = 0.5F + 0.5F * Mth.sin(t * 0.2F);
                g.fill(x0, y, x0 + SIDEBAR_W - 6, y + ROW_H, 0x60E8B84A);
                g.fill(x0, y, x0 + 2, y + ROW_H, lerp(0xFFC8901E, 0xFFFFE890, k));
            } else if (hovered) {
                g.fill(x0, y, x0 + SIDEBAR_W - 6, y + ROW_H, 0x30FFFFFF);
            }
            ItemStack icon = r.kind == 1 ? new ItemStack(net.minecraft.world.item.Items.SPYGLASS)
                    : r.kind == 2 ? new ItemStack(net.minecraft.world.item.Items.COMPASS)
                    : secs.get(r.section).entries().get(r.entry).icon().get();
            g.pose().pushPose();
            g.pose().translate(x0 + 4, y + 1, 0);
            g.pose().scale(0.6875F, 0.6875F, 1F);
            g.renderItem(icon, 0, 0);
            g.pose().popPose();
            String name = r.kind == 1 ? "Showcase" : r.kind == 2 ? "Voyage Goals" : secs.get(r.section).entries().get(r.entry).title();
            g.drawString(font, fit(name, SIDEBAR_W - 26, false), x0 + 18, y + 3, selected ? 0xFFFFF0C0 : 0xFFE0D2B4, false);
        }
        g.disableScissor();
        if (contentH > sh) scrollbar(g, x0 + SIDEBAR_W - 4, y0, sh, sideScroll, contentH);
    }

    /** Text cut to fit, with an ellipsis when it doesn't. */
    private String fit(String text, int width, boolean bold) {
        java.util.function.ToIntFunction<String> wOf = s -> bold ? font.width(Component.literal(s).withStyle(ChatFormatting.BOLD)) : font.width(s);
        if (wOf.applyAsInt(text) <= width) return text;
        String t = text;
        while (!t.isEmpty() && wOf.applyAsInt(t + "\u2026") > width) t = t.substring(0, t.length() - 1);
        return t + "\u2026";
    }

    private void scrollbar(GuiGraphics g, int x, int y, int h, double scroll, int contentH) {
        g.fill(x, y, x + 2, y + h, 0x40000000);
        int bar = Math.max(12, h * h / contentH);
        int by = y + (int) ((h - bar) * (scroll / Math.max(1, contentH - h)));
        g.fill(x, by, x + 2, by + bar, 0xFFC8901E);
    }

    private int pageX() {
        return left + SIDEBAR_W + 12;
    }

    private int pageW() {
        return w - SIDEBAR_W - 22;
    }

    // ------------------------------------------------------------------ living portraits of the foes

    private record Portrait(java.util.function.Supplier<? extends net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.LivingEntity>> type, int scale) {}

    private static final java.util.Map<String, Portrait> PORTRAITS = java.util.Map.of(
            "Commodore Graves", new Portrait(com.piratecrew.registry.ModEntities.COMMODORE, 26),
            "The Kraken", new Portrait(com.piratecrew.registry.ModEntities.KRAKEN, 8),
            "Tempest Admiral Sorel", new Portrait(com.piratecrew.registry.ModEntities.TEMPEST_ADMIRAL, 25),
            "The Leviathan", new Portrait(com.piratecrew.registry.ModEntities.LEVIATHAN, 5),
            "Fleet Admiral Vane", new Portrait(com.piratecrew.registry.ModEntities.FLEET_ADMIRAL, 22),
            "Marines", new Portrait(com.piratecrew.registry.ModEntities.MARINE, 30));
    private final java.util.Map<String, net.minecraft.world.entity.LivingEntity> portraitCache = new java.util.HashMap<>();

    private net.minecraft.world.entity.LivingEntity portrait(String title) {
        Portrait p = PORTRAITS.get(title);
        if (p == null || minecraft == null || minecraft.level == null) return null;
        return portraitCache.computeIfAbsent(title, k -> {
            var e = p.type().get().create(minecraft.level);
            if (e instanceof com.piratecrew.entity.boss.MarineBossEntity b) b.setupBoss();
            else if (e instanceof com.piratecrew.entity.MarineEntity m) m.setupMarine(com.piratecrew.entity.MarineEntity.Rank.CAPTAIN);
            return e;
        });
    }

    @Override
    public void tick() {
        super.tick();
        for (var e : portraitCache.values()) if (e != null) e.tickCount++;
    }

    private void drawPortrait(GuiGraphics g, net.minecraft.world.entity.LivingEntity mob, int x, int y, int w, int h, float t, int scale) {
        g.fill(x, y, x + w, y + h, 0xFF0A1620);
        g.fill(x, y, x + w, y + 1, 0xFFC8901E);
        g.fill(x, y + h - 1, x + w, y + h, 0xFF8A6420);
        g.fill(x, y, x + 1, y + h, 0xFFC8901E);
        g.fill(x + w - 1, y, x + w, y + h, 0xFF8A6420);
        float spin = (t * 1.2F) % 360F;
        mob.yBodyRot = spin;
        mob.yBodyRotO = spin;
        mob.setYRot(spin);
        mob.yHeadRot = spin;
        mob.yHeadRotO = spin;
        mob.setXRot(0);
        org.joml.Quaternionf pose = new org.joml.Quaternionf().rotateZ((float) Math.PI);
        org.joml.Quaternionf cam = new org.joml.Quaternionf().rotateX(12F * Mth.DEG_TO_RAD);
        pose.mul(cam);
        g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
        net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventory(g, x + w / 2, y + h - 10, scale, pose, cam, mob);
        g.disableScissor();
    }

    private void drawEntry(GuiGraphics g, float t) {
        var sec = CodexContent.sections().get(section);
        var e = sec.entries().get(entry);
        int x = pageX(), y0 = top + HEADER_H + 6, pw = pageW(), ph = h - HEADER_H - 14;
        var mob = portrait(e.title());
        if (mob != null) {
            int bw = 84, bh = ph - 50;
            drawPortrait(g, mob, x + pw - bw, y0 + 46, bw, bh, t, PORTRAITS.get(e.title()).scale());
            pw -= bw + 6;
        }
        // big bobbing icon in a brass ring
        int ix = x, iy = y0 + 2 + Math.round(Mth.sin(t * 0.12F) * 1.5F);
        ring(g, x - 2, y0, 38, t, 0xFF000000 | sec.color());
        g.pose().pushPose();
        g.pose().translate(ix + 1, iy + 1, 0);
        g.pose().scale(2F, 2F, 1F);
        g.renderItem(e.icon().get(), 0, 0);
        g.pose().popPose();
        // title and subtitle
        g.pose().pushPose();
        g.pose().translate(x + 44, y0 + 4, 0);
        g.pose().scale(1.5F, 1.5F, 1F);
        g.drawString(font, Component.literal(e.title()).withStyle(ChatFormatting.BOLD), 0, 0, 0xFFFFF0C8, true);
        g.pose().popPose();
        g.drawString(font, e.subtitle(), x + 44, y0 + 22, 0xFF000000 | sec.color(), false);
        GuiDraw.rope(g, x, y0 + 38, pageW());
        int by = y0 + 50;
        if (!e.items().isEmpty()) {
            for (int i = 0; i < e.items().size(); i++) {
                int sx = x + 2 + i * 20, sy = y0 + 49 + Math.round(Mth.sin(t * 0.12F + i * 0.7F));
                g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF1A2A38);
                g.fill(sx - 1, sy + 17, sx + 17, sy + 18, 0x80C8901E);
                g.renderItem(e.items().get(i).get(), sx, sy);
            }
            by += 24;
        }
        // body, scrollable
        int bh = ph - (by - y0);
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String para : e.body()) {
            if (para.isEmpty()) lines.add(FormattedCharSequence.EMPTY);
            else lines.addAll(font.split(Component.literal(para), pw - 6));
        }
        pageContentH = lines.size() * 10;
        pageScroll = Mth.clamp(pageScroll, 0, Math.max(0, pageContentH - bh));
        g.enableScissor(x, by, x + pw, by + bh);
        for (int i = 0; i < lines.size(); i++) {
            int ly = by + i * 10 - (int) pageScroll;
            if (ly < by - 10 || ly > by + bh) continue;
            g.drawString(font, lines.get(i), x + 2, ly, 0xFFE8DCC0, false);
        }
        g.disableScissor();
        if (pageContentH > bh) scrollbar(g, x + pw - 2, by, bh, pageScroll, pageContentH);
    }

    /** A pulsing brass ring around the page icon. */
    private void ring(GuiGraphics g, int x, int y, int size, float t, int accent) {
        float k = 0.5F + 0.5F * Mth.sin(t * 0.18F);
        int col = lerp(0xFF8A6420, 0xFFFFD870, k);
        g.fill(x, y, x + size, y + size, 0xFF1A2A38);
        g.fill(x, y, x + size, y + 1, col);
        g.fill(x, y + size - 1, x + size, y + size, col);
        g.fill(x, y, x + 1, y + size, col);
        g.fill(x + size - 1, y, x + size, y + size, col);
        g.fill(x + 2, y + 2, x + size - 2, y + 3, (accent & 0x00FFFFFF) | 0x60000000);
        // little sparkle orbiting the ring
        double a = t * 0.08;
        int sx = x + size / 2 + (int) Math.round(Math.cos(a) * (size / 2 - 1));
        int sy = y + size / 2 + (int) Math.round(Math.sin(a) * (size / 2 - 1));
        g.fill(sx - 1, sy, sx + 2, sy + 1, 0xFFFFFFFF);
        g.fill(sx, sy - 1, sx + 1, sy + 2, 0xFFFFFFFF);
    }

    // ------------------------------------------------------------------ voyage goals

    private void drawGoals(GuiGraphics g, int mouseX, int mouseY, float t) {
        var goals = com.piratecrew.goals.Goal.values();
        int done = com.piratecrew.client.ClientGoals.count();
        int x = pageX(), y0 = top + HEADER_H + 6, pw = pageW();
        g.pose().pushPose();
        g.pose().translate(x, y0 + 2, 0);
        g.pose().scale(1.5F, 1.5F, 1F);
        g.drawString(font, Component.literal("Voyage Goals").withStyle(ChatFormatting.BOLD), 0, 0, 0xFFFFF0C8, true);
        g.pose().popPose();
        String count = done + " / " + goals.length;
        g.drawString(font, count, x + pw - font.width(count) - 2, y0 + 6, 0xFFE8B84A, false);
        // progress gauge filling with gold, a glint running along it
        int by = y0 + 20, bw = pw - 2;
        g.fill(x, by, x + bw, by + 8, 0xFF1A0F08);
        g.fill(x + 1, by + 1, x + bw - 1, by + 7, 0xFF1C2C3A);
        int fill = (int) ((bw - 2) * (done / (float) goals.length));
        for (int i = 0; i < fill; i++) {
            float k = 0.5F + 0.5F * Mth.sin((i - t * 2F) / 9F);
            g.fill(x + 1 + i, by + 1, x + 2 + i, by + 7, lerp(0xFFC8901E, 0xFFFFE070, k * 0.6F));
        }
        g.fill(x + 1, by + 1, x + 1 + fill, by + 2, 0x60FFFFFF);
        GuiDraw.rope(g, x, y0 + 30, pw);
        int cy0 = cardsTop(), bh = top + h - 8 - cy0;
        int cols = 2, cw = (pw - 4) / 2;
        int rowsN = (goals.length + cols - 1) / cols;
        pageContentH = rowsN * (CARD_H + 4);
        pageScroll = Mth.clamp(pageScroll, 0, Math.max(0, pageContentH - bh));
        g.enableScissor(x, cy0, x + pw, cy0 + bh);
        for (int i = 0; i < goals.length; i++) {
            var goal = goals[i];
            boolean got = com.piratecrew.client.ClientGoals.has(goal);
            int cx = x + (i % cols) * (cw + 4);
            int cy = cy0 + (i / cols) * (CARD_H + 4) - (int) pageScroll;
            if (cy > cy0 + bh || cy + CARD_H < cy0) continue;
            g.fill(cx, cy, cx + cw, cy + CARD_H, 0xFF1A0F08);
            g.fill(cx + 1, cy + 1, cx + cw - 1, cy + CARD_H - 1, got ? 0xFF243A2E : 0xFF18222C);
            int border = got ? lerp(0xFFC8901E, 0xFFFFE890, 0.5F + 0.5F * Mth.sin(t * 0.15F + i)) : 0xFF3A4652;
            g.fill(cx, cy, cx + cw, cy + 1, border);
            g.fill(cx, cy, cx + 1, cy + CARD_H, border);
            g.fill(cx + cw - 1, cy, cx + cw, cy + CARD_H, (border & 0xFFFFFF) | 0x90000000);
            g.fill(cx, cy + CARD_H - 1, cx + cw, cy + CARD_H, (border & 0xFFFFFF) | 0x90000000);
            g.renderItem(goal.icon.get(), cx + 5, cy + 7);
            if (!got) {
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                g.fill(cx + 4, cy + 6, cx + 22, cy + 24, 0xB0101820);
                g.drawString(font, "?", cx + 11, cy + 11, 0xFF6A7682, false);
                g.pose().popPose();
            }
            g.drawString(font, Component.literal(fit(goal.title, cw - 30, true)).withStyle(ChatFormatting.BOLD), cx + 25, cy + 6,
                    got ? 0xFFFFF0C8 : 0xFF8A96A2, false);
            g.drawString(font, fit(goal.description, cw - 30, false), cx + 25, cy + 17, got ? 0xFFB8D8B0 : 0xFF6A7682, false);
            if (got) g.drawString(font, "\u2714", cx + cw - 9, cy + CARD_H - 11, 0xFF7FE07F, false);
            if (mouseX >= cx && mouseX < cx + cw && mouseY >= cy && mouseY < cy + CARD_H && mouseY >= cy0 && mouseY < cy0 + bh) {
                g.fill(cx + 1, cy + 1, cx + cw - 1, cy + CARD_H - 1, 0x18FFFFFF);
            }
        }
        g.disableScissor();
        if (pageContentH > bh) scrollbar(g, x + pw - 2, cy0, bh, pageScroll, pageContentH);
    }

    // ------------------------------------------------------------------ showcase

    private int cols() {
        return pageW() >= 330 ? 3 : 2;
    }

    private int cardW() {
        return (pageW() - (cols() - 1) * 4) / cols();
    }

    private static final int CARD_H = 30;

    private int cardsTop() {
        return top + HEADER_H + 44;
    }

    private void drawShowcase(GuiGraphics g, int mouseX, int mouseY, float t) {
        int x = pageX(), y0 = top + HEADER_H + 6, pw = pageW();
        g.pose().pushPose();
        g.pose().translate(x, y0 + 2, 0);
        g.pose().scale(1.5F, 1.5F, 1F);
        g.drawString(font, Component.literal("Showcase").withStyle(ChatFormatting.BOLD), 0, 0, 0xFFFFF0C8, true);
        g.pose().popPose();
        g.drawString(font, "One-click tools for operators", x, y0 + 18, 0xFFE8B84A, false);
        GuiDraw.rope(g, x, y0 + 28, pw);
        var tools = CodexContent.tools();
        int cy0 = cardsTop(), bh = top + h - 8 - cy0;
        int rowsN = (tools.size() + cols() - 1) / cols();
        pageContentH = rowsN * (CARD_H + 4);
        pageScroll = Mth.clamp(pageScroll, 0, Math.max(0, pageContentH - bh));
        g.enableScissor(x, cy0, x + pw, cy0 + bh);
        for (int i = 0; i < tools.size(); i++) {
            var tool = tools.get(i);
            int cx = x + (i % cols()) * (cardW() + 4);
            int cy = cy0 + (i / cols()) * (CARD_H + 4) - (int) pageScroll;
            boolean hov = mouseX >= cx && mouseX < cx + cardW() && mouseY >= cy && mouseY < cy + CARD_H && mouseY >= cy0 && mouseY < cy0 + bh;
            card(g, cx, cy - (hov ? 1 : 0), cardW(), CARD_H, hov, t, i);
            g.renderItem(tool.icon().get(), cx + 5, cy + 7 - (hov ? 1 : 0));
            g.drawString(font, Component.literal(fit(tool.title(), cardW() - 28, true)).withStyle(ChatFormatting.BOLD),
                    cx + 25, cy + 6 - (hov ? 1 : 0), 0xFFFFF0C8, false);
            g.drawString(font, fit(tool.subtitle(), cardW() - 28, false), cx + 25, cy + 17 - (hov ? 1 : 0), 0xFFB8A888, false);
        }
        g.disableScissor();
        if (pageContentH > bh) scrollbar(g, x + pw - 2, cy0, bh, pageScroll, pageContentH);
    }

    private void card(GuiGraphics g, int x, int y, int w, int h, boolean hovered, float t, int i) {
        int[] accents = {0xFF2FB3B5, 0xFFE8B84A, 0xFFD070FF, 0xFFE04040, 0xFF9AE0A0};
        int accent = accents[i % accents.length];
        g.fill(x, y, x + w, y + h, 0xFF1A0F08);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, hovered ? 0xFF2A3E50 : 0xFF1C2C3A);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x30FFFFFF);
        int border = accent;
        if (hovered) border = lerp(accent, 0xFFFFFFFF, 0.35F + 0.35F * Mth.sin(t * 0.3F));
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, (border & 0x00FFFFFF) | 0x90000000);
        g.fill(x, y, x + 1, y + h, border);
        g.fill(x + w - 1, y, x + w, y + h, (border & 0x00FFFFFF) | 0x90000000);
    }

    private int toolAt(double mx, double my) {
        if (mode != 1) return -1;
        int x = pageX(), cy0 = cardsTop(), bh = top + h - 8 - cy0;
        if (my < cy0 || my >= cy0 + bh || mx < x || mx >= x + pageW()) return -1;
        int col = (int) ((mx - x) / (cardW() + 4));
        int row = (int) ((my - cy0 + pageScroll) / (CARD_H + 4));
        if (col >= cols()) return -1;
        double inX = (mx - x) - col * (cardW() + 4), inY = (my - cy0 + pageScroll) - row * (CARD_H + 4);
        if (inX >= cardW() || inY >= CARD_H) return -1;
        int i = row * cols() + col;
        return i < CodexContent.tools().size() ? i : -1;
    }

    private void drawStripTooltip(GuiGraphics g, int mouseX, int mouseY) {
        var e = CodexContent.sections().get(section).entries().get(entry);
        int x = pageX(), y0 = top + HEADER_H + 6;
        for (int i = 0; i < e.items().size(); i++) {
            int sx = x + 2 + i * 20, sy = y0 + 49;
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                g.renderTooltip(font, e.items().get(i).get(), mouseX, mouseY);
            }
        }
    }

    private void drawToolTooltip(GuiGraphics g, int mouseX, int mouseY) {
        int i = toolAt(mouseX, mouseY);
        if (i >= 0) g.renderTooltip(font, Component.literal(CodexContent.tools().get(i).tooltip()), mouseX, mouseY);
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        int x0 = left + 6, y0 = top + HEADER_H + 3, sh = h - HEADER_H - 10;
        if (mx >= x0 && mx < x0 + SIDEBAR_W - 6 && my >= y0 && my < y0 + sh) {
            int i = (int) ((my - y0 + sideScroll) / ROW_H);
            if (i >= 0 && i < rows.size() && !rows.get(i).header) {
                Row r = rows.get(i);
                if (r.kind != 0) mode = r.kind;
                else {
                    mode = 0;
                    section = r.section;
                    entry = r.entry;
                }
                pageScroll = 0;
                click();
                return true;
            }
        }
        int tool = toolAt(mx, my);
        if (tool >= 0) {
            ShowcaseTools.Action a = CodexContent.tools().get(tool).action();
            ModNetwork.sendToServer(new CodexActionPacket(a));
            click();
            if (a == ShowcaseTools.Action.TO_SEA) onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx < left + SIDEBAR_W + 6) sideScroll -= delta * ROW_H * 2;
        else pageScroll -= delta * 20;
        return true;
    }

    private void click() {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }

    private static int lerp(int a, int b, float t) {
        t = Mth.clamp(t, 0, 1);
        int aa = a >>> 24, ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int ba = b >>> 24, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return (int) (aa + (ba - aa) * t) << 24 | (int) (ar + (br - ar) * t) << 16 | (int) (ag + (bg - ag) * t) << 8 | (int) (ab + (bb - ab) * t);
    }
}
