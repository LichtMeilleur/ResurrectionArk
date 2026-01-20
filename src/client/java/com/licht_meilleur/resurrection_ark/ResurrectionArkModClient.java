package com.licht_meilleur.resurrection_ark;

import com.licht_meilleur.resurrection_ark.screen.ModScreenHandlers;
import com.licht_meilleur.resurrection_ark.screen.ResurrectionArkScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class ResurrectionArkModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.RESURRECTION_ARK, ResurrectionArkScreen::new);
    }
}