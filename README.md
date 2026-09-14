# 永昼永夜 / Perpetual Day or Perpetual Night

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://github.com/19136644525lxy/Perpetual-Day-or-Perpetual-Night/blob/main/LICENSE)
[![GitHub](https://img.shields.io/badge/GitHub-源码仓库-blue)](https://github.com/19136644525lxy/Perpetual-Day-or-Perpetual-Night)
[![Platform](https://img.shields.io/badge/平台-Fabric-darkgreen)](#平台支持)
[![Version](https://img.shields.io/badge/Minecraft-1.20.1-blue)](#平台支持)
[![Java](https://img.shields.io/badge/Java-17-orange)](#平台支持)

> 通过指令切换永昼 / 永夜 / 正常循环三种模式，并引入温度、口渴、净水烧炼、实体增强等硬核生存机制的 Minecraft 模组。

Jump to the English introduction: [README_en.md](https://github.com/19136644525lxy/Perpetual-Day-or-Perpetual-Night/blob/main/README_en.md)

---

## 平台支持

| 加载器 | Minecraft 版本 | 模组版本 | 状态 |
|---|---|---|---|
| **Fabric** | 1.20.1 | `1.0.0 Fabric` | ✅ 功能完整 |

> 需同时安装 **Fabric Loader 0.19.3+** 与 **Fabric API 0.92.11+1.20.1**。

---

## 功能特性

- **三模式切换**：永昼（时间锁定正午）、永夜（时间锁定午夜）、正常循环，三种模式间任意切换，无需密钥
- **温度系统**：体温受群系 / 维度 / 时间 / 天气 / 海拔 / 附近方块 / 手持物品 / 装备隔热综合影响，体温过高烧死、过低冻死
- **偏移累加机制**：永昼 / 永夜模式下温度偏移每天累加 `dailyDriftAmount`（默认 1°C），持续不衰减；永昼越久越热，永夜越久越冷；永昼↔永夜切换时偏移直接取反
- **正常模式安全保护**：纯环境温度 clamp 到 ±`normalSafeRange`（默认 ±60°C），不会致死；方块和手持物品影响不 clamp，仍可致死
- **口渴系统**：玩家拥有口渴值（默认初始 100），随时间消耗；不同水源饮水效果不同（淡水湖安全，海水 / 咸水湖 / 普通水 75% 概率脱水）
- **净水烧炼**：脏水瓶 / 脏水桶必须放入熔炉 / 高炉 / 烟熏炉烧炼后才能饮用
- **直接饮水**：空手右键水面可喝水，淡水湖 100% 安全，其他水体 75% 概率口渴加剧
- **实体增强**：永昼 / 永夜模式下敌对生物属性与 AI 增强（速度 / 攻击 / 血量提升，中立生物主动追踪玩家）
- **HUD 显示**：Actionbar 合并显示体温 + 口渴值，不污染聊天栏
- **数据持久化**：玩家温度 / 口渴数据 + 全局偏移值跨会话保存，服务器重启不丢失

---

## 模式详解

| 模式 | 时间锁定 | 温度偏移 | 实体增强 |
|---|---|---|---|
| **正常循环** (`/pdopn cycle`) | 跟随原版昼夜循环 | 无偏移（衰减回 0） | 无 |
| **永昼** (`/pdopn day`) | 正午 6000 ticks | 每天累加 +1°C，越来越热 | 亡灵燃烧，其他敌对增强 |
| **永夜** (`/pdopn night`) | 午夜 18000 ticks | 每天累加 -1°C，越来越冷 | 所有敌对生物增强 |

### 偏移转换规则

| 切换场景 | 偏移处理 |
|---|---|
| 永昼 → 永夜 | `+N°C` → `-N°C`，继续向冷方向累加 |
| 永夜 → 永昼 | `-N°C` → `+N°C`，继续向热方向累加 |
| 永昼 / 永夜 → 正常 | 不取反，按 `driftDecayRate` 衰减回 0 |
| 正常 → 永昼 / 永夜 | 从 0 开始正向 / 负向累加 |

---

## 指令系统

所有指令均需 OP 权限（默认等级 2）。

| 指令 | 说明 |
|---|---|
| `/pdopn day` | 切换到永昼模式 |
| `/pdopn night` | 切换到永夜模式 |
| `/pdopn cycle` | 恢复正常昼夜循环 |
| `/pdopn status` | 查看当前模式 |
| `/pdopn temp` | 查看自身体温 |
| `/pdopn temp set <value>` | 设置自身体温（-100 ~ 100） |
| `/pdopn temp maxdays <days>` | 设置最大生存天数（仅参考值） |
| `/pdopn temp hud` | 切换温度 HUD 显示 |
| `/pdopn thirst` | 查看自身体口渴值 |
| `/pdopn thirst set <value>` | 设置自身体口渴值（0 ~ 100） |
| `/pdopn drift` | 查看当前累计温度偏移（控制台可用） |
| `/pdopn drift reset` | 清空累计偏移与持续天数计数，无需重启服务器（控制台可用） |
| `/pdopn reload` | 重新加载 `pdopn.json` 配置（控制台可用） |

> `temp maxdays` 目前仅作为「达到该天数时提示一次」的预警阈值（HUD 与 ActionBar），
> 不会强制结束游戏或改变温度计算。

---

## 温度系统

### 温度影响因素

| 因素 | 说明 |
|---|---|
| **群系** | 沙漠 +45°C，冰刺 -25°C，下界废地 +42°C，末地 -20°C 等 |
| **维度** | 下界基础 +40°C，末地基础 -20°C（覆盖群系温度） |
| **时间** | 白天 +5°C，夜晚 -5°C，黄昏 +2°C，黎明 -2°C |
| **天气** | 下雨 -3°C，雷暴 -5°C（室外全效，室内减弱至 30%） |
| **海拔** | Y > 120 每格 -0.05°C，Y < 0 每格 -0.03°C |
| **附近方块** | 岩浆 +15（半径 5）、火 +10（半径 3）、冰 -5（半径 3）等 |
| **手持物品** | 岩浆桶 +0.03/tick、冰 -0.005/tick 等（每 20 tick 施加） |
| **装备隔热** | 皮革套 0.15~0.20，下界合金套 0.13~0.18，降低环境温度影响速率 |

### 降温手段

高温环境下有两条可用的降温途径（此前水体对体温毫无影响，永昼+沙漠无解）：

| 手段 | 效果 |
|---|---|
| **泡在水中** | 体温趋向水温。冰洋 -25°C、冻河 -20°C、冷水洋 +2°C、普通河湖 +12°C、暖水洋 +22°C、丛林 +20°C |
| **饮用净水** | 每瓶 -6°C（净水桶 -20°C），持续 10 秒；直接饮淡水 -3°C，非淡水 -2°C |

> 水温按群系判定；只有水温低于体温时才降温，泡温水不会升温。
> 装备隔热会同时减缓降温效果（穿厚衣服更难散热）。
> 体温 ≤ 0°C 时冷饮不再继续降温，避免把自己冻死。

### 温度效果

| 体温范围 | 效果 |
|---|---|
| `≥ 100°C` | 烧死（火焰伤害，每次 25% 最大生命） |
| `≥ 70°C` | 起火 + 反胃 III |
| `≥ 45°C` | 缓慢 II + 虚弱 II |
| `≥ 25°C` | 缓慢 I |
| `≤ -100°C` | 冻死（冰冻伤害，每次 25% 最大生命） |
| `≤ -70°C` | 凋零 II + 失明 |
| `≤ -45°C` | 缓慢 II + 挖掘疲劳 II |
| `≤ -25°C` | 缓慢 I |

### 温度致死伤害类型

体温触顶 / 触底的致死伤害使用模组自定义伤害类型，因此死亡消息是专属文本，
而非原版通用的「被烧死了」：

| 场景 | 伤害类型 | 死亡消息 |
|---|---|---|
| `≥ 100°C` | `pdopn:heat` | `%s 的体温失控，被高温烧尽` |
| `≤ -100°C` | `pdopn:cold` | `%s 的体温失控，被严寒冻毙` |

> 定义位于 `data/pdopn/damage_type/heat.json` 与 `cold.json`。
> 死亡消息文本键由 `"death.attack." + message_id` 决定，
> 因此文本键为 `death.attack.pdopn.heat` / `death.attack.pdopn.cold`
> （另含 `.player` 变体，用于死亡时有攻击者的场合）。

---

## 口渴系统

### 口渴阶段

| 口渴值范围 | 效果 |
|---|---|
| `≥ 60` | 舒适（无效果） |
| `40 ~ 60` | 轻度口渴（缓慢 I） |
| `25 ~ 40` | 中度脱水（缓慢 II + 虚弱 I） |
| `10 ~ 25` | 重度脱水（缓慢 III + 虚弱 II + 恶心 I） |
| `< 10` | 极度脱水（缓慢 IV + 虚弱 III + 恶心 II + 生命逐渐流失） |

### 水源饮水效果

| 水源类型 | 饮水结果 |
|---|---|
| **淡水湖**（河流 / 沼泽 / 红树林沼泽 / 海滩等群系且非咸水湖） | 100% 恢复 15 点口渴 |
| **海洋** | 75% 概率脱水 15 点，25% 概率恢复 2.5 点 |
| **咸水湖**（候选群系且哈希命中） | 75% 概率脱水 10 点，25% 概率恢复 2.5 点 |
| **普通水**（其他群系水体） | 75% 概率脱水 5 点，25% 概率恢复 2.5 点 |

### 净水烧炼配方

| 输入 | 设备 | 输出 | 时间 |
|---|---|---|---|
| 水瓶（脏水） | 熔炉 / 高炉 / 烟熏炉 | 净水瓶 | 200 / 100 / 100 tick |
| 水桶（脏水） | 熔炉 / 高炉 / 烟熏炉 | 净水桶 | 200 / 100 / 100 tick |

> 配方使用**原版配方类型**（`minecraft:smelting` / `blasting` / `smoking`），
> 输入为原版水瓶或水桶。
>
> ⚠️ **已知限制**：原版 `Ingredient` 的 JSON 只支持 `item` / `tag`，其匹配逻辑
> （`isItemEqual` → `isOf(Item)`）**不比较 NBT**，因此无法只匹配「水瓶」。
> 结果是**任意药水**（治疗 / 力量 / 抗火等）放进熔炉都会被烧成净水瓶。
> 请勿把贵重药水放进熔炉。
>
> 历史说明：曾尝试用自定义 `RecipeSerializer` + 自定义 `RecipeType` 来过滤输入，
> 但炉子按配方类型建立索引并查询（`RecipeManager#getAllOfType` 即
> `recipes.getOrDefault(type, ...)`），自定义类型会导致配方落入永远不会被查询的桶中，
> 表现为「配方存在但炉子不认」。为保证配方可用，已回退为原版配方类型。

### 饮水冷却

直接饮水（空手右键水面）有 40 tick（2 秒）冷却，防止快速连击。

---

## 实体增强

### 永昼模式

| 实体类型 | 血量 | 速度 | 攻击 |
|---|---|---|---|
| 普通敌对 | ×10 | ×1.5 | ×1.5 |
| Boss（末影龙 / 凋灵） | ×2.5 | ×1.5 | ×0.7 |
| 亡灵 | 不处理（持续日照燃烧） | — | — |

> 血量按基准值倍率缩放（原版 20 血僵尸 → 200）。旧版本对普通敌对使用固定值 200，会对监守者等高血量生物造成削弱，现已改为倍率。
> 属性修正使用 `MULTIPLY_TOTAL`（真乘法），确保 ×1.5 / ×0.7 等倍率对任何生物都精确成立。

### 永夜模式

| 实体类型 | 血量 | 速度 | 攻击 |
|---|---|---|---|
| 普通敌对 | ×50 | ×0.7 | ×5.0 |
| Boss（末影龙 / 凋灵） | ×10 | ×0.7 | ×3.0 |

> 血量钳制上限为 1024，因此高倍率下部分生物无法达到标称值。

中立敌对生物（末影人、蜘蛛、僵尸猪灵等）在永昼 / 永夜模式下会主动追踪玩家（注入 `ActiveTargetGoal`）。

---

## 游戏规则

模组注册了一组**每存档生效**的游戏规则，可用原版 `/gamerule` 命令热改（需 OP），
也会出现在「创建世界 → 游戏规则」界面中。

| 游戏规则 | 默认 | 说明 |
|---|---|---|
| `pdopnTemperature` | `true` | 温度系统总开关。关闭后不再有体温变化、效果与致死 |
| `pdopnThirst` | `true` | 口渴系统总开关。关闭后不再消耗口渴 |
| `pdopnDrift` | `true` | 偏移累加开关。关闭后**永昼/永夜的时间锁定仍然生效**，只是不再逐日累加偏移 |
| `pdopnMaxDrift` | `100` | 偏移绝对值上限（°C）。`0` = 不限；默认 ±100 与致死线一致，约第 100 天封顶 |
| `pdopnBlockTemp` | `true` | 附近危险方块（岩浆 / 火 / 冰等）的温度影响 |
| `pdopnLethalDamage` | `true` | 温度与脱水致死。关闭后仍会中暑 / 失温 / 脱水，但不会死 |
| `pdopnMobBoost` | `true` | 敌对生物属性增强 |
| `pdopnNeutralAggro` | `true` | 中立生物（末影人 / 蜘蛛等）主动追踪玩家 |
| `pdopnHudDefault` | `true` | 新玩家 HUD 的默认开关（已主动关闭过的玩家不受影响） |
| `pdopnMaxDaysEnforce` | `false` | 是否把 `maxDays` 从「仅预警」升级为强制：到期自动切回正常循环 |

### 为什么用游戏规则而不是配置文件

`pdopn.json` 位于 `.minecraft/config/`，对同一服务器的**所有存档**生效；
而模式与偏移是**每存档**的状态，温度是**每玩家**的状态，天然更适合 per-world 控制。
游戏规则自带 `/gamerule` 命令、OP 权限校验、存档持久化与客户端同步，无需自行实现。

两者分工：**配置文件提供全局默认值，游戏规则提供每存档覆盖**。
规则一律是「开关 / 上限」语义，具体数值仍由配置承担，避免两套数值来源互相打架。

> 典型用法：`/gamerule pdopnDrift false` —— 只要永昼风景，不要逐日升温的生存压力。
> 或 `/gamerule pdopnMaxDrift 60` —— 把偏移封顶在 ±60°C，之后维持极端但可生存。

---

## 配置文件

配置文件位于 `.minecraft/config/pdopn/pdopn.json`，修改后可用 `/pdopn reload` 立即生效，
也可重启服务器。温度 / 口渴的具体数值均在此处调整。

### 温度配置

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `baseEnvRate` | `0.005` | 体温趋向环境温度的速率 |
| `dailyDriftAmount` | `1.0` | 永昼 / 永夜每天累加的偏移值 (°C) |
| `driftDecayRate` | `0.02` | 正常模式下偏移每 tick 衰减值 |
| `normalSafeRange` | `60.0` | 正常模式纯环境温度安全范围 (±°C) |
| `netherBaseTemp` | `40.0` | 下界基础温度 |
| `endBaseTemp` | `-20.0` | 末地基础温度 |
| `rainModifier` | `-3.0` | 下雨温度修正 |
| `thunderModifier` | `-5.0` | 雷暴温度修正 |

### 口渴配置

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `baseDrainRate` | `0.005` | 每 tick 基础口渴消耗 |
| `maxValue` | `100.0` | 口渴满值 |
| `initialValue` | `100.0` | 初始口渴值 |
| `pureWaterBottleRestore` | `15.0` | 净水瓶恢复量 |
| `pureWaterBucketRestore` | `25.0` | 净水桶恢复量 |
| `freshwaterDrinkRestore` | `15.0` | 淡水直接饮用恢复量 |
| `seawaterDrinkDrain` | `-15.0` | 海水饮用脱水值 |
| `saltLakeDrinkDrain` | `-10.0` | 咸水湖饮用脱水值 |
| `unsafeDrinkChance` | `0.75` | 非淡水湖饮水脱水概率 |
| `drinkCooldownTicks` | `40` | 直接饮水冷却（tick） |
| `saltLakeChance` | `0.25` | 咸水湖生成概率 |
| `pureWaterBottleCooling` | `6.0` | 净水瓶降温量（°C） |
| `pureWaterBucketCooling` | `20.0` | 净水桶降温量（°C） |
| `freshwaterDrinkCooling` | `3.0` | 直接饮用淡水降温量（°C） |
| `unsafeDrinkCooling` | `2.0` | 饮用非淡水降温量（°C） |
| `coolantDurationTicks` | `200` | 降温效果持续时间（tick，200 = 10 秒） |

### 实体增强配置

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `entity.whitelist` | `[]`（空） | 允许增强的实体 ID。为空表示「除黑名单外全部允许」 |
| `entity.blacklist` | `[]`（空） | 禁止增强的实体 ID，优先级高于白名单 |

> 用途：避让同样改写生物属性的模组（精英怪物 / 史诗战斗等）。
> 例如 `"blacklist": ["modid:elite_zombie"]`，或
> `"whitelist": ["minecraft:zombie", "minecraft:skeleton"]`。

### 配置版本

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `configVersion` | `3` | 配置文件结构版本，用于旧配置自动迁移 |

> 修改配置后可用 `/pdopn reload` 立即生效，无需重启服务器。
> 若文件中出现 `configVersion` 小于当前版本，模组会自动补齐新增字段并回写文件。

---

## 安装方法

1. 安装 Minecraft 1.20.1
2. 安装 [Fabric Loader](https://fabricmc.net/) 0.19.3 或更高版本
3. 下载 [Fabric API](https://modrinth.com/mod/fabric-api) 0.92.11+1.20.1
4. 下载本模组 jar 文件
5. 将 Fabric API 和本模组 jar 放入 `.minecraft/mods/` 目录
6. 启动游戏

---

## 构建与开发

### 项目结构

```
Perpetual day or perpetual night/
├── src/main/java/yifei/pdopn/
│   ├── PerpetualDayOrPerpetualNight.java   # 主入口
│   ├── client/                             # 客户端入口
│   ├── command/                            # 指令系统
│   ├── config/                             # 配置管理
│   ├── entity/                             # 实体属性修改
│   ├── hud/                                # HUD 渲染
│   ├── items/                              # 净水瓶 / 净水桶
│   ├── mixin/                              # Mixin 注入
│   ├── mode/                               # 模式枚举
│   ├── recipe/                             # 净水自定义配方类型
│   ├── storage/                            # 玩家数据持久化
│   ├── temperature/                        # 温度系统（含 TemperatureBands 纯数值逻辑）
│   └── thirst/                             # 口渴系统
├── src/test/java/yifei/pdopn/              # 单元测试（纯数值逻辑，无需启动游戏）
├── src/main/resources/
│   ├── assets/pdopn/                      # 资源文件（lang/models/textures）
│   └── data/pdopn/                         # 数据文件（recipes/advancements）
├── gradle.properties
└── README.md
```

### 构建命令

```bash
./gradlew build            # 产物 → build/libs/pdopn-1.0.0.jar
./gradlew test             # 运行单元测试
```

构建同时自动生成 `-sources.jar` 源代码包。

> **单元测试**覆盖纯数值逻辑（体温档位、偏移累加/衰减、时间与海拔修正、水体分类），
> 全部位于 `src/test/java`，不依赖 Minecraft 运行时。
> 被测逻辑集中在 `TemperatureBands` 与 `SaltLakeDetector.classify`，刻意与游戏引擎解耦。
> 首次运行 `./gradlew test` 需要联网下载 JUnit（之后可加 `--offline`）。

### 技术栈

- **加载器**：Fabric Loader 0.19.3
- **API**：Fabric API 0.92.11+1.20.1
- **映射**：Yarn 1.20.1+build.10
- **构建工具**：Fabric Loom 1.17-SNAPSHOT
- **Java**：编译目标 17；**构建需 JDK 21+**（Loom 1.17 要求，已在 `gradle.properties` 中通过 `org.gradle.java.home` 指定）

---

## 许可证

本项目采用 **MIT License**，详见 [LICENSE](https://github.com/19136644525lxy/Perpetual-Day-or-Perpetual-Night/blob/main/LICENSE)。
