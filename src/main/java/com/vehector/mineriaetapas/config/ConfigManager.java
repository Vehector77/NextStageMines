package com.vehector.mineriaetapas.config;

import com.vehector.mineriaetapas.MineriaEtapasPlugin;
import com.vehector.mineriaetapas.config.RegionAccess;
import com.vehector.mineriaetapas.config.Stage;
import com.vehector.mineriaetapas.config.StageList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public final class ConfigManager {
    private final MineriaEtapasPlugin plugin;
    private StageList globalStages = new StageList(List.of());
    private final Map<String, StageList> rankStages = new LinkedHashMap<String, StageList>();
    private boolean generalEnabled = true;
    private Set<String> enabledWorlds = Set.of();
    private boolean enabledAllWorlds = true;
    private HookMode hookMode = HookMode.AUTO;
    private boolean adminsBypass = true;
    private boolean ignoreCreative = true;
    private int cacheSize = 256;
    private int autosaveInterval = 300;
    private long restrictionMessageCooldown = 1500L;
    private boolean regionsEnabled = true;
    private boolean regionsAutoAdd = true;
    private int regionsCheckIntervalSeconds = 10;
    private String regionsMoveCheckMode = "BLOCK";
    private List<String> regionsDefaultAllowedGroups = List.of("default");
    private String regionsDefaultDenyMessage = "%prefix%&cNo tienes el rango necesario para entrar a &e%region%&c.";
    private long regionsDefaultDenyCooldown = 1500L;
    private final Map<String, RegionAccess> regionList = new LinkedHashMap<String, RegionAccess>();
    private final Map<String, String> messages = new HashMap<String, String>();

    public ConfigManager(MineriaEtapasPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        this.plugin.saveDefaultConfig();
        this.plugin.reloadConfig();
        FileConfiguration cfg = this.plugin.getConfig();
        ConfigurationSection s = cfg.getConfigurationSection("settings");
        if (s == null) {
            this.plugin.getLogger().warning("Seccion 'settings' ausente en config.yml. Usando valores por defecto.");
            s = cfg.createSection("settings");
        }
        List<String> worlds = s.getStringList("enabled-worlds");
        this.enabledAllWorlds = worlds.isEmpty();
        HashSet<String> tmp = new HashSet<String>();
        for (String w : worlds) {
            tmp.add(w.toLowerCase(Locale.ROOT));
        }
        this.enabledWorlds = Collections.unmodifiableSet(tmp);
        switch (s.getString("hook-mode", "auto").toLowerCase(Locale.ROOT)) {
            case "blockregen": {
                this.hookMode = HookMode.BLOCKREGEN;
                break;
            }
            case "vanilla": {
                this.hookMode = HookMode.VANILLA;
                break;
            }
            case "both": {
                this.hookMode = HookMode.BOTH;
                break;
            }
            default: {
                this.hookMode = HookMode.AUTO;
            }
        }
        this.adminsBypass = s.getBoolean("admins-bypass-restriction", true);
        this.ignoreCreative = s.getBoolean("ignore-creative", true);
        this.cacheSize = Math.max(0, s.getInt("cache-size", 256));
        this.autosaveInterval = Math.max(0, s.getInt("autosave-interval", 300));
        this.restrictionMessageCooldown = Math.max(0L, s.getLong("restriction-message-cooldown", 1500L));
        ConfigurationSection sm = cfg.getConfigurationSection("stages-mode");
        this.generalEnabled = sm == null || sm.getBoolean("general-enabled", true);
        this.globalStages = new StageList(this.loadStagesFromList(cfg.getMapList("stages"), "global"));
        this.rankStages.clear();
        ConfigurationSection rs = cfg.getConfigurationSection("rank-stages");
        if (rs != null) {
            for (String key : rs.getKeys(false)) {
                List raw = rs.getMapList(key);
                this.rankStages.put(key.toLowerCase(Locale.ROOT), new StageList(this.loadStagesFromList(raw, "rank:" + key)));
            }
        }
        if (!this.generalEnabled && !this.rankStages.containsKey("default")) {
            this.plugin.getLogger().warning("stages-mode.general-enabled=false pero no hay 'rank-stages.default'. Se usara la lista global como fallback.");
        }
        this.regionList.clear();
        ConfigurationSection reg = cfg.getConfigurationSection("regions");
        if (reg != null) {
            this.regionsEnabled = reg.getBoolean("enabled", true);
            this.regionsAutoAdd = reg.getBoolean("auto-add", true);
            this.regionsCheckIntervalSeconds = Math.max(2, reg.getInt("check-interval-seconds", 10));
            this.regionsMoveCheckMode = reg.getString("move-check-mode", "BLOCK").toUpperCase(Locale.ROOT);
            this.regionsDefaultAllowedGroups = reg.getStringList("default-allowed-groups");
            if (this.regionsDefaultAllowedGroups == null || this.regionsDefaultAllowedGroups.isEmpty()) {
                this.regionsDefaultAllowedGroups = List.of("default");
            }
            this.regionsDefaultDenyMessage = reg.getString("default-deny-message", this.regionsDefaultDenyMessage);
            this.regionsDefaultDenyCooldown = Math.max(0L, reg.getLong("default-deny-message-cooldown", 1500L));
            ConfigurationSection listSec = reg.getConfigurationSection("list");
            if (listSec != null) {
                for (String name : listSec.getKeys(false)) {
                    ConfigurationSection one = listSec.getConfigurationSection(name);
                    if (one == null) continue;
                    List groups = one.getStringList("allowed-groups");
                    String denyMsg = one.getString("deny-message", this.regionsDefaultDenyMessage);
                    this.regionList.put(name.toLowerCase(Locale.ROOT), new RegionAccess(name, new LinkedHashSet<String>(groups), denyMsg));
                }
            }
        }
        this.messages.clear();
        ConfigurationSection ms = cfg.getConfigurationSection("messages");
        if (ms != null) {
            for (String k : ms.getKeys(false)) {
                this.messages.put(k, ms.getString(k, ""));
            }
        }
        this.plugin.getLogger().info("Cargadas " + this.globalStages.size() + " etapas globales, " + this.rankStages.size() + " grupos con rank-stages, " + this.regionList.size() + " regiones configuradas.");
    }

    private List<Stage> loadStagesFromList(List<Map<?, ?>> rawStages, String context) {
        ArrayList<Stage> loaded = new ArrayList<Stage>();
        if (rawStages == null || rawStages.isEmpty()) {
            return loaded;
        }
        int i = 0;
        for (Map<?, ?> raw : rawStages) {
            int n;
            Object idObj = raw.get("id");
            if (idObj == null) {
                this.plugin.getLogger().warning("[" + context + "] Etapa #" + i + " sin 'id'. Saltada.");
                continue;
            }
            String id = String.valueOf(idObj);
            String display = raw.get("display") == null ? id : String.valueOf(raw.get("display"));
            Object obj = raw.get("required");
            if (obj instanceof Number) {
                Number n2 = (Number)obj;
                n = n2.intValue();
            } else {
                n = 0;
            }
            int required = n;
            Object blocksObj = raw.get("blocks");
            HashSet<Material> mats = new HashSet<Material>();
            if (blocksObj instanceof List) {
                List list = (List)blocksObj;
                for (Object o : list) {
                    String matName = String.valueOf(o).toUpperCase(Locale.ROOT).trim();
                    Material m = Material.matchMaterial((String)matName);
                    if (m == null) {
                        this.plugin.getLogger().log(Level.WARNING, "[" + context + "] Material '" + matName + "' invalido en etapa '" + id + "'. Ignorado.");
                        continue;
                    }
                    mats.add(m);
                }
            }
            loaded.add(new Stage(i, id, display, required, mats));
            ++i;
        }
        return loaded;
    }

    public boolean addRegionIfMissing(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        if (this.regionList.containsKey(key)) {
            return false;
        }
        FileConfiguration cfg = this.plugin.getConfig();
        String path = "regions.list." + name;
        cfg.set(path + ".allowed-groups", this.regionsDefaultAllowedGroups);
        cfg.set(path + ".deny-message", (Object)this.regionsDefaultDenyMessage);
        this.plugin.saveConfig();
        this.regionList.put(key, new RegionAccess(name, new LinkedHashSet<String>(this.regionsDefaultAllowedGroups), this.regionsDefaultDenyMessage));
        return true;
    }

    public StageList getGlobalStages() {
        return this.globalStages;
    }

    public Map<String, StageList> getRankStages() {
        return this.rankStages;
    }

    public boolean isGeneralStagesEnabled() {
        return this.generalEnabled;
    }

    public boolean isWorldEnabled(String worldName) {
        if (this.enabledAllWorlds) {
            return true;
        }
        return this.enabledWorlds.contains(worldName.toLowerCase(Locale.ROOT));
    }

    public HookMode getHookMode() {
        return this.hookMode;
    }

    public boolean adminsBypass() {
        return this.adminsBypass;
    }

    public boolean ignoreCreative() {
        return this.ignoreCreative;
    }

    public int getCacheSize() {
        return this.cacheSize;
    }

    public int getAutosaveInterval() {
        return this.autosaveInterval;
    }

    public long getRestrictionMessageCooldown() {
        return this.restrictionMessageCooldown;
    }

    public boolean isRegionsEnabled() {
        return this.regionsEnabled;
    }

    public boolean isRegionsAutoAdd() {
        return this.regionsAutoAdd;
    }

    public int getRegionsCheckIntervalSeconds() {
        return this.regionsCheckIntervalSeconds;
    }

    public String getRegionsMoveCheckMode() {
        return this.regionsMoveCheckMode;
    }

    public long getRegionsDefaultDenyCooldown() {
        return this.regionsDefaultDenyCooldown;
    }

    public Map<String, RegionAccess> getRegionList() {
        return this.regionList;
    }

    public RegionAccess getRegion(String name) {
        if (name == null) {
            return null;
        }
        return this.regionList.get(name.toLowerCase(Locale.ROOT));
    }

    public String getMessage(String key) {
        return this.messages.getOrDefault(key, "");
    }

    public static enum HookMode {
        AUTO,
        BLOCKREGEN,
        VANILLA,
        BOTH;

    }
}

