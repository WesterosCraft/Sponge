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
package org.spongepowered.vanilla.server.packs;

import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import org.apache.commons.io.FilenameUtils;
import org.spongepowered.common.launch.Launch;
import org.spongepowered.plugin.PluginResource;
import org.spongepowered.vanilla.bridge.server.packs.repository.PackRepositoryBridge_Vanilla;
import org.spongepowered.vanilla.launch.plugin.VanillaPluginManager;

import java.util.function.Consumer;

public final class PluginRepositorySource implements RepositorySource {

    private final PackRepository repository;

    public PluginRepositorySource(final PackRepository repository) {
        this.repository = repository;
    }

    @Override
    public void loadPacks(final Consumer<Pack> callback, final Pack.PackConstructor constructor) {
        final VanillaPluginManager pluginManager = (VanillaPluginManager) Launch.instance().pluginManager();
        pluginManager.locatedResources().forEach((id, resources) -> {
            for (final PluginResource resource : resources) {
                final String filename = FilenameUtils.getBaseName(resource.path().getFileName().toString());
                final String extension = FilenameUtils.getExtension(resource.path().getFileName().toString());
                // Only jars for now
                if ("jar".equals(extension)) {
                    final PluginPackResources packResources = new PluginPackResources(filename, resource);
                    final String packId = "mod:" + filename;
                    final Pack pack = Pack.create(packId, true, () -> packResources, constructor, Pack.Position.BOTTOM, PackSource.DEFAULT);
                    ((PackRepositoryBridge_Vanilla) this.repository).bridge$registerResourcePack(resource, pack);
                    callback.accept(pack);
                }
            }
        });
    }
}
