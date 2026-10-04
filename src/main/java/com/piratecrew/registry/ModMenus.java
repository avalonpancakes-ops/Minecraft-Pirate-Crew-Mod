package com.piratecrew.registry;

import com.piratecrew.PirateCrew;
import com.piratecrew.menu.PirateMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, PirateCrew.MODID);

    public static final RegistryObject<MenuType<PirateMenu>> PIRATE = MENUS.register("pirate",
            () -> IForgeMenuType.create(PirateMenu::fromNetwork));
}
