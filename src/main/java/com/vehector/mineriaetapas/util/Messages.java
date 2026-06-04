package com.vehector.mineriaetapas.util;

import com.vehector.mineriaetapas.MineriaEtapasPlugin;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public final class Messages {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder().character('&').hexCharacter('#').hexColors().useUnusualXRepeatedCharacterHexFormat().build();
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Pattern AMP_HEX = Pattern.compile("&#([0-9A-Fa-f]{6})");
    private static final Pattern MM_TAG = Pattern.compile("<[^<>]+>");

    private Messages() {
    }

    public static Component format(MineriaEtapasPlugin plugin, String key, Map<String, String> placeholders) {
        String raw = plugin.getConfigManager().getMessage(key);
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        return Messages.parse(plugin, raw, placeholders);
    }

    public static void send(CommandSender to, MineriaEtapasPlugin plugin, String key, Map<String, String> placeholders) {
        Component c = Messages.format(plugin, key, placeholders);
        if (c == Component.empty()) {
            return;
        }
        to.sendMessage(c);
    }

    public static Component legacy(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        return Messages.parse(null, raw, null);
    }

    public static Component parse(MineriaEtapasPlugin plugin, String raw, Map<String, String> placeholders) {
        String s = raw;
        if (plugin != null) {
            String prefix = plugin.getConfigManager().getMessage("prefix");
            s = s.replace("%prefix%", (CharSequence)(prefix == null ? "" : prefix));
        }
        if (placeholders != null) {
            for (Map.Entry entry : placeholders.entrySet()) {
                s = s.replace("%" + (String)entry.getKey() + "%", entry.getValue() == null ? "" : (CharSequence)entry.getValue());
            }
        }
        boolean hasMM = MM_TAG.matcher(s).find();
        s = Messages.expandAmpHex(s);
        if (hasMM) {
            TextComponent textComponent = LEGACY.deserialize(s);
            try {
                Component mmComp = MM.deserialize(s);
                String legacyFromMM = LegacyComponentSerializer.legacyAmpersand().serialize(mmComp);
                return LEGACY.deserialize(legacyFromMM).decoration(TextDecoration.ITALIC, mmComp.decoration(TextDecoration.ITALIC));
            }
            catch (Exception ex) {
                return textComponent;
            }
        }
        return LEGACY.deserialize(s);
    }

    private static String expandAmpHex(String input) {
        if (input.indexOf("&#") < 0) {
            return input;
        }
        Matcher m = AMP_HEX.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String hex = m.group(1);
            StringBuilder rep = new StringBuilder("&x");
            for (char c : hex.toCharArray()) {
                rep.append('&').append(c);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(rep.toString()));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}

