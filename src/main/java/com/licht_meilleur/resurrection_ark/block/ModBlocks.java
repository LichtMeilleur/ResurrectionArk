package com.licht_meilleur.resurrection_ark.block;

import com.licht_meilleur.resurrection_ark.ResurrectionArkMod;
import net.minecraft.block.Block;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {

    // ★名前を RESURRECTION_ARK_BLOCK に統一（ここが重要）
    public static final Block RESURRECTION_ARK_BLOCK = registerBlock(
            "resurrection_ark",
            new ResurrectionArkBlock(AbstractBlock.Settings.copy(Blocks.STONE).strength(4f))
    );

    private static Block registerBlock(String name, Block block) {
        Registry.register(Registries.ITEM, new Identifier(ResurrectionArkMod.MOD_ID, name),
                new BlockItem(block, new Item.Settings()));
        return Registry.register(Registries.BLOCK, new Identifier(ResurrectionArkMod.MOD_ID, name), block);
    }

    public static void registerAll() {
        System.out.println("Registering ModBlocks for " + ResurrectionArkMod.MOD_ID);
    }
}