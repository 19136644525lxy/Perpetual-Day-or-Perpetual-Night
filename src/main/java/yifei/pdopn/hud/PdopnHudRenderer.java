package yifei.pdopn.hud;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import yifei.pdopn.config.PdopnConfig;
import yifei.pdopn.mode.PdopnMode;
import yifei.pdopn.temperature.PdopnTemperatureManager;
import yifei.pdopn.thirst.PdopnThirstManager;

import java.util.UUID;

/**
 * HUD 合并渲染器。
 * 职责：将温度、口渴、模式偏移三组数据合并为单条 action bar 消息发送，避免互相覆盖。
 * 设计依据：SRP（单一职责）—— Manager 负责数据计算，Renderer 负责呈现。
 */
public final class PdopnHudRenderer {

    /** 各数据段之间的分隔符 */
    private static final String SEPARATOR = "  |  ";

    /** 分隔符颜色 */
    private static final int SEPARATOR_COLOR = 0x666666;

    /** 「环境温度」次要文字颜色 */
    private static final int MUTED_COLOR = 0x888888;

    /** 箭头文字颜色 */
    private static final int ARROW_COLOR = 0xAAAAAA;

    /** 偏移安全阈值（°C）：超过此值开始用警示色提示 */
    private static final double DRIFT_WARN_THRESHOLD = 20.0;

    private PdopnHudRenderer() {}

    /** 合并发送温度 + 口渴 + 偏移 HUD */
    public static void send(ServerPlayerEntity player,
                            PdopnTemperatureManager tempMgr,
                            PdopnThirstManager thirstMgr) {
        UUID id = player.getUuid();
        double bodyTemp = tempMgr.getBodyTemp(id);
        double envTemp = tempMgr.getLastEnvTemp(id);
        double hydration = thirstMgr.getHydration(id);
        double prevHydration = thirstMgr.getPreviousHydration(id);

        Text hud = Text.literal("")
            .append(buildTempPart(bodyTemp, envTemp))
            .append(separator())
            .append(buildThirstPart(hydration, prevHydration))
            .append(buildDriftPart(tempMgr));

        player.sendMessage(hud, true);
    }

