# Dragon species file format

A subspecies is one JSON file at `data/<namespace>/heart_of_scales/dragon_species/<name>.json`. The built-in six live
under `src/main/resources/data/heart_of_scales/heart_of_scales/dragon_species/`. Any datapack can add more; a new
file appears as an egg in the creative tab, is picked by random spawns, and hatches under its condition with no code
change. Its display name comes from a lang entry `subspecies.<namespace>.<name>`.

Datapack registries load once when a world opens. Edits need the world reopened, not `/reload`.

## Required fields

| field | values | meaning |
|---|---|---|
| `species` | `land`, `air`, `water` | Parent group. Dragons only breed within a group. |
| `egg_tint` | `"#RRGGBB"` | Egg colour, and for now the dragon's body tint. |
| `foods` | item tag id | Items that count as food for taming. |
| `favourite_food` | item id | Worth more taming points. Counts as food even if not in the tag. |
| `hatch_condition` | `dark`, `dim`, `open_air`, `waterlogged` | What the nest needs before the egg can hatch. |

## Optional fields

| field | default | meaning |
|---|---|---|
| `flies` | `false` | Can take off and be flown. |
| `swims` | `false` | Roams in water, crawls on land, breathes underwater. |
| `stats` | all defaults | Tuning numbers, see below. |

## Stats

`stats` is an object of sections. Every section is optional, and every field inside a section is optional. Leave out
anything you do not want to change. A file with no `stats` at all gets exactly the values below.

Units follow Minecraft's tick-based conventions: 20 ticks per second, speeds in blocks per tick, angles in degrees,
turn rates in degrees per tick. A "factor" multiplies something else. A "responsiveness" or "smoothing" value between
0 and 1 says how much of the gap to the target is closed each tick, where 1 is instant.

### Complete example with every field at its default

```json
{
  "species": "air",
  "flies": true,
  "swims": false,
  "egg_tint": "#9BA98E",
  "foods": "heart_of_scales:dragon-food/air",
  "favourite_food": "minecraft:cooked_rabbit",
  "hatch_condition": "open_air",
  "stats": {
    "attributes": {
      "max_health": 30.0,
      "movement_speed": 0.25,
      "flying_speed": 0.6,
      "attack_damage": 4.0,
      "follow_range": 16.0
    },
    "stamina": {
      "max": 100.0,
      "drain_seconds": 5.0,
      "regen": 0.5,
      "regen_delay_ticks": 20,
      "recovered_fraction": 0.25
    },
    "taming": {
      "threshold": 300,
      "food_points": 10,
      "favourite_food_points": 30
    },
    "ground": {
      "sprint_speed_factor": 3.0,
      "ridden_strafe_factor": 0.5,
      "ridden_reverse_factor": 0.25
    },
    "flight": {
      "ridden_speed_factor": 1.0,
      "ascend_input": 0.8,
      "free_responsiveness": 0.3,
      "landing_grace_ticks": 10,
      "roll_per_yaw_degree": 8.0,
      "max_roll": 50.0,
      "roll_smoothing": 0.15,
      "free_strafe_roll": 25.0,
      "free_reverse_pitch": 20.0,
      "free_cam_key_pitch_rate": 2.5,
      "free_cam_bank_turn_rate": 3.0,
      "free_cam_bank_smoothing": 0.15,
      "free_cam_level_rate": 4.0,
      "free_cam_release_turn_rate": 8.0,
      "free_cam_head_yaw_limit": 70.0,
      "ai_max_yaw_turn": 3.0,
      "ai_max_pitch_turn": 10.0,
      "roam_takeoff_chance": 600,
      "roam_min_ticks": 300,
      "roam_max_ticks": 600,
      "roam_min_distance": 8.0,
      "roam_max_distance": 16.0,
      "roam_min_height": 3,
      "roam_max_height": 12
    },
    "glide": {
      "yaw_rate": 4.0,
      "pitch_rate": 3.0,
      "max_speed_factor": 3.5,
      "stall_speed_factor": 0.5,
      "neutral_pitch_min": 4.0,
      "neutral_pitch_max": 6.0,
      "dive_accel": 0.06,
      "climb_decel": 0.042,
      "sprint_extra_speed": 0.5,
      "sprint_accel": 0.025,
      "sprint_excess_bleed": 0.05,
      "strafe_responsiveness": 0.2,
      "stall_fall_accel": 0.015,
      "stall_fall_max": 0.6,
      "stall_fall_recovery": 0.8,
      "stall_pitch_rate": 9.0,
      "stall_fall_to_speed": 0.25,
      "stall_assist_delay_ticks": 10,
      "free_cam_stall_assist_pitch": 30.0,
      "free_cam_stall_assist_rate": 4.0
    },
    "swim": {
      "accel": 0.06,
      "drag": 0.9,
      "ridden_responsiveness": 0.15,
      "ai_max_yaw_turn": 5.0,
      "ai_max_pitch_turn": 10.0,
      "roam_min_ticks": 300,
      "roam_max_ticks": 600,
      "rest_min_ticks": 200,
      "rest_max_ticks": 400,
      "roam_min_distance": 8.0,
      "roam_max_distance": 16.0,
      "return_horizontal_range": 16,
      "return_vertical_range": 4
    },
    "home": {
      "range_horizontal": 16,
      "range_down": 3,
      "range_up": 17,
      "check_interval_ticks": 20
    }
  }
}
```

