/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.util.path;

import java.nio.file.Path;

public record SymlinkEntry(Path link, Path target) {
}

