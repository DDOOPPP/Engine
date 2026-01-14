package org.gi.resource;

import java.util.Collection;
import java.util.Optional;

public interface IResourceRegistry {
    boolean register(IResourceConfig config);

    Optional<IResourceConfig> getConfig(String id);

    Collection<IResourceConfig> getAll();

    boolean contains(String id);

    void clear();
}
