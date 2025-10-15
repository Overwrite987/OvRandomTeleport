package ru.overwrite.rtp.channels.settings;

import org.bukkit.configuration.ConfigurationSection;
import ru.overwrite.rtp.configuration.Config;
import ru.overwrite.rtp.utils.Utils;

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
        String failToFindLocation
) {

    public static Messages create(ConfigurationSection messages, Config pluginConfig) {
        Messages defaultMessages = pluginConfig.getDefaultChannelMessages();
        if (messages == null) {
            return defaultMessages;
        }

        String prefix = Utils.COLORIZER.colorize(messages.getString("prefix", pluginConfig.getMessagesPrefix()));

        String noPerms = pluginConfig.getPrefixed(messages.getString("no_perms", defaultMessages.noPerms()), prefix);
        String invalidWorld = pluginConfig.getPrefixed(messages.getString("invalid_world", defaultMessages.invalidWorld()), prefix);
        String notEnoughPlayers = pluginConfig.getPrefixed(messages.getString("not_enough_players", defaultMessages.notEnoughPlayers()), prefix);
        String notEnoughMoney = pluginConfig.getPrefixed(messages.getString("not_enough_money", defaultMessages.notEnoughMoney()), prefix);
        String notEnoughHunger = pluginConfig.getPrefixed(messages.getString("not_enough_hunger", defaultMessages.notEnoughHunger()), prefix);
        String notEnoughExp = pluginConfig.getPrefixed(messages.getString("not_enough_experience", defaultMessages.notEnoughExp()), prefix);
        String cooldown = pluginConfig.getPrefixed(messages.getString("cooldown", defaultMessages.cooldown()), prefix);
        String movedOnTeleport = pluginConfig.getPrefixed(messages.getString("moved_on_teleport", defaultMessages.movedOnTeleport()), prefix);
        String teleportedOnTeleport = pluginConfig.getPrefixed(messages.getString("teleported_on_teleport", defaultMessages.teleportedOnTeleport()), prefix);
        String damagedOnTeleport = pluginConfig.getPrefixed(messages.getString("damaged_on_teleport", defaultMessages.damagedOnTeleport()), prefix);
        String damagedOtherOnTeleport = pluginConfig.getPrefixed(messages.getString("damaged_other_on_teleport", defaultMessages.damagedOtherOnTeleport()), prefix);
        String failToFindLocation = pluginConfig.getPrefixed(messages.getString("fail_to_find_location", defaultMessages.failToFindLocation()), prefix);

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
}