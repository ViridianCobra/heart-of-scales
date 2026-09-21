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
- Riding. The owner mounts a saddled dragon and steers on the ground with the usual keys. On air dragons, hold Ascend (Space) to take off. Toggle Flight Mode (F, which replaces Swap Hands while riding) switches between free flight and glide, and landing always returns the dragon to free flight. While flying, your speed in blocks per second shows beside the hotbar, and turns red when a glide is stalling. The rider stays seated on the saddle at any angle, straight up and straight down included.
- Free flight. The dragon flies where you look. W and S move forward and back, A and D strafe, Ascend (Space) climbs and Descend (Left Alt) drops; fly into the ground to land. The dragon leans into a strafe and lifts its nose when backing up.
- Glide. Nothing but the pitch changes your speed: it holds steady with the nose 4 to 6 degrees below the horizon, builds quickly in a dive and bleeds off above that, level flight included. Pulling up from a fast dive sheds the extra speed quickly, then eases. There is no flap and no brake, and Ascend and Descend do nothing in a glide. The mouse steers, W and S pitch the nose down and up, and A and D side-slip the dragon left and right without turning it, for lining up a gap without moving the camera. The dragon banks into turns and leans into a side-slip. Hitting something costs the speed you hit it with.
- Stalling. Run out of glide speed and the dragon stalls. It hangs for a moment before the fall builds, the nose answers much faster while stalled, and once the nose is down part of the fall turns into glide speed, so a dive recovers quickly. If you do nothing for half a second the view is eased down into a dive for you.
- Free cam. Hold Dragon Free Cam (right mouse button, shared with Use) while flying and the dragon stops following the camera, so you can look and aim freely; its head turns to follow your look, up to a limit. Gliding, W and S pitch the dragon and A and D bank it into a turn like a plane. In free flight WASD moves along the dragon's heading instead of the camera's. On release the dragon swings round to where you are looking rather than snapping.
- Third-person camera pulls back while flying (client config `flightCamera` to disable or adjust).
