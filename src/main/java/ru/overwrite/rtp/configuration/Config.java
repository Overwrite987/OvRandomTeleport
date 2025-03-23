package ru.overwrite.rtp.configuration;

import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.overwrite.rtp.Main;
import ru.overwrite.rtp.RtpManager;
import ru.overwrite.rtp.channels.ChannelTemplate;
import ru.overwrite.rtp.channels.settings.*;
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
    private final Main plugin;
    @Getter(AccessLevel.NONE)
    private final RtpManager rtpManager;

    public Config(Main plugin) {
        this.plugin = plugin;
        this.rtpManager = plugin.getRtpManager();
    }

    private String messagesPrefix;

    private Messages defaultChannelMessages;

    private CommandMessages commandMessages;

    private PlaceholderMessages placeholderMessages;

    public static String timeHours, timeMinutes, timeSeconds;

    private final Map<String, ChannelTemplate> channelTemplates = new HashMap<>();

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
            Costs costs = rtpManager.setupChannelCosts(templateSection.getConfigurationSection("costs"), null, false);
            LocationGenOptions locationGenOptions = rtpManager.setupChannelGenOptions(templateSection.getConfigurationSection("location_generation_options"), null, false);
            Cooldown cooldown = rtpManager.setupChannelCooldown(templateSection.getConfigurationSection("cooldown"), null, false);
            Bossbar bossBar = rtpManager.setupChannelBossBar(templateSection.getConfigurationSection("bossbar"), null, false);
            Particles particles = rtpManager.setupChannelParticles(templateSection.getConfigurationSection("particles"), null, false);
            Restrictions restrictions = rtpManager.setupChannelRestrictions(templateSection.getConfigurationSection("restrictions"), null, false);
            Avoidance avoidance = rtpManager.setupChannelAvoidance(templateSection.getConfigurationSection("avoid"), Bukkit.getPluginManager(), null, false);
            Actions actions = rtpManager.setupChannelActions(templateSection.getConfigurationSection("actions"), null, false);
            ChannelTemplate newTemplate = new ChannelTemplate(templateID, costs, locationGenOptions, cooldown, bossBar, particles, restrictions, avoidance, actions);
            channelTemplates.put(templateID, newTemplate);
        }
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
