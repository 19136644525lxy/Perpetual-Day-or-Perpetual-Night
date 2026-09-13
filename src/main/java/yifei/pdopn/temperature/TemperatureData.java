package yifei.pdopn.temperature;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.registry.RegistryKey;

import java.util.HashMap;
import java.util.Map;

/**
 * 温度系统所有静态数据常量。
 * 包含群系、方块、物品、装备的温度值映射。
 */
public final class TemperatureData {

    private TemperatureData() {}

    /* ══════════ 体温常量 ══════════ */

    /* ══════════ 体温常量 ══════════
     * 纯数值逻辑（档位 / 偏移 / 时间 / 海拔）见 {@link TemperatureBands}，
     * 那里不依赖 Minecraft 注册表，可直接单元测试。
     */

    public static final double MIN_TEMP = TemperatureBands.MIN_TEMP;
    public static final double MAX_TEMP = TemperatureBands.MAX_TEMP;
    public static final double DEFAULT_BODY_TEMP = TemperatureBands.DEFAULT_BODY_TEMP;

    /* ══════════ 维度基础温度 ══════════ */

    public static final double NETHER_BASE_TEMP = 40.0;
    public static final double END_BASE_TEMP = -20.0;

    /* ══════════ 群系基础温度 ══════════ */

    private static final Map<RegistryKey<Biome>, Double> BIOME_TEMPS = new HashMap<>();

