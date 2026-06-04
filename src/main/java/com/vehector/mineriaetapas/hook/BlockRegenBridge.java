package com.vehector.mineriaetapas.hook;

import java.util.Collections;
import java.util.List;
import nl.aurorion.blockregen.BlockRegenPlugin;
import nl.aurorion.blockregen.region.struct.RegenerationArea;
import nl.aurorion.blockregen.region.struct.RegenerationRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public final class BlockRegenBridge {
    private boolean available;

    public BlockRegenBridge() {
        this.check();
    }

    public void check() {
        this.available = Bukkit.getPluginManager().isPluginEnabled("BlockRegen");
    }

    public boolean isAvailable() {
        return this.available;
    }

    public List<String> getLoadedRegionNames() {
        if (!this.available) {
            return Collections.emptyList();
        }
        try {
            BlockRegenPlugin br = BlockRegenPlugin.getInstance();
            if (br == null || br.getRegionManager() == null) {
                return Collections.emptyList();
            }
            return br.getRegionManager().getLoadedAreas().stream().map(RegenerationArea::getName).toList();
        }
        catch (Throwable t) {
            return Collections.emptyList();
        }
    }

    public String getRegionAt(Location loc) {
        if (!this.available || loc == null || loc.getWorld() == null) {
            return null;
        }
        try {
            BlockRegenPlugin br = BlockRegenPlugin.getInstance();
            if (br == null || br.getRegionManager() == null) {
                return null;
            }
            for (RegenerationArea area : br.getRegionManager().getLoadedAreas()) {
                if (area instanceof RegenerationRegion) {
                    RegenerationRegion rr = (RegenerationRegion)area;
                    Location min = rr.getMin();
                    Location max = rr.getMax();
                    if (max.getWorld() != null && !max.getWorld().equals((Object)loc.getWorld())) continue;
                    double x = loc.getX();
                    double y = loc.getY();
                    double z = loc.getZ();
                    if (!(x >= min.getX()) || !(x <= max.getX()) || !(y >= min.getY()) || !(y <= max.getY()) || !(z >= min.getZ()) || !(z <= max.getZ())) continue;
                    return rr.getName();
                }
                try {
                    if (area.getName() == null || loc.getWorld() == null || !this.areaContainsBlock(area, loc)) continue;
                    return area.getName();
                }
                catch (Throwable throwable) {
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return null;
    }

    private boolean areaContainsBlock(RegenerationArea area, Location loc) {
        try {
            return area.contains(loc.getBlock());
        }
        catch (Throwable t) {
            return false;
        }
    }
}

