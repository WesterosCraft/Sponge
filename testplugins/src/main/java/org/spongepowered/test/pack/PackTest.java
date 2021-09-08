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
package org.spongepowered.test.pack;

import com.google.inject.Inject;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.Engine;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.resource.Resource;
import org.spongepowered.api.resource.ResourcePath;
import org.spongepowered.api.resource.pack.Pack;
import org.spongepowered.api.resource.pack.PackRoot;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

@Plugin("packtest")
public final class PackTest {

    private final PluginContainer plugin;

    @Inject
    public PackTest(final PluginContainer plugin) {
        this.plugin = plugin;
    }

    @Listener
    public void onStartedEngine(final StartedEngineEvent<@NonNull Engine> event) {
        this.plugin.logger().warn("Printing packs for engine: {}", event.engine().toString());
        for (final Pack pack : event.engine().packRepository().all()) {
            this.plugin.logger().error(pack.id());
        }

        final Pack pack = event.engine().packRepository().pack(this.plugin);
        try (final Resource resource = pack.contents().resource(PackRoot.assets(), ResourcePath.of(this.plugin, "test.txt"))) {
            System.err.println(resource);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
