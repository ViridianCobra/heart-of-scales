package net.basilisk.heartofscales.menu;

import net.basilisk.heartofscales.registry.ModBlocks;
import net.basilisk.heartofscales.registry.ModMenuTypes;
import net.basilisk.heartofscales.roster.BeaconRow;
import net.basilisk.heartofscales.roster.DragonRoster;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The beacon's dragon list. Has no slots: it carries the rows to the client and button clicks back. */
public class DragonBeaconMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final List<BeaconRow> rows;

    public DragonBeaconMenu(int windowId, ContainerLevelAccess access, List<BeaconRow> rows) {
        super(ModMenuTypes.DRAGON_BEACON.get(), windowId);
        this.access = access;
        this.rows = new ArrayList<>(rows);
    }

    public List<BeaconRow> getRows() {
        return Collections.unmodifiableList(rows);
    }

    /** Button ids are row indexes. Both sides drop the row, whatever the roster says, so their lists stay in step. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= rows.size() || !rows.get(id).dismissable()) return false;
        if (player instanceof ServerPlayer serverPlayer) {
            DragonRoster.get(serverPlayer.server).dismiss(rows.get(id).dragon(), player.getUUID());
        }
        rows.remove(id);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.DRAGON_BEACON.get());
    }
}
