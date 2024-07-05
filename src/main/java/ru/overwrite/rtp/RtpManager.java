package ru.overwrite.rtp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import lombok.Getter;
import ru.overwrite.rtp.actions.Action;
import ru.overwrite.rtp.channels.*;
import ru.overwrite.rtp.utils.*;

public class RtpManager {

    private final Main plugin;
    private final Config pluginConfig;

    @Getter
    private Channel defaultChannel;

    @Getter
    private final Map<String, Channel> namedChannels = new HashMap<>();

    @Getter
    private final Specifications specifications = Specifications.createEmpty();

    @Getter
    private final Map<String, RtpTask> perPlayerActiveRtpTask = new ConcurrentHashMap<>();

    private final Random random = new Random();

    public RtpManager(Main plugin) {
        this.plugin = plugin;
        this.pluginConfig = plugin.getPluginConfig();
    }

    public void setupChannels(FileConfiguration config, PluginManager pluginManager) {
        for (String channelId : config.getConfigurationSection("channels").getKeys(false)) {
            if (Utils.DEBUG) {
                plugin.getPluginLogger().info("Id: " + channelId);
            }
            ConfigurationSection channelSection = config.getConfigurationSection("channels." + channelId);
            String name = channelSection.getString("name", "");
            ChannelType type = channelSection.getString("type") == null ? ChannelType.DEFAULT : ChannelType.valueOf(channelSection.getString("type").toUpperCase());
            if (type == ChannelType.NEAR_REGION && !pluginManager.isPluginEnabled("WorldGuard")) {
                type = ChannelType.DEFAULT;
            }
            List<World> activeWorlds = getWorldList(channelSection.getStringList("active_worlds"));
            boolean teleportToFirstAllowedWorld = channelSection.getBoolean("teleport_to_first_world", false);
            int minPlayersToUse = channelSection.getInt("min_players_to_use", -1);
            double teleportCost = plugin.getEconomy() != null ? channelSection.getDouble("teleport_cost", -1) : -1;
            LocationGenOptions locationGenOptions = setupChannelGenOptions(channelSection.getConfigurationSection("location_generation_options"));
            if (locationGenOptions == null) {
                continue;
            }
            int invulnerableTicks = channelSection.getInt("invulnerable_after_teleport", 1);
            int cooldown = channelSection.getInt("cooldown", 60);
            int teleportCooldown = channelSection.getInt("teleport_cooldown", -1);
            BossBar bossBar = setupChannelBossBar(channelSection.getConfigurationSection("bossbar"));
            Restrictions restrictions = setupChannelRestrictions(channelSection.getConfigurationSection("restrictions"));
            Avoidance avoidance = setupChannelAvoidance(channelSection.getConfigurationSection("avoid"), pluginManager);
            Actions channelActions = setupChannelActions(channelSection.getConfigurationSection("actions"));
            if (channelActions == null) {
                continue;
            }
            Messages messages = setupChannelMessages(channelSection.getConfigurationSection("messages"));

            Channel newChannel = new Channel(channelId,
                    name,
                    type,
                    activeWorlds,
                    teleportToFirstAllowedWorld,
                    minPlayersToUse,
                    teleportCost,
                    locationGenOptions,
                    invulnerableTicks,
                    cooldown,
                    teleportCooldown,
                    bossBar,
                    restrictions,
                    avoidance,
                    channelActions,
                    messages);
            namedChannels.put(channelId, newChannel);
            assignChannelToSpecification(channelSection.getConfigurationSection("specifications"), newChannel, channelId);
        }
        this.defaultChannel = getChannelByName(config.getString("main_settings.default_channel"));
    }

    private List<World> getWorldList(List<String> worldNames) {
        List<World> worldList = new ArrayList<>();
        for (String w : worldNames) {
            worldList.add(Bukkit.getWorld(w));
        }
        return worldList;
    }

