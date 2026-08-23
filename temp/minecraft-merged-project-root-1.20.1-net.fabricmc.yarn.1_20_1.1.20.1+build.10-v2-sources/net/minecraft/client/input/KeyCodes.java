/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.client.input;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class KeyCodes {
    public static boolean isToggle(int keyCode) {
        return keyCode == 257 || keyCode == 32 || keyCode == 335;
    }
}

