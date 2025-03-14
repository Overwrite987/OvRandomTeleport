package ru.overwrite.rtp;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import ru.overwrite.rtp.actions.Action;
import ru.overwrite.rtp.actions.ActionRegistry;
import ru.overwrite.rtp.actions.impl.*;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.channels.ChannelTemplate;
import ru.overwrite.rtp.channels.ChannelType;
import ru.overwrite.rtp.channels.settings.*;
import ru.overwrite.rtp.configuration.Config;
import ru.overwrite.rtp.utils.TimedExpiringMap;
import ru.overwrite.rtp.utils.Utils;
import ru.overwrite.rtp.utils.VersionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Getter
public final class RtpManager {

    @Getter(AccessLevel.NONE)
    private final Main plugin;
    @Getter(AccessLevel.NONE)
    private final Config pluginConfig;

    private final ActionRegistry actionRegistry;

    private Channel defaultChannel;

    private final Map<String, Channel> namedChannels = new HashMap<>();

    private final Specifications specifications = new Specifications(new HashSet<>(), new HashMap<>(), new HashMap<>());

    private final Map<String, RtpTask> perPlayerActiveRtpTask = new ConcurrentHashMap<>();

    private final LocationGenerator locationGenerator;

    private Map<String, String> proxyCalls;

    public RtpManager(Main plugin) {
        this.plugin = plugin;
        this.pluginConfig = plugin.getPluginConfig();
        this.actionRegistry = new ActionRegistry(plugin);
        this.locationGenerator = new LocationGenerator(plugin, this);
        registerDefaultActions();
    }

    private void registerDefaultActions() {
        actionRegistry.register(new ActionBarActionType());
        actionRegistry.register(new ConsoleActionType());
        actionRegistry.register(new EffectActionType());
        actionRegistry.register(new MessageActionType());
        actionRegistry.register(new SoundActionType());
        actionRegistry.register(new TitleActionType());
    }

    public void initProxyCalls() {
        proxyCalls = new HashMap<>();
    }

    public void setupChannels(FileConfiguration config, PluginManager pluginManager) {
        long startTime = System.currentTimeMillis();
        for (String channelId : config.getConfigurationSection("channels").getKeys(false)) {
            printDebug("Id: " + channelId);
            ConfigurationSection channelSection = config.getConfigurationSection("channels." + channelId);
            if (!channelSection.getString("file", "").isEmpty()) {
                channelSection = pluginConfig.getChannelFile(plugin.getDataFolder().getAbsolutePath() + "/channels", channelSection.getString("file"));
                if (channelSection == null) {
                    printDebug("Unable to get channel settings. Skipping...");
                    continue;
                }
            }
            String name = channelSection.getString("name", "");
            ChannelType type = ChannelType.valueOf(channelSection.getString("type", "DEFAULT").toUpperCase(Locale.ENGLISH));
            if (type == ChannelType.NEAR_REGION && !pluginManager.isPluginEnabled("WorldGuard")) {
                type = ChannelType.DEFAULT;
            }
            List<World> activeWorlds = Utils.getWorldList(channelSection.getStringList("active_worlds"));
            boolean teleportToFirstAllowedWorld = channelSection.getBoolean("teleport_to_first_world", false);
            String serverToMove = channelSection.getString("server_to_move", "");
            int minPlayersToUse = channelSection.getInt("min_players_to_use", -1);
            int invulnerableTicks = channelSection.getInt("invulnerable_after_teleport", 12);
            ChannelTemplate template = pluginConfig.getChannelTemplates().get(channelSection.getString("template"));
            Costs costs = setupChannelCosts(channelSection.getConfigurationSection("costs"), template);
            LocationGenOptions locationGenOptions = setupChannelGenOptions(channelSection.getConfigurationSection("location_generation_options"), template);
            if (locationGenOptions == null) {
                printDebug("Could not setup location generator options for channel '" + channelId + "'. Skipping...");
                continue;
            }
            Cooldown cooldown = setupChannelCooldown(channelSection.getConfigurationSection("cooldown"), template);
            Bossbar bossBar = setupChannelBossBar(channelSection.getConfigurationSection("bossbar"), template);
            Particles particles = setupChannelParticles(channelSection.getConfigurationSection("particles"), template);
            Restrictions restrictions = setupChannelRestrictions(channelSection.getConfigurationSection("restrictions"), template);
            Avoidance avoidance = setupChannelAvoidance(channelSection.getConfigurationSection("avoid"), pluginManager, template);
            Actions channelActions = setupChannelActions(channelSection.getConfigurationSection("actions"), template);
            Messages messages = setupChannelMessages(channelSection.getConfigurationSection("messages"));

            Channel newChannel = new Channel(channelId,
                    name,
                    type,
                    activeWorlds,
                    teleportToFirstAllowedWorld,
                    serverToMove,
                    minPlayersToUse,
                    invulnerableTicks,
                    costs,
                    locationGenOptions,
                    cooldown,
                    bossBar,
                    particles,
                    restrictions,
                    avoidance,
                    channelActions,
                    messages);
            namedChannels.put(channelId, newChannel);
            assignChannelToSpecification(channelSection.getConfigurationSection("specifications"), newChannel);
        }
        this.defaultChannel = getChannelById(config.getString("main_settings.default_channel", ""));
        if (Utils.DEBUG) {
            if (defaultChannel != null) {
                plugin.getPluginLogger().info("Default channel is: " + defaultChannel.id());
            } else {
                plugin.getPluginLogger().info("Default channel not specified.");
            }
        }
        long endTime = System.currentTimeMillis();
        printDebug("Channels setup done in " + (endTime - startTime) + " ms");
    }

