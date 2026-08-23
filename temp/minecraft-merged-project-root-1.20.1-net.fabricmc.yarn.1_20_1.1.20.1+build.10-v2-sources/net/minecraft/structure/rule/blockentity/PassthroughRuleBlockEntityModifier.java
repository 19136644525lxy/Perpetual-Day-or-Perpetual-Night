/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.structure.rule.blockentity;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.rule.blockentity.RuleBlockEntityModifier;
import net.minecraft.structure.rule.blockentity.RuleBlockEntityModifierType;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

public class PassthroughRuleBlockEntityModifier
implements RuleBlockEntityModifier {
    public static final PassthroughRuleBlockEntityModifier INSTANCE = new PassthroughRuleBlockEntityModifier();
    public static final Codec<PassthroughRuleBlockEntityModifier> CODEC = Codec.unit(INSTANCE);

    @Override
    @Nullable
    public NbtCompound modifyBlockEntityNbt(Random random, @Nullable NbtCompound nbt) {
        return nbt;
    }

    @Override
    public RuleBlockEntityModifierType<?> getType() {
        return RuleBlockEntityModifierType.PASSTHROUGH;
    }
}

