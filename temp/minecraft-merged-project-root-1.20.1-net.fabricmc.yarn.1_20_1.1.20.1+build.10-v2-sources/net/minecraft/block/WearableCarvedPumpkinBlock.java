/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.CarvedPumpkinBlock;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Equipment;

public class WearableCarvedPumpkinBlock
extends CarvedPumpkinBlock
implements Equipment {
    public WearableCarvedPumpkinBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public EquipmentSlot getSlotType() {
        return EquipmentSlot.HEAD;
    }
}

