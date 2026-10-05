package com.piratecrew.client;

import com.piratecrew.PirateCrew;
import com.piratecrew.item.GearSets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Flavour lines and set bonuses on Sundered Sea gear tooltips. */
@Mod.EventBusSubscriber(modid = PirateCrew.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class Tooltips {
    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        if (event.getToolTip().isEmpty()) return;
        GearSets.tooltip(event.getItemStack(), event.getToolTip());
    }
}