### attributes

Vanilla attributes, set as base values when the dragon gets its genome. The water-bound crawl multiplies
`movement_speed` on top of this.

| field | meaning |
|---|---|
| `max_health` | Health points. 30 is 15 hearts. |
| `movement_speed` | Walking speed. Vanilla mobs sit around 0.2 to 0.3. |
| `flying_speed` | Cruise speed in the air, blocks per tick. Free flight, glide and AI flight all scale from this. |
| `attack_damage` | Melee damage per hit. |
| `follow_range` | How far it notices targets and its owner. |

### stamina

Sprinting while ridden in the air or water spends stamina; the bar is drawn under the speed readout.

| field | meaning |
|---|---|
| `max` | Size of the bar. Only matters relative to `regen`. |
| `drain_seconds` | How long a full bar lasts while sprinting. |
| `regen` | Stamina gained per tick while not sprinting. |
| `regen_delay_ticks` | Ticks after sprinting stops before regen starts. |
| `recovered_fraction` | Once the bar runs dry, sprint is locked until this share of it is back. |

### taming

Feeding a wild dragon adds points. It is tamed at the threshold.

| field | meaning |
|---|---|
| `threshold` | Points needed. |
| `food_points` | Points per item from the `foods` tag. |
| `favourite_food_points` | Points per `favourite_food` item. |

### ground

Ridden input handling that applies in every mode.

| field | meaning |
|---|---|
| `sprint_speed_factor` | Speed multiplier while sprinting in free flight or swimming. |
| `ridden_strafe_factor` | Scale on the rider's A and D input. |
| `ridden_reverse_factor` | Scale on the rider's S input. |

### flight

Ridden free flight, the render lean, free cam, and the AI's own flying.

