package ru.overwrite.rtp.utils;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.overwrite.rtp.Main;

import java.io.File;

public class Config {

    private final Main plugin;

    public Config(Main plugin) {
        this.plugin = plugin;
    }

    public String messages_prefix,
            messages_no_perms,
            messages_invalid_world,
            messages_not_enough_players,
            messages_not_enough_money,
            messages_not_enough_hunger,
            messages_not_enough_exp,
            messages_cooldown,
            messages_incorrect_channel,
            messages_moved_on_teleport,
            messages_teleported_on_teleport,
            messages_damaged_on_teleport,
            messages_damaged_other_on_teleport,
            messages_fail_to_find_location,
            messages_already_teleporting,
            messages_reload,
            messages_unknown_argument,
            messages_player_not_found,
            messages_admin_help;

    public static String time_hours, time_minutes, time_seconds, papi_nocooldown;

    public void setupMessages(FileConfiguration config) {
        final ConfigurationSection messages = config.getConfigurationSection("messages");
        messages_prefix = Utils.COLORIZER.colorize(messages.getString("prefix", "messages.prefix"));
        messages_no_perms = getPrefixed(messages.getString("no_perms", "messages.no_perms"), messages_prefix);
        messages_invalid_world = getPrefixed(messages.getString("invalid_world", "messages.invalid_world"), messages_prefix);
        messages_not_enough_players = getPrefixed(messages.getString("not_enough_players", "messages.not_enough_players"), messages_prefix);
        messages_not_enough_money = getPrefixed(messages.getString("not_enough_money", "messages.not_enough_money"), messages_prefix);
        messages_not_enough_hunger = getPrefixed(messages.getString("not_enough_hunger", "messages.not_enough_hunger"), messages_prefix);
        messages_not_enough_exp = getPrefixed(messages.getString("not_enough_experience", "messages.not_enough_experience"), messages_prefix);
        messages_cooldown = getPrefixed(messages.getString("cooldown", "messages.cooldown"), messages_prefix);
        messages_incorrect_channel = getPrefixed(messages.getString("incorrect_channel", "messages.incorrect_channel"), messages_prefix);
        messages_moved_on_teleport = getPrefixed(messages.getString("moved_on_teleport", "messages.moved_on_teleport"), messages_prefix);
        messages_teleported_on_teleport = getPrefixed(messages.getString("teleported_on_teleport", "messages.teleported_on_teleport"), messages_prefix);
        messages_damaged_on_teleport = getPrefixed(messages.getString("damaged_on_teleport", "messages.damaged_on_teleport"), messages_prefix);
        messages_damaged_other_on_teleport = getPrefixed(messages.getString("damaged_other_on_teleport", "messages.damaged_other_on_teleport"), messages_prefix);
        messages_fail_to_find_location = getPrefixed(messages.getString("fail_to_find_location", "messages.fail_to_find_location"), messages_prefix);
        messages_already_teleporting = getPrefixed(messages.getString("already_teleporting", "messages.already_teleporting"), messages_prefix);
        final ConfigurationSection admin = messages.getConfigurationSection("admin");
        messages_reload = getPrefixed(admin.getString("reload"), messages_prefix);
        messages_unknown_argument = getPrefixed(admin.getString("unknown_argument"), messages_prefix);
        messages_player_not_found = getPrefixed(admin.getString("player_not_found"), messages_prefix);
        messages_admin_help = getPrefixed(admin.getString("admin_help"), messages_prefix);
        final ConfigurationSection placeholders = messages.getConfigurationSection("placeholders");
        papi_nocooldown = Utils.COLORIZER.colorize(placeholders.getString("no_cooldown"));
        final ConfigurationSection time = placeholders.getConfigurationSection("time");
        time_hours = Utils.COLORIZER.colorize(time.getString("hours", " ч."));
        time_minutes = Utils.COLORIZER.colorize(time.getString("minutes", " мин."));
        time_seconds = Utils.COLORIZER.colorize(time.getString("seconds", " сек."));
    }

    public String getPrefixed(String message, String prefix) {
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
