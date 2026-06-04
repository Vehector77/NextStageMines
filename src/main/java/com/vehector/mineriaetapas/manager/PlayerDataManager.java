package com.vehector.mineriaetapas.manager;

import com.vehector.mineriaetapas.MineriaEtapasPlugin;
import com.vehector.mineriaetapas.config.Stage;
import com.vehector.mineriaetapas.config.StageList;
import com.vehector.mineriaetapas.data.DataStore;
import com.vehector.mineriaetapas.data.PlayerData;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class PlayerDataManager {
    private final MineriaEtapasPlugin plugin;
    private final DataStore store;
    private final LinkedHashMap<UUID, PlayerData> cache;
    private final int maxSize;
    private BukkitTask autosaveTask;

    public PlayerDataManager(MineriaEtapasPlugin plugin, DataStore store, final int maxSize) {
        this.plugin = plugin;
        this.store = store;
        this.maxSize = maxSize;
        this.cache = new LinkedHashMap<UUID, PlayerData>(64, 0.75f, true){

            @Override
            protected boolean removeEldestEntry(Map.Entry<UUID, PlayerData> eldest) {
                if (maxSize <= 0) {
                    return false;
                }
                if (this.size() <= maxSize) {
                    return false;
                }
                return Bukkit.getPlayer((UUID)eldest.getKey()) == null;
            }
        };
    }

    public synchronized PlayerData getOrLoad(UUID uuid) {
        PlayerData d = this.cache.get(uuid);
        if (d != null) {
            return d;
        }
        d = this.store.load(uuid);
        this.cache.put(uuid, d);
        return d;
    }

    public synchronized PlayerData getCached(UUID uuid) {
        return this.cache.get(uuid);
    }

    public synchronized void putLoaded(PlayerData data) {
        this.cache.put(data.getUuid(), data);
    }

    public void loadAsync(UUID uuid, Consumer<PlayerData> callback) {
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            PlayerData d = this.getOrLoad(uuid);
            if (callback != null) {
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> callback.accept(d));
            }
        });
    }

    public void saveAsync(PlayerData data) {
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> this.store.save(data));
    }

    public synchronized void unload(UUID uuid, boolean save) {
        PlayerData d = (PlayerData)this.cache.remove(uuid);
        if (d != null && save && d.isDirty()) {
            Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> this.store.save(d));
        }
    }

    public void startAutosave(int seconds) {
        this.stopAutosave();
        if (seconds <= 0) {
            return;
        }
        long ticks = (long)seconds * 20L;
        this.autosaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously((Plugin)this.plugin, this::flushDirty, ticks, ticks);
    }

    public void stopAutosave() {
        if (this.autosaveTask != null) {
            this.autosaveTask.cancel();
            this.autosaveTask = null;
        }
    }

    public synchronized void flushDirty() {
        for (Map.Entry<UUID, PlayerData> e : this.cache.entrySet()) {
            PlayerData d = e.getValue();
            if (!d.isDirty()) continue;
            this.store.save(d);
        }
    }

    public synchronized void flushAndCloseSync() {
        for (PlayerData d : this.cache.values()) {
            if (!d.isDirty()) continue;
            this.store.save(d);
        }
        this.cache.clear();
    }

    public boolean recomputeStage(Player player, PlayerData data) {
        StageList list = this.plugin.getStageManager().getStagesFor(player);
        return this.recomputeWith(list, data);
    }

    public boolean recomputeGlobal(PlayerData data) {
        return this.recomputeWith(this.plugin.getConfigManager().getGlobalStages(), data);
    }

    private boolean recomputeWith(StageList list, PlayerData data) {
        int got;
        Stage current;
        Stage next;
        int target = data.getCurrentStageIndex();
        while ((next = list.byIndex(target + 1)) != null && (current = list.byIndex(target)) != null && (got = data.getCount(current.getId())) >= next.getRequired()) {
            ++target;
        }
        if (target != data.getCurrentStageIndex()) {
            data.setCurrentStageIndex(target);
            return true;
        }
        return false;
    }

    public int cacheSize() {
        return this.cache.size();
    }
}

