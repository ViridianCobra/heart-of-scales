package net.basilisk.heartofscales.registry;

import net.basilisk.heartofscales.HeartOfScales;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Tinted breath clouds. Their look is registered client side in ClientModEvents. */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, HeartOfScales.MOD_ID);

    public static final RegistryObject<SimpleParticleType> AIR_BREATH =
            PARTICLE_TYPES.register("air-breath", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> WATER_BREATH =
            PARTICLE_TYPES.register("water-breath", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> DUST_BREATH =
            PARTICLE_TYPES.register("dust-breath", () -> new SimpleParticleType(false));

    private ModParticles() {}
}
