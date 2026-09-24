package net.basilisk.heartofscales.client.hud;

import net.basilisk.heartofscales.entity.DragonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.Locale;

/**
 * Speed readout beside the hotbar while the player is flying a dragon, red when a glide is stalling, with the
 * dragon's sprint stamina as a bar beneath it, red while it is locked out after running dry.
 */
public final class FlightSpeedOverlay implements IGuiOverlay {
    public static final String ID = "flight_speed";
    private static final int TICKS_PER_SECOND = 20;
    private static final int HOTBAR_HALF_WIDTH = 91;
    private static final int COLOUR_NORMAL = 0xFFFFFF;
    private static final int COLOUR_STALLING = 0xFF5555;
    private static final int STAMINA_BAR_WIDTH = 50;
    private static final int STAMINA_BAR_HEIGHT = 2;
    private static final int COLOUR_STAMINA_BACK = 0x80000000;
    private static final int COLOUR_STAMINA = 0xFF55FF55;
    private static final int COLOUR_STAMINA_EXHAUSTED = 0xFFFF5555;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (!(mc.player.getVehicle() instanceof DragonEntity dragon) || !dragon.isInFluidMode()) return;
        if (dragon.getControllingPassenger() != mc.player) return;

        // Distance covered in the last tick, so it reads the same in free flight and glide
        double dx = dragon.getX() - dragon.xo;
        double dy = dragon.getY() - dragon.yo;
        double dz = dragon.getZ() - dragon.zo;
        double blocksPerSecond = Math.sqrt(dx * dx + dy * dy + dz * dz) * TICKS_PER_SECOND;

        Component label = Component.translatable("hud.heart_of_scales.speed", String.format(Locale.ROOT, "%.1f", blocksPerSecond));
        int x = screenWidth / 2 + HOTBAR_HALF_WIDTH + 8;
        int y = screenHeight - 14;
        graphics.drawString(mc.font, label, x, y, dragon.isGlideStalling() ? COLOUR_STALLING : COLOUR_NORMAL);

        int barY = y + mc.font.lineHeight + 1;
        int filled = Math.round(STAMINA_BAR_WIDTH * dragon.getStaminaFraction());
        graphics.fill(x, barY, x + STAMINA_BAR_WIDTH, barY + STAMINA_BAR_HEIGHT, COLOUR_STAMINA_BACK);
        graphics.fill(x, barY, x + filled, barY + STAMINA_BAR_HEIGHT, dragon.isExhausted() ? COLOUR_STAMINA_EXHAUSTED : COLOUR_STAMINA);
    }
}
