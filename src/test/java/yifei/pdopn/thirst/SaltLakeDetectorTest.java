package yifei.pdopn.thirst;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 水体分类逻辑测试。
 *
 * <p>分类顺序（海洋优先 → 候选群系 → 咸水湖哈希）此前藏在
 * {@code PdopnThirstManager.detectWaterType} 的多层分支里，
 * 现提取为 {@link SaltLakeDetector#classify} 以便直接验证。
 */
class SaltLakeDetectorTest {

    @Test
    @DisplayName("海洋类群系一律判定为 OCEAN（优先于其他候选）")
    void oceanTakesPriority() {
        assertEquals(WorldWaterType.OCEAN,
            SaltLakeDetector.classify("minecraft:ocean", false));
        assertEquals(WorldWaterType.OCEAN,
            SaltLakeDetector.classify("minecraft:frozen_ocean", false));
        assertEquals(WorldWaterType.OCEAN,
            SaltLakeDetector.classify("minecraft:deep_cold_ocean", false));
        // 即使咸水湖哈希命中，海洋仍优先
        assertEquals(WorldWaterType.OCEAN,
            SaltLakeDetector.classify("minecraft:ocean", true));
    }

    @Test
    @DisplayName("候选群系且未命中哈希 → 淡水湖")
    void candidateWithoutHashIsFreshwater() {
        assertEquals(WorldWaterType.FRESHWATER_LAKE,
            SaltLakeDetector.classify("minecraft:river", false));
        assertEquals(WorldWaterType.FRESHWATER_LAKE,
            SaltLakeDetector.classify("minecraft:swamp", false));
        assertEquals(WorldWaterType.FRESHWATER_LAKE,
            SaltLakeDetector.classify("minecraft:mangrove_swamp", false));
        assertEquals(WorldWaterType.FRESHWATER_LAKE,
            SaltLakeDetector.classify("minecraft:beach", false));
        assertEquals(WorldWaterType.FRESHWATER_LAKE,
            SaltLakeDetector.classify("minecraft:frozen_river", false));
    }

    @Test
    @DisplayName("候选群系且命中哈希 → 咸水湖")
    void candidateWithHashIsSaltLake() {
        assertEquals(WorldWaterType.SALT_LAKE,
            SaltLakeDetector.classify("minecraft:river", true));
        assertEquals(WorldWaterType.SALT_LAKE,
            SaltLakeDetector.classify("minecraft:swamp", true));
    }

    @Test
    @DisplayName("非候选群系 → 普通水（无论哈希结果）")
    void nonCandidateIsNormalWater() {
        assertEquals(WorldWaterType.NORMAL_WATER,
            SaltLakeDetector.classify("minecraft:plains", false));
        assertEquals(WorldWaterType.NORMAL_WATER,
            SaltLakeDetector.classify("minecraft:desert", true));
        assertEquals(WorldWaterType.NORMAL_WATER,
            SaltLakeDetector.classify("minecraft:the_nether", false));
    }

    @Test
    @DisplayName("null 群系 ID 安全降级为普通水")
    void nullBiomeIsNormalWater() {
        assertEquals(WorldWaterType.NORMAL_WATER, SaltLakeDetector.classify(null, true));
    }

    @Test
    @DisplayName("只有淡水湖是安全水源")
    void onlyFreshwaterIsSafe() {
        assertFalse(WorldWaterType.FRESHWATER_LAKE.isUnsafe());
        assertTrue(WorldWaterType.OCEAN.isUnsafe());
        assertTrue(WorldWaterType.SALT_LAKE.isUnsafe());
        assertTrue(WorldWaterType.NORMAL_WATER.isUnsafe());
    }
}
