/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.structure.rule.blockentity;

import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.rule.blockentity.RuleBlockEntityModifier;
import net.minecraft.structure.rule.blockentity.RuleBlockEntityModifierType;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

public class AppendStaticRuleBlockEntityModifier
implements RuleBlockEntityModifier {
    public static final Codec<AppendStaticRuleBlockEntityModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(((MapCodec)NbtCompound.CODEC.fieldOf("data")).forGetter(modifier -> modifier.nbt)).apply((Applicative<AppendStaticRuleBlockEntityModifier, ?>)instance, AppendStaticRuleBlockEntityModifier::new));
    private final NbtCompound nbt;

    public AppendStaticRuleBlockEntityModifier(NbtCompound nbt) {
        this.nbt = nbt;
    }

    @Override
    public NbtCompound modifyBlockEntityNbt(Random random, @Nullable NbtCompound nbt) {
        return nbt == null ? this.nbt.copy() : nbt.copyFrom(this.nbt);
    }

    @Override
    public RuleBlockEntityModifierType<?> getType() {
        return RuleBlockEntityModifierType.APPEND_STATIC;
    }
}

