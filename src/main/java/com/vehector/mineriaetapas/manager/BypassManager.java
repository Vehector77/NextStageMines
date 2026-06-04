package com.vehector.mineriaetapas.manager;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

/**
 * Gestiona el estado del bypass por jugador (en memoria).
 *
 * Reglas:
 *  - Un jugador SOLO puede usar el bypass si tiene alguno de los permisos de staff
 *    (mineriaetapas.bypass, mineriaetapas.admin, mineriaetapas.staff) o es OP.
 *  - Si tiene permiso, por defecto el bypass está ACTIVO.
 *  - Con /etapas bypass off lo desactiva temporalmente (para testear progresión).
 *  - Con /etapas bypass on lo vuelve a activar.
 *  - El estado se pierde al reiniciar el servidor o al reconectar el jugador (Es a propósito,
 *    así un staff nunca queda accidentalmente sin proteccion permanente).
 */
public final class BypassManager {
    private final Set<UUID> disabledFor = ConcurrentHashMap.newKeySet();

    public boolean hasPermission(Player player) {
        if (player == null) return false;
        return player.hasPermission("mineriaetapas.bypass")
                || player.hasPermission("mineriaetapas.admin")
                || player.hasPermission("mineriaetapas.staff")
                || player.isOp();
    }

    /** true si el bypass DEBE aplicarse ahora mismo a este jugador. */
    public boolean isBypassing(Player player) {
        if (!hasPermission(player)) return false;
        return !disabledFor.contains(player.getUniqueId());
    }

    /** Devuelve el estado actual (true = activado, false = desactivado). */
    public boolean isEnabled(Player player) {
        return !disabledFor.contains(player.getUniqueId());
    }

    public void setEnabled(Player player, boolean enabled) {
        if (enabled) {
            disabledFor.remove(player.getUniqueId());
        } else {
            disabledFor.add(player.getUniqueId());
        }
    }

    /** Cambia el estado y devuelve el nuevo valor (true = activado). */
    public boolean toggle(Player player) {
        UUID id = player.getUniqueId();
        if (disabledFor.contains(id)) {
            disabledFor.remove(id);
            return true;
        }
        disabledFor.add(id);
        return false;
    }

    public void clearPlayer(UUID uuid) {
        disabledFor.remove(uuid);
    }
}
