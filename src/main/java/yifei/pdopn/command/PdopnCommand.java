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
import yifei.pdopn.config.PdopnConfig;
import yifei.pdopn.hud.PdopnHudRenderer;
import yifei.pdopn.mode.PdopnMode;
import yifei.pdopn.temperature.PdopnTemperatureManager;
import yifei.pdopn.thirst.PdopnThirstManager;

import java.util.List;

/**
 * /pdopn 指令的注册与执行。
 * 职责：指令注册、模式切换、标题广播。
 *
 * <p>依赖通过构造函数注入（而非静态字段），因此本类可脱离服务器实例化与测试；
 * 指令执行过程中不再读取任何可变静态状态。
 */
public final class PdopnCommand {

    /** 模式切换回调（由主类注入），避免直接依赖主类实现 */
    private final ModeChangeListener modeChangeListener;

    /** 温度管理器 */
    private final PdopnTemperatureManager temperatureManager;

    /** 口渴管理器 */
    private final PdopnThirstManager thirstManager;

    public PdopnCommand(ModeChangeListener modeChangeListener,
                        PdopnTemperatureManager temperatureManager,
                        PdopnThirstManager thirstManager) {
        this.modeChangeListener = modeChangeListener;
        this.temperatureManager = temperatureManager;
        this.thirstManager = thirstManager;
    }

