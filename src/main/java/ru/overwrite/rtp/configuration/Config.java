package ru.overwrite.rtp.configuration;

import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.overwrite.rtp.OvRandomTeleport;
import ru.overwrite.rtp.RtpManager;
import ru.overwrite.rtp.channels.Settings;
import ru.overwrite.rtp.channels.settings.Messages;
import ru.overwrite.rtp.configuration.data.CommandMessages;
import ru.overwrite.rtp.configuration.data.PlaceholderMessages;
import ru.overwrite.rtp.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class Config {

    @Getter(AccessLevel.NONE)
    private final OvRandomTeleport plugin;
    @Getter(AccessLevel.NONE)
    private final RtpManager rtpManager;

    public Config(OvRandomTeleport plugin) {
        this.plugin = plugin;
        this.rtpManager = plugin.getRtpManager();
    }

    private String messagesPrefix;

    private Messages defaultChannelMessages;

    private CommandMessages commandMessages;

    private PlaceholderMessages placeholderMessages;

    public static String timeHours, timeMinutes, timeSeconds;

    private final Map<String, Settings> channelTemplates = new HashMap<>();

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
                getPrefixed(messages.getString("channel_not_specified", "messages.channel_not_specified"), messagesPrefix),
                getPrefixed(messages.getString("canceled", "messages.canceled"), messagesPrefix),
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

    public void setupTemplates() {
        final FileConfiguration templatesConfig = getFile(plugin.getDataFolder().getAbsolutePath(), "templates.yml");
        for (String templateID : templatesConfig.getKeys(false)) {
            final ConfigurationSection templateSection = templatesConfig.getConfigurationSection(templateID);
            Settings newTemplate = Settings.create(plugin, templateSection, this, null, false);
            channelTemplates.put(templateID, newTemplate);
        }
    }

    public boolean isConfigValueExist(ConfigurationSection section, String key) {
        return section.getString(key) != null;
    }

    public boolean isNullSection(ConfigurationSection section) {
        return section == null;
    }

    public List<String> getStringListInAnyCase(Object raw) {
        List<String> stringList = new ArrayList<>();
        if (raw instanceof String singleId) {
            stringList.add(singleId);
        } else if (raw instanceof List<?> listIds) {
            for (Object obj : listIds) {
                if (obj instanceof String id) {
                    stringList.add(id);
                }
            }
        }
        return stringList;
    }

    public FileConfiguration getChannelFile(String path, String fileName) {
        File file = new File(path, fileName);
        if (!file.exists()) {
            plugin.saveResource("channels/" + fileName, false);
            plugin.getPluginLogger().warn("Channel file with name " + fileName + " does not exist.");
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getFile(String path, String fileName) {
        File file = new File(path, fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

}
