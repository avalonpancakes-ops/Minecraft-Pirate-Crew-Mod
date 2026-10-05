package com.piratecrew.client;

import com.piratecrew.PirateCrew;
import com.piratecrew.item.GearSets;
import com.piratecrew.pact.SoulPact;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Flavour lines and set bonuses on Sundered Sea gear tooltips, and tier-coloured tooltip frames for every mod item. */
@Mod.EventBusSubscriber(modid = PirateCrew.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class Tooltips {
    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        if (event.getToolTip().isEmpty()) return;
        GearSets.tooltip(event.getItemStack(), event.getToolTip());
    }

    /** prefix, bright trim, dark trim, shimmer (boss-tier frames glint). */
    private static final Object[][] FRAMES = {
            {"tidesteel", 0x7AF0E8, 0x1F6670, false}, {"abyssal", 0xC9A0FF, 0x4B2C8C, false},
            {"krakenbone", 0xFF8AC4, 0x8A3A6A, true}, {"stormforged", 0xFFE85A, 0x8A6A00, true},
            {"leviathan", 0x7AF0FF, 0x146A78, true}, {"sovereign", 0xFFE070, 0xA0281E, true},
            {"broadside_cutlass", 0xFFB050, 0x7A3A10, true}, {"krakens_grasp", 0x7AF0D8, 0x6A2A5A, true},
            {"stormcaller", 0xFFF07A, 0x2E4478, true}, {"leviathans_fang", 0xC8FFF4, 0x0E5A66, true},
            {"iron_tide", 0xFF6070, 0x7A1018, true},
            {"ruby", 0xFF5A6A, 0x8A1020, false}, {"raw_ruby", 0xFF5A6A, 0x8A1020, false},
    };

    @SubscribeEvent
    public static void frame(RenderTooltipEvent.Color event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null || !key.getNamespace().equals(PirateCrew.MODID)) return;
        String path = key.getPath();
        int top = 0xF0CC6A, bottom = 0x6B4A12;
        boolean shimmer = false;
        for (Object[] f : FRAMES) {
            if (path.startsWith((String) f[0])) {
                top = (int) f[1];
                bottom = (int) f[2];
                shimmer = (boolean) f[3];
                break;
            }
        }
        if (path.startsWith("soul_pact_")) {
            SoulPact p = SoulPact.byId(path.substring("soul_pact_".length()));
            if (p != null) {
                top = p.seal;
                bottom = scale(p.seal, 0.4F);
                shimmer = true;
            }
        }
        if (shimmer) {
            float k = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 260.0);
            top = lerp(top, 0xFFFFFF, k * 0.45F);
        }
        event.setBorderStart(0xFF000000 | top);
        event.setBorderEnd(0xFF000000 | bottom);
        event.setBackground(0xF0000000 | lerp(0x140C08, scale(bottom, 0.25F), 0.6F));
    }

    private static int scale(int c, float f) {
        return ((int) (((c >> 16) & 255) * f) << 16) | ((int) (((c >> 8) & 255) * f) << 8) | (int) ((c & 255) * f);
    }

    private static int lerp(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) + ((((b >> 16) & 255) - ((a >> 16) & 255)) * t));
        int g = (int) (((a >> 8) & 255) + ((((b >> 8) & 255) - ((a >> 8) & 255)) * t));
        int bl = (int) ((a & 255) + (((b & 255) - (a & 255)) * t));
        return (r << 16) | (g << 8) | bl;
    }
}
