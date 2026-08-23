/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.client.gui.screen.multiplayer;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.LoadingDisplay;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.WorldIcon;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.network.LanServerInfo;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.logging.UncaughtExceptionLogger;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class MultiplayerServerListWidget
extends AlwaysSelectedEntryListWidget<Entry> {
    static final Logger LOGGER = LogUtils.getLogger();
    static final ThreadPoolExecutor SERVER_PINGER_THREAD_POOL = new ScheduledThreadPoolExecutor(5, new ThreadFactoryBuilder().setNameFormat("Server Pinger #%d").setDaemon(true).setUncaughtExceptionHandler(new UncaughtExceptionLogger(LOGGER)).build());
    private static final Identifier UNKNOWN_SERVER_TEXTURE = new Identifier("textures/misc/unknown_server.png");
    static final Identifier SERVER_SELECTION_TEXTURE = new Identifier("textures/gui/server_selection.png");
    static final Identifier ICONS_TEXTURE = new Identifier("textures/gui/icons.png");
    static final Text LAN_SCANNING_TEXT = Text.translatable("lanServer.scanning");
    static final Text CANNOT_RESOLVE_TEXT = Text.translatable("multiplayer.status.cannot_resolve").styled(style -> style.withColor(-65536));
    static final Text CANNOT_CONNECT_TEXT = Text.translatable("multiplayer.status.cannot_connect").styled(style -> style.withColor(-65536));
    static final Text INCOMPATIBLE_TEXT = Text.translatable("multiplayer.status.incompatible");
    static final Text NO_CONNECTION_TEXT = Text.translatable("multiplayer.status.no_connection");
    static final Text PINGING_TEXT = Text.translatable("multiplayer.status.pinging");
    static final Text ONLINE_TEXT = Text.translatable("multiplayer.status.online");
    private final MultiplayerScreen screen;
    private final List<ServerEntry> servers = Lists.newArrayList();
    private final Entry scanningEntry = new ScanningEntry();
    private final List<LanServerEntry> lanServers = Lists.newArrayList();

    public MultiplayerServerListWidget(MultiplayerScreen screen, MinecraftClient client, int width, int height, int top, int bottom, int entryHeight) {
        super(client, width, height, top, bottom, entryHeight);
        this.screen = screen;
    }

    private void updateEntries() {
        this.clearEntries();
        this.servers.forEach(server -> this.addEntry(server));
        this.addEntry(this.scanningEntry);
        this.lanServers.forEach(lanServer -> this.addEntry(lanServer));
    }

    @Override
    public void setSelected(@Nullable Entry entry) {
        super.setSelected(entry);
        this.screen.updateButtonActivationStates();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Entry entry = (Entry)this.getSelectedOrNull();
        return entry != null && entry.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void setServers(ServerList servers) {
        this.servers.clear();
        for (int i = 0; i < servers.size(); ++i) {
            this.servers.add(new ServerEntry(this.screen, servers.get(i)));
        }
        this.updateEntries();
    }

    public void setLanServers(List<LanServerInfo> lanServers) {
        int i = lanServers.size() - this.lanServers.size();
        this.lanServers.clear();
        for (LanServerInfo lanServerInfo : lanServers) {
            this.lanServers.add(new LanServerEntry(this.screen, lanServerInfo));
        }
        this.updateEntries();
        for (int j = this.lanServers.size() - i; j < this.lanServers.size(); ++j) {
            LanServerEntry lanServerEntry = this.lanServers.get(j);
            int k = j - this.lanServers.size() + this.children().size();
            int l = this.getRowTop(k);
            int m = this.getRowBottom(k);
            if (m < this.top || l > this.bottom) continue;
            this.client.getNarratorManager().narrateSystemMessage(Text.translatable("multiplayer.lan.server_found", lanServerEntry.getMotdNarration()));
        }
    }

    @Override
    protected int getScrollbarPositionX() {
        return super.getScrollbarPositionX() + 30;
    }

    @Override
    public int getRowWidth() {
        return super.getRowWidth() + 85;
    }

    public void onRemoved() {
    }

    @Environment(value=EnvType.CLIENT)
    public static class ScanningEntry
    extends Entry {
        private final MinecraftClient client = MinecraftClient.getInstance();

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int i = y + entryHeight / 2 - this.client.textRenderer.fontHeight / 2;
            context.drawText(this.client.textRenderer, LAN_SCANNING_TEXT, this.client.currentScreen.width / 2 - this.client.textRenderer.getWidth(LAN_SCANNING_TEXT) / 2, i, 0xFFFFFF, false);
            String string = LoadingDisplay.get(Util.getMeasuringTimeMs());
            context.drawText(this.client.textRenderer, string, this.client.currentScreen.width / 2 - this.client.textRenderer.getWidth(string) / 2, i + this.client.textRenderer.fontHeight, 0x808080, false);
        }

        @Override
        public Text getNarration() {
            return LAN_SCANNING_TEXT;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static abstract class Entry
    extends AlwaysSelectedEntryListWidget.Entry<Entry>
    implements AutoCloseable {
        @Override
        public void close() {
        }
    }

    @Environment(value=EnvType.CLIENT)
    public class ServerEntry
    extends Entry {
        private static final int field_32387 = 32;
        private static final int field_32388 = 32;
        private static final int field_32389 = 0;
        private static final int field_32390 = 32;
        private static final int field_32391 = 64;
        private static final int field_32392 = 96;
        private static final int field_32393 = 0;
        private static final int field_32394 = 32;
        private final MultiplayerScreen screen;
        private final MinecraftClient client;
        private final ServerInfo server;
        private final WorldIcon icon;
        @Nullable
        private byte[] favicon;
        private long time;

        protected ServerEntry(MultiplayerScreen screen, ServerInfo server) {
            this.screen = screen;
            this.server = server;
            this.client = MinecraftClient.getInstance();
            this.icon = WorldIcon.forServer(this.client.getTextureManager(), server.address);
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            List<Text> list2;
            Text text2;
            int l;
            if (!this.server.online) {
                this.server.online = true;
                this.server.ping = -2L;
                this.server.label = ScreenTexts.EMPTY;
                this.server.playerCountLabel = ScreenTexts.EMPTY;
                SERVER_PINGER_THREAD_POOL.submit(() -> {
                    try {
                        this.screen.getServerListPinger().add(this.server, () -> this.client.execute(this::saveFile));
                    } catch (UnknownHostException unknownHostException) {
                        this.server.ping = -1L;
                        this.server.label = CANNOT_RESOLVE_TEXT;
                    } catch (Exception exception) {
                        this.server.ping = -1L;
                        this.server.label = CANNOT_CONNECT_TEXT;
                    }
                });
            }
            boolean bl = !this.protocolVersionMatches();
            context.drawText(this.client.textRenderer, this.server.name, x + 32 + 3, y + 1, 0xFFFFFF, false);
            List<OrderedText> list = this.client.textRenderer.wrapLines(this.server.label, entryWidth - 32 - 2);
            for (int i = 0; i < Math.min(list.size(), 2); ++i) {
                context.drawText(this.client.textRenderer, list.get(i), x + 32 + 3, y + 12 + this.client.textRenderer.fontHeight * i, 0x808080, false);
            }
            Text text = bl ? this.server.version.copy().formatted(Formatting.RED) : this.server.playerCountLabel;
            int j = this.client.textRenderer.getWidth(text);
            context.drawText(this.client.textRenderer, text, x + entryWidth - j - 15 - 2, y + 1, 0x808080, false);
            int k = 0;
            if (bl) {
                l = 5;
                text2 = INCOMPATIBLE_TEXT;
                list2 = this.server.playerListSummary;
            } else if (this.pinged()) {
                l = this.server.ping < 0L ? 5 : (this.server.ping < 150L ? 0 : (this.server.ping < 300L ? 1 : (this.server.ping < 600L ? 2 : (this.server.ping < 1000L ? 3 : 4))));
                if (this.server.ping < 0L) {
                    text2 = NO_CONNECTION_TEXT;
                    list2 = Collections.emptyList();
                } else {
                    text2 = Text.translatable("multiplayer.status.ping", this.server.ping);
                    list2 = this.server.playerListSummary;
                }
            } else {
                k = 1;
                l = (int)(Util.getMeasuringTimeMs() / 100L + (long)(index * 2) & 7L);
                if (l > 4) {
                    l = 8 - l;
                }
                text2 = PINGING_TEXT;
                list2 = Collections.emptyList();
            }
            context.drawTexture(ICONS_TEXTURE, x + entryWidth - 15, y, k * 10, 176 + l * 8, 10, 8, 256, 256);
            byte[] bs = this.server.getFavicon();
            if (!Arrays.equals(bs, this.favicon)) {
                if (this.uploadFavicon(bs)) {
                    this.favicon = bs;
                } else {
                    this.server.setFavicon(null);
                    this.saveFile();
                }
            }
            this.draw(context, x, y, this.icon.getTextureId());
            int m = mouseX - x;
            int n = mouseY - y;
            if (m >= entryWidth - 15 && m <= entryWidth - 5 && n >= 0 && n <= 8) {
                this.screen.setMultiplayerScreenTooltip(Collections.singletonList(text2));
            } else if (m >= entryWidth - j - 15 - 2 && m <= entryWidth - 15 - 2 && n >= 0 && n <= 8) {
                this.screen.setMultiplayerScreenTooltip(list2);
            }
            if (this.client.options.getTouchscreen().getValue().booleanValue() || hovered) {
                context.fill(x, y, x + 32, y + 32, -1601138544);
                int o = mouseX - x;
                int p = mouseY - y;
                if (this.canConnect()) {
                    if (o < 32 && o > 16) {
                        context.drawTexture(SERVER_SELECTION_TEXTURE, x, y, 0.0f, 32.0f, 32, 32, 256, 256);
                    } else {
                        context.drawTexture(SERVER_SELECTION_TEXTURE, x, y, 0.0f, 0.0f, 32, 32, 256, 256);
                    }
                }
                if (index > 0) {
                    if (o < 16 && p < 16) {
                        context.drawTexture(SERVER_SELECTION_TEXTURE, x, y, 96.0f, 32.0f, 32, 32, 256, 256);
                    } else {
                        context.drawTexture(SERVER_SELECTION_TEXTURE, x, y, 96.0f, 0.0f, 32, 32, 256, 256);
                    }
                }
                if (index < this.screen.getServerList().size() - 1) {
                    if (o < 16 && p > 16) {
                        context.drawTexture(SERVER_SELECTION_TEXTURE, x, y, 64.0f, 32.0f, 32, 32, 256, 256);
                    } else {
                        context.drawTexture(SERVER_SELECTION_TEXTURE, x, y, 64.0f, 0.0f, 32, 32, 256, 256);
                    }
                }
            }
        }

        private boolean pinged() {
            return this.server.online && this.server.ping != -2L;
        }

        private boolean protocolVersionMatches() {
            return this.server.protocolVersion == SharedConstants.getGameVersion().getProtocolVersion();
        }

        public void saveFile() {
            this.screen.getServerList().saveFile();
        }

        protected void draw(DrawContext context, int x, int y, Identifier textureId) {
            RenderSystem.enableBlend();
            context.drawTexture(textureId, x, y, 0.0f, 0.0f, 32, 32, 32, 32);
            RenderSystem.disableBlend();
        }

        private boolean canConnect() {
            return true;
        }

        private boolean uploadFavicon(@Nullable byte[] bytes) {
            if (bytes == null) {
                this.icon.destroy();
            } else {
                try {
                    this.icon.load(NativeImage.read(bytes));
                } catch (Throwable throwable) {
                    LOGGER.error("Invalid icon for server {} ({})", this.server.name, this.server.address, throwable);
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (Screen.hasShiftDown()) {
                MultiplayerServerListWidget multiplayerServerListWidget = this.screen.serverListWidget;
                int i = multiplayerServerListWidget.children().indexOf(this);
                if (i == -1) {
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_DOWN && i < this.screen.getServerList().size() - 1 || keyCode == GLFW.GLFW_KEY_UP && i > 0) {
                    this.swapEntries(i, keyCode == GLFW.GLFW_KEY_DOWN ? i + 1 : i - 1);
                    return true;
                }
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        private void swapEntries(int i, int j) {
            this.screen.getServerList().swapEntries(i, j);
            this.screen.serverListWidget.setServers(this.screen.getServerList());
            Entry entry = (Entry)this.screen.serverListWidget.children().get(j);
            this.screen.serverListWidget.setSelected(entry);
            MultiplayerServerListWidget.this.ensureVisible(entry);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            double d = mouseX - (double)MultiplayerServerListWidget.this.getRowLeft();
            double e = mouseY - (double)MultiplayerServerListWidget.this.getRowTop(MultiplayerServerListWidget.this.children().indexOf(this));
            if (d <= 32.0) {
                if (d < 32.0 && d > 16.0 && this.canConnect()) {
                    this.screen.select(this);
                    this.screen.connect();
                    return true;
                }
                int i = this.screen.serverListWidget.children().indexOf(this);
                if (d < 16.0 && e < 16.0 && i > 0) {
                    this.swapEntries(i, i - 1);
                    return true;
                }
                if (d < 16.0 && e > 16.0 && i < this.screen.getServerList().size() - 1) {
                    this.swapEntries(i, i + 1);
                    return true;
                }
            }
            this.screen.select(this);
            if (Util.getMeasuringTimeMs() - this.time < 250L) {
                this.screen.connect();
            }
            this.time = Util.getMeasuringTimeMs();
            return true;
        }

        public ServerInfo getServer() {
            return this.server;
        }

        @Override
        public Text getNarration() {
            MutableText mutableText = Text.empty();
            mutableText.append(Text.translatable("narrator.select", this.server.name));
            mutableText.append(ScreenTexts.SENTENCE_SEPARATOR);
            if (!this.protocolVersionMatches()) {
                mutableText.append(INCOMPATIBLE_TEXT);
                mutableText.append(ScreenTexts.SENTENCE_SEPARATOR);
                mutableText.append(Text.translatable("multiplayer.status.version.narration", this.server.version));
                mutableText.append(ScreenTexts.SENTENCE_SEPARATOR);
                mutableText.append(Text.translatable("multiplayer.status.motd.narration", this.server.label));
            } else if (this.server.ping < 0L) {
                mutableText.append(NO_CONNECTION_TEXT);
            } else if (!this.pinged()) {
                mutableText.append(PINGING_TEXT);
            } else {
                mutableText.append(ONLINE_TEXT);
                mutableText.append(ScreenTexts.SENTENCE_SEPARATOR);
                mutableText.append(Text.translatable("multiplayer.status.ping.narration", this.server.ping));
                mutableText.append(ScreenTexts.SENTENCE_SEPARATOR);
                mutableText.append(Text.translatable("multiplayer.status.motd.narration", this.server.label));
                if (this.server.players != null) {
                    mutableText.append(ScreenTexts.SENTENCE_SEPARATOR);
                    mutableText.append(Text.translatable("multiplayer.status.player_count.narration", this.server.players.online(), this.server.players.max()));
                    mutableText.append(ScreenTexts.SENTENCE_SEPARATOR);
                    mutableText.append(Texts.join(this.server.playerListSummary, Text.literal(", ")));
                }
            }
            return mutableText;
        }

        @Override
        public void close() {
            this.icon.close();
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class LanServerEntry
    extends Entry {
        private static final int field_32386 = 32;
        private static final Text TITLE_TEXT = Text.translatable("lanServer.title");
        private static final Text HIDDEN_ADDRESS_TEXT = Text.translatable("selectServer.hiddenAddress");
        private final MultiplayerScreen screen;
        protected final MinecraftClient client;
        protected final LanServerInfo server;
        private long time;

        protected LanServerEntry(MultiplayerScreen screen, LanServerInfo server) {
            this.screen = screen;
            this.server = server;
            this.client = MinecraftClient.getInstance();
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            context.drawText(this.client.textRenderer, TITLE_TEXT, x + 32 + 3, y + 1, 0xFFFFFF, false);
            context.drawText(this.client.textRenderer, this.server.getMotd(), x + 32 + 3, y + 12, 0x808080, false);
            if (this.client.options.hideServerAddress) {
                context.drawText(this.client.textRenderer, HIDDEN_ADDRESS_TEXT, x + 32 + 3, y + 12 + 11, 0x303030, false);
            } else {
                context.drawText(this.client.textRenderer, this.server.getAddressPort(), x + 32 + 3, y + 12 + 11, 0x303030, false);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            this.screen.select(this);
            if (Util.getMeasuringTimeMs() - this.time < 250L) {
                this.screen.connect();
            }
            this.time = Util.getMeasuringTimeMs();
            return false;
        }

        public LanServerInfo getLanServerEntry() {
            return this.server;
        }

        @Override
        public Text getNarration() {
            return Text.translatable("narrator.select", this.getMotdNarration());
        }

        public Text getMotdNarration() {
            return Text.empty().append(TITLE_TEXT).append(ScreenTexts.SPACE).append(this.server.getMotd());
        }
    }
}