    static {
        // ── 炎热 (+20 ~ +45) ──
        BIOME_TEMPS.put(BiomeKeys.DESERT, 45.0);
        BIOME_TEMPS.put(BiomeKeys.BADLANDS, 42.0);
        BIOME_TEMPS.put(BiomeKeys.ERODED_BADLANDS, 42.0);
        BIOME_TEMPS.put(BiomeKeys.WOODED_BADLANDS, 38.0);
        BIOME_TEMPS.put(BiomeKeys.SAVANNA, 35.0);
        BIOME_TEMPS.put(BiomeKeys.SAVANNA_PLATEAU, 35.0);
        BIOME_TEMPS.put(BiomeKeys.WINDSWEPT_SAVANNA, 32.0);
        BIOME_TEMPS.put(BiomeKeys.JUNGLE, 30.0);
        BIOME_TEMPS.put(BiomeKeys.BAMBOO_JUNGLE, 30.0);
        BIOME_TEMPS.put(BiomeKeys.SPARSE_JUNGLE, 28.0);
        BIOME_TEMPS.put(BiomeKeys.MANGROVE_SWAMP, 25.0);

        // ── 温暖 (+10 ~ +20) ──
        BIOME_TEMPS.put(BiomeKeys.SWAMP, 18.0);
        BIOME_TEMPS.put(BiomeKeys.WARM_OCEAN, 20.0);
        BIOME_TEMPS.put(BiomeKeys.LUKEWARM_OCEAN, 15.0);
        BIOME_TEMPS.put(BiomeKeys.DEEP_LUKEWARM_OCEAN, 12.0);
        BIOME_TEMPS.put(BiomeKeys.MUSHROOM_FIELDS, 15.0);
        BIOME_TEMPS.put(BiomeKeys.FLOWER_FOREST, 12.0);

        // ── 温和 (-5 ~ +10) ──
        BIOME_TEMPS.put(BiomeKeys.PLAINS, 8.0);
        BIOME_TEMPS.put(BiomeKeys.SUNFLOWER_PLAINS, 8.0);
        BIOME_TEMPS.put(BiomeKeys.FOREST, 6.0);
        BIOME_TEMPS.put(BiomeKeys.BIRCH_FOREST, 6.0);
        BIOME_TEMPS.put(BiomeKeys.OLD_GROWTH_BIRCH_FOREST, 6.0);
        BIOME_TEMPS.put(BiomeKeys.DARK_FOREST, 5.0);
        BIOME_TEMPS.put(BiomeKeys.MEADOW, 5.0);
        BIOME_TEMPS.put(BiomeKeys.CHERRY_GROVE, 8.0);
        BIOME_TEMPS.put(BiomeKeys.RIVER, 5.0);
        BIOME_TEMPS.put(BiomeKeys.OCEAN, 5.0);
        BIOME_TEMPS.put(BiomeKeys.DEEP_OCEAN, 3.0);
        BIOME_TEMPS.put(BiomeKeys.BEACH, 10.0);
        BIOME_TEMPS.put(BiomeKeys.WINDSWEPT_HILLS, 0.0);
        BIOME_TEMPS.put(BiomeKeys.WINDSWEPT_GRAVELLY_HILLS, 0.0);
        BIOME_TEMPS.put(BiomeKeys.WINDSWEPT_FOREST, 2.0);
        BIOME_TEMPS.put(BiomeKeys.THE_VOID, 0.0);

        // ── 寒冷 (-20 ~ -5) ──
        BIOME_TEMPS.put(BiomeKeys.TAIGA, -5.0);
        BIOME_TEMPS.put(BiomeKeys.OLD_GROWTH_PINE_TAIGA, -5.0);
        BIOME_TEMPS.put(BiomeKeys.OLD_GROWTH_SPRUCE_TAIGA, -5.0);
        BIOME_TEMPS.put(BiomeKeys.COLD_OCEAN, -10.0);
        BIOME_TEMPS.put(BiomeKeys.DEEP_COLD_OCEAN, -12.0);
        BIOME_TEMPS.put(BiomeKeys.STONY_SHORE, -8.0);
        BIOME_TEMPS.put(BiomeKeys.GROVE, -15.0);
        BIOME_TEMPS.put(BiomeKeys.SNOWY_SLOPES, -18.0);
        BIOME_TEMPS.put(BiomeKeys.ICE_SPIKES, -25.0);

        // ── 极寒 (-40 ~ -20) ──
        BIOME_TEMPS.put(BiomeKeys.SNOWY_PLAINS, -25.0);
        BIOME_TEMPS.put(BiomeKeys.FROZEN_RIVER, -25.0);
        BIOME_TEMPS.put(BiomeKeys.SNOWY_TAIGA, -25.0);
        BIOME_TEMPS.put(BiomeKeys.SNOWY_BEACH, -10.0);
        BIOME_TEMPS.put(BiomeKeys.FROZEN_OCEAN, -30.0);
        BIOME_TEMPS.put(BiomeKeys.DEEP_FROZEN_OCEAN, -35.0);
        BIOME_TEMPS.put(BiomeKeys.FROZEN_PEAKS, -40.0);
        BIOME_TEMPS.put(BiomeKeys.JAGGED_PEAKS, -35.0);
        BIOME_TEMPS.put(BiomeKeys.STONY_PEAKS, -10.0);

        // ── 洞穴 ──
        BIOME_TEMPS.put(BiomeKeys.DRIPSTONE_CAVES, 5.0);
        BIOME_TEMPS.put(BiomeKeys.LUSH_CAVES, 12.0);
        BIOME_TEMPS.put(BiomeKeys.DEEP_DARK, -15.0);

        // ── 下界 ──
        BIOME_TEMPS.put(BiomeKeys.NETHER_WASTES, 42.0);
        BIOME_TEMPS.put(BiomeKeys.CRIMSON_FOREST, 38.0);
        BIOME_TEMPS.put(BiomeKeys.WARPED_FOREST, 25.0);
        BIOME_TEMPS.put(BiomeKeys.SOUL_SAND_VALLEY, 50.0);
        BIOME_TEMPS.put(BiomeKeys.BASALT_DELTAS, 55.0);

        // ── 末地 ──
        BIOME_TEMPS.put(BiomeKeys.THE_END, -20.0);
        BIOME_TEMPS.put(BiomeKeys.END_HIGHLANDS, -30.0);
        BIOME_TEMPS.put(BiomeKeys.END_MIDLANDS, -25.0);
        BIOME_TEMPS.put(BiomeKeys.SMALL_END_ISLANDS, -25.0);
        BIOME_TEMPS.put(BiomeKeys.END_BARRENS, -22.0);
    }

    /** 获取群系基础温度，未注册群系按 Minecraft 原始温度值线性映射 */
    public static double getBiomeTemp(RegistryKey<Biome> biomeKey, Biome biome) {
        Double val = BIOME_TEMPS.get(biomeKey);
        if (val != null) return val;
        // 模组群系：将原版 0.0~2.0 映射到 -20~+50
        return -20.0 + biome.getTemperature() * 35.0;
    }

