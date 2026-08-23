/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.client.realms.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.realms.RealmsError;
import net.minecraft.client.realms.exception.RealmsServiceException;
import net.minecraft.client.realms.gui.screen.RealmsScreen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;

@Environment(value=EnvType.CLIENT)
public class RealmsGenericErrorScreen
extends RealmsScreen {
    private final Screen parent;
    private final ErrorMessages errorMessages;
    private MultilineText description = MultilineText.EMPTY;

    public RealmsGenericErrorScreen(RealmsServiceException realmsServiceException, Screen parent) {
        super(NarratorManager.EMPTY);
        this.parent = parent;
        this.errorMessages = RealmsGenericErrorScreen.getErrorMessages(realmsServiceException);
    }

    public RealmsGenericErrorScreen(Text description, Screen parent) {
        super(NarratorManager.EMPTY);
        this.parent = parent;
        this.errorMessages = RealmsGenericErrorScreen.getErrorMessages(description);
    }

    public RealmsGenericErrorScreen(Text title, Text description, Screen parent) {
        super(NarratorManager.EMPTY);
        this.parent = parent;
        this.errorMessages = RealmsGenericErrorScreen.getErrorMessages(title, description);
    }

    private static ErrorMessages getErrorMessages(RealmsServiceException exception) {
        RealmsError realmsError = exception.error;
        if (realmsError == null) {
            return RealmsGenericErrorScreen.getErrorMessages(Text.translatable("mco.errorMessage.realmsService", exception.httpResultCode), Text.literal(exception.httpResponseText));
        }
        int i = realmsError.getErrorCode();
        String string = "mco.errorMessage." + i;
        return RealmsGenericErrorScreen.getErrorMessages(Text.translatable("mco.errorMessage.realmsService.realmsError", i), I18n.hasTranslation(string) ? Text.translatable(string) : Text.of(realmsError.getErrorMessage()));
    }

    private static ErrorMessages getErrorMessages(Text description) {
        return RealmsGenericErrorScreen.getErrorMessages(Text.translatable("mco.errorMessage.generic"), description);
    }

    private static ErrorMessages getErrorMessages(Text title, Text description) {
        return new ErrorMessages(title, description);
    }

    @Override
    public void init() {
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.OK, button -> this.client.setScreen(this.parent)).dimensions(this.width / 2 - 100, this.height - 52, 200, 20).build());
        this.description = MultilineText.create(this.textRenderer, (StringVisitable)this.errorMessages.detail, this.width * 3 / 4);
    }

    @Override
    public Text getNarratedTitle() {
        return Text.empty().append(this.errorMessages.title).append(": ").append(this.errorMessages.detail);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        context.drawCenteredTextWithShadow(this.textRenderer, this.errorMessages.title, this.width / 2, 80, 0xFFFFFF);
        this.description.drawCenterWithShadow(context, this.width / 2, 100, this.client.textRenderer.fontHeight, 0xFF0000);
        super.render(context, mouseX, mouseY, delta);
    }

    @Environment(value=EnvType.CLIENT)
    record ErrorMessages(Text title, Text detail) {
    }
}

