package net.basilisk.heartofscales.entity;

import net.basilisk.heartofscales.block.DragonBeaconBlock;
import net.basilisk.heartofscales.entity.ai.DragonFlightMoveControl;
import net.basilisk.heartofscales.entity.ai.DragonFollowOwnerGoal;
import net.basilisk.heartofscales.entity.ai.OwnerCatchUp;
import net.basilisk.heartofscales.entity.ai.DragonLookControl;
import net.basilisk.heartofscales.entity.ai.DragonRoamFlightGoal;
import net.basilisk.heartofscales.entity.ai.DragonRoamSwimGoal;
import net.basilisk.heartofscales.entity.ai.DragonSwimMoveControl;
import net.basilisk.heartofscales.entity.ai.FleeCarelessPlayerGoal;
import net.basilisk.heartofscales.entity.ai.ReturnToWaterGoal;
import net.basilisk.heartofscales.entity.ai.TemptWithFoodGoal;
import net.basilisk.heartofscales.genome.DragonGenome;
import net.basilisk.heartofscales.genome.Inheritance;
import net.basilisk.heartofscales.item.ChowItem;
import net.basilisk.heartofscales.item.DragonStaffItem;
import net.basilisk.heartofscales.menu.DragonMenu;
import net.basilisk.heartofscales.registry.ModItems;
import net.basilisk.heartofscales.nbt.GenomeNbt;
import net.basilisk.heartofscales.roster.DragonRoster;
import net.basilisk.heartofscales.species.DragonSpecies;
import net.basilisk.heartofscales.species.ModRegistries;
import net.basilisk.heartofscales.species.SpeciesGroup;
import net.basilisk.heartofscales.species.stats.AttributeStats;
import net.basilisk.heartofscales.species.stats.DragonStats;
import net.basilisk.heartofscales.species.stats.FlightStats;
import net.basilisk.heartofscales.species.stats.GlideStats;
import net.basilisk.heartofscales.species.stats.GroundStats;
import net.basilisk.heartofscales.species.stats.HomeStats;
import net.basilisk.heartofscales.species.stats.JumpStats;
import net.basilisk.heartofscales.species.stats.StaminaStats;
import net.basilisk.heartofscales.species.stats.TamingStats;
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
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
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
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class DragonEntity extends TamableAnimal implements GeoEntity, PlayerRideableJumping {
    private static final EntityDataAccessor<String> DATA_SUBSPECIES =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.STRING);
    private static final String TAG_GENOME = "Genome";
    private static final String TAG_TAME_PROGRESS = "TameProgress";
    private static final String TAG_TAMED_AT = "TamedAt";
    private static final int ROSTER_UPDATE_INTERVAL = 100;
    private static final EntityDataAccessor<Byte> DATA_COMMAND =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BYTE);
    private static final String TAG_BEACON = "Beacon";
    private static final String TAG_BEACON_DIMENSION = "BeaconDimension";
    private static final String TAG_WILD_HOME = "WildHome";
    private static final String TAG_WILD_HOME_DIMENSION = "WildHomeDimension";
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
    private static final EntityDataAccessor<Float> DATA_AI_BANK =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_GLIDING =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    /** Share of the way the client roll closes on the synced AI bank each tick; the server has already eased it. */
    private static final float SYNCED_BANK_EASING = 0.5f;
    private static final int LEFT_BEHIND_CHECK_TICKS = 20;
    private static final EntityDataAccessor<Boolean> DATA_EXHAUSTED =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> DATA_FLIGHT_MODE =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_JUMP_STATE =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BYTE);
    private static final String TAG_FLIGHT_MODE = "FlightMode";
    public static final int SADDLE_SLOT = 0;
    private static final int INVENTORY_SIZE = 1;
    /** Marks glideSpeed as not yet seeded; the first glide tick takes the speed the dragon already has. */
    private static final double GLIDE_SPEED_UNSET = -1.0;
    /** A ridden jump still on the ground after this many ticks is over: a low ceiling, or lift-off the server never heard of. */
    private static final int JUMP_LIFTOFF_TICKS = 10;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private DragonGenome genome = DragonGenome.defaultGenome();
    private boolean genomeAssigned;
    // Resolved from the genome's subspecies id; never null, so stats can be read before the first sync arrives
    private DragonSpecies species = DragonSpecies.DEFAULT;
    private DragonBody body = DragonBody.PLACEHOLDER;
    private int tameProgress;
    private long tamedAt;
    @Nullable
    private GlobalPos home;
    @Nullable
    private GlobalPos wildHome;
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
    private boolean riderJumping;
    private int staminaRestTicks;
    private boolean wasFreeCam;
    private boolean freeCamCatchingUp;
    private boolean swimNavigationConfigured;
    private float bankTurnRate;
    private int riddenFlightTicks;
    // Ridden jump: a release waiting to launch (rider's client), and the jump in progress (server)
    private int pendingJumpPower;
    private int jumpTicks;
    private boolean jumpLeftGround;
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
    // Server-side AI bank, synced through DATA_AI_BANK
    private float aiBank;
    private float lastServerYaw;

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
        // A synced value still at its default is never sent to clients, so a forest dragon never triggers onSyncedDataUpdated
        resolveSpecies();
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
        entityData.define(DATA_JUMP_STATE, (byte) JumpState.NONE.ordinal());
        entityData.define(DATA_STAMINA, StaminaStats.DEFAULT.max());
        entityData.define(DATA_AI_BANK, 0.0f);
        entityData.define(DATA_GLIDING, false);
        entityData.define(DATA_EXHAUSTED, false);
    }

    public FlightMode getFlightMode() {
        return FlightMode.byOrdinal(entityData.get(DATA_FLIGHT_MODE));
    }

    /** The modes this species offers, in the order the toggle key cycles them. */
    public List<FlightMode> availableFlightModes() {
        return FlightMode.available(species.flies(), species.glides());
    }

    /** The mode a landing resets to, and the one a stored mode falls back to when the species no longer offers it. */
    private FlightMode defaultFlightMode() {
        return availableFlightModes().get(0);
    }

    private void setFlightMode(FlightMode mode) {
        entityData.set(DATA_FLIGHT_MODE, (byte) mode.ordinal());
    }

    /** Cycles to the next mode the species offers. With a single mode there is nothing to switch to, so nothing happens. */
    public void toggleFlightMode(Player rider) {
        if (isInSwimMode()) return;
        FlightMode mode = getFlightMode().next(availableFlightModes());
        if (mode == getFlightMode()) return;
        setFlightMode(mode);
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

    /** The vanilla Jump key, which charges the ridden jump. */
    public void setRiderJumping(boolean jumping) {
        this.riderJumping = jumping;
    }

    /** Stamina left, 0 to 1. */
    public float getStaminaFraction() {
        return entityData.get(DATA_STAMINA) / getStats().stamina().max();
    }

    public boolean isExhausted() {
        return entityData.get(DATA_EXHAUSTED);
    }

    /**
     * Whether the sprint is actually happening this tick: key held, stamina to spend, and some movement to boost, so
     * hovering or standing with the key held costs nothing. On the ground only forward counts, as for a player. The
     * same answer on the server, which spends the stamina, and on the rider's client, which applies the speed.
     */
    public boolean isSprinting() {
        if (!riderSprinting || isExhausted() || entityData.get(DATA_STAMINA) <= 0.0f) return false;
        if (!(getControllingPassenger() instanceof Player rider)) return false;
        if (!isInFluidMode()) return rider.zza > 0.0f;
        if (isFlying() && getFlightMode() == FlightMode.GLIDE) return true;
        return rider.xxa != 0.0f || rider.zza != 0.0f || riderAscending || riderDescending;
    }

    private void tickStamina() {
        StaminaStats stats = getStats().stamina();
        float stamina = entityData.get(DATA_STAMINA);
        if (isSprinting()) {
            stamina = Math.max(0.0f, stamina - stats.drainPerTick());
            staminaRestTicks = 0;
            if (stamina <= 0.0f) entityData.set(DATA_EXHAUSTED, true);
        } else if (staminaRestTicks < stats.regenDelayTicks()) {
            staminaRestTicks++;
        } else {
            stamina = Math.min(stats.max(), stamina + stats.regen());
        }
        if (isExhausted() && stamina >= stats.recoveredThreshold()) entityData.set(DATA_EXHAUSTED, false);
        entityData.set(DATA_STAMINA, stamina);
    }

    /** True on the rider's client while a glide is below stall speed; glide speed is only simulated there. */
    public boolean isGlideStalling() {
        return isFlying() && getFlightMode() == FlightMode.GLIDE && glideSpeed >= 0
                && glideSpeed < flightCruiseSpeed() * getStats().glide().stallSpeedFactor();
    }

    public boolean isStallAssistActive() {
        return isGlideStalling() && glideStallTicks > getStats().glide().stallAssistDelayTicks();
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

    /** Whether this dragon's species can take off from the ground. */
    public boolean canFly() {
        return species.flies();
    }

    /** Whether this dragon's species has glide mode. Without {@link #canFly()} the only way into the air is a fall. */
    public boolean canGlide() {
        return species.glides();
    }

    /** Whether this dragon's species swims. Water-bound species crawl on land and roam in water. */
    public boolean canSwim() {
        return species.swims();
    }

    /** Space is this dragon's jump key: the species jumps and cannot fly. A flier takes off on Space instead. */
    private boolean jumpsWhenRidden() {
        return species.jumps() && !canFly();
    }

    /**
     * Whether the rider can charge and release a jump: a jumping species out of flight and swim mode. Vanilla asks on
     * the rider's client, to charge and draw the jump bar, and on the server, to accept a release.
     */
    @Override
    public boolean canJump() {
        return jumpsWhenRidden() && !isFlying() && !isInSwimMode();
    }

    public JumpState getJumpState() {
        return JumpState.byOrdinal(entityData.get(DATA_JUMP_STATE));
    }

    /** Server only: a client's copy would be overwritten by the next sync and flicker the animation. */
    private void setJumpState(JumpState state) {
        if (!level().isClientSide) entityData.set(DATA_JUMP_STATE, (byte) state.ordinal());
    }

    /** Rider's client, on release. A release off the ground is dropped, not saved for the landing as a horse does. */
    @Override
    public void onPlayerJump(int power) {
        if (power > 0 && onGround()) pendingJumpPower = power;
    }

    /** Server, on release, so every other player sees the jump too. */
    @Override
    public void handleStartJump(int power) {
        if (onGround()) startJump();
    }

    @Override
    public void handleStopJump() {
    }

    public boolean isFlying() {
        return entityData.get(DATA_FLYING);
    }

    /** The only place gravity, move control and navigation are switched. Server side. */
    public void setFlying(boolean flying) {
        if (flying == isFlying()) return;
        if (flying && isInSwimMode()) return;
        entityData.set(DATA_FLYING, flying);
        // Every landing, ridden or not, puts the dragon back in its first mode for the next take-off
        if (!flying) setFlightMode(defaultFlightMode());
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
        return getStats().swim().cruiseSpeed();
    }

    /** Ridden free-flight cruise speed in blocks per tick: the flying speed attribute scaled by the species factor. */
    public double flightCruiseSpeed() {
        return getAttributeValue(Attributes.FLYING_SPEED) * getStats().flight().riddenSpeedFactor();
    }

    /** AI flight cruise speed in blocks per tick: the flying speed attribute scaled by the species factor. */
    public double aiFlightSpeed() {
        return getAttributeValue(Attributes.FLYING_SPEED) * getStats().flight().aiSpeedFactor();
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
        resolveSpecies();
        // The synced mode starts as free flight, which a glide-only species does not offer
        setFlightMode(FlightMode.resolve(getFlightMode(), availableFlightModes()));
        applyAttributes();
    }

    /** Synced to clients, unlike the full genome. */
    public String getSubspecies() {
        return entityData.get(DATA_SUBSPECIES);
    }

    /** The subspecies entry this dragon was resolved to, or the default if its id is unknown. */
    public DragonSpecies getSpecies() {
        return species;
    }

    /** The tunable numbers for this dragon's subspecies. */
    public DragonStats getStats() {
        return species.stats();
    }

    /** The model, seat and animations this dragon's subspecies is drawn and ridden with. */
    public DragonBody getBody() {
        return body;
    }

    /** The body's scale, halved for a baby to match the halved hitbox vanilla gives baby mobs. */
    public float getBodyScale() {
        return body.scale() * (isBaby() ? 0.5f : 1.0f);
    }

    private void resolveSpecies() {
        species = ModRegistries.speciesOrDefault(level().registryAccess(), getSubspecies());
        body = DragonBody.forSubspecies(getSubspecies());
    }

    /** The client learns the subspecies through synced data, so it resolves the species there rather than in setGenome. */
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_SUBSPECIES.equals(key) && level().isClientSide) resolveSpecies();
    }

    /** Sets the base attributes from the species. Leaves current health alone, so a loaded dragon keeps what it had. */
    private void applyAttributes() {
        AttributeStats stats = getStats().attributes();
        setBaseAttribute(Attributes.MAX_HEALTH, stats.maxHealth());
        setBaseAttribute(Attributes.MOVEMENT_SPEED, stats.movementSpeed());
        setBaseAttribute(Attributes.FLYING_SPEED, stats.flyingSpeed());
        setBaseAttribute(Attributes.ATTACK_DAMAGE, stats.attackDamage());
        setBaseAttribute(Attributes.FOLLOW_RANGE, stats.followRange());
    }

    private void setBaseAttribute(Attribute attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        if (!genomeAssigned) {
            level.registryAccess().registry(ModRegistries.DRAGON_SPECIES)
                    .flatMap(registry -> registry.getRandom(random))
                    .ifPresent(entry -> setGenome(new DragonGenome(entry.key().location().toString())));
        }
        setHealth(getMaxHealth());
        return super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put(TAG_GENOME, GenomeNbt.save(genome, new CompoundTag()));
        tag.putInt(TAG_TAME_PROGRESS, tameProgress);
        tag.putLong(TAG_TAMED_AT, tamedAt);
        tag.putString(TAG_COMMAND, getCommand().id());
        tag.putBoolean(TAG_FLYING, isFlying());
        tag.putBoolean(TAG_SWIM_MODE, isInSwimMode());
        tag.putString(TAG_FLIGHT_MODE, getFlightMode().id());
        ItemStack saddle = inventory.getItem(SADDLE_SLOT);
        if (!saddle.isEmpty()) tag.put(TAG_SADDLE, saddle.save(new CompoundTag()));
        writeGlobalPos(tag, home, TAG_BEACON, TAG_BEACON_DIMENSION);
        writeGlobalPos(tag, wildHome, TAG_WILD_HOME, TAG_WILD_HOME_DIMENSION);
    }

    private static void writeGlobalPos(CompoundTag tag, @Nullable GlobalPos pos, String posKey, String dimensionKey) {
        if (pos == null) return;
        tag.put(posKey, NbtUtils.writeBlockPos(pos.pos()));
        tag.putString(dimensionKey, pos.dimension().location().toString());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_GENOME, Tag.TAG_COMPOUND)) {
            setGenome(GenomeNbt.load(tag.getCompound(TAG_GENOME)));
        }
        tameProgress = tag.getInt(TAG_TAME_PROGRESS);
        tamedAt = tag.getLong(TAG_TAMED_AT);
        home = readGlobalPos(tag, TAG_BEACON, TAG_BEACON_DIMENSION);
        wildHome = readGlobalPos(tag, TAG_WILD_HOME, TAG_WILD_HOME_DIMENSION);
        setFlying(tag.getBoolean(TAG_FLYING));
        setSwimMode(tag.getBoolean(TAG_SWIM_MODE));
        // The species may have lost a mode since the save was written; an unavailable mode falls back to the first
        FlightMode storedMode = FlightMode.byId(tag.getString(TAG_FLIGHT_MODE));
        setFlightMode(FlightMode.resolve(storedMode, availableFlightModes()));
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
    private GlobalPos readGlobalPos(CompoundTag tag, String posKey, String dimensionKey) {
        if (!tag.contains(posKey, Tag.TAG_COMPOUND)) return null;
        ResourceLocation dimension = tag.contains(dimensionKey, Tag.TAG_STRING)
                ? ResourceLocation.tryParse(tag.getString(dimensionKey)) : null;
        ResourceKey<Level> dimensionId = dimension != null ? ResourceKey.create(Registries.DIMENSION, dimension) : level().dimension();
        return GlobalPos.of(dimensionId, NbtUtils.readBlockPos(tag.getCompound(posKey)));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
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
        goalSelector.addGoal(6, new DragonFollowOwnerGoal(this));
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
        updateRoster();
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

    /** Where a wild dragon first found itself, which its idle flights circle. Always null once tamed. */
    @Nullable
    public GlobalPos getWildHome() {
        return isTame() ? null : wildHome;
    }

    // Set here rather than on spawn so dragons from older saves get one too; a tamed dragon circles its beacon or owner instead
    private void tickWildHome() {
        if (isTame()) {
            wildHome = null;
        } else if (wildHome == null || wildHome.dimension() != level().dimension()) {
            wildHome = GlobalPos.of(level().dimension(), blockPosition());
        }
    }

    /** Gives the dragon a home and sends it there to wander. */
    public void setHome(GlobalPos home) {
        this.home = GlobalPos.of(home.dimension(), home.pos().immutable());
        setCommand(DragonCommand.WANDER);
        updateRoster();
    }

    public void clearHome() {
        this.home = null;
        if (getCommand() == DragonCommand.WANDER) setCommand(DragonCommand.FOLLOW);
        updateRoster();
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
        updateRoster();
    }

    // Entity.setRemoved is final and chunk unloads call it directly, so this Forge hook is the one place to see a dragon leave
    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        if (!isRosterTracked() || !(level() instanceof ServerLevel serverLevel)) return;
        RemovalReason reason = getRemovalReason();
        // No reason yet means its area stopped being tracked ahead of unloading
        if (reason == null || reason == RemovalReason.UNLOADED_TO_CHUNK || reason == RemovalReason.UNLOADED_WITH_PLAYER) {
            DragonRoster.get(serverLevel.getServer()).update(this);
        } else if (reason == RemovalReason.DISCARDED) {
            DragonRoster.get(serverLevel.getServer()).markRemoved(this);
        }
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
        return isHomeBound() ? getStats().home().rangeHorizontal() * 1.5f : super.getRestrictRadius();
    }

    @Override
    public boolean isWithinRestriction(BlockPos pos) {
        return isHomeBound() ? isInsideHomeArea(pos) : super.isWithinRestriction(pos);
    }

    /** Inside the box around its home beacon, whatever it has been told to do. False with no home here. */
    public boolean isInsideHomeArea(BlockPos pos) {
        if (!isHomeInThisDimension()) return false;
        BlockPos beacon = home.pos();
        HomeStats range = getStats().home();
        return Math.abs(pos.getX() - beacon.getX()) <= range.rangeHorizontal()
                && Math.abs(pos.getZ() - beacon.getZ()) <= range.rangeHorizontal()
                && pos.getY() >= beacon.getY() - range.rangeDown()
                && pos.getY() <= beacon.getY() + range.rangeUp();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            tickRoll();
            return;
        }
        tickAiBank();
        tickGliding();
        tickLeftBehind();
        tickStamina();
        WaterBoundCrawl.tick(this);
        if (isInSwimMode()) RiddenSwimming.topUpPassengerAir(this);
        if (!swimNavigationConfigured && canSwim()) {
            // Species is not known in the constructor; once it is, let land paths enter water instead of stopping at the bank
            groundNavigation.setCanFloat(true);
            swimNavigationConfigured = true;
        }
        if (tickCount % ROSTER_UPDATE_INTERVAL == 0) updateRoster();
        tickWildHome();
        if (home == null || tickCount % getStats().home().checkIntervalTicks() != 0) return;
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

    /**
     * Runs on the server and the rider's client, so take-off and landing are decided identically on both. Vanilla also
     * runs it on every other client that sees the rider, so nothing here may write synced state on a client.
     */
    @Override
    protected void tickRidden(Player rider, Vec3 input) {
        super.tickRidden(rider, input);
        boolean freeCam = riderFreeCam && isInFluidMode();
        if (freeCam && !wasFreeCam) bankTurnRate = 0.0f;
        if (!freeCam && wasFreeCam) freeCamCatchingUp = true;
        wasFreeCam = freeCam;

        FlightStats flight = getStats().flight();
        if (isFlying() && getFlightMode() == FlightMode.GLIDE) {
            // Glide speed lives on the rider's client, so only that side ever sees a stall
            GlideStats glide = getStats().glide();
            boolean stalling = isGlideStalling();
            glideStallTicks = stalling ? glideStallTicks + 1 : 0;
            if (freeCam) {
                tickFreeCamGlide(rider);
            } else {
                // Momentum: the heading lags behind the look
                setRot(Mth.approachDegrees(getYRot(), rider.getYRot(), glide.yawRate()),
                        Mth.approachDegrees(getXRot(), rider.getXRot(), stalling ? glide.stallPitchRate() : glide.pitchRate()));
            }
            freeCamCatchingUp = false;
        } else if (freeCam) {
            // Free flight keeps its yaw and levels out, so WASD moves flat along the heading and Ascend and
            // Descend handle height. Holding whatever pitch the look happened to have would leave it stuck nose up or down.
            setRot(getYRot(), Mth.approach(getXRot(), 0.0f, flight.freeCamLevelRate()));
        } else if (isInFluidMode() && freeCamCatchingUp) {
            setRot(Mth.approachDegrees(getYRot(), rider.getYRot(), flight.freeCamReleaseTurnRate()),
                    Mth.approachDegrees(getXRot(), rider.getXRot(), flight.freeCamReleaseTurnRate()));
            freeCamCatchingUp = Math.abs(Mth.degreesDifference(getYRot(), rider.getYRot())) > 1.0f
                    || Math.abs(getXRot() - rider.getXRot()) > 1.0f;
        } else {
            freeCamCatchingUp = false;
            setRot(rider.getYRot(), isInFluidMode() ? rider.getXRot() : rider.getXRot() * 0.5f);
        }
        yRotO = yBodyRot = yHeadRot = getYRot();
        if (freeCam) {
            yHeadRot += Mth.clamp(Mth.wrapDegrees(rider.getYRot() - getYRot()), -flight.freeCamHeadYawLimit(), flight.freeCamHeadYawLimit());
        }

        tickJump(input);
        RiddenSwimming.tickMode(this);
        if (isInSwimMode()) return;
        if (!isFlying()) {
            boolean takeOff = riderAscending && onGround() && canFly();
            // A glider that cannot take off opens its wings once a fall is long enough not to be a hop off a step
            boolean deploy = !canFly() && canGlide() && !onGround() && !isInWater()
                    && fallDistance > getStats().glide().autoDeployFall();
            if (takeOff || deploy) {
                setFlying(true);
                riddenFlightTicks = 0;
            }
            return;
        }
        riddenFlightTicks++;
        // A jumper's Space is its jump key, so holding it to charge the next jump must not keep it skimming the ground
        boolean holdingUp = riderAscending && !jumpsWhenRidden();
        if (onGround() && !holdingUp && riddenFlightTicks > flight.landingGraceTicks()) setFlying(false);
    }

    /**
     * Charge while Jump is held on the ground, launch on release, and end on landing. Only the server decides the synced
     * state, from the rider's Jump key and the movement the rider's client reports; the rider's client only launches.
     * Watching clients run tickRidden too and must leave the state alone.
     */
    private void tickJump(Vec3 input) {
        if (level().isClientSide) {
            if (pendingJumpPower > 0 && isControlledByLocalInstance() && canJump() && onGround()) launch(pendingJumpPower, input);
            pendingJumpPower = 0;
            return;
        }
        if (!canJump()) {
            setJumpState(JumpState.NONE);
            return;
        }
        if (getJumpState() == JumpState.JUMPING) {
            jumpTicks++;
            if (!onGround()) jumpLeftGround = true;
            else if (jumpLeftGround || jumpTicks > JUMP_LIFTOFF_TICKS) setJumpState(JumpState.NONE);
        }
        if (getJumpState() != JumpState.JUMPING) {
            setJumpState(riderJumping && onGround() ? JumpState.CHARGING : JumpState.NONE);
        }
    }

    /** The horse's jump, with the species' height in place of the horse's jump strength. Rider's client only. */
    private void launch(int power, Vec3 input) {
        JumpStats jump = getStats().jump();
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(motion.x, jump.launchSpeed(power) * getBlockJumpFactor() + getJumpBoostPower(), motion.z);
        if (input.z > 0) {
            double push = jump.forwardPush() * JumpStats.chargeScale(power);
            double yaw = Math.toRadians(getYRot());
            setDeltaMovement(getDeltaMovement().add(-Math.sin(yaw) * push, 0, Math.cos(yaw) * push));
        }
        hasImpulse = true;
        ForgeHooks.onLivingJump(this);
    }

    private void startJump() {
        setJumpState(JumpState.JUMPING);
        jumpTicks = 0;
        jumpLeftGround = false;
    }

    /** Free cam glide: the keys steer the body's own heading. A is left, which is a falling yaw; W is nose down. */
    private void tickFreeCamGlide(Player rider) {
        FlightStats flight = getStats().flight();
        GlideStats glide = getStats().glide();
        float wantedTurn = -Math.signum(rider.xxa) * flight.freeCamBankTurnRate();
        bankTurnRate += (wantedTurn - bankTurnRate) * flight.freeCamBankSmoothing();
        float pitch = getXRot() + Math.signum(rider.zza) * flight.freeCamKeyPitchRate();
        if (isStallAssistActive() && pitch < glide.freeCamStallAssistPitch()) {
            pitch = Math.min(glide.freeCamStallAssistPitch(), pitch + glide.freeCamStallAssistRate());
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
            travelFreeSteered(rider, input, swimCruiseSpeed() * (isSprinting() ? getStats().ground().sprintSpeedFactor() : 1.0),
                    getStats().swim().riddenResponsiveness(), true);
            return;
        }
        if (isInSwimMode() && isEffectiveAi() && !isVehicle()) {
            travelSwimming(input);
            return;
        }
        if (isFlying() && isEffectiveAi() && !isVehicle()) {
            // The flight move control sets the velocity; vanilla's air push and drag on top made it bounce
            move(MoverType.SELF, getDeltaMovement());
            calculateEntityAnimation(true);
            return;
        }
        super.travel(input);
    }

    /** The move control's input gives the direction; a fixed push and drag give the speed. */
    private void travelSwimming(Vec3 input) {
        if (input.lengthSqr() > 1.0e-7) {
            setDeltaMovement(getDeltaMovement().add(input.normalize().scale(getStats().swim().accel()).yRot((float) -Math.toRadians(getYRot()))));
        }
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(getStats().swim().drag()));
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
        double speed = flightCruiseSpeed() * (isSprinting() ? getStats().ground().sprintSpeedFactor() : 1.0);
        travelFreeSteered(rider, input, speed, getStats().flight().freeResponsiveness(), false);
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
        double ascend = getStats().flight().ascendInput();
        if (riderAscending) wanted = wanted.add(0, ascend, 0);
        if (riderDescending) wanted = wanted.add(0, -ascend, 0);
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
        GlideStats glide = getStats().glide();
        double cruise = flightCruiseSpeed();
        double stallSpeed = cruise * glide.stallSpeedFactor();
        if (glideSpeed < 0) glideSpeed = Math.max(getDeltaMovement().length(), stallSpeed);

        // XRot is positive nose-down, so the band sits at +4..+6
        float pitch = getXRot();
        float pastBand = pitch > glide.neutralPitchMax() ? pitch - glide.neutralPitchMax()
                : pitch < glide.neutralPitchMin() ? pitch - glide.neutralPitchMin() : 0.0f;
        double pitchEffect = Math.sin(Math.toRadians(pastBand));
        boolean sprinting = isSprinting();
        if (pitchEffect > 0) {
            glideSpeed += pitchEffect * glide.diveAccel();
        } else if (!sprinting) {
            // Powered wingbeats hold speed through a climb; only an unpowered glide bleeds it
            glideSpeed += pitchEffect * glide.climbDecel() * Math.max(1.0, glideSpeed / cruise);
        }
        double maxSpeed = cruise * glide.maxSpeedFactor();
        double sprintMaxSpeed = maxSpeed + glide.sprintExtraSpeed();
        if (sprinting) {
            // Nose above the band: wingbeats only hold what you have. In the band or diving they add.
            if (pitchEffect >= 0) glideSpeed = Math.min(sprintMaxSpeed, glideSpeed + glide.sprintAccel());
        } else if (glideSpeed > maxSpeed) {
            glideSpeed = Math.max(maxSpeed, glideSpeed - glide.sprintExcessBleed());
        }
        if (glideSpeed < stallSpeed && pastBand > 0) {
            double caught = glideFallSpeed * glide.stallFallToSpeed();
            glideSpeed += caught;
            glideFallSpeed -= caught;
        }
        glideSpeed = Mth.clamp(glideSpeed, 0.0, sprintMaxSpeed);

        if (glideSpeed < stallSpeed) glideFallSpeed = Math.min(glide.stallFallMax(), glideFallSpeed + glide.stallFallAccel());
        else glideFallSpeed *= glide.stallFallRecovery();

        glideStrafe += (input.x * cruise - glideStrafe) * glide.strafeResponsiveness();
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
        FlightStats flight = getStats().flight();
        GroundStats ground = getStats().ground();
        float targetRoll = 0.0f;
        float targetPitch = 0.0f;
        if (isInFluidMode() && getControllingPassenger() != null) {
            targetRoll = yawDelta * flight.rollPerYawDegree();
            // Sideways and backward speed relative to the heading, as a share of full strafe / reverse speed
            double cruise = isInSwimMode() ? swimCruiseSpeed() : flightCruiseSpeed();
            double yaw = Math.toRadians(getYRot());
            double dx = getX() - xo;
            double dz = getZ() - zo;
            double rightward = dx * -Math.cos(yaw) + dz * -Math.sin(yaw);
            targetRoll += flight.freeStrafeRoll() * (float) Mth.clamp(rightward / (cruise * ground.riddenStrafeFactor()), -1.0, 1.0);
            if (isInSwimMode() || getFlightMode() == FlightMode.FREE) {
                double backward = -(dx * -Math.sin(yaw) + dz * Math.cos(yaw));
                targetPitch = -flight.freeReversePitch() * (float) Mth.clamp(backward / (cruise * ground.riddenReverseFactor()), 0.0, 1.0);
            }
            targetRoll = Mth.clamp(targetRoll, -flight.maxRoll(), flight.maxRoll());
        }
        float smoothing = flight.rollSmoothing();
        if (isFlying() && getControllingPassenger() == null) {
            targetRoll = entityData.get(DATA_AI_BANK);
            smoothing = SYNCED_BANK_EASING;
        }
        roll += (targetRoll - roll) * smoothing;
        tiltPitch += (targetPitch - tiltPitch) * flight.rollSmoothing();
    }

    /**
     * AI flight leans into its turns. Worked out on the server from the exact yaw and synced: clients only get the yaw in
     * 1.4 degree steps, and only once a step is crossed, so a slow turn reaches them stop-start and the lean stuttered.
     */
    private void tickAiBank() {
        float yawDelta = Mth.degreesDifference(lastServerYaw, getYRot());
        lastServerYaw = getYRot();
        FlightStats flight = getStats().flight();
        float target = isFlying() && getControllingPassenger() == null
                ? Mth.clamp(yawDelta * flight.aiRollPerYawDegree(), -flight.maxRoll(), flight.maxRoll()) : 0.0f;
        aiBank += (target - aiBank) * flight.rollSmoothing();
        entityData.set(DATA_AI_BANK, aiBank);
    }

    /** The owner a tamed dragon told to Follow is keeping with, when they are here to follow: same dimension, not spectating. */
    @Nullable
    public LivingEntity getFollowedOwner() {
        if (!isTame() || getCommand() != DragonCommand.FOLLOW) return null;
        LivingEntity owner = getOwner();
        return owner != null && !owner.isSpectator() ? owner : null;
    }

    /**
     * A following dragon about to fall out of the area the server runs is moved to its owner while it still can be: once
     * frozen it runs no code. Checked here rather than in a goal so it applies walking, chasing in the air or otherwise.
     */
    private void tickLeftBehind() {
        if (tickCount % LEFT_BEHIND_CHECK_TICKS != 0 || isPassenger() || isLeashed() || isVehicle()
                || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity owner = getFollowedOwner();
        if (owner == null) return;
        ChunkPos here = chunkPosition();
        ChunkPos theirs = owner.chunkPosition();
        if (OwnerCatchUp.nearSimulationEdge(here.x, here.z, theirs.x, theirs.z, serverLevel.getServer().getPlayerList().getSimulationDistance())) {
            OwnerCatchUp.teleportNear(this, owner);
        }
    }

    /** Wings held out rather than flapping. Synced: the client has neither an AI dragon's exact velocity nor the rider's sprint. */
    public boolean isGliding() {
        return entityData.get(DATA_GLIDING);
    }

    private void tickGliding() {
        boolean gliding;
        if (!isFlying()) {
            gliding = false;
        } else if (getControllingPassenger() != null) {
            // Sprinting in glide mode is the powered wingbeats
            gliding = getFlightMode() == FlightMode.GLIDE && !isSprinting();
        } else {
            gliding = DragonFlightMoveControl.nextAiGliding(isGliding(), getDeltaMovement());
        }
        entityData.set(DATA_GLIDING, gliding);
    }

    @Override
    protected Vec3 getRiddenInput(Player rider, Vec3 input) {
        float strafe = rider.xxa * getStats().ground().riddenStrafeFactor();
        float forward = rider.zza;
        if (forward <= 0.0f) forward *= getStats().ground().riddenReverseFactor();
        return new Vec3(strafe, 0.0, forward);
    }

    /** Only the ground reaches this; flight and ridden swimming have their own travel. */
    @Override
    protected float getRiddenSpeed(Player rider) {
        float speed = (float) getAttributeValue(Attributes.MOVEMENT_SPEED);
        return isSprinting() ? speed * (float) getStats().ground().walkSprintSpeedFactor() : speed;
    }

    @Override
    public double getPassengersRidingOffset() {
        return body.seatHeight() * getBodyScale();
    }

    /** In flight the saddle point swings with the body's pitch and roll about the render pivot. */
    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        if (!hasPassenger(passenger)) return;
        if (!isInFluidMode()) {
            super.positionRider(passenger, callback);
            return;
        }
        double scale = getBodyScale();
        double pivot = body.pivotHeight() * scale;
        // The rider's own offset swings with the body too; added straight down it pulls them off the back in a steep dive
        double seat = body.seatHeight() * scale + passenger.getMyRidingOffset() - pivot;
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
        riderJumping = false;
        pendingJumpPower = 0;
        jumpTicks = 0;
        jumpLeftGround = false;
        setJumpState(JumpState.NONE);
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
        ItemStack saddle = inventory.removeItemNoUpdate(SADDLE_SLOT);
        if (!saddle.isEmpty()) spawnAtLocation(saddle);
    }

    @Override
    public void die(DamageSource source) {
        // super.die clears the combat log, so read the message first (vanilla TamableAnimal does the same)
        Component deathMessage = getCombatTracker().getDeathMessage();
        super.die(source);
        if (dead && isTame() && getOwnerUUID() != null && level() instanceof ServerLevel serverLevel) {
            DragonRoster.get(serverLevel.getServer()).markDied(this, deathMessage);
        }
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
    public void tame(Player player) {
        super.tame(player);
        markTamed();
    }

    /** Tames to an owner who may not be online, as when an egg hatches. */
    public void tameBy(UUID owner) {
        setTame(true);
        setOwnerUUID(owner);
        markTamed();
    }

    /** Real-world time this dragon was tamed, in epoch milliseconds, or 0 if it was tamed before this was recorded. */
    public long getTamedAt() {
        return tamedAt;
    }

    private void markTamed() {
        tamedAt = System.currentTimeMillis();
        updateRoster();
    }

    /** Living tamed dragons keep their roster entry current; death writes its own entry once. */
    private boolean isRosterTracked() {
        return isTame() && getOwnerUUID() != null && !isDeadOrDying();
    }

    private void updateRoster() {
        if (isRosterTracked() && level() instanceof ServerLevel serverLevel) {
            DragonRoster.get(serverLevel.getServer()).update(this);
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof DragonStaffItem staff) return staff.useOnDragon(stack, player, this);
        if (!isTame()) {
            if (!species.isFood(stack)) return super.mobInteract(player, hand);
            if (level().isClientSide) return InteractionResult.CONSUME;

            TamingStats taming = getStats().taming();
            boolean favourite = species.isFavouriteFood(stack);
            // Read before eating: the stack is empty once the last chow is used
            boolean chow = stack.getItem() instanceof ChowItem;
            usePlayerItem(player, hand, stack);
            if (chow) ChowItem.returnBowl(player, hand);
            tameProgress = Math.min(taming.threshold(), tameProgress + (favourite ? taming.favouriteFoodPoints() : taming.foodPoints()));
            if (tameProgress >= taming.threshold() && !ForgeEventFactory.onAnimalTame(this, player)) {
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

    public SpeciesGroup getSpeciesGroup() {
        return species.species();
    }

    /** Vanilla checks same class + both in love; dragons must also be tamed and of the same parent species. */
    @Override
    public boolean canMate(Animal other) {
        if (!super.canMate(other) || !(other instanceof DragonEntity mate) || !isTame() || !mate.isTame()) return false;
        return getSpeciesGroup() == mate.getSpeciesGroup();
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
            DragonBody.Animations animations = body.animations();
            AnimationController<DragonEntity> controller = state.getController();
            controller.transitionLength(5);
            if (isFlying()) return state.setAndContinue(isGliding() ? animations.glide() : animations.fly());
            if (isInSwimMode()) return state.setAndContinue(animations.swim());
            // GeckoLib blends into an animation's first pose before playing it. The leap starts where the crouch ends, so
            // a blend would only hold the crouch into the air; the crouch's short one keeps it in step with the jump bar.
            JumpState jump = getJumpState();
            if (jump == JumpState.CHARGING) {
                controller.transitionLength(2);
                return state.setAndContinue(animations.jumpCharge());
            }
            if (jump == JumpState.JUMPING) {
                controller.transitionLength(0);
                return state.setAndContinue(animations.jump());
            }
            if (isInSittingPose()) return state.setAndContinue(animations.sit());
            return state.setAndContinue(state.isMoving() ? animations.walk() : animations.idle());
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
