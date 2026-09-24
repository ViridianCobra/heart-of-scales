package net.basilisk.heartofscales.species;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * One subspecies, loaded from data/<ns>/heart_of_scales/dragon_species/<name>.json.
 * Fields grow as the spec needs them (favourite food, hatch condition, habitat...).
 */
public record DragonSpecies(SpeciesGroup species, boolean flies, boolean swims, int eggTint, TagKey<Item> foods, Item favouriteFood, HatchCondition hatchCondition) {
    public boolean isFavouriteFood(ItemStack stack) {
        return stack.is(favouriteFood);
    }

    public boolean isFood(ItemStack stack) {
        return stack.is(foods) || isFavouriteFood(stack);
    }

    private static final Codec<Integer> HEX_COLOUR = Codec.STRING.comapFlatMap(
            s -> {
                try {
                    return DataResult.success(Integer.parseInt(s.startsWith("#") ? s.substring(1) : s, 16));
                } catch (NumberFormatException e) {
                    return DataResult.error(() -> "Not a hex colour: " + s);
                }
            },
            i -> String.format("#%06X", i));

    public static final Codec<DragonSpecies> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SpeciesGroup.CODEC.fieldOf("species").forGetter(DragonSpecies::species),
            Codec.BOOL.optionalFieldOf("flies", false).forGetter(DragonSpecies::flies),
            Codec.BOOL.optionalFieldOf("swims", false).forGetter(DragonSpecies::swims),
            HEX_COLOUR.fieldOf("egg_tint").forGetter(DragonSpecies::eggTint),
            TagKey.codec(Registries.ITEM).fieldOf("foods").forGetter(DragonSpecies::foods),
            ForgeRegistries.ITEMS.getCodec().fieldOf("favourite_food").forGetter(DragonSpecies::favouriteFood),
            HatchCondition.CODEC.fieldOf("hatch_condition").forGetter(DragonSpecies::hatchCondition)
    ).apply(instance, DragonSpecies::new));
}
