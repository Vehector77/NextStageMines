package com.vehector.mineriaetapas.manager;

import com.vehector.mineriaetapas.MineriaEtapasPlugin;
import com.vehector.mineriaetapas.config.Stage;
import com.vehector.mineriaetapas.config.StageList;
import com.vehector.mineriaetapas.data.PlayerData;
import com.vehector.mineriaetapas.util.Messages;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ProgressionEngine {
    private final MineriaEtapasPlugin plugin;
    private final Map<UUID, Long> lastRestrictionMessageNs = new HashMap<UUID, Long>();

    public ProgressionEngine(MineriaEtapasPlugin plugin) {
        this.plugin = plugin;
    }

    public Result handleBreak(Player player, Material material, String worldName) {
        int got;
        if (!this.plugin.getConfigManager().isWorldEnabled(worldName)) {
            return Result.ALLOWED_UNTRACKED;
        }
        // Bypass por modo de juego (creativo/espectador)
        if (this.plugin.getConfigManager().ignoreCreative()
                && (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR)) {
            return Result.ALLOWED_UNTRACKED;
        }
        // Bypass por permisos de staff (admin/bypass/staff/OP); puede desactivarse con /etapas bypass off
        boolean bypass = this.plugin.getConfigManager().adminsBypass()
                && this.plugin.getBypassManager().isBypassing(player);
        if (bypass) {
            return Result.ALLOWED_UNTRACKED;
        }
        StageList list = this.plugin.getStageManager().getStagesFor(player);
        Stage stage = list.forMaterial(material);
        if (stage == null) {
            return Result.ALLOWED_UNTRACKED;
        }
        PlayerData data = this.plugin.getPlayerDataManager().getCached(player.getUniqueId());
        if (data == null) {
            data = this.plugin.getPlayerDataManager().getOrLoad(player.getUniqueId());
        }
        if (stage.getIndex() > data.getCurrentStageIndex()) {
            this.sendRestrictionMessage(player, stage);
            return Result.DENIED;
        }
        data.incrementCount(stage.getId(), 1);
        Stage next = list.byIndex(data.getCurrentStageIndex() + 1);
        if (next != null && stage.getIndex() == data.getCurrentStageIndex() && (got = data.getCount(stage.getId())) >= next.getRequired()) {
            data.setCurrentStageIndex(next.getIndex());
            HashMap<String, String> ph = new HashMap<String, String>();
            ph.put("player", player.getName());
            ph.put("stage", next.getDisplay());
            Messages.send((CommandSender)player, this.plugin, "stage-unlocked", ph);
            if (list.byIndex(next.getIndex() + 1) == null) {
                Messages.send((CommandSender)player, this.plugin, "max-stage", ph);
            }
        }
        return Result.ALLOWED_COUNTED;
    }

    private void sendRestrictionMessage(Player player, Stage stage) {
        long now = System.nanoTime();
        long cooldownNs = this.plugin.getConfigManager().getRestrictionMessageCooldown() * 1000000L;
        Long last = this.lastRestrictionMessageNs.get(player.getUniqueId());
        if (last != null && now - last < cooldownNs) {
            return;
        }
        this.lastRestrictionMessageNs.put(player.getUniqueId(), now);
        HashMap<String, String> ph = new HashMap<String, String>();
        ph.put("player", player.getName());
        ph.put("stage", stage.getDisplay());
        Messages.send((CommandSender)player, this.plugin, "blocked-mining", ph);
    }

    public void clearPlayerCache(UUID uuid) {
        this.lastRestrictionMessageNs.remove(uuid);
    }

    public static enum Result {
        ALLOWED_UNTRACKED,
        ALLOWED_COUNTED,
        DENIED;

    }
}

