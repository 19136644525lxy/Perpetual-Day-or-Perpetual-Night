package yifei.pdopn.thirst;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import yifei.pdopn.config.PdopnConfig;

/**
 * 咸水湖检测器。
 * 职责：基于 chunk 坐标哈希 + 候选群系白名单，确定性判定当前位置是否为咸水湖。
 * 设计：无状态工具类，结果稳定可复现，无需持久化。
 */
public final class SaltLakeDetector {

    /** 候选群系关键字（沼泽/红树林/沙滩/河流/冰河） */
    private static final String[] CANDIDATE_KEYWORDS = {
        "swamp", "mangrove",
        "beach", "shore",
        "river", "frozen_river"
    };

    private SaltLakeDetector() {}

    /** 判定当前位置是否为咸水湖 */
    public static boolean isSaltLake(BlockPos pos, RegistryKey<Biome> biomeKey) {
        if (biomeKey == null) return false;

        // 1. 群系白名单过滤
        String biomeId = biomeKey.getValue().toString();
        if (!isCandidateBiome(biomeId)) return false;

        // 2. 配置的异变概率
        double chance = PdopnConfig.getInstance().thirst.saltLakeChance;
        if (chance <= 0.0) return false;
        if (chance >= 1.0) return true;

        // 3. 基于 chunk 坐标的确定性哈希（结果稳定可复现）
        return chunkHashToChance(pos) < chance;
    }

    /** 判断群系是否属于淡水湖候选群系（沼泽/沙滩/河流等） */
    public static boolean isCandidateBiome(RegistryKey<Biome> biomeKey) {
        if (biomeKey == null) return false;
        return isCandidateBiome(biomeKey.getValue().toString());
    }

    /** 判断群系 ID 是否属于咸水湖候选 */
    private static boolean isCandidateBiome(String biomeId) {
        for (String keyword : CANDIDATE_KEYWORDS) {
            if (biomeId.contains(keyword)) return true;
        }
        return false;
    }

    /** 判定当前位置是否为淡水湖（候选群系 + 非咸水湖） */
    public static boolean isFreshwaterLake(BlockPos pos, RegistryKey<Biome> biomeKey) {
        return isCandidateBiome(biomeKey) && !isSaltLake(pos, biomeKey);
    }

    /**
     * 将 chunk 坐标哈希到 [0, 1) 区间。
     * 使用 chunk 坐标（而非 block 坐标）保证整个 chunk 判定一致。
     */
    private static double chunkHashToChance(BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        long hash = chunkX * 341873128712L ^ chunkZ * 132897987541L;
        // 取绝对值并模 10000，归一化到 [0, 1)
        long abs = Math.abs(hash) % 10000L;
        return abs / 10000.0;
    }
}
