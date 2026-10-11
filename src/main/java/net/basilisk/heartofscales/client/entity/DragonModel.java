package net.basilisk.heartofscales.client.entity;

import net.basilisk.heartofscales.HeartOfScales;
import net.basilisk.heartofscales.entity.DragonBody;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

import java.util.IdentityHashMap;
import java.util.Map;

/** One model for every dragon; the files come from each dragon's body. */
public class DragonModel extends DefaultedEntityGeoModel<DragonEntity> {
    private static final String SADDLE_BONE = "saddle";
    private final Map<DragonBody, Paths> paths = new IdentityHashMap<>();

    public DragonModel() {
        super(ResourceLocation.fromNamespaceAndPath(HeartOfScales.MOD_ID, "dragon"), true);
    }

    @Override
    public ResourceLocation getModelResource(DragonEntity dragon) {
        return paths(dragon).model();
    }

    @Override
    public ResourceLocation getTextureResource(DragonEntity dragon) {
        return paths(dragon).texture();
    }

    @Override
    public ResourceLocation getAnimationResource(DragonEntity dragon) {
        return paths(dragon).animation();
    }

    /** Bones are shared by every dragon drawn with the same model, so the saddle is set for each dragon every frame. */
    @Override
    public void setCustomAnimations(DragonEntity dragon, long instanceId, AnimationState<DragonEntity> animationState) {
        if (dragon.getBody().turnsHead()) super.setCustomAnimations(dragon, instanceId, animationState);
        CoreGeoBone saddle = getAnimationProcessor().getBone(SADDLE_BONE);
        if (saddle != null) saddle.setHidden(!dragon.isSaddled());
    }

    private Paths paths(DragonEntity dragon) {
        return paths.computeIfAbsent(dragon.getBody(), body -> {
            ResourceLocation asset = ResourceLocation.fromNamespaceAndPath(HeartOfScales.MOD_ID, body.asset());
            return new Paths(buildFormattedModelPath(asset), buildFormattedTexturePath(asset), buildFormattedAnimationPath(asset));
        });
    }

    private record Paths(ResourceLocation model, ResourceLocation texture, ResourceLocation animation) {
    }
}
