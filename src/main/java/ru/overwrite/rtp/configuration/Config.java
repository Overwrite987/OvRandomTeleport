package ru.overwrite.rtp.configuration;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Biome;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import ru.overwrite.rtp.Main;
import ru.overwrite.rtp.actions.Action;
import ru.overwrite.rtp.channels.ChannelTemplate;
import ru.overwrite.rtp.channels.settings.*;
import ru.overwrite.rtp.configuration.data.CommandMessages;
import ru.overwrite.rtp.configuration.data.PlaceholderMessages;
import ru.overwrite.rtp.utils.TimedExpiringMap;
import ru.overwrite.rtp.utils.Utils;
import ru.overwrite.rtp.utils.VersionUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Getter
public class Config {

    @Getter(AccessLevel.NONE)
    private final Main plugin;

    public Config(Main plugin) {
        this.plugin = plugin;
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
            Costs costs = setupTemplateCosts(templateSection.getConfigurationSection("costs"));
            LocationGenOptions locationGenOptions = setupTemplateGenOptions(templateSection.getConfigurationSection("location_generation_options"));
            Cooldown cooldown = setupTemplateCooldown(templateSection.getConfigurationSection("cooldown"));
            Bossbar bossBar = setupTemplateBossBar(templateSection.getConfigurationSection("bossbar"));
            Particles particles = setupTemplateParticles(templateSection.getConfigurationSection("particles"));
            Restrictions restrictions = setupTemplateRestrictions(templateSection.getConfigurationSection("restrictions"));
            Avoidance avoidance = setupTemplateAvoidance(templateSection.getConfigurationSection("avoid"), Bukkit.getPluginManager());
            Actions actions = setupTemplateActions(templateSection.getConfigurationSection("actions"));
            ChannelTemplate newTemplate = new ChannelTemplate(templateID, costs, locationGenOptions, cooldown, bossBar, particles, restrictions, avoidance, actions);
            channelTemplates.put(templateID, newTemplate);
        }
    }

    private Costs setupTemplateCosts(ConfigurationSection channelCosts) {
        if (isNullSection(channelCosts)) {
            return null;
        }
        Costs.MoneyType moneyType = Costs.MoneyType.valueOf(channelCosts.getString("money_type", "VAULT").toUpperCase(Locale.ENGLISH));
        double moneyCost = channelCosts.getDouble("money_cost", -1);
        int hungerCost = channelCosts.getInt("hunger_cost", -1);
        int expCost = channelCosts.getInt("experience_cost", -1);

        return new Costs(plugin.getEconomy(), moneyType, moneyCost, hungerCost, expCost);
    }

    private LocationGenOptions setupTemplateGenOptions(ConfigurationSection locationGenOptions) {
        if (locationGenOptions == null) {
            return null;
        }
        LocationGenOptions.Shape shape = LocationGenOptions.Shape.valueOf(locationGenOptions.getString("shape", "SQUARE").toUpperCase(Locale.ENGLISH));
        LocationGenOptions.GenFormat genFormat = LocationGenOptions.GenFormat.valueOf(locationGenOptions.getString("gen_format", "RECTANGULAR").toUpperCase(Locale.ENGLISH));
        int minX = locationGenOptions.getInt("min_x");
        int maxX = locationGenOptions.getInt("max_x");
        int minZ = locationGenOptions.getInt("min_z");
        int maxZ = locationGenOptions.getInt("max_z");
        int nearRadiusMin = locationGenOptions.getInt("min_near_point_distance", 30);
        int nearRadiusMax = locationGenOptions.getInt("max_near_point_distance", 60);
        int centerX = locationGenOptions.getInt("center_x", 0);
        int centerZ = locationGenOptions.getInt("center_z", 0);
        int maxLocationAttempts = locationGenOptions.getInt("max_location_attempts", 50);

        return new LocationGenOptions(shape, genFormat, minX, maxX, minZ, maxZ, nearRadiusMin, nearRadiusMax, centerX, centerZ, maxLocationAttempts);
    }

    private Cooldown setupTemplateCooldown(ConfigurationSection cooldown) {
        Object2IntSortedMap<String> groupCooldownsMap = new Object2IntLinkedOpenHashMap<>();
        Object2IntSortedMap<String> preTeleportCooldownsMap = new Object2IntLinkedOpenHashMap<>();
        if (isNullSection(cooldown)) {
            return null;
        }
        int defaultCooldown = cooldown.getInt("default_cooldown", -1);
        TimedExpiringMap<String, Long> playerCooldowns = defaultCooldown > 0 ? new TimedExpiringMap<>(TimeUnit.SECONDS) : null;
        final ConfigurationSection groupCooldowns = cooldown.getConfigurationSection("group_cooldowns");
        boolean useLastGroupCooldown = cooldown.getBoolean("use_last_group_cooldown", false);
        if (!isNullSection(groupCooldowns) && plugin.getPerms() != null) {
            for (String groupName : groupCooldowns.getKeys(false)) {
                int cd = groupCooldowns.getInt(groupName);
                groupCooldownsMap.put(groupName, cd);
            }
        }
        if (!groupCooldownsMap.isEmpty()) {
            defaultCooldown = useLastGroupCooldown
                    ? groupCooldowns.getInt(new ArrayList<>(groupCooldownsMap.keySet()).get(groupCooldownsMap.size() - 1))
                    : defaultCooldown;
        }
        int defaultPreTeleportCooldown = cooldown.getInt("default_teleport_cooldown", -1);
        final ConfigurationSection preTeleportGroupCooldowns = cooldown.getConfigurationSection("pre_teleport_group_cooldowns");
        if (!isNullSection(preTeleportGroupCooldowns) && plugin.getPerms() != null) {
            for (String groupName : preTeleportGroupCooldowns.getKeys(false)) {
                int cd = preTeleportGroupCooldowns.getInt(groupName);
                preTeleportCooldownsMap.put(groupName, cd);
            }
        }
        if (!groupCooldownsMap.isEmpty()) {
            defaultPreTeleportCooldown = useLastGroupCooldown
                    ? groupCooldowns.getInt(new ArrayList<>(preTeleportCooldownsMap.keySet()).get(preTeleportCooldownsMap.size() - 1))
                    : defaultPreTeleportCooldown;
        }
        return new Cooldown(defaultCooldown, playerCooldowns, groupCooldownsMap, defaultPreTeleportCooldown, preTeleportCooldownsMap);
    }

    private Bossbar setupTemplateBossBar(ConfigurationSection bossbar) {
        if (isNullSection(bossbar)) {
            return null;
        }
        boolean enabled = bossbar.getBoolean("enabled");
        String title = Utils.COLORIZER.colorize(bossbar.getString("title"));
        BarColor color = BarColor.valueOf(bossbar.getString("color").toUpperCase(Locale.ENGLISH));
        BarStyle style = BarStyle.valueOf(bossbar.getString("style").toUpperCase(Locale.ENGLISH));

        return new Bossbar(enabled, title, color, style);
    }

    private Particles setupTemplateParticles(ConfigurationSection particles) {
        if (isNullSection(particles)) {
            return null;
        }
        boolean preTeleportEnabled = false;
        boolean preTeleportSendOnlyToPlayer = false;
        Particle preTeleportId = null;
        int preTeleportDots = 0;
        double preTeleportRadius = 0;
        double preTeleportParticleSpeed = 0;
        double preTeleportSpeed = 0;
        boolean preTeleportInvert = false;
        boolean preTeleportJumping = false;
        boolean preTeleportMoveNear = false;
        boolean afterTeleportParticleEnabled = false;
        boolean afterTeleportSendOnlyToPlayer = false;
        Particle afterTeleportParticle = null;
        int afterTeleportCount = 0;
        double afterTeleportRadius = 0;
        double afterTeleportParticleSpeed = 0;
        final ConfigurationSection preTeleport = particles.getConfigurationSection("pre_teleport");
        if (!isNullSection(preTeleport)) {
            preTeleportEnabled = preTeleport.getBoolean("enabled", false);
            preTeleportSendOnlyToPlayer = preTeleport.getBoolean("send_only_to_player", false);
            preTeleportId = Particle.valueOf(preTeleport.getString("id").toUpperCase(Locale.ENGLISH));
            preTeleportDots = preTeleport.getInt("dots");
            preTeleportRadius = preTeleport.getDouble("radius");
            preTeleportParticleSpeed = preTeleport.getDouble("particle_speed");
            preTeleportSpeed = preTeleport.getDouble("speed");
            preTeleportInvert = preTeleport.getBoolean("invert");
            preTeleportJumping = preTeleport.getBoolean("jumping");
            preTeleportMoveNear = preTeleport.getBoolean("move_near");
        }
        final ConfigurationSection afterTeleport = particles.getConfigurationSection("after_teleport");
        if (!isNullSection(afterTeleport)) {
            afterTeleportParticleEnabled = afterTeleport.getBoolean("enabled", false);
            afterTeleportSendOnlyToPlayer = afterTeleport.getBoolean("send_only_to_player", false);
            afterTeleportParticle = Particle.valueOf(afterTeleport.getString("id").toUpperCase(Locale.ENGLISH));
            afterTeleportCount = afterTeleport.getInt("count");
            afterTeleportRadius = afterTeleport.getDouble("radius");
            afterTeleportParticleSpeed = afterTeleport.getDouble("particle_speed");
        }

        return new Particles(
                preTeleportEnabled, preTeleportSendOnlyToPlayer, preTeleportId, preTeleportDots, preTeleportRadius, preTeleportParticleSpeed, preTeleportSpeed, preTeleportInvert, preTeleportJumping, preTeleportMoveNear,
                afterTeleportParticleEnabled, afterTeleportSendOnlyToPlayer, afterTeleportParticle, afterTeleportCount, afterTeleportRadius, afterTeleportParticleSpeed);
    }

    private Restrictions setupTemplateRestrictions(ConfigurationSection restrictions) {
        if (isNullSection(restrictions)) {
            return null;
        }

        return new Restrictions(
                restrictions.getBoolean("move", false),
                restrictions.getBoolean("teleport", false),
                restrictions.getBoolean("damage", false),
                restrictions.getBoolean("damage_others", false),
                restrictions.getBoolean("damage_check_only_players", false));
    }

    private Avoidance setupTemplateAvoidance(ConfigurationSection avoid, PluginManager pluginManager) {
        if (isNullSection(avoid)) {
            return null;
        }
        Set<Material> avoidBlocks = EnumSet.noneOf(Material.class);
        boolean avoidBlocksBlacklist;
        avoidBlocksBlacklist = avoid.getBoolean("blocks.blacklist", true);
        for (String material : avoid.getStringList("blocks.list")) {
            avoidBlocks.add(Material.valueOf(material.toUpperCase(Locale.ENGLISH)));
        }
        Set<Biome> avoidBiomes = VersionUtils.SUB_VERSION > 20 ? new HashSet<>() : EnumSet.noneOf(Biome.class);
        boolean avoidBiomesBlacklist;
        avoidBiomesBlacklist = avoid.getBoolean("biomes.blacklist", true);
        for (String biome : avoid.getStringList("biomes.list")) {
            avoidBiomes.add(Biome.valueOf(biome.toUpperCase(Locale.ENGLISH)));
        }
        boolean avoidRegions = avoid.getBoolean("regions", false) && pluginManager.isPluginEnabled("WorldGuard");
        boolean avoidTowns = avoid.getBoolean("towns", false) && pluginManager.isPluginEnabled("Towny");

        return new Avoidance(avoidBlocksBlacklist, avoidBlocks, avoidBiomesBlacklist, avoidBiomes, avoidRegions, avoidTowns);
    }

    private Actions setupTemplateActions(ConfigurationSection actions) {
        if (isNullSection(actions)) {
            return null;
        }
        List<Action> preTeleportActions = plugin.getRtpManager().getActionList(actions.getStringList("pre_teleport"));
        Int2ObjectMap<List<Action>> onCooldownActions = new Int2ObjectOpenHashMap<>();
        final ConfigurationSection cooldownActions = actions.getConfigurationSection("on_cooldown");
        if (!isNullSection(cooldownActions)) {
            for (String actionId : cooldownActions.getKeys(false)) {
                if (!Utils.isNumeric(actionId)) {
                    continue;
                }
                int time = Integer.parseInt(actionId);
                List<Action> actionList = plugin.getRtpManager().getActionList(cooldownActions.getStringList(actionId));
                onCooldownActions.put(time, actionList);
            }
        }
        List<Action> afterTeleportActions = plugin.getRtpManager().getActionList(actions.getStringList("after_teleport"));

        return new Actions(preTeleportActions, onCooldownActions, afterTeleportActions);
    }

    public boolean isNullSection(ConfigurationSection section) {
        return section == null;
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
