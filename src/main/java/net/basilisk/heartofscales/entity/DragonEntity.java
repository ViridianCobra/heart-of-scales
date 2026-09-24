package net.basilisk.heartofscales.entity;

import net.basilisk.heartofscales.block.DragonBeaconBlock;
import net.basilisk.heartofscales.entity.ai.DragonFlightMoveControl;
import net.basilisk.heartofscales.entity.ai.DragonLookControl;
import net.basilisk.heartofscales.entity.ai.DragonRoamFlightGoal;
import net.basilisk.heartofscales.entity.ai.DragonRoamSwimGoal;
import net.basilisk.heartofscales.entity.ai.DragonSwimMoveControl;
import net.basilisk.heartofscales.entity.ai.FleeCarelessPlayerGoal;
import net.basilisk.heartofscales.entity.ai.ReturnToWaterGoal;
import net.basilisk.heartofscales.entity.ai.TemptWithFoodGoal;
import net.basilisk.heartofscales.genome.DragonGenome;
import net.basilisk.heartofscales.genome.Inheritance;
import net.basilisk.heartofscales.item.DragonStaffItem;
import net.basilisk.heartofscales.menu.DragonMenu;
import net.basilisk.heartofscales.registry.ModItems;
import net.basilisk.heartofscales.nbt.GenomeNbt;
import net.basilisk.heartofscales.species.DragonSpecies;
import net.basilisk.heartofscales.species.ModRegistries;
import net.basilisk.heartofscales.species.SpeciesGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;
import java.util.Random;
import java.util.Set;

