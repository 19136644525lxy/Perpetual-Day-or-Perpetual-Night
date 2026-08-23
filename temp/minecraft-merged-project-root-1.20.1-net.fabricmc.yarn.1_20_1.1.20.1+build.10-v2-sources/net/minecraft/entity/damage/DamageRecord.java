/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.entity.damage;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.FallLocation;
import org.jetbrains.annotations.Nullable;

public record DamageRecord(DamageSource damageSource, float damage, @Nullable FallLocation fallLocation, float fallDistance) {
    @Nullable
    public FallLocation fallLocation() {
        return this.fallLocation;
    }
}

