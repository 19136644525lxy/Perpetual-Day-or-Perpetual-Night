/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.client.font;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.io.IOException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.Font;
import net.minecraft.client.font.FontType;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

@Environment(value=EnvType.CLIENT)
public interface FontLoader {
    public static final Codec<FontLoader> CODEC = FontType.CODEC.dispatch(FontLoader::getType, fontType -> fontType.getLoaderCodec().codec());

    public FontType getType();

    public Either<Loadable, Reference> build();

    @Environment(value=EnvType.CLIENT)
    public record Reference(Identifier id) {
    }

    @Environment(value=EnvType.CLIENT)
    public static interface Loadable {
        public Font load(ResourceManager var1) throws IOException;
    }
}