    /* ══════════ 环境方块温度（5×5×5 范围） ══════════ */

    private static final Map<Block, Double> BLOCK_TEMPS = new HashMap<>();
    private static final Map<Block, Integer> BLOCK_RADIUS = new HashMap<>();

    static {
        regBlock(Blocks.LAVA, 15.0, 5);
        regBlock(Blocks.MAGMA_BLOCK, 8.0, 3);
        regBlock(Blocks.FIRE, 10.0, 3);
        regBlock(Blocks.CAMPFIRE, 5.0, 3);
        regBlock(Blocks.FURNACE, 6.0, 3);
        regBlock(Blocks.TORCH, 1.0, 2);
        regBlock(Blocks.WALL_TORCH, 1.0, 2);
        regBlock(Blocks.LANTERN, 1.5, 2);
        regBlock(Blocks.GLOWSTONE, 4.0, 3);
        regBlock(Blocks.JACK_O_LANTERN, 2.0, 2);
        regBlock(Blocks.NETHER_PORTAL, 3.0, 3);
        regBlock(Blocks.SOUL_CAMPFIRE, -3.0, 3);
        regBlock(Blocks.SOUL_TORCH, -1.0, 2);
        regBlock(Blocks.SOUL_WALL_TORCH, -1.0, 2);
        regBlock(Blocks.SOUL_LANTERN, -1.5, 2);
        regBlock(Blocks.SOUL_FIRE, -4.0, 2);
        regBlock(Blocks.ICE, -5.0, 3);
        regBlock(Blocks.PACKED_ICE, -8.0, 3);
        regBlock(Blocks.BLUE_ICE, -12.0, 3);
        regBlock(Blocks.SNOW, -2.0, 2);
        regBlock(Blocks.SNOW_BLOCK, -3.0, 2);
        regBlock(Blocks.POWDER_SNOW, -15.0, 3);
    }

    private static void regBlock(Block block, double temp, int radius) {
        BLOCK_TEMPS.put(block, temp);
        BLOCK_RADIUS.put(block, radius);
    }

    public static double getBlockTemp(Block block) {
        return BLOCK_TEMPS.getOrDefault(block, 0.0);
    }

    public static int getBlockRadius(Block block) {
        return BLOCK_RADIUS.getOrDefault(block, 0);
    }

    public static boolean hasBlockTemp(Block block) {
        return BLOCK_TEMPS.containsKey(block);
    }

    /* ══════════ 水体温度 ══════════
     * 作为「体温趋向目标」而非纯环境温度的一部分：
     * 泡在水里时体温会被拉向水温，因此冷水比热空气更能有效降温。
     * 此前水体对体温毫无影响（只有口渴系统关心水），
     * 导致永昼 + 沙漠等高温场景完全没有降温手段。
     */

    private static final Map<RegistryKey<Biome>, Double> WATER_TEMPS = new HashMap<>();

    /** 默认水温（普通河流 / 湖泊） */
    public static final double DEFAULT_WATER_TEMP = 12.0;

    static {
        // 冰水
        WATER_TEMPS.put(BiomeKeys.FROZEN_OCEAN, -25.0);
        WATER_TEMPS.put(BiomeKeys.DEEP_FROZEN_OCEAN, -25.0);
        WATER_TEMPS.put(BiomeKeys.FROZEN_RIVER, -20.0);
        // 冷水
        WATER_TEMPS.put(BiomeKeys.COLD_OCEAN, 2.0);
        WATER_TEMPS.put(BiomeKeys.DEEP_COLD_OCEAN, 0.0);
        // 常温
        WATER_TEMPS.put(BiomeKeys.OCEAN, 10.0);
        WATER_TEMPS.put(BiomeKeys.DEEP_OCEAN, 6.0);
        WATER_TEMPS.put(BiomeKeys.RIVER, 12.0);
        WATER_TEMPS.put(BiomeKeys.BEACH, 13.0);
        WATER_TEMPS.put(BiomeKeys.SNOWY_BEACH, 0.0);
        WATER_TEMPS.put(BiomeKeys.STONY_SHORE, 13.0);
        // 温水
        WATER_TEMPS.put(BiomeKeys.LUKEWARM_OCEAN, 18.0);
        WATER_TEMPS.put(BiomeKeys.DEEP_LUKEWARM_OCEAN, 17.0);
        WATER_TEMPS.put(BiomeKeys.WARM_OCEAN, 22.0);
        WATER_TEMPS.put(BiomeKeys.SWAMP, 16.0);
        WATER_TEMPS.put(BiomeKeys.MANGROVE_SWAMP, 17.0);
        WATER_TEMPS.put(BiomeKeys.JUNGLE, 20.0);
        WATER_TEMPS.put(BiomeKeys.BAMBOO_JUNGLE, 20.0);
        WATER_TEMPS.put(BiomeKeys.SPARSE_JUNGLE, 19.0);
    }

