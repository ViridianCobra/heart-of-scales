package net.basilisk.heartofscales.menu;

import net.basilisk.heartofscales.entity.DragonEntity;
import net.basilisk.heartofscales.registry.ModItems;
import net.basilisk.heartofscales.registry.ModMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Horse-style dragon inventory: one saddle slot for now, laid out so armour or bags can join later. */
public class DragonMenu extends AbstractContainerMenu {
    private static final int SADDLE_SLOT = 0;
    private static final int PLAYER_INVENTORY_START = 1;
    private static final int PLAYER_HOTBAR_START = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_SLOTS_END = PLAYER_HOTBAR_START + 9;
    private static final double MAX_REACH = 8.0;

    private final DragonEntity dragon;
    private final Container dragonInventory;

    public DragonMenu(int windowId, Inventory playerInventory, DragonEntity dragon) {
        super(ModMenuTypes.DRAGON.get(), windowId);
        this.dragon = dragon;
        this.dragonInventory = dragon.getInventory();
        dragonInventory.startOpen(playerInventory.player);

        addSlot(new Slot(dragonInventory, DragonEntity.SADDLE_SLOT, 8, 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.DRAGON_SADDLE.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
    }

    public DragonEntity getDragon() {
        return dragon;
    }

    @Override
    public boolean stillValid(Player player) {
        return dragonInventory.stillValid(player) && dragon.isAlive() && dragon.distanceTo(player) < MAX_REACH;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack moved = slot.getItem();
        ItemStack original = moved.copy();

        if (index == SADDLE_SLOT) {
            if (!moveItemStackTo(moved, PLAYER_INVENTORY_START, PLAYER_SLOTS_END, true)) return ItemStack.EMPTY;
        } else {
            Slot saddle = slots.get(SADDLE_SLOT);
            if (saddle.mayPlace(moved) && !saddle.hasItem()) {
                if (!moveItemStackTo(moved, SADDLE_SLOT, SADDLE_SLOT + 1, false)) return ItemStack.EMPTY;
            } else if (index < PLAYER_HOTBAR_START) {
                if (!moveItemStackTo(moved, PLAYER_HOTBAR_START, PLAYER_SLOTS_END, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(moved, PLAYER_INVENTORY_START, PLAYER_HOTBAR_START, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (moved.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        dragonInventory.stopOpen(player);
    }
}
