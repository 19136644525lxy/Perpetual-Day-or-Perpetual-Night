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
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yifei.pdopn.command.PdopnCommand;
import yifei.pdopn.entity.PdopnEntityModifier;
import yifei.pdopn.hud.PdopnHudRenderer;
import yifei.pdopn.mode.PdopnMode;
import yifei.pdopn.items.PdopnItems;
import yifei.pdopn.callback.PdopnPlayerDeathCallback;
import yifei.pdopn.recipe.PdopnRecipes;
import yifei.pdopn.rules.PdopnGameRules;
import yifei.pdopn.rules.PdopnSettings;
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

    /**
     * 指令处理器实例（依赖注入，替代原先的静态 setter 注入）。
     * 需要被 {@link #onModeChange} 访问以通知模式管理层，故保持静态引用。
     */
    private static PdopnCommand commandHandler;

    @Override
    public void onInitialize() {
        LOGGER.info("[PDoPN] Perpetual Day or Perpetual Night 模组已加载");

        // 注册自定义物品（净水瓶、净水桶）
        PdopnItems.register();

        // 注册净水烧炼配方类型（只接受水瓶，防止贵重药水被误烧）
        PdopnRecipes.register();

        // 注册模组游戏规则（必须在此处触发类加载，静态字段负责实际注册）
        LOGGER.info("[PDoPN] 已注册 {} 条游戏规则", PdopnGameRules.ruleCount());

        // 构造指令处理器：通过构造函数注入依赖（DIP）
        commandHandler = new PdopnCommand(this, temperatureManager, thirstManager);

        // 注册指令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            commandHandler.register(dispatcher)
        );

        // 实体加载时按当前模式施加修改（覆盖新生成和区块加载的生物）
        // 规则每 tick 解析一次即可，实体加载是低频事件，这里即时解析以保持准确
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) ->
            entityModifier.onEntityLoaded(entity, currentMode, PdopnSettings.resolve(world.getServer()))
        );

        // 每个服务端 tick 结束时锁定时间 + 温度更新
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

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
            boolean hudDefault = PdopnSettings.resolve(server).hudDefault();
            temperatureManager.onPlayerJoin(handler.getPlayer(), hudDefault);
            thirstManager.onPlayerJoin(handler.getPlayer());
        });

        // 玩家离开时保存温度 + 口渴数据
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            temperatureManager.onPlayerLeave(handler.getPlayer().getUuid());
            thirstManager.onPlayerLeave(handler.getPlayer().getUuid());
        });

        // 玩家死亡时：口渴重置为初始值，体温重置（全局偏移保留）
        // 通过 Mixin 回调机制注册（PlayerEntityDeathMixin 调用此回调）
        PdopnPlayerDeathCallback.register(player -> {
            temperatureManager.onPlayerDeath(player);
            thirstManager.onPlayerDeath(player);
        });

        // 玩家右键水方块 → 直接饮水
        // 参照 LegendarySurvivalOverhaul 实现：
        // 水方块没有碰撞箱，UseBlockCallback 传来的 hitResult 指向水底方块，
        // 因此用 Entity.raycast(distance, tickDelta, includeFluids=true) 重新进行包含流体的射线检测
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            // 仅服务端处理
            if (world.isClient) return ActionResult.PASS;
            // 仅主手触发（副手忽略，避免双触发）
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
            // 潜行时跳过（玩家潜行时正常右键方块，如打开容器）
            if (player.isSneaking()) return ActionResult.PASS;

            // 仅空手允许饮水（LegendarySurvivalOverhaul 也是仅空手触发）
            ItemStack stack = player.getStackInHand(hand);
            if (!stack.isEmpty()) return ActionResult.PASS;

            // 用 includeFluids=true 重新进行射线检测，专门捕获流体方块
            // 4.5 格为 1.20.1 生存模式默认交互距离
            HitResult fluidHit = player.raycast(4.5, 1.0f, true);
            if (fluidHit.getType() != HitResult.Type.BLOCK) return ActionResult.PASS;

            BlockPos waterPos = ((BlockHitResult) fluidHit).getBlockPos();
            BlockState state = world.getBlockState(waterPos);

            // 必须是水方块（过滤岩浆等其他流体）
            if (!state.isOf(Blocks.WATER)) return ActionResult.PASS;

            // 转发到口渴管理器处理
            boolean handled = thirstManager.onDrinkWaterFromWorld(
                (ServerPlayerEntity) player,
                (ServerWorld) world,
                waterPos
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
            entityModifier.onModeChanged(newMode, serverRef, PdopnSettings.resolve(serverRef));
            // 立即持久化，避免崩溃 / 强退导致模式与偏移不一致
            temperatureManager.saveGlobalData();
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

        // 首次 tick 时加载全局偏移数据，并恢复存档中记录的模式
        if (!globalDataLoaded) {
            PdopnMode persisted = temperatureManager.loadGlobalData();
            if (persisted != PdopnMode.NORMAL) {
                // 模式必须与偏移一起恢复：否则重启后模式回到 NORMAL，
                // 而 NORMAL 不应用偏移，玩家会以为永昼/永夜进度丢失。
                currentMode = persisted;
                temperatureManager.setCurrentMode(persisted);
                LOGGER.info("[PDoPN] 已从存档恢复模式：{}", persisted);
            }
            globalDataLoaded = true;
        }

        // 解析本 tick 生效的游戏规则（per-world 覆盖），供各系统共享
        PdopnSettings settings = PdopnSettings.resolve(server);

        // 温度 + 口渴系统更新（每 tick 调用；由 pdopnTemperature / pdopnThirst 规则控制）
        if (settings.temperatureSystem()) {
            temperatureManager.tick(server, settings);
        }
        if (settings.thirstSystem()) {
            thirstManager.tick(server, settings);
        }

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

        // 每 6000 tick（约 5 分钟）与在线实体对齐一次实体记录表，
        // 清理已卸载区块中生物的残留 UUID（否则其重新加载时不会被增强）
        if (hudTickCount % 6000 == 0) {
            entityModifier.reconcile(server);
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
