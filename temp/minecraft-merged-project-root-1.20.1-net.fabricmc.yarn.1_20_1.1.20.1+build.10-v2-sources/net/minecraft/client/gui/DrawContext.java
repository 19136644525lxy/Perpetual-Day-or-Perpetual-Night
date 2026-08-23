/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.client.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.tooltip.TooltipBackgroundRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.client.item.TooltipData;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Divider;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector2ic;

@Environment(value=EnvType.CLIENT)
public class DrawContext {
    public static final float field_44931 = 10000.0f;
    public static final float field_44932 = -10000.0f;
    private static final int field_44655 = 2;
    private final MinecraftClient client;
    private final MatrixStack matrices;
    private final VertexConsumerProvider.Immediate vertexConsumers;
    private final ScissorStack scissorStack = new ScissorStack();
    private boolean runningDrawCallback;

    private DrawContext(MinecraftClient client, MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers) {
        this.client = client;
        this.matrices = matrices;
        this.vertexConsumers = vertexConsumers;
    }

    public DrawContext(MinecraftClient client, VertexConsumerProvider.Immediate vertexConsumers) {
        this(client, new MatrixStack(), vertexConsumers);
    }

    @Deprecated
    public void draw(Runnable drawCallback) {
        this.draw();
        this.runningDrawCallback = true;
        drawCallback.run();
        this.runningDrawCallback = false;
        this.draw();
    }

    @Deprecated
    private void tryDraw() {
        if (!this.runningDrawCallback) {
            this.draw();
        }
    }

    @Deprecated
    private void drawIfRunning() {
        if (this.runningDrawCallback) {
            this.draw();
        }
    }

    public int getScaledWindowWidth() {
        return this.client.getWindow().getScaledWidth();
    }

    public int getScaledWindowHeight() {
        return this.client.getWindow().getScaledHeight();
    }

    public MatrixStack getMatrices() {
        return this.matrices;
    }

    public VertexConsumerProvider.Immediate getVertexConsumers() {
        return this.vertexConsumers;
    }

    public void draw() {
        RenderSystem.disableDepthTest();
        this.vertexConsumers.draw();
        RenderSystem.enableDepthTest();
    }

    public void drawHorizontalLine(int x1, int x2, int y, int color) {
        this.drawHorizontalLine(RenderLayer.getGui(), x1, x2, y, color);
    }

    public void drawHorizontalLine(RenderLayer layer, int x1, int x2, int y, int color) {
        if (x2 < x1) {
            int i = x1;
            x1 = x2;
            x2 = i;
        }
        this.fill(layer, x1, y, x2 + 1, y + 1, color);
    }

    public void drawVerticalLine(int x, int y1, int y2, int color) {
        this.drawVerticalLine(RenderLayer.getGui(), x, y1, y2, color);
    }

    public void drawVerticalLine(RenderLayer layer, int x, int y1, int y2, int color) {
        if (y2 < y1) {
            int i = y1;
            y1 = y2;
            y2 = i;
        }
        this.fill(layer, x, y1 + 1, x + 1, y2, color);
    }

    public void enableScissor(int x1, int y1, int x2, int y2) {
        this.setScissor(this.scissorStack.push(new ScreenRect(x1, y1, x2 - x1, y2 - y1)));
    }

    public void disableScissor() {
        this.setScissor(this.scissorStack.pop());
    }

    private void setScissor(@Nullable ScreenRect rect) {
        this.drawIfRunning();
        if (rect != null) {
            Window window = MinecraftClient.getInstance().getWindow();
            int i = window.getFramebufferHeight();
            double d = window.getScaleFactor();
            double e = (double)rect.getLeft() * d;
            double f = (double)i - (double)rect.getBottom() * d;
            double g = (double)rect.width() * d;
            double h = (double)rect.height() * d;
            RenderSystem.enableScissor((int)e, (int)f, Math.max(0, (int)g), Math.max(0, (int)h));
        } else {
            RenderSystem.disableScissor();
        }
    }

    public void setShaderColor(float red, float green, float blue, float alpha) {
        this.drawIfRunning();
        RenderSystem.setShaderColor(red, green, blue, alpha);
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        this.fill(x1, y1, x2, y2, 0, color);
    }

    public void fill(int x1, int y1, int x2, int y2, int z, int color) {
        this.fill(RenderLayer.getGui(), x1, y1, x2, y2, z, color);
    }

