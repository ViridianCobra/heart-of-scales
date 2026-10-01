package net.basilisk.heartofscales.client.screen;

import net.basilisk.heartofscales.menu.DragonBeaconMenu;
import net.basilisk.heartofscales.roster.BeaconLang;
import net.basilisk.heartofscales.roster.BeaconRow;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

/** Lists the dragons living at a beacon, on vanilla's plain dialog panel. */
public class DragonBeaconScreen extends AbstractContainerScreen<DragonBeaconMenu> {
    private static final ResourceLocation PANEL = ResourceLocation.withDefaultNamespace("textures/gui/demo_background.png");
    private static final int PANEL_WIDTH = 248;
    private static final int PANEL_HEIGHT = 166;
    private static final int LIST_TOP = 18;
    private static final int LIST_MARGIN = 8;
    private static final int ROW_HEIGHT = 26;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int TEXT_COLOUR = 0x404040;
    private static final int FAINT_COLOUR = 0x707070;
    private static final int DISMISS_HOVER_COLOUR = 0xC03030;
    private static final Component DISMISS = Component.literal("✕");

    private RowList list;

    public DragonBeaconScreen(DragonBeaconMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = PANEL_WIDTH;
        imageHeight = PANEL_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        list = new RowList();
        addRenderableWidget(list);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(PANEL, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT_COLOUR, false);
        if (menu.getRows().isEmpty()) {
            Component empty = Component.translatable(BeaconLang.EMPTY);
            graphics.drawString(font, empty, (imageWidth - font.width(empty)) / 2, imageHeight / 2, FAINT_COLOUR, false);
        }
    }

    // AbstractContainerScreen keeps mouse drags for its slots, so pass them on or the scrollbar cannot be dragged
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return list.mouseDragged(mouseX, mouseY, button, dragX, dragY) || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void dismiss(int index) {
        if (!menu.clickMenuButton(minecraft.player, index)) return;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
        list.refresh();
    }

    private class RowList extends ObjectSelectionList<RowList.RowEntry> {
        RowList() {
            super(DragonBeaconScreen.this.minecraft, PANEL_WIDTH - 2 * LIST_MARGIN, DragonBeaconScreen.this.height,
                    DragonBeaconScreen.this.topPos + LIST_TOP, DragonBeaconScreen.this.topPos + PANEL_HEIGHT - LIST_MARGIN,
                    ROW_HEIGHT);
            setLeftPos(leftPos + LIST_MARGIN);
            setRenderBackground(false);
            setRenderTopAndBottom(false);
            setRenderSelection(false);
            refresh();
        }

        void refresh() {
            replaceEntries(menu.getRows().stream().map(RowEntry::new).toList());
        }

        // Rows are centred, so trimming both sides keeps them clear of the scrollbar
        @Override
        public int getRowWidth() {
            return width - 2 * (SCROLLBAR_WIDTH + 2);
        }

        // Vanilla draws rows 2px right of where it hit-tests them; line the two up so the whole ✕ is clickable
        @Override
        public int getRowLeft() {
            return x0 + (width - getRowWidth()) / 2;
        }

        @Override
        protected int getScrollbarPosition() {
            return x1 - SCROLLBAR_WIDTH;
        }

        private class RowEntry extends ObjectSelectionList.Entry<RowEntry> {
            private final BeaconRow row;

            RowEntry(BeaconRow row) {
                this.row = row;
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                               int mouseX, int mouseY, boolean hovering, float partialTick) {
                int right = left + width;
                boolean dismissable = canDismiss(index);
                int tamedRight = dismissable ? right - font.width(DISMISS) - 4 : right;
                Component tamed = BeaconRowText.tamed(row);
                int tamedLeft = tamedRight - font.width(tamed);
                graphics.drawString(font, BeaconRowText.fit(font, row.name(), tamedLeft - left - 4), left, top + 2, TEXT_COLOUR, false);
                graphics.drawString(font, tamed, tamedLeft, top + 2, FAINT_COLOUR, false);
                ResourceKey<Level> here = minecraft.level.dimension();
                graphics.drawString(font, BeaconRowText.fit(font, BeaconRowText.details(row, here), width), left, top + 13, FAINT_COLOUR, false);
                if (dismissable) {
                    int colour = isOverDismiss(mouseX, mouseY, top, right) ? DISMISS_HOVER_COLOUR : FAINT_COLOUR;
                    graphics.drawString(font, DISMISS, right - font.width(DISMISS), top + 2, colour, false);
                }
                if (hovering) setTooltipForNextRenderPass(BeaconRowText.tooltip(font, row, here));
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                int index = children().indexOf(this);
                if (button != 0 || !canDismiss(index) || !isOverDismiss(mouseX, mouseY, getRowTop(index), getRowRight())) {
                    return false;
                }
                dismiss(index);
                return true;
            }

            // Vanilla sends the button id as a byte
            private boolean canDismiss(int index) {
                return row.dismissable() && index <= Byte.MAX_VALUE;
            }

            private boolean isOverDismiss(double mouseX, double mouseY, int top, int right) {
                return mouseX >= right - font.width(DISMISS) - 2 && mouseX <= right && mouseY >= top && mouseY < top + 11;
            }

            @Override
            public Component getNarration() {
                return row.name();
            }
        }
    }
}
