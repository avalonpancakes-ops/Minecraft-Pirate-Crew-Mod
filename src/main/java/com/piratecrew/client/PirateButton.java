package com.piratecrew.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.util.Mth;

/** A button drawn as a wooden plank with brass trim. Build with {@code Button.builder(...).build(PirateButton::new)}. */
public class PirateButton extends Button {
    public PirateButton(Button.Builder builder) {
        super(builder);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        GuiDraw.plank(g, getX(), getY(), getWidth(), getHeight(), isHoveredOrFocused(), this.active);
        int color = this.active ? 0xF4E4C0 : 0x8A7A66;
        renderString(g, Minecraft.getInstance().font, color | Mth.ceil(this.alpha * 255.0F) << 24);
    }
}