    public void fill(RenderLayer layer, int x1, int y1, int x2, int y2, int color) {
        this.fill(layer, x1, y1, x2, y2, 0, color);
    }

    public void fill(RenderLayer layer, int x1, int y1, int x2, int y2, int z, int color) {
        int i;
        Matrix4f matrix4f = this.matrices.peek().getPositionMatrix();
        if (x1 < x2) {
            i = x1;
            x1 = x2;
            x2 = i;
        }
        if (y1 < y2) {
            i = y1;
            y1 = y2;
            y2 = i;
        }
        float f = (float)ColorHelper.Argb.getAlpha(color) / 255.0f;
        float g = (float)ColorHelper.Argb.getRed(color) / 255.0f;
        float h = (float)ColorHelper.Argb.getGreen(color) / 255.0f;
        float j = (float)ColorHelper.Argb.getBlue(color) / 255.0f;
        VertexConsumer vertexConsumer = this.vertexConsumers.getBuffer(layer);
        vertexConsumer.vertex(matrix4f, x1, y1, z).color(g, h, j, f).next();
        vertexConsumer.vertex(matrix4f, x1, y2, z).color(g, h, j, f).next();
        vertexConsumer.vertex(matrix4f, x2, y2, z).color(g, h, j, f).next();
        vertexConsumer.vertex(matrix4f, x2, y1, z).color(g, h, j, f).next();
        this.tryDraw();
    }

    public void fillGradient(int startX, int startY, int endX, int endY, int colorStart, int colorEnd) {
        this.fillGradient(startX, startY, endX, endY, 0, colorStart, colorEnd);
    }

    public void fillGradient(int startX, int startY, int endX, int endY, int z, int colorStart, int colorEnd) {
        this.fillGradient(RenderLayer.getGui(), startX, startY, endX, endY, colorStart, colorEnd, z);
    }

    public void fillGradient(RenderLayer layer, int startX, int startY, int endX, int endY, int colorStart, int colorEnd, int z) {
        VertexConsumer vertexConsumer = this.vertexConsumers.getBuffer(layer);
        this.fillGradient(vertexConsumer, startX, startY, endX, endY, z, colorStart, colorEnd);
        this.tryDraw();
    }

    private void fillGradient(VertexConsumer vertexConsumer, int startX, int startY, int endX, int endY, int z, int colorStart, int colorEnd) {
        float f = (float)ColorHelper.Argb.getAlpha(colorStart) / 255.0f;
        float g = (float)ColorHelper.Argb.getRed(colorStart) / 255.0f;
        float h = (float)ColorHelper.Argb.getGreen(colorStart) / 255.0f;
        float i = (float)ColorHelper.Argb.getBlue(colorStart) / 255.0f;
        float j = (float)ColorHelper.Argb.getAlpha(colorEnd) / 255.0f;
        float k = (float)ColorHelper.Argb.getRed(colorEnd) / 255.0f;
        float l = (float)ColorHelper.Argb.getGreen(colorEnd) / 255.0f;
        float m = (float)ColorHelper.Argb.getBlue(colorEnd) / 255.0f;
        Matrix4f matrix4f = this.matrices.peek().getPositionMatrix();
        vertexConsumer.vertex(matrix4f, startX, startY, z).color(g, h, i, f).next();
        vertexConsumer.vertex(matrix4f, startX, endY, z).color(k, l, m, j).next();
        vertexConsumer.vertex(matrix4f, endX, endY, z).color(k, l, m, j).next();
        vertexConsumer.vertex(matrix4f, endX, startY, z).color(g, h, i, f).next();
    }

    public void drawCenteredTextWithShadow(TextRenderer textRenderer, String text, int centerX, int y, int color) {
        this.drawTextWithShadow(textRenderer, text, centerX - textRenderer.getWidth(text) / 2, y, color);
    }

    public void drawCenteredTextWithShadow(TextRenderer textRenderer, Text text, int centerX, int y, int color) {
        OrderedText orderedText = text.asOrderedText();
        this.drawTextWithShadow(textRenderer, orderedText, centerX - textRenderer.getWidth(orderedText) / 2, y, color);
    }

