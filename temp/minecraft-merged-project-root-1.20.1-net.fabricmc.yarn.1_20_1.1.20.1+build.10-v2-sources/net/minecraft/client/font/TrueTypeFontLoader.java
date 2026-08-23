/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.client.font;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.Font;
import net.minecraft.client.font.FontLoader;
import net.minecraft.client.font.FontType;
import net.minecraft.client.font.TrueTypeFont;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Struct;

@Environment(value=EnvType.CLIENT)
public record TrueTypeFontLoader(Identifier location, float size, float oversample, Shift shift, String skip) implements FontLoader
{
    private static final Codec<String> SKIP_CODEC = Codec.either(Codec.STRING, Codec.STRING.listOf()).xmap(either -> either.map(string -> string, list -> String.join((CharSequence)"", list)), Either::left);
    public static final MapCodec<TrueTypeFontLoader> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(((MapCodec)Identifier.CODEC.fieldOf("file")).forGetter(TrueTypeFontLoader::location), Codec.FLOAT.optionalFieldOf("size", Float.valueOf(11.0f)).forGetter(TrueTypeFontLoader::size), Codec.FLOAT.optionalFieldOf("oversample", Float.valueOf(1.0f)).forGetter(TrueTypeFontLoader::oversample), Shift.CODEC.optionalFieldOf("shift", Shift.NONE).forGetter(TrueTypeFontLoader::shift), SKIP_CODEC.optionalFieldOf("skip", "").forGetter(TrueTypeFontLoader::skip)).apply((Applicative<TrueTypeFontLoader, ?>)instance, TrueTypeFontLoader::new));

    @Override
    public FontType getType() {
        return FontType.TTF;
    }

    @Override
    public Either<FontLoader.Loadable, FontLoader.Reference> build() {
        return Either.left(this::load);
    }

    private Font load(ResourceManager resourceManager) throws IOException {
        TrueTypeFont trueTypeFont;
        block10: {
            Struct sTBTTFontinfo = null;
            ByteBuffer byteBuffer = null;
            InputStream inputStream = resourceManager.open(this.location.withPrefixedPath("font/"));
            try {
                sTBTTFontinfo = STBTTFontinfo.malloc();
                byteBuffer = TextureUtil.readResource(inputStream);
                byteBuffer.flip();
                if (!STBTruetype.stbtt_InitFont((STBTTFontinfo)sTBTTFontinfo, byteBuffer)) {
                    throw new IOException("Invalid ttf");
                }
                trueTypeFont = new TrueTypeFont(byteBuffer, (STBTTFontinfo)sTBTTFontinfo, this.size, this.oversample, this.shift.x, this.shift.y, this.skip);
                if (inputStream == null) break block10;
            } catch (Throwable throwable) {
                try {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                } catch (Exception exception) {
                    if (sTBTTFontinfo != null) {
                        sTBTTFontinfo.free();
                    }
                    MemoryUtil.memFree(byteBuffer);
                    throw exception;
                }
            }
            inputStream.close();
        }
        return trueTypeFont;
    }

    @Environment(value=EnvType.CLIENT)
    public record Shift(float x, float y) {
        public static final Shift NONE = new Shift(0.0f, 0.0f);
        public static final Codec<Shift> CODEC = Codec.FLOAT.listOf().comapFlatMap(floatList2 -> Util.decodeFixedLengthList(floatList2, 2).map(floatList -> new Shift(((Float)floatList.get(0)).floatValue(), ((Float)floatList.get(1)).floatValue())), shift -> List.of(Float.valueOf(shift.x), Float.valueOf(shift.y)));
    }
}

