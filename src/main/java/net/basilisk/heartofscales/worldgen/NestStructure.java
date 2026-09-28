package net.basilisk.heartofscales.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.basilisk.heartofscales.registry.ModStructureTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.EmptyPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BiConsumer;

/**
 * Places one template from a pool with its bottom layer resting on the ground rather than buried.
 * Vanilla jigsaw always buries a rigid piece's bottom layer (ground level delta 1), and terrain adaptation
 * fills the world up to that layer. Here {@code foundation_layers} says how many bottom layers of the
 * template are ground to be buried: 0 puts the whole template on the surface, 1 matches vanilla.
 * Two modes: {@code project_start_to_heightmap} for surface placement, otherwise a cave-floor search
 * between {@code min_y} and {@code max_y} needing {@code headroom} air blocks. Surface placement samples
 * the ground across the whole footprint and gives up if it varies by more than {@code max_slope}; the
 * template then sits on the highest point so no terrain pokes through its air. With {@code avoid_fluids}
 * the cave search also rejects floors with water or lava inside the template's box or within
 * {@code fluid_margin} blocks around and above it: the template's air would breach the rock sealing a
 * neighbouring flooded pocket and let it pour in.
 */
public class NestStructure extends Structure {
    public static final Codec<NestStructure> CODEC = RecordCodecBuilder.<NestStructure>mapCodec(instance -> instance.group(
            settingsCodec(instance),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
            Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(s -> s.heightmap),
            Codec.INT.optionalFieldOf("min_y", -64).forGetter(s -> s.minY),
            Codec.INT.optionalFieldOf("max_y", 320).forGetter(s -> s.maxY),
            Codec.intRange(1, 64).optionalFieldOf("headroom", 1).forGetter(s -> s.headroom),
            Codec.intRange(0, 16).optionalFieldOf("foundation_layers", 0).forGetter(s -> s.foundationLayers),
            Codec.BOOL.optionalFieldOf("avoid_fluids", false).forGetter(s -> s.avoidFluids),
            Codec.intRange(0, 32).optionalFieldOf("fluid_margin", 6).forGetter(s -> s.fluidMargin),
            Codec.intRange(0, 16).optionalFieldOf("max_slope", 2).forGetter(s -> s.maxSlope)
    ).apply(instance, NestStructure::new)).codec();

    private static final int FOOTPRINT_SAMPLE_STEP = 4;
    private static final int FLUID_MARGIN_BELOW = 2;

    private final Holder<StructureTemplatePool> startPool;
    private final Optional<Heightmap.Types> heightmap;
    private final int minY;
    private final int maxY;
    private final int headroom;
    private final int foundationLayers;
    private final boolean avoidFluids;
    private final int fluidMargin;
    private final int maxSlope;

    public NestStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, Optional<Heightmap.Types> heightmap, int minY, int maxY, int headroom, int foundationLayers, boolean avoidFluids, int fluidMargin, int maxSlope) {
        super(settings);
        this.startPool = startPool;
        this.heightmap = heightmap;
        this.minY = minY;
        this.maxY = maxY;
        this.headroom = headroom;
        this.foundationLayers = foundationLayers;
        this.avoidFluids = avoidFluids;
        this.fluidMargin = fluidMargin;
        this.maxSlope = maxSlope;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();

        StructurePoolElement element = startPool.value().getRandomTemplate(context.random());
        if (element == EmptyPoolElement.INSTANCE) return Optional.empty();
        Rotation rotation = Rotation.getRandom(context.random());

        // The footprint only depends on x, z and rotation, so it can be worked out before the height is known
        BoundingBox footprint = element.getBoundingBox(context.structureTemplateManager(), new BlockPos(x, 0, z), rotation);
        OptionalInt groundY = heightmap.isPresent() ? surfaceGround(context, footprint) : caveFloor(context, x, z, footprint);
        if (groundY.isEmpty()) return Optional.empty();

        // Foundation layers replace the ground, so the origin drops by that many and the beardifier fills up to them
        BlockPos origin = new BlockPos(x, groundY.getAsInt() + 1 - foundationLayers, z);
        PoolElementStructurePiece piece = new PoolElementStructurePiece(
                context.structureTemplateManager(), element, origin, foundationLayers, rotation,
                element.getBoundingBox(context.structureTemplateManager(), origin, rotation));
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(piece)));
    }

    /** Highest top solid block across the footprint, or empty if the ground there is steeper than max_slope. */
    private OptionalInt surfaceGround(GenerationContext context, BoundingBox footprint) {
        List<Integer> heights = new ArrayList<>();
        forEachSample(footprint, (sx, sz) -> heights.add(
                context.chunkGenerator().getFirstFreeHeight(sx, sz, heightmap.get(), context.heightAccessor(), context.randomState()) - 1));
        return FlatGround.highestIfFlat(heights, maxSlope);
    }

    /** A random usable cave floor in the column at x,z, or empty if there is none. */
    private OptionalInt caveFloor(GenerationContext context, int x, int z, BoundingBox footprint) {
        NoiseColumn column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
        List<Integer> floors = new ArrayList<>(CaveFloorFinder.floors(minY, maxY, headroom,
                y -> isSolid(column.getBlock(y)),
                y -> column.getBlock(y).isAir()));
        if (floors.isEmpty()) return OptionalInt.empty();
        if (!avoidFluids) return OptionalInt.of(floors.get(context.random().nextInt(floors.size())));

        List<NoiseColumn> samples = sampleFootprint(context, footprint.inflatedBy(fluidMargin));
        while (!floors.isEmpty()) {
            int floorY = floors.remove(context.random().nextInt(floors.size()));
            int boxMinY = floorY + 1 - foundationLayers;
            // Only a small margin below: deep nests would otherwise always be rejected by the lava layer under y -54
            int fromY = boxMinY - Math.min(fluidMargin, FLUID_MARGIN_BELOW);
            int toY = boxMinY + footprint.getYSpan() - 1 + fluidMargin;
            if (samples.stream().allMatch(c -> CaveFloorFinder.isClear(fromY, toY, y -> !c.getBlock(y).getFluidState().isEmpty()))) {
                return OptionalInt.of(floorY);
            }
        }
        return OptionalInt.empty();
    }

    /** Terrain columns on a grid across the footprint, edges included. The generator's column sample includes aquifer fluids. */
    private static List<NoiseColumn> sampleFootprint(GenerationContext context, BoundingBox footprint) {
        List<NoiseColumn> columns = new ArrayList<>();
        forEachSample(footprint, (sx, sz) -> columns.add(
                context.chunkGenerator().getBaseColumn(sx, sz, context.heightAccessor(), context.randomState())));
        return columns;
    }

    /** Visits x,z on a grid across the footprint, always including the far edges. */
    private static void forEachSample(BoundingBox footprint, BiConsumer<Integer, Integer> visitor) {
        for (int sx = footprint.minX(); sx < footprint.maxX() + FOOTPRINT_SAMPLE_STEP; sx += FOOTPRINT_SAMPLE_STEP) {
            for (int sz = footprint.minZ(); sz < footprint.maxZ() + FOOTPRINT_SAMPLE_STEP; sz += FOOTPRINT_SAMPLE_STEP) {
                visitor.accept(Math.min(sx, footprint.maxX()), Math.min(sz, footprint.maxZ()));
            }
        }
    }

    private static boolean isSolid(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty();
    }

    @Override
    public StructureType<?> type() {
        return ModStructureTypes.NEST.get();
    }
}
