package com.licht_meilleur.resurrection_ark.item;

import com.licht_meilleur.resurrection_ark.ResurrectionArkMod;
import com.licht_meilleur.resurrection_ark.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    public static final ItemGroup RESURRECTION_ARK_GROUP = Registry.register(
            Registries.ITEM_GROUP,
            new Identifier(ResurrectionArkMod.MOD_ID, "resurrection_ark_group"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemgroup.resurrection_ark"))
                    .icon(() -> new ItemStack(ModBlocks.RESURRECTION_ARK_BLOCK)) // アイコンにLinkBlockを設定
                    .entries((displayContext, entries) -> {
                        entries.add(ModItems.RESURRECTION_CROSS);
                        entries.add(ModBlocks.RESURRECTION_ARK_BLOCK); // ブロックを登録
                        // 後で追加アイテムもここに書ける
                    })
                    .build()
    );

    public static void registerItemGroups() {
        ResurrectionArkMod.LOGGER.info("Registering Item Groups for " + ResurrectionArkMod.MOD_ID);
    }
}