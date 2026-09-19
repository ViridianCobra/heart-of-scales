package net.basilisk.heartofscales.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.basilisk.heartofscales.HeartOfScales;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Rider keys. Both share vanilla's in-game conflict context, so Ascend can sit on Space next to Jump. */
@Mod.EventBusSubscriber(modid = HeartOfScales.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModKeyMappings {
    private static final String CATEGORY = "key.categories." + HeartOfScales.MOD_ID;

    public static final KeyMapping ASCEND = new KeyMapping("key." + HeartOfScales.MOD_ID + ".ascend",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.KEY_SPACE, CATEGORY);
    public static final KeyMapping FLIGHT_MODE = new KeyMapping("key." + HeartOfScales.MOD_ID + ".flight-mode",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.KEY_G, CATEGORY);

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(ASCEND);
        event.register(FLIGHT_MODE);
    }

    private ModKeyMappings() {}
}
