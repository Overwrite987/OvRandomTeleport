package ru.overwrite.rtp;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;
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
import ru.overwrite.rtp.actions.Action;
import ru.overwrite.rtp.actions.ActionRegistry;
import ru.overwrite.rtp.actions.impl.*;
import ru.overwrite.rtp.channels.*;
import ru.overwrite.rtp.utils.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static ru.overwrite.rtp.utils.Config.serializer;

public class RtpManager {

    private final Main plugin;
    private final Config pluginConfig;

    @Getter
    private final ActionRegistry actionRegistry;

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
        this.actionRegistry = new ActionRegistry(plugin);
        registerDefaultActions();
    }

    private void registerDefaultActions() {
        actionRegistry.register(new ConsoleActionType());
        actionRegistry.register(new EffectActionType());
        actionRegistry.register(new MessageActionType());
        actionRegistry.register(new SoundActionType());
        actionRegistry.register(new TitleActionType());
    }

    public void setupChannels(FileConfiguration config, PluginManager pluginManager) {
        long startTime = System.currentTimeMillis();
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
            List<World> activeWorlds = Utils.getWorldList(channelSection.getStringList("active_worlds"));
            boolean teleportToFirstAllowedWorld = channelSection.getBoolean("teleport_to_first_world", false);
            int minPlayersToUse = channelSection.getInt("min_players_to_use", -1);
            Costs costs = setupChannelCosts(channelSection.getConfigurationSection("costs"));
            LocationGenOptions locationGenOptions = setupChannelGenOptions(channelSection.getConfigurationSection("location_generation_options"));
            if (locationGenOptions == null) {
                continue;
            }
            int invulnerableTicks = channelSection.getInt("invulnerable_after_teleport", 1);
            Cooldown cooldown = setupCooldown(channelSection.getConfigurationSection("cooldown"));
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
                    costs,
                    locationGenOptions,
                    invulnerableTicks,
                    cooldown,
                    bossBar,
                    restrictions,
                    avoidance,
                    channelActions,
                    messages);
            namedChannels.put(channelId, newChannel);
            assignChannelToSpecification(channelSection.getConfigurationSection("specifications"), newChannel);
        }
        this.defaultChannel = getChannelByName(config.getString("main_settings.default_channel"));
        long endTime = System.currentTimeMillis();
        if (Utils.DEBUG) {
            plugin.getLogger().info("Channels setup done in " + (endTime - startTime) + " ms");
        }
    }

    public record Specifications(Set<Channel> joinChannels,
                                 Map<Channel, List<World>> voidChannels,
                                 Map<Channel, List<World>> respawnChannels) {

        public static Specifications createEmpty() {
            return new Specifications(new HashSet<>(), new HashMap<>(), new HashMap<>());
        }

        public void assign(Channel newChannel, ConfigurationSection section) {
            if (section == null) return;

            if (section.getBoolean("teleport_on_first_join", false)) {
                joinChannels.add(newChannel);
            }
            List<World> voidWorlds = Utils.getWorldList(section.getStringList("void_worlds"));
            if (!voidWorlds.isEmpty()) {
                voidChannels.put(newChannel, voidWorlds);
            }
            List<World> respawnWorlds = Utils.getWorldList(section.getStringList("respawn_worlds"));
            if (!respawnWorlds.isEmpty()) {
                respawnChannels.put(newChannel, respawnWorlds);
            }
        }
    }

    private void assignChannelToSpecification(ConfigurationSection specificationsSection, Channel newChannel) {
        specifications.assign(newChannel, specificationsSection);
    }

    private Costs setupChannelCosts(ConfigurationSection channelCosts) {
        Costs.MoneyType moneyType = Costs.MoneyType.valueOf(channelCosts.getString("money_type", "VAULT").toUpperCase());
        double moneyCost = channelCosts.getDouble("money_cost", -1);
        int hungerCost = channelCosts.getInt("hunger_cost", -1);
        float expCost = (float) channelCosts.getDouble("experience_cost", -1);
        return new Costs(moneyType, moneyCost, hungerCost, expCost);
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
        int nearRadiusMin = locationGenOptions.getInt("min_near_point_distance", 30);
        int nearRadiusMax = locationGenOptions.getInt("max_near_point_distance", 60);
        int maxLocationAttempts = locationGenOptions.getInt("max_location_attempts", 50);

        return new LocationGenOptions(shape, genFormat, minX, maxX, minZ, maxZ, nearRadiusMin, nearRadiusMax, centerX, centerZ, maxLocationAttempts);
    }

    private Cooldown setupCooldown(ConfigurationSection cooldown) {
        Object2IntLinkedOpenHashMap<String> cooldownsMap = new Object2IntLinkedOpenHashMap<>();
        if (isSectionNull(cooldown)) {
            return new Cooldown(-1, cooldownsMap, false, -1);
        }
        int defaultCooldown = cooldown.getInt("default_cooldown", -1);
        ConfigurationSection groupCooldowns = cooldown.getConfigurationSection("group_cooldowns");
        boolean useLastGroupCooldown = false;
        if (!isSectionNull(groupCooldowns) && plugin.getPerms() != null) {
            for (String groupName : groupCooldowns.getKeys(false)) {
                int cd = groupCooldowns.getInt(groupName);
                cooldownsMap.put(groupName, cd);
            }
            useLastGroupCooldown = cooldown.getBoolean("use_last_group_cooldown", false);
        }
        int teleportCooldown = cooldown.getInt("teleport_cooldown", -1);
        return new Cooldown(defaultCooldown, cooldownsMap, useLastGroupCooldown, teleportCooldown);
    }

    private BossBar setupChannelBossBar(ConfigurationSection bossbar) {
        if (isSectionNull(bossbar)) {
            return new BossBar(false, "", BarColor.PURPLE, BarStyle.SOLID);
        }
        boolean enabled = bossbar.getBoolean("enabled", false);
        String title = Utils.colorize(bossbar.getString("title"), serializer);
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
        Int2ObjectMap<List<Action>> onCooldownActions = new Int2ObjectOpenHashMap<>();
        ConfigurationSection cdActions = actions.getConfigurationSection("on_cooldown");
        if (!isSectionNull(cdActions)) {
            for (String s : cdActions.getKeys(false)) {
                if (!Utils.isNumeric(s)) {
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
        for (String actionStr : actionStrings) {
            try {
                actions.add(Objects.requireNonNull(actionRegistry.resolveAction(actionStr), "Type doesn't exist"));
            } catch (Exception ex) {
                plugin.getSLF4JLogger().warn("Couldn't create action for string '{}'", actionStr, ex);
            }
        }
        return actions;
    }

    private Messages setupChannelMessages(ConfigurationSection messages) {
        String prefix = isConfigValueExist(messages, "prefix") ? messages.getString("prefix") : pluginConfig.messages_prefix;
        String noPermsMessage = getMessage(messages, "no_perms", pluginConfig.messages_no_perms, prefix);
        String invalidWorldMessage = getMessage(messages, "invalid_world", pluginConfig.messages_invalid_world, prefix);
        String notEnoughPlayersMessage = getMessage(messages, "not_enough_players", pluginConfig.messages_not_enough_players, prefix);
        String notEnoughMoneyMessage = getMessage(messages, "not_enough_money", pluginConfig.messages_not_enough_money, prefix);
        String notEnoughHungerMessage = getMessage(messages, "not_enough_hunger", pluginConfig.messages_not_enough_hunger, prefix);
        String notEnoughExpMessage = getMessage(messages, "not_enough_experience", pluginConfig.messages_not_enough_exp, prefix);
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
                notEnoughHungerMessage,
                notEnoughExpMessage,
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
            Utils.sendMessage(channel.getMessages().alreadyTeleportingMessage(), p);
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
                Utils.sendMessage(channel.getMessages().failToFindLocationMessage(), p);
                returnCost(p, channel); // return what we took
                return;
            }
            if (channel.getCooldown().teleportCooldown() > 0) {
                this.executeActions(p, channel, channel.getActions().preTeleportActions(), p.getLocation());
                RtpTask rtpTask = new RtpTask(plugin, this, p.getName(), channel);
                perPlayerActiveRtpTask.put(p.getName(), rtpTask);
                rtpTask.startPreTeleportTimer(p, channel, loc);
                return;
            }
            teleportPlayer(p, channel, loc);
        });
    }

    public boolean takeCost(Player p, Channel channel) {
        Costs costs = channel.getCosts();
        double moneyCost = costs.moneyCost();
        switch (costs.moneyType()) {
            case VAULT: {
                if (plugin.getEconomy() != null && moneyCost > 0) {
                    if (plugin.getEconomy().getBalance(p) < moneyCost) {
                        Utils.sendMessage(channel.getMessages().notEnoughMoneyMessage().replace("%required%", Double.toString(moneyCost)), p);
                        return false;
                    }
                    plugin.getEconomy().withdrawPlayer(p, moneyCost);
                }
                break;
            }
            case PLAYERPOINTS: {
                if (PlayerPointsUtils.getBalance(p) < moneyCost && moneyCost > 0) {
                    Utils.sendMessage(channel.getMessages().notEnoughMoneyMessage().replace("%required%", Double.toString(moneyCost)), p);
                    return false;
                }
                PlayerPointsUtils.withdraw(p, (int) moneyCost);
                break;
            }
            default: {
                break;
            }
        }
        if (costs.hungerCost() > 0) {
            if (p.getFoodLevel() < costs.hungerCost()) {
                Utils.sendMessage(channel.getMessages().notEnoughHungerMessage().replace("%required%", Integer.toString(costs.hungerCost())), p);
                return false;
            }
            p.setFoodLevel(p.getFoodLevel() - costs.hungerCost());
        }
        if (costs.expCost() > 0) {
            if (p.getExp() < costs.expCost()) {
                Utils.sendMessage(channel.getMessages().notEnoughExpMessage().replace("%required%", Float.toString(costs.expCost())), p);
                return false;
            }
            p.setExp(p.getExp() - costs.expCost());
        }
        return true;
    }

    private void returnCost(Player p, Channel channel) {
        Costs costs = channel.getCosts();
        double moneyCost = costs.moneyCost();
        switch (costs.moneyType()) {
            case VAULT: {
                if (plugin.getEconomy() != null && moneyCost > 0) {
                    plugin.getEconomy().depositPlayer(p, moneyCost);
                }
                break;
            }
            case PLAYERPOINTS: {
                if (moneyCost > 0) {
                    PlayerPointsUtils.deposit(p, (int) moneyCost);
                }
                break;
            }
            default: {
                break;
            }
        }
        if (costs.hungerCost() > 0) {
            p.setFoodLevel(p.getFoodLevel() + costs.hungerCost());
        }
        if (costs.expCost() > 0) {
            p.setExp(p.getExp() + costs.expCost());
        }
    }

    public Location generateRandomLocation(Player p, Channel channel, World world) {
        if (Utils.DEBUG) {
            plugin.getPluginLogger().info("Iterations for player " + p.getName() + ": " + LocationUtils.iterationsPerPlayer.getInt(p.getName()));
        }
        if (LocationUtils.iterationsPerPlayer.getInt(p.getName()) >= channel.getLocationGenOptions().maxLocationAttempts()) {
            LocationUtils.iterationsPerPlayer.removeInt(p.getName());
            if (Utils.DEBUG) {
                plugin.getPluginLogger().info("Max iterations reached for player " + p.getName());
            }
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
                plugin.getPluginLogger().info("Location for player " + p.getName() + " found in " + LocationUtils.iterationsPerPlayer.getInt(p.getName()) + " iterations");
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
            if (Utils.DEBUG) {
                plugin.getPluginLogger().info("No players to generate location near player");
            }
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
            if (getChannelCooldown(p, channel.getCooldown()) > 0 && !p.hasPermission("rtp.bypasscooldown")) {
                channel.getPlayerCooldowns().put(p.getName(), System.currentTimeMillis(), getChannelCooldown(p, channel.getCooldown()));
            }
            this.executeActions(p, channel, channel.getActions().afterTeleportActions(), loc);
        });
    }

    public int getChannelCooldown(Player p, Cooldown cooldown) {
        if (cooldown.defaultCooldown() < 0) {
            return -1;
        }
        Object2IntLinkedOpenHashMap<String> groupCooldowns = cooldown.groupCooldowns();
        if (groupCooldowns.isEmpty()) {
            return cooldown.defaultCooldown();
        }
        String playerGroup = plugin.getPerms().getPrimaryGroup(p);
        int defaultCooldown = cooldown.useLastGroupCooldown()
                ? groupCooldowns.getInt(new ArrayList<>(groupCooldowns.keySet()).get(groupCooldowns.size() - 1))
                : cooldown.defaultCooldown();
        return groupCooldowns.getOrDefault(playerGroup, defaultCooldown);
    }

    final String[] searchList = {"%player%", "%name%", "%time%", "%x%", "%y%", "%z%"};

    public void executeActions(Player p, Channel channel, List<Action> actions, Location loc) {
        if (actions.isEmpty()) {
            return;
        }
        String name = channel.getName();
        String cd = Utils.getTime(channel.getCooldown().teleportCooldown());
        String x = Integer.toString(loc.getBlockX());
        String y = Integer.toString(loc.getBlockY());
        String z = Integer.toString(loc.getBlockZ());
        String[] replacementList = {p.getName(), name, cd, x, y, z};
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Action action : actions) {
                action.perform(channel, p, searchList, replacementList);
            }
        });
    }

    public boolean hasActiveTasks(String playerName) {
        return !perPlayerActiveRtpTask.isEmpty() && perPlayerActiveRtpTask.containsKey(playerName);
    }

    public boolean hasCooldown(Channel channel, Player p) {
        return channel.getPlayerCooldowns() != null && channel.getPlayerCooldowns().containsKey(p.getName());
    }
}
