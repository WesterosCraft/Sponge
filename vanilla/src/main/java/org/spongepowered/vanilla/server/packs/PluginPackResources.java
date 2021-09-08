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

import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.ResourcePackFileNotFoundException;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.plugin.PluginResource;
import org.spongepowered.plugin.builtin.jvm.locator.JVMPluginResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class PluginPackResources extends AbstractPackResources {

    private final String name;
    private final PluginResource resource;
    private final PackMetadataSection metadata;
    private FileSystem fileSystem;

    public PluginPackResources(final String name, final PluginResource resource) {
        super(new File("ignore_me"));
        this.name = name;
        this.resource = resource;
        this.metadata = new PackMetadataSection(new TextComponent("Plugin Resources"), 6);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    protected InputStream getResource(final String rawPath) throws IOException {
        if (!this.hasResource(rawPath)) {
            throw new ResourcePackFileNotFoundException(this.file, rawPath);
        }

        return Files.newInputStream(this.filePath(rawPath));
    }

    @Override
    protected boolean hasResource(final String rawPath) {
        try {
            return Files.exists(this.filePath(rawPath));
        } catch (final Exception e) {
            return false;
        }
    }

    @Override
    public Collection<ResourceLocation> getResources(final PackType type, final String namespace, final String path, final int depth,
            final Predicate<String> fileNameValidator) {
        try {
            Path root = this.typeRoot(type);
            return Files.walk(root.resolve(namespace).resolve(namespace), depth)
                .filter(s -> !s.getFileName().toString().endsWith(".mcmeta"))
                .map(Object::toString)
                .filter(fileNameValidator)
                .map(s -> new ResourceLocation(namespace, path))
                .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(final MetadataSectionSerializer<T> deserializer) throws IOException {
        if (deserializer.getMetadataSectionName().equals("pack")) {
            return (T) this.metadata;
        }
        return null;
    }

    @Override
    public Set<String> getNamespaces(final PackType type) {
        try {
            return Files.list(this.typeRoot(type))
                .map(Path::getFileName)
                .map(Object::toString)
                .filter(s -> {
                    if (s.equals(s.toLowerCase(Locale.ROOT))) {
                        return true;
                    } else {
                        SpongeCommon.logger().warn("Pack: ignored non-lowercased namespace: {} in {}", s, this.resource.path());
                        return false;
                    }
                })
                .collect(Collectors.toSet());
        } catch (final IOException e) {
            return Collections.emptySet();
        }
    }

    @Override
    public void close() {

    }

    private FileSystem fileSystem() throws IOException {
        if (this.fileSystem == null) {
            if (this.resource instanceof JVMPluginResource) {
                this.fileSystem = ((JVMPluginResource) this.resource).fileSystem();
            } else {
                // How...would non JVM resources work here?
            }
        }

        return this.fileSystem;
    }

    private Path typeRoot(final PackType type) throws IOException {
        return this.fileSystem().getPath(type.getDirectory());
    }

    private Path filePath(final String path) throws IOException {
        return this.fileSystem().getPath(path);
    }
}