    public record Specifications(Map<Channel, String> joinChannels,
                                 Map<Channel, String> voidChannels,
                                 Map<Channel, String> respawnChannels) {

        public static Specifications createEmpty() {
            return new Specifications(new HashMap<>(), new HashMap<>(), new HashMap<>());
        }

        public void assign(Channel newChannel, String channelId, ConfigurationSection section) {
            if (section == null) return;

            if (section.getBoolean("teleport_on_first_join", false)) {
                joinChannels.put(newChannel, channelId);
            }
            if (section.getBoolean("teleport_on_void", false)) {
                voidChannels.put(newChannel, channelId);
            }
            if (section.getBoolean("teleport_on_respawn", false)) {
                respawnChannels.put(newChannel, channelId);
            }
        }
    }

    private void assignChannelToSpecification(ConfigurationSection specificationsSection, Channel newChannel, String channelId) {
        specifications.assign(newChannel, channelId, specificationsSection);
    }

    private LocationGenOptions setupChannelGenOptions(ConfigurationSection locationGenOptions) {
        if (locationGenOptions == null) {
            return null;
        }
        LocationGenOptions.Shape shape = LocationGenOptions.Shape.valueOf(locationGenOptions.getString("shape", "SQUARE").toUpperCase());
        LocationGenOptions.GenFormat genFormat = LocationGenOptions.GenFormat.valueOf(locationGenOptions.getString("gen_format", "RECTANGULAR").toUpperCase());
        int minX = locationGenOptions.getInt("min_x");
        int maxX = locationGenOptions.getInt("max_x");
        int minZ = locationGenOptions.getInt("min_z");
        int maxZ = locationGenOptions.getInt("max_z");
        int centerX = locationGenOptions.getInt("center_x", 0);
        int centerZ = locationGenOptions.getInt("center_z", 0);
        plugin.getPluginLogger().info(String.valueOf(locationGenOptions.getInt("min_near_point_distance", 30)));
        int nearRadiusMin = locationGenOptions.getInt("min_near_point_distance", 30);
        int nearRadiusMax = locationGenOptions.getInt("max_near_point_distance", 60);
        int maxLocationAttempts = locationGenOptions.getInt("max_location_attemps", 50);

        return new LocationGenOptions(shape, genFormat, minX, maxX, minZ, maxZ, nearRadiusMin, nearRadiusMax, centerX, centerZ, maxLocationAttempts);
    }

    private BossBar setupChannelBossBar(ConfigurationSection bossbar) {
        if (isSectionNull(bossbar)) {
            return new BossBar(false, "", BarColor.PURPLE, BarStyle.SOLID);
        }
        boolean enabled = bossbar.getBoolean("enabled", false);
        String title = Utils.colorize(bossbar.getString("title"));
        BarColor color = BarColor.valueOf(bossbar.getString("color").toUpperCase());
        BarStyle style = BarStyle.valueOf(bossbar.getString("style").toUpperCase());

        return new BossBar(enabled, title, color, style);
    }

    private Restrictions setupChannelRestrictions(ConfigurationSection restrictions) {
        boolean restrictMove = !isSectionNull(restrictions) && restrictions.getBoolean("move", false);
        boolean restrictTeleport = !isSectionNull(restrictions) && restrictions.getBoolean("teleport", false);
        boolean restrictDamage = !isSectionNull(restrictions) && restrictions.getBoolean("damage", false);
        boolean restrictDamageOthers = !isSectionNull(restrictions) && restrictions.getBoolean("damage_others", false);
        boolean damageCheckOnlyPlayers = !isSectionNull(restrictions) && restrictions.getBoolean("damage_check_only_players", false);

        return new Restrictions(restrictMove, restrictTeleport, restrictDamage, restrictDamageOthers, damageCheckOnlyPlayers);
    }