public class DragonEntity extends TamableAnimal implements GeoEntity {
    private static final EntityDataAccessor<String> DATA_SUBSPECIES =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.STRING);
    private static final String TAG_GENOME = "Genome";
    private static final String TAG_TAME_PROGRESS = "TameProgress";
    private static final EntityDataAccessor<Byte> DATA_COMMAND =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BYTE);
    private static final String TAG_BEACON = "Beacon";
    private static final String TAG_BEACON_DIMENSION = "BeaconDimension";
    private static final String TAG_COMMAND = "Command";
    private static final EntityDataAccessor<Boolean> DATA_FLYING =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final String TAG_FLYING = "Flying";
    private static final EntityDataAccessor<Boolean> DATA_SWIM_MODE =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final String TAG_SWIM_MODE = "SwimMode";
    private static final EntityDataAccessor<Boolean> DATA_SADDLED =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final String TAG_SADDLE = "Saddle";
    private static final EntityDataAccessor<Float> DATA_STAMINA =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_EXHAUSTED =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> DATA_FLIGHT_MODE =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BYTE);
    private static final String TAG_FLIGHT_MODE = "FlightMode";
    public static final int SADDLE_SLOT = 0;
    private static final int INVENTORY_SIZE = 1;
    // Seat height above the feet; the placeholder saddle bone is at 20/16 but a sitting rider looks right a little lower
    private static final double SADDLE_HEIGHT = 1.1;
    /** Height the body pitches and rolls about in flight; must match DragonRenderer's pivot. */
    public static final double BODY_PIVOT_HEIGHT = 0.9;
    private static final float RIDDEN_STRAFE_FACTOR = 0.5f;
    private static final float RIDDEN_REVERSE_FACTOR = 0.25f;
    // Ridden flight: velocity is set directly from the rider's look, not through vanilla air physics
    private static final double RIDDEN_FLIGHT_SPEED_FACTOR = 1.0;
    private static final double RIDDEN_ASCEND_INPUT = 0.8;
    /** How quickly free-mode velocity snaps to the wanted direction per tick (1 = instant). */
    private static final double FREE_MODE_RESPONSIVENESS = 0.3;
    /** Ticks after take-off before touching the ground counts as landing. */
    private static final int LANDING_GRACE_TICKS = 10;
    // Vanilla's in-water travel ignores the mob's speed, so swimming pushes itself like a dolphin.
    // Cruise settles at SWIM_ACCEL * SWIM_DRAG / (1 - SWIM_DRAG) blocks per tick: about 0.54, just under flight cruise.
    private static final double SWIM_ACCEL = 0.06;
    private static final double SWIM_DRAG = 0.9;
    /** Ridden swimming chases the wanted velocity at half the rate of free flight, for a heavier feel in water. */
    private static final double RIDDEN_SWIM_RESPONSIVENESS = 0.15;
    // Glide mode: the heading chases the rider's look at a limited rate and speed is carried as momentum.
    // Speeds are in blocks per tick relative to FLYING_SPEED (cruise = 1.0 x attribute).
    private static final float GLIDE_YAW_RATE = 4.0f;
    private static final float GLIDE_PITCH_RATE = 3.0f;
    private static final double GLIDE_MAX_SPEED_FACTOR = 3.5;
    private static final double GLIDE_STALL_SPEED_FACTOR = 0.5;
    /**
     * Pitch band, in degrees below the horizon, where speed holds steady. Nose above it loses speed, nose below
     * it gains, so level flight slowly runs out of momentum.
     */
    private static final float GLIDE_NEUTRAL_PITCH_MIN = 4.0f;
    private static final float GLIDE_NEUTRAL_PITCH_MAX = 6.0f;
    /** Speed gained per tick in a vertical dive (scaled by sin of the angle past the band). */
    private static final double GLIDE_DIVE_ACCEL = 0.06;
    /**
     * Speed lost per tick in a vertical climb at cruise speed (scaled by sin of the angle past the band). Above
     * cruise the loss grows with speed, so a fast sweep upward sheds its extra speed quickly, then eases off.
     */
    private static final double GLIDE_CLIMB_DECEL = 0.042;
    // Free cam: while it is switched on the body ignores the rider's look and keeps its own heading, so the mouse
    // is left to the camera. Gliding, W and S pitch that heading and A and D bank into a turn; the turn rate ramps
    // so the visual roll, which comes from the yaw rate, leans in and out like a plane.
    private static final float FREE_CAM_KEY_PITCH_RATE = 2.5f;
    private static final float FREE_CAM_BANK_TURN_RATE = 3.0f;
    private static final float FREE_CAM_BANK_SMOOTHING = 0.15f;
    /** Stalled in free cam, the assist tips the body itself into a dive, since the look no longer steers it. */
    private static final float FREE_CAM_STALL_ASSIST_PITCH = 30.0f;
    private static final float FREE_CAM_STALL_ASSIST_RATE = 4.0f;
    /** Free flight levels its pitch at this rate once free cam is on, degrees per tick. */
    private static final float FREE_CAM_LEVEL_RATE = 4.0f;
    /** After free cam is switched off in free flight the body swings to the look at this rate instead of snapping. */
    private static final float FREE_CAM_RELEASE_TURN_RATE = 8.0f;
    /** How far the head may turn from the body to follow the rider's look in free cam, degrees. */
    private static final float FREE_CAM_HEAD_YAW_LIMIT = 70.0f;
    // Sprint: the rider holds vanilla's Sprint key to fly faster at a stamina cost. Stamina is counted in ticks of
    // sprinting, lives on the server and is synced, and refills whenever the dragon is not sprinting.
    private static final float STAMINA_MAX = 200.0f;
    /** How long a full bar lasts while sprinting. */
    private static final float STAMINA_DRAIN_SECONDS = 8.0f;
    /** Unit of sprint used every tick. */
    private static final float STAMINA_DRAIN = STAMINA_MAX / (STAMINA_DRAIN_SECONDS * 20.0f);
    private static final float STAMINA_REGEN = 0.5f;
    private static final int STAMINA_REGEN_DELAY_TICKS = 20;
    /** Run dry and sprinting is locked until this much has come back, so an empty bar cannot stutter-sprint. */
    private static final float STAMINA_RECOVERED_FRACTION = 0.25f;
    private static final double SPRINT_SPEED_FACTOR = 3;
    /** Glide sprint is powered wingbeats: speed added per tick, but never past cruise x SPRINT_SPEED_FACTOR. */
    private static final double GLIDE_SPRINT_ACCEL = 0.02;
    /** How quickly the glide side-slip reaches full strafe speed per tick (1 = instant). */
    private static final double GLIDE_STRAFE_RESPONSIVENESS = 0.2;
    // Below stall speed the wings stop carrying the dragon and it falls faster each tick until it has speed again.
    // The ramp is gentle so there is a moment of hang at the top of a climb to get the nose down.
    private static final double GLIDE_STALL_FALL_ACCEL = 0.015;
    private static final double GLIDE_STALL_FALL_MAX = 0.6;
    private static final double GLIDE_STALL_FALL_RECOVERY = 0.8;
    /** Stalled, the nose answers the look this fast, so a dive can be set up in under a second. */
    private static final float GLIDE_STALL_PITCH_RATE = 9.0f;
    /** Share of the fall turned into glide speed each tick once the nose is below the band: the dive catching. */
    private static final double GLIDE_STALL_FALL_TO_SPEED = 0.25;
    /** Stalled this long, RiderInputHandler starts easing the rider's look down into a dive. */
    private static final int GLIDE_STALL_ASSIST_DELAY_TICKS = 10;
    /** Marks glideSpeed as not yet seeded; the first glide tick takes the speed the dragon already has. */
    private static final double GLIDE_SPEED_UNSET = -1.0;
    // Visual roll, client side. Glide banks from the yaw rate; free flight also leans into a strafe and lifts
    // the nose when backing up. Both come from yaw and position changes, which are already synced.
    private static final float ROLL_PER_YAW_DEGREE = 8.0f;
    private static final float MAX_ROLL = 50.0f;
    private static final float ROLL_SMOOTHING = 0.15f;
    private static final float FREE_STRAFE_ROLL = 25.0f;
    private static final float FREE_REVERSE_PITCH = 20.0f;
    // Wandering dragons stay in a 33 x 33 box centred on their home beacon, from 3 below it to 17 above
    private static final int HOME_RANGE_HORIZONTAL = 16;
    private static final int HOME_RANGE_DOWN = 3;
    private static final int HOME_RANGE_UP = 17;
    private static final int HOME_CHECK_INTERVAL = 20;
    private static final int TAME_THRESHOLD = 100;
    private static final int FOOD_TAME_STEP = 15;
    private static final int FAVOURITE_FOOD_TAME_STEP = 40;
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("misc.sit");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("misc.fly");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private DragonGenome genome = DragonGenome.defaultGenome();
    private boolean genomeAssigned;
    private int tameProgress;
    @Nullable
    private GlobalPos home;
    // Ground and air movement each keep their own controller and navigator; setFlying swaps between them
    private final MoveControl groundMoveControl;
    private final PathNavigation groundNavigation;
    private final MoveControl airMoveControl;
    private final PathNavigation airNavigation;
    private final MoveControl swimMoveControl;
    private final PathNavigation swimNavigation;
    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE);
    // Transient rider input, held on the server and on the rider's client
    private boolean riderAscending;
    private boolean riderDescending;
    private boolean riderFreeCam;
    private boolean riderSprinting;
    private int staminaRestTicks;
    private boolean wasFreeCam;
    private boolean freeCamCatchingUp;
    private boolean swimNavigationConfigured;
    private float bankTurnRate;
    private int riddenFlightTicks;
    private double glideSpeed = GLIDE_SPEED_UNSET;
    private double glideFallSpeed;
    private int glideStallTicks;
    private double glideStrafe;
    // Client-side render state
    private float roll;
    private float rollO;
    private float tiltPitch;
    private float tiltPitchO;
    private float lastYaw;

    public DragonEntity(EntityType<? extends DragonEntity> type, Level level) {
        super(type, level);
        lookControl = new DragonLookControl(this);
        groundMoveControl = moveControl;
        groundNavigation = navigation;
        airMoveControl = new DragonFlightMoveControl(this);
        FlyingPathNavigation flying = new FlyingPathNavigation(this, level);
        flying.setCanOpenDoors(false);
        flying.setCanFloat(true);
        flying.setCanPassDoors(true);
        airNavigation = flying;
        swimMoveControl = new DragonSwimMoveControl(this);
        WaterBoundPathNavigation swimming = new WaterBoundPathNavigation(this, level);
        swimming.setCanFloat(false);
        swimNavigation = swimming;
        inventory.addListener(container -> onInventoryChanged());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_SUBSPECIES, DragonGenome.DEFAULT_SUBSPECIES);
        entityData.define(DATA_COMMAND, (byte) DragonCommand.FOLLOW.ordinal());
        entityData.define(DATA_FLYING, false);
        entityData.define(DATA_SWIM_MODE, false);
        entityData.define(DATA_SADDLED, false);
        entityData.define(DATA_FLIGHT_MODE, (byte) FlightMode.FREE.ordinal());
        entityData.define(DATA_STAMINA, STAMINA_MAX);
        entityData.define(DATA_EXHAUSTED, false);
    }

    public FlightMode getFlightMode() {
        return FlightMode.byOrdinal(entityData.get(DATA_FLIGHT_MODE));
    }

    public void toggleFlightMode(Player rider) {
        if (isInSwimMode()) return;
        FlightMode mode = getFlightMode().next();
        entityData.set(DATA_FLIGHT_MODE, (byte) mode.ordinal());
        rider.displayClientMessage(Component.translatable("flight_mode.heart_of_scales." + mode.id()), true);
    }

    public boolean isRiderAscending() {
        return riderAscending;
    }

    public void setRiderAscending(boolean ascending) {
        this.riderAscending = ascending;
    }

    public void setRiderDescending(boolean descending) {
        this.riderDescending = descending;
    }

    public boolean isRiderFreeCam() {
        return riderFreeCam;
    }

    public void setRiderFreeCam(boolean freeCam) {
        this.riderFreeCam = freeCam;
    }

    public void setRiderSprinting(boolean sprinting) {
        this.riderSprinting = sprinting;
    }

    /** Stamina left, 0 to 1. */
    public float getStaminaFraction() {
        return entityData.get(DATA_STAMINA) / STAMINA_MAX;
    }

    public boolean isExhausted() {
        return entityData.get(DATA_EXHAUSTED);
    }

    /**
     * Whether the sprint is actually happening this tick: key held, stamina to spend, and in free flight some
     * movement to boost, so hovering with the key held costs nothing. The same answer on the server, which spends
     * the stamina, and on the rider's client, which applies the speed.
     */
    public boolean isSprinting() {
        if (!riderSprinting || !isInFluidMode() || isExhausted() || entityData.get(DATA_STAMINA) <= 0.0f) return false;
        if (!(getControllingPassenger() instanceof Player rider)) return false;
        if (getFlightMode() == FlightMode.GLIDE) return true;
        return rider.xxa != 0.0f || rider.zza != 0.0f || riderAscending || riderDescending;
    }

    private void tickStamina() {
        float stamina = entityData.get(DATA_STAMINA);
        if (isSprinting()) {
            stamina = Math.max(0.0f, stamina - STAMINA_DRAIN);
            staminaRestTicks = 0;
            if (stamina <= 0.0f) entityData.set(DATA_EXHAUSTED, true);
        } else if (staminaRestTicks < STAMINA_REGEN_DELAY_TICKS) {
            staminaRestTicks++;
        } else {
            stamina = Math.min(STAMINA_MAX, stamina + STAMINA_REGEN);
        }
        if (isExhausted() && stamina >= STAMINA_MAX * STAMINA_RECOVERED_FRACTION) entityData.set(DATA_EXHAUSTED, false);
        entityData.set(DATA_STAMINA, stamina);
    }

    /** True on the rider's client while a glide is below stall speed; glide speed is only simulated there. */
    public boolean isGlideStalling() {
        return isFlying() && getFlightMode() == FlightMode.GLIDE && glideSpeed >= 0
                && glideSpeed < getAttributeValue(Attributes.FLYING_SPEED) * RIDDEN_FLIGHT_SPEED_FACTOR * GLIDE_STALL_SPEED_FACTOR;
    }

    public boolean isStallAssistActive() {
        return isGlideStalling() && glideStallTicks > GLIDE_STALL_ASSIST_DELAY_TICKS;
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    public boolean isSaddled() {
        return entityData.get(DATA_SADDLED);
    }

    /** Keeps the synced saddle flag in step with the saddle slot; losing the saddle throws the rider off. */
    private void onInventoryChanged() {
        if (level().isClientSide) return;
        boolean saddled = inventory.getItem(SADDLE_SLOT).is(ModItems.DRAGON_SADDLE.get());
        entityData.set(DATA_SADDLED, saddled);
        if (!saddled) ejectPassengers();
    }

    public void openInventory(ServerPlayer player) {
        NetworkHooks.openScreen(player,
                new SimpleMenuProvider((windowId, playerInventory, p) -> new DragonMenu(windowId, playerInventory, this),
                        Component.translatable("container.heart_of_scales.dragon")),
                buf -> buf.writeInt(getId()));
    }

    /** Whether this dragon's species can fly at all. */
    public boolean canFly() {
        return ModRegistries.species(level().registryAccess(), getSubspecies()).map(DragonSpecies::flies).orElse(false);
    }

    /** Whether this dragon's species swims. Water-bound species crawl on land and roam in water. */
    public boolean canSwim() {
        return ModRegistries.species(level().registryAccess(), getSubspecies()).map(DragonSpecies::swims).orElse(false);
    }

    public boolean isFlying() {
        return entityData.get(DATA_FLYING);
    }

    /** The only place gravity, move control and navigation are switched. Server side. */
    public void setFlying(boolean flying) {
        if (flying == isFlying()) return;
        if (flying && isInSwimMode()) return;
        entityData.set(DATA_FLYING, flying);
        // Every landing, ridden or not, puts the dragon back in free flight for the next take-off
        if (!flying) entityData.set(DATA_FLIGHT_MODE, (byte) FlightMode.FREE.ordinal());
        navigation.stop();
        moveControl = flying ? airMoveControl : groundMoveControl;
        navigation = flying ? airNavigation : groundNavigation;
        setNoGravity(flying);
    }

    public boolean isInSwimMode() {
        return entityData.get(DATA_SWIM_MODE);
    }

    /** Flying or swimming: the modes a rider steers by look rather than by vanilla ground movement. */
    public boolean isInFluidMode() {
        return isFlying() || isInSwimMode();
    }

    /** The speed AI swimming settles at under its push and drag; ridden swimming cruises at the same. */
    public double swimCruiseSpeed() {
        return SWIM_ACCEL * SWIM_DRAG / (1 - SWIM_DRAG);
    }

    /** The only place the swim move control, navigation and gravity are switched. Server side. */
    public void setSwimMode(boolean swim) {
        if (swim == isInSwimMode()) return;
        if (swim && isFlying()) return;
        entityData.set(DATA_SWIM_MODE, swim);
        navigation.stop();
        moveControl = swim ? swimMoveControl : groundMoveControl;
        navigation = swim ? swimNavigation : groundNavigation;
        setNoGravity(swim);
    }

    /** The land navigator, whichever mode is active. Used by goals that walk the dragon somewhere. */
    public PathNavigation getGroundNavigation() {
        return groundNavigation;
    }

    public DragonGenome getGenome() {
        return genome;
    }

    public void setGenome(DragonGenome genome) {
        this.genome = genome;
        this.genomeAssigned = true;
        entityData.set(DATA_SUBSPECIES, genome.subspecies());
    }

    /** Synced to clients, unlike the full genome. */
    public String getSubspecies() {
        return entityData.get(DATA_SUBSPECIES);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        if (!genomeAssigned) {
            level.registryAccess().registry(ModRegistries.DRAGON_SPECIES)
                    .flatMap(registry -> registry.getRandom(random))
                    .ifPresent(species -> setGenome(new DragonGenome(species.key().location().toString())));
        }
        return super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put(TAG_GENOME, GenomeNbt.save(genome, new CompoundTag()));
        tag.putInt(TAG_TAME_PROGRESS, tameProgress);
        tag.putString(TAG_COMMAND, getCommand().id());
        tag.putBoolean(TAG_FLYING, isFlying());
        tag.putBoolean(TAG_SWIM_MODE, isInSwimMode());
        tag.putString(TAG_FLIGHT_MODE, getFlightMode().id());
        ItemStack saddle = inventory.getItem(SADDLE_SLOT);
        if (!saddle.isEmpty()) tag.put(TAG_SADDLE, saddle.save(new CompoundTag()));
        if (home != null) {
            tag.put(TAG_BEACON, NbtUtils.writeBlockPos(home.pos()));
            tag.putString(TAG_BEACON_DIMENSION, home.dimension().location().toString());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_GENOME, Tag.TAG_COMPOUND)) {
            setGenome(GenomeNbt.load(tag.getCompound(TAG_GENOME)));
        }
        tameProgress = tag.getInt(TAG_TAME_PROGRESS);
        home = readHome(tag);
        setFlying(tag.getBoolean(TAG_FLYING));
        setSwimMode(tag.getBoolean(TAG_SWIM_MODE));
        entityData.set(DATA_FLIGHT_MODE, (byte) FlightMode.byId(tag.getString(TAG_FLIGHT_MODE)).ordinal());
        inventory.setItem(SADDLE_SLOT, tag.contains(TAG_SADDLE, Tag.TAG_COMPOUND)
                ? ItemStack.of(tag.getCompound(TAG_SADDLE)) : ItemStack.EMPTY);

        DragonCommand command;
        if (tag.contains(TAG_COMMAND, Tag.TAG_STRING)) {
            command = DragonCommand.byId(tag.getString(TAG_COMMAND));
        } else {
            // Saved before commands existed: keep doing whatever it was doing
            command = isOrderedToSit() ? DragonCommand.SIT : home != null ? DragonCommand.WANDER : DragonCommand.FOLLOW;
        }
        if (command == DragonCommand.WANDER && home == null) command = DragonCommand.FOLLOW;
        entityData.set(DATA_COMMAND, (byte) command.ordinal());
    }

    @Nullable
    private GlobalPos readHome(CompoundTag tag) {
        if (!tag.contains(TAG_BEACON, Tag.TAG_COMPOUND)) return null;
        ResourceLocation dimension = tag.contains(TAG_BEACON_DIMENSION, Tag.TAG_STRING)
                ? ResourceLocation.tryParse(tag.getString(TAG_BEACON_DIMENSION)) : null;
        ResourceKey<Level> dimensionKey = dimension != null ? ResourceKey.create(Registries.DIMENSION, dimension) : level().dimension();
        return GlobalPos.of(dimensionKey, NbtUtils.readBlockPos(tag.getCompound(TAG_BEACON)));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        // Vanilla's float goal bobs any mob to the surface, which would fight a dive
        goalSelector.addGoal(0, new FloatGoal(this) {
            @Override
            public boolean canUse() {
                return !canSwim() && super.canUse();
            }
        });
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(2, new FleeCarelessPlayerGoal(this, 8.0f, 1.2, 1.6));
        goalSelector.addGoal(3, new TemptWithFoodGoal(this));
        goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        goalSelector.addGoal(4, new DragonRoamFlightGoal(this));
        goalSelector.addGoal(4, new DragonRoamSwimGoal(this));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.0, 10.0f, 2.0f, false) {
            @Override
            public boolean canUse() {
                return getCommand() == DragonCommand.FOLLOW && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return getCommand() == DragonCommand.FOLLOW && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(7, new ReturnToWaterGoal(this));
        goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(10, new RandomLookAroundGoal(this));
    }

    public DragonCommand getCommand() {
        return DragonCommand.byOrdinal(entityData.get(DATA_COMMAND));
    }

    /** Returns false, changing nothing, if the dragon cannot carry the command out: wandering needs a home in this dimension. */
    public boolean setCommand(DragonCommand command) {
        if (command == DragonCommand.WANDER && !isHomeInThisDimension()) return false;
        entityData.set(DATA_COMMAND, (byte) command.ordinal());
        setOrderedToSit(command == DragonCommand.SIT);
        jumping = false;
        navigation.stop();
        if (isWalkingHome()) DragonHomecoming.track(this);
        return true;
    }

    public boolean hasHome() {
        return home != null;
    }

    /** Lower half of this dragon's home beacon, or null. */
    @Nullable
    public GlobalPos getHome() {
        return home;
    }

    public boolean isHomeInThisDimension() {
        return home != null && home.dimension() == level().dimension();
    }

    /** Gives the dragon a home and sends it there to wander. */
    public void setHome(GlobalPos home) {
        this.home = GlobalPos.of(home.dimension(), home.pos().immutable());
        setCommand(DragonCommand.WANDER);
    }

    public void clearHome() {
        this.home = null;
        if (getCommand() == DragonCommand.WANDER) setCommand(DragonCommand.FOLLOW);
    }

    private boolean isHomeBound() {
        return getCommand() == DragonCommand.WANDER && isHomeInThisDimension();
    }

    /** Told to wander but not back inside its home area yet. */
    public boolean isWalkingHome() {
        return isHomeBound() && !isWithinRestriction();
    }

    /** Skips the rest of the walk. Used by DragonHomecoming once nobody is around to see it. */
    public void teleportHome() {
        if (!isHomeBound() || !(level() instanceof ServerLevel serverLevel)) return;
        BlockPos beacon = home.pos();
        serverLevel.getChunkAt(beacon);
        Vec3 spot = findLandingSpot(serverLevel, beacon);
        teleportTo(serverLevel, spot.x, spot.y, spot.z, Set.of(), getYRot(), getXRot());
        navigation.stop();
        serverLevel.sendParticles(ParticleTypes.POOF, spot.x, spot.y + getBbHeight() / 2, spot.z, 20, 0.5, 0.5, 0.5, 0.02);
    }

    private Vec3 findLandingSpot(ServerLevel serverLevel, BlockPos beacon) {
        for (int radius = 2; radius <= 4; radius++) {
            for (BlockPos pos : BlockPos.betweenClosed(beacon.offset(-radius, -1, -radius), beacon.offset(radius, 2, radius))) {
                boolean onRing = Math.max(Math.abs(pos.getX() - beacon.getX()), Math.abs(pos.getZ() - beacon.getZ())) == radius;
                if (!onRing || !serverLevel.getBlockState(pos.below()).isFaceSturdy(serverLevel, pos.below(), Direction.UP)) continue;
                Vec3 spot = Vec3.atBottomCenterOf(pos);
                if (serverLevel.noCollision(this, getDimensions(getPose()).makeBoundingBox(spot))) return spot;
            }
        }
        return Vec3.atBottomCenterOf(beacon.above(2));
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        // A dragon loaded into an area that is not ticking never gets to run its own check
        if (!level().isClientSide && isWalkingHome()) DragonHomecoming.track(this);
    }

    @Override
    public boolean hasRestriction() {
        return isHomeBound() || super.hasRestriction();
    }

    @Override
    public BlockPos getRestrictCenter() {
        return isHomeBound() ? home.pos() : super.getRestrictCenter();
    }

    // Vanilla only uses this to decide whether the dragon is close enough for the restriction to matter,
    // so it has to reach the corners of the box
    @Override
    public float getRestrictRadius() {
        return isHomeBound() ? HOME_RANGE_HORIZONTAL * 1.5f : super.getRestrictRadius();
    }

    @Override
    public boolean isWithinRestriction(BlockPos pos) {
        if (!isHomeBound()) return super.isWithinRestriction(pos);
        BlockPos beacon = home.pos();
        return Math.abs(pos.getX() - beacon.getX()) <= HOME_RANGE_HORIZONTAL
                && Math.abs(pos.getZ() - beacon.getZ()) <= HOME_RANGE_HORIZONTAL
                && pos.getY() >= beacon.getY() - HOME_RANGE_DOWN
                && pos.getY() <= beacon.getY() + HOME_RANGE_UP;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            tickRoll();
            return;
        }
        tickStamina();
        WaterBoundCrawl.tick(this);
        if (isInSwimMode()) RiddenSwimming.topUpPassengerAir(this);
        if (!swimNavigationConfigured && canSwim()) {
            // Species is not known in the constructor; once it is, let land paths enter water instead of stopping at the bank
            groundNavigation.setCanFloat(true);
            swimNavigationConfigured = true;
        }
        if (home == null || tickCount % HOME_CHECK_INTERVAL != 0) return;
        if (isWalkingHome()) DragonHomecoming.track(this);
        if (isHomeInThisDimension() && level().isLoaded(home.pos())) {
            BlockState state = level().getBlockState(home.pos());
            boolean beaconPresent = state.getBlock() instanceof DragonBeaconBlock
                    && state.getValue(DragonBeaconBlock.HALF) == DoubleBlockHalf.LOWER;
            if (!beaconPresent) clearHome();
        }
    }

    /** The owner in the saddle drives; anyone else on board is a passenger with no control. */
    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player rider && isSaddled() && isOwnedBy(rider) ? rider : null;
    }

    /** Runs on the server and the rider's client, so take-off and landing are decided identically on both. */
    @Override
    protected void tickRidden(Player rider, Vec3 input) {
        super.tickRidden(rider, input);
        boolean freeCam = riderFreeCam && isInFluidMode();
        if (freeCam && !wasFreeCam) bankTurnRate = 0.0f;
        if (!freeCam && wasFreeCam) freeCamCatchingUp = true;
        wasFreeCam = freeCam;

        if (isFlying() && getFlightMode() == FlightMode.GLIDE) {
            // Glide speed lives on the rider's client, so only that side ever sees a stall
            boolean stalling = isGlideStalling();
            glideStallTicks = stalling ? glideStallTicks + 1 : 0;
            if (freeCam) {
                tickFreeCamGlide(rider);
            } else {
                // Momentum: the heading lags behind the look
                setRot(Mth.approachDegrees(getYRot(), rider.getYRot(), GLIDE_YAW_RATE),
                        Mth.approachDegrees(getXRot(), rider.getXRot(), stalling ? GLIDE_STALL_PITCH_RATE : GLIDE_PITCH_RATE));
            }
            freeCamCatchingUp = false;
        } else if (freeCam) {
            // Free flight keeps its yaw and levels out, so WASD moves flat along the heading and Ascend and
            // Descend handle height. Holding whatever pitch the look happened to have would leave it stuck nose up or down.
            setRot(getYRot(), Mth.approach(getXRot(), 0.0f, FREE_CAM_LEVEL_RATE));
        } else if (isInFluidMode() && freeCamCatchingUp) {
            setRot(Mth.approachDegrees(getYRot(), rider.getYRot(), FREE_CAM_RELEASE_TURN_RATE),
                    Mth.approachDegrees(getXRot(), rider.getXRot(), FREE_CAM_RELEASE_TURN_RATE));
            freeCamCatchingUp = Math.abs(Mth.degreesDifference(getYRot(), rider.getYRot())) > 1.0f
                    || Math.abs(getXRot() - rider.getXRot()) > 1.0f;
        } else {
            freeCamCatchingUp = false;
            setRot(rider.getYRot(), isInFluidMode() ? rider.getXRot() : rider.getXRot() * 0.5f);
        }
        yRotO = yBodyRot = yHeadRot = getYRot();
        if (freeCam) {
            yHeadRot += Mth.clamp(Mth.wrapDegrees(rider.getYRot() - getYRot()), -FREE_CAM_HEAD_YAW_LIMIT, FREE_CAM_HEAD_YAW_LIMIT);
        }

        RiddenSwimming.tickMode(this);
        if (isInSwimMode()) return;
        if (!isFlying()) {
            if (riderAscending && onGround() && canFly()) {
                setFlying(true);
                riddenFlightTicks = 0;
            }
            return;
        }
        riddenFlightTicks++;
        if (onGround() && !riderAscending && riddenFlightTicks > LANDING_GRACE_TICKS) setFlying(false);
    }

    /** Free cam glide: the keys steer the body's own heading. A is left, which is a falling yaw; W is nose down. */
    private void tickFreeCamGlide(Player rider) {
        float wantedTurn = -Math.signum(rider.xxa) * FREE_CAM_BANK_TURN_RATE;
        bankTurnRate += (wantedTurn - bankTurnRate) * FREE_CAM_BANK_SMOOTHING;
        float pitch = getXRot() + Math.signum(rider.zza) * FREE_CAM_KEY_PITCH_RATE;
        if (isStallAssistActive() && pitch < FREE_CAM_STALL_ASSIST_PITCH) {
            pitch = Math.min(FREE_CAM_STALL_ASSIST_PITCH, pitch + FREE_CAM_STALL_ASSIST_RATE);
        }
        setRot(getYRot() + bankTurnRate, Mth.clamp(pitch, -90.0f, 90.0f));
    }

    /** Vanilla's body control swings a still mob's body round to face its head, which would undo free cam's head turn. */
    @Override
    protected float tickHeadTurn(float yRot, float animStep) {
        if (isInFluidMode() && getControllingPassenger() != null) return animStep;
        return super.tickHeadTurn(yRot, animStep);
    }

    @Override
    public void travel(Vec3 input) {
        if (isFlying() && getControllingPassenger() instanceof Player rider && isControlledByLocalInstance()) {
            travelFlyingRidden(rider, input);
            return;
        }
        if (isInSwimMode() && getControllingPassenger() instanceof Player rider && isControlledByLocalInstance()) {
            travelFreeSteered(rider, input, swimCruiseSpeed() * (isSprinting() ? SPRINT_SPEED_FACTOR : 1.0),
                    RIDDEN_SWIM_RESPONSIVENESS, true);
            return;
        }
        if (isInSwimMode() && isEffectiveAi() && !isVehicle()) {
            travelSwimming(input);
            return;
        }
        super.travel(input);
    }

    /** The move control's input gives the direction; a fixed push and drag give the speed. */
    private void travelSwimming(Vec3 input) {
        if (input.lengthSqr() > 1.0e-7) {
            setDeltaMovement(getDeltaMovement().add(input.normalize().scale(SWIM_ACCEL).yRot((float) -Math.toRadians(getYRot()))));
        }
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(SWIM_DRAG));
        calculateEntityAnimation(false);
    }

    /**
     * Free mode: the dragon goes where the rider looks. Forward input follows the look vector, pitch included,
     * strafe slides sideways, ascend and descend add straight up and down. Vanilla's air friction is skipped because it slows
     * horizontal motion five times more than vertical.
     */
    private void travelFlyingRidden(Player rider, Vec3 input) {
        if (getFlightMode() == FlightMode.GLIDE) {
            travelGliding(input);
            return;
        }
        glideSpeed = GLIDE_SPEED_UNSET;
        glideFallSpeed = 0;
        glideStallTicks = 0;
        glideStrafe = 0;
        double speed = getAttributeValue(Attributes.FLYING_SPEED) * RIDDEN_FLIGHT_SPEED_FACTOR
                * (isSprinting() ? SPRINT_SPEED_FACTOR : 1.0);
        travelFreeSteered(rider, input, speed, FREE_MODE_RESPONSIVENESS, false);
    }

    /**
     * Shared by free flight and ridden swimming: the dragon goes where the rider looks. Forward input follows the look
     * vector, pitch included, strafe slides sideways, ascend and descend add straight up and down, and velocity chases
     * the result at the given responsiveness.
     */
    private void travelFreeSteered(Player rider, Vec3 input, double speed, double responsiveness, boolean capAtSurface) {
        double yaw = Math.toRadians(getYRot());
        Vec3 left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        Vec3 forward = riderFreeCam ? Vec3.directionFromRotation(getXRot(), getYRot()) : rider.getLookAngle();
        Vec3 wanted = forward.scale(input.z).add(left.scale(input.x));
        if (riderAscending) wanted = wanted.add(0, RIDDEN_ASCEND_INPUT, 0);
        if (riderDescending) wanted = wanted.add(0, -RIDDEN_ASCEND_INPUT, 0);
        if (wanted.lengthSqr() > 1.0) wanted = wanted.normalize();
        wanted = wanted.scale(speed);
        if (capAtSurface) wanted = RiddenSwimming.capAtSurface(this, wanted);

        Vec3 velocity = getDeltaMovement().lerp(wanted, responsiveness);
        setDeltaMovement(velocity);
        move(MoverType.SELF, velocity);
        calculateEntityAnimation(true);
    }

    /**
     * Glide mode: speed is a scalar carried along the dragon's own heading, and only the pitch changes it.
     * Inside the neutral band it holds, nose above the band loses speed, nose below gains. Apart from sprinting,
     * nothing else adds speed: a glide runs on momentum and dives, and pulling the nose up is the only brake. A and D side-slip without
     * turning, so the dragon can be shifted around an obstacle while holding its course. Below stall speed the
     * dragon falls until a dive gives speed back.
     */
    private void travelGliding(Vec3 input) {
        double cruise = getAttributeValue(Attributes.FLYING_SPEED) * RIDDEN_FLIGHT_SPEED_FACTOR;
        double stallSpeed = cruise * GLIDE_STALL_SPEED_FACTOR;
        if (glideSpeed < 0) glideSpeed = Math.max(getDeltaMovement().length(), stallSpeed);

        // XRot is positive nose-down, so the band sits at +4..+6
        float pitch = getXRot();
        float pastBand = pitch > GLIDE_NEUTRAL_PITCH_MAX ? pitch - GLIDE_NEUTRAL_PITCH_MAX
                : pitch < GLIDE_NEUTRAL_PITCH_MIN ? pitch - GLIDE_NEUTRAL_PITCH_MIN : 0.0f;
        double pitchEffect = Math.sin(Math.toRadians(pastBand));
        glideSpeed += pitchEffect > 0 ? pitchEffect * GLIDE_DIVE_ACCEL
                : pitchEffect * GLIDE_CLIMB_DECEL * Math.max(1.0, glideSpeed / cruise);
        double sprintCeiling = cruise * SPRINT_SPEED_FACTOR;
        if (isSprinting() && glideSpeed < sprintCeiling) glideSpeed = Math.min(sprintCeiling, glideSpeed + GLIDE_SPRINT_ACCEL);
        if (glideSpeed < stallSpeed && pastBand > 0) {
            double caught = glideFallSpeed * GLIDE_STALL_FALL_TO_SPEED;
            glideSpeed += caught;
            glideFallSpeed -= caught;
        }
        glideSpeed = Mth.clamp(glideSpeed, 0.0, cruise * GLIDE_MAX_SPEED_FACTOR);

        if (glideSpeed < stallSpeed) glideFallSpeed = Math.min(GLIDE_STALL_FALL_MAX, glideFallSpeed + GLIDE_STALL_FALL_ACCEL);
        else glideFallSpeed *= GLIDE_STALL_FALL_RECOVERY;

        glideStrafe += (input.x * cruise - glideStrafe) * GLIDE_STRAFE_RESPONSIVENESS;
        double yaw = Math.toRadians(getYRot());
        Vec3 left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        Vec3 velocity = Vec3.directionFromRotation(getXRot(), getYRot()).scale(glideSpeed)
                .add(left.scale(glideStrafe)).add(0, -glideFallSpeed, 0);

        Vec3 before = position();
        setDeltaMovement(velocity);
        move(MoverType.SELF, velocity);
        // Hitting something: keep only the speed the move really achieved, so a head-on hit stalls the dragon
        if (horizontalCollision || verticalCollision) {
            Vec3 moved = position().subtract(before);
            glideSpeed = Math.min(glideSpeed, moved.length());
            setDeltaMovement(moved);
        }
        calculateEntityAnimation(true);
    }

    /** Render roll, interpolated. Positive rolls the right wing down. */
    public float getRoll(float partialTick) {
        return Mth.lerp(partialTick, rollO, roll);
    }

    /** Extra render pitch on top of XRot, interpolated. Negative lifts the nose. */
    public float getTiltPitch(float partialTick) {
        return Mth.lerp(partialTick, tiltPitchO, tiltPitch);
    }

    private void tickRoll() {
        rollO = roll;
        tiltPitchO = tiltPitch;
        float yawDelta = Mth.degreesDifference(lastYaw, getYRot());
        lastYaw = getYRot();
        float targetRoll = 0.0f;
        float targetPitch = 0.0f;
        if (isInFluidMode() && getControllingPassenger() != null) {
            targetRoll = yawDelta * ROLL_PER_YAW_DEGREE;
            // Sideways and backward speed relative to the heading, as a share of full strafe / reverse speed
            double cruise = isInSwimMode() ? swimCruiseSpeed() : getAttributeValue(Attributes.FLYING_SPEED) * RIDDEN_FLIGHT_SPEED_FACTOR;
            double yaw = Math.toRadians(getYRot());
            double dx = getX() - xo;
            double dz = getZ() - zo;
            double rightward = dx * -Math.cos(yaw) + dz * -Math.sin(yaw);
            targetRoll += FREE_STRAFE_ROLL * (float) Mth.clamp(rightward / (cruise * RIDDEN_STRAFE_FACTOR), -1.0, 1.0);
            if (isInSwimMode() || getFlightMode() == FlightMode.FREE) {
                double backward = -(dx * -Math.sin(yaw) + dz * Math.cos(yaw));
                targetPitch = -FREE_REVERSE_PITCH * (float) Mth.clamp(backward / (cruise * RIDDEN_REVERSE_FACTOR), 0.0, 1.0);
            }
            targetRoll = Mth.clamp(targetRoll, -MAX_ROLL, MAX_ROLL);
        }
        roll += (targetRoll - roll) * ROLL_SMOOTHING;
        tiltPitch += (targetPitch - tiltPitch) * ROLL_SMOOTHING;
    }

    @Override
    protected Vec3 getRiddenInput(Player rider, Vec3 input) {
        float strafe = rider.xxa * RIDDEN_STRAFE_FACTOR;
        float forward = rider.zza;
        if (forward <= 0.0f) forward *= RIDDEN_REVERSE_FACTOR;
        return new Vec3(strafe, 0.0, forward);
    }

    @Override
    protected float getRiddenSpeed(Player rider) {
        return (float) getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    @Override
    public double getPassengersRidingOffset() {
        return SADDLE_HEIGHT * (isBaby() ? 0.5 : 1.0);
    }

    /** In flight the saddle point swings with the body's pitch and roll about the render pivot. */
    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        if (!hasPassenger(passenger)) return;
        if (!isInFluidMode()) {
            super.positionRider(passenger, callback);
            return;
        }
        double scale = isBaby() ? 0.5 : 1.0;
        double pivot = BODY_PIVOT_HEIGHT * scale;
        // The rider's own offset swings with the body too; added straight down it pulls them off the back in a steep dive
        double seat = SADDLE_HEIGHT * scale + passenger.getMyRidingOffset() - pivot;
        double pitch = Math.toRadians(getXRot() + tiltPitch);
        double rollRad = Math.toRadians(roll);
        double yaw = Math.toRadians(getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 offset = new Vec3(0, pivot + seat * Math.cos(pitch) * Math.cos(rollRad), 0)
                .add(forward.scale(seat * Math.sin(pitch)))
                .add(right.scale(seat * Math.sin(rollRad)));
        callback.accept(passenger, getX() + offset.x, getY() + offset.y, getZ() + offset.z);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        riderAscending = false;
        riderDescending = false;
        riderFreeCam = false;
        riderSprinting = false;
        wasFreeCam = false;
        freeCamCatchingUp = false;
        riddenFlightTicks = 0;
        glideSpeed = GLIDE_SPEED_UNSET;
        glideFallSpeed = 0;
        glideStallTicks = 0;
        glideStrafe = 0;
    }

    @Override
    protected void dropEquipment() {
        super.dropEquipment();
        ItemStack saddle = inventory.getItem(SADDLE_SLOT);
        if (!saddle.isEmpty()) spawnAtLocation(saddle);
    }

    /** Dragons never take fall damage, flying species or not. */
    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return canSwim();
    }

    /** A swimming dragon holds its curve against river currents. */
    @Override
    public boolean isPushedByFluid() {
        return !isInSwimMode();
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable("subspecies." + getSubspecies().replace(':', '.'));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof DragonStaffItem staff) return staff.useOnDragon(stack, player, this);
        if (!isTame()) {
            Optional<DragonSpecies> species = ModRegistries.species(level().registryAccess(), getSubspecies());
            if (species.isEmpty() || !species.get().isFood(stack)) return super.mobInteract(player, hand);
            if (level().isClientSide) return InteractionResult.CONSUME;

            boolean favourite = species.get().isFavouriteFood(stack);
            usePlayerItem(player, hand, stack);
            tameProgress = Math.min(TAME_THRESHOLD, tameProgress + (favourite ? FAVOURITE_FOOD_TAME_STEP : FOOD_TAME_STEP));
            if (tameProgress >= TAME_THRESHOLD && !ForgeEventFactory.onAnimalTame(this, player)) {
                tame(player);
                navigation.stop();
                level().broadcastEntityEvent(this, EntityEvent.TAMING_SUCCEEDED);
            } else {
                level().broadcastEntityEvent(this, EntityEvent.TAMING_FAILED);
            }
            return InteractionResult.SUCCESS;
        }

        if (isOwnedBy(player)) {
            if (player.isSecondaryUseActive() && stack.isEmpty()) {
                if (player instanceof ServerPlayer serverPlayer) openInventory(serverPlayer);
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if (!isFood(stack) && isSaddled() && !isVehicle() && !isBaby()) {
                if (!level().isClientSide) {
                    if (getCommand() == DragonCommand.SIT) setCommand(DragonCommand.FOLLOW);
                    player.startRiding(this);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
        }

        // Everything else a tamed dragon does is driven by the staff
        return super.mobInteract(player, hand);
    }

    /** Amorberry is the breeding food; wild dragons ignore it so only tamed pairs breed. */
    @Override
    public boolean isFood(ItemStack stack) {
        return isTame() && stack.is(ModItems.AMORBERRY.get());
    }

    public Optional<SpeciesGroup> getSpeciesGroup() {
        return ModRegistries.species(level().registryAccess(), getSubspecies()).map(DragonSpecies::species);
    }

    /** Vanilla checks same class + both in love; dragons must also be tamed and of the same parent species. */
    @Override
    public boolean canMate(Animal other) {
        if (!super.canMate(other) || !(other instanceof DragonEntity mate) || !isTame() || !mate.isTame()) return false;
        Optional<SpeciesGroup> mine = getSpeciesGroup();
        return mine.isPresent() && mine.equals(mate.getSpeciesGroup());
    }

    /** Dragons lay an egg carrying the child genome instead of spawning a baby. */
    @Override
    public void spawnChildFromBreeding(ServerLevel level, Animal mate) {
        if (mate instanceof DragonEntity other) {
            DragonGenome child = Inheritance.child(getGenome(), other.getGenome(), new Random(random.nextLong()));
            EggLaying.lay(level, this, child);
        }
        finalizeSpawnChildFromBreeding(level, mate, null);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "Movement", 5, state -> {
            // Fly is the placeholder until a misc.swim animation exists
            if (isFlying() || isInSwimMode()) return state.setAndContinue(FLY);
            if (isInSittingPose()) return state.setAndContinue(SIT);
            return state.setAndContinue(state.isMoving() ? DefaultAnimations.WALK : DefaultAnimations.IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
