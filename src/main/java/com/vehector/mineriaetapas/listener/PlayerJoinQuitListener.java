package com.vehector.mineriaetapas.listener;

import com.vehector.mineriaetapas.MineriaEtapasPlugin;
import com.vehector.mineriaetapas.data.PlayerData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerJoinQuitListener
implements Listener {
    private final MineriaEtapasPlugin plugin;

    public PlayerJoinQuitListener(MineriaEtapasPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        this.plugin.getPlayerDataManager().loadAsync(event.getPlayer().getUniqueId(), data -> this.plugin.getPlayerDataManager().recomputeStage(event.getPlayer(), (PlayerData)data));
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        this.plugin.getProgressionEngine().clearPlayerCache(event.getPlayer().getUniqueId());
        this.plugin.getBypassManager().clearPlayer(event.getPlayer().getUniqueId());
        PlayerData data = this.plugin.getPlayerDataManager().getCached(event.getPlayer().getUniqueId());
        if (data != null && data.isDirty()) {
            this.plugin.getPlayerDataManager().saveAsync(data);
        }
    }
}

