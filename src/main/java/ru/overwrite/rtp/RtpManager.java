package ru.overwrite.rtp;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
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
import ru.overwrite.rtp.channels.ChannelType;
import ru.overwrite.rtp.channels.Settings;
import ru.overwrite.rtp.channels.Specifications;
import ru.overwrite.rtp.channels.settings.Cooldown;
import ru.overwrite.rtp.channels.settings.Costs;
import ru.overwrite.rtp.channels.settings.Messages;
import ru.overwrite.rtp.channels.settings.Particles;
import ru.overwrite.rtp.configuration.Config;
import ru.overwrite.rtp.utils.Utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

@Getter
public final class RtpManager {

    @Getter(AccessLevel.NONE)
    private final OvRandomTeleport plugin;
    @Getter(AccessLevel.NONE)
    private final Config pluginConfig;

    private final ActionRegistry actionRegistry;

    private Channel defaultChannel;

    private final Map<String, Channel> namedChannels;

    private final ConcurrentMap<String, RtpTask> perPlayerActiveRtpTask;

    private final LocationGenerator locationGenerator;

    private final Specifications specifications;

    private Map<String, String> proxyCalls;

    @Getter(AccessLevel.NONE)
    @Setter
    private int maxTeleporting;

    public RtpManager(OvRandomTeleport plugin) {
        this.plugin = plugin;
        this.pluginConfig = plugin.getPluginConfig();
        this.namedChannels = new HashMap<>();
        this.perPlayerActiveRtpTask = new ConcurrentHashMap<>();
        this.actionRegistry = new ActionRegistry(plugin);
        this.locationGenerator = new LocationGenerator(plugin, this);
        this.specifications = Specifications.create();
        this.registerDefaultActions();
    }

    private void registerDefaultActions() {
        actionRegistry.register(new ActionBarActionType());
        actionRegistry.register(new ConsoleActionType());
        actionRegistry.register(new DelayedActionActionType());
        actionRegistry.register(new EffectActionType());
        actionRegistry.register(new MessageActionType());
        actionRegistry.register(new PlayerActionType());
        actionRegistry.register(new SoundActionType());
        actionRegistry.register(new TitleActionType());
    }

    public void initProxyCalls() {
        this.proxyCalls = new HashMap<>();
    }

