package ru.overwrite.rtp.utils;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.overwrite.rtp.Main;
import ru.overwrite.rtp.channels.settings.Messages;

import java.io.File;

public class Config {

    private final Main plugin;

    public Config(Main plugin) {
        this.plugin = plugin;
    }

    @Getter
    private String messagesPrefix;

    @Getter
    private Messages defaultChannelMessages;

    @Getter
    private CommandMessages commandMessages;

    public record CommandMessages(
            String incorrectChannel,
            String reload,
            String unknownArgument,
            String playerNotFound,
            String adminHelp
    ) {
    }

    @Getter
    private PlaceholderMessages placeholderMessages;

    public record PlaceholderMessages(
            String noCooldown,
            String noValue
    ) {
    }

    public static String timeHours, timeMinutes, timeSeconds;

    public void setupMessages(FileConfiguration config) {
        final ConfigurationSection messages = config.getConfigurationSection("messages");
        messagesPrefix = Utils.COLORIZER.colorize(messages.getString("prefix", "messages.prefix"));

        this.defaultChannelMessages = new Messages(
                getPrefixed(messages.getString("no_perms", "messages.no_perms"), messagesPrefix),
                getPrefixed(messages.getString("invalid_world", "messages.invalid_world"), messagesPrefix),
                getPrefixed(messages.getString("not_enough_players", "messages.not_enough_players"), messagesPrefix),
                getPrefixed(messages.getString("not_enough_money", "messages.not_enough_money"), messagesPrefix),
                getPrefixed(messages.getString("not_enough_hunger", "messages.not_enough_hunger"), messagesPrefix),
                getPrefixed(messages.getString("not_enough_experience", "messages.not_enough_experience"), messagesPrefix),
                getPrefixed(messages.getString("cooldown", "messages.cooldown"), messagesPrefix),
                getPrefixed(messages.getString("moved_on_teleport", "messages.moved_on_teleport"), messagesPrefix),
                getPrefixed(messages.getString("teleported_on_teleport", "messages.teleported_on_teleport"), messagesPrefix),
                getPrefixed(messages.getString("damaged_on_teleport", "messages.damaged_on_teleport"), messagesPrefix),
                getPrefixed(messages.getString("damaged_other_on_teleport", "messages.damaged_other_on_teleport"), messagesPrefix),
                getPrefixed(messages.getString("fail_to_find_location", "messages.fail_to_find_location"), messagesPrefix)
        );

        final ConfigurationSection admin = messages.getConfigurationSection("admin");
        this.commandMessages = new CommandMessages(
                getPrefixed(messages.getString("incorrect_channel", "messages.incorrect_channel"), messagesPrefix),
                getPrefixed(admin.getString("reload"), messagesPrefix),
                getPrefixed(admin.getString("unknown_argument"), messagesPrefix),
                getPrefixed(admin.getString("player_not_found"), messagesPrefix),
                getPrefixed(admin.getString("admin_help"), messagesPrefix)
        );

        final ConfigurationSection placeholders = messages.getConfigurationSection("placeholders");
        this.placeholderMessages = new PlaceholderMessages(
                Utils.COLORIZER.colorize(placeholders.getString("no_cooldown", "&aКулдаун отсутствует!")),
                Utils.COLORIZER.colorize(placeholders.getString("no_value", "&cОтсутствует"))
        );

        final ConfigurationSection time = placeholders.getConfigurationSection("time");
        timeHours = Utils.COLORIZER.colorize(time.getString("hours", " ч."));
        timeMinutes = Utils.COLORIZER.colorize(time.getString("minutes", " мин."));
        timeSeconds = Utils.COLORIZER.colorize(time.getString("seconds", " сек."));
    }


    public String getPrefixed(String message, String prefix) {
        if (message == null || prefix == null) {
            return message;
        }
        return Utils.COLORIZER.colorize(message.replace("%prefix%", prefix));
    }

    public FileConfiguration getFile(String path, String fileName) {
        File file = new File(path, fileName);
        if (!file.exists()) {
            plugin.saveResource("channels/" + fileName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

}
