package net.basilisk.heartofscales.species;

import net.basilisk.heartofscales.HeartOfScales;
import net.basilisk.heartofscales.genome.DragonGenome;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

import java.util.Optional;

public final class ModRegistries {
    private static final int NO_TINT = 0xFFFFFF;

    public static final ResourceKey<Registry<DragonSpecies>> DRAGON_SPECIES =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(HeartOfScales.MOD_ID, "dragon_species"));

    public static void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        // Second codec syncs entries to clients on join, so the client can look up egg tints
        event.dataPackRegistry(DRAGON_SPECIES, DragonSpecies.CODEC, DragonSpecies.CODEC);
    }

    public static Optional<DragonSpecies> species(RegistryAccess registries, DragonGenome genome) {
        return species(registries, genome.subspecies());
    }

    public static Optional<DragonSpecies> species(RegistryAccess registries, String subspecies) {
        ResourceLocation id = ResourceLocation.tryParse(subspecies);
        if (id == null) return Optional.empty();
        return registries.registry(DRAGON_SPECIES).map(registry -> registry.get(id));
    }

    /** Egg tint for this genome's subspecies, or white if no datapack defines it. */
    public static int eggTint(RegistryAccess registries, DragonGenome genome) {
        return tint(registries, genome.subspecies());
    }

    /** Tint for a subspecies id, or white if no datapack defines it. */
    public static int tint(RegistryAccess registries, String subspecies) {
        return species(registries, subspecies).map(DragonSpecies::eggTint).orElse(NO_TINT);
    }

    private ModRegistries() {}
}
