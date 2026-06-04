package com.vehector.mineriaetapas.config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Material;

public final class Stage {
    private final String id;
    private final String display;
    private final int required;
    private final Set<Material> blocks;
    private final int index;

    public Stage(int index, String id, String display, int required, Set<Material> blocks) {
        this.index = index;
        this.id = id;
        this.display = display;
        this.required = required;
        this.blocks = Collections.unmodifiableSet(new HashSet<Material>(blocks));
    }

    public int getIndex() {
        return this.index;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplay() {
        return this.display;
    }

    public int getRequired() {
        return this.required;
    }

    public Set<Material> getBlocks() {
        return this.blocks;
    }

    public boolean contains(Material material) {
        return this.blocks.contains(material);
    }
}

