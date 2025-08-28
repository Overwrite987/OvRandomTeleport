package ru.overwrite.rtp.channels.settings;

import org.bukkit.configuration.ConfigurationSection;
import ru.overwrite.rtp.configuration.Config;

public record Messages(
        String noPerms,
        String invalidWorld,
        String notEnoughPlayers,
        String notEnoughMoney,
        String notEnoughHunger,
        String notEnoughExp,
        String cooldown,
        String movedOnTeleport,
        String teleportedOnTeleport,
        String damagedOnTeleport,
        String damagedOtherOnTeleport,
        String failToFindLocation) {

    public static Messages create(ConfigurationSection messages, Config pluginConfig) {
        Messages defaultMessages = pluginConfig.getDefaultChannelMessages();
        if (pluginConfig.isNullSection(messages)) {
            return defaultMessages;
        }
        String prefix = pluginConfig.isConfigValueExist(messages, "prefix") ? messages.getString("prefix") : pluginConfig.getMessagesPrefix();
        String noPerms = getMessage(messages, pluginConfig, "no_perms", defaultMessages.noPerms(), prefix);
        String invalidWorld = getMessage(messages, pluginConfig, "invalid_world", defaultMessages.invalidWorld(), prefix);
        String notEnoughPlayers = getMessage(messages, pluginConfig, "not_enough_players", defaultMessages.notEnoughPlayers(), prefix);
        String notEnoughMoney = getMessage(messages, pluginConfig, "not_enough_money", defaultMessages.notEnoughMoney(), prefix);
        String notEnoughHunger = getMessage(messages, pluginConfig, "not_enough_hunger", defaultMessages.notEnoughHunger(), prefix);
        String notEnoughExp = getMessage(messages, pluginConfig, "not_enough_experience", defaultMessages.notEnoughExp(), prefix);
        String cooldown = getMessage(messages, pluginConfig, "cooldown", defaultMessages.cooldown(), prefix);
        String movedOnTeleport = getMessage(messages, pluginConfig, "moved_on_teleport", defaultMessages.movedOnTeleport(), prefix);
        String teleportedOnTeleport = getMessage(messages, pluginConfig, "teleported_on_teleport", defaultMessages.teleportedOnTeleport(), prefix);
        String damagedOnTeleport = getMessage(messages, pluginConfig, "damaged_on_teleport", defaultMessages.damagedOnTeleport(), prefix);
        String damagedOtherOnTeleport = getMessage(messages, pluginConfig, "damaged_other_on_teleport", defaultMessages.damagedOtherOnTeleport(), prefix);
        String failToFindLocation = getMessage(messages, pluginConfig, "fail_to_find_location", defaultMessages.failToFindLocation(), prefix);

        return new Messages(
                noPerms,
                invalidWorld,
                notEnoughPlayers,
                notEnoughMoney,
                notEnoughHunger,
                notEnoughExp,
                cooldown,
                movedOnTeleport,
                teleportedOnTeleport,
                damagedOnTeleport,
                damagedOtherOnTeleport,
                failToFindLocation
        );
    }

    private static String getMessage(ConfigurationSection messages, Config pluginConfig, String key, String global, String prefix) {
        return pluginConfig.isConfigValueExist(messages, key) ? pluginConfig.getPrefixed(messages.getString(key), prefix) : global;
    }
}
