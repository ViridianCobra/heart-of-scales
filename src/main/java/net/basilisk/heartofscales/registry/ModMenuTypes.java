package net.basilisk.heartofscales.registry;

import net.basilisk.heartofscales.HeartOfScales;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.basilisk.heartofscales.menu.DragonBeaconMenu;
import net.basilisk.heartofscales.menu.DragonMenu;
import net.basilisk.heartofscales.roster.BeaconRow;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, HeartOfScales.MOD_ID);

    /** The server writes the dragon's entity id; the client looks the dragon up from it. */
    public static final DeferredHolder<MenuType<?>, MenuType<DragonMenu>> DRAGON = MENU_TYPES.register("dragon",
            () -> IMenuTypeExtension.create((windowId, inventory, buf) -> {
                DragonEntity dragon = (DragonEntity) inventory.player.level().getEntity(buf.readInt());
                return new DragonMenu(windowId, inventory, dragon);
            }));

    /** The server writes the rows. The client has no beacon position, so only the server closes the menu when you walk away. */
    public static final DeferredHolder<MenuType<?>, MenuType<DragonBeaconMenu>> DRAGON_BEACON = MENU_TYPES.register("dragon-beacon",
            () -> IMenuTypeExtension.create((windowId, inventory, buf) ->
                    new DragonBeaconMenu(windowId, ContainerLevelAccess.NULL, BeaconRow.readList(buf))));

    private ModMenuTypes() {}
}
