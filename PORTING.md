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

## Build
- Build files come from the NeoForge MDK (ModDevGradle). No `fg.deobf`, no Mixin Gradle plugin.
- `mods.toml` becomes `src/main/templates/META-INF/neoforge.mods.toml`, expanded by `generateModMetadata`.
  Extra properties (mod_authors, mod_description) must be added to its property map in build.gradle.
- Dependencies: `mandatory=true` becomes `type="required"`.
- `pack.mcmeta` pack_format 15 becomes 48.
- Libraries: GeckoLib `geckolib-neoforge-<mc>`, Curios `curios-neoforge:<ver>+<mc>`, Patchouli `<mc>-<build>-NEOFORGE`.

## Registries and events
- `ForgeRegistries.X` becomes `Registries.X`. `RegistryObject<T>` becomes `DeferredHolder<R, T>`.
- `@Mod.EventBusSubscriber(bus = MOD)` becomes `@EventBusSubscriber(modid, value = Dist.CLIENT)`. The bus is inferred; `bus =` is deprecated.
- Mod constructor takes `IEventBus`.
- Loot modifiers: codec is a `MapCodec`, registry key `NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS`, item codec `BuiltInRegistries.ITEM.byNameCodec()`.
- `DataPackRegistryEvent.NewRegistry` is unchanged apart from the package.
- `StructureType#codec()` returns a `MapCodec` from 1.20.5, so `NestStructure.CODEC` drops the trailing `.codec()` and becomes a `MapCodec`. `PoolElementStructurePiece`, `getFirstFreeHeight` and `getBaseColumn` keep their signatures.

## Item NBT becomes data components
This is the largest change.
- Item data lives in a registered `DataComponentType` (see `registry/ModDataComponents`), not NBT.
- `stack.getOrDefault(type, default)` and `stack.set(type, value)` replace tag reads and writes.
- Block entities: `load` becomes `loadAdditional(tag, registries)`. `saveAdditional` and `getUpdateTag` take a `HolderLookup.Provider`.
- Item to block entity on placement: override `applyImplicitComponents`.
- Block entity to item for pick-block and loot: override `collectImplicitComponents` and `removeComponentsFromTag`.
- Loot tables: `copy_nbt` becomes `copy_components` with `source: block_entity` and an `include` list.
- Recipes: result is `{"id": ..., "components": {...}}`.
- Item strings in Patchouli books and commands use component syntax: `item[ns:component="value"]`, not `item{Tag:...}`.

## Blocks
- `Block#use` splits into `useItemOn` (returns `ItemInteractionResult`) and `useWithoutItem`.
  `PASS_TO_DEFAULT_BLOCK_INTERACTION` from the first falls through to the second.
- `getShape`, `entityInside` and similar are protected.
- `Properties.copy` becomes `ofFullCopy`.
- `getCloneItemStack` takes `LevelReader`.

## Client
- `RegisterGuiOverlaysEvent` / `IGuiOverlay` become `RegisterGuiLayersEvent` / `LayeredDraw.Layer`.
  `render(GuiGraphics, DeltaTracker)`, screen size from `graphics.guiWidth()`. Layer ids are ResourceLocations.
- Colour handler events keep their shape under `net.neoforged.neoforge.client.event`.
- Curios 9: `CuriosApi.getCuriosInventory` returns a plain `Optional`.

## Dragon beacon roster
- `DragonBeaconBlock.use` moves to `useWithoutItem`. Add `useItemOn` returning `SKIP_DEFAULT_BLOCK_INTERACTION` while holding the staff, so the staff's `useOn` still assigns homes.
- `DragonRoster`: `computeIfAbsent` takes a `SavedData.Factory`, and `load`/`save` take a `HolderLookup.Provider`. `Component.Serializer.toJson`/`fromJson` in `RosterEntry` need that provider too.
- `NetworkHooks.openScreen` becomes `player.openMenu(provider, buf -> ...)`, `IForgeMenuType` becomes `IMenuTypeExtension`, and `BeaconRow` encodes components with `ComponentSerialization.TRUSTED_STREAM_CODEC` on a `RegistryFriendlyByteBuf`.
- `DragonBeaconScreen`'s list: `AbstractSelectionList` takes `(minecraft, width, height, y, itemHeight)` from 1.20.2, and its background and selection flags changed; check `RowList` against the 1.21.1 class.
- `Entity.onRemovedFromWorld` and `getRemovalReason` keep their names on NeoForge.

## Data folder names
- `loot_tables` to `loot_table`, `recipes` to `recipe`, `tags/items` to `tags/item`, `tags/blocks` to `tags/block`.
- `structures` to `structure` (template NBT files). Worldgen JSON for structures is unchanged.
- `forge/` to `neoforge/`: `neoforge:add_features`, `neoforge:loot_table_id`.
