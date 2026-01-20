package com.licht_meilleur.resurrection_ark.block;

import com.licht_meilleur.resurrection_ark.ResurrectionArkMod;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {

    public static final Block RESURRECTION_ARK_BLOCK =
            Registry.register(
                    Registries.BLOCK,
                    new Identifier(ResurrectionArkMod.MOD_ID, "resurrection_ark"),
                    new ResurrectionArkBlock(
                            AbstractBlock.Settings.copy(Blocks.STONE).strength(4f)
                    )
            );

    public static void registerAll() {
        Registry.register(
                Registries.ITEM,
                new Identifier(ResurrectionArkMod.MOD_ID, "resurrection_ark"),
                new BlockItem(RESURRECTION_ARK_BLOCK, new Item.Settings())
        );
    }
}