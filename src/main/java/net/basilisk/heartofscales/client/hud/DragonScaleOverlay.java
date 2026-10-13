package net.basilisk.heartofscales.client.hud;

import net.basilisk.heartofscales.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

/**
 * Draws the scale readout to the right of the crosshair, level with it. Kept beside rather than below it so it
 * stays clear of the attack indicator and of the action bar and item name text above the hotbar.
 */
public final class DragonScaleOverlay implements IGuiOverlay {
    public static final String ID = "dragon_scale";
    /** Past the right edge of the crosshair and attack indicator, both 16px wide and centred. */
    private static final int CROSSHAIR_GAP = 12;
    private static final int SCREEN_MARGIN = 4;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;
        if (!hasScaleEquipped(mc)) return;

        int x = screenWidth / 2 + CROSSHAIR_GAP;
        int maxWidth = screenWidth - x - SCREEN_MARGIN;
        int y = screenHeight / 2 - mc.font.lineHeight / 2;
        for (Component line : ScaleReadout.lookedAt(mc)) {
            for (FormattedCharSequence wrapped : mc.font.split(line, maxWidth)) {
                graphics.drawString(mc.font, wrapped, x, y, 0xFFFFFF);
                y += mc.font.lineHeight + 1;
            }
        }
    }

    private static boolean hasScaleEquipped(Minecraft mc) {
        return CuriosApi.getCuriosInventory(mc.player)
                .map(inv -> inv.isEquipped(ModItems.DRAGON_SCALE.get()))
                .orElse(false);
    }
}
