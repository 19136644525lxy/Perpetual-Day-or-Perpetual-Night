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

### Drinking Cooldown

Direct drinking (empty-handed right-click on water) has a 40-tick (2 seconds) cooldown to prevent rapid spamming.

---

## Entity Enhancement

### Perpetual Day Mode

| Entity Type | Health | Speed | Attack |
|---|---|---|---|
| Regular hostile | 200 | ×1.5 | ×0.5 |
| Boss (Ender Dragon / Wither) | ×2.5 | ×1.5 | ×0.7 |
| Undead | Not processed (burns in sunlight) | — | — |

### Perpetual Night Mode

| Entity Type | Health | Speed | Attack |
|---|---|---|---|
| Regular hostile | 1000 | ×0.7 | ×5.0 |
| Boss (Ender Dragon / Wither) | ×10 | ×0.7 | ×3.0 |

Neutral hostile mobs (Enderman, Spider, Zombie Piglin, etc.) actively track players in Perpetual Day / Night modes (via injected `ActiveTargetGoal`).

---

## Configuration

Config file is located at `.minecraft/config/pdopn/pdopn.json`; restart the game after modification.

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
