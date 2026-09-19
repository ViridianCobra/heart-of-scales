# Changelog

Each branch carries its own copy. What a version contains on a given target is whatever is listed under it on that branch.
When a version is released, drop "(Unreleased)" from its heading and start the next version above it.

## 0.0.1 (Unreleased)
- Dragon egg block and item carrying a subspecies, tinted per species.
- Nest block that holds one egg. Right-click to insert, empty hand to remove.
- Six subspecies defined as datapack JSON.
- Dracip crop. Seeds drop rarely from grass. Petals and seeds from mature plants. Keeps dry farmland from reverting to dirt, like wheat.
- Amorberry bush, picked like sweet berries, generating in forest and birch biomes.
- Dragon scale item, worn as a Curios charm, shows egg subspecies under the crosshair.
- Dragon Codex guidebook via Patchouli, crafted from a book and a dragon scale.
- Cave nest structure generating on cave floors in all overworld biomes. Needs six blocks of headroom and fills the ground beneath it so it never hangs off a ledge. Avoids flooded caves and spots where it would breach a neighbouring water or lava pocket.
- Nest structures generate after ores, stone blobs and vegetation, so their blocks are no longer replaced and trees do not grow inside them.
- Forest nest structure generating on the surface in forest and birch biomes.
- Glowing mushroom block, light level 9, placeable on any solid block. Placeholder brown mushroom texture.
- Breeding. Feed two tamed dragons of the same parent species amorberries and they lay an egg with one parent's subspecies, into a nearby empty nest if there is one, else on the ground beside them.
- Flight for air dragons (plains, mountain). Idle dragons take off, sweep around and land on their own. Dragons never take fall damage.
- Dragon saddle item and a horse-style dragon inventory, opened by the owner with a crouch right-click.
- Riding. The owner mounts a saddled dragon and steers on the ground with the usual keys. On air dragons, hold Ascend (Space) to take off and fly where you look; fly into the ground to land. Toggle Flight Mode (G) switches between free flight and glide, where dives build speed, climbs bleed it and the dragon banks into turns.
- Third-person camera pulls back while flying (client config `flightCamera` to disable or adjust).
