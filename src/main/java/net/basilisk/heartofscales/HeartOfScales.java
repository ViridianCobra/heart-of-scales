package net.basilisk.heartofscales;

import net.basilisk.heartofscales.config.HeartOfScalesClientConfig;
import net.basilisk.heartofscales.config.HeartOfScalesConfig;
import net.basilisk.heartofscales.network.ModNetwork;
import net.basilisk.heartofscales.registry.ModBlockEntities;
import net.basilisk.heartofscales.registry.ModBlocks;
import net.basilisk.heartofscales.registry.ModCreativeTabs;
import net.basilisk.heartofscales.registry.ModDataComponents;
import net.basilisk.heartofscales.registry.ModEntities;
import net.basilisk.heartofscales.registry.ModItems;
import net.basilisk.heartofscales.registry.ModLootModifiers;
import net.basilisk.heartofscales.registry.ModMenuTypes;
import net.basilisk.heartofscales.registry.ModStructureTypes;
import net.basilisk.heartofscales.species.ModRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(HeartOfScales.MOD_ID)
public class HeartOfScales {
    public static final String MOD_ID = "heart_of_scales";

    public HeartOfScales(IEventBus modEventBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(ModEntities::registerAttributes);
        ModCreativeTabs.TABS.register(modEventBus);
        ModLootModifiers.LOOT_MODIFIERS.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModStructureTypes.STRUCTURE_TYPES.register(modEventBus);
        modEventBus.addListener(ModRegistries::registerDatapackRegistries);
        modEventBus.addListener(ModNetwork::register);

        container.registerConfig(ModConfig.Type.COMMON, HeartOfScalesConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, HeartOfScalesClientConfig.SPEC);
    }
}
