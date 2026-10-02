package net.basilisk.heartofscales.client;

import net.basilisk.heartofscales.HeartOfScales;
import net.basilisk.heartofscales.block.entity.DragonEggBlockEntity;
import net.basilisk.heartofscales.block.entity.NestBlockEntity;
import net.basilisk.heartofscales.genome.DragonGenome;
import net.basilisk.heartofscales.item.DragonEggItem;
import net.basilisk.heartofscales.species.ModRegistries;
import net.basilisk.heartofscales.registry.ModBlocks;
import net.basilisk.heartofscales.registry.ModEntities;
import net.basilisk.heartofscales.registry.ModParticles;
import net.basilisk.heartofscales.client.particle.BreathCloudParticle;
import net.basilisk.heartofscales.registry.ModItems;
import net.basilisk.heartofscales.client.entity.DragonRenderer;
import net.basilisk.heartofscales.client.screen.DragonBeaconScreen;
import net.basilisk.heartofscales.client.screen.DragonScreen;
import net.basilisk.heartofscales.registry.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.basilisk.heartofscales.client.hud.DragonScaleOverlay;
import net.basilisk.heartofscales.client.hud.FlightSpeedOverlay;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HeartOfScales.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private static final int NO_TINT = 0xFFFFFF;
    /** Thrown items draw at a quarter block; a dragon's rock should read at a glance from behind the rider. */
    private static final float ROCK_RENDER_SCALE = 2.0f;

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (level == null || pos == null) return NO_TINT;
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null || be.getLevel() == null) return NO_TINT;
            DragonGenome egg = null;
            if (be instanceof DragonEggBlockEntity eggBe) egg = eggBe.getGenome();
            if (be instanceof NestBlockEntity nest) egg = nest.getEgg();
            return egg == null ? NO_TINT : ModRegistries.eggTint(be.getLevel().registryAccess(), egg);
        }, ModBlocks.DRAGON_EGG.get(), ModBlocks.NEST.get());
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (Minecraft.getInstance().level == null) return NO_TINT;
            return ModRegistries.eggTint(Minecraft.getInstance().level.registryAccess(), DragonEggItem.genomeOf(stack));
        }, ModItems.DRAGON_EGG.get());
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.DRAGON.get(), DragonRenderer::new);
        event.registerEntityRenderer(ModEntities.DRAGON_ROCK.get(), context -> new ThrownItemRenderer<>(context, ROCK_RENDER_SCALE, false));
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.WATER_BREATH.get(), sprites -> new BreathCloudParticle.Provider(sprites, 0.35f, 0.6f, 1.0f));
        event.registerSpriteSet(ModParticles.DUST_BREATH.get(), sprites -> new BreathCloudParticle.Provider(sprites, 0.55f, 0.48f, 0.4f));
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.DRAGON.get(), DragonScreen::new);
            MenuScreens.register(ModMenuTypes.DRAGON_BEACON.get(), DragonBeaconScreen::new);
        });
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll(DragonScaleOverlay.ID, new DragonScaleOverlay());
        event.registerAboveAll(FlightSpeedOverlay.ID, new FlightSpeedOverlay());
    }

    private ClientModEvents() {}
}