    private Avoidance setupChannelAvoidance(ConfigurationSection avoid, PluginManager pluginManager) {
        boolean isNullSection = isSectionNull(avoid);
        Set<Material> avoidBlocks = new ObjectOpenHashSet<>();
        boolean avoidBlocksBlacklist = true;
        if (!isNullSection) {
            avoidBlocksBlacklist = avoid.getBoolean("blocks.blacklist", true);
            for (String m : avoid.getStringList("blocks.list")) {
                avoidBlocks.add(Material.valueOf(m.toUpperCase()));
            }
        }
        Set<Biome> avoidBiomes = new ObjectOpenHashSet<>();
        boolean avoidBiomesBlacklist = true;
        if (!isNullSection) {
            avoidBiomesBlacklist = avoid.getBoolean("biomes.blacklist", true);
            for (String b : avoid.getStringList("biomes.list")) {
                avoidBiomes.add(Biome.valueOf(b.toUpperCase()));
            }
        }
        boolean avoidRegions = !isNullSection && avoid.getBoolean("regions", false) && pluginManager.isPluginEnabled("WorldGuard");
        boolean avoidTowns = !isNullSection && avoid.getBoolean("towns", false) && pluginManager.isPluginEnabled("Towny");

        return new Avoidance(avoidBlocksBlacklist, avoidBlocks, avoidBiomesBlacklist, avoidBiomes, avoidRegions, avoidTowns);
    }

    private Actions setupChannelActions(ConfigurationSection actions) {
        if (isSectionNull(actions)) {
            return null;
        }
        List<Action> preTeleportActions = getActionList(actions.getStringList("pre_teleport"));
        Map<Integer, List<Action>> onCooldownActions = new Int2ObjectOpenHashMap<>();
        ConfigurationSection cdActions = actions.getConfigurationSection("on_cooldown");
        if (!isSectionNull(cdActions)) {
            for (String s : cdActions.getKeys(false)) {
                if (!StringUtils.isNumeric(s)) {
                    continue;
                }
                int time = Integer.parseInt(s);
                List<Action> actionList = getActionList(cdActions.getStringList(s));
                onCooldownActions.put(time, actionList);
            }
        }
        List<Action> afterTeleportActions = getActionList(actions.getStringList("after_teleport"));

        return new Actions(preTeleportActions, onCooldownActions, afterTeleportActions);
    }

    private List<Action> getActionList(List<String> actionStrings) {
        List<Action> actions = new ArrayList<>(actionStrings.size());
        for (String actionString : actionStrings) {
            actions.add(Action.fromString(actionString));
        }
        return actions;
    }

    private Messages setupChannelMessages(ConfigurationSection messages) {
        String prefix = isConfigValueExist(messages, "prefix") ? messages.getString("prefix") : pluginConfig.messages_prefix;
        String noPermsMessage = getMessage(messages, "no_perms", pluginConfig.messages_no_perms, prefix);
        String invalidWorldMessage = getMessage(messages, "invalid_world", pluginConfig.messages_invalid_world, prefix);
        String notEnoughPlayersMessage = getMessage(messages, "not_enough_players", pluginConfig.messages_not_enough_players, prefix);
        String notEnoughMoneyMessage = getMessage(messages, "not_enough_money", pluginConfig.messages_not_enough_money, prefix);
        String cooldownMessage = getMessage(messages, "cooldown", pluginConfig.messages_cooldown, prefix);
        String movedOnTeleportMessage = getMessage(messages, "moved_on_teleport", pluginConfig.messages_moved_on_teleport, prefix);
        String teleportedOnTeleportMessage = getMessage(messages, "teleported_on_teleport", pluginConfig.messages_teleported_on_teleport, prefix);
        String damagedOnTeleportMessage = getMessage(messages, "damaged_on_teleport", pluginConfig.messages_damaged_on_teleport, prefix);
        String damagedOtherOnTeleportMessage = getMessage(messages, "damaged_other_on_teleport", pluginConfig.messages_damaged_other_on_teleport, prefix);
        String failToFindLocationMessage = getMessage(messages, "fail_to_find_location", pluginConfig.messages_fail_to_find_location, prefix);
        String alreadyTeleportingMessage = getMessage(messages, "already_teleporting", pluginConfig.messages_already_teleporting, prefix);

        return new Messages(
                noPermsMessage,
                invalidWorldMessage,
                notEnoughPlayersMessage,
                notEnoughMoneyMessage,
                cooldownMessage,
                movedOnTeleportMessage,
                teleportedOnTeleportMessage,
                damagedOnTeleportMessage,
                damagedOtherOnTeleportMessage,
                failToFindLocationMessage,
                alreadyTeleportingMessage
        );
    }

