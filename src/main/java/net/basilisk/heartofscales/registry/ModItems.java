package net.basilisk.heartofscales.registry;

import net.basilisk.heartofscales.HeartOfScales;
import net.basilisk.heartofscales.item.DragonEggItem;
import net.basilisk.heartofscales.item.DragonStaffItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, HeartOfScales.MOD_ID);

    public static final DeferredHolder<Item, Item> DRAGON_EGG = ITEMS.register("dragon-egg",
            () -> new DragonEggItem(ModBlocks.DRAGON_EGG.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> NEST = ITEMS.register("nest-block",
            () -> new BlockItem(ModBlocks.NEST.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> DRACIP_SEEDS = ITEMS.register("dracip-seeds",
            () -> new ItemNameBlockItem(ModBlocks.DRACIP.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> DRACIP_PETALS = ITEMS.register("dracip-petals",
            () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> DRAGON_SCALE = ITEMS.register("dragon-scale",
            () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> AMORBERRY = ITEMS.register("amorberry",
            () -> new ItemNameBlockItem(ModBlocks.AMORBERRY_BUSH.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> MUTATION_MUSHROOM = ITEMS.register("mutation-mushroom",
            () -> new BlockItem(ModBlocks.MUTATION_MUSHROOM.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> DRAGON_SPAWN_EGG = ITEMS.register("dragon-spawn-egg",
            () -> new DeferredSpawnEggItem(ModEntities.DRAGON, 0x5B7F3A, 0xD8C27A, new Item.Properties()));

    public static final DeferredHolder<Item, Item> DRAGON_BEACON = ITEMS.register("dragon-beacon",
            () -> new DoubleHighBlockItem(ModBlocks.DRAGON_BEACON.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> DRAGON_STAFF = ITEMS.register("dragon-staff",
            () -> new DragonStaffItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> DRAGON_SADDLE = ITEMS.register("dragon-saddle",
            () -> new Item(new Item.Properties().stacksTo(1)));

    private ModItems() {}
}
