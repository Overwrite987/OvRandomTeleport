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
        boolean isNullSection = pluginConfig.isNullSection(channelCosts);
        if (isNullSection && !applyTemplate) {
            return null;
        }
        boolean hasTemplateCosts = template != null && template.costs() != null;

        Costs.MoneyType moneyType = getOrDefaultValue(
                !isNullSection && channelCosts.contains("money_type"),
                () -> Costs.MoneyType.valueOf(
                        getOrDefaultValue(
                                !isNullSection,
                                () -> channelCosts.getString("money_type", "VAULT"),
                                "VAULT"
                        ).toUpperCase(Locale.ENGLISH)
                ),
                getOrDefaultValue(
                        hasTemplateCosts,
                        () -> template.costs().moneyType(),
                        null
                )
        );

        Supplier<Double> moneyCostSupplier = () -> getOrDefaultValue(
                hasTemplateCosts,
                () -> template.costs().moneyCost(),
                -1.0
        );
        double moneyCost = getOrDefaultValue(
                !isNullSection,
                () -> channelCosts.getDouble("money_cost", moneyCostSupplier.get()),
                moneyCostSupplier.get()
        );

        Supplier<Integer> hungerCostSupplier = () -> getOrDefaultValue(
                hasTemplateCosts,
                () -> template.costs().hungerCost(),
                -1
        );
        int hungerCost = getOrDefaultValue(
                !isNullSection,
                () -> channelCosts.getInt("hunger_cost", hungerCostSupplier.get()),
                hungerCostSupplier.get()
        );

        Supplier<Integer> expCostSupplier = () -> getOrDefaultValue(
                hasTemplateCosts,
                () -> template.costs().expCost(),
                -1
        );
        int expCost = getOrDefaultValue(
                !isNullSection,
                () -> channelCosts.getInt("experience_cost", expCostSupplier.get()),
                expCostSupplier.get()
        );

        return new Costs(plugin.getEconomy(), moneyType, moneyCost, hungerCost, expCost);
    }

    public static LocationGenOptions setupLocationGenOptions(ConfigurationSection locationGenOptions, Settings template, Config pluginConfig, boolean applyTemplate) {
        boolean isNullSection = pluginConfig.isNullSection(locationGenOptions);
        if (isNullSection && !applyTemplate) {
            return null;
        }
        boolean hasTemplateOptions = template != null && template.locationGenOptions() != null;

        LocationGenOptions.Shape shape = getOrDefaultValue(
                !isNullSection && locationGenOptions.contains("shape"),
                () -> LocationGenOptions.Shape.valueOf(
                        getOrDefaultValue(
                                !isNullSection,
                                () -> locationGenOptions.getString("shape", "SQUARE"),
                                "SQUARE"
                        ).toUpperCase(Locale.ENGLISH)
                ),
                getOrDefaultValue(
                        hasTemplateOptions,
                        () -> template.locationGenOptions().shape(),
                        LocationGenOptions.Shape.SQUARE
                )
        );

        LocationGenOptions.GenFormat genFormat = getOrDefaultValue(
                !isNullSection && locationGenOptions.contains("gen_format"),
                () -> LocationGenOptions.GenFormat.valueOf(
                        getOrDefaultValue(
                                !isNullSection,
                                () -> locationGenOptions.getString("gen_format", "RECTANGULAR"),
                                "RECTANGULAR"
                        ).toUpperCase(Locale.ENGLISH)
                ),
                getOrDefaultValue(
                        hasTemplateOptions,
                        () -> template.locationGenOptions().genFormat(),
                        LocationGenOptions.GenFormat.RECTANGULAR
                )
        );

        Supplier<Integer> minXSupplier = () -> getOrDefaultValue(
                hasTemplateOptions,
                () -> template.locationGenOptions().minX(),
                0
        );
        int minX = getOrDefaultValue(
                !isNullSection,
                () -> locationGenOptions.getInt("min_x", minXSupplier.get()),
                minXSupplier.get()
        );

        Supplier<Integer> maxXSupplier = () -> getOrDefaultValue(
                hasTemplateOptions,
                () -> template.locationGenOptions().maxX(),
                0
        );
        int maxX = getOrDefaultValue(
                !isNullSection,
                () -> locationGenOptions.getInt("max_x", maxXSupplier.get()),
                maxXSupplier.get()
        );

        Supplier<Integer> minZSupplier = () -> getOrDefaultValue(
                hasTemplateOptions,
                () -> template.locationGenOptions().minZ(),
                0
        );
        int minZ = getOrDefaultValue(
                !isNullSection,
                () -> locationGenOptions.getInt("min_z", minZSupplier.get()),
                minZSupplier.get()
        );

        Supplier<Integer> maxZSupplier = () -> getOrDefaultValue(
                hasTemplateOptions,
                () -> template.locationGenOptions().maxZ(),
                0
        );
        int maxZ = getOrDefaultValue(
                !isNullSection,
                () -> locationGenOptions.getInt("max_z", maxZSupplier.get()),
                maxZSupplier.get()
        );

        Supplier<Integer> radiusMinSupplier = () -> getOrDefaultValue(
                hasTemplateOptions,
                () -> template.locationGenOptions().nearRadiusMin(),
                30
        );
        int nearRadiusMin = getOrDefaultValue(
                !isNullSection,
                () -> locationGenOptions.getInt("min_near_point_distance", radiusMinSupplier.get()),
                radiusMinSupplier.get()
        );

        Supplier<Integer> radiusMaxSupplier = () -> getOrDefaultValue(
                hasTemplateOptions,
                () -> template.locationGenOptions().nearRadiusMax(),
                60
        );
        int nearRadiusMax = getOrDefaultValue(
                !isNullSection,
                () -> locationGenOptions.getInt("max_near_point_distance", radiusMaxSupplier.get()),
                radiusMaxSupplier.get()
        );

        Supplier<Integer> centerXSupplier = () -> getOrDefaultValue(
                hasTemplateOptions,
                () -> template.locationGenOptions().centerX(),
                0
        );
        int centerX = getOrDefaultValue(
                !isNullSection,
                () -> locationGenOptions.getInt("center_x", centerXSupplier.get()),
                centerXSupplier.get()
        );

        Supplier<Integer> centerZSupplier = () -> getOrDefaultValue(
                hasTemplateOptions,
                () -> template.locationGenOptions().centerZ(),
                0
        );
        int centerZ = getOrDefaultValue(
                !isNullSection,
                () -> locationGenOptions.getInt("center_z", centerZSupplier.get()),
                centerZSupplier.get()
        );

        Supplier<Integer> attemptsSupplier = () -> getOrDefaultValue(
                hasTemplateOptions,
                () -> template.locationGenOptions().maxLocationAttempts(),
                50
        );
        int maxLocationAttempts = getOrDefaultValue(
                !isNullSection,
                () -> locationGenOptions.getInt("max_location_attempts", attemptsSupplier.get()),
                attemptsSupplier.get()
        );

        return new LocationGenOptions(shape, genFormat, minX, maxX, minZ, maxZ, nearRadiusMin, nearRadiusMax, centerX, centerZ, maxLocationAttempts);
    }

    public static Cooldown setupCooldown(OvRandomTeleport plugin, ConfigurationSection cooldown, Settings template, Config pluginConfig, boolean applyTemplate) {
        boolean isNullSection = pluginConfig.isNullSection(cooldown);
        if (isNullSection && !applyTemplate) {
            return null;
        }
        boolean hasTemplateCooldown = template != null && template.cooldown() != null;

        Supplier<Integer> defaultCooldownSupplier = () -> getOrDefaultValue(
                hasTemplateCooldown,
                () -> template.cooldown().defaultCooldown(),
                -1
        );
        int defaultCooldown = getOrDefaultValue(
                !isNullSection,
                () -> cooldown.getInt("default_cooldown", defaultCooldownSupplier.get()),
                defaultCooldownSupplier.get()
        );

        TimedExpiringMap<String, Long> playerCooldowns = defaultCooldown > 0
                ? new TimedExpiringMap<>(TimeUnit.SECONDS)
                : null;

        Object2IntSortedMap<String> groupCooldownsMap = new Object2IntLinkedOpenHashMap<>();
        Object2IntSortedMap<String> preTeleportCooldownsMap = new Object2IntLinkedOpenHashMap<>();

        boolean useLastGroupCooldown = !isNullSection && cooldown.getBoolean("use_last_group_cooldown", false);

        ConfigurationSection groupCooldowns = getOrDefaultValue(
                !isNullSection,
                () -> cooldown.getConfigurationSection("group_cooldowns"),
                null
        );
        if (!pluginConfig.isNullSection(groupCooldowns)) {
            defaultCooldown = processCooldownSection(plugin, groupCooldowns, groupCooldownsMap, useLastGroupCooldown, defaultCooldown, pluginConfig);
        } else if (hasTemplateCooldown) {
            groupCooldownsMap.putAll(template.cooldown().groupCooldowns());
        }

        Supplier<Integer> preTeleportCooldownSupplier = () -> getOrDefaultValue(
                hasTemplateCooldown,
                () -> template.cooldown().defaultPreTeleportCooldown(),
                -1
        );
        int defaultPreTeleportCooldown = getOrDefaultValue(
                !isNullSection,
                () -> cooldown.getInt("default_pre_teleport_cooldown", preTeleportCooldownSupplier.get()),
                preTeleportCooldownSupplier.get()
        );

        ConfigurationSection preTeleportGroupCooldowns = getOrDefaultValue(
                !isNullSection,
                () -> cooldown.getConfigurationSection("pre_teleport_group_cooldowns"),
                null
        );
        if (!pluginConfig.isNullSection(preTeleportGroupCooldowns)) {
            defaultPreTeleportCooldown = processCooldownSection(plugin, preTeleportGroupCooldowns, preTeleportCooldownsMap, useLastGroupCooldown, defaultPreTeleportCooldown, pluginConfig);
        } else if (hasTemplateCooldown) {
            preTeleportCooldownsMap.putAll(template.cooldown().preTeleportCooldowns());
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
        boolean isNullSection = pluginConfig.isNullSection(bossbar);
        if (isNullSection && !applyTemplate) {
            return null;
        }
        boolean hasTemplateBossbar = template != null && template.bossbar() != null;

        boolean enabledDefault = hasTemplateBossbar && template.bossbar().bossbarEnabled();
        boolean enabled = !isNullSection ? bossbar.getBoolean("enabled", enabledDefault) : enabledDefault;

        String title = getOrDefaultValue(
                !isNullSection && bossbar.contains("title"),
                () -> Utils.COLORIZER.colorize(bossbar.getString("title")),
                getOrDefaultValue(
                        hasTemplateBossbar,
                        () -> template.bossbar().bossbarTitle(),
                        null
                )
        );

        BarColor color = getOrDefaultValue(
                !isNullSection && bossbar.contains("color"),
                () -> BarColor.valueOf(bossbar.getString("color", "WHITE").toUpperCase(Locale.ENGLISH)),
                getOrDefaultValue(
                        hasTemplateBossbar,
                        () -> template.bossbar().bossbarColor(),
                        null
                )
        );

        BarStyle style = getOrDefaultValue(
                !isNullSection && bossbar.contains("style"),
                () -> BarStyle.valueOf(bossbar.getString("style", "SEGMENTED_12").toUpperCase(Locale.ENGLISH)),
                getOrDefaultValue(
                        hasTemplateBossbar,
                        () -> template.bossbar().bossbarStyle(),
                        null
                )
        );

        return new Bossbar(enabled, title, color, style);
    }

    public static Particles setupParticles(ConfigurationSection particles, Settings template, Config pluginConfig, boolean applyTemplate) {
        boolean isNullSection = pluginConfig.isNullSection(particles);
        if (isNullSection && !applyTemplate) {
            return null;
        }
        boolean hasTemplateParticles = template != null && template.particles() != null;

        boolean preTeleportEnabled = hasTemplateParticles && template.particles().preTeleportEnabled();
        boolean preTeleportSendOnlyToPlayer = hasTemplateParticles && template.particles().preTeleportSendOnlyToPlayer();
        List<Particles.ParticleData> preTeleportParticles = getOrDefaultValue(
                hasTemplateParticles,
                () -> template.particles().preTeleportParticles(),
                null
        );
        int preTeleportDots = getOrDefaultValue(
                hasTemplateParticles,
                () -> template.particles().preTeleportDots(),
                0
        );
        double preTeleportRadius = getOrDefaultValue(
                hasTemplateParticles,
                () -> template.particles().preTeleportRadius(),
                0.0
        );
        double preTeleportParticleSpeed = getOrDefaultValue(
                hasTemplateParticles,
                () -> template.particles().preTeleportParticleSpeed(),
                0.0
        );
        double preTeleportSpeed = getOrDefaultValue(
                hasTemplateParticles,
                () -> template.particles().preTeleportSpeed(),
                0.0
        );
        boolean preTeleportInvert = hasTemplateParticles && template.particles().preTeleportInvert();
        boolean preTeleportJumping = hasTemplateParticles && template.particles().preTeleportJumping();
        boolean preTeleportMoveNear = hasTemplateParticles && template.particles().preTeleportMoveNear();

        boolean afterTeleportParticleEnabled = hasTemplateParticles && template.particles().afterTeleportEnabled();
        boolean afterTeleportSendOnlyToPlayer = hasTemplateParticles && template.particles().afterTeleportSendOnlyToPlayer();
        Particles.ParticleData afterTeleportParticle = getOrDefaultValue(
                hasTemplateParticles,
                () -> template.particles().afterTeleportParticle(),
                null
        );
        int afterTeleportCount = getOrDefaultValue(
                hasTemplateParticles,
                () -> template.particles().afterTeleportCount(),
                0
        );
        double afterTeleportRadius = getOrDefaultValue(
                hasTemplateParticles,
                () -> template.particles().afterTeleportRadius(),
                0.0
        );
        double afterTeleportParticleSpeed = getOrDefaultValue(
                hasTemplateParticles,
                () -> template.particles().afterTeleportParticleSpeed(),
                0.0
        );

        ConfigurationSection preTeleport = getOrDefaultValue(
                !isNullSection,
                () -> particles.getConfigurationSection("pre_teleport"),
                null
        );

        if (!pluginConfig.isNullSection(preTeleport)) {
            preTeleportEnabled = preTeleport.getBoolean("enabled", preTeleportEnabled);
            preTeleportSendOnlyToPlayer = preTeleport.getBoolean("send_only_to_player", preTeleportSendOnlyToPlayer);
            if (preTeleport.contains("id")) {
                preTeleportParticles = ImmutableList.copyOf(
                        pluginConfig.getStringListInAnyCase(preTeleport.get("id"))
                                .stream()
                                .map(Utils::createParticleData)
                                .toList()
                );
            }
            preTeleportDots = preTeleport.getInt("dots", preTeleportDots);
            preTeleportRadius = preTeleport.getDouble("radius", preTeleportRadius);
            preTeleportParticleSpeed = preTeleport.getDouble("particle_speed", preTeleportParticleSpeed);
            preTeleportSpeed = preTeleport.getDouble("speed", preTeleportSpeed);
            preTeleportInvert = preTeleport.getBoolean("invert", preTeleportInvert);
            preTeleportJumping = preTeleport.getBoolean("jumping", preTeleportJumping);
            preTeleportMoveNear = preTeleport.getBoolean("move_near", preTeleportMoveNear);
        }

        ConfigurationSection afterTeleport = getOrDefaultValue(
                !isNullSection,
                () -> particles.getConfigurationSection("after_teleport"),
                null
        );

        if (!pluginConfig.isNullSection(afterTeleport)) {
            afterTeleportParticleEnabled = afterTeleport.getBoolean("enabled", afterTeleportParticleEnabled);
            afterTeleportSendOnlyToPlayer = afterTeleport.getBoolean("send_only_to_player", afterTeleportSendOnlyToPlayer);
            if (afterTeleport.contains("id")) {
                afterTeleportParticle = Utils.createParticleData(afterTeleport.getString("id"));
            }
            afterTeleportCount = afterTeleport.getInt("count", afterTeleportCount);
            afterTeleportRadius = afterTeleport.getDouble("radius", afterTeleportRadius);
            afterTeleportParticleSpeed = afterTeleport.getDouble("particle_speed", afterTeleportParticleSpeed);
        }

        return new Particles(
                preTeleportEnabled,
                preTeleportSendOnlyToPlayer,
                preTeleportParticles,
                preTeleportDots,
                preTeleportRadius,
                preTeleportParticleSpeed,
                preTeleportSpeed,
                preTeleportInvert,
                preTeleportJumping,
                preTeleportMoveNear,
                afterTeleportParticleEnabled,
                afterTeleportSendOnlyToPlayer,
                afterTeleportParticle,
                afterTeleportCount,
                afterTeleportRadius,
                afterTeleportParticleSpeed
        );
    }

    public static Restrictions setupRestrictions(ConfigurationSection restrictions, Settings template, Config pluginConfig, boolean applyTemplate) {
        boolean isNullSection = pluginConfig.isNullSection(restrictions);
        if (isNullSection && !applyTemplate) {
            return null;
        }
        boolean hasTemplateRestrictions = template != null && template.restrictions() != null;

        boolean templateRestrictMove = hasTemplateRestrictions && template.restrictions().restrictMove();
        boolean restrictMove = getOrDefaultValue(!isNullSection,
                () -> restrictions.getBoolean("move", templateRestrictMove),
                templateRestrictMove
        );

        boolean templateRestrictTeleport = hasTemplateRestrictions && template.restrictions().restrictTeleport();
        boolean restrictTeleport = getOrDefaultValue(!isNullSection,
                () -> restrictions.getBoolean("teleport", templateRestrictTeleport),
                templateRestrictTeleport
        );

        boolean templateRestrictDamage = hasTemplateRestrictions && template.restrictions().restrictDamage();
        boolean restrictDamage = getOrDefaultValue(!isNullSection,
                () -> restrictions.getBoolean("damage", templateRestrictDamage),
                templateRestrictDamage
        );

        boolean templateRestrictDamageOthers = hasTemplateRestrictions && template.restrictions().restrictDamageOthers();
        boolean restrictDamageOthers = getOrDefaultValue(!isNullSection,
                () -> restrictions.getBoolean("damage_others", templateRestrictDamageOthers),
                templateRestrictDamageOthers
        );

        boolean templateDamageCheckOnlyPlayers = hasTemplateRestrictions && template.restrictions().damageCheckOnlyPlayers();
        boolean damageCheckOnlyPlayers = getOrDefaultValue(!isNullSection,
                () -> restrictions.getBoolean("damage_check_only_players", templateDamageCheckOnlyPlayers),
                templateDamageCheckOnlyPlayers
        );

        return new Restrictions(restrictMove, restrictTeleport, restrictDamage, restrictDamageOthers, damageCheckOnlyPlayers);
    }

    public static Avoidance setupAvoidance(ConfigurationSection avoidSection, PluginManager pluginManager, Settings template, Config pluginConfig, boolean applyTemplate) {
        boolean isNullSection = pluginConfig.isNullSection(avoidSection);
        if (isNullSection && !applyTemplate) {
            return null;
        }
        boolean hasTemplateAvoidance = template != null && template.avoidance() != null;

        ConfigurationSection blocksSection = getOrDefaultValue(
                !isNullSection,
                () -> avoidSection.getConfigurationSection("blocks"),
                null
        );

        boolean templateBlocksBlacklist = hasTemplateAvoidance && template.avoidance().avoidBlocksBlacklist();
        boolean avoidBlocksBlacklist = getOrDefaultValue(
                !pluginConfig.isNullSection(blocksSection),
                () -> blocksSection.getBoolean("blacklist", templateBlocksBlacklist),
                templateBlocksBlacklist
        );

        Supplier<Set<Material>> blocksSupplier = () -> getOrDefaultValue(
                hasTemplateAvoidance,
                () -> template.avoidance().avoidBlocks(),
                Set.of()
        );
        Set<Material> avoidBlocks = getOrDefaultValue(
                !pluginConfig.isNullSection(blocksSection) && blocksSection.contains("list"),
                () -> createMaterialSet(blocksSection.getStringList("list")),
                blocksSupplier.get()
        );

        ConfigurationSection biomesSection = getOrDefaultValue(
                !isNullSection,
                () -> avoidSection.getConfigurationSection("biomes"),
                null
        );

        boolean templateBiomesBlacklist = hasTemplateAvoidance && template.avoidance().avoidBiomesBlacklist();
        boolean avoidBiomesBlacklist = getOrDefaultValue(
                !pluginConfig.isNullSection(biomesSection),
                () -> biomesSection.getBoolean("blacklist", templateBiomesBlacklist),
                templateBiomesBlacklist
        );

        Supplier<Set<Biome>> biomesSupplier = () -> getOrDefaultValue(
                hasTemplateAvoidance,
                () -> template.avoidance().avoidBiomes(),
                Set.of()
        );
        Set<Biome> avoidBiomes = getOrDefaultValue(
                !pluginConfig.isNullSection(biomesSection) && biomesSection.contains("list"),
                () -> createBiomeSet(biomesSection.getStringList("list")),
                biomesSupplier.get()
        );

        boolean templateAvoidRegions = hasTemplateAvoidance && template.avoidance().avoidRegions();
        boolean avoidRegions = getOrDefaultValue(
                !isNullSection,
                () -> avoidSection.getBoolean("regions", templateAvoidRegions),
                templateAvoidRegions)
                && pluginManager.isPluginEnabled("WorldGuard");

        boolean templateAvoidTowns = hasTemplateAvoidance && template.avoidance().avoidTowns();
        boolean avoidTowns = getOrDefaultValue(
                !isNullSection,
                () -> avoidSection.getBoolean("towns", templateAvoidTowns),
                templateAvoidTowns)
                && pluginManager.isPluginEnabled("Towny");

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

    public static Actions setupActions(OvRandomTeleport plugin, ConfigurationSection actionsSection,
                                       Settings template, Config pluginConfig, boolean applyTemplate) {
        boolean isNullSection = pluginConfig.isNullSection(actionsSection);
        if (isNullSection && !applyTemplate) {
            return null;
        }
        boolean hasTemplateActions = template != null && template.actions() != null;

        ActionRegistry actionRegistry = plugin.getRtpManager().getActionRegistry();

        List<Action> preTeleportActions = getOrDefaultValue(
                !isNullSection && actionsSection.contains("pre_teleport"),
                () -> getActionList(plugin, actionRegistry, actionsSection.getStringList("pre_teleport")),
                getOrDefaultValue(
                        hasTemplateActions,
                        () -> template.actions().preTeleportActions(),
                        List.of()
                )
        );

        Int2ObjectMap<List<Action>> onCooldownActions = new Int2ObjectOpenHashMap<>();
        if (!isNullSection && actionsSection.contains("on_cooldown")) {
            ConfigurationSection cdSection = actionsSection.getConfigurationSection("on_cooldown");
            if (!pluginConfig.isNullSection(cdSection)) {
                for (String key : cdSection.getKeys(false)) {
                    if (Utils.isNumeric(key)) {
                        onCooldownActions.put(
                                Integer.parseInt(key),
                                getActionList(plugin, actionRegistry, cdSection.getStringList(key))
                        );
                    }
                }
            }
        } else if (hasTemplateActions) {
            onCooldownActions.putAll(template.actions().onCooldownActions());
        }

        List<Action> afterTeleportActions = getOrDefaultValue(
                !isNullSection && actionsSection.contains("after_teleport"),
                () -> getActionList(plugin, actionRegistry, actionsSection.getStringList("after_teleport")),
                getOrDefaultValue(
                        hasTemplateActions,
                        () -> template.actions().afterTeleportActions(),
                        List.of()
                )
        );

        return new Actions(preTeleportActions, onCooldownActions, afterTeleportActions);
    }

    private static <T> T getOrDefaultValue(boolean hasValue, Supplier<T> supplier, T defaultValue) {
        return hasValue ? supplier.get() : defaultValue;
    }

    private static ImmutableList<Action> getActionList(OvRandomTeleport plugin, ActionRegistry actionRegistry, List<String> actionStrings) {
        ImmutableList.Builder<Action> builder = ImmutableList.builder();
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
