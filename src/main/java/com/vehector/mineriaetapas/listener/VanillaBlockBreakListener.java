package com.vehector.mineriaetapas.listener;

import com.vehector.mineriaetapas.MineriaEtapasPlugin;
import com.vehector.mineriaetapas.manager.ProgressionEngine;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public final class VanillaBlockBreakListener
implements Listener {
    private final MineriaEtapasPlugin plugin;

    public VanillaBlockBreakListener(MineriaEtapasPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onBreak(BlockBreakEvent event) {
        if (this.plugin.getBreakDeduper().isJustHandled(event.getBlock())) {
            return;
        }
        Block b = event.getBlock();
        ProgressionEngine.Result r = this.plugin.getProgressionEngine().handleBreak(event.getPlayer(), b.getType(), b.getWorld().getName());
        if (r == ProgressionEngine.Result.DENIED) {
            event.setCancelled(true);
        }
    }
}

