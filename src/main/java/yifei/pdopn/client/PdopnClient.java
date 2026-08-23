package yifei.pdopn.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import yifei.pdopn.items.PdopnItems;
import yifei.pdopn.temperature.TemperatureData;

/**
 * 客户端初始化器。
 * 职责：注册按键绑定（HUD 开关）、物品温度 Tooltip、发送自定义网络包。
 */
public class PdopnClient implements ClientModInitializer {

    public static final Identifier TOGGLE_HUD_CHANNEL = new Identifier("pdopn", "toggle_hud");

    private static KeyBinding toggleHudKey;

    /** 原版水瓶（water bottle）的着色值，来自 PotionContents water */
    private static final int WATER_BOTTLE_COLOR = 0x385DC6;

    @Override
    public void onInitializeClient() {
        // 注册按键绑定（默认无按键，玩家可在控制设置中自行绑定）
        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.pdopn.toggle_hud",
            InputUtil.UNKNOWN_KEY.getCode(),
            "category.pdopn"
        ));

        // 净水瓶：复用原版 potion 纹理，通过 tint 着色为水瓶蓝色（视觉等同于原版 water bottle）
        ColorProviderRegistry.ITEM.register((stack, tintIndex) ->
            tintIndex == 0 ? WATER_BOTTLE_COLOR : -1,
            PdopnItems.PURE_WATER_BOTTLE);

        // 每 tick 检查按键
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleHudKey.wasPressed()) {
                ClientPlayNetworking.send(TOGGLE_HUD_CHANNEL, PacketByteBufs.create());
            }
        });

        // 物品温度 Tooltip
        ItemTooltipCallback.EVENT.register((ItemStack stack, TooltipContext context, java.util.List<Text> lines) -> {
            double itemTemp = TemperatureData.getItemTemp(stack.getItem());
            if (itemTemp != 0.0) {
                String sign = itemTemp > 0 ? "+" : "";
                String value = String.format("%.3f", itemTemp);
                int color = itemTemp > 0 ? 0xFF6600 : 0x5555FF;
                lines.add(Text.literal("🌡 " + sign + value + "°C")
                    .formatted(Formatting.GRAY)
                    .copy()
                    .setStyle(lines.get(0).getStyle().withColor(TextColor.fromRgb(color))));
            }

            double insulation = TemperatureData.getArmorInsulation(stack.getItem());
            if (insulation > 0.0) {
                String value = String.format("%.0f", insulation * 100);
                lines.add(Text.literal("🛡 " + value + "%")
                    .formatted(Formatting.GRAY)
                    .copy()
                    .setStyle(lines.get(0).getStyle().withColor(TextColor.fromRgb(0x55FF55))));
            }
        });
    }
}
