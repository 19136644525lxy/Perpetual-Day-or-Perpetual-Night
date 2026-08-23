package yifei.pdopn.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.network.packet.s2c.play.ClearTitleS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.world.GameRules;
import yifei.pdopn.mode.PdopnMode;
import yifei.pdopn.temperature.PdopnTemperatureManager;
import yifei.pdopn.temperature.TemperatureData;
import yifei.pdopn.thirst.PdopnThirstManager;

import java.util.List;

/**
 * /pdopn 指令的注册与执行。
 * 职责：指令注册、模式切换、标题广播。
 */
public final class PdopnCommand {

    /** 模式切换回调，由主类在初始化时注入，避免直接依赖主类 */
    private static ModeChangeListener modeChangeListener;

    /** 温度管理器引用，由主类注入 */
    private static PdopnTemperatureManager temperatureManager;

    /** 口渴管理器引用，由主类注入 */
    private static PdopnThirstManager thirstManager;

    private PdopnCommand() {}

    /** 注册模式切换回调，解耦指令层与模式管理层 */
    public static void setModeChangeListener(ModeChangeListener listener) {
        modeChangeListener = listener;
    }

    /** 注入温度管理器 */
    public static void setTemperatureManager(PdopnTemperatureManager manager) {
        temperatureManager = manager;
    }

    /** 注入口渴管理器 */
    public static void setThirstManager(PdopnThirstManager manager) {
        thirstManager = manager;
    }

    /**
     * 将指令注册到 Brigadier 调度器。
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("pdopn")
                .then(CommandManager.literal("day")
                    .executes(PdopnCommand::switchToDay))
                .then(CommandManager.literal("night")
                    .executes(PdopnCommand::switchToNight))
                .then(CommandManager.literal("cycle")
                    .executes(PdopnCommand::restoreCycle))
                .then(CommandManager.literal("status")
                    .executes(PdopnCommand::showStatus))
                // 温度系统子命令
                .then(CommandManager.literal("temp")
                    .executes(PdopnCommand::showTemp)
                    .then(CommandManager.literal("maxdays")
                        .then(CommandManager.argument("days", IntegerArgumentType.integer(1))
                            .executes(PdopnCommand::setMaxDays)))
                    .then(CommandManager.literal("set")
                        .then(CommandManager.argument("value", FloatArgumentType.floatArg(-100.0f, 100.0f))
                            .executes(PdopnCommand::setTemp)))
                    .then(CommandManager.literal("hud")
                        .executes(PdopnCommand::toggleHud)))
                // 口渴系统子命令
                .then(CommandManager.literal("thirst")
                    .executes(PdopnCommand::showThirst)
                    .then(CommandManager.literal("set")
                        .then(CommandManager.argument("value", FloatArgumentType.floatArg(0.0f, 100.0f))
                            .executes(PdopnCommand::setThirst))))
        );
    }

    /* ────────── 子命令实现 ────────── */

    private static int switchToDay(CommandContext<ServerCommandSource> context) {
        executeSwitch(context.getSource().getPlayer(), PdopnMode.PERPETUAL_DAY);
        return 1;
    }

    private static int switchToNight(CommandContext<ServerCommandSource> context) {
        executeSwitch(context.getSource().getPlayer(), PdopnMode.PERPETUAL_NIGHT);
        return 1;
    }

    private static int restoreCycle(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        PdopnMode current = getCurrentMode();

        if (current == PdopnMode.NORMAL) {
            source.sendFeedback(() -> Text.translatable("pdopn.feedback.already_normal"), false);
            return 1;
        }
        executeSwitch(source.getPlayer(), PdopnMode.NORMAL);
        return 1;
    }

    private static int showStatus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        PdopnMode mode = modeChangeListener != null ? modeChangeListener.getCurrentMode() : PdopnMode.NORMAL;

        String modeKey;
        int color;
        switch (mode) {
            case PERPETUAL_DAY -> { modeKey = "pdopn.mode.day"; color = 0xFFAA00; }
            case PERPETUAL_NIGHT -> { modeKey = "pdopn.mode.night"; color = 0x5555FF; }
            default -> { modeKey = "pdopn.mode.normal"; color = 0x55FF55; }
        }

