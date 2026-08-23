/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.client.font;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;

@Environment(value=EnvType.CLIENT)
public interface MultilineText {
    public static final MultilineText EMPTY = new MultilineText(){

        @Override
        public int drawCenterWithShadow(DrawContext context, int x, int y) {
            return y;
        }

        @Override
        public int drawCenterWithShadow(DrawContext context, int x, int y, int lineHeight, int color) {
            return y;
        }

        @Override
        public int drawWithShadow(DrawContext context, int x, int y, int lineHeight, int color) {
            return y;
        }

        @Override
        public int draw(DrawContext context, int x, int y, int lineHeight, int color) {
            return y;
        }

        @Override
        public void fillBackground(DrawContext context, int centerX, int centerY, int lineHeight, int padding, int color) {
        }

        @Override
        public int count() {
            return 0;
        }

        @Override
        public int getMaxWidth() {
            return 0;
        }
    };

    public static MultilineText create(TextRenderer renderer, StringVisitable text2, int width) {
        return MultilineText.create(renderer, renderer.wrapLines(text2, width).stream().map(text -> new Line((OrderedText)text, renderer.getWidth((OrderedText)text))).collect(ImmutableList.toImmutableList()));
    }

    public static MultilineText create(TextRenderer renderer, StringVisitable text2, int width, int maxLines) {
        return MultilineText.create(renderer, renderer.wrapLines(text2, width).stream().limit(maxLines).map(text -> new Line((OrderedText)text, renderer.getWidth((OrderedText)text))).collect(ImmutableList.toImmutableList()));
    }

    public static MultilineText create(TextRenderer renderer, Text ... texts) {
        return MultilineText.create(renderer, Arrays.stream(texts).map(Text::asOrderedText).map(text -> new Line((OrderedText)text, renderer.getWidth((OrderedText)text))).collect(ImmutableList.toImmutableList()));
    }

    public static MultilineText createFromTexts(TextRenderer renderer, List<Text> texts) {
        return MultilineText.create(renderer, texts.stream().map(Text::asOrderedText).map(text -> new Line((OrderedText)text, renderer.getWidth((OrderedText)text))).collect(ImmutableList.toImmutableList()));
    }

    public static MultilineText create(final TextRenderer textRenderer, final List<Line> lines) {
        if (lines.isEmpty()) {
            return EMPTY;
        }
        return new MultilineText(){
            private final int maxWidth;
            {
                this.maxWidth = lines.stream().mapToInt(line -> line.width).max().orElse(0);
            }

            @Override
            public int drawCenterWithShadow(DrawContext context, int x, int y) {
                return this.drawCenterWithShadow(context, x, y, textRenderer.fontHeight, 0xFFFFFF);
            }

            @Override
            public int drawCenterWithShadow(DrawContext context, int x, int y, int lineHeight, int color) {
                int i = y;
                for (Line line : lines) {
                    context.drawTextWithShadow(textRenderer, line.text, x - line.width / 2, i, color);
                    i += lineHeight;
                }
                return i;
            }

            @Override
            public int drawWithShadow(DrawContext context, int x, int y, int lineHeight, int color) {
                int i = y;
                for (Line line : lines) {
                    context.drawTextWithShadow(textRenderer, line.text, x, i, color);
                    i += lineHeight;
                }
                return i;
            }

            @Override
            public int draw(DrawContext context, int x, int y, int lineHeight, int color) {
                int i = y;
                for (Line line : lines) {
                    context.drawText(textRenderer, line.text, x, i, color, false);
                    i += lineHeight;
                }
                return i;
            }

            @Override
            public void fillBackground(DrawContext context, int centerX, int centerY, int lineHeight, int padding, int color) {
                int i = lines.stream().mapToInt(line -> line.width).max().orElse(0);
                if (i > 0) {
                    context.fill(centerX - i / 2 - padding, centerY - padding, centerX + i / 2 + padding, centerY + lines.size() * lineHeight + padding, color);
                }
            }

            @Override
            public int count() {
                return lines.size();
            }

            @Override
            public int getMaxWidth() {
                return this.maxWidth;
            }
        };
    }

    public int drawCenterWithShadow(DrawContext var1, int var2, int var3);

    public int drawCenterWithShadow(DrawContext var1, int var2, int var3, int var4, int var5);

    public int drawWithShadow(DrawContext var1, int var2, int var3, int var4, int var5);

    public int draw(DrawContext var1, int var2, int var3, int var4, int var5);

    public void fillBackground(DrawContext var1, int var2, int var3, int var4, int var5, int var6);

    public int count();

    public int getMaxWidth();

    @Environment(value=EnvType.CLIENT)
    public static class Line {
        final OrderedText text;
        final int width;

        Line(OrderedText text, int width) {
            this.text = text;
            this.width = width;
        }
    }
}

