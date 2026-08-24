# 永昼永夜 / Perpetual Day or Perpetual Night

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://github.com/19136644525lxy/Perpetual-Day-or-Perpetual-Night/blob/main/LICENSE)
[![GitHub](https://img.shields.io/badge/GitHub-源码仓库-blue)](https://github.com/19136644525lxy/Perpetual-Day-or-Perpetual-Night)
[![Platform](https://img.shields.io/badge/平台-Fabric-darkgreen)](#平台支持)
[![Version](https://img.shields.io/badge/Minecraft-1.20.1-blue)](#平台支持)
[![Java](https://img.shields.io/badge/Java-17-orange)](#平台支持)

> 通过指令切换永昼 / 永夜 / 正常循环三种模式，并引入温度、口渴、净水烧炼、实体增强等硬核生存机制的 Minecraft 模组。

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

### 饮水冷却

直接饮水（空手右键水面）有 40 tick（2 秒）冷却，防止快速连击。

---

## 实体增强

### 永昼模式

| 实体类型 | 血量 | 速度 | 攻击 |
|---|---|---|---|
| 普通敌对 | 200 | ×1.5 | ×0.5 |
| Boss（末影龙 / 凋灵） | ×2.5 | ×1.5 | ×0.7 |
| 亡灵 | 不处理（持续日照燃烧） | — | — |

### 永夜模式

| 实体类型 | 血量 | 速度 | 攻击 |
|---|---|---|---|
| 普通敌对 | 1000 | ×0.7 | ×5.0 |
| Boss（末影龙 / 凋灵） | ×10 | ×0.7 | ×3.0 |

中立敌对生物（末影人、蜘蛛、僵尸猪灵等）在永昼 / 永夜模式下会主动追踪玩家（注入 `ActiveTargetGoal`）。

---

## 配置文件

配置文件位于 `.minecraft/config/pdopn/pdopn.json`，修改后重启生效。

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
│   ├── temperature/                        # 温度系统
│   └── thirst/                             # 口渴系统
├── src/main/resources/
│   ├── assets/pdopn/                      # 资源文件（lang/models/textures）
│   └── data/pdopn/                         # 数据文件（recipes）
├── gradle.properties
└── README.md
```

### 构建命令

```bash
./gradlew build            # 产物 → build/libs/pdopn-1.0.0 Fabric.jar
```

构建同时自动生成 `-sources.jar` 源代码包。

### 技术栈

- **加载器**：Fabric Loader 0.19.3
- **API**：Fabric API 0.92.11+1.20.1
- **映射**：Yarn 1.20.1+build.10
- **构建工具**：Fabric Loom 1.17-SNAPSHOT
- **Java**：17

---

## 设计原则

- **SOLID 原则**：单一职责（每个类只承担一个职责）、开闭原则（通过接口扩展功能）、依赖倒置（指令层依赖 `ModeChangeListener` 接口而非主类）
- **线程安全**：`ConcurrentHashMap` 存储玩家数据、`CopyOnWriteArrayList` 并发遍历、关键方法 `synchronized` 保证原子性
- **持久化**：玩家数据按 UUID 独立保存，全局偏移值单独存储，服务器重启不丢失
- **解耦设计**：温度 / 口渴 / 实体修改 / HUD 渲染各自独立管理器，通过接口通信

---

## 许可证

本项目采用 **MIT License**，详见 [LICENSE](https://github.com/19136644525lxy/Perpetual-Day-or-Perpetual-Night/blob/main/LICENSE)。