    public record Specifications(Set<String> joinChannels,
                                 Map<String, List<World>> voidChannels,
                                 Map<String, List<World>> respawnChannels) {

        public void clearAll() {
            this.joinChannels.clear();
            this.voidChannels.clear();
            this.respawnChannels.clear();
        }

        public void assign(Channel newChannel, ConfigurationSection section) {
            if (section == null) {
                return;
            }

            if (section.getBoolean("teleport_on_first_join", false)) {
                joinChannels.add(newChannel.id());
            }
            List<World> voidWorlds = Utils.getWorldList(section.getStringList("void_worlds"));
            if (!voidWorlds.isEmpty() && !voidWorlds.equals(Collections.singletonList(null))) {
                voidChannels.put(newChannel.id(), voidWorlds);
            }
            List<World> respawnWorlds = Utils.getWorldList(section.getStringList("respawn_worlds"));
            if (!respawnWorlds.isEmpty() && !respawnWorlds.equals(Collections.singletonList(null))) {
                respawnChannels.put(newChannel.id(), respawnWorlds);
            }
        }
    }

    private void assignChannelToSpecification(ConfigurationSection specificationsSection, Channel newChannel) {
        specifications.assign(newChannel, specificationsSection);
    }

    private Costs setupChannelCosts(ConfigurationSection channelCosts, ChannelTemplate template) {
        if (channelCosts == null) {
            return (template != null && template.costs() != null)
                    ? template.costs()
                    : new Costs(null, null, -1, -1, -1);
        }
        Costs.MoneyType moneyType = Costs.MoneyType.valueOf(channelCosts.getString("money_type", "VAULT").toUpperCase(Locale.ENGLISH));
        double moneyCost = channelCosts.getDouble("money_cost", -1);
        int hungerCost = channelCosts.getInt("hunger_cost", -1);
        int expCost = channelCosts.getInt("experience_cost", -1);

        return new Costs(plugin.getEconomy(), moneyType, moneyCost, hungerCost, expCost);
    }

