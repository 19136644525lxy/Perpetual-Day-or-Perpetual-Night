/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.client.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

@Environment(value=EnvType.CLIENT)
public class MessageScreen
extends Screen {
    public MessageScreen(Text text) {
        super(text);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackgroundTexture(context);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 70, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }
}