| field | meaning |
|---|---|
| `ridden_speed_factor` | Multiplies `flying_speed` for ridden cruise. |
| `ascend_input` | Vertical input strength for the ascend and descend keys, 0 to 1. |
| `free_responsiveness` | How fast free-flight velocity chases the wanted direction. |
| `landing_grace_ticks` | Ticks after take-off before touching ground counts as landing. |
| `roll_per_yaw_degree` | Render roll per degree of yaw change per tick. |
| `max_roll` | Render roll cap, degrees. |
| `roll_smoothing` | How fast the render roll and reverse pitch chase their targets. |
| `free_strafe_roll` | Render roll at full sideways speed, degrees. |
| `free_reverse_pitch` | Render nose-up at full reverse speed, degrees. |
| `free_cam_key_pitch_rate` | Free cam glide: pitch change per tick from W and S. |
| `free_cam_bank_turn_rate` | Free cam glide: full turn rate from A and D. |
| `free_cam_bank_smoothing` | Free cam glide: how fast the turn rate ramps in and out. |
| `free_cam_level_rate` | Free cam free flight: how fast pitch levels out. |
| `free_cam_release_turn_rate` | After free cam is released, how fast the body swings back to the look. |
| `free_cam_head_yaw_limit` | How far the head may turn from the body to follow the rider's look, degrees. |
| `ai_max_yaw_turn` | AI flight: yaw turn per tick. |
| `ai_max_pitch_turn` | AI flight: pitch turn per tick. |
| `roam_takeoff_chance` | Each tick on the ground an idle dragon has a 1 in this chance to take off. |
| `roam_min_ticks`, `roam_max_ticks` | How long a roaming flight lasts. |
| `roam_min_distance`, `roam_max_distance` | How far ahead each roam target is picked. |
| `roam_min_height`, `roam_max_height` | Blocks above the ground a roam target sits. |

### glide

Glide mode: speed is momentum carried along the heading, and pitch is the only thing that changes it. Speed
factors multiply the ridden cruise speed.

| field | meaning |
|---|---|
| `yaw_rate`, `pitch_rate` | How fast the heading chases the rider's look. |
| `max_speed_factor` | Top speed from diving. |
| `stall_speed_factor` | Below this the wings stop carrying the dragon. |
| `neutral_pitch_min`, `neutral_pitch_max` | Pitch band below the horizon where speed holds steady. |
| `dive_accel` | Speed gained per tick in a vertical dive. |
| `climb_decel` | Speed lost per tick in a vertical climb at cruise. Grows with speed above cruise. |
| `sprint_extra_speed` | Blocks per tick sprint adds above the normal cap. |
| `sprint_accel` | Speed added per tick while sprinting. |
| `sprint_excess_bleed` | Speed shed per tick after sprint is released above the normal cap. |
| `strafe_responsiveness` | How fast the side-slip reaches full strafe speed. |
| `stall_fall_accel` | Fall speed gained per tick while stalled. |
| `stall_fall_max` | Fall speed cap while stalled. |
| `stall_fall_recovery` | Fall speed kept each tick once out of the stall. |
| `stall_pitch_rate` | Pitch rate while stalled, so a dive can be set up quickly. |
| `stall_fall_to_speed` | Share of the fall turned into glide speed each tick once the nose is down. |
| `stall_assist_delay_ticks` | Stalled this long, the rider's look starts easing down into a dive. |
| `free_cam_stall_assist_pitch` | In free cam, the assist tips the body to this pitch. |
| `free_cam_stall_assist_rate` | How fast it tips. |

### swim

AI swimming pushes itself by `accel` each tick and is slowed by `drag`, settling at `accel * drag / (1 - drag)`
blocks per tick. Ridden swimming cruises at that same speed.

| field | meaning |
|---|---|
| `accel` | Push per tick. |
| `drag` | Share of velocity kept each tick. |
| `ridden_responsiveness` | How fast ridden swim velocity chases the wanted direction. |
| `ai_max_yaw_turn`, `ai_max_pitch_turn` | AI swim turn rates per tick. |
| `roam_min_ticks`, `roam_max_ticks` | How long a roaming swim lasts. |
| `rest_min_ticks`, `rest_max_ticks` | How long it drifts between swims. |
| `roam_min_distance`, `roam_max_distance` | How far ahead each swim target is picked. |
| `return_horizontal_range`, `return_vertical_range` | How far a beached dragon looks for water. |

### home

The box around a home beacon a wandering dragon stays inside.

| field | meaning |
|---|---|
| `range_horizontal` | Blocks either side of the beacon. |
| `range_down`, `range_up` | Blocks below and above the beacon. |
| `check_interval_ticks` | How often it checks the beacon still exists. |
