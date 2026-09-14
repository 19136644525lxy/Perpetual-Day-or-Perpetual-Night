# Perpetual Day or Perpetual Night

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://github.com/19136644525lxy/Perpetual-Day-or-Perpetual-Night/blob/main/LICENSE)
[![GitHub](https://img.shields.io/badge/GitHub-Source-blue)](https://github.com/19136644525lxy/Perpetual-Day-or-Perpetual-Night)
[![Platform](https://img.shields.io/badge/Platform-Fabric-darkgreen)](#platform-support)
[![Version](https://img.shields.io/badge/Minecraft-1.20.1-blue)](#platform-support)
[![Java](https://img.shields.io/badge/Java-17-orange)](#platform-support)

> A Minecraft mod that switches between Perpetual Day / Perpetual Night / Normal Cycle modes via commands, introducing hardcore survival mechanics such as temperature, thirst, water purification, and entity enhancement.

跳转到中文介绍: [README.md](https://github.com/19136644525lxy/Perpetual-Day-or-Perpetual-Night/blob/main/README.md)

---

## Platform Support

| Loader | Minecraft Version | Mod Version | Status |
|---|---|---|---|
| **Fabric** | 1.20.1 | `1.0.0 Fabric` | ✅ Feature-complete |

> Requires **Fabric Loader 0.19.3+** and **Fabric API 0.92.11+1.20.1**.

---

## Features

- **Three-Mode Switching**: Perpetual Day (locked at noon), Perpetual Night (locked at midnight), Normal Cycle — switch freely between all three without any key
- **Temperature System**: Body temperature is affected by biome / dimension / time / weather / altitude / nearby blocks / held items / armor insulation; extreme heat burns, extreme cold freezes
- **Drift Accumulation**: In Perpetual Day / Night modes, temperature drift accumulates by `dailyDriftAmount` (default 1°C) per day without decay; the longer the perpetual mode, the more extreme; switching between Day↔Night inverts the drift directly
- **Normal Mode Safety**: Pure environmental temperature is clamped to ±`normalSafeRange` (default ±60°C), preventing death; block and held-item effects are not clamped and can still be lethal
- **Thirst System**: Players have a hydration value (default initial 100) that drains over time; different water sources have different effects (freshwater lakes are safe, oceans / salt lakes / normal water have a 75% dehydration chance)
- **Water Purification**: Dirty water bottles / buckets must be smelted in furnace / blast furnace / smoker before drinking
- **Direct Drinking**: Right-click water surface while empty-handed to drink; freshwater lakes are 100% safe, other water sources have a 75% dehydration chance
- **Entity Enhancement**: In Perpetual Day / Night modes, hostile entities gain enhanced attributes and AI (speed / attack / health boost; neutral mobs actively track players)
- **HUD Display**: Body temperature and hydration are merged and displayed on the action bar without polluting chat
- **Data Persistence**: Player temperature / thirst data + global drift value persist across sessions; server restarts do not lose data

---

## Mode Details

| Mode | Time Lock | Temperature Drift | Entity Enhancement |
|---|---|---|---|
| **Normal Cycle** (`/pdopn cycle`) | Follows vanilla day-night cycle | No drift (decays to 0) | None |
| **Perpetual Day** (`/pdopn day`) | Noon 6000 ticks | +1°C per day, getting hotter | Undead burn, others enhanced |
| **Perpetual Night** (`/pdopn night`) | Midnight 18000 ticks | -1°C per day, getting colder | All hostile mobs enhanced |

### Drift Transition Rules

| Switch Scenario | Drift Handling |
|---|---|
| Perpetual Day → Night | `+N°C` → `-N°C`, continues accumulating toward cold |
| Perpetual Night → Day | `-N°C` → `+N°C`, continues accumulating toward hot |
| Perpetual Day/Night → Normal | No inversion, decays to 0 at `driftDecayRate` |
| Normal → Perpetual Day/Night | Starts from 0, accumulates positively/negatively |

---

## Commands

All commands require OP permission (default level 2).

| Command | Description |
|---|---|
| `/pdopn day` | Switch to Perpetual Day mode |
| `/pdopn night` | Switch to Perpetual Night mode |
| `/pdopn cycle` | Restore normal day-night cycle |
| `/pdopn status` | Show current mode |
| `/pdopn temp` | Show your body temperature |
| `/pdopn temp set <value>` | Set your body temperature (-100 to 100) |
| `/pdopn temp maxdays <days>` | Set max survival days (reference only) |
| `/pdopn temp hud` | Toggle temperature HUD display |
| `/pdopn thirst` | Show your hydration level |
| `/pdopn thirst set <value>` | Set your hydration level (0 to 100) |
| `/pdopn drift` | Show the current accumulated temperature drift (works from console) |
| `/pdopn drift reset` | Clear accumulated drift and day counter without restarting the server (works from console) |
| `/pdopn reload` | Reload `pdopn.json` (works from console) |

> `temp maxdays` currently only acts as the threshold for a one-time warning (HUD / ActionBar)
> when that many perpetual days have elapsed. It does not end the game or alter temperature math.

---

## Temperature System

### Temperature Factors

| Factor | Description |
|---|---|
| **Biome** | Desert +45°C, Ice Spikes -25°C, Nether Wastes +42°C, The End -20°C, etc. |
| **Dimension** | Nether base +40°C, End base -20°C (overrides biome temperature) |
| **Time** | Day +5°C, Night -5°C, Dusk +2°C, Dawn -2°C |
| **Weather** | Rain -3°C, Thunderstorm -5°C (full effect outdoors, 30% indoors) |
| **Altitude** | Y > 120: -0.05°C per block, Y < 0: -0.03°C per block |
| **Nearby Blocks** | Lava +15 (radius 5), Fire +10 (radius 3), Ice -5 (radius 3), etc. |
| **Held Items** | Lava bucket +0.03/tick, Ice -0.005/tick, etc. (applied every 20 ticks) |
| **Armor Insulation** | Leather set 0.15~0.20, Netherite set 0.13~0.18, reduces environmental temperature effect rate |

### Cooling Options

Hot environments now have two usable cooling paths
(previously water had **no** effect on body temperature, leaving perpetual-day + desert unsurvivable):

| Method | Effect |
|---|---|
| **Standing in water** | Body temperature moves toward the water temperature. Frozen ocean -25°C, frozen river -20°C, cold ocean +2°C, normal river/lake +12°C, warm ocean +22°C, jungle +20°C |
| **Drinking pure water** | -6°C per bottle (-20°C per bucket) over 10 seconds; direct freshwater drink -3°C, unsafe water -2°C |

> Water temperature is determined by biome; it only cools when colder than your body,
> so standing in warm water never heats you up.
> Armor insulation also slows cooling (thick armor traps heat).
> Below 0°C body temperature, cold drinks stop cooling so you cannot freeze yourself.

### Temperature Effects

| Body Temp Range | Effect |
|---|---|
| `≥ 100°C` | Burns to death (fire damage, 25% max health per hit) |
| `≥ 70°C` | Ignition + Nausea III |
| `≥ 45°C` | Slowness II + Weakness II |
| `≥ 25°C` | Slowness I |
| `≤ -100°C` | Freezes to death (freeze damage, 25% max health per hit) |
| `≤ -70°C` | Wither II + Blindness |
| `≤ -45°C` | Slowness II + Mining Fatigue II |
| `≤ -25°C` | Slowness I |

### Lethal Temperature Damage Types

The lethal damage at the temperature extremes uses custom damage types,
so the death message is mod-specific rather than the generic vanilla text:

| Case | Damage type | Death message |
|---|---|---|
| `≥ 100°C` | `pdopn:heat` | `%s's body temperature ran away and was consumed by extreme heat` |
| `≤ -100°C` | `pdopn:cold` | `%s's body temperature ran away and froze to death` |

> Defined in `data/pdopn/damage_type/heat.json` and `cold.json`.
> The translation key is `"death.attack." + message_id`, i.e.
> `death.attack.pdopn.heat` / `death.attack.pdopn.cold`
> (plus `.player` variants used when there is a killer).

---

## Thirst System

### Thirst Stages

| Hydration Range | Effect |
|---|---|
| `≥ 60` | Comfortable (no effect) |
| `40 ~ 60` | Light thirst (Slowness I) |
| `25 ~ 40` | Moderate dehydration (Slowness II + Weakness I) |
| `10 ~ 25` | Severe dehydration (Slowness III + Weakness II + Nausea I) |
| `< 10` | Extreme dehydration (Slowness IV + Weakness III + Nausea II + gradual health loss) |

### Water Source Drinking Effects

| Water Source | Drinking Result |
|---|---|
| **Freshwater Lake** (river / swamp / mangrove swamp / beach biomes, non-salt lake) | 100% restore 15 hydration |
| **Ocean** | 75% chance dehydrate 15, 25% chance restore 2.5 |
| **Salt Lake** (candidate biome with hash match) | 75% chance dehydrate 10, 25% chance restore 2.5 |
| **Normal Water** (other biome water) | 75% chance dehydrate 5, 25% chance restore 2.5 |

### Water Purification Recipes

| Input | Device | Output | Time |
|---|---|---|---|
| Water Bottle (dirty) | Furnace / Blast Furnace / Smoker | Pure Water Bottle | 200 / 100 / 100 ticks |
| Water Bucket (dirty) | Furnace / Blast Furnace / Smoker | Pure Water Bucket | 200 / 100 / 100 ticks |

> These recipes use **vanilla recipe types** (`minecraft:smelting` / `blasting` / `smoking`)
> with a vanilla water bottle or water bucket as input.
>
> ⚠️ **Known limitation**: vanilla `Ingredient` JSON only supports `item` / `tag`, and its matching
> logic (`isItemEqual` → `isOf(Item)`) does **not** compare NBT, so it cannot match "a water bottle" specifically.
> As a result, **any potion** (Healing / Strength / Fire Resistance, etc.) smelted in a furnace
> will turn into a Pure Water Bottle. Do not put valuable potions in a furnace.
>
> History: a custom `RecipeSerializer` + custom `RecipeType` was attempted to filter the input, but blocks
> index recipes by type and look them up via `RecipeManager#getAllOfType`
> (`recipes.getOrDefault(type, ...)`), so a custom type makes the recipe land in a bucket that is never queried —
> appearing as "the recipe exists but the furnace ignores it". To keep the recipes working,
> they were reverted to vanilla recipe types.

### Drinking Cooldown

Direct drinking (empty-handed right-click on water) has a 40-tick (2 seconds) cooldown to prevent rapid spamming.

---

## Entity Enhancement

### Perpetual Day Mode

| Entity Type | Health | Speed | Attack |
|---|---|---|---|
| Regular hostile | ×10 | ×1.5 | ×1.5 |
| Boss (Ender Dragon / Wither) | ×2.5 | ×1.5 | ×0.7 |
| Undead | Not processed (burns in sunlight) | — | — |

> Health is scaled by a multiplier relative to the base value (vanilla 20-HP zombie → 200). Older versions used a fixed 200, which actually weakened high-HP mobs such as the Warden; it is now a true multiplier.
> Attribute modifiers use `MULTIPLY_TOTAL`, so ratios such as ×1.5 / ×0.7 hold exactly for any entity.

### Perpetual Night Mode

| Entity Type | Health | Speed | Attack |
|---|---|---|---|
| Regular hostile | ×50 | ×0.7 | ×5.0 |
| Boss (Ender Dragon / Wither) | ×10 | ×0.7 | ×3.0 |

> Health is clamped at 1024, so some entities cannot reach the nominal value at high multipliers.

Neutral hostile mobs (Enderman, Spider, Zombie Piglin, etc.) actively track players in Perpetual Day / Night modes (via injected `ActiveTargetGoal`).

---

## Game Rules

The mod registers a set of **per-world** game rules, changeable at runtime with vanilla
`/gamerule` (OP required). They also appear in the "Create World → Game Rules" screen.

| Game Rule | Default | Description |
|---|---|---|
| `pdopnTemperature` | `true` | Master switch for the temperature system |
| `pdopnThirst` | `true` | Master switch for the thirst system |
| `pdopnDrift` | `true` | Drift accumulation. When off, **the day/night time lock still works** — drift simply stops accumulating |
| `pdopnMaxDrift` | `100` | Absolute drift cap (°C). `0` = unlimited; default ±100 matches the lethal threshold, capping around day 100 |
| `pdopnBlockTemp` | `true` | Temperature influence of nearby dangerous blocks (lava / fire / ice, etc.) |
| `pdopnLethalDamage` | `true` | Lethal temperature and dehydration damage. When off you still overheat / freeze / dehydrate, but do not die |
| `pdopnMobBoost` | `true` | Hostile mob attribute enhancement |
| `pdopnNeutralAggro` | `true` | Neutral mobs (Enderman / Spider, etc.) actively track players |
| `pdopnHudDefault` | `true` | Default HUD state for new players (players who turned it off keep their choice) |
| `pdopnMaxDaysEnforce` | `false` | Upgrade `maxDays` from "warning only" to enforced: auto-revert to Normal cycle on expiry |

### Why game rules instead of config options

`pdopn.json` lives in `.minecraft/config/` and applies to **every world** on the server,
while mode and drift are **per-world** state and temperature is **per-player** state.
Game rules come with `/gamerule`, OP permission checks, world-save persistence and
client sync out of the box — none of which needs reimplementing.

Division of labour: **the config supplies global defaults, game rules supply per-world overrides.**
Rules are strictly "switch / cap" semantics; concrete numbers stay in the config so the two
sources never fight each other.

> Typical use: `/gamerule pdopnDrift false` — keep the perpetual-day scenery without the
> ever-rising temperature pressure.
> Or `/gamerule pdopnMaxDrift 60` — cap drift at ±60°C so it stays extreme but survivable.

---

## Configuration

Config file is located at `.minecraft/config/pdopn/pdopn.json`;
apply changes with `/pdopn reload` or by restarting the server.
All concrete temperature / thirst numbers live here.

### Temperature Config

| Option | Default | Description |
|---|---|---|
| `baseEnvRate` | `0.005` | Rate at which body temperature approaches environmental temperature |
| `dailyDriftAmount` | `1.0` | Drift value accumulated per day in Perpetual Day/Night modes (°C) |
| `driftDecayRate` | `0.02` | Drift decay per tick in Normal mode |
| `normalSafeRange` | `60.0` | Safe range for pure environmental temperature in Normal mode (±°C) |
| `netherBaseTemp` | `40.0` | Nether base temperature |
| `endBaseTemp` | `-20.0` | End base temperature |
| `rainModifier` | `-3.0` | Rain temperature modifier |
| `thunderModifier` | `-5.0` | Thunderstorm temperature modifier |

### Thirst Config

| Option | Default | Description |
|---|---|---|
| `baseDrainRate` | `0.005` | Base thirst drain per tick |
| `maxValue` | `100.0` | Max hydration value |
| `initialValue` | `100.0` | Initial hydration value |
| `pureWaterBottleRestore` | `15.0` | Pure water bottle restore amount |
| `pureWaterBucketRestore` | `25.0` | Pure water bucket restore amount |
| `freshwaterDrinkRestore` | `15.0` | Freshwater direct drinking restore amount |
| `seawaterDrinkDrain` | `-15.0` | Seawater drinking dehydration value |
| `saltLakeDrinkDrain` | `-10.0` | Salt lake drinking dehydration value |
| `unsafeDrinkChance` | `0.75` | Dehydration probability for non-freshwater sources |
| `drinkCooldownTicks` | `40` | Direct drinking cooldown (ticks) |
| `saltLakeChance` | `0.25` | Salt lake generation probability |
| `pureWaterBottleCooling` | `6.0` | Body-temperature cooling per pure water bottle (°C) |
| `pureWaterBucketCooling` | `20.0` | Body-temperature cooling per pure water bucket (°C) |
| `freshwaterDrinkCooling` | `3.0` | Cooling from drinking freshwater directly (°C) |
| `unsafeDrinkCooling` | `2.0` | Cooling from drinking unsafe water (°C) |
| `coolantDurationTicks` | `200` | Duration of the cooling effect (ticks; 200 = 10s) |

### Entity Enhancement Options

| Option | Default | Description |
|---|---|---|
| `entity.whitelist` | `[]` (empty) | Entity IDs allowed to be enhanced. Empty means "everything except the blacklist" |
| `entity.blacklist` | `[]` (empty) | Entity IDs never enhanced; takes priority over the whitelist |

> Purpose: avoid conflicts with mods that also rewrite mob attributes (elite mobs, epic fight, etc.).
> For example `"blacklist": ["modid:elite_zombie"]`, or
> `"whitelist": ["minecraft:zombie", "minecraft:skeleton"]`.

### Config Version

| Option | Default | Description |
|---|---|---|
| `configVersion` | `3` | Config schema version, used for automatic migration |

> Use `/pdopn reload` to apply config changes without restarting the server.

If the config file is missing or corrupt, a complete default config is **regenerated automatically**.

When the file is missing some entries (typically new fields added by a mod update), the mod **merges** them in:
new entries are written with their default values, while **any value you changed is kept and never overwritten**.
The added entries are printed to the log (`已补齐缺失配置项: [...]` / "missing config entries added"),
so you can see exactly what an update introduced.

> Implementation note: the merge compares against the **file's actual JSON content** key by key,
> rather than re-serializing the config object — the latter would overwrite your values and drop
> keys that have no matching class field. `configVersion` is mod-managed metadata and is upgraded automatically.

---

## Installation

1. Install Minecraft 1.20.1
2. Install [Fabric Loader](https://fabricmc.net/) 0.19.3 or later
3. Download [Fabric API](https://modrinth.com/mod/fabric-api) 0.92.11+1.20.1
4. Download this mod's jar file
5. Place both Fabric API and this mod's jar into the `.minecraft/mods/` directory
6. Launch the game

---

## Build & Development

### Project Structure

```
Perpetual day or perpetual night/
├── src/main/java/yifei/pdopn/
│   ├── PerpetualDayOrPerpetualNight.java   # Main entry
│   ├── client/                             # Client entry
│   ├── command/                            # Command system
│   ├── config/                             # Config management
│   ├── entity/                             # Entity attribute modification
│   ├── hud/                                # HUD rendering
│   ├── items/                              # Pure water bottle / bucket
│   ├── mixin/                              # Mixin injections
│   ├── mode/                               # Mode enum
│   ├── temperature/                        # Temperature system
│   └── thirst/                             # Thirst system
├── src/main/resources/
│   ├── assets/pdopn/                      # Resources (lang/models/textures)
│   └── data/pdopn/                         # Data files (recipes)
├── gradle.properties
└── README.md
```

### Build Command

```bash
./gradlew build            # Output → build/libs/pdopn-1.0.0 Fabric.jar
```

The build also auto-generates a `-sources.jar` source package.

### Tech Stack

- **Loader**: Fabric Loader 0.19.3
- **API**: Fabric API 0.92.11+1.20.1
- **Mappings**: Yarn 1.20.1+build.10
- **Build Tool**: Fabric Loom 1.17-SNAPSHOT
- **Java**: 17

---

## License

This project is licensed under the **MIT License**, see [LICENSE](https://github.com/19136644525lxy/Perpetual-Day-or-Perpetual-Night/blob/main/LICENSE).