    private static Text separator() {
        return Text.literal(SEPARATOR).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(SEPARATOR_COLOR)));
    }

    /* ────────── 温度部分 ────────── */

    private static Text buildTempPart(double bodyTemp, double envTemp) {
        int color = getTempColor(bodyTemp);
        String sign = bodyTemp >= 0 ? "+" : "";
        String bodyStr = String.format("%.1f", bodyTemp);
        String envStr = String.format("%.1f", envTemp);
        String arrow = getTempArrow(bodyTemp, envTemp);

        return Text.literal("")
            .append(Text.literal("🌡 ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))))
            .append(Text.literal(sign + bodyStr + "°C ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))))
            .append(Text.literal(arrow + " ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(ARROW_COLOR))))
            .append(Text.translatable("pdopn.hud.env").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(MUTED_COLOR))))
            .append(Text.literal(" " + envStr + "°C").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(MUTED_COLOR))));
    }

    /** 根据体温返回 HUD 颜色（与原温度 manager 逻辑一致，保持视觉延续） */
    public static int getTempColor(double temp) {
        double abs = Math.abs(temp);
        if (abs <= 10.0) return 0xFFFFFF;
        if (abs <= 25.0) return temp > 0 ? 0xFFD475 : 0x88CCFF;
        if (abs <= 45.0) return temp > 0 ? 0xFFAA00 : 0x5555FF;
        if (abs <= 70.0) return temp > 0 ? 0xFF6600 : 0x2222CC;
        if (abs <= 85.0) return temp > 0 ? 0xFF3300 : 0x6600CC;
        return temp > 0 ? 0xFF0000 : 0x9900FF;
    }

    private static String getTempArrow(double bodyTemp, double envTemp) {
        if (Math.abs(bodyTemp - envTemp) < 1.0) return "→";
        return bodyTemp < envTemp ? "↑" : "↓";
    }

    /* ────────── 口渴部分 ────────── */

    private static Text buildThirstPart(double hydration, double prevHydration) {
        int color = getHydrationColor(hydration);
        String valueStr = String.format("%.1f", hydration);
        String arrow = getThirstArrow(hydration, prevHydration);

        return Text.literal("")
            .append(Text.literal("💧 ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))))
            .append(Text.literal(valueStr).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))))
            .append(Text.literal(" " + arrow).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(ARROW_COLOR))));
    }

    /** 根据口渴值返回颜色（阈值来自配置，与指令显示保持同源） */
    public static int getHydrationColor(double hydration) {
        PdopnConfig.ThirstConfig cfg = PdopnConfig.getInstance().thirst;
        if (hydration >= cfg.comfortZoneLow) return 0x55FFFF;
        if (hydration >= cfg.lightThirstLow) return 0xFFAA00;
        if (hydration >= cfg.mediumDehydrationLow) return 0xFF6600;
        if (hydration >= cfg.heavyDehydrationLow) return 0xFF3300;
        return 0xFF0000;
    }

    private static String getThirstArrow(double hydration, double prevHydration) {
        double diff = hydration - prevHydration;
        if (Math.abs(diff) < 0.001) return "→";
        return diff > 0 ? "↑" : "↓";
    }

    /* ────────── 模式与偏移部分 ────────── */

    /**
     * 构建模式 / 偏移显示段。
     *
     * <p>永昼 / 永夜模式下显示「模式 第N天 偏移±X°C」，让玩家能看见本模组最核心的
     * 漂移机制（旧 HUD 完全没有这项信息，玩家无从判断自己已经深入多少天）。
     * 正常模式下仅在仍有残余偏移时显示偏移衰减情况。
     */
    private static Text buildDriftPart(PdopnTemperatureManager tempMgr) {
        PdopnMode mode = tempMgr.getCurrentMode();
        double drift = tempMgr.getAccumulatedDrift();

        // 正常模式且偏移已衰减干净 → 不占用 HUD 空间
        if (mode == PdopnMode.NORMAL && Math.abs(drift) < 0.05) {
            return Text.literal("");
        }

        int driftColor = getDriftColor(drift, mode);
        String driftSign = drift >= 0 ? "+" : "";
        String driftStr = String.format("%.1f", drift);

        Text part = Text.literal("")
            .append(separator())
            .append(Text.literal("🧭 ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(driftColor))))
            .append(Text.translatable(modeKey(mode)).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(driftColor))));

        // 天数只对永昼 / 永夜有意义
        if (mode != PdopnMode.NORMAL) {
            part = part.copy()
                .append(Text.literal(" " + tempMgr.getPerpetualDays())
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(MUTED_COLOR))))
                .append(Text.translatable("pdopn.hud.days")
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(MUTED_COLOR))));
        }

        return part.copy()
            .append(Text.literal(" ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(SEPARATOR_COLOR))))
            .append(Text.translatable("pdopn.hud.drift")
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(MUTED_COLOR))))
            .append(Text.literal(" " + driftSign + driftStr + "°C")
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(driftColor))));
    }

    /** 偏移越大颜色越警示（超过阈值转红） */
    private static int getDriftColor(double drift, PdopnMode mode) {
        if (mode == PdopnMode.NORMAL) return 0x55FF55;
        double abs = Math.abs(drift);
        if (abs < DRIFT_WARN_THRESHOLD) return 0xFFD475;
        if (abs < 45.0) return 0xFF6600;
        return 0xFF3300;
    }

    private static String modeKey(PdopnMode mode) {
        return switch (mode) {
            case PERPETUAL_DAY -> "pdopn.hud.perpetual_day";
            case PERPETUAL_NIGHT -> "pdopn.hud.perpetual_night";
            case NORMAL -> "pdopn.hud.normal";
        };
    }
}
