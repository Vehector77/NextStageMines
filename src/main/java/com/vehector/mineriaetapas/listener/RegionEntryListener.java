package com.vehector.mineriaetapas.listener;

import com.vehector.mineriaetapas.MineriaEtapasPlugin;
import com.vehector.mineriaetapas.manager.RegionAccessManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class RegionEntryListener
implements Listener {
    private final MineriaEtapasPlugin plugin;

    public RegionEntryListener(MineriaEtapasPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onMove(PlayerMoveEvent event) {
        if (!this.plugin.getConfigManager().isRegionsEnabled()) {
            return;
        }
        Location to = event.getTo();
        Location from = event.getFrom();
        if (to == null) {
            return;
        }
        String mode = this.plugin.getConfigManager().getRegionsMoveCheckMode();
        if ("BLOCK".equalsIgnoreCase(mode) && from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ() && from.getWorld() == to.getWorld()) {
            return;
        }
        RegionAccessManager.RegionCheckResult res = new RegionAccessManager.RegionCheckResult();
        boolean ok = this.plugin.getRegionAccessManager().canEnter(event.getPlayer(), to, res);
        if (!ok) {
            event.setCancelled(true);
            event.getPlayer().teleport(from);
            this.plugin.getRegionAccessManager().sendDenyMessage(event.getPlayer(), res.access);
            this.plugin.getRegionAccessManager().setCurrentRegion(event.getPlayer().getUniqueId(), null);
            return;
        }
        this.plugin.getRegionAccessManager().setCurrentRegion(event.getPlayer().getUniqueId(), res.region);
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!this.plugin.getConfigManager().isRegionsEnabled()) {
            return;
        }
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        RegionAccessManager.RegionCheckResult res = new RegionAccessManager.RegionCheckResult();
        boolean ok = this.plugin.getRegionAccessManager().canEnter(event.getPlayer(), to, res);
        if (!ok) {
            event.setCancelled(true);
            this.plugin.getRegionAccessManager().sendDenyMessage(event.getPlayer(), res.access);
            return;
        }
        this.plugin.getRegionAccessManager().setCurrentRegion(event.getPlayer().getUniqueId(), res.region);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.plugin.getRegionAccessManager().clearPlayer(event.getPlayer().getUniqueId());
    }
}

