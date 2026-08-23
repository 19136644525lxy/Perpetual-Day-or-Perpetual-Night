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

public class ClearRuleBlockEntityModifier
implements RuleBlockEntityModifier {
    private static final ClearRuleBlockEntityModifier INSTANCE = new ClearRuleBlockEntityModifier();
    public static final Codec<ClearRuleBlockEntityModifier> CODEC = Codec.unit(INSTANCE);

    @Override
    public NbtCompound modifyBlockEntityNbt(Random random, @Nullable NbtCompound nbt) {
        return new NbtCompound();
    }

    @Override
    public RuleBlockEntityModifierType<?> getType() {
        return RuleBlockEntityModifierType.CLEAR;
    }
}

