package net.basilisk.heartofscales.registry;

import net.basilisk.heartofscales.HeartOfScales;
import net.basilisk.heartofscales.worldgen.NestStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModStructureTypes {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, HeartOfScales.MOD_ID);

    public static final DeferredHolder<StructureType<?>, StructureType<NestStructure>> NEST =
            STRUCTURE_TYPES.register("nest", () -> () -> NestStructure.CODEC);

    private ModStructureTypes() {}
}