    /** 获取群系对应水温；未知群系按「温和水域」处理 */
    public static double getWaterTemp(RegistryKey<Biome> biomeKey) {
        if (biomeKey == null) return DEFAULT_WATER_TEMP;
        return WATER_TEMPS.getOrDefault(biomeKey, DEFAULT_WATER_TEMP);
    }

    /* ══════════ 手持物品温度 ══════════ */

    private static final Map<Item, Double> ITEM_TEMPS = new HashMap<>();

    static {
        // ── 降温物品 ──
        ITEM_TEMPS.put(Items.ICE, -0.005);
        ITEM_TEMPS.put(Items.PACKED_ICE, -0.008);
        ITEM_TEMPS.put(Items.BLUE_ICE, -0.012);
        ITEM_TEMPS.put(Items.SNOWBALL, -0.002);
        ITEM_TEMPS.put(Items.SNOW_BLOCK, -0.003);
        ITEM_TEMPS.put(Items.POWDER_SNOW_BUCKET, -0.015);
        ITEM_TEMPS.put(Items.WATER_BUCKET, -0.004);
        ITEM_TEMPS.put(Items.TRIDENT, -0.002);
        ITEM_TEMPS.put(Items.COD, -0.001);
        ITEM_TEMPS.put(Items.SALMON, -0.001);
        ITEM_TEMPS.put(Items.TROPICAL_FISH, -0.001);
        ITEM_TEMPS.put(Items.PUFFERFISH, -0.001);
        ITEM_TEMPS.put(Items.KELP, -0.001);
        ITEM_TEMPS.put(Items.SEA_PICKLE, -0.001);
        ITEM_TEMPS.put(Items.NAUTILUS_SHELL, -0.003);
        ITEM_TEMPS.put(Items.MELON_SLICE, -0.002);

        // ── 升温物品 ──
        ITEM_TEMPS.put(Items.LAVA_BUCKET, 0.03);
        ITEM_TEMPS.put(Items.BLAZE_ROD, 0.01);
        ITEM_TEMPS.put(Items.BLAZE_POWDER, 0.008);
        ITEM_TEMPS.put(Items.MAGMA_CREAM, 0.006);
        ITEM_TEMPS.put(Items.MAGMA_BLOCK, 0.008);
        ITEM_TEMPS.put(Items.GLOWSTONE, 0.004);
        ITEM_TEMPS.put(Items.GLOWSTONE_DUST, 0.002);
        ITEM_TEMPS.put(Items.COAL, 0.003);
        ITEM_TEMPS.put(Items.CHARCOAL, 0.003);
        ITEM_TEMPS.put(Items.COAL_BLOCK, 0.005);
        ITEM_TEMPS.put(Items.SOUL_SAND, 0.002);
        ITEM_TEMPS.put(Items.SOUL_SOIL, 0.002);
        ITEM_TEMPS.put(Items.NETHER_BRICK, 0.003);
        ITEM_TEMPS.put(Items.QUARTZ, 0.002);
        ITEM_TEMPS.put(Items.TORCH, 0.002);
        ITEM_TEMPS.put(Items.LANTERN, 0.003);
        ITEM_TEMPS.put(Items.COOKED_BEEF, 0.001);
        ITEM_TEMPS.put(Items.COOKED_PORKCHOP, 0.001);
        ITEM_TEMPS.put(Items.COOKED_CHICKEN, 0.001);
        ITEM_TEMPS.put(Items.BREAD, 0.001);
        ITEM_TEMPS.put(Items.PUMPKIN_PIE, 0.001);
    }

