package com.vehector.mineriaetapas.manager;

import com.vehector.mineriaetapas.MineriaEtapasPlugin;
import com.vehector.mineriaetapas.config.RegionAccess;
import com.vehector.mineriaetapas.util.Messages;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class RegionAccessManager {
    private final MineriaEtapasPlugin plugin;
    private final Map<UUID, String> currentRegion = new HashMap<UUID, String>();
    private final Map<UUID, Long> lastDenyMsgNs = new HashMap<UUID, Long>();
    private BukkitTask autoAddTask;

    public RegionAccessManager(MineriaEtapasPlugin plugin) {
        this.plugin = plugin;
    }

    public void startAutoAddTask() {
        this.stopAutoAddTask();
        if (!this.plugin.getConfigManager().isRegionsEnabled()) {
            return;
        }
        if (!this.plugin.getConfigManager().isRegionsAutoAdd()) {
            return;
        }
        int seconds = this.plugin.getConfigManager().getRegionsCheckIntervalSeconds();
        long ticks = (long)seconds * 20L;
        this.autoAddTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, this::syncFromBlockRegen, ticks, ticks);
    }

    public void stopAutoAddTask() {
        if (this.autoAddTask != null) {
            this.autoAddTask.cancel();
            this.autoAddTask = null;
        }
    }

    public void syncFromBlockRegen() {
        if (!this.plugin.getBlockRegenBridge().isAvailable()) {
            return;
        }
        for (String name : this.plugin.getBlockRegenBridge().getLoadedRegionNames()) {
            boolean added = this.plugin.getConfigManager().addRegionIfMissing(name);
            if (!added) continue;
            this.plugin.getLogger().info("Region nueva detectada en BlockRegen y anadida a config: " + name);
            HashMap<String, String> ph = new HashMap<String, String>();
            ph.put("region", name);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.hasPermission("mineriaetapas.admin")) continue;
                Messages.send((CommandSender)p, this.plugin, "region-added-auto", ph);
            }
        }
    }

    public boolean canEnter(Player player, Location loc, RegionCheckResult outResult) {
        outResult.region = null;
        outResult.access = null;
        if (!this.plugin.getConfigManager().isRegionsEnabled()) {
            return true;
        }
        if (this.plugin.getBypassManager().isBypassing(player)
                || player.hasPermission("mineriaetapas.regions.bypass")) {
            return true;
        }
        String regionName = this.plugin.getBlockRegenBridge().getRegionAt(loc);
        if (regionName == null) {
            return true;
        }
        outResult.region = regionName;
        RegionAccess acc = this.plugin.getConfigManager().getRegion(regionName);
        if (acc == null) {
            if (this.plugin.getConfigManager().isRegionsAutoAdd()) {
                this.plugin.getConfigManager().addRegionIfMissing(regionName);
                acc = this.plugin.getConfigManager().getRegion(regionName);
            }
            if (acc == null) {
                return true;
            }
        }
        outResult.access = acc;
        List<String> groups = this.plugin.getLuckPermsHook().getGroups(player);
        return acc.allowsAny(groups);
    }

    public void sendDenyMessage(Player player, RegionAccess access) {
        if (access == null) {
            return;
        }
        long now = System.nanoTime();
        long cdNs = this.plugin.getConfigManager().getRegionsDefaultDenyCooldown() * 1000000L;
        Long last = this.lastDenyMsgNs.get(player.getUniqueId());
        if (last != null && now - last < cdNs) {
            return;
        }
        this.lastDenyMsgNs.put(player.getUniqueId(), now);
        HashMap<String, String> ph = new HashMap<String, String>();
        ph.put("player", player.getName());
        ph.put("region", access.getName());
        ph.put("group", this.plugin.getLuckPermsHook().getPrimaryGroup(player));
        player.sendMessage(Messages.parse(this.plugin, access.getDenyMessage(), ph));
    }

    public void clearPlayer(UUID uuid) {
        this.currentRegion.remove(uuid);
        this.lastDenyMsgNs.remove(uuid);
    }

    public String getCurrentRegion(UUID uuid) {
        return this.currentRegion.get(uuid);
    }

    public void setCurrentRegion(UUID uuid, String region) {
        if (region == null) {
            this.currentRegion.remove(uuid);
        } else {
            this.currentRegion.put(uuid, region);
        }
    }

    public static final class RegionCheckResult {
        public String region;
        public RegionAccess access;
    }
}

