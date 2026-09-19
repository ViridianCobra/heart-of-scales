package net.basilisk.heartofscales.registry;

import net.basilisk.heartofscales.HeartOfScales;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.basilisk.heartofscales.menu.DragonMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, HeartOfScales.MOD_ID);

    /** The server writes the dragon's entity id; the client looks the dragon up from it. */
    public static final RegistryObject<MenuType<DragonMenu>> DRAGON = MENU_TYPES.register("dragon",
            () -> IForgeMenuType.create((windowId, inventory, buf) -> {
                DragonEntity dragon = (DragonEntity) inventory.player.level().getEntity(buf.readInt());
                return new DragonMenu(windowId, inventory, dragon);
            }));

    private ModMenuTypes() {}
}
