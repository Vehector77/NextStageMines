package com.vehector.mineriaetapas.config;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class RegionAccess {
    private final String name;
    private final Set<String> allowedGroups;
    private final String denyMessage;

    public RegionAccess(String name, Set<String> allowedGroups, String denyMessage) {
        this.name = name;
        LinkedHashSet<String> normalized = new LinkedHashSet<String>();
        if (allowedGroups != null) {
            for (String g : allowedGroups) {
                if (g == null) continue;
                normalized.add(g.toLowerCase(Locale.ROOT).trim());
            }
        }
        this.allowedGroups = Collections.unmodifiableSet(normalized);
        this.denyMessage = denyMessage == null ? "" : denyMessage;
    }

    public String getName() {
        return this.name;
    }

    public Set<String> getAllowedGroups() {
        return this.allowedGroups;
    }

    public String getDenyMessage() {
        return this.denyMessage;
    }

    public boolean allows(String group) {
        if (this.allowedGroups.isEmpty()) {
            return true;
        }
        if (group == null) {
            return false;
        }
        return this.allowedGroups.contains(group.toLowerCase(Locale.ROOT));
    }

    public boolean allowsAny(Iterable<String> groups) {
        if (this.allowedGroups.isEmpty()) {
            return true;
        }
        if (groups == null) {
            return false;
        }
        for (String g : groups) {
            if (g == null || !this.allowedGroups.contains(g.toLowerCase(Locale.ROOT))) continue;
            return true;
        }
        return false;
    }
}

