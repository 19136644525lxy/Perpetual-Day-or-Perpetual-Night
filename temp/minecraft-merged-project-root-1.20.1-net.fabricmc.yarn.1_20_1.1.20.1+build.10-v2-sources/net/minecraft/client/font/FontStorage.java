/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.client.font;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.HashSet;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.BuiltinEmptyGlyph;
import net.minecraft.client.font.Font;
import net.minecraft.client.font.Glyph;
import net.minecraft.client.font.GlyphAtlasTexture;
import net.minecraft.client.font.GlyphContainer;
import net.minecraft.client.font.GlyphRenderer;
import net.minecraft.client.font.RenderableGlyph;
import net.minecraft.client.font.TextRenderLayerSet;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

@Environment(value=EnvType.CLIENT)
public class FontStorage
implements AutoCloseable {
    private static final Random RANDOM = Random.create();
    private static final float MAX_ADVANCE = 32.0f;
    private final TextureManager textureManager;
    private final Identifier id;
    private GlyphRenderer blankGlyphRenderer;
    private GlyphRenderer whiteRectangleGlyphRenderer;
    private final List<Font> fonts = Lists.newArrayList();
    private final GlyphContainer<GlyphRenderer> glyphRendererCache = new GlyphContainer(GlyphRenderer[]::new, rowCount -> new GlyphRenderer[rowCount][]);
    private final GlyphContainer<GlyphPair> glyphCache = new GlyphContainer(GlyphPair[]::new, rowCount -> new GlyphPair[rowCount][]);
    private final Int2ObjectMap<IntList> charactersByWidth = new Int2ObjectOpenHashMap<IntList>();
    private final List<GlyphAtlasTexture> glyphAtlases = Lists.newArrayList();

    public FontStorage(TextureManager textureManager, Identifier id) {
        this.textureManager = textureManager;
        this.id = id;
    }

    public void setFonts(List<Font> fonts) {
        this.closeFonts();
        this.closeGlyphAtlases();
        this.glyphRendererCache.clear();
        this.glyphCache.clear();
        this.charactersByWidth.clear();
        this.blankGlyphRenderer = BuiltinEmptyGlyph.MISSING.bake(this::getGlyphRenderer);
        this.whiteRectangleGlyphRenderer = BuiltinEmptyGlyph.WHITE.bake(this::getGlyphRenderer);
        IntOpenHashSet intSet = new IntOpenHashSet();
        for (Font font : fonts) {
            intSet.addAll(font.getProvidedGlyphs());
        }
        HashSet set = Sets.newHashSet();
        intSet.forEach(codePoint -> {
            for (Font font : fonts) {
                Glyph glyph = font.getGlyph(codePoint);
                if (glyph == null) continue;
                set.add(font);
                if (glyph == BuiltinEmptyGlyph.MISSING) break;
                this.charactersByWidth.computeIfAbsent(MathHelper.ceil(glyph.getAdvance(false)), advance -> new IntArrayList()).add(codePoint);
                break;
            }
        });
        fonts.stream().filter(set::contains).forEach(this.fonts::add);
    }

    @Override
    public void close() {
        this.closeFonts();
        this.closeGlyphAtlases();
    }

    private void closeFonts() {
        for (Font font : this.fonts) {
            font.close();
        }
        this.fonts.clear();
    }

    private void closeGlyphAtlases() {
        for (GlyphAtlasTexture glyphAtlasTexture : this.glyphAtlases) {
            glyphAtlasTexture.close();
        }
        this.glyphAtlases.clear();
    }

    private static boolean isAdvanceInvalid(Glyph glyph) {
        float f = glyph.getAdvance(false);
        if (f < 0.0f || f > 32.0f) {
            return true;
        }
        float g = glyph.getAdvance(true);
        return g < 0.0f || g > 32.0f;
    }

    /**
     * {@return the glyph of {@code codePoint}}
     * 
     * @apiNote Call {@link #getGlyph} instead, as that method provides caching.
     */
    private GlyphPair findGlyph(int codePoint) {
        Glyph glyph = null;
        for (Font font : this.fonts) {
            Glyph glyph2 = font.getGlyph(codePoint);
            if (glyph2 == null) continue;
            if (glyph == null) {
                glyph = glyph2;
            }
            if (FontStorage.isAdvanceInvalid(glyph2)) continue;
            return new GlyphPair(glyph, glyph2);
        }
        if (glyph != null) {
            return new GlyphPair(glyph, BuiltinEmptyGlyph.MISSING);
        }
        return GlyphPair.MISSING;
    }

    /**
     * {@return the glyph of {@code codePoint}}
     * 
     * @implNote {@link BuiltinEmptyGlyph#MISSING} is returned for missing code points.
     */
    public Glyph getGlyph(int codePoint, boolean validateAdvance) {
        return this.glyphCache.computeIfAbsent(codePoint, this::findGlyph).getGlyph(validateAdvance);
    }

    private GlyphRenderer findGlyphRenderer(int codePoint) {
        for (Font font : this.fonts) {
            Glyph glyph = font.getGlyph(codePoint);
            if (glyph == null) continue;
            return glyph.bake(this::getGlyphRenderer);
        }
        return this.blankGlyphRenderer;
    }

    public GlyphRenderer getGlyphRenderer(int codePoint) {
        return this.glyphRendererCache.computeIfAbsent(codePoint, this::findGlyphRenderer);
    }

    private GlyphRenderer getGlyphRenderer(RenderableGlyph c) {
        for (GlyphAtlasTexture glyphAtlasTexture : this.glyphAtlases) {
            GlyphRenderer glyphRenderer = glyphAtlasTexture.getGlyphRenderer(c);
            if (glyphRenderer == null) continue;
            return glyphRenderer;
        }
        Identifier identifier = this.id.withSuffixedPath("/" + this.glyphAtlases.size());
        boolean bl = c.hasColor();
        TextRenderLayerSet textRenderLayerSet = bl ? TextRenderLayerSet.of(identifier) : TextRenderLayerSet.ofIntensity(identifier);
        GlyphAtlasTexture glyphAtlasTexture2 = new GlyphAtlasTexture(textRenderLayerSet, bl);
        this.glyphAtlases.add(glyphAtlasTexture2);
        this.textureManager.registerTexture(identifier, glyphAtlasTexture2);
        GlyphRenderer glyphRenderer2 = glyphAtlasTexture2.getGlyphRenderer(c);
        return glyphRenderer2 == null ? this.blankGlyphRenderer : glyphRenderer2;
    }

    public GlyphRenderer getObfuscatedGlyphRenderer(Glyph glyph) {
        IntList intList = (IntList)this.charactersByWidth.get(MathHelper.ceil(glyph.getAdvance(false)));
        if (intList != null && !intList.isEmpty()) {
            return this.getGlyphRenderer(intList.getInt(RANDOM.nextInt(intList.size())));
        }
        return this.blankGlyphRenderer;
    }

    public GlyphRenderer getRectangleRenderer() {
        return this.whiteRectangleGlyphRenderer;
    }

    @Environment(value=EnvType.CLIENT)
    record GlyphPair(Glyph glyph, Glyph advanceValidatedGlyph) {
        static final GlyphPair MISSING = new GlyphPair(BuiltinEmptyGlyph.MISSING, BuiltinEmptyGlyph.MISSING);

        Glyph getGlyph(boolean validateAdvance) {
            return validateAdvance ? this.advanceValidatedGlyph : this.glyph;
        }
    }
}

