package ru.overwrite.rtp.channels;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.PluginManager;
import ru.overwrite.rtp.OvRandomTeleport;
import ru.overwrite.rtp.actions.Action;
import ru.overwrite.rtp.actions.ActionRegistry;
import ru.overwrite.rtp.channels.settings.*;
import ru.overwrite.rtp.configuration.Config;
import ru.overwrite.rtp.utils.TimedExpiringMap;
import ru.overwrite.rtp.utils.Utils;
import ru.overwrite.rtp.utils.VersionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

public record Settings(
        Costs costs,
        LocationGenOptions locationGenOptions,
        Cooldown cooldown,
        Bossbar bossbar,
        Particles particles,
        Restrictions restrictions,
        Avoidance avoidance,
        Actions actions) {

    public static Settings create(OvRandomTeleport plugin, ConfigurationSection config, Config pluginConfig, Settings template, boolean applyTemplate) {
        return new Settings(
                setupCosts(plugin, config.getConfigurationSection("costs"), template, pluginConfig, applyTemplate),
                setupLocationGenOptions(config.getConfigurationSection("location_generation_options"), template, pluginConfig, applyTemplate),
                setupCooldown(plugin, config.getConfigurationSection("cooldown"), template, pluginConfig, applyTemplate),
                setupBossBar(config.getConfigurationSection("bossbar"), template, pluginConfig, applyTemplate),
                setupParticles(config.getConfigurationSection("particles"), template, pluginConfig, applyTemplate),
                setupRestrictions(config.getConfigurationSection("restrictions"), template, pluginConfig, applyTemplate),
                setupAvoidance(config.getConfigurationSection("avoid"), Bukkit.getPluginManager(), template, pluginConfig, applyTemplate),
                setupActions(plugin, config.getConfigurationSection("actions"), template, pluginConfig, applyTemplate)
        );
    }

    public static Costs setupCosts(OvRandomTeleport plugin, ConfigurationSection channelCosts, Settings template, Config pluginConfig, boolean applyTemplate) {
        if (pluginConfig.isNullSection(channelCosts)) {
            if (!applyTemplate) {
                return null;
            }
            return template != null && template.costs() != null
                    ? template.costs()
                    : new Costs(null, null, -1, -1, -1);
        }
        Costs.MoneyType moneyType = Costs.MoneyType.valueOf(channelCosts.getString("money_type", "VAULT").toUpperCase(Locale.ENGLISH));
        double moneyCost = channelCosts.getDouble("money_cost", -1);
        int hungerCost = channelCosts.getInt("hunger_cost", -1);
        int expCost = channelCosts.getInt("experience_cost", -1);

        return new Costs(plugin.getEconomy(), moneyType, moneyCost, hungerCost, expCost);
    }

    public static LocationGenOptions setupLocationGenOptions(ConfigurationSection locationGenOptions, Settings template, Config pluginConfig, boolean applyTemplate) {
        if (pluginConfig.isNullSection(locationGenOptions)) {
            if (!applyTemplate || template == null || template.locationGenOptions() == null) {
                return null;
            }
            return template.locationGenOptions();
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

    public static Cooldown setupCooldown(OvRandomTeleport plugin, ConfigurationSection cooldown, Settings template, Config pluginConfig, boolean applyTemplate) {
        Object2IntSortedMap<String> groupCooldownsMap = new Object2IntLinkedOpenHashMap<>();
        Object2IntSortedMap<String> preTeleportCooldownsMap = new Object2IntLinkedOpenHashMap<>();
        if (pluginConfig.isNullSection(cooldown)) {
            if (!applyTemplate) {
                return null;
            }
            return template != null && template.cooldown() != null
                    ? template.cooldown()
                    : new Cooldown(-1, null, groupCooldownsMap, -1, preTeleportCooldownsMap);
        }
        int defaultCooldown = cooldown.getInt("default_cooldown", -1);
        TimedExpiringMap<String, Long> playerCooldowns = defaultCooldown > 0 ? new TimedExpiringMap<>(TimeUnit.SECONDS) : null;

        boolean useLastGroupCooldown = cooldown.getBoolean("use_last_group_cooldown", false);
        defaultCooldown = processCooldownSection(plugin, cooldown.getConfigurationSection("group_cooldowns"), groupCooldownsMap, useLastGroupCooldown, defaultCooldown, pluginConfig);

        int defaultPreTeleportCooldown = cooldown.getInt("default_pre_teleport_cooldown", -1);
        defaultPreTeleportCooldown = processCooldownSection(plugin, cooldown.getConfigurationSection("pre_teleport_group_cooldowns"), preTeleportCooldownsMap, useLastGroupCooldown, defaultPreTeleportCooldown, pluginConfig);

        return new Cooldown(defaultCooldown, playerCooldowns, groupCooldownsMap, defaultPreTeleportCooldown, preTeleportCooldownsMap);
    }

    private static int processCooldownSection(OvRandomTeleport plugin, ConfigurationSection section, Object2IntSortedMap<String> map, boolean useLastGroup, int currentDefault, Config pluginConfig) {
        if (!pluginConfig.isNullSection(section) && plugin.getPerms() != null) {
            for (String groupName : section.getKeys(false)) {
                map.put(groupName, section.getInt(groupName));
            }
            if (!map.isEmpty() && useLastGroup) {
                List<String> keys = new ArrayList<>(map.keySet());
                currentDefault = section.getInt(keys.get(keys.size() - 1));
            }
        }
        return currentDefault;
    }

    public static Bossbar setupBossBar(ConfigurationSection bossbar, Settings template, Config pluginConfig, boolean applyTemplate) {
        if (pluginConfig.isNullSection(bossbar)) {
            if (!applyTemplate) {
                return null;
            }
            return template != null && template.bossbar() != null
                    ? template.bossbar()
                    : new Bossbar(false, null, null, null);
        }
        boolean enabled = bossbar.getBoolean("enabled");
        String title = Utils.COLORIZER.colorize(bossbar.getString("title"));
        BarColor color = BarColor.valueOf(bossbar.getString("color").toUpperCase(Locale.ENGLISH));
        BarStyle style = BarStyle.valueOf(bossbar.getString("style").toUpperCase(Locale.ENGLISH));

        return new Bossbar(enabled, title, color, style);
    }

    public static Particles setupParticles(ConfigurationSection particles, Settings template, Config pluginConfig, boolean applyTemplate) {
        if (pluginConfig.isNullSection(particles)) {
            if (!applyTemplate) {
                return null;
            }
            return template != null && template.particles() != null
                    ? template.particles()
                    : new Particles(false, false, null, -1, -1, -1, -1, false, false, false,
                    false, false, null, -1, -1, -1);
        }
        boolean preTeleportEnabled = false;
        boolean preTeleportSendOnlyToPlayer = false;
        List<Particles.ParticleData> preTeleportParticles = null;
        int preTeleportDots = 0;
        double preTeleportRadius = 0;
        double preTeleportParticleSpeed = 0;
        double preTeleportSpeed = 0;
        boolean preTeleportInvert = false;
        boolean preTeleportJumping = false;
        boolean preTeleportMoveNear = false;
        boolean afterTeleportParticleEnabled = false;
        boolean afterTeleportSendOnlyToPlayer = false;
        Particles.ParticleData afterTeleportParticle = null;
        int afterTeleportCount = 0;
        double afterTeleportRadius = 0;
        double afterTeleportParticleSpeed = 0;
        final ConfigurationSection preTeleport = particles.getConfigurationSection("pre_teleport");
        if (!pluginConfig.isNullSection(preTeleport)) {
            preTeleportEnabled = preTeleport.getBoolean("enabled", false);
            preTeleportSendOnlyToPlayer = preTeleport.getBoolean("send_only_to_player", false);
            preTeleportParticles = ImmutableList.copyOf(pluginConfig.getStringListInAnyCase(preTeleport.get("id")).stream().map(Utils::createParticleData).toList());
            preTeleportDots = preTeleport.getInt("dots");
            preTeleportRadius = preTeleport.getDouble("radius");
            preTeleportParticleSpeed = preTeleport.getDouble("particle_speed");
            preTeleportSpeed = preTeleport.getDouble("speed");
            preTeleportInvert = preTeleport.getBoolean("invert");
            preTeleportJumping = preTeleport.getBoolean("jumping");
            preTeleportMoveNear = preTeleport.getBoolean("move_near");
        }
        final ConfigurationSection afterTeleport = particles.getConfigurationSection("after_teleport");
        if (!pluginConfig.isNullSection(afterTeleport)) {
            afterTeleportParticleEnabled = afterTeleport.getBoolean("enabled", false);
            afterTeleportSendOnlyToPlayer = afterTeleport.getBoolean("send_only_to_player", false);
            afterTeleportParticle = Utils.createParticleData(afterTeleport.getString("id"));
            afterTeleportCount = afterTeleport.getInt("count");
            afterTeleportRadius = afterTeleport.getDouble("radius");
            afterTeleportParticleSpeed = afterTeleport.getDouble("particle_speed");
        }

        return new Particles(
                preTeleportEnabled, preTeleportSendOnlyToPlayer, preTeleportParticles, preTeleportDots, preTeleportRadius, preTeleportParticleSpeed, preTeleportSpeed, preTeleportInvert, preTeleportJumping, preTeleportMoveNear,
                afterTeleportParticleEnabled, afterTeleportSendOnlyToPlayer, afterTeleportParticle, afterTeleportCount, afterTeleportRadius, afterTeleportParticleSpeed);
    }

    public static Restrictions setupRestrictions(ConfigurationSection restrictions, Settings template, Config pluginConfig, boolean applyTemplate) {
        if (pluginConfig.isNullSection(restrictions)) {
            if (!applyTemplate) {
                return null;
            }
            return template != null && template.restrictions() != null
                    ? template.restrictions()
                    : new Restrictions(false, false, false, false, false);
        }
        return new Restrictions(
                restrictions.getBoolean("move", false),
                restrictions.getBoolean("teleport", false),
                restrictions.getBoolean("damage", false),
                restrictions.getBoolean("damage_others", false),
                restrictions.getBoolean("damage_check_only_players", false)
        );
    }

    public static Avoidance setupAvoidance(ConfigurationSection avoid, PluginManager pluginManager, Settings template, Config pluginConfig, boolean applyTemplate) {
        if (pluginConfig.isNullSection(avoid)) {
            if (!applyTemplate) {
                return null;
            }
            return template != null && template.avoidance() != null
                    ? template.avoidance()
                    : new Avoidance(true, Set.of(), true, Set.of(), false, false);
        }
        boolean avoidBlocksBlacklist = avoid.getBoolean("blocks.blacklist", true);
        Set<Material> avoidBlocks = EnumSet.noneOf(Material.class);
        for (String material : avoid.getStringList("blocks.list")) {
            avoidBlocks.add(Material.valueOf(material.toUpperCase(Locale.ENGLISH)));
        }
        boolean avoidBiomesBlacklist = avoid.getBoolean("biomes.blacklist", true);
        Set<Biome> avoidBiomes = VersionUtils.SUB_VERSION > 20 ? new HashSet<>() : EnumSet.noneOf(Biome.class);
        for (String biome : avoid.getStringList("biomes.list")) {
            avoidBiomes.add(Biome.valueOf(biome.toUpperCase(Locale.ENGLISH)));
        }
        boolean avoidRegions = avoid.getBoolean("regions", false) && pluginManager.isPluginEnabled("WorldGuard");
        boolean avoidTowns = avoid.getBoolean("towns", false) && pluginManager.isPluginEnabled("Towny");

        return new Avoidance(avoidBlocksBlacklist, avoidBlocks, avoidBiomesBlacklist, avoidBiomes, avoidRegions, avoidTowns);
    }

    public static Actions setupActions(OvRandomTeleport plugin, ConfigurationSection actions, Settings template, Config pluginConfig, boolean applyTemplate) {
        if (pluginConfig.isNullSection(actions)) {
            if (!applyTemplate) {
                return null;
            }
            return template != null && template.actions() != null
                    ? template.actions()
                    : new Actions(List.of(), new Int2ObjectOpenHashMap<>(), List.of());
        }

        ActionRegistry actionRegistry = plugin.getRtpManager().getActionRegistry();
        List<Action> preTeleportActions = getActionList(plugin, actionRegistry, actions.getStringList("pre_teleport"));
        Int2ObjectMap<List<Action>> onCooldownActions = new Int2ObjectOpenHashMap<>();
        ConfigurationSection cooldownActions = actions.getConfigurationSection("on_cooldown");
        if (!pluginConfig.isNullSection(cooldownActions)) {
            for (String actionId : cooldownActions.getKeys(false)) {
                if (!Utils.isNumeric(actionId)) {
                    continue;
                }
                int time = Integer.parseInt(actionId);
                List<Action> actionList = getActionList(plugin, actionRegistry, cooldownActions.getStringList(actionId));
                onCooldownActions.put(time, actionList);
            }
        }
        List<Action> afterTeleportActions = getActionList(plugin, actionRegistry, actions.getStringList("after_teleport"));

        return new Actions(preTeleportActions, onCooldownActions, afterTeleportActions);
    }

    private static ImmutableList<Action> getActionList(OvRandomTeleport plugin, ActionRegistry actionRegistry, List<String> actionStrings) {
        ImmutableList.Builder<Action> builder = ImmutableList.builderWithExpectedSize(actionStrings.size());
        for (String actionStr : actionStrings) {
            try {
                builder.add(Objects.requireNonNull(actionRegistry.resolveAction(actionStr), "Type doesn't exist"));
            } catch (Exception ex) {
                plugin.getSLF4JLogger().warn("Couldn't create action for string '{}'", actionStr, ex);
            }
        }
        return builder.build();
    }
}
