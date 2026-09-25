package net.basilisk.heartofscales.registry;

import net.basilisk.heartofscales.HeartOfScales;
import net.basilisk.heartofscales.genome.DragonGenome;
import net.basilisk.heartofscales.item.DragonEggItem;
import net.basilisk.heartofscales.species.ModRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HeartOfScales.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> HEART_OF_SCALES = TABS.register("heart_of_scales",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + HeartOfScales.MOD_ID))
                    .icon(() -> DragonEggItem.withGenome(DragonGenome.defaultGenome()))
                    .displayItems((parameters, output) -> {
                        parameters.holders().lookup(ModRegistries.DRAGON_SPECIES).ifPresent(species ->
                                species.listElementIds().forEach(key -> output.accept(DragonEggItem.withGenome(new DragonGenome(key.location().toString())))));
                        output.accept(ModItems.NEST.get());
                        output.accept(ModItems.DRACIP_SEEDS.get());
                        output.accept(ModItems.DRACIP_PETALS.get());
                        output.accept(ModItems.DRAGON_SCALE.get());
                        output.accept(ModItems.AMORBERRY.get());
                        output.accept(ModItems.MUTATION_MUSHROOM.get());
                        output.accept(ModItems.DRAGON_SPAWN_EGG.get());
                        output.accept(ModItems.DRAGON_BEACON.get());
                        output.accept(ModItems.DRAGON_STAFF.get());
                        output.accept(ModItems.DRAGON_SADDLE.get());
                    })
                    .build());

    private ModCreativeTabs() {}
}
