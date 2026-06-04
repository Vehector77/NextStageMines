package com.vehector.mineriaetapas;

import com.vehector.mineriaetapas.command.EtapasCommand;
import com.vehector.mineriaetapas.config.ConfigManager;
import com.vehector.mineriaetapas.data.DataStore;
import com.vehector.mineriaetapas.data.PlayerData;
import com.vehector.mineriaetapas.hook.BlockRegenBridge;
import com.vehector.mineriaetapas.hook.LuckPermsHook;
import com.vehector.mineriaetapas.listener.BlockRegenHookListener;
import com.vehector.mineriaetapas.listener.BreakDeduper;
import com.vehector.mineriaetapas.listener.PlayerJoinQuitListener;
import com.vehector.mineriaetapas.listener.RegionEntryListener;
import com.vehector.mineriaetapas.listener.VanillaBlockBreakListener;
import com.vehector.mineriaetapas.manager.BypassManager;
import com.vehector.mineriaetapas.manager.PlayerDataManager;
import com.vehector.mineriaetapas.manager.ProgressionEngine;
import com.vehector.mineriaetapas.manager.RegionAccessManager;
import com.vehector.mineriaetapas.manager.StageManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class MineriaEtapasPlugin
extends JavaPlugin {
    private ConfigManager configManager;
    private StageManager stageManager;
    private DataStore dataStore;
    private PlayerDataManager playerDataManager;
    private ProgressionEngine progressionEngine;
    private BreakDeduper breakDeduper;
    private LuckPermsHook luckPermsHook;
    private BlockRegenBridge blockRegenBridge;
    private RegionAccessManager regionAccessManager;
    private BypassManager bypassManager;
    private final List<Listener> registered = new ArrayList<Listener>();
    private BukkitTask deduperTask;

    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.configManager.load();
        this.luckPermsHook = new LuckPermsHook();
        this.blockRegenBridge = new BlockRegenBridge();
        this.stageManager = new StageManager(this);
        this.dataStore = new DataStore(this);
        try {
            this.dataStore.init();
        }
        catch (SQLException e) {
            this.getLogger().severe("No se pudo inicializar SQLite: " + e.getMessage());
            this.getServer().getPluginManager().disablePlugin((Plugin)this);
            return;
        }
        this.playerDataManager = new PlayerDataManager(this, this.dataStore, this.configManager.getCacheSize());
        this.playerDataManager.startAutosave(this.configManager.getAutosaveInterval());
        this.progressionEngine = new ProgressionEngine(this);
        this.breakDeduper = new BreakDeduper();
        this.regionAccessManager = new RegionAccessManager(this);
        this.bypassManager = new BypassManager();
        this.deduperTask = Bukkit.getScheduler().runTaskTimer((Plugin)this, this.breakDeduper::tick, 20L, 20L);
        this.registerListeners();
        Bukkit.getScheduler().runTaskLater((Plugin)this, () -> {
            this.blockRegenBridge.check();
            this.regionAccessManager.syncFromBlockRegen();
            this.regionAccessManager.startAutoAddTask();
        }, 40L);
        PluginCommand cmd = this.getCommand("etapas");
        if (cmd != null) {
            EtapasCommand handler = new EtapasCommand(this);
            cmd.setExecutor((CommandExecutor)handler);
            cmd.setTabCompleter((TabCompleter)handler);
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            this.playerDataManager.loadAsync(p.getUniqueId(), data -> this.playerDataManager.recomputeStage(p, (PlayerData)data));
        }
        this.getLogger().info("NextStageMine v" + this.getDescription().getVersion() + " habilitado. LuckPerms=" + this.luckPermsHook.isAvailable() + ", BlockRegen=" + this.blockRegenBridge.isAvailable());
    }

    public void onDisable() {
        if (this.deduperTask != null) {
            this.deduperTask.cancel();
        }
        if (this.regionAccessManager != null) {
            this.regionAccessManager.stopAutoAddTask();
        }
        this.unregisterListeners();
        if (this.playerDataManager != null) {
            this.playerDataManager.stopAutosave();
            this.playerDataManager.flushAndCloseSync();
        }
        if (this.dataStore != null) {
            this.dataStore.close();
        }
        this.getLogger().info("NextStageMine deshabilitado.");
    }

    private void registerListeners() {
        boolean useVanilla;
        boolean useRegen;
        this.addListener(new PlayerJoinQuitListener(this));
        if (this.configManager.isRegionsEnabled()) {
            this.addListener(new RegionEntryListener(this));
        }
        ConfigManager.HookMode mode = this.configManager.getHookMode();
        boolean brLoaded = Bukkit.getPluginManager().isPluginEnabled("BlockRegen");
        switch (mode) {
            case BLOCKREGEN: {
                useRegen = true;
                useVanilla = false;
                break;
            }
            case VANILLA: {
                useRegen = false;
                useVanilla = true;
                break;
            }
            case BOTH: {
                useRegen = true;
                useVanilla = true;
                break;
            }
            default: {
                useRegen = brLoaded;
                boolean bl = useVanilla = !brLoaded;
            }
        }
        if (useRegen) {
            if (!brLoaded) {
                this.getLogger().warning("hook-mode requiere BlockRegen pero no esta cargado. Fallback a vanilla.");
                useVanilla = true;
            } else {
                try {
                    this.addListener(new BlockRegenHookListener(this));
                    this.getLogger().info("Hook a BlockRegen registrado correctamente.");
                }
                catch (Throwable t) {
                    this.getLogger().warning("No se pudo registrar el hook de BlockRegen: " + t.getMessage());
                    useVanilla = true;
                }
            }
        }
        if (useVanilla) {
            this.addListener(new VanillaBlockBreakListener(this));
            this.getLogger().info("Listener vanilla BlockBreakEvent registrado.");
        }
    }

    private void addListener(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, (Plugin)this);
        this.registered.add(listener);
    }

    private void unregisterListeners() {
        for (Listener l : this.registered) {
            HandlerList.unregisterAll((Listener)l);
        }
        this.registered.clear();
    }

    public void reloadAll() {
        this.configManager.load();
        this.unregisterListeners();
        this.luckPermsHook.initialize();
        this.blockRegenBridge.check();
        this.registerListeners();
        if (this.regionAccessManager != null) {
            this.regionAccessManager.stopAutoAddTask();
            this.regionAccessManager.startAutoAddTask();
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerData d = this.playerDataManager.getCached(p.getUniqueId());
            if (d == null) continue;
            this.playerDataManager.recomputeStage(p, d);
        }
        this.playerDataManager.startAutosave(this.configManager.getAutosaveInterval());
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public StageManager getStageManager() {
        return this.stageManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return this.playerDataManager;
    }

    public ProgressionEngine getProgressionEngine() {
        return this.progressionEngine;
    }

    public BreakDeduper getBreakDeduper() {
        return this.breakDeduper;
    }

    public LuckPermsHook getLuckPermsHook() {
        return this.luckPermsHook;
    }

    public BlockRegenBridge getBlockRegenBridge() {
        return this.blockRegenBridge;
    }

    public RegionAccessManager getRegionAccessManager() {
        return this.regionAccessManager;
    }

    public BypassManager getBypassManager() {
        return this.bypassManager;
    }
}

