package yifei.pdopn;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yifei.pdopn.command.PdopnCommand;
import yifei.pdopn.entity.PdopnEntityModifier;
import yifei.pdopn.hud.PdopnHudRenderer;
import yifei.pdopn.mode.PdopnMode;
import yifei.pdopn.items.PdopnItems;
import yifei.pdopn.temperature.PdopnTemperatureManager;
import yifei.pdopn.thirst.PdopnThirstManager;

/**
 * 永昼或永夜 — 主入口类。
 * 职责：模组初始化、模式状态管理、服务端 Tick 时间锁定。
 */
public class PerpetualDayOrPerpetualNight implements ModInitializer, PdopnCommand.ModeChangeListener {
    public static final String MOD_ID = "pdopn";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** 当前运行模式，volatile 保证跨线程可见性 */
    private static volatile PdopnMode currentMode = PdopnMode.NORMAL;

    /** 缓存的服务端引用，由 onServerTick 更新 */
    private static MinecraftServer serverRef;

    /** HUD 渲染节拍计数器（每 20 tick = 1 秒发送一次合并 HUD） */
    private static int hudTickCount = 0;

    /** 全局偏移数据是否已加载（首次 tick 时加载） */
    private static boolean globalDataLoaded = false;

    /** 实体属性与 AI 修改器 */
    private final PdopnEntityModifier entityModifier = new PdopnEntityModifier();

    /** 温度系统管理器 */
    private static final PdopnTemperatureManager temperatureManager = new PdopnTemperatureManager();

    /** 口渴系统管理器 */
    private static final PdopnThirstManager thirstManager = new PdopnThirstManager();

    @Override
    public void onInitialize() {
        LOGGER.info("[PDoPN] Perpetual Day or Perpetual Night 模组已加载");

        // 注册自定义物品（净水瓶、净水桶）
        PdopnItems.register();

        // 注入模式变更监听器，实现指令层与模式管理层的解耦 (DIP)
        PdopnCommand.setModeChangeListener(this);

        // 注册指令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            PdopnCommand.register(dispatcher)
        );

        // 实体加载时按当前模式施加修改（覆盖新生成和区块加载的生物）
        ServerEntityEvents.ENTITY_LOAD.register((entity, server) ->
            entityModifier.onEntityLoaded(entity, currentMode)
        );

        // 每个服务端 tick 结束时锁定时间 + 温度更新
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

        // 暴露温度管理器给指令层
        PdopnCommand.setTemperatureManager(temperatureManager);
        PdopnCommand.setThirstManager(thirstManager);

        // 口渴系统关联温度管理器
        thirstManager.setTemperatureManager(temperatureManager);

        // 服务端接收客户端 HUD 切换包
        Identifier toggleHudChannel = new Identifier(MOD_ID, "toggle_hud");
        ServerPlayNetworking.registerGlobalReceiver(toggleHudChannel,
            (server, player, handler, buf, responseSender) -> {
                boolean newState = temperatureManager.toggleHud(player.getUuid());
                server.execute(() -> {
                    String key = newState ? "pdopn.temp.hud_on" : "pdopn.temp.hud_off";
                    player.sendMessage(Text.translatable(key).formatted(Formatting.GRAY), true);
                });
            }
        );

        // 玩家加入时加载温度 + 口渴数据
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            temperatureManager.onPlayerJoin(handler.getPlayer());
            thirstManager.onPlayerJoin(handler.getPlayer());
        });

        // 玩家离开时保存温度 + 口渴数据
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            temperatureManager.onPlayerLeave(handler.getPlayer().getUuid());
            thirstManager.onPlayerLeave(handler.getPlayer().getUuid());
        });

        // 玩家右键水方块 → 直接饮水
        // 触发条件：空手或手持无功能物品 + 视线指向水方块
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            // 仅服务端处理
            if (world.isClient) return ActionResult.PASS;
            // 仅主手触发（副手忽略，避免双触发）
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
            // 潜行时跳过（玩家潜行时正常右键方块，如打开容器）
            if (player.isSneaking()) return ActionResult.PASS;

            // 拦截空手 / 棒 / 棍 / 红粉 / 火把 等无功能物品
            ItemStack stack = player.getStackInHand(hand);
            boolean canDrink = stack.isEmpty()
                || stack.getItem() == Items.STICK
                || stack.getItem() == Items.BONE
                || stack.getItem() == Items.BLAZE_ROD
                || stack.getItem() == Items.REDSTONE
                || stack.getItem() == Items.GLOWSTONE_DUST
                || stack.getItem() == Items.SUGAR;
            if (!canDrink) return ActionResult.PASS;

            // 视线必须命中方块
            if (!(hitResult instanceof BlockHitResult)) return ActionResult.PASS;
            BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();
            BlockState state = world.getBlockState(pos);

            // 必须是水方块
            if (!state.isOf(Blocks.WATER)) return ActionResult.PASS;

            // 转发到口渴管理器处理
            boolean handled = thirstManager.onDrinkWaterFromWorld(
                (ServerPlayerEntity) player,
                (ServerWorld) world,
                pos
            );
            return handled ? ActionResult.SUCCESS : ActionResult.PASS;
        });
    }

    /* ────────── ModeChangeListener 实现 ────────── */

    @Override
    public void onModeChange(PdopnMode newMode) {
        currentMode = newMode;
        temperatureManager.setCurrentMode(newMode);
        if (serverRef != null) {
            entityModifier.onModeChanged(newMode, serverRef);
        }
    }

    @Override
    public PdopnMode getCurrentMode() {
        return currentMode;
    }

    /* ────────── Tick 处理 ────────── */

    /** 每个 tick 结束：锁定时间 + 温度更新 */
    private void onServerTick(MinecraftServer server) {
        serverRef = server;
        PdopnMode mode = currentMode;
        hudTickCount++;

        // 首次 tick 时加载全局偏移数据
        if (!globalDataLoaded) {
            temperatureManager.loadGlobalData();
            globalDataLoaded = true;
        }

        // 温度 + 口渴系统更新（每 tick 调用）
        temperatureManager.tick(server);
        thirstManager.tick(server);

        // 每 20 tick 合并发送温度+口渴 HUD（共用温度 HUD 开关，避免互相覆盖）
        if (hudTickCount % 20 == 0) {
            for (ServerWorld world : server.getWorlds()) {
                for (ServerPlayerEntity player : world.getPlayers()) {
                    if (temperatureManager.isHudEnabled(player.getUuid())) {
                        PdopnHudRenderer.send(player, temperatureManager, thirstManager);
                    }
                }
            }
        }

        if (!mode.isTimeLocked()) {
            return;
        }

        long targetTime = mode.getTargetTime();
        for (ServerWorld world : server.getWorlds()) {
            // 确保昼夜循环规则处于关闭状态
            if (world.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE)) {
                world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false, server);
            }
            // 时间偏移时修正
            if (world.getTimeOfDay() != targetTime) {
                world.setTimeOfDay(targetTime);
            }
        }
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    /** 获取温度管理器实例 */
    public static PdopnTemperatureManager getTemperatureManager() {
        return temperatureManager;
    }

    /** 获取口渴管理器实例 */
    public static PdopnThirstManager getThirstManager() {
        return thirstManager;
    }
}
