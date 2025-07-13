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
import java.util.function.Supplier;

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
        if (pluginConfig.isNullSection(channelCosts) && !applyTemplate) {
            return null;
        }
        boolean hasTemplateCosts = template != null && template.costs() != null;

        Costs.MoneyType moneyType = channelCosts.contains("money_type")
                ? Costs.MoneyType.valueOf(channelCosts.getString("money_type", "VAULT").toUpperCase(Locale.ENGLISH))
                : getTemplateOrDefaultValue(hasTemplateCosts, () -> template.costs().moneyType(), null);

        double moneyCost = channelCosts.getDouble("money_cost", getTemplateOrDefaultValue(hasTemplateCosts, () -> template.costs().moneyCost(), -1.0));
        int hungerCost = channelCosts.getInt("hunger_cost", getTemplateOrDefaultValue(hasTemplateCosts, () -> template.costs().hungerCost(), -1));
        int expCost = channelCosts.getInt("experience_cost", getTemplateOrDefaultValue(hasTemplateCosts, () -> template.costs().expCost(), -1));

        return new Costs(plugin.getEconomy(), moneyType, moneyCost, hungerCost, expCost);
    }

    public static LocationGenOptions setupLocationGenOptions(ConfigurationSection locationGenOptions, Settings template, Config pluginConfig, boolean applyTemplate) {
        if (pluginConfig.isNullSection(locationGenOptions) && !applyTemplate) {
            return null;
        }
        boolean hasTemplateOptions = template != null && template.locationGenOptions() != null;

        LocationGenOptions.Shape shape = locationGenOptions.contains("shape")
                ? LocationGenOptions.Shape.valueOf(locationGenOptions.getString("shape", "SQUARE").toUpperCase(Locale.ENGLISH))
                : getTemplateOrDefaultValue(hasTemplateOptions, () -> template.locationGenOptions().shape(), LocationGenOptions.Shape.SQUARE);

        LocationGenOptions.GenFormat genFormat = locationGenOptions.contains("gen_format")
                ? LocationGenOptions.GenFormat.valueOf(locationGenOptions.getString("gen_format", "RECTANGULAR").toUpperCase(Locale.ENGLISH))
                : getTemplateOrDefaultValue(hasTemplateOptions, () -> template.locationGenOptions().genFormat(), LocationGenOptions.GenFormat.RECTANGULAR);

        int minX = locationGenOptions.getInt("min_x", getTemplateOrDefaultValue(hasTemplateOptions, () -> template.locationGenOptions().minX(), 0));
        int maxX = locationGenOptions.getInt("max_x", getTemplateOrDefaultValue(hasTemplateOptions, () -> template.locationGenOptions().maxX(), 0));
        int minZ = locationGenOptions.getInt("min_z", getTemplateOrDefaultValue(hasTemplateOptions, () -> template.locationGenOptions().minZ(), 0));
        int maxZ = locationGenOptions.getInt("max_z", getTemplateOrDefaultValue(hasTemplateOptions, () -> template.locationGenOptions().maxZ(), 0));
        int nearRadiusMin = locationGenOptions.getInt("min_near_point_distance", getTemplateOrDefaultValue(hasTemplateOptions, () -> template.locationGenOptions().nearRadiusMin(), 30));
        int nearRadiusMax = locationGenOptions.getInt("max_near_point_distance", getTemplateOrDefaultValue(hasTemplateOptions, () -> template.locationGenOptions().nearRadiusMax(), 60));
        int centerX = locationGenOptions.getInt("center_x", getTemplateOrDefaultValue(hasTemplateOptions, () -> template.locationGenOptions().centerX(), 0));
        int centerZ = locationGenOptions.getInt("center_z", getTemplateOrDefaultValue(hasTemplateOptions, () -> template.locationGenOptions().centerZ(), 0));
        int maxLocationAttempts = locationGenOptions.getInt("max_location_attempts", getTemplateOrDefaultValue(hasTemplateOptions, () -> template.locationGenOptions().maxLocationAttempts(), 50));

        return new LocationGenOptions(shape, genFormat, minX, maxX, minZ, maxZ, nearRadiusMin, nearRadiusMax, centerX, centerZ, maxLocationAttempts);
    }

    public static Cooldown setupCooldown(OvRandomTeleport plugin, ConfigurationSection cooldown, Settings template, Config pluginConfig, boolean applyTemplate) {
        if (pluginConfig.isNullSection(cooldown) && !applyTemplate) {
            return null;
        }
        boolean hasTemplateCooldown = template != null && template.cooldown() != null;

        int defaultCooldown = cooldown.getInt("default_cooldown", getTemplateOrDefaultValue(hasTemplateCooldown, () -> template.cooldown().defaultCooldown(), -1));

        TimedExpiringMap<String, Long> playerCooldowns = defaultCooldown > 0 ? new TimedExpiringMap<>(TimeUnit.SECONDS) : null;

        Object2IntSortedMap<String> groupCooldownsMap = new Object2IntLinkedOpenHashMap<>();
        Object2IntSortedMap<String> preTeleportCooldownsMap = new Object2IntLinkedOpenHashMap<>();

        boolean useLastGroupCooldown = cooldown.getBoolean("use_last_group_cooldown", false);
        ConfigurationSection groupCooldowns = cooldown.getConfigurationSection("group_cooldowns");
        if (!pluginConfig.isNullSection(groupCooldowns)) {
            defaultCooldown = processCooldownSection(plugin, groupCooldowns, groupCooldownsMap, useLastGroupCooldown, defaultCooldown, pluginConfig);
        } else {
            groupCooldownsMap = getTemplateOrDefaultValue(hasTemplateCooldown, () -> template.cooldown().groupCooldowns(), groupCooldownsMap);
        }
        int defaultPreTeleportCooldown = cooldown.getInt("default_pre_teleport_cooldown", getTemplateOrDefaultValue(hasTemplateCooldown, () -> template.cooldown().defaultPreTeleportCooldown(), -1));
        ConfigurationSection preTeleportGroupCooldowns = cooldown.getConfigurationSection("pre_teleport_group_cooldowns");
        if (!pluginConfig.isNullSection(preTeleportGroupCooldowns)) {
            defaultPreTeleportCooldown = processCooldownSection(plugin, preTeleportGroupCooldowns, preTeleportCooldownsMap, useLastGroupCooldown, defaultPreTeleportCooldown, pluginConfig);
        } else {
            preTeleportCooldownsMap = getTemplateOrDefaultValue(hasTemplateCooldown, () -> template.cooldown().preTeleportCooldowns(), preTeleportCooldownsMap);
        }
        return new Cooldown(defaultCooldown, playerCooldowns, groupCooldownsMap, defaultPreTeleportCooldown, preTeleportCooldownsMap);
    }

    private static int processCooldownSection(OvRandomTeleport plugin, ConfigurationSection section, Object2IntSortedMap<String> map, boolean useLastGroup, int currentDefault, Config pluginConfig) {
        if (plugin.getPerms() != null) {
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
        if (pluginConfig.isNullSection(bossbar) && !applyTemplate) {
            return null;
        }
        boolean hasTemplateBossbar = template != null && template.bossbar() != null;

        boolean enabled = bossbar.getBoolean("enabled",
                getTemplateOrDefaultValue(hasTemplateBossbar, () -> template.bossbar().bossbarEnabled(), false));

        String title = bossbar.contains("title")
                ? Utils.COLORIZER.colorize(bossbar.getString("title"))
                : getTemplateOrDefaultValue(hasTemplateBossbar, () -> template.bossbar().bossbarTitle(), null);

        BarColor color = bossbar.contains("color")
                ? BarColor.valueOf(bossbar.getString("color", "WHITE").toUpperCase(Locale.ENGLISH))
                : getTemplateOrDefaultValue(hasTemplateBossbar, () -> template.bossbar().bossbarColor(), null);

        BarStyle style = bossbar.contains("style")
                ? BarStyle.valueOf(bossbar.getString("style", "SEGMENTED_12").toUpperCase(Locale.ENGLISH))
                : getTemplateOrDefaultValue(hasTemplateBossbar, () -> template.bossbar().bossbarStyle(), null);

        return new Bossbar(enabled, title, color, style);
    }

    public static Particles setupParticles(ConfigurationSection particles, Settings template, Config pluginConfig, boolean applyTemplate) {
        if (pluginConfig.isNullSection(particles) && !applyTemplate) {
            return null;
        }
        boolean hasTemplateParticles = template != null && template.particles() != null;

        boolean preTeleportEnabled = hasTemplateParticles && template.particles().preTeleportEnabled();
        boolean preTeleportSendOnlyToPlayer = hasTemplateParticles && template.particles().preTeleportSendOnlyToPlayer();
        List<Particles.ParticleData> preTeleportParticles = getTemplateOrDefaultValue(hasTemplateParticles, () -> template.particles().preTeleportParticles(), null);
        int preTeleportDots = getTemplateOrDefaultValue(hasTemplateParticles, () -> template.particles().preTeleportDots(), 0);
        double preTeleportRadius = getTemplateOrDefaultValue(hasTemplateParticles, () -> template.particles().preTeleportRadius(), 0.0);
        double preTeleportParticleSpeed = getTemplateOrDefaultValue(hasTemplateParticles, () -> template.particles().preTeleportParticleSpeed(), 0.0);
        double preTeleportSpeed = getTemplateOrDefaultValue(hasTemplateParticles, () -> template.particles().preTeleportSpeed(), 0.0);
        boolean preTeleportInvert = hasTemplateParticles && template.particles().preTeleportInvert();
        boolean preTeleportJumping = hasTemplateParticles && template.particles().preTeleportJumping();
        boolean preTeleportMoveNear = hasTemplateParticles && template.particles().preTeleportMoveNear();
        boolean afterTeleportParticleEnabled = hasTemplateParticles && template.particles().afterTeleportEnabled();
        boolean afterTeleportSendOnlyToPlayer = hasTemplateParticles && template.particles().afterTeleportSendOnlyToPlayer();
        Particles.ParticleData afterTeleportParticle = getTemplateOrDefaultValue(hasTemplateParticles, () -> template.particles().afterTeleportParticle(), null);
        int afterTeleportCount = getTemplateOrDefaultValue(hasTemplateParticles, () -> template.particles().afterTeleportCount(), 0);
        double afterTeleportRadius = getTemplateOrDefaultValue(hasTemplateParticles, () -> template.particles().afterTeleportRadius(), 0.0);
        double afterTeleportParticleSpeed = getTemplateOrDefaultValue(hasTemplateParticles, () -> template.particles().afterTeleportParticleSpeed(), 0.0);

        final ConfigurationSection preTeleport = particles.getConfigurationSection("pre_teleport");
        if (!pluginConfig.isNullSection(preTeleport)) {
            preTeleportEnabled = preTeleport.getBoolean("enabled", preTeleportEnabled);
            preTeleportSendOnlyToPlayer = preTeleport.getBoolean("send_only_to_player", preTeleportSendOnlyToPlayer);
            preTeleportParticles = ImmutableList.copyOf(pluginConfig.getStringListInAnyCase(preTeleport.get("id")).stream().map(Utils::createParticleData).toList());
            preTeleportDots = preTeleport.getInt("dots", preTeleportDots);
            preTeleportRadius = preTeleport.getDouble("radius", preTeleportRadius);
            preTeleportParticleSpeed = preTeleport.getDouble("particle_speed", preTeleportParticleSpeed);
            preTeleportSpeed = preTeleport.getDouble("speed", preTeleportSpeed);
            preTeleportInvert = preTeleport.getBoolean("invert", preTeleportInvert);
            preTeleportJumping = preTeleport.getBoolean("jumping", preTeleportJumping);
            preTeleportMoveNear = preTeleport.getBoolean("move_near", preTeleportMoveNear);
        }
        final ConfigurationSection afterTeleport = particles.getConfigurationSection("after_teleport");
        if (!pluginConfig.isNullSection(afterTeleport)) {
            afterTeleportParticleEnabled = afterTeleport.getBoolean("enabled", afterTeleportParticleEnabled);
            afterTeleportSendOnlyToPlayer = afterTeleport.getBoolean("send_only_to_player", afterTeleportSendOnlyToPlayer);
            afterTeleportParticle = Utils.createParticleData(afterTeleport.getString("id"));
            afterTeleportCount = afterTeleport.getInt("count", afterTeleportCount);
            afterTeleportRadius = afterTeleport.getDouble("radius", afterTeleportRadius);
            afterTeleportParticleSpeed = afterTeleport.getDouble("particle_speed", afterTeleportParticleSpeed);
        }

        return new Particles(
                preTeleportEnabled, preTeleportSendOnlyToPlayer, preTeleportParticles, preTeleportDots, preTeleportRadius, preTeleportParticleSpeed, preTeleportSpeed, preTeleportInvert, preTeleportJumping, preTeleportMoveNear,
                afterTeleportParticleEnabled, afterTeleportSendOnlyToPlayer, afterTeleportParticle, afterTeleportCount, afterTeleportRadius, afterTeleportParticleSpeed);
    }

    public static Restrictions setupRestrictions(ConfigurationSection restrictions, Settings template, Config pluginConfig, boolean applyTemplate) {
        if (pluginConfig.isNullSection(restrictions) && !applyTemplate) {
            return null;
        }
        boolean hasTemplateRestrictions = template != null && template.avoidance() != null;

        return new Restrictions(
                restrictions.getBoolean("move", hasTemplateRestrictions && template.restrictions().restrictMove()),
                restrictions.getBoolean("teleport", hasTemplateRestrictions && template.restrictions().restrictTeleport()),
                restrictions.getBoolean("damage", hasTemplateRestrictions && template.restrictions().restrictDamage()),
                restrictions.getBoolean("damage_others", hasTemplateRestrictions && template.restrictions().restrictDamageOthers()),
                restrictions.getBoolean("damage_check_only_players", hasTemplateRestrictions && template.restrictions().damageCheckOnlyPlayers())
        );
    }

    public static Avoidance setupAvoidance(ConfigurationSection avoidSection, PluginManager pluginManager, Settings template, Config pluginConfig, boolean applyTemplate) {
        if (pluginConfig.isNullSection(avoidSection) && !applyTemplate) {
            return null;
        }
        boolean hasTemplateAvoidance = template != null && template.avoidance() != null;

        ConfigurationSection blocksSection = avoidSection.getConfigurationSection("blocks");
        Supplier<Boolean> blocksBlacklistSupplier = () -> getTemplateOrDefaultValue(hasTemplateAvoidance, () -> template.avoidance().avoidBlocksBlacklist(), false);
        boolean avoidBlocksBlacklist = !pluginConfig.isNullSection(blocksSection)
                ? blocksSection.getBoolean("blacklist", blocksBlacklistSupplier.get())
                : blocksBlacklistSupplier.get();
        Set<Material> avoidBlocks = getTemplateOrDefaultValue(hasTemplateAvoidance, () -> template.avoidance().avoidBlocks(), Set.of());
        if (!pluginConfig.isNullSection(blocksSection) && blocksSection.contains("list")) {
            avoidBlocks = createMaterialSet(blocksSection.getStringList("list"));
        }

        ConfigurationSection biomesSection = avoidSection.getConfigurationSection("biomes");
        Supplier<Boolean> biomesBlacklistSupplier = () -> getTemplateOrDefaultValue(hasTemplateAvoidance, () -> template.avoidance().avoidBiomesBlacklist(), false);
        boolean avoidBiomesBlacklist = !pluginConfig.isNullSection(biomesSection)
                ? biomesSection.getBoolean("blacklist", biomesBlacklistSupplier.get())
                : biomesBlacklistSupplier.get();
        Set<Biome> avoidBiomes = getTemplateOrDefaultValue(hasTemplateAvoidance, () -> template.avoidance().avoidBiomes(), Set.of());
        if (!pluginConfig.isNullSection(biomesSection) && biomesSection.contains("list")) {
            avoidBiomes = createBiomeSet(biomesSection.getStringList("list"));
        }

        boolean avoidRegions = avoidSection.getBoolean("regions", getTemplateOrDefaultValue(hasTemplateAvoidance, () -> template.avoidance().avoidRegions(), false)) && pluginManager.isPluginEnabled("WorldGuard");

        boolean avoidTowns = avoidSection.getBoolean("towns", getTemplateOrDefaultValue(hasTemplateAvoidance, () -> template.avoidance().avoidTowns(), false)) && pluginManager.isPluginEnabled("Towny");

        return new Avoidance(avoidBlocksBlacklist, avoidBlocks, avoidBiomesBlacklist, avoidBiomes, avoidRegions, avoidTowns);
    }

    private static Set<Material> createMaterialSet(List<String> stringList) {
        Set<Material> materialSet = EnumSet.noneOf(Material.class);
        for (String material : stringList) {
            materialSet.add(Material.valueOf(material.toUpperCase(Locale.ENGLISH)));
        }
        return materialSet;
    }

    private static Set<Biome> createBiomeSet(List<String> stringList) {
        Set<Biome> biomeSet = VersionUtils.SUB_VERSION > 20
                ? new HashSet<>()
                : EnumSet.noneOf(Biome.class);
        for (String biome : stringList) {
            biomeSet.add(Biome.valueOf(biome.toUpperCase(Locale.ENGLISH)));
        }
        return biomeSet;
    }

    public static Actions setupActions(OvRandomTeleport plugin, ConfigurationSection actionsSection, Settings template, Config pluginConfig, boolean applyTemplate) {
        if (pluginConfig.isNullSection(actionsSection) && !applyTemplate) {
            return null;
        }
        boolean hasTemplateActions = template != null && template.actions() != null;

        ActionRegistry actionRegistry = plugin.getRtpManager().getActionRegistry();

        List<Action> preTeleportActions = actionsSection.contains("pre_teleport")
                ? getActionList(plugin, actionRegistry, actionsSection.getStringList("pre_teleport"))
                : getTemplateOrDefaultValue(hasTemplateActions, () -> template.actions().preTeleportActions(), List.of());

        Int2ObjectMap<List<Action>> onCooldownActions = new Int2ObjectOpenHashMap<>();
        if (actionsSection.contains("on_cooldown")) {
            ConfigurationSection cdSection = actionsSection.getConfigurationSection("on_cooldown");
            if (!pluginConfig.isNullSection(cdSection)) {
                for (String key : cdSection.getKeys(false)) {
                    if (!Utils.isNumeric(key)) {
                        continue;
                    }
                    int time = Integer.parseInt(key);
                    List<Action> list = getActionList(plugin, actionRegistry, cdSection.getStringList(key));
                    onCooldownActions.put(time, list);
                }
            }
        } else if (hasTemplateActions) {
            onCooldownActions.putAll(template.actions().onCooldownActions());
        }

        List<Action> afterTeleportActions = actionsSection.contains("after_teleport")
                ? getActionList(plugin, actionRegistry, actionsSection.getStringList("after_teleport"))
                : getTemplateOrDefaultValue(hasTemplateActions, () -> template.actions().afterTeleportActions(), List.of());

        return new Actions(preTeleportActions, onCooldownActions, afterTeleportActions);
    }

    private static <T> T getTemplateOrDefaultValue(boolean hasTemplate, Supplier<T> supplier, T defaultValue) {
        return hasTemplate ? supplier.get() : defaultValue;
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
