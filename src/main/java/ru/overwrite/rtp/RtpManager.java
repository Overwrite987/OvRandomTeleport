package ru.overwrite.rtp;

import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import ru.overwrite.rtp.actions.Action;
import ru.overwrite.rtp.actions.ActionRegistry;
import ru.overwrite.rtp.actions.impl.*;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.channels.Settings;
import ru.overwrite.rtp.channels.ChannelType;
import ru.overwrite.rtp.channels.settings.*;
import ru.overwrite.rtp.configuration.Config;
import ru.overwrite.rtp.utils.Utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
            Settings baseTemplate = pluginConfig.getChannelTemplates().get(channelSection.getString("template"));
            Settings channelSettings = Settings.create(plugin, channelSection, pluginConfig, baseTemplate, true);
            LocationGenOptions locationGenOptions = channelSettings.locationGenOptions();
            if (locationGenOptions == null) {
                printDebug("Could not setup location generator options for channel '" + channelId + "'. Skipping...");
                continue;
            }

            Messages messages = setupChannelMessages(channelSection.getConfigurationSection("messages"));

            Channel newChannel = new Channel(channelId,
                    name,
                    type,
                    activeWorlds,
                    teleportToFirstAllowedWorld,
                    serverToMove,
                    minPlayersToUse,
                    invulnerableTicks,
                    channelSettings,
                    messages);
            namedChannels.put(channelId, newChannel);
            assignChannelToSpecification(channelSection.getConfigurationSection("specifications"), newChannel);
        }
        this.defaultChannel = getChannelById(config.getString("main_settings.default_channel", ""));
        if (defaultChannel != null) {
            printDebug("Default channel is: " + defaultChannel.id());
        } else {
            printDebug("Default channel not specified.");
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
            if (!voidWorlds.isEmpty()) {
                voidChannels.put(newChannel.id(), voidWorlds);
            }
            List<World> respawnWorlds = Utils.getWorldList(section.getStringList("respawn_worlds"));
            if (!respawnWorlds.isEmpty()) {
                respawnChannels.put(newChannel.id(), respawnWorlds);
            }
        }
    }

    private void assignChannelToSpecification(ConfigurationSection specificationsSection, Channel newChannel) {
        specifications.assign(newChannel, specificationsSection);
    }

    private Messages setupChannelMessages(ConfigurationSection messages) {
        Messages defaultMessages = pluginConfig.getDefaultChannelMessages();
        if (pluginConfig.isNullSection(messages)) {
            return defaultMessages;
        }
        String prefix = pluginConfig.isConfigValueExist(messages, "prefix") ? messages.getString("prefix") : pluginConfig.getMessagesPrefix();
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
        return pluginConfig.isConfigValueExist(messages, key) ? pluginConfig.getPrefixed(messages.getString(key), prefix) : global;
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
        Settings settings = channel.settings();
        int channelPreTeleportCooldown = getChannelPreTeleportCooldown(player, settings.cooldown());
        boolean finalForce = force || channelPreTeleportCooldown <= 0;
        printDebug("Pre teleporting player '" + playerName + "' with channel '" + channel.id() + "' in world '" + world.getName() + "' (force: " + finalForce + ")");
        teleportingNow.add(playerName);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            locationGenerator.getIterationsPerPlayer().put(playerName, 1);
            Location loc = switch (channel.type()) {
                case DEFAULT -> locationGenerator.generateRandomLocation(player, settings, world);
                case NEAR_PLAYER -> locationGenerator.generateRandomLocationNearPlayer(player, settings, world);
                case NEAR_REGION -> locationGenerator.getWgLocationGenerator() != null ?
                        locationGenerator.getWgLocationGenerator().generateRandomLocationNearRandomRegion(player, settings, world) :
                        locationGenerator.generateRandomLocation(player, settings, world);
            };
            if (loc == null) {
                teleportingNow.remove(playerName);
                Utils.sendMessage(channel.messages().failToFindLocation(), player);
                this.returnCost(player, channel);
                return;
            }
            if (!finalForce) {
                this.executeActions(player, channel, settings.actions().preTeleportActions(), player.getLocation());
                printDebug("Generating task and starting pre teleport timer for player '" + playerName + "' with channel '" + channel.id() + "'");
                RtpTask rtpTask = new RtpTask(plugin, this, playerName, channelPreTeleportCooldown, channel);
                rtpTask.startPreTeleportTimer(player, channel, loc);
                return;
            }
            this.teleportPlayer(player, channel, loc);
        });
    }

    public boolean takeCost(Player player, Channel channel) {
        Costs costs = channel.settings().costs();
        return costs.processMoneyCost(player, channel) &&
                costs.processHungerCost(player, channel) &&
                costs.processExpCost(player, channel);
    }

    private void returnCost(Player player, Channel channel) {
        Costs costs = channel.settings().costs();
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
        this.handlePlayerCooldown(player, channel.settings().cooldown());
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.teleport(loc);
            teleportingNow.remove(player.getName());
            this.spawnParticleSphere(player, channel.settings().particles());
            this.executeActions(player, channel, channel.settings().actions().afterTeleportActions(), loc);
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
        String cd = Utils.getTime(getChannelPreTeleportCooldown(player, channel.settings().cooldown()));
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
