package com.vehector.mineriaetapas.config;

import com.vehector.mineriaetapas.config.Stage;
import java.util.List;
import org.bukkit.Material;

public final class StageList {
    private final List<Stage> stages;

    public StageList(List<Stage> stages) {
        this.stages = List.copyOf(stages);
    }

    public List<Stage> all() {
        return this.stages;
    }

    public int size() {
        return this.stages.size();
    }

    public Stage byIndex(int i) {
        if (i < 0 || i >= this.stages.size()) {
            return null;
        }
        return this.stages.get(i);
    }

    public Stage byId(String id) {
        if (id == null) {
            return null;
        }
        for (Stage s : this.stages) {
            if (!s.getId().equalsIgnoreCase(id)) continue;
            return s;
        }
        return null;
    }

    public Stage forMaterial(Material m) {
        if (m == null) {
            return null;
        }
        for (Stage s : this.stages) {
            if (!s.contains(m)) continue;
            return s;
        }
        return null;
    }
}

