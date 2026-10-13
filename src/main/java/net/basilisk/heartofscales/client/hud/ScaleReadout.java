package net.basilisk.heartofscales.client.hud;

import net.basilisk.heartofscales.block.AmorberryBushBlock;
import net.basilisk.heartofscales.block.DracipCropBlock;
import net.basilisk.heartofscales.block.MutationMushroomBlock;
import net.basilisk.heartofscales.block.NestBlock;
import net.basilisk.heartofscales.block.entity.DragonEggBlockEntity;
import net.basilisk.heartofscales.block.entity.NestBlockEntity;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.basilisk.heartofscales.genome.DragonGenome;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** What the worn dragon scale says about whatever is under the crosshair. The first line is the name. */
public final class ScaleReadout {
    /** Vanilla crops and berry bushes only grow at this light level or above. */
    private static final int MIN_GROWTH_LIGHT = 9;
    private static final String SEPARATOR = " · ";

    public static List<Component> lookedAt(Minecraft mc) {
        if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof DragonEntity dragon) {
            return dragon(mc, dragon);
        }
        if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            return block(mc.level, hit.getBlockPos());
        }
        return List.of();
    }

    private static List<Component> dragon(Minecraft mc, DragonEntity dragon) {
        List<Component> lines = new ArrayList<>();
        lines.add(name(dragon.getName()));

        MutableComponent health = Component.translatable("hud.heart_of_scales.health",
                (int) Math.ceil(dragon.getHealth()), (int) Math.ceil(dragon.getMaxHealth()));
        if (dragon.isBaby()) health.append(SEPARATOR).append(Component.translatable("hud.heart_of_scales.baby"));
        lines.add(detail(health));

        if (dragon.isTame()) {
            lines.add(detail(Component.translatable("hud.heart_of_scales.tamed_by", ownerName(mc, dragon.getOwnerUUID()))
                    .append(SEPARATOR).append(dragon.getCommand().displayName())));
        } else {
            lines.add(detail(Component.translatable("hud.heart_of_scales.wild")));
        }
        return lines;
    }

    private static Component ownerName(Minecraft mc, UUID owner) {
        if (owner != null) {
            PlayerInfo info = mc.getConnection() != null ? mc.getConnection().getPlayerInfo(owner) : null;
            if (info != null) return Component.literal(info.getProfile().getName());
            Player player = mc.level.getPlayerByUUID(owner);
            if (player != null) return player.getName();
        }
        return Component.translatable("hud.heart_of_scales.unknown_owner");
    }

    private static List<Component> block(ClientLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        // The block state is server confirmed; the block entity's egg may be a client prediction the server rejected
        if (be instanceof NestBlockEntity nest) return state.getValue(NestBlock.HAS_EGG) ? nest(nest) : List.of();
        if (be instanceof DragonEggBlockEntity egg) {
            return List.of(eggName(egg.getGenome()), detail(Component.translatable("hud.heart_of_scales.needs_nest")));
        }

        Block block = state.getBlock();
        if (block instanceof DracipCropBlock crop) {
            List<Component> lines = new ArrayList<>();
            lines.add(name(block.getName()));
            if (crop.isMaxAge(state)) {
                lines.add(detail(Component.translatable("hud.heart_of_scales.ready_to_harvest")));
            } else {
                lines.add(detail(Component.translatable("hud.heart_of_scales.growing_stage", crop.getAge(state) + 1, crop.getMaxAge() + 1)));
                if (level.getRawBrightness(pos, 0) < MIN_GROWTH_LIGHT) lines.add(warning("hud.heart_of_scales.too_dark"));
            }
            return lines;
        }
        if (block instanceof AmorberryBushBlock) {
            List<Component> lines = new ArrayList<>();
            lines.add(name(block.getName()));
            int age = state.getValue(SweetBerryBushBlock.AGE);
            if (age == SweetBerryBushBlock.MAX_AGE) {
                lines.add(detail(Component.translatable("hud.heart_of_scales.ripe")));
            } else {
                lines.add(detail(Component.translatable(age >= 2 ? "hud.heart_of_scales.some_berries" : "hud.heart_of_scales.growing")));
                // Berry bushes check the light above themselves, unlike crops
                if (level.getRawBrightness(pos.above(), 0) < MIN_GROWTH_LIGHT) lines.add(warning("hud.heart_of_scales.too_dark"));
            }
            return lines;
        }
        if (block instanceof MutationMushroomBlock) return List.of(name(block.getName()));
        return List.of();
    }

    private static List<Component> nest(NestBlockEntity nest) {
        DragonGenome egg = nest.getEgg();
        if (egg == null) return List.of();

        List<Component> lines = new ArrayList<>();
        lines.add(eggName(egg));
        if (!nest.isHatching()) {
            lines.add(detail(Component.translatable("hud.heart_of_scales.start_hatching")));
        } else if (nest.isConditionMet()) {
            lines.add(detail(Component.translatable("hud.heart_of_scales.hatching", Countdown.format(nest.hatchTicksLeft()))));
        } else {
            lines.add(warning("hud.heart_of_scales.hatch_paused"));
            nest.hatchCondition().ifPresent(condition -> lines.add(detail(
                    Component.translatable("hud.heart_of_scales.needs." + condition.getSerializedName()))));
        }
        return lines;
    }

    private static Component eggName(DragonGenome genome) {
        Component subspecies = Component.translatable("subspecies." + genome.subspecies().replace(':', '.'));
        return name(Component.translatable("hud.heart_of_scales.egg", subspecies));
    }

    private static Component name(Component text) {
        return text.copy().withStyle(ChatFormatting.WHITE);
    }

    private static Component detail(MutableComponent text) {
        return text.withStyle(ChatFormatting.GRAY);
    }

    private static Component warning(String key) {
        return Component.translatable(key).withStyle(ChatFormatting.YELLOW);
    }

    private ScaleReadout() {}
}
