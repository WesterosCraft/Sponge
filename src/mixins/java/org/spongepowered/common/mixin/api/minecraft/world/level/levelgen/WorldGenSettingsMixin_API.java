/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.mixin.api.minecraft.world.level.levelgen;

import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.spongepowered.api.world.generation.config.WorldGenerationConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Interface.Remap;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.util.SeedUtil;

import java.util.Objects;

@Mixin(WorldGenSettings.class)
@Implements(@Interface(iface = WorldGenerationConfig.class, prefix = "worldGenerationConfig$", remap = Remap.NONE))
public abstract class WorldGenSettingsMixin_API implements WorldGenerationConfig.Mutable {

    // @formatter:off
    @org.spongepowered.asm.mixin.Mutable @Shadow @Final private long seed;
    @org.spongepowered.asm.mixin.Mutable @Shadow @Final private boolean generateFeatures;
    @org.spongepowered.asm.mixin.Mutable @Shadow @Final private boolean generateBonusChest;

    @Shadow public abstract long shadow$seed();
    @Shadow public abstract boolean shadow$generateFeatures();
    @Shadow public abstract boolean shadow$generateBonusChest();
    // @formatter:on

    @Intrinsic
    public long worldGenerationConfig$seed() {
        return this.shadow$seed();
    }

    @Override
    public void setSeed(final long seed) {
        this.seed = seed;
    }

    @Override
    public void setSeed(final String seed) {
        this.seed = SeedUtil.compute(seed);
    }

    @Intrinsic
    public boolean worldGenerationConfig$generateFeatures() {
        return this.shadow$generateFeatures();
    }

    @Override
    public void setGenerateFeatures(final boolean generateFeatures) {
        this.generateFeatures = generateFeatures;
    }

    @Intrinsic
    public boolean worldGenerationConfig$generateBonusChest() {
        return this.shadow$generateBonusChest();
    }

    @Override
    public void setGenerateBonusChest(final boolean generateBonusChest) {
        this.generateBonusChest = generateBonusChest;
    }
}
