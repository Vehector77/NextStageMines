package com.vehector.mineriaetapas.listener;

import com.vehector.mineriaetapas.MineriaEtapasPlugin;
import com.vehector.mineriaetapas.manager.ProgressionEngine;
import nl.aurorion.blockregen.api.BlockRegenBlockBreakEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public final class BlockRegenHookListener
implements Listener {
    private final MineriaEtapasPlugin plugin;

    public BlockRegenHookListener(MineriaEtapasPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onRegenBreak(BlockRegenBlockBreakEvent event) {
        Player player = null;
        Event src = event.getEvent();
        if (src instanceof BlockBreakEvent) {
            BlockBreakEvent bbe = (BlockBreakEvent)src;
            player = bbe.getPlayer();
        }
        if (player == null) {
            return;
        }
        ProgressionEngine.Result r = this.plugin.getProgressionEngine().handleBreak(player, event.getBlock().getType(), event.getBlock().getWorld().getName());
        if (r == ProgressionEngine.Result.DENIED) {
            event.setCancelled(true);
            if (src instanceof BlockBreakEvent) {
                BlockBreakEvent bbe2 = (BlockBreakEvent)src;
                bbe2.setCancelled(true);
            }
        } else if (r == ProgressionEngine.Result.ALLOWED_COUNTED) {
            this.plugin.getBreakDeduper().markHandled(event.getBlock());
        }
    }
}

