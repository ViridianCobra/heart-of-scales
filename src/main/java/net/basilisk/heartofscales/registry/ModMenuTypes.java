package net.basilisk.heartofscales.registry;

import net.basilisk.heartofscales.HeartOfScales;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.basilisk.heartofscales.menu.DragonMenu;
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

    private ModMenuTypes() {}
}
