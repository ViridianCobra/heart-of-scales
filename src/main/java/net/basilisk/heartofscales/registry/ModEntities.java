package net.basilisk.heartofscales.registry;

import net.basilisk.heartofscales.HeartOfScales;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, HeartOfScales.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<DragonEntity>> DRAGON = ENTITY_TYPES.register("dragon",
            () -> EntityType.Builder.of(DragonEntity::new, MobCategory.CREATURE)
                    .sized(1.5f, 1.6f)
                    .build("dragon"));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(DRAGON.get(), DragonEntity.createAttributes().build());
    }

    private ModEntities() {}
}
