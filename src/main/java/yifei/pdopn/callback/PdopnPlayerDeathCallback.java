package yifei.pdopn.callback;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家死亡回调。由 PlayerEntityDeathMixin 触发，
 * 通过注册机制将 Mixin 与业务逻辑解耦（DIP 依赖倒置）。
 */
public final class PdopnPlayerDeathCallback {

    private static final List<Callback> listeners = new ArrayList<>();

    public static void register(Callback callback) {
        listeners.add(callback);
    }

    public static void fire(ServerPlayerEntity player) {
        for (Callback c : listeners) {
            c.onPlayerDeath(player);
        }
    }

    @FunctionalInterface
    public interface Callback {
        void onPlayerDeath(ServerPlayerEntity player);
    }
}