    private LocationGenOptions setupChannelGenOptions(ConfigurationSection locationGenOptions, ChannelTemplate template) {
        if (pluginConfig.isNullSection(locationGenOptions)) {
            return (template != null && template.locationGenOptions() != null)
                    ? template.locationGenOptions()
                    : null;
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

    private Cooldown setupChannelCooldown(ConfigurationSection cooldown, ChannelTemplate template) {
        Object2IntSortedMap<String> groupCooldownsMap = new Object2IntLinkedOpenHashMap<>();
        Object2IntSortedMap<String> preTeleportCooldownsMap = new Object2IntLinkedOpenHashMap<>();
        if (pluginConfig.isNullSection(cooldown)) {
            return (template != null && template.cooldown() != null)
                    ? template.cooldown()
                    : new Cooldown(-1, null, groupCooldownsMap, -1, preTeleportCooldownsMap);
        }
        int defaultCooldown = cooldown.getInt("default_cooldown", -1);
        TimedExpiringMap<String, Long> playerCooldowns = defaultCooldown > 0 ? new TimedExpiringMap<>(TimeUnit.SECONDS) : null;
        final ConfigurationSection groupCooldowns = cooldown.getConfigurationSection("group_cooldowns");
        boolean useLastGroupCooldown = cooldown.getBoolean("use_last_group_cooldown", false);
        if (!pluginConfig.isNullSection(groupCooldowns) && plugin.getPerms() != null) {
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
        int defaultPreTeleportCooldown = cooldown.getInt("default_pre_teleport_cooldown", -1);
        final ConfigurationSection preTeleportGroupCooldowns = cooldown.getConfigurationSection("pre_teleport_group_cooldowns");
        if (!pluginConfig.isNullSection(preTeleportGroupCooldowns) && plugin.getPerms() != null) {
            for (String groupName : preTeleportGroupCooldowns.getKeys(false)) {
                int cd = preTeleportGroupCooldowns.getInt(groupName);
                preTeleportCooldownsMap.put(groupName, cd);
            }
        }
        if (!preTeleportCooldownsMap.isEmpty()) {
            defaultPreTeleportCooldown = useLastGroupCooldown
                    ? preTeleportGroupCooldowns.getInt(new ArrayList<>(preTeleportCooldownsMap.keySet()).get(preTeleportCooldownsMap.size() - 1))
                    : defaultPreTeleportCooldown;
        }
        return new Cooldown(defaultCooldown, playerCooldowns, groupCooldownsMap, defaultPreTeleportCooldown, preTeleportCooldownsMap);
    }

    private Bossbar setupChannelBossBar(ConfigurationSection bossbar, ChannelTemplate template) {
        if (pluginConfig.isNullSection(bossbar)) {
            return (template != null && template.bossbar() != null)
                    ? template.bossbar()
                    : new Bossbar(false, null, null, null);
        }
        boolean enabled = bossbar.getBoolean("enabled");
        String title = Utils.COLORIZER.colorize(bossbar.getString("title"));
        BarColor color = BarColor.valueOf(bossbar.getString("color").toUpperCase(Locale.ENGLISH));
        BarStyle style = BarStyle.valueOf(bossbar.getString("style").toUpperCase(Locale.ENGLISH));

        return new Bossbar(enabled, title, color, style);
    }

    private Particles setupChannelParticles(ConfigurationSection particles, ChannelTemplate template) {
        if (pluginConfig.isNullSection(particles)) {
            return (template != null && template.particles() != null)
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

    private Restrictions setupChannelRestrictions(ConfigurationSection restrictions, ChannelTemplate template) {
        if (pluginConfig.isNullSection(restrictions)) {
            return (template != null && template.restrictions() != null)
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

    private Avoidance setupChannelAvoidance(ConfigurationSection avoid, PluginManager pluginManager, ChannelTemplate template) {
        if (pluginConfig.isNullSection(avoid)) {
            return (template != null && template.avoidance() != null)
                    ? template.avoidance()
                    : new Avoidance(true, EnumSet.noneOf(Material.class), true, Set.of(), false, false);
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

    private Actions setupChannelActions(ConfigurationSection actions, ChannelTemplate template) {
        if (pluginConfig.isNullSection(actions)) {
            return (template != null && template.actions() != null)
                    ? template.actions()
                    : new Actions(List.of(), new Int2ObjectOpenHashMap<>(), List.of());
        }

        List<Action> preTeleportActions = getActionList(actions.getStringList("pre_teleport"));
        Int2ObjectMap<List<Action>> onCooldownActions = new Int2ObjectOpenHashMap<>();
        ConfigurationSection cooldownActions = actions.getConfigurationSection("on_cooldown");
        if (!pluginConfig.isNullSection(cooldownActions)) {
            for (String actionId : cooldownActions.getKeys(false)) {
                if (!Utils.isNumeric(actionId)) {
                    continue;
                }
                int time = Integer.parseInt(actionId);
                List<Action> actionList = getActionList(cooldownActions.getStringList(actionId));
                onCooldownActions.put(time, actionList);
            }
        }
        List<Action> afterTeleportActions = getActionList(actions.getStringList("after_teleport"));

        return new Actions(preTeleportActions, onCooldownActions, afterTeleportActions);
    }

    public ImmutableList<Action> getActionList(List<String> actionStrings) {
        List<Action> actions = new ArrayList<>(actionStrings.size());
        for (String actionStr : actionStrings) {
            try {
                actions.add(Objects.requireNonNull(actionRegistry.resolveAction(actionStr), "Type doesn't exist"));
            } catch (Exception ex) {
                plugin.getSLF4JLogger().warn("Couldn't create action for string '{}'", actionStr, ex);
            }
        }
        return ImmutableList.copyOf(actions);
    }

    private Messages setupChannelMessages(ConfigurationSection messages) {
        Messages defaultMessages = pluginConfig.getDefaultChannelMessages();
        if (pluginConfig.isNullSection(messages)) {
            return defaultMessages;
        }
        String prefix = isConfigValueExist(messages, "prefix") ? messages.getString("prefix") : pluginConfig.getMessagesPrefix();
        String noPerms = getMessage(messages, "no_perms", defaultMessages.noPerms(), prefix);
        String invalidWorld = getMessage(messages, "invalid_world", defaultMessages.invalidWorld(), prefix);
        String notEnoughPlayers = getMessage(messages, "not_enough_players", defaultMessages.notEnoughPlayers(), prefix);
        String notEnoughMoney = getMessage(messages, "not_enough_money", defaultMessages.notEnoughMoney(), prefix);
        String notEnoughHunger = getMessage(messages, "not_enough_hunger", defaultMessages.notEnoughHunger(), prefix);
        String notEnoughExp = getMessage(messages, "not_enough_experience", defaultMessages.notEnoughExp(), prefix);
        String cooldown = getMessage(messages, "cooldown", defaultMessages.cooldown(), prefix);
        String movedOnTeleport = getMessage(messages, "moved_on_teleport", defaultMessages.movedOnTeleport(), prefix);
        String teleportedOnTeleport = getMessage(messages, "teleported_on_teleport", defaultMessages.teleportedOnTeleport(), prefix);
        String damagedOnTeleport = getMessage(messages, "damaged_on_teleport", defaultMessages.damagedOnTeleport(), prefix);
        String damagedOtherOnTeleport = getMessage(messages, "damaged_other_on_teleport", defaultMessages.damagedOtherOnTeleport(), prefix);
        String failToFindLocation = getMessage(messages, "fail_to_find_location", defaultMessages.failToFindLocation(), prefix);

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

    private String getMessage(ConfigurationSection messages, String key, String global, String prefix) {
        return isConfigValueExist(messages, key) ? pluginConfig.getPrefixed(messages.getString(key), prefix) : global;
    }

    private boolean isConfigValueExist(ConfigurationSection section, String key) {
        return section.getString(key) != null;
    }

    public Channel getChannelById(String channelId) {
        if (channelId.isEmpty()) {
            return null;
        }
        return namedChannels.get(channelId);
    }

    public boolean hasActiveTasks(String playerName) {
        return !perPlayerActiveRtpTask.isEmpty() && perPlayerActiveRtpTask.containsKey(playerName);
    }

    private final List<String> teleportingNow = new ArrayList<>();

    public void preTeleport(Player player, Channel channel, World world, boolean force) {
        String playerName = player.getName();
        if (teleportingNow.contains(playerName)) {
            return;
        }
        if (proxyCalls != null && !channel.serverToMove().isEmpty()) {
            printDebug("Moving player '" + playerName + "' with channel '" + channel.id() + "' to server " + channel.serverToMove());
            plugin.getPluginMessage().sendCrossProxy(player, channel.serverToMove(), playerName + " " + channel.id() + ";" + world.getName());
            teleportingNow.remove(playerName);
            plugin.getPluginMessage().connectToServer(player, channel.serverToMove());
            return;
        }
        boolean finalForce = force || getChannelPreTeleportCooldown(player, channel.cooldown()) <= 0;
        printDebug("Pre teleporting player '" + playerName + "' with channel '" + channel.id() + "' in world '" + world.getName() + "' (force: " + finalForce + ")");
        teleportingNow.add(playerName);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            locationGenerator.getIterationsPerPlayer().put(playerName, 1);
            Location loc = switch (channel.type()) {
                case DEFAULT -> locationGenerator.generateRandomLocation(player, channel, world);
                case NEAR_PLAYER -> locationGenerator.generateRandomLocationNearPlayer(player, channel, world);
                case NEAR_REGION -> locationGenerator.getWgLocationGenerator() != null ?
                        locationGenerator.getWgLocationGenerator().generateRandomLocationNearRandomRegion(player, channel, world) :
                        locationGenerator.generateRandomLocation(player, channel, world);
            };
            if (loc == null) {
                teleportingNow.remove(playerName);
                Utils.sendMessage(channel.messages().failToFindLocation(), player);
                this.returnCost(player, channel);
                return;
            }
            if (!finalForce) {
                this.executeActions(player, channel, channel.actions().preTeleportActions(), player.getLocation());
                printDebug("Generating task and starting pre teleport timer for player '" + playerName + "' with channel '" + channel.id() + "'");
                RtpTask rtpTask = new RtpTask(plugin, this, playerName, channel);
                rtpTask.startPreTeleportTimer(player, channel, loc);
                return;
            }
            this.teleportPlayer(player, channel, loc);
        });
    }

    public boolean takeCost(Player player, Channel channel) {
        Costs costs = channel.costs();
        return costs.processMoneyCost(player, channel) &&
                costs.processHungerCost(player, channel) &&
                costs.processExpCost(player, channel);
    }

    private void returnCost(Player player, Channel channel) {
        Costs costs = channel.costs();
        costs.processMoneyReturn(player);
        costs.processHungerReturn(player);
        costs.processExpReturn(player);
    }

    public void teleportPlayer(Player player, Channel channel, Location loc) {
        printDebug("Teleporting player '" + player.getName() + "' with channel '" + channel.id() + "' to location " + Utils.locationToString(loc));
        if (channel.invulnerableTicks() > 0) {
            player.setInvulnerable(true);
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> player.setInvulnerable(false), channel.invulnerableTicks());
        }
        this.handlePlayerCooldown(player, channel.cooldown());
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.teleport(loc);
            teleportingNow.remove(player.getName());
            this.spawnParticleSphere(player, channel.particles());
            this.executeActions(player, channel, channel.actions().afterTeleportActions(), loc);
        });
    }

    public void spawnParticleSphere(Player player, Particles particles) {
        if (!particles.afterTeleportEnabled()) {
            return;
        }
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            final Location loc = player.getLocation();
            loc.add(0, 1, 0);
            final World world = loc.getWorld();

            final double goldenAngle = Math.PI * (3 - Math.sqrt(5));

            final List<Player> receivers = particles.afterTeleportSendOnlyToPlayer() ? List.of(player) : null;

            for (int i = 0; i < particles.afterTeleportCount(); i++) {
                double yOffset = 1 - (2.0 * i) / (particles.afterTeleportCount() - 1);
                double radiusAtHeight = Math.sqrt(1 - yOffset * yOffset);

                double theta = goldenAngle * i;

                double xOffset = particles.afterTeleportRadius() * radiusAtHeight * Math.cos(theta);
                double zOffset = particles.afterTeleportRadius() * radiusAtHeight * Math.sin(theta);

                Location particleLocation = loc.clone().add(xOffset, yOffset * particles.afterTeleportRadius(), zOffset);

                world.spawnParticle(
                        particles.afterTeleportParticle().particle(),
                        receivers,
                        player,
                        particleLocation.getX(),
                        particleLocation.getY(),
                        particleLocation.getZ(),
                        1,
                        0,
                        0,
                        0,
                        particles.afterTeleportParticleSpeed(),
                        particles.afterTeleportParticle().dustOptions());
            }
        }, 1L);
    }

    private void handlePlayerCooldown(Player player, Cooldown cooldown) {
        int cooldownTime = getChannelCooldown(player, cooldown);
        if (getChannelCooldown(player, cooldown) > 0 && !player.hasPermission("rtp.bypasscooldown")) {
            cooldown.setCooldown(player.getName(), cooldownTime);
        }
    }

    public int getChannelCooldown(Player player, Cooldown cooldown) {
        if (cooldown.defaultCooldown() < 0) {
            return -1;
        }
        Object2IntSortedMap<String> groupCooldowns = cooldown.groupCooldowns();
        if (groupCooldowns.isEmpty()) {
            return cooldown.defaultCooldown();
        }
        final String playerGroup = plugin.getPerms().getPrimaryGroup(player);
        return groupCooldowns.getOrDefault(playerGroup, cooldown.defaultCooldown());
    }

    public int getChannelPreTeleportCooldown(Player player, Cooldown cooldown) {
        if (cooldown.defaultPreTeleportCooldown() < 0) {
            return -1;
        }
        Object2IntSortedMap<String> preTeleportCooldowns = cooldown.preTeleportCooldowns();
        if (preTeleportCooldowns.isEmpty()) {
            return cooldown.defaultPreTeleportCooldown();
        }
        final String playerGroup = plugin.getPerms().getPrimaryGroup(player);
        return preTeleportCooldowns.getOrDefault(playerGroup, cooldown.defaultPreTeleportCooldown());
    }

    @Getter(AccessLevel.NONE)
    private final String[] searchList = {"%player%", "%name%", "%time%", "%x%", "%y%", "%z%"};

    public void executeActions(Player player, Channel channel, List<Action> actionList, Location loc) {
        if (actionList.isEmpty()) {
            return;
        }
        String name = channel.name();
        String cd = Utils.getTime(getChannelPreTeleportCooldown(player, channel.cooldown()));
        String x = Integer.toString(loc.getBlockX());
        String y = Integer.toString(loc.getBlockY());
        String z = Integer.toString(loc.getBlockZ());
        final String[] replacementList = {player.getName(), name, cd, x, y, z};
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Action action : actionList) {
                action.perform(channel, player, searchList, replacementList);
            }
        });
    }

    public void cancelAllTasks() {
        for (RtpTask task : perPlayerActiveRtpTask.values()) {
            task.cancel();
        }
    }

    public void printDebug(String message) {
        if (Utils.DEBUG) {
            plugin.getPluginLogger().info(message);
        }
    }
}
