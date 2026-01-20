package com.licht_meilleur.resurrection_ark.screen;

import com.licht_meilleur.resurrection_ark.ResurrectionArkMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {
    public static ScreenHandlerType<ResurrectionArkScreenHandler> RESURRECTION_ARK;

    public static void register() {
        RESURRECTION_ARK = Registry.register(
                Registries.SCREEN_HANDLER,
                new Identifier(ResurrectionArkMod.MOD_ID, "resurrection_ark"),
                new ScreenHandlerType<>(ResurrectionArkScreenHandler::new)
        );
    }
}