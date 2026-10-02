package net.basilisk.heartofscales.registry;

import net.basilisk.heartofscales.HeartOfScales;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, HeartOfScales.MOD_ID);

    public static final RegistryObject<EntityType<DragonEntity>> DRAGON = ENTITY_TYPES.register("dragon",
            () -> EntityType.Builder.of(DragonEntity::new, MobCategory.CREATURE)
                    .sized(1.5f, 1.6f)
                    // Every tick rather than vanilla's 3: clients only slide toward the last position sent, so at flight
                    // speed one late update makes the dragon visibly stop and catch up
                    .updateInterval(1)
                    .build("dragon"));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(DRAGON.get(), DragonEntity.createAttributes().build());
    }

    private ModEntities() {}
}
