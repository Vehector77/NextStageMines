package com.vehector.mineriaetapas.hook;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class LuckPermsHook {
    private LuckPerms api;
    private boolean available;

    public LuckPermsHook() {
        this.initialize();
    }

    public void initialize() {
        if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            try {
                this.api = LuckPermsProvider.get();
                this.available = true;
            }
            catch (IllegalStateException e) {
                this.available = false;
            }
        } else {
            this.available = false;
        }
    }

    public boolean isAvailable() {
        return this.available;
    }

    public String getPrimaryGroup(Player player) {
        if (!this.available || player == null) {
            return "default";
        }
        try {
            User u = this.api.getUserManager().getUser(player.getUniqueId());
            if (u != null && u.getPrimaryGroup() != null) {
                return u.getPrimaryGroup().toLowerCase(Locale.ROOT);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return "default";
    }

    public List<String> getGroups(Player player) {
        ArrayList<String> out = new ArrayList<String>();
        if (player == null) {
            return out;
        }
        if (this.available) {
            try {
                User u = this.api.getUserManager().getUser(player.getUniqueId());
                if (u != null) {
                    u.getInheritedGroups(u.getQueryOptions()).forEach(g -> out.add(g.getName().toLowerCase(Locale.ROOT)));
                    if (!out.contains(u.getPrimaryGroup().toLowerCase(Locale.ROOT))) {
                        out.add(u.getPrimaryGroup().toLowerCase(Locale.ROOT));
                    }
                    return out;
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        player.getEffectivePermissions().forEach(perm -> {
            if (perm.getValue() && perm.getPermission().startsWith("group.")) {
                out.add(perm.getPermission().substring(6).toLowerCase(Locale.ROOT));
            }
        });
        if (out.isEmpty()) {
            out.add("default");
        }
        return out;
    }
}