    /**
     * 将指令注册到 Brigadier 调度器。
     */
    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("pdopn")
                .then(CommandManager.literal("day")
                    .executes(this::switchToDay))
                .then(CommandManager.literal("night")
                    .executes(this::switchToNight))
                .then(CommandManager.literal("cycle")
                    .executes(this::restoreCycle))
                .then(CommandManager.literal("status")
                    .executes(this::showStatus))
                // 温度系统子命令
                .then(CommandManager.literal("temp")
                    .executes(this::showTemp)
                    .then(CommandManager.literal("maxdays")
                        .then(CommandManager.argument("days", IntegerArgumentType.integer(1))
                            .executes(this::setMaxDays)))
                    .then(CommandManager.literal("set")
                        .then(CommandManager.argument("value", FloatArgumentType.floatArg(-100.0f, 100.0f))
                            .executes(this::setTemp)))
                    .then(CommandManager.literal("hud")
                        .executes(this::toggleHud)))
                // 口渴系统子命令
                .then(CommandManager.literal("thirst")
                    .executes(this::showThirst)
                    .then(CommandManager.literal("set")
                        .then(CommandManager.argument("value", FloatArgumentType.floatArg(0.0f, 100.0f))
                            .executes(this::setThirst))))
                // 全局偏移子命令（可在控制台执行）
                .then(CommandManager.literal("drift")
                    .executes(this::showDrift)
                    .then(CommandManager.literal("reset")
                        .executes(this::resetDrift)))
                // 重新加载配置文件（可在控制台执行）
                .then(CommandManager.literal("reload")
                    .executes(this::reloadConfig))
        );
    }

    /* ────────── 子命令实现 ────────── */

    /**
     * 解析指令执行者。
     * 控制台 / 命令方块 / 其他非玩家执行者没有玩家实体，
     * 旧实现直接调用 {@code source.getPlayer()} 会抛 NPE 导致指令崩溃。
     *
     * @return 执行者玩家；非玩家执行者返回 null（并已反馈错误）
     */
    private ServerPlayerEntity requirePlayer(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.translatable("pdopn.error.player_only"));
        }
        return player;
    }

    private int switchToDay(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = requirePlayer(context.getSource());
        if (player == null) return 0;
        executeSwitch(player, PdopnMode.PERPETUAL_DAY);
        return 1;
    }

    private int switchToNight(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = requirePlayer(context.getSource());
        if (player == null) return 0;
        executeSwitch(player, PdopnMode.PERPETUAL_NIGHT);
        return 1;
    }

    private int restoreCycle(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        PdopnMode current = getCurrentMode();

        if (current == PdopnMode.NORMAL) {
            source.sendFeedback(() -> Text.translatable("pdopn.feedback.already_normal"), false);
            return 1;
        }
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) return 0;
        executeSwitch(player, PdopnMode.NORMAL);
        return 1;
    }

    private int showStatus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        PdopnMode mode = getCurrentMode();

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

    private int showTemp(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = requirePlayer(source);

        if (player == null || temperatureManager == null) return 0;

        double bodyTemp = temperatureManager.getBodyTemp(player.getUuid());
        String sign = bodyTemp >= 0 ? "+" : "";
        String tempStr = String.format("%.1f", bodyTemp);

        // 颜色梯度统一取自 PdopnHudRenderer，避免指令显示与 HUD 出现两套数值
        int color = PdopnHudRenderer.getTempColor(bodyTemp);
        source.sendFeedback(
            () -> Text.translatable("pdopn.temp.display")
                .append(Text.literal(sign + tempStr + "°C")
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)).withBold(true))),
            false
        );
        return 1;
    }

    private int setMaxDays(CommandContext<ServerCommandSource> context) {
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

    private int setTemp(CommandContext<ServerCommandSource> context) {
        float value = FloatArgumentType.getFloat(context, "value");
        ServerPlayerEntity player = requirePlayer(context.getSource());
        if (player == null) return 0;

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

    private int toggleHud(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = requirePlayer(context.getSource());
        if (player == null || temperatureManager == null) return 0;

        boolean newState = temperatureManager.toggleHud(player.getUuid());
        String key = newState ? "pdopn.temp.hud_on" : "pdopn.temp.hud_off";
        context.getSource().sendFeedback(
            () -> Text.translatable(key),
            true
        );
        return 1;
    }

    /* ────────── 口渴系统子命令 ────────── */

    private int showThirst(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = requirePlayer(source);

        if (player == null || thirstManager == null) return 0;

        double hydration = thirstManager.getHydration(player.getUuid());
        String valueStr = String.format("%.1f", hydration);

        int color = PdopnHudRenderer.getHydrationColor(hydration);
        source.sendFeedback(
            () -> Text.translatable("pdopn.thirst.display")
                .append(Text.literal(valueStr)
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)).withBold(true))),
            false
        );
        return 1;
    }

    private int setThirst(CommandContext<ServerCommandSource> context) {
        float value = FloatArgumentType.getFloat(context, "value");
        ServerPlayerEntity player = requirePlayer(context.getSource());
        if (player == null) return 0;

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

    /* ────────── 全局偏移子命令 ────────── */

    /** 查看当前累计偏移（控制台也可执行） */
    private int showDrift(CommandContext<ServerCommandSource> context) {
        if (temperatureManager == null) return 0;

        double drift = temperatureManager.getAccumulatedDrift();
        String sign = drift >= 0 ? "+" : "";
        context.getSource().sendFeedback(
            () -> Text.translatable("pdopn.drift.display")
                .append(Text.literal(sign + String.format("%.1f", drift) + "°C")
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFD475)).withBold(true))),
            false
        );
        return 1;
    }

    /**
     * 清空累计偏移，无需重启服务器。
     * 此前玩家一旦让偏移滚到致死温度就无解，只能改配置并重启。
     */
    private int resetDrift(CommandContext<ServerCommandSource> context) {
        if (temperatureManager == null) return 0;

        double before = temperatureManager.getAccumulatedDrift();
        temperatureManager.resetAccumulatedDrift();
        temperatureManager.saveGlobalData();

        String sign = before >= 0 ? "+" : "";
        context.getSource().sendFeedback(
            () -> Text.translatable("pdopn.drift.reset")
                .append(Text.literal(" (" + sign + String.format("%.1f", before) + "°C → 0.0°C)")),
            true
        );
        return 1;
    }

    /* ────────── 配置重载 ────────── */

    /** 重新加载 pdopn.json（控制台也可执行） */
    private int reloadConfig(CommandContext<ServerCommandSource> context) {
        PdopnConfig.reload();
        context.getSource().sendFeedback(
            () -> Text.translatable("pdopn.config.reloaded"),
            true
        );
        return 1;
    }

    /* ────────── 核心切换逻辑 ────────── */

    /** 执行模式切换 */
    public void executeSwitch(ServerPlayerEntity player, PdopnMode targetMode) {
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
    private void disableDaylightCycle(ServerWorld world, ServerCommandSource source) {
        world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false, source.getServer());
    }

    /** 获取当前模式 */
    private PdopnMode getCurrentMode() {
        return modeChangeListener != null ? modeChangeListener.getCurrentMode() : PdopnMode.NORMAL;
    }

    /** 通知模式管理层切换模式 */
    private void notifyModeChange(PdopnMode newMode) {
        if (modeChangeListener != null) {
            modeChangeListener.onModeChange(newMode);
        }
    }

    /**
     * 向服务器所有玩家广播主标题 + 副标题。
     * 淡入 10 ticks (0.5s), 停留 60 ticks (3s), 淡出 10 ticks (0.5s)
     */
    private void broadcastTitle(ServerCommandSource source, Text title, Text subtitle) {
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
    private void clearAllTitles(ServerCommandSource source) {
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
