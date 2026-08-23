/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

@Environment(value=EnvType.CLIENT)
public class LockButtonWidget
extends ButtonWidget {
    private boolean locked;

    public LockButtonWidget(int x, int y, ButtonWidget.PressAction action) {
        super(x, y, 20, 20, Text.translatable("narrator.button.difficulty_lock"), action, DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    protected MutableText getNarrationMessage() {
        return ScreenTexts.joinSentences(super.getNarrationMessage(), this.isLocked() ? Text.translatable("narrator.button.difficulty_lock.locked") : Text.translatable("narrator.button.difficulty_lock.unlocked"));
    }

    public boolean isLocked() {
        return this.locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @Override
    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        IconLocation iconLocation = !this.active ? (this.locked ? IconLocation.LOCKED_DISABLED : IconLocation.UNLOCKED_DISABLED) : (this.isSelected() ? (this.locked ? IconLocation.LOCKED_HOVER : IconLocation.UNLOCKED_HOVER) : (this.locked ? IconLocation.LOCKED : IconLocation.UNLOCKED));
        context.drawTexture(ButtonWidget.WIDGETS_TEXTURE, this.getX(), this.getY(), iconLocation.getU(), iconLocation.getV(), this.width, this.height);
    }

    @Environment(value=EnvType.CLIENT)
    static enum IconLocation {
        LOCKED(0, 146),
        LOCKED_HOVER(0, 166),
        LOCKED_DISABLED(0, 186),
        UNLOCKED(20, 146),
        UNLOCKED_HOVER(20, 166),
        UNLOCKED_DISABLED(20, 186);

        private final int u;
        private final int v;

        private IconLocation(int u, int v) {
            this.u = u;
            this.v = v;
        }

        public int getU() {
            return this.u;
        }

        public int getV() {
            return this.v;
        }
    }
}

