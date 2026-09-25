# Porting notes: 1.20.1 Forge to 1.21.1 NeoForge

Checklist for moving a feature from `master` (1.20.1 Forge) to the `1.21.1` branch (NeoForge).
Prefer `git cherry-pick` of the merged PR, then fix conflicts using this list.
Add anything new you hit.

## What ports untouched
- The `genome` package (no Minecraft imports).
- Species codec and `dragon_species` JSON.
- Blockstates, models, textures. NeoForge honours the root `transform` key and `render_type`.
- Patchouli book content (but see item strings below).
- Curios slot and entity JSON. Worldgen JSON. Lang files.
- GeckoLib geo, animation and texture files. Entity AI goals, move controls and navigation subclasses.
- Structure template NBT files, but see the folder rename below.

## Build
- Build files come from the NeoForge MDK (ModDevGradle). No `fg.deobf`, no Mixin Gradle plugin.
- `mods.toml` becomes `src/main/templates/META-INF/neoforge.mods.toml`, expanded by `generateModMetadata`.
  Extra properties (mod_authors, mod_description) must be added to its property map in build.gradle.
- Dependencies: `mandatory=true` becomes `type="required"`.
- `pack.mcmeta` pack_format 15 becomes 48.
- Libraries: GeckoLib `geckolib-neoforge-<mc>`, Curios `curios-neoforge:<ver>+<mc>`, Patchouli `<mc>-<build>-NEOFORGE`.
- Unit tests that touch Minecraft classes need `neoForge.addModdingDependenciesTo(sourceSets.test)` in build.gradle.
- Mixins: NeoForge loads them natively from a `[[mixins]] config="..."` block in `neoforge.mods.toml`, with `"compatibilityLevel": "JAVA_21"`
  and no `refmap` in the JSON. No annotation processor, no `-mixin.config` run arg. The 1.21.1 branch currently has no mixins:
  `CameraMixin` is replaced by NeoForge's `CalculateDetachedCameraDistanceEvent` (see `client/FlightCameraHandler`).

## Registries and events
- `ForgeRegistries.X` becomes `Registries.X`. `RegistryObject<T>` becomes `DeferredHolder<R, T>`.
- `@Mod.EventBusSubscriber(bus = MOD)` becomes `@EventBusSubscriber(modid, value = Dist.CLIENT)`. The bus is inferred; `bus =` is deprecated.
- Mod constructor takes `(IEventBus modBus, ModContainer container)`. Configs register through `container.registerConfig`.
- `ForgeConfigSpec` becomes `ModConfigSpec`, same builder API.
- Loot modifiers: codec is a `MapCodec`, registry key `NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS`, item codec `BuiltInRegistries.ITEM.byNameCodec()`.
- `DataPackRegistryEvent.NewRegistry` is unchanged apart from the package.
- `StructureType#codec()` returns a `MapCodec` from 1.20.5, so `NestStructure.CODEC` drops the trailing `.codec()` and becomes a `MapCodec`.
  `PoolElementStructurePiece` takes a trailing `LiquidSettings` (use `APPLY_WATERLOGGING`). `getFirstFreeHeight` and `getBaseColumn` keep their signatures.
- `ForgeSpawnEggItem` becomes `DeferredSpawnEggItem`. `EntityAttributeCreationEvent` keeps its shape under `net.neoforged.neoforge.event.entity`.
- `ForgeEventFactory.onAnimalTame` becomes `EventHooks.onAnimalTame`.
- Tick events: `TickEvent.ServerTickEvent` with `Phase.END` becomes `ServerTickEvent.Post`; `TickEvent.ClientTickEvent` START and END become
  `ClientTickEvent.Pre` and `ClientTickEvent.Post`; `TickEvent.RenderTickEvent` START becomes `RenderFrameEvent.Pre`.
  `Minecraft#getDeltaFrameTime` becomes `event.getPartialTick().getGameTimeDeltaTicks()`.
- `RenderPlayerEvent.Pre`, `RegisterKeyMappingsEvent`, `KeyConflictContext` and `ServerStoppedEvent` keep their shape under `net.neoforged.*`.

## Networking
- `SimpleChannel` is gone. A packet is a record implementing `CustomPacketPayload` with a `Type` and a `StreamCodec`.
- Register in a mod-bus `RegisterPayloadHandlersEvent` listener: `event.registrar(version).playToServer(TYPE, CODEC, handler)`.
- Handler is `(payload, IPayloadContext context)`; `context.player()` is the sender, `context.enqueueWork` runs on the main thread.
- Client sends with `PacketDistributor.sendToServer(payload)`.

