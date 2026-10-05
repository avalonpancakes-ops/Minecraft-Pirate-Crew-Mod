package com.piratecrew.util;

import com.piratecrew.PirateCrew;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Vanilla caps armor at 30 and toughness at 20, which would make the top Sundered Sea sets
 * pointless. This lifts both caps (the same thing mods like AttributeFix do), so high armor still
 * soaks up the huge hits from late bosses.
 */
public class AttributeCaps {
    public static void raise() {
        raise(Attributes.ARMOR, 30.0, 1024.0);
        raise(Attributes.ARMOR_TOUGHNESS, 20.0, 1024.0);
    }

    private static void raise(Attribute attribute, double oldMax, double newMax) {
        if (!(attribute instanceof RangedAttribute ranged)) return;
        try {
            for (Field f : RangedAttribute.class.getDeclaredFields()) {
                if (f.getType() != double.class || Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                if (f.getDouble(ranged) == oldMax) {
                    f.setDouble(ranged, newMax);
                    return;
                }
            }
            PirateCrew.LOGGER.warn("Pirate Crew: couldn't find the cap of {}", attribute.getDescriptionId());
        } catch (Exception e) {
            PirateCrew.LOGGER.warn("Pirate Crew: couldn't raise the cap of {}", attribute.getDescriptionId(), e);
        }
    }
}
