package com.licht_meilleur.resurrection_ark;

import com.licht_meilleur.resurrection_ark.block.ModBlocks;
import com.licht_meilleur.resurrection_ark.block.entity.ModBlockEntities;
import com.licht_meilleur.resurrection_ark.event.MobRegisterEvent;
import com.licht_meilleur.resurrection_ark.item.ModItemGroups;
import com.licht_meilleur.resurrection_ark.item.ModItems;
import com.licht_meilleur.resurrection_ark.screen.ModScreenHandlers;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResurrectionArkMod implements ModInitializer {
	public static final String MOD_ID = "resurrection_ark";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.registerAll();
		ModItems.registerAll();
		ModBlockEntities.registerAll();
		ModItemGroups.registerItemGroups();
		MobRegisterEvent.register();
		ModScreenHandlers.register(); // ← これを追加

		System.out.println("Resurrection Ark initialized!");
	}

}