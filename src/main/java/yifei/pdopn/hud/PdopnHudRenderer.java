package yifei.pdopn.hud;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import yifei.pdopn.config.PdopnConfig;
import yifei.pdopn.temperature.PdopnTemperatureManager;
import yifei.pdopn.thirst.PdopnThirstManager;

import java.util.UUID;

/**
 * HUD 合并渲染器。
 * 职责：将温度与口渴两条数据合并为单条 action bar 消息发送，避免互相覆盖。
 * 设计依据：SRP（单一职责）—— Manager 负责数据计算，Renderer 负责呈现。
 */
public final class PdopnHudRenderer {

    /** 温度部分与环境部分之间的分隔符 */
    private static final String SEPARATOR = "  |  ";

    private PdopnHudRenderer() {}

    /** 合并发送温度 + 口渴 HUD */
    public static void send(ServerPlayerEntity player,
                            PdopnTemperatureManager tempMgr,
                            PdopnThirstManager thirstMgr) {
        UUID id = player.getUuid();
        double bodyTemp = tempMgr.getBodyTemp(id);
        double envTemp = tempMgr.getLastEnvTemp(id);
        double hydration = thirstMgr.getHydration(id);
        double prevHydration = thirstMgr.getPreviousHydration(id);

        Text tempPart = buildTempPart(bodyTemp, envTemp);
        Text thirstPart = buildThirstPart(hydration, prevHydration);

        Text hud = Text.literal("")
            .append(tempPart)
            .append(Text.literal(SEPARATOR).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x666666))))
            .append(thirstPart);

        player.sendMessage(hud, true);
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
            .append(Text.literal(arrow + " ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA))))
            .append(Text.literal("环境 " + envStr + "°C").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x888888))));
    }

    /** 根据体温返回 HUD 颜色（与原温度 manager 逻辑一致，保持视觉延续） */
    private static int getTempColor(double temp) {
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
            .append(Text.literal(" " + arrow).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA))));
    }

    /** 根据口渴值返回颜色（与原口渴 manager 逻辑一致） */
    private static int getHydrationColor(double hydration) {
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
}
