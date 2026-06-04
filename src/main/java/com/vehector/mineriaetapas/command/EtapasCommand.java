package com.vehector.mineriaetapas.command;

import com.vehector.mineriaetapas.MineriaEtapasPlugin;
import com.vehector.mineriaetapas.config.RegionAccess;
import com.vehector.mineriaetapas.config.Stage;
import com.vehector.mineriaetapas.config.StageList;
import com.vehector.mineriaetapas.data.PlayerData;
import com.vehector.mineriaetapas.util.Messages;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class EtapasCommand
implements CommandExecutor,
TabCompleter {
    private final MineriaEtapasPlugin plugin;

    public EtapasCommand(MineriaEtapasPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                Player p = (Player)sender;
                this.showInfo(sender, (OfflinePlayer)p);
            } else {
                this.sendHelp(sender);
            }
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help": 
            case "ayuda": {
                this.sendHelp(sender);
                break;
            }
            case "info": {
                this.handleInfo(sender, args);
                break;
            }
            case "stages": 
            case "etapas": {
                this.handleStages(sender, args);
                break;
            }
            case "setstage": {
                this.handleSetStage(sender, args);
                break;
            }
            case "addblocks": {
                this.handleAddBlocks(sender, args);
                break;
            }
            case "reset": {
                this.handleReset(sender, args);
                break;
            }
            case "reload": {
                this.handleReload(sender);
                break;
            }
            case "regions": {
                this.handleRegions(sender, args);
                break;
            }
            case "bypass": {
                this.handleBypass(sender, args);
                break;
            }
            default: {
                this.sendHelp(sender);
            }
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Messages.legacy("&8&m---------&r &bNextStageMine &8&m---------"));
        sender.sendMessage(Messages.legacy("&e/etapas &7- Tu progreso."));
        sender.sendMessage(Messages.legacy("&e/etapas info &8[&7jugador&8] &7- Ver progreso."));
        sender.sendMessage(Messages.legacy("&e/etapas stages &8[&7grupo&8] &7- Lista de etapas."));
        sender.sendMessage(Messages.legacy("&e/etapas regions &7- Lista de regiones configuradas."));
        if (sender.hasPermission("mineriaetapas.setstage")) {
            sender.sendMessage(Messages.legacy("&e/etapas setstage &8<&7jugador&8> <&7etapa&8>"));
        }
        if (sender.hasPermission("mineriaetapas.addblocks")) {
            sender.sendMessage(Messages.legacy("&e/etapas addblocks &8<&7jugador&8> <&7etapa&8> <&7cantidad&8>"));
        }
        if (sender.hasPermission("mineriaetapas.reset")) {
            sender.sendMessage(Messages.legacy("&e/etapas reset &8<&7jugador&8>"));
        }
        if (sender.hasPermission("mineriaetapas.reload")) {
            sender.sendMessage(Messages.legacy("&e/etapas reload"));
        }
        if (sender.hasPermission("mineriaetapas.bypass") || sender.hasPermission("mineriaetapas.admin") || sender.hasPermission("mineriaetapas.staff") || sender.isOp()) {
            sender.sendMessage(Messages.legacy("&e/etapas bypass &8[&7on&8|&7off&8] &7- Activa/desactiva tu bypass."));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            if (!sender.hasPermission("mineriaetapas.info")) {
                Messages.send(sender, this.plugin, "no-permission", null);
                return;
            }
            OfflinePlayer t = this.resolvePlayer(args[1]);
            if (t == null) {
                Messages.send(sender, this.plugin, "unknown-player", Map.of("player", args[1]));
                return;
            }
            this.loadAndShow(sender, t);
        } else {
            if (!(sender instanceof Player)) {
                Messages.send(sender, this.plugin, "player-only", null);
                return;
            }
            Player p = (Player)sender;
            this.showInfo(sender, (OfflinePlayer)p);
        }
    }

    private void showInfo(CommandSender to, OfflinePlayer target) {
        this.loadAndShow(to, target);
    }

    private void loadAndShow(CommandSender to, OfflinePlayer target) {
        this.plugin.getPlayerDataManager().loadAsync(target.getUniqueId(), data -> {
            Player online = target.getPlayer();
            StageList list = online != null ? this.plugin.getStageManager().getStagesFor(online) : this.plugin.getConfigManager().getGlobalStages();
            Stage current = list.byIndex(data.getCurrentStageIndex());
            Stage next = list.byIndex(data.getCurrentStageIndex() + 1);
            String name = target.getName() == null ? target.getUniqueId().toString() : target.getName();
            to.sendMessage(Messages.legacy(this.plugin.getConfigManager().getMessage("progress-header").replace("%player%", name)));
            if (current != null) {
                to.sendMessage(Messages.legacy(this.plugin.getConfigManager().getMessage("progress-current").replace("%stage%", current.getDisplay())));
            }
            if (next != null && current != null) {
                int got = data.getCount(current.getId());
                int req = Math.max(1, next.getRequired());
                int remaining = Math.max(0, next.getRequired() - got);
                int percent = (int)Math.min(100L, Math.round((double)got * 100.0 / (double)req));
                String line = this.plugin.getConfigManager().getMessage("progress-next").replace("%next_stage%", next.getDisplay()).replace("%current%", String.valueOf(got)).replace("%required%", String.valueOf(next.getRequired())).replace("%remaining%", String.valueOf(remaining)).replace("%percent%", String.valueOf(percent));
                to.sendMessage(Messages.legacy(line));
            } else {
                to.sendMessage(Messages.legacy(this.plugin.getConfigManager().getMessage("progress-completed")));
            }
        });
    }

    private void handleStages(CommandSender sender, String[] args) {
        Object header;
        StageList list;
        if (args.length >= 2) {
            list = this.plugin.getStageManager().getStagesByGroup(args[1]);
            header = "&8&m------&r &bEtapas (grupo: &f" + args[1] + "&b) &8&m------";
        } else if (this.plugin.getConfigManager().isGeneralStagesEnabled() || !(sender instanceof Player)) {
            list = this.plugin.getConfigManager().getGlobalStages();
            header = "&8&m------&r &bEtapas globales &8&m------";
        } else {
            Player p = (Player)sender;
            list = this.plugin.getStageManager().getStagesFor(p);
            header = "&8&m------&r &bTus etapas &8&m------";
        }
        sender.sendMessage(Messages.legacy((String)header));
        for (Stage s : list.all()) {
            sender.sendMessage(Messages.legacy("&8#&7" + s.getIndex() + " &f" + s.getId() + " &8- " + s.getDisplay() + " &8(requiere &e" + s.getRequired() + "&8)"));
        }
    }

    private void handleSetStage(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mineriaetapas.setstage")) {
            Messages.send(sender, this.plugin, "no-permission", null);
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(Messages.legacy("&cUso: /etapas setstage <jugador> <etapa>"));
            return;
        }
        OfflinePlayer t = this.resolvePlayer(args[1]);
        if (t == null) {
            Messages.send(sender, this.plugin, "unknown-player", Map.of("player", args[1]));
            return;
        }
        Player online = t.getPlayer();
        StageList list = online != null ? this.plugin.getStageManager().getStagesFor(online) : this.plugin.getConfigManager().getGlobalStages();
        Stage stage = list.byId(args[2]);
        if (stage == null) {
            Messages.send(sender, this.plugin, "unknown-stage", Map.of("stage", args[2]));
            return;
        }
        this.plugin.getPlayerDataManager().loadAsync(t.getUniqueId(), data -> {
            data.setCurrentStageIndex(stage.getIndex());
            this.plugin.getPlayerDataManager().saveAsync((PlayerData)data);
            Messages.send(sender, this.plugin, "stage-set", Map.of("player", t.getName() == null ? args[1] : t.getName(), "stage", stage.getDisplay()));
        });
    }

    private void handleAddBlocks(CommandSender sender, String[] args) {
        int amount;
        if (!sender.hasPermission("mineriaetapas.addblocks")) {
            Messages.send(sender, this.plugin, "no-permission", null);
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(Messages.legacy("&cUso: /etapas addblocks <jugador> <etapa> <cantidad>"));
            return;
        }
        OfflinePlayer t = this.resolvePlayer(args[1]);
        if (t == null) {
            Messages.send(sender, this.plugin, "unknown-player", Map.of("player", args[1]));
            return;
        }
        Player online = t.getPlayer();
        StageList list = online != null ? this.plugin.getStageManager().getStagesFor(online) : this.plugin.getConfigManager().getGlobalStages();
        Stage stage = list.byId(args[2]);
        if (stage == null) {
            Messages.send(sender, this.plugin, "unknown-stage", Map.of("stage", args[2]));
            return;
        }
        try {
            amount = Integer.parseInt(args[3]);
        }
        catch (NumberFormatException e) {
            sender.sendMessage(Messages.legacy("&cCantidad invalida: &f" + args[3]));
            return;
        }
        int finalAmount = amount;
        this.plugin.getPlayerDataManager().loadAsync(t.getUniqueId(), data -> {
            data.incrementCount(stage.getId(), finalAmount);
            if (online != null) {
                this.plugin.getPlayerDataManager().recomputeStage(online, (PlayerData)data);
            } else {
                this.plugin.getPlayerDataManager().recomputeGlobal((PlayerData)data);
            }
            this.plugin.getPlayerDataManager().saveAsync((PlayerData)data);
            Messages.send(sender, this.plugin, "blocks-added", Map.of("player", t.getName() == null ? args[1] : t.getName(), "stage", stage.getDisplay(), "current", String.valueOf(finalAmount)));
        });
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mineriaetapas.reset")) {
            Messages.send(sender, this.plugin, "no-permission", null);
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Messages.legacy("&cUso: /etapas reset <jugador>"));
            return;
        }
        OfflinePlayer t = this.resolvePlayer(args[1]);
        if (t == null) {
            Messages.send(sender, this.plugin, "unknown-player", Map.of("player", args[1]));
            return;
        }
        this.plugin.getPlayerDataManager().loadAsync(t.getUniqueId(), data -> {
            data.resetCounts();
            data.setCurrentStageIndex(0);
            this.plugin.getPlayerDataManager().saveAsync((PlayerData)data);
            Messages.send(sender, this.plugin, "player-reset", Map.of("player", t.getName() == null ? args[1] : t.getName()));
        });
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("mineriaetapas.reload")) {
            Messages.send(sender, this.plugin, "no-permission", null);
            return;
        }
        this.plugin.reloadAll();
        Messages.send(sender, this.plugin, "reloaded", null);
    }

    private void handleRegions(CommandSender sender, String[] args) {
        sender.sendMessage(Messages.legacy("&8&m------&r &bRegiones &8&m------"));
        Map<String, RegionAccess> list = this.plugin.getConfigManager().getRegionList();
        if (list.isEmpty()) {
            sender.sendMessage(Messages.legacy("&7(sin regiones configuradas)"));
        } else {
            list.forEach((k, v) -> sender.sendMessage(Messages.legacy("&8- &b" + v.getName() + " &7| grupos: &f" + String.join((CharSequence)", ", v.getAllowedGroups()))));
        }
        sender.sendMessage(Messages.legacy("&7BlockRegen: " + (this.plugin.getBlockRegenBridge().isAvailable() ? "&aOK" : "&cNo cargado") + " &8| &7LuckPerms: " + (this.plugin.getLuckPermsHook().isAvailable() ? "&aOK" : "&cNo cargado")));
    }

    private void handleBypass(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            Messages.send(sender, this.plugin, "player-only", null);
            return;
        }
        Player player = (Player)sender;
        if (!this.plugin.getBypassManager().hasPermission(player)) {
            Messages.send(sender, this.plugin, "no-permission", null);
            return;
        }
        boolean nowOn;
        if (args.length >= 2) {
            String v = args[1].toLowerCase(Locale.ROOT);
            if (v.equals("on") || v.equals("true") || v.equals("activar") || v.equals("enable")) {
                this.plugin.getBypassManager().setEnabled(player, true);
                nowOn = true;
            } else if (v.equals("off") || v.equals("false") || v.equals("desactivar") || v.equals("disable")) {
                this.plugin.getBypassManager().setEnabled(player, false);
                nowOn = false;
            } else {
                nowOn = this.plugin.getBypassManager().toggle(player);
            }
        } else {
            nowOn = this.plugin.getBypassManager().toggle(player);
        }
        Messages.send(sender, this.plugin, nowOn ? "bypass-on" : "bypass-off", java.util.Map.of("player", player.getName()));
    }

    private OfflinePlayer resolvePlayer(String nameOrUuid) {
        try {
            UUID uuid = UUID.fromString(nameOrUuid);
            return Bukkit.getOfflinePlayer((UUID)uuid);
        }
        catch (IllegalArgumentException ignored) {
            Player online = Bukkit.getPlayerExact((String)nameOrUuid);
            if (online != null) {
                return online;
            }
            OfflinePlayer off = Bukkit.getOfflinePlayer((String)nameOrUuid);
            if (!off.hasPlayedBefore() && !off.isOnline()) {
                return null;
            }
            return off;
        }
    }

    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            ArrayList<String> opts = new ArrayList<String>(Arrays.asList("help", "info", "stages", "regions"));
            if (sender.hasPermission("mineriaetapas.bypass") || sender.hasPermission("mineriaetapas.admin") || sender.hasPermission("mineriaetapas.staff") || sender.isOp()) {
                opts.add("bypass");
            }
            if (sender.hasPermission("mineriaetapas.setstage")) {
                opts.add("setstage");
            }
            if (sender.hasPermission("mineriaetapas.addblocks")) {
                opts.add("addblocks");
            }
            if (sender.hasPermission("mineriaetapas.reset")) {
                opts.add("reset");
            }
            if (sender.hasPermission("mineriaetapas.reload")) {
                opts.add("reload");
            }
            return this.filter(opts, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && (sub.equals("setstage") || sub.equals("addblocks") || sub.equals("reset") || sub.equals("info"))) {
            ArrayList<String> names = new ArrayList<String>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                names.add(p.getName());
            }
            return this.filter(names, args[1]);
        }
        if (args.length == 2 && sub.equals("stages")) {
            ArrayList<String> groups = new ArrayList<String>(this.plugin.getConfigManager().getRankStages().keySet());
            return this.filter(groups, args[1]);
        }
        if (args.length == 2 && sub.equals("bypass")) {
            return this.filter(new ArrayList<String>(Arrays.asList("on", "off")), args[1]);
        }
        if (args.length == 3 && (sub.equals("setstage") || sub.equals("addblocks"))) {
            ArrayList<String> ids = new ArrayList<String>();
            for (Stage s : this.plugin.getConfigManager().getGlobalStages().all()) {
                ids.add(s.getId());
            }
            return this.filter(ids, args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> in, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        ArrayList<String> out = new ArrayList<String>();
        for (String s : in) {
            if (!s.toLowerCase(Locale.ROOT).startsWith(lower)) continue;
            out.add(s);
        }
        return out;
    }
}

