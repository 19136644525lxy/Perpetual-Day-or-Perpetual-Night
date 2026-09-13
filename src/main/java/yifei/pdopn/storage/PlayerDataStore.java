package yifei.pdopn.storage;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;
import java.util.function.DoubleUnaryOperator;

/**
 * 玩家数值的存档级持久化工具。
 *
 * <p>温度与口渴此前各有一份几乎逐行重复的 {@link Properties} 读写实现
 * （各自处理 mkdirs / 读取 / 解析异常 / 回写），这里统一为一份。
 *
 * <p>文件位于世界存档的 {@code <world>/pdopn/<name>.properties}，键为玩家 UUID。
 */
public final class PlayerDataStore {

    private static final Logger LOGGER = LoggerFactory.getLogger("PDoPN-Storage");

    /** 存档内数据目录名 */
    private static final String DIR_NAME = "pdopn";

    private PlayerDataStore() {}

    /** 解析数据文件路径；服务端引用为空时返回 null */
    public static Path resolveFile(MinecraftServer server, String fileName) {
        if (server == null) return null;
        return server.getSavePath(WorldSavePath.ROOT).resolve(DIR_NAME).resolve(fileName);
    }

    /**
     * 读取单个玩家的数值。
     *
     * @param path     数据文件路径（可为 null）
     * @param playerId 玩家 UUID
     * @param fallback 无记录或读取失败时的默认值
     * @param validator 对读到的值做校验/钳制（可为 null）
     */
    public static double loadDouble(Path path, UUID playerId, double fallback,
                                    DoubleUnaryOperator validator) {
        if (path == null) return apply(validator, fallback);
        File file = path.toFile();
        if (!file.exists()) return apply(validator, fallback);

        try (InputStream in = Files.newInputStream(path)) {
            Properties props = new Properties();
            props.load(in);
            String raw = props.getProperty(playerId.toString());
            double value = raw != null ? Double.parseDouble(raw) : fallback;
            return apply(validator, value);
        } catch (IOException | NumberFormatException e) {
            LOGGER.warn("Failed to load {} for {}: {}", file.getName(), playerId, e.getMessage());
            return apply(validator, fallback);
        }
    }

    /**
     * 写入单个玩家的数值（保留文件中其他玩家的记录）。
     *
     * @return 是否写入成功
     */
    public static boolean saveDouble(Path path, UUID playerId, double value) {
        if (path == null) return false;

        try {
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);

            // 先读出既有内容，只更新目标玩家，避免覆盖其他玩家
            Properties props = new Properties();
            if (Files.exists(path)) {
                try (InputStream in = Files.newInputStream(path)) {
                    props.load(in);
                }
            }
            props.setProperty(playerId.toString(), String.valueOf(value));

            try (OutputStream out = Files.newOutputStream(path)) {
                props.store(out, "PDoPN Player Data");
            }
            return true;
        } catch (IOException e) {
            LOGGER.warn("Failed to save {} for {}: {}", path.getFileName(), playerId, e.getMessage());
            return false;
        }
    }

    private static double apply(DoubleUnaryOperator validator, double value) {
        return validator != null ? validator.applyAsDouble(value) : value;
    }
}
