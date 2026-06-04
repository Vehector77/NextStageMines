package com.vehector.mineriaetapas.manager;

import com.vehector.mineriaetapas.MineriaEtapasPlugin;
import com.vehector.mineriaetapas.config.ConfigManager;
import com.vehector.mineriaetapas.config.Stage;
import com.vehector.mineriaetapas.config.StageList;
import java.util.List;
import java.util.Locale;
import org.bukkit.entity.Player;

public final class StageManager {
    private final MineriaEtapasPlugin plugin;
    private final ConfigManager config;

    public StageManager(MineriaEtapasPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    public StageList getStagesFor(Player player) {
        if (this.config.isGeneralStagesEnabled() || player == null) {
            return this.config.getGlobalStages();
        }
        String group = this.plugin.getLuckPermsHook().getPrimaryGroup(player);
        StageList list = this.config.getRankStages().get(group);
        if (list != null && list.size() > 0) {
            return list;
        }
        list = this.config.getRankStages().get("default");
        if (list != null && list.size() > 0) {
            return list;
        }
        return this.config.getGlobalStages();
    }

    public StageList getStagesByGroup(String group) {
        if (this.config.isGeneralStagesEnabled() || group == null) {
            return this.config.getGlobalStages();
        }
        StageList list = this.config.getRankStages().get(group.toLowerCase(Locale.ROOT));
        if (list != null) {
            return list;
        }
        list = this.config.getRankStages().get("default");
        if (list != null) {
            return list;
        }
        return this.config.getGlobalStages();
    }

    public List<Stage> getRawGlobalStages() {
        return this.config.getGlobalStages().all();
    }
}

