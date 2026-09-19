package net.basilisk.heartofscales.registry;

import net.basilisk.heartofscales.HeartOfScales;
import net.basilisk.heartofscales.item.DragonEggItem;
import net.basilisk.heartofscales.item.DragonStaffItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, HeartOfScales.MOD_ID);

    public static final RegistryObject<Item> DRAGON_EGG = ITEMS.register("dragon-egg",
            () -> new DragonEggItem(ModBlocks.DRAGON_EGG.get(), new Item.Properties()));

    public static final RegistryObject<Item> NEST = ITEMS.register("nest-block",
            () -> new BlockItem(ModBlocks.NEST.get(), new Item.Properties()));
    public static final RegistryObject<Item> DRACIP_SEEDS = ITEMS.register("dracip-seeds",
            () -> new ItemNameBlockItem(ModBlocks.DRACIP.get(), new Item.Properties()));

    public static final RegistryObject<Item> DRACIP_PETALS = ITEMS.register("dracip-petals",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> DRAGON_SCALE = ITEMS.register("dragon-scale",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> AMORBERRY = ITEMS.register("amorberry",
            () -> new ItemNameBlockItem(ModBlocks.AMORBERRY_BUSH.get(), new Item.Properties()));

    public static final RegistryObject<Item> MUTATION_MUSHROOM = ITEMS.register("mutation-mushroom",
            () -> new BlockItem(ModBlocks.MUTATION_MUSHROOM.get(), new Item.Properties()));

    public static final RegistryObject<Item> DRAGON_SPAWN_EGG = ITEMS.register("dragon-spawn-egg",
            () -> new ForgeSpawnEggItem(ModEntities.DRAGON, 0x5B7F3A, 0xD8C27A, new Item.Properties()));

    public static final RegistryObject<Item> DRAGON_BEACON = ITEMS.register("dragon-beacon",
            () -> new DoubleHighBlockItem(ModBlocks.DRAGON_BEACON.get(), new Item.Properties()));

    public static final RegistryObject<Item> DRAGON_STAFF = ITEMS.register("dragon-staff",
            () -> new DragonStaffItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> DRAGON_SADDLE = ITEMS.register("dragon-saddle",
            () -> new Item(new Item.Properties().stacksTo(1)));

    private ModItems() {}
}
