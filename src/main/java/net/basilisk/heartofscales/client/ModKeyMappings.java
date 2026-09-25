package net.basilisk.heartofscales.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.basilisk.heartofscales.HeartOfScales;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

/**
 * Rider keys. All share vanilla's in-game conflict context, so Ascend can sit on Space next to Jump. Flight Mode
 * shares F with Swap Hands; RiderInputHandler swallows the swap while riding. Free Cam is a toggle on
 * the middle mouse button, which it shares with Pick Block.
 */
@EventBusSubscriber(modid = HeartOfScales.MOD_ID, value = Dist.CLIENT)
public final class ModKeyMappings {
    private static final String CATEGORY = "key.categories." + HeartOfScales.MOD_ID;

    public static final KeyMapping ASCEND = new KeyMapping("key." + HeartOfScales.MOD_ID + ".ascend",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.KEY_SPACE, CATEGORY);
    public static final KeyMapping DESCEND = new KeyMapping("key." + HeartOfScales.MOD_ID + ".descend",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.KEY_LALT, CATEGORY);
    public static final KeyMapping FLIGHT_MODE = new KeyMapping("key." + HeartOfScales.MOD_ID + ".flight-mode",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.KEY_F, CATEGORY);
    public static final KeyMapping FREE_CAM = new KeyMapping("key." + HeartOfScales.MOD_ID + ".free-cam",
            KeyConflictContext.IN_GAME, InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_MIDDLE, CATEGORY);

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(ASCEND);
        event.register(DESCEND);
        event.register(FLIGHT_MODE);
        event.register(FREE_CAM);
    }

    private ModKeyMappings() {}
}