        source.sendFeedback(
            () -> Text.translatable("pdopn.feedback.current_mode")
                .append(Text.translatable(modeKey).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)).withBold(true))),
            false
        );
        return 1;
    }

    /* ────────── 温度系统子命令 ────────── */

    private static int showTemp(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (temperatureManager == null) return 1;

        double bodyTemp = temperatureManager.getBodyTemp(player.getUuid());
        String sign = bodyTemp >= 0 ? "+" : "";
        String tempStr = String.format("%.1f", bodyTemp);

        int color = getTempDisplayColor(bodyTemp);
        source.sendFeedback(
            () -> Text.translatable("pdopn.temp.display")
                .append(Text.literal(sign + tempStr + "°C")
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)).withBold(true))),
            false
        );
        return 1;
    }

    private static int setMaxDays(CommandContext<ServerCommandSource> context) {
        int days = IntegerArgumentType.getInteger(context, "days");
        if (temperatureManager != null) {
            temperatureManager.setMaxDays(days);
        }
        context.getSource().sendFeedback(
            () -> Text.translatable("pdopn.temp.maxdays_set").append(Text.literal(" " + days)),
            true
        );
        return 1;
    }

    private static int setTemp(CommandContext<ServerCommandSource> context) {
        float value = FloatArgumentType.getFloat(context, "value");
        ServerPlayerEntity player = context.getSource().getPlayer();

        if (temperatureManager != null) {
            temperatureManager.setBodyTemp(player.getUuid(), value);
        }

        String sign = value >= 0 ? "+" : "";
        context.getSource().sendFeedback(
            () -> Text.translatable("pdopn.temp.set")
                .append(Text.literal(" " + sign + String.format("%.1f", value) + "°C")),
            false
        );
        return 1;
    }

    private static int toggleHud(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (temperatureManager == null) return 1;

        boolean newState = temperatureManager.toggleHud(player.getUuid());
        String key = newState ? "pdopn.temp.hud_on" : "pdopn.temp.hud_off";
        context.getSource().sendFeedback(
            () -> Text.translatable(key),
            true
        );
        return 1;
    }

    /** 根据体温返回显示颜色 */
    private static int getTempDisplayColor(double temp) {
        double abs = Math.abs(temp);
        if (abs <= 10.0) return 0xFFFFFF;
        if (abs <= 25.0) return temp > 0 ? 0xFFD475 : 0x88CCFF;
        if (abs <= 45.0) return temp > 0 ? 0xFFAA00 : 0x5555FF;
        if (abs <= 70.0) return temp > 0 ? 0xFF6600 : 0x2222CC;
        return temp > 0 ? 0xFF0000 : 0x9900FF;
    }

    /* ────────── 口渴系统子命令 ────────── */

    private static int showThirst(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (thirstManager == null) return 1;

        double hydration = thirstManager.getHydration(player.getUuid());
        String valueStr = String.format("%.1f", hydration);

        int color = getHydrationColor(hydration);
        source.sendFeedback(
            () -> Text.translatable("pdopn.thirst.display")
                .append(Text.literal(valueStr)
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)).withBold(true))),
            false
        );
        return 1;
    }

    private static int setThirst(CommandContext<ServerCommandSource> context) {
        float value = FloatArgumentType.getFloat(context, "value");
        ServerPlayerEntity player = context.getSource().getPlayer();

        if (thirstManager != null) {
            thirstManager.setHydration(player.getUuid(), value);
        }

        context.getSource().sendFeedback(
            () -> Text.translatable("pdopn.thirst.set")
                .append(Text.literal(" " + String.format("%.1f", value))),
            false
        );
        return 1;
    }

    private static int getHydrationColor(double hydration) {
        if (hydration >= 60.0) return 0x55FFFF;
        if (hydration >= 40.0) return 0xFFAA00;
        if (hydration >= 25.0) return 0xFF6600;
        if (hydration >= 10.0) return 0xFF3300;
        return 0xFF0000;
    }

    /* ────────── 核心切换逻辑 ────────── */

    /** 执行模式切换 */
    public static void executeSwitch(ServerPlayerEntity player, PdopnMode targetMode) {
        ServerCommandSource source = player.getCommandSource();
        ServerWorld world = source.getWorld();

        if (targetMode == PdopnMode.NORMAL) {
            // 恢复所有世界的昼夜循环
            for (ServerWorld w : source.getServer().getWorlds()) {
                if (!w.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE)) {
                    w.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(true, source.getServer());
                }
            }
            notifyModeChange(PdopnMode.NORMAL);
            clearAllTitles(source);
            player.sendMessage(Text.translatable("pdopn.feedback.cycle_restored"), false);
        } else if (targetMode == PdopnMode.PERPETUAL_DAY) {
            disableDaylightCycle(world, source);
            world.setTimeOfDay(PdopnMode.PERPETUAL_DAY.getTargetTime());
            notifyModeChange(PdopnMode.PERPETUAL_DAY);
            broadcastTitle(source,
                Text.translatable("pdopn.title.perpetual_day").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFAA00)).withBold(true)),
                Text.translatable("pdopn.subtitle.perpetual_day").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFD475)))
            );
            player.sendMessage(Text.translatable("pdopn.feedback.switched_day"), false);
        } else if (targetMode == PdopnMode.PERPETUAL_NIGHT) {
            disableDaylightCycle(world, source);
            world.setTimeOfDay(PdopnMode.PERPETUAL_NIGHT.getTargetTime());
            notifyModeChange(PdopnMode.PERPETUAL_NIGHT);
            broadcastTitle(source,
                Text.translatable("pdopn.title.perpetual_night").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x5555FF)).withBold(true)),
                Text.translatable("pdopn.subtitle.perpetual_night").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x8888FF)))
            );
            player.sendMessage(Text.translatable("pdopn.feedback.switched_night"), false);
        }
    }

    /* ────────── 工具方法 ────────── */

    /** 关闭指定世界的昼夜循环规则 */
    private static void disableDaylightCycle(ServerWorld world, ServerCommandSource source) {
        world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false, source.getServer());
    }

    /** 获取当前模式 */
    private static PdopnMode getCurrentMode() {
        return modeChangeListener != null ? modeChangeListener.getCurrentMode() : PdopnMode.NORMAL;
    }

    /** 通知模式管理层切换模式 */
    private static void notifyModeChange(PdopnMode newMode) {
        if (modeChangeListener != null) {
            modeChangeListener.onModeChange(newMode);
        }
    }

    /**
     * 向服务器所有玩家广播主标题 + 副标题。
     * 淡入 10 ticks (0.5s), 停留 60 ticks (3s), 淡出 10 ticks (0.5s)
     */
    private static void broadcastTitle(ServerCommandSource source, Text title, Text subtitle) {
        List<ServerPlayerEntity> players = source.getServer().getPlayerManager().getPlayerList();
        TitleFadeS2CPacket fadePacket = new TitleFadeS2CPacket(10, 60, 10);
        SubtitleS2CPacket subtitlePacket = new SubtitleS2CPacket(subtitle);
        TitleS2CPacket titlePacket = new TitleS2CPacket(title);

        for (ServerPlayerEntity player : players) {
            player.networkHandler.sendPacket(fadePacket);
            player.networkHandler.sendPacket(subtitlePacket);
            player.networkHandler.sendPacket(titlePacket);
        }
    }

    /** 清除所有玩家的标题显示 */
    private static void clearAllTitles(ServerCommandSource source) {
        ClearTitleS2CPacket packet = new ClearTitleS2CPacket(false);
        for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(packet);
        }
    }

    /**
     * 模式变更监听接口，用于解耦指令层与模式管理层。
     * 遵循依赖倒置原则 (DIP)：指令层依赖抽象接口而非具体实现。
     */
    public interface ModeChangeListener {
        void onModeChange(PdopnMode newMode);
        PdopnMode getCurrentMode();
    }
}
