/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.client.realms.gui.screen;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.realms.RealmsLabel;
import net.minecraft.client.realms.RealmsObjectSelectionList;
import net.minecraft.client.realms.gui.screen.RealmsGenericErrorScreen;
import net.minecraft.client.realms.gui.screen.RealmsResetWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsScreen;
import net.minecraft.client.realms.gui.screen.RealmsUploadScreen;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class RealmsSelectFileToUploadScreen
extends RealmsScreen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Text LOADING_ERROR_TEXT = Text.translatable("selectWorld.unable_to_load");
    static final Text WORLD_LANG = Text.translatable("selectWorld.world");
    static final Text HARDCORE_TEXT = Text.translatable("mco.upload.hardcore").styled(style -> style.withColor(-65536));
    static final Text CHEATS_TEXT = Text.translatable("selectWorld.cheats");
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat();
    private final RealmsResetWorldScreen parent;
    private final long worldId;
    private final int slotId;
    ButtonWidget uploadButton;
    List<LevelSummary> levelList = Lists.newArrayList();
    int selectedWorld = -1;
    WorldSelectionList worldSelectionList;
    private final Runnable onBack;

    public RealmsSelectFileToUploadScreen(long worldId, int slotId, RealmsResetWorldScreen parent, Runnable onBack) {
        super(Text.translatable("mco.upload.select.world.title"));
        this.parent = parent;
        this.worldId = worldId;
        this.slotId = slotId;
        this.onBack = onBack;
    }

    private void loadLevelList() throws Exception {
        LevelStorage.LevelList levelList = this.client.getLevelStorage().getLevelList();
        this.levelList = this.client.getLevelStorage().loadSummaries(levelList).join().stream().filter(a -> !a.requiresConversion() && !a.isLocked()).collect(Collectors.toList());
        for (LevelSummary levelSummary : this.levelList) {
            this.worldSelectionList.addEntry(levelSummary);
        }
    }

    @Override
    public void init() {
        this.worldSelectionList = new WorldSelectionList();
        try {
            this.loadLevelList();
        } catch (Exception exception) {
            LOGGER.error("Couldn't load level list", exception);
            this.client.setScreen(new RealmsGenericErrorScreen(LOADING_ERROR_TEXT, Text.of(exception.getMessage()), this.parent));
            return;
        }
        this.addSelectableChild(this.worldSelectionList);
        this.uploadButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("mco.upload.button.name"), button -> this.upload()).dimensions(this.width / 2 - 154, this.height - 32, 153, 20).build());
        this.uploadButton.active = this.selectedWorld >= 0 && this.selectedWorld < this.levelList.size();
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.BACK, button -> this.client.setScreen(this.parent)).dimensions(this.width / 2 + 6, this.height - 32, 153, 20).build());
        this.addLabel(new RealmsLabel(Text.translatable("mco.upload.select.world.subtitle"), this.width / 2, RealmsSelectFileToUploadScreen.row(-1), 0xA0A0A0));
        if (this.levelList.isEmpty()) {
            this.addLabel(new RealmsLabel(Text.translatable("mco.upload.select.world.none"), this.width / 2, this.height / 2 - 20, 0xFFFFFF));
        }
    }

    @Override
    public Text getNarratedTitle() {
        return ScreenTexts.joinSentences(this.getTitle(), this.narrateLabels());
    }

    private void upload() {
        if (this.selectedWorld != -1 && !this.levelList.get(this.selectedWorld).isHardcore()) {
            LevelSummary levelSummary = this.levelList.get(this.selectedWorld);
            this.client.setScreen(new RealmsUploadScreen(this.worldId, this.slotId, this.parent, levelSummary, this.onBack));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        this.worldSelectionList.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 13, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.client.setScreen(this.parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    static Text getGameModeName(LevelSummary summary) {
        return summary.getGameMode().getTranslatableName();
    }

    static String getLastPlayed(LevelSummary summary) {
        return DATE_FORMAT.format(new Date(summary.getLastPlayed()));
    }

    @Environment(value=EnvType.CLIENT)
    class WorldSelectionList
    extends RealmsObjectSelectionList<WorldListEntry> {
        public WorldSelectionList() {
            super(RealmsSelectFileToUploadScreen.this.width, RealmsSelectFileToUploadScreen.this.height, RealmsSelectFileToUploadScreen.row(0), RealmsSelectFileToUploadScreen.this.height - 40, 36);
        }

        public void addEntry(LevelSummary summary) {
            this.addEntry(new WorldListEntry(summary));
        }

        @Override
        public int getMaxPosition() {
            return RealmsSelectFileToUploadScreen.this.levelList.size() * 36;
        }

        @Override
        public void renderBackground(DrawContext context) {
            RealmsSelectFileToUploadScreen.this.renderBackground(context);
        }

        @Override
        public void setSelected(@Nullable WorldListEntry worldListEntry) {
            super.setSelected(worldListEntry);
            RealmsSelectFileToUploadScreen.this.selectedWorld = this.children().indexOf(worldListEntry);
            RealmsSelectFileToUploadScreen.this.uploadButton.active = RealmsSelectFileToUploadScreen.this.selectedWorld >= 0 && RealmsSelectFileToUploadScreen.this.selectedWorld < this.getEntryCount() && !RealmsSelectFileToUploadScreen.this.levelList.get(RealmsSelectFileToUploadScreen.this.selectedWorld).isHardcore();
        }
    }

    @Environment(value=EnvType.CLIENT)
    class WorldListEntry
    extends AlwaysSelectedEntryListWidget.Entry<WorldListEntry> {
        private final LevelSummary summary;
        private final String displayName;
        private final Text nameAndLastPlayed;
        private final Text details;

        public WorldListEntry(LevelSummary summary) {
            this.summary = summary;
            this.displayName = summary.getDisplayName();
            this.nameAndLastPlayed = Text.translatable("mco.upload.entry.id", summary.getName(), RealmsSelectFileToUploadScreen.getLastPlayed(summary));
            Text text = summary.isHardcore() ? HARDCORE_TEXT : RealmsSelectFileToUploadScreen.getGameModeName(summary);
            if (summary.hasCheats()) {
                text = Text.translatable("mco.upload.entry.cheats", text.getString(), CHEATS_TEXT);
            }
            this.details = text;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            this.renderItem(context, index, x, y);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            RealmsSelectFileToUploadScreen.this.worldSelectionList.setSelected(RealmsSelectFileToUploadScreen.this.levelList.indexOf(this.summary));
            return true;
        }

        protected void renderItem(DrawContext context, int index, int x, int y) {
            Object string = this.displayName.isEmpty() ? WORLD_LANG + " " + (index + 1) : this.displayName;
            context.drawText(RealmsSelectFileToUploadScreen.this.textRenderer, (String)string, x + 2, y + 1, 0xFFFFFF, false);
            context.drawText(RealmsSelectFileToUploadScreen.this.textRenderer, this.nameAndLastPlayed, x + 2, y + 12, 0x808080, false);
            context.drawText(RealmsSelectFileToUploadScreen.this.textRenderer, this.details, x + 2, y + 12 + 10, 0x808080, false);
        }

        @Override
        public Text getNarration() {
            Text text = ScreenTexts.joinLines(Text.literal(this.summary.getDisplayName()), Text.literal(RealmsSelectFileToUploadScreen.getLastPlayed(this.summary)), RealmsSelectFileToUploadScreen.getGameModeName(this.summary));
            return Text.translatable("narrator.select", text);
        }
    }
}

