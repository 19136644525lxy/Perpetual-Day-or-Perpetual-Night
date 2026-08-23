/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.client.realms.gui.screen;

import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.realms.dto.Backup;
import net.minecraft.client.realms.gui.screen.RealmsScreen;
import net.minecraft.client.realms.gui.screen.RealmsSlotOptionsScreen;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

@Environment(value=EnvType.CLIENT)
public class RealmsBackupInfoScreen
extends RealmsScreen {
    private static final Text UNKNOWN = Text.translatable("mco.backup.unknown");
    private final Screen parent;
    final Backup backup;
    private BackupInfoList backupInfoList;

    public RealmsBackupInfoScreen(Screen parent, Backup backup) {
        super(Text.translatable("mco.backup.info.title"));
        this.parent = parent;
        this.backup = backup;
    }

    @Override
    public void tick() {
    }

    @Override
    public void init() {
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.BACK, button -> this.client.setScreen(this.parent)).dimensions(this.width / 2 - 100, this.height / 4 + 120 + 24, 200, 20).build());
        this.backupInfoList = new BackupInfoList(this.client);
        this.addSelectableChild(this.backupInfoList);
        this.focusOn(this.backupInfoList);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.client.setScreen(this.parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        this.backupInfoList.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    Text checkForSpecificMetadata(String key, String value) {
        String string = key.toLowerCase(Locale.ROOT);
        if (string.contains("game") && string.contains("mode")) {
            return this.gameModeMetadata(value);
        }
        if (string.contains("game") && string.contains("difficulty")) {
            return this.gameDifficultyMetadata(value);
        }
        return Text.literal(value);
    }

    private Text gameDifficultyMetadata(String value) {
        try {
            return RealmsSlotOptionsScreen.DIFFICULTIES.get(Integer.parseInt(value)).getTranslatableName();
        } catch (Exception exception) {
            return UNKNOWN;
        }
    }

    private Text gameModeMetadata(String value) {
        try {
            return RealmsSlotOptionsScreen.GAME_MODES.get(Integer.parseInt(value)).getSimpleTranslatableName();
        } catch (Exception exception) {
            return UNKNOWN;
        }
    }

    @Environment(value=EnvType.CLIENT)
    class BackupInfoList
    extends AlwaysSelectedEntryListWidget<BackupInfoListEntry> {
        public BackupInfoList(MinecraftClient client) {
            super(client, RealmsBackupInfoScreen.this.width, RealmsBackupInfoScreen.this.height, 32, RealmsBackupInfoScreen.this.height - 64, 36);
            this.setRenderSelection(false);
            if (RealmsBackupInfoScreen.this.backup.changeList != null) {
                RealmsBackupInfoScreen.this.backup.changeList.forEach((key, value) -> this.addEntry(new BackupInfoListEntry((String)key, (String)value)));
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    class BackupInfoListEntry
    extends AlwaysSelectedEntryListWidget.Entry<BackupInfoListEntry> {
        private static final Text TEMPLATE_NAME_TEXT = Text.translatable("mco.backup.entry.templateName");
        private static final Text GAME_DIFFICULTY_TEXT = Text.translatable("mco.backup.entry.gameDifficulty");
        private static final Text NAME_TEXT = Text.translatable("mco.backup.entry.name");
        private static final Text GAME_SERVER_VERSION_TEXT = Text.translatable("mco.backup.entry.gameServerVersion");
        private static final Text UPLOADED_TEXT = Text.translatable("mco.backup.entry.uploaded");
        private static final Text ENABLED_PACK_TEXT = Text.translatable("mco.backup.entry.enabledPack");
        private static final Text DESCRIPTION_TEXT = Text.translatable("mco.backup.entry.description");
        private static final Text GAME_MODE_TEXT = Text.translatable("mco.backup.entry.gameMode");
        private static final Text SEED_TEXT = Text.translatable("mco.backup.entry.seed");
        private static final Text WORLD_TYPE_TEXT = Text.translatable("mco.backup.entry.worldType");
        private static final Text UNDEFINED_TEXT = Text.translatable("mco.backup.entry.undefined");
        private final String key;
        private final String value;

        public BackupInfoListEntry(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            context.drawTextWithShadow(RealmsBackupInfoScreen.this.textRenderer, this.getTextFromKey(this.key), x, y, 0xA0A0A0);
            context.drawTextWithShadow(RealmsBackupInfoScreen.this.textRenderer, RealmsBackupInfoScreen.this.checkForSpecificMetadata(this.key, this.value), x, y + 12, 0xFFFFFF);
        }

        private Text getTextFromKey(String key) {
            return switch (key) {
                case "template_name" -> TEMPLATE_NAME_TEXT;
                case "game_difficulty" -> GAME_DIFFICULTY_TEXT;
                case "name" -> NAME_TEXT;
                case "game_server_version" -> GAME_SERVER_VERSION_TEXT;
                case "uploaded" -> UPLOADED_TEXT;
                case "enabled_pack" -> ENABLED_PACK_TEXT;
                case "description" -> DESCRIPTION_TEXT;
                case "game_mode" -> GAME_MODE_TEXT;
                case "seed" -> SEED_TEXT;
                case "world_type" -> WORLD_TYPE_TEXT;
                default -> UNDEFINED_TEXT;
            };
        }

        @Override
        public Text getNarration() {
            return Text.translatable("narrator.select", this.key + " " + this.value);
        }
    }
}