## Entities
- `defineSynchedData()` becomes `defineSynchedData(SynchedEntityData.Builder builder)` and `builder.define(...)`.
- `finalizeSpawn` loses its trailing `CompoundTag` parameter.
- `ItemStack#save(CompoundTag)` becomes `save(registryAccess())`; `ItemStack.of(tag)` becomes `ItemStack.parse(registryAccess(), tag).orElse(EMPTY)`.
- `NbtUtils.writeBlockPos` returns an int array tag and `readBlockPos(tag, key)` returns an `Optional<BlockPos>`.
- `FollowOwnerGoal` drops its trailing `canFly` boolean.
- `onAddedToWorld` becomes `onAddedToLevel`.
- `NetworkHooks.openScreen(player, provider, writer)` becomes `player.openMenu(provider, writer)`.
- `TamableAnimal#setTame(boolean)` becomes `setTame(boolean, boolean applyTamingSideEffects)`.
- `canBreatheUnderwater` is final. Override `canDrownInFluidType(FluidType)` instead.
- Riding offsets: `getPassengersRidingOffset` becomes `getPassengerAttachmentPoint(entity, dimensions, partialTick)` returning a `Vec3`,
  and `passenger.getMyRidingOffset()` becomes `-passenger.getVehicleAttachmentPoint(this).y`. A player's vehicle attachment is 0.6 where the
  1.20.1 riding offset was 0.35, so the saddle point gains 0.25 to sit the rider where it did (`DragonEntity.SEAT_ADJUST`).
- `AttributeModifier` is keyed by `ResourceLocation`, not UUID and name. `Operation.MULTIPLY_TOTAL` becomes `ADD_MULTIPLIED_TOTAL`.
  `hasModifier` and `removeModifier` take the id.

## Item NBT becomes data components
This is the largest change.
- Item data lives in a registered `DataComponentType` (see `registry/ModDataComponents`), not NBT.
- `stack.getOrDefault(type, default)` and `stack.set(type, value)` replace tag reads and writes. `removeTagKey` becomes `stack.remove(type)`,
  `tag.hasUUID` becomes `stack.has(type)`. UUIDs use `UUIDUtil.CODEC` and `UUIDUtil.STREAM_CODEC`.
- `Item#appendHoverText` takes an `Item.TooltipContext` instead of a `Level`.
- Block entities: `load` becomes `loadAdditional(tag, registries)`. `saveAdditional`, `getUpdateTag` and `onDataPacket` take a `HolderLookup.Provider`.
- Item to block entity on placement: override `applyImplicitComponents`.
- Block entity to item for pick-block and loot: override `collectImplicitComponents` and `removeComponentsFromTag`.
- Loot tables: `copy_nbt` becomes `copy_components` with `source: block_entity` and an `include` list.
- Recipes: result is `{"id": ..., "components": {...}}`.
- Item strings in Patchouli books and commands use component syntax: `item[ns:component="value"]`, not `item{Tag:...}`.

## Blocks
- `Block#use` splits into `useItemOn` (returns `ItemInteractionResult`) and `useWithoutItem`.
  `PASS_TO_DEFAULT_BLOCK_INTERACTION` from the first falls through to the second.
- `getShape`, `entityInside`, `getFluidState`, `updateShape` and similar are protected.
- `Properties.copy` becomes `ofFullCopy`.
- `getCloneItemStack` takes `LevelReader`.
- `playerWillDestroy` returns the `BlockState`.
- Every `Block` subclass needs a `codec()`; for a plain constructor use `simpleCodec(MyBlock::new)`.

## Client
- `RegisterGuiOverlaysEvent` / `IGuiOverlay` become `RegisterGuiLayersEvent` / `LayeredDraw.Layer`.
  `render(GuiGraphics, DeltaTracker)`, screen size from `graphics.guiWidth()`. Layer ids are ResourceLocations.
- Colour handler events keep their shape under `net.neoforged.neoforge.client.event`.
- `MenuScreens.register` in `FMLClientSetupEvent` becomes a `RegisterMenuScreensEvent` listener. `IForgeMenuType.create` becomes `IMenuTypeExtension.create`.
- `AbstractContainerScreen#render` already draws the background; drop the explicit `renderBackground(graphics)` call.
- `InventoryScreen.renderEntityInInventoryFollowsMouse` takes a box `(x1, y1, x2, y2)`, scale, y offset and the raw mouse position. Use the
  horse screen's numbers: `(left + 26, top + 18, left + 78, top + 70, 17, 0.25f, mouseX, mouseY, entity)`.
- Curios 9: `CuriosApi.getCuriosInventory` returns a plain `Optional`.
- GeckoLib 4.9: `software.bernie.geckolib.core.animation.*` becomes `software.bernie.geckolib.animation.*`,
  `core.animatable.instance.AnimatableInstanceCache` becomes `animatable.instance.AnimatableInstanceCache`, `core.object.Color` becomes `util.Color`.
  The five-argument `applyRotations` is deprecated in favour of a six-argument overload with a trailing `nativeScale`.

## Data folder names
- `loot_tables` to `loot_table`, `recipes` to `recipe`, `tags/items` to `tags/item`, `tags/blocks` to `tags/block`.
- `structures` to `structure` (template NBT files). Worldgen JSON for structures is unchanged.
- `forge/` to `neoforge/`: `neoforge:add_features`, `neoforge:loot_table_id`.
