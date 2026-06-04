package com.vehector.mineriaetapas.listener;

import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;

public final class BreakDeduper {
    private final ConcurrentHashMap<Key, Boolean> seen = new ConcurrentHashMap();

    public void markHandled(Block block) {
        this.seen.put(this.keyOf(block), Boolean.TRUE);
    }

    public boolean isJustHandled(Block block) {
        return this.seen.containsKey(this.keyOf(block));
    }

    public void tick() {
        int now = Bukkit.getCurrentTick();
        this.seen.keySet().removeIf(k -> k.tick() < now);
    }

    private Key keyOf(Block b) {
        return new Key(b.getWorld().getName(), b.getX(), b.getY(), b.getZ(), Bukkit.getCurrentTick());
    }

    public record Key(String world, int x, int y, int z, int tick) {
    }
}

