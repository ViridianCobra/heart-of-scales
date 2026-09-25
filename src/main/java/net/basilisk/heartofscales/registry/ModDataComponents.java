package net.basilisk.heartofscales.registry;

import com.mojang.serialization.Codec;
import net.basilisk.heartofscales.HeartOfScales;
import net.basilisk.heartofscales.genome.DragonGenome;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;

/** Item data components. The genome rides on the egg item as a component instead of NBT (1.20.5+). */
public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, HeartOfScales.MOD_ID);

    private static final Codec<DragonGenome> GENOME_CODEC = Codec.STRING.xmap(DragonGenome::new, DragonGenome::subspecies);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DragonGenome>> GENOME =
            DATA_COMPONENTS.register("genome", () -> DataComponentType.<DragonGenome>builder()
                    .persistent(GENOME_CODEC)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8.map(DragonGenome::new, DragonGenome::subspecies))
                    .build());

    /** Dragon picked with the staff in assign mode, waiting for a beacon click. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> STAFF_SELECTED =
            DATA_COMPONENTS.register("staff_selected", () -> DataComponentType.<UUID>builder()
                    .persistent(UUIDUtil.CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> STAFF_MODE =
            DATA_COMPONENTS.register("staff_mode", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    private ModDataComponents() {}
}