    private String getMessage(ConfigurationSection messages, String key, String global, String prefix) {
        return isConfigValueExist(messages, key) ? pluginConfig.getPrefixed(messages.getString(key), prefix) : global;
    }

    private boolean isConfigValueExist(ConfigurationSection section, String key) {
        return !isSectionNull(section) && section.getString(key) != null;
    }

    private boolean isSectionNull(ConfigurationSection section) {
        return section == null;
    }

    public Channel getChannelByName(String channelName) {
        return namedChannels.get(channelName);
    }

    public final List<String> teleportingNow = new ArrayList<>();

    public void preTeleport(Player p, Channel channel, World world) {
        if (teleportingNow.contains(p.getName())) {
            p.sendMessage(channel.getMessages().alreadyTeleportingMessage());
            return;
        }
        teleportingNow.add(p.getName());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            LocationUtils.iterationsPerPlayer.put(p.getName(), 1);
            Location loc = switch (channel.getType()) {
                case DEFAULT -> generateRandomLocation(p, channel, world);
                case NEAR_PLAYER -> generateRandomLocationNearPlayer(p, channel, world);
                case NEAR_REGION -> WGUtils.generateRandomLocationNearRandomRegion(p, channel, world);
            };
            if (loc == null) {
                teleportingNow.remove(p.getName());
                p.sendMessage(pluginConfig.messages_fail_to_find_location);
                return;
            }
            if (channel.getTeleportCooldown() > 0) {
                this.executeActions(p, channel, channel.getActions().preTeleportActions(), p.getLocation());
                RtpTask rtpTask = new RtpTask(plugin, this, p.getName(), channel);
                perPlayerActiveRtpTask.put(p.getName(), rtpTask);
                rtpTask.startPreTeleportTimer(p, channel, loc);
                return;
            }
            teleportPlayer(p, channel, loc);
        });
    }

    public Location generateRandomLocation(Player p, Channel channel, World world) {
        if (Utils.DEBUG) {
            plugin.getPluginLogger().info("Iterations for player " + p.getName() + ": " + LocationUtils.iterationsPerPlayer.getOrDefault(p.getName(), 0));
        }
        if (LocationUtils.iterationsPerPlayer.getInt(p.getName()) >= channel.getLocationGenOptions().maxLocationAttempts()) {
            LocationUtils.iterationsPerPlayer.removeInt(p.getName());
            return null;
        }

        LocationGenOptions.Shape shape = channel.getLocationGenOptions().shape();
        Location location = switch (shape) {
            case SQUARE -> LocationUtils.generateRandomSquareLocation(p, channel, world);
            case ROUND -> LocationUtils.generateRandomRoundLocation(p, channel, world);
        };

        if (location == null) {
            LocationUtils.iterationsPerPlayer.addTo(p.getName(), 1);
            return generateRandomLocation(p, channel, world);
        } else {
            if (Utils.DEBUG) {
                plugin.getPluginLogger().info("Location for player " + p.getName() + " found in " + LocationUtils.iterationsPerPlayer.get(p.getName()) + " iterations");
            }
            LocationUtils.iterationsPerPlayer.removeInt(p.getName());
            return location;
        }
    }

    private Location generateRandomLocationNearPlayer(Player p, Channel channel, World world) {
        if (LocationUtils.iterationsPerPlayer.getInt(p.getName()) >= channel.getLocationGenOptions().maxLocationAttempts()) {
            LocationUtils.iterationsPerPlayer.removeInt(p.getName());
            return null;
        }
        List<Player> nearbyPlayers = getNearbyPlayers(p, channel, world);

        if (nearbyPlayers.isEmpty()) {
            return null;
        }

        Player targetPlayer = nearbyPlayers.get(random.nextInt(nearbyPlayers.size()));

        Location loc = targetPlayer.getLocation();
        int centerX = loc.getBlockX();
        int centerZ = loc.getBlockZ();

        LocationGenOptions.Shape shape = channel.getLocationGenOptions().shape();
        Location location = LocationUtils.generateRandomLocationNearPoint(shape, p, centerX, centerZ, channel, world);

        if (location == null) {
            LocationUtils.iterationsPerPlayer.addTo(p.getName(), 1);
            return generateRandomLocationNearPlayer(p, channel, world);
        } else {
            if (Utils.DEBUG) {
                plugin.getPluginLogger().info("Location for player " + p.getName() + " found in " + LocationUtils.iterationsPerPlayer.get(p.getName()) + " iterations");
            }
            LocationUtils.iterationsPerPlayer.removeInt(p.getName());
            return location;
        }
    }

    private List<Player> getNearbyPlayers(Player player, Channel channel, World world) {
        List<Player> nearbyPlayers = new ArrayList<>();
        LocationGenOptions locationGenOptions = channel.getLocationGenOptions();
        int minX = locationGenOptions.minX();
        int maxX = locationGenOptions.maxX();
        int minZ = locationGenOptions.minZ();
        int maxZ = locationGenOptions.maxZ();

        for (Player p : world.getPlayers()) {
            if (!p.equals(player) && !p.hasPermission("rtp.near.bypass")) {
                Location loc = p.getLocation();
                int px = loc.getBlockX();
                int pz = loc.getBlockZ();
                if (px >= minX && px <= maxX && pz >= minZ && pz <= maxZ) {
                    nearbyPlayers.add(p);
                }
            }
        }
        return nearbyPlayers;
    }

    public void teleportPlayer(Player p, Channel channel, Location loc) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (channel.getInvulnerableTicks() > 0) {
                p.setInvulnerable(true);
                Bukkit.getScheduler().runTaskLater(plugin, () -> p.setInvulnerable(false), channel.getInvulnerableTicks());
            }
            p.teleport(loc);
            teleportingNow.remove(p.getName());
            if (channel.getCooldown() > 0 && !p.hasPermission("rtp.bypasscooldown")) {
                channel.getPlayerCooldowns().put(p.getName(), System.currentTimeMillis());
            }
            this.executeActions(p, channel, channel.getActions().afterTeleportActions(), loc);
        });
    }

    private final String[] searchList = {"%player%", "%name%", "%time%", "%x%", "%y%", "%z%"};

    public void executeActions(Player p, Channel channel, List<Action> actions, Location loc) {
        if (actions.isEmpty()) {
            return;
        }
        String name = channel.getName();
        String cd = Utils.getTime(channel.getTeleportCooldown());
        String x = Integer.toString(loc.getBlockX());
        String y = Integer.toString(loc.getBlockY());
        String z = Integer.toString(loc.getBlockZ());
        String[] replacementList = {p.getName(), name, cd, x, y, z};
        for (Action action : actions) {
            switch (action.type()) {
                case MESSAGE: {
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        String message = Utils.colorize(StringUtils.replaceEach(action.context(), searchList, replacementList));
                        p.sendMessage(message);
                    });
                    break;
                }
                case TITLE: {
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        String result = Utils.colorize(StringUtils.replaceEach(action.context(), searchList, replacementList));
                        String[] titledMessage = result.split(";");
                        Utils.sendTitleMessage(titledMessage, p);
                    });
                    break;
                }
                case SOUND: {
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        Utils.sendSound(action.context().split(";"), p);
                    });
                    break;
                }
                case EFFECT: {
                    Utils.giveEffect(action.context().split(";"), p);
                    break;
                }
                case CONSOLE: {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), StringUtils.replaceEach(action.context(), searchList, replacementList));
                    break;
                }
                default: {
                    break;
                }
            }
        }
    }

    public boolean hasActiveTasks(String playerName) {
        return !perPlayerActiveRtpTask.isEmpty() && perPlayerActiveRtpTask.containsKey(playerName);
    }
}
