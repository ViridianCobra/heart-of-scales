package net.basilisk.heartofscales.client.entity;

import net.basilisk.heartofscales.HeartOfScales;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class DragonModel extends DefaultedEntityGeoModel<DragonEntity> {
    public DragonModel() {
        super(ResourceLocation.fromNamespaceAndPath(HeartOfScales.MOD_ID, "dragon"), true);
    }
}
