package net.basilisk.heartofscales.client.screen;

import net.basilisk.heartofscales.menu.DragonMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Reuses vanilla's horse inventory art: the saddle slot outline is a separate sprite below the main panel. */
public class DragonScreen extends AbstractContainerScreen<DragonMenu> {
    private static final ResourceLocation HORSE_INVENTORY = ResourceLocation.withDefaultNamespace("textures/gui/container/horse.png");
    private static final int SLOT_SPRITE_SIZE = 18;
    private static final int SADDLE_SPRITE_U = 18;
    private static final int ENTITY_SCALE = 17;

    private float mouseX;
    private float mouseY;

    public DragonScreen(DragonMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = (width - imageWidth) / 2;
        int top = (height - imageHeight) / 2;
        graphics.blit(HORSE_INVENTORY, left, top, 0, 0, imageWidth, imageHeight);
        graphics.blit(HORSE_INVENTORY, left + 7, top + 17, SADDLE_SPRITE_U, imageHeight + 54, SLOT_SPRITE_SIZE, SLOT_SPRITE_SIZE);
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, left + 51, top + 60, ENTITY_SCALE,
                (left + 51) - this.mouseX, (top + 75 - 50) - this.mouseY, menu.getDragon());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