    public void drawCenteredTextWithShadow(TextRenderer textRenderer, OrderedText text, int centerX, int y, int color) {
        this.drawTextWithShadow(textRenderer, text, centerX - textRenderer.getWidth(text) / 2, y, color);
    }

    public int drawTextWithShadow(TextRenderer textRenderer, @Nullable String text, int x, int y, int color) {
        return this.drawText(textRenderer, text, x, y, color, true);
    }

    public int drawText(TextRenderer textRenderer, @Nullable String text, int x, int y, int color, boolean shadow) {
        if (text == null) {
            return 0;
        }
        int i = textRenderer.draw(text, x, y, color, shadow, this.matrices.peek().getPositionMatrix(), this.vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0, textRenderer.isRightToLeft());
        this.tryDraw();
        return i;
    }

    public int drawTextWithShadow(TextRenderer textRenderer, OrderedText text, int x, int y, int color) {
        return this.drawText(textRenderer, text, x, y, color, true);
    }

    public int drawText(TextRenderer textRenderer, OrderedText text, int x, int y, int color, boolean shadow) {
        int i = textRenderer.draw(text, (float)x, (float)y, color, shadow, this.matrices.peek().getPositionMatrix(), (VertexConsumerProvider)this.vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
        this.tryDraw();
        return i;
    }

    public int drawTextWithShadow(TextRenderer textRenderer, Text text, int x, int y, int color) {
        return this.drawText(textRenderer, text, x, y, color, true);
    }

    public int drawText(TextRenderer textRenderer, Text text, int x, int y, int color, boolean shadow) {
        return this.drawText(textRenderer, text.asOrderedText(), x, y, color, shadow);
    }

    public void drawTextWrapped(TextRenderer textRenderer, StringVisitable text, int x, int y, int width, int color) {
        for (OrderedText orderedText : textRenderer.wrapLines(text, width)) {
            this.drawText(textRenderer, orderedText, x, y, color, false);
            y += textRenderer.fontHeight;
        }
    }

    public void drawSprite(int x, int y, int z, int width, int height, Sprite sprite) {
        this.drawTexturedQuad(sprite.getAtlasId(), x, x + width, y, y + height, z, sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV());
    }

    public void drawSprite(int x, int y, int z, int width, int height, Sprite sprite, float red, float green, float blue, float alpha) {
        this.drawTexturedQuad(sprite.getAtlasId(), x, x + width, y, y + height, z, sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV(), red, green, blue, alpha);
    }

    public void drawBorder(int x, int y, int width, int height, int color) {
        this.fill(x, y, x + width, y + 1, color);
        this.fill(x, y + height - 1, x + width, y + height, color);
        this.fill(x, y + 1, x + 1, y + height - 1, color);
        this.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    /**
     * Draws a textured rectangle from a region in a 256x256 texture.
     * 
     * <p>The Z coordinate of the rectangle is {@code 0}.
     * 
     * <p>The width and height of the region are the same as
     * the dimensions of the rectangle.
     */
    public void drawTexture(Identifier texture, int x, int y, int u, int v, int width, int height) {
        this.drawTexture(texture, x, y, 0, u, v, width, height, 256, 256);
    }

    /**
     * Draws a textured rectangle from a region in a texture.
     * 
     * <p>The width and height of the region are the same as
     * the dimensions of the rectangle.
     */
    public void drawTexture(Identifier texture, int x, int y, int z, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        this.drawTexture(texture, x, x + width, y, y + height, z, width, height, u, v, textureWidth, textureHeight);
    }

    /**
     * Draws a textured rectangle from a region in a texture.
     */
    public void drawTexture(Identifier texture, int x, int y, int width, int height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        this.drawTexture(texture, x, x + width, y, y + height, 0, regionWidth, regionHeight, u, v, textureWidth, textureHeight);
    }

    /**
     * Draws a textured rectangle from a region in a texture.
     * 
     * <p>The width and height of the region are the same as
     * the dimensions of the rectangle.
     */
    public void drawTexture(Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        this.drawTexture(texture, x, y, width, height, u, v, width, height, textureWidth, textureHeight);
    }

    void drawTexture(Identifier texture, int x1, int x2, int y1, int y2, int z, int regionWidth, int regionHeight, float u, float v, int textureWidth, int textureHeight) {
        this.drawTexturedQuad(texture, x1, x2, y1, y2, z, (u + 0.0f) / (float)textureWidth, (u + (float)regionWidth) / (float)textureWidth, (v + 0.0f) / (float)textureHeight, (v + (float)regionHeight) / (float)textureHeight);
    }

    void drawTexturedQuad(Identifier texture, int x1, int x2, int y1, int y2, int z, float u1, float u2, float v1, float v2) {
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        Matrix4f matrix4f = this.matrices.peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex(matrix4f, x1, y1, z).texture(u1, v1).next();
        bufferBuilder.vertex(matrix4f, x1, y2, z).texture(u1, v2).next();
        bufferBuilder.vertex(matrix4f, x2, y2, z).texture(u2, v2).next();
        bufferBuilder.vertex(matrix4f, x2, y1, z).texture(u2, v1).next();
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    void drawTexturedQuad(Identifier texture, int x1, int x2, int y1, int y2, int z, float u1, float u2, float v1, float v2, float red, float green, float blue, float alpha) {
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionColorTexProgram);
        RenderSystem.enableBlend();
        Matrix4f matrix4f = this.matrices.peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
        bufferBuilder.vertex(matrix4f, x1, y1, z).color(red, green, blue, alpha).texture(u1, v1).next();
        bufferBuilder.vertex(matrix4f, x1, y2, z).color(red, green, blue, alpha).texture(u1, v2).next();
        bufferBuilder.vertex(matrix4f, x2, y2, z).color(red, green, blue, alpha).texture(u2, v2).next();
        bufferBuilder.vertex(matrix4f, x2, y1, z).color(red, green, blue, alpha).texture(u2, v1).next();
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    public void drawNineSlicedTexture(Identifier texture, int x, int y, int width, int height, int outerSliceSize, int centerSliceWidth, int centerSliceHeight, int u, int v) {
        this.drawNineSlicedTexture(texture, x, y, width, height, outerSliceSize, outerSliceSize, outerSliceSize, outerSliceSize, centerSliceWidth, centerSliceHeight, u, v);
    }

    public void drawNineSlicedTexture(Identifier texture, int x, int y, int width, int height, int outerSliceWidth, int outerSliceHeight, int centerSliceWidth, int centerSliceHeight, int u, int v) {
        this.drawNineSlicedTexture(texture, x, y, width, height, outerSliceWidth, outerSliceHeight, outerSliceWidth, outerSliceHeight, centerSliceWidth, centerSliceHeight, u, v);
    }

    public void drawNineSlicedTexture(Identifier texture, int x, int y, int width, int height, int leftSliceWidth, int topSliceHeight, int rightSliceWidth, int bottomSliceHeight, int centerSliceWidth, int centerSliceHeight, int u, int v) {
        leftSliceWidth = Math.min(leftSliceWidth, width / 2);
        rightSliceWidth = Math.min(rightSliceWidth, width / 2);
        topSliceHeight = Math.min(topSliceHeight, height / 2);
        bottomSliceHeight = Math.min(bottomSliceHeight, height / 2);
        if (width == centerSliceWidth && height == centerSliceHeight) {
            this.drawTexture(texture, x, y, u, v, width, height);
            return;
        }
        if (height == centerSliceHeight) {
            this.drawTexture(texture, x, y, u, v, leftSliceWidth, height);
            this.drawRepeatingTexture(texture, x + leftSliceWidth, y, width - rightSliceWidth - leftSliceWidth, height, u + leftSliceWidth, v, centerSliceWidth - rightSliceWidth - leftSliceWidth, centerSliceHeight);
            this.drawTexture(texture, x + width - rightSliceWidth, y, u + centerSliceWidth - rightSliceWidth, v, rightSliceWidth, height);
            return;
        }
        if (width == centerSliceWidth) {
            this.drawTexture(texture, x, y, u, v, width, topSliceHeight);
            this.drawRepeatingTexture(texture, x, y + topSliceHeight, width, height - bottomSliceHeight - topSliceHeight, u, v + topSliceHeight, centerSliceWidth, centerSliceHeight - bottomSliceHeight - topSliceHeight);
            this.drawTexture(texture, x, y + height - bottomSliceHeight, u, v + centerSliceHeight - bottomSliceHeight, width, bottomSliceHeight);
            return;
        }
        this.drawTexture(texture, x, y, u, v, leftSliceWidth, topSliceHeight);
        this.drawRepeatingTexture(texture, x + leftSliceWidth, y, width - rightSliceWidth - leftSliceWidth, topSliceHeight, u + leftSliceWidth, v, centerSliceWidth - rightSliceWidth - leftSliceWidth, topSliceHeight);
        this.drawTexture(texture, x + width - rightSliceWidth, y, u + centerSliceWidth - rightSliceWidth, v, rightSliceWidth, topSliceHeight);
        this.drawTexture(texture, x, y + height - bottomSliceHeight, u, v + centerSliceHeight - bottomSliceHeight, leftSliceWidth, bottomSliceHeight);
        this.drawRepeatingTexture(texture, x + leftSliceWidth, y + height - bottomSliceHeight, width - rightSliceWidth - leftSliceWidth, bottomSliceHeight, u + leftSliceWidth, v + centerSliceHeight - bottomSliceHeight, centerSliceWidth - rightSliceWidth - leftSliceWidth, bottomSliceHeight);
        this.drawTexture(texture, x + width - rightSliceWidth, y + height - bottomSliceHeight, u + centerSliceWidth - rightSliceWidth, v + centerSliceHeight - bottomSliceHeight, rightSliceWidth, bottomSliceHeight);
        this.drawRepeatingTexture(texture, x, y + topSliceHeight, leftSliceWidth, height - bottomSliceHeight - topSliceHeight, u, v + topSliceHeight, leftSliceWidth, centerSliceHeight - bottomSliceHeight - topSliceHeight);
        this.drawRepeatingTexture(texture, x + leftSliceWidth, y + topSliceHeight, width - rightSliceWidth - leftSliceWidth, height - bottomSliceHeight - topSliceHeight, u + leftSliceWidth, v + topSliceHeight, centerSliceWidth - rightSliceWidth - leftSliceWidth, centerSliceHeight - bottomSliceHeight - topSliceHeight);
        this.drawRepeatingTexture(texture, x + width - rightSliceWidth, y + topSliceHeight, leftSliceWidth, height - bottomSliceHeight - topSliceHeight, u + centerSliceWidth - rightSliceWidth, v + topSliceHeight, rightSliceWidth, centerSliceHeight - bottomSliceHeight - topSliceHeight);
    }

    public void drawRepeatingTexture(Identifier texture, int x, int y, int width, int height, int u, int v, int textureWidth, int textureHeight) {
        int i = x;
        IntIterator intIterator = DrawContext.createDivider(width, textureWidth);
        while (intIterator.hasNext()) {
            int j = intIterator.nextInt();
            int k = (textureWidth - j) / 2;
            int l = y;
            IntIterator intIterator2 = DrawContext.createDivider(height, textureHeight);
            while (intIterator2.hasNext()) {
                int m = intIterator2.nextInt();
                int n = (textureHeight - m) / 2;
                this.drawTexture(texture, i, l, u + k, v + n, j, m);
                l += m;
            }
            i += j;
        }
    }

    private static IntIterator createDivider(int sideLength, int textureSideLength) {
        int i = MathHelper.ceilDiv(sideLength, textureSideLength);
        return new Divider(sideLength, i);
    }

    public void drawItem(ItemStack item, int x, int y) {
        this.drawItem(this.client.player, this.client.world, item, x, y, 0);
    }

    public void drawItem(ItemStack stack, int x, int y, int seed) {
        this.drawItem(this.client.player, this.client.world, stack, x, y, seed);
    }

    public void drawItem(ItemStack stack, int x, int y, int seed, int z) {
        this.drawItem(this.client.player, this.client.world, stack, x, y, seed, z);
    }

    public void drawItemWithoutEntity(ItemStack stack, int x, int y) {
        this.drawItem(null, this.client.world, stack, x, y, 0);
    }

    public void drawItem(LivingEntity entity, ItemStack stack, int x, int y, int seed) {
        this.drawItem(entity, entity.getWorld(), stack, x, y, seed);
    }

    private void drawItem(@Nullable LivingEntity entity, @Nullable World world, ItemStack stack, int x, int y, int seed) {
        this.drawItem(entity, world, stack, x, y, seed, 0);
    }

    private void drawItem(@Nullable LivingEntity entity, @Nullable World world, ItemStack stack, int x, int y, int seed, int z) {
        if (stack.isEmpty()) {
            return;
        }
        BakedModel bakedModel = this.client.getItemRenderer().getModel(stack, world, entity, seed);
        this.matrices.push();
        this.matrices.translate(x + 8, y + 8, 150 + (bakedModel.hasDepth() ? z : 0));
        try {
            boolean bl;
            this.matrices.multiplyPositionMatrix(new Matrix4f().scaling(1.0f, -1.0f, 1.0f));
            this.matrices.scale(16.0f, 16.0f, 16.0f);
            boolean bl2 = bl = !bakedModel.isSideLit();
            if (bl) {
                DiffuseLighting.disableGuiDepthLighting();
            }
            this.client.getItemRenderer().renderItem(stack, ModelTransformationMode.GUI, false, this.matrices, this.getVertexConsumers(), 0xF000F0, OverlayTexture.DEFAULT_UV, bakedModel);
            this.draw();
            if (bl) {
                DiffuseLighting.enableGuiDepthLighting();
            }
        } catch (Throwable throwable) {
            CrashReport crashReport = CrashReport.create(throwable, "Rendering item");
            CrashReportSection crashReportSection = crashReport.addElement("Item being rendered");
            crashReportSection.add("Item Type", () -> String.valueOf(stack.getItem()));
            crashReportSection.add("Item Damage", () -> String.valueOf(stack.getDamage()));
            crashReportSection.add("Item NBT", () -> String.valueOf(stack.getNbt()));
            crashReportSection.add("Item Foil", () -> String.valueOf(stack.hasGlint()));
            throw new CrashException(crashReport);
        }
        this.matrices.pop();
    }

    public void drawItemInSlot(TextRenderer textRenderer, ItemStack stack, int x, int y) {
        this.drawItemInSlot(textRenderer, stack, x, y, null);
    }

    public void drawItemInSlot(TextRenderer textRenderer, ItemStack stack, int x, int y, @Nullable String countOverride) {
        ClientPlayerEntity clientPlayerEntity;
        float f;
        int l;
        int k;
        if (stack.isEmpty()) {
            return;
        }
        this.matrices.push();
        if (stack.getCount() != 1 || countOverride != null) {
            String string = countOverride == null ? String.valueOf(stack.getCount()) : countOverride;
            this.matrices.translate(0.0f, 0.0f, 200.0f);
            this.drawText(textRenderer, string, x + 19 - 2 - textRenderer.getWidth(string), y + 6 + 3, 0xFFFFFF, true);
        }
        if (stack.isItemBarVisible()) {
            int i = stack.getItemBarStep();
            int j = stack.getItemBarColor();
            k = x + 2;
            l = y + 13;
            this.fill(RenderLayer.getGuiOverlay(), k, l, k + 13, l + 2, -16777216);
            this.fill(RenderLayer.getGuiOverlay(), k, l, k + i, l + 1, j | 0xFF000000);
        }
        float f2 = f = (clientPlayerEntity = this.client.player) == null ? 0.0f : clientPlayerEntity.getItemCooldownManager().getCooldownProgress(stack.getItem(), this.client.getTickDelta());
        if (f > 0.0f) {
            k = y + MathHelper.floor(16.0f * (1.0f - f));
            l = k + MathHelper.ceil(16.0f * f);
            this.fill(RenderLayer.getGuiOverlay(), x, k, x + 16, l, Integer.MAX_VALUE);
        }
        this.matrices.pop();
    }

    public void drawItemTooltip(TextRenderer textRenderer, ItemStack stack, int x, int y) {
        this.drawTooltip(textRenderer, Screen.getTooltipFromItem(this.client, stack), stack.getTooltipData(), x, y);
    }

    public void drawTooltip(TextRenderer textRenderer, List<Text> text, Optional<TooltipData> data2, int x, int y) {
        List<TooltipComponent> list = text.stream().map(Text::asOrderedText).map(TooltipComponent::of).collect(Collectors.toList());
        data2.ifPresent(data -> list.add(1, TooltipComponent.of(data)));
        this.drawTooltip(textRenderer, list, x, y, HoveredTooltipPositioner.INSTANCE);
    }

    public void drawTooltip(TextRenderer textRenderer, Text text, int x, int y) {
        this.drawOrderedTooltip(textRenderer, List.of(text.asOrderedText()), x, y);
    }

    public void drawTooltip(TextRenderer textRenderer, List<Text> text, int x, int y) {
        this.drawOrderedTooltip(textRenderer, Lists.transform(text, Text::asOrderedText), x, y);
    }

    public void drawOrderedTooltip(TextRenderer textRenderer, List<? extends OrderedText> text, int x, int y) {
        this.drawTooltip(textRenderer, text.stream().map(TooltipComponent::of).collect(Collectors.toList()), x, y, HoveredTooltipPositioner.INSTANCE);
    }

    public void drawTooltip(TextRenderer textRenderer, List<OrderedText> text, TooltipPositioner positioner, int x, int y) {
        this.drawTooltip(textRenderer, text.stream().map(TooltipComponent::of).collect(Collectors.toList()), x, y, positioner);
    }

    private void drawTooltip(TextRenderer textRenderer, List<TooltipComponent> components, int x, int y, TooltipPositioner positioner) {
        TooltipComponent tooltipComponent2;
        int r;
        if (components.isEmpty()) {
            return;
        }
        int i = 0;
        int j = components.size() == 1 ? -2 : 0;
        for (TooltipComponent tooltipComponent : components) {
            int k = tooltipComponent.getWidth(textRenderer);
            if (k > i) {
                i = k;
            }
            j += tooltipComponent.getHeight();
        }
        int l = i;
        int m = j;
        Vector2ic vector2ic = positioner.getPosition(this.getScaledWindowWidth(), this.getScaledWindowHeight(), x, y, l, m);
        int n = vector2ic.x();
        int o = vector2ic.y();
        this.matrices.push();
        int p = 400;
        this.draw(() -> TooltipBackgroundRenderer.render(this, n, o, l, m, 400));
        this.matrices.translate(0.0f, 0.0f, 400.0f);
        int q = o;
        for (r = 0; r < components.size(); ++r) {
            tooltipComponent2 = components.get(r);
            tooltipComponent2.drawText(textRenderer, n, q, this.matrices.peek().getPositionMatrix(), this.vertexConsumers);
            q += tooltipComponent2.getHeight() + (r == 0 ? 2 : 0);
        }
        q = o;
        for (r = 0; r < components.size(); ++r) {
            tooltipComponent2 = components.get(r);
            tooltipComponent2.drawItems(textRenderer, n, q, this);
            q += tooltipComponent2.getHeight() + (r == 0 ? 2 : 0);
        }
        this.matrices.pop();
    }

    public void drawHoverEvent(TextRenderer textRenderer, @Nullable Style style, int x, int y) {
        if (style == null || style.getHoverEvent() == null) {
            return;
        }
        HoverEvent hoverEvent = style.getHoverEvent();
        HoverEvent.ItemStackContent itemStackContent = hoverEvent.getValue(HoverEvent.Action.SHOW_ITEM);
        if (itemStackContent != null) {
            this.drawItemTooltip(textRenderer, itemStackContent.asStack(), x, y);
        } else {
            HoverEvent.EntityContent entityContent = hoverEvent.getValue(HoverEvent.Action.SHOW_ENTITY);
            if (entityContent != null) {
                if (this.client.options.advancedItemTooltips) {
                    this.drawTooltip(textRenderer, entityContent.asTooltip(), x, y);
                }
            } else {
                Text text = hoverEvent.getValue(HoverEvent.Action.SHOW_TEXT);
                if (text != null) {
                    this.drawOrderedTooltip(textRenderer, textRenderer.wrapLines(text, Math.max(this.getScaledWindowWidth() / 2, 200)), x, y);
                }
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    static class ScissorStack {
        private final Deque<ScreenRect> stack = new ArrayDeque<ScreenRect>();

        ScissorStack() {
        }

        public ScreenRect push(ScreenRect rect) {
            ScreenRect screenRect = this.stack.peekLast();
            if (screenRect != null) {
                ScreenRect screenRect2 = Objects.requireNonNullElse(rect.intersection(screenRect), ScreenRect.empty());
                this.stack.addLast(screenRect2);
                return screenRect2;
            }
            this.stack.addLast(rect);
            return rect;
        }

        @Nullable
        public ScreenRect pop() {
            if (this.stack.isEmpty()) {
                throw new IllegalStateException("Scissor stack underflow");
            }
            this.stack.removeLast();
            return this.stack.peekLast();
        }
    }
}

