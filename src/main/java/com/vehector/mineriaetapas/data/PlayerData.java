package com.vehector.mineriaetapas.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerData {
    private final UUID uuid;
    private volatile int currentStageIndex;
    private final Map<String, Integer> stageCounts = new ConcurrentHashMap<String, Integer>();
    private volatile boolean dirty;

    public PlayerData(UUID uuid, int currentStageIndex) {
        this.uuid = uuid;
        this.currentStageIndex = currentStageIndex;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public int getCurrentStageIndex() {
        return this.currentStageIndex;
    }

    public void setCurrentStageIndex(int currentStageIndex) {
        if (this.currentStageIndex != currentStageIndex) {
            this.currentStageIndex = currentStageIndex;
            this.dirty = true;
        }
    }

    public int getCount(String stageId) {
        Integer v = this.stageCounts.get(stageId);
        return v == null ? 0 : v;
    }

    public int incrementCount(String stageId, int delta) {
        int v = this.stageCounts.merge(stageId, delta, Integer::sum);
        this.dirty = true;
        return v;
    }

    public void setCount(String stageId, int value) {
        if (value <= 0) {
            this.stageCounts.remove(stageId);
        } else {
            this.stageCounts.put(stageId, value);
        }
        this.dirty = true;
    }

    public void resetCounts() {
        this.stageCounts.clear();
        this.dirty = true;
    }

    public Map<String, Integer> snapshotCounts() {
        return new HashMap<String, Integer>(this.stageCounts);
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void clearDirty() {
        this.dirty = false;
    }

    public void markDirty() {
        this.dirty = true;
    }
}