    public void setupChannels(FileConfiguration config, PluginManager pluginManager) {
        long startTime = System.currentTimeMillis();
        ConfigurationSection channelsSection = config.getConfigurationSection("channels");
        for (String channelId : channelsSection.getKeys(false)) {
            printDebug("Id: " + channelId);
            if (channelId.equalsIgnoreCase("admin")) {
                plugin.getPluginLogger().warn("Channel ID cannot be 'admin'. Skipping...");
                continue;
            }
            ConfigurationSection channelSection = channelsSection.getConfigurationSection(channelId);
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
            int invulnerableTicks = channelSection.getInt("invulnerable_after_teleport", 15);
            boolean allowInCommands = channelSection.getBoolean("allow_in_command", true);
            boolean bypassMaxTeleportLimit = channelsSection.getBoolean("bypass_max_teleport_limit", false);
            Settings baseTemplate = pluginConfig.getChannelTemplates().get(channelSection.getString("template"));
            Settings channelSettings = Settings.create(plugin, channelSection, pluginConfig, baseTemplate, true);

            Messages messages = Messages.create(channelSection.getConfigurationSection("messages"), pluginConfig);

            Channel newChannel = new Channel(channelId,
                    name,
                    type,
                    activeWorlds,
                    teleportToFirstAllowedWorld,
                    serverToMove,
                    minPlayersToUse,
                    invulnerableTicks,
                    allowInCommands,
                    bypassMaxTeleportLimit,
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

    private void assignChannelToSpecification(ConfigurationSection specificationsSection, Channel newChannel) {
        specifications.assign(newChannel, specificationsSection);
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

    private final ReferenceList<String> teleportingNow = new ReferenceArrayList<>();

    public void preTeleport(Player player, Channel channel, World world, boolean force) {
        String playerName = player.getName();
        if (teleportingNow.contains(playerName)) {
            return;
        }
        if (proxyCalls != null && !channel.serverToMove().isEmpty()) {
            printDebug("Moving player '" + playerName + "' with channel '" + channel.id() + "' to server " + channel.serverToMove());
            plugin.getPluginMessage().sendCrossProxy(player, channel.serverToMove(), playerName + " " + channel.id() + ";" + world.getName());
            plugin.getPluginMessage().connectToServer(player, channel.serverToMove());
            return;
        }
        if (teleportingNow.size() > maxTeleporting && !channel.bypassMaxTeleportLimit()) {
            Utils.sendMessage(pluginConfig.getCommandMessages().tooMuchTeleporting(), player);
            printDebug("Unable to pre teleport player '" + playerName + "' because too much players are teleporting and channel '" + channel.id() + "' does not have a bypass");
            return;
        }
        Settings settings = channel.settings();
        int channelPreTeleportCooldown = getCooldown(player, settings.cooldown().defaultPreTeleportCooldown(), settings.cooldown().preTeleportCooldowns());
        boolean finalForce = force || channelPreTeleportCooldown <= 0;
        printDebug("Pre teleporting player '" + playerName + "' with channel '" + channel.id() + "' in world '" + world.getName() + "' (cooldown: " + channelPreTeleportCooldown + " force: " + finalForce + ")");
        teleportingNow.add(playerName);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            locationGenerator.getIterationsPerPlayer().put(playerName, 1);
            long startTime = System.currentTimeMillis();
            Location loc = switch (channel.type()) {
                case DEFAULT -> locationGenerator.generateRandomLocation(player, settings, world);
                case NEAR_PLAYER -> locationGenerator.generateRandomLocationNearPlayer(player, settings, world);
                case NEAR_REGION -> locationGenerator.getWgLocationGenerator() != null ?
                        locationGenerator.getWgLocationGenerator().generateRandomLocationNearRandomRegion(player, settings, world) :
                        locationGenerator.generateRandomLocation(player, settings, world);
            };
            long endTime = System.currentTimeMillis();
            long locationFound = endTime - startTime;
            if (locationFound > 500) {
                plugin.getPluginLogger().warn("Генерация локации заняла слишком много времени! (" + locationFound + "ms)");
                plugin.getPluginLogger().warn("Убедитесь, что вы прогрузили карту при помощи Chunky!");
            }
            if (loc == null) {
                teleportingNow.remove(playerName);
                Utils.sendMessage(channel.messages().failToFindLocation(), player);
                this.returnCost(player, channel);
                return;
            }
            if (!finalForce) {
                this.executeActions(player, channel, channelPreTeleportCooldown, settings.actions().preTeleportActions(), player.getLocation());
                printDebug(() -> "Generating task and starting pre teleport timer for player '" + playerName + "' with channel '" + channel.id() + "'");
                RtpTask rtpTask = new RtpTask(plugin, this, player, channel, channelPreTeleportCooldown);
                rtpTask.startPreTeleportTimer(loc);
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

    public void returnCost(Player player, Channel channel) {
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
            this.executeActions(player, channel, 0, channel.settings().actions().afterTeleportActions(), loc);
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

                double afterTeleportRadius = particles.afterTeleportRadius();

                double xOffset = afterTeleportRadius * radiusAtHeight * Math.cos(theta);
                double zOffset = afterTeleportRadius * radiusAtHeight * Math.sin(theta);

                Location particleLocation = loc.clone().add(xOffset, yOffset * afterTeleportRadius, zOffset);

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
        int cooldownTime = getCooldown(player, cooldown.defaultCooldown(), cooldown.groupCooldowns());
        if (cooldownTime > 0 && !player.hasPermission("rtp.bypasscooldown")) {
            cooldown.setCooldown(player.getName(), cooldownTime);
        }
    }

    public int getCooldown(Player player, int defaultCooldown, Object2IntSortedMap<String> groupCooldowns) {
        if (defaultCooldown < 0) {
            return -1;
        }
        if (groupCooldowns.isEmpty()) {
            printDebug("Group cooldowns is empty. Returning default cooldown " + defaultCooldown);
            return defaultCooldown;
        }
        final String playerGroup = plugin.getPerms().getPrimaryGroup(player);
        printDebug("Group cooldowns isn't empty. Player group for cooldown is " + playerGroup);
        return groupCooldowns.getOrDefault(playerGroup, defaultCooldown);
    }

    @Getter(AccessLevel.NONE)
    private final String[] searchList = {"%player%", "%name%", "%time%", "%x%", "%y%", "%z%"};

    public void executeActions(Player player, Channel channel, int cooldown, List<Action> actionList, Location loc) {
        if (actionList.isEmpty()) {
            return;
        }
        final String[] replacementList = {
                player.getName(),
                channel.name(),
                Utils.getTime(cooldown),
                Integer.toString(loc.getBlockX()),
                Integer.toString(loc.getBlockY()),
                Integer.toString(loc.getBlockZ())
        };
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Action action : actionList) {
                action.perform(player, searchList, replacementList);
            }
        });
    }

    public void cancelAllTasks() {
        for (RtpTask task : perPlayerActiveRtpTask.values()) {
            task.cancel(true);
        }
    }

    public void printDebug(final Supplier<String> messageEntry) {
        if (Utils.DEBUG) {
            plugin.getPluginLogger().info(messageEntry.get());
        }
    }

    public void printDebug(final String message) {
        if (Utils.DEBUG) {
            plugin.getPluginLogger().info(message);
        }
    }
}