    public static double getItemTemp(Item item) {
        return ITEM_TEMPS.getOrDefault(item, 0.0);
    }

    /* ══════════ 装备隔热系数 ══════════ */

    private static final Map<Item, Double> ARMOR_INSULATION = new HashMap<>();

    static {
        // 皮革
        ARMOR_INSULATION.put(Items.LEATHER_HELMET, 0.15);
        ARMOR_INSULATION.put(Items.LEATHER_CHESTPLATE, 0.20);
        ARMOR_INSULATION.put(Items.LEATHER_LEGGINGS, 0.15);
        ARMOR_INSULATION.put(Items.LEATHER_BOOTS, 0.10);
        ARMOR_INSULATION.put(Items.LEATHER_HORSE_ARMOR, 0.10);
        // 锁链
        ARMOR_INSULATION.put(Items.CHAINMAIL_HELMET, 0.07);
        ARMOR_INSULATION.put(Items.CHAINMAIL_CHESTPLATE, 0.10);
        ARMOR_INSULATION.put(Items.CHAINMAIL_LEGGINGS, 0.07);
        ARMOR_INSULATION.put(Items.CHAINMAIL_BOOTS, 0.06);
        // 铁
        ARMOR_INSULATION.put(Items.IRON_HELMET, 0.06);
        ARMOR_INSULATION.put(Items.IRON_CHESTPLATE, 0.08);
        ARMOR_INSULATION.put(Items.IRON_LEGGINGS, 0.06);
        ARMOR_INSULATION.put(Items.IRON_BOOTS, 0.05);
        // 金
        ARMOR_INSULATION.put(Items.GOLDEN_HELMET, 0.08);
        ARMOR_INSULATION.put(Items.GOLDEN_CHESTPLATE, 0.12);
        ARMOR_INSULATION.put(Items.GOLDEN_LEGGINGS, 0.08);
        ARMOR_INSULATION.put(Items.GOLDEN_BOOTS, 0.07);
        // 钻石
        ARMOR_INSULATION.put(Items.DIAMOND_HELMET, 0.10);
        ARMOR_INSULATION.put(Items.DIAMOND_CHESTPLATE, 0.12);
        ARMOR_INSULATION.put(Items.DIAMOND_LEGGINGS, 0.10);
        ARMOR_INSULATION.put(Items.DIAMOND_BOOTS, 0.08);
        // 下界合金
        ARMOR_INSULATION.put(Items.NETHERITE_HELMET, 0.13);
        ARMOR_INSULATION.put(Items.NETHERITE_CHESTPLATE, 0.18);
        ARMOR_INSULATION.put(Items.NETHERITE_LEGGINGS, 0.13);
        ARMOR_INSULATION.put(Items.NETHERITE_BOOTS, 0.11);
        // 特殊
        ARMOR_INSULATION.put(Items.TURTLE_HELMET, 0.08);
    }

    public static double getArmorInsulation(Item item) {
        return ARMOR_INSULATION.getOrDefault(item, 0.0);
    }

    /* ══════════ 时间修正 ══════════ */

    /* ══════════ 时间修正 / 偏移 / 海拔 ══════════
     * 已迁移至 {@link TemperatureBands}（纯函数、可单元测试）。
     * 仅保留以下转发方法，兼容既有调用点。
     */

    /** @see TemperatureBands#getTimeModifier(long) */
    public static double getTimeModifier(long worldTime) {
        return TemperatureBands.getTimeModifier(worldTime);
    }

    /** @see TemperatureBands#getAltitudeModifier(int) */
    public static double getAltitudeModifier(int y) {
        return TemperatureBands.getAltitudeModifier(y);
    }

    /* ══════════ 天气修正 ══════════
     * 实际数值来自 PdopnConfig.temperature 的 rainModifier / thunderModifier，
     * 此处不再重复定义常量（旧常量无人引用且会与配置产生两套数值）。
     */
}
