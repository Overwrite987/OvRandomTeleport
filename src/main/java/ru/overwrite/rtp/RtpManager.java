package ru.overwrite.rtp;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

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
import ru.overwrite.rtp.channels.*;
import ru.overwrite.rtp.channels.settings.*;
import ru.overwrite.rtp.utils.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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

    @Getter
    private final LocationGenerator locationGenerator;

    public RtpManager(Main plugin) {
        this.plugin = plugin;
        this.pluginConfig = plugin.getPluginConfig();
        this.actionRegistry = new ActionRegistry(plugin);
        this.locationGenerator = new LocationGenerator(plugin);
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
            if (!channelSection.getString("file", "").isEmpty()) {
                channelSection = pluginConfig.getFile(plugin.getDataFolder().getAbsolutePath() + "/channels", channelSection.getString("file"));
            }
            String name = channelSection.getString("name", "");
            ChannelType type = channelSection.getString("type") == null ? ChannelType.DEFAULT : ChannelType.valueOf(channelSection.getString("type").toUpperCase());
            if (type == ChannelType.NEAR_REGION && !pluginManager.isPluginEnabled("WorldGuard")) {
                type = ChannelType.DEFAULT;
            }
            List<World> activeWorlds = Utils.getWorldList(channelSection.getStringList("active_worlds"));
            boolean teleportToFirstAllowedWorld = channelSection.getBoolean("teleport_to_first_world", false);
            int minPlayersToUse = channelSection.getInt("min_players_to_use", -1);
            int invulnerableTicks = channelSection.getInt("invulnerable_after_teleport", 1);
            Costs costs = setupChannelCosts(channelSection.getConfigurationSection("costs"));
            LocationGenOptions locationGenOptions = setupChannelGenOptions(channelSection.getConfigurationSection("location_generation_options"));
            if (locationGenOptions == null) {
                if (Utils.DEBUG) {
                    plugin.getPluginLogger().info("Could not setup location generator options for channel " + channelId);
                }
                continue;
            }
            Cooldown cooldown = setupCooldown(channelSection.getConfigurationSection("cooldown"));
            BossBar bossBar = setupChannelBossBar(channelSection.getConfigurationSection("bossbar"));
            Particles particles = setupChannelParticles(channelSection.getConfigurationSection("particles"));
            Restrictions restrictions = setupChannelRestrictions(channelSection.getConfigurationSection("restrictions"));
            Avoidance avoidance = setupChannelAvoidance(channelSection.getConfigurationSection("avoid"), pluginManager);
            Actions channelActions = setupChannelActions(channelSection.getConfigurationSection("actions"));
            Messages messages = setupChannelMessages(channelSection.getConfigurationSection("messages"));

            Channel newChannel = new Channel(channelId,
                    name,
                    type,
                    activeWorlds,
                    teleportToFirstAllowedWorld,
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
        this.defaultChannel = getChannelByName(config.getString("main_settings.default_channel", ""));
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

        public void clearAll() {
            this.joinChannels.clear();
            this.voidChannels.clear();
            this.respawnChannels.clear();
        }

        public void assign(Channel newChannel, ConfigurationSection section) {
            if (section == null) return;

            if (section.getBoolean("teleport_on_first_join", false)) {
                joinChannels.add(newChannel);
            }
            List<World> voidWorlds = Utils.getWorldList(section.getStringList("void_worlds"));
            if (!voidWorlds.isEmpty() && !voidWorlds.equals(Collections.singletonList(null))) {
                voidChannels.put(newChannel, voidWorlds);
            }
            List<World> respawnWorlds = Utils.getWorldList(section.getStringList("respawn_worlds"));
            if (!respawnWorlds.isEmpty() && !respawnWorlds.equals(Collections.singletonList(null))) {
                respawnChannels.put(newChannel, respawnWorlds);
            }
        }
    }

    private void assignChannelToSpecification(ConfigurationSection specificationsSection, Channel newChannel) {
        specifications.assign(newChannel, specificationsSection);
    }

    private Costs setupChannelCosts(ConfigurationSection channelCosts) {
        if (channelCosts == null) {
            return new Costs(null, null, -1, -1, -1);
        }
        Costs.MoneyType moneyType = Costs.MoneyType.valueOf(channelCosts.getString("money_type", "VAULT").toUpperCase());
        double moneyCost = channelCosts.getDouble("money_cost", -1);
        int hungerCost = channelCosts.getInt("hunger_cost", -1);
        int expCost = channelCosts.getInt("experience_cost", -1);

        return new Costs(plugin.getEconomy(), moneyType, moneyCost, hungerCost, expCost);
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
        int nearRadiusMin = locationGenOptions.getInt("min_near_point_distance", 30);
        int nearRadiusMax = locationGenOptions.getInt("max_near_point_distance", 60);
        int centerX = locationGenOptions.getInt("center_x", 0);
        int centerZ = locationGenOptions.getInt("center_z", 0);
        int maxLocationAttempts = locationGenOptions.getInt("max_location_attempts", 50);

        return new LocationGenOptions(shape, genFormat, minX, maxX, minZ, maxZ, nearRadiusMin, nearRadiusMax, centerX, centerZ, maxLocationAttempts);
    }

    private Cooldown setupCooldown(ConfigurationSection cooldown) {
        Object2IntLinkedOpenHashMap<String> groupCooldownsMap = new Object2IntLinkedOpenHashMap<>();
        if (isSectionNull(cooldown)) {
            return new Cooldown(-1, null, groupCooldownsMap, false, -1);
        }
        int defaultCooldown = cooldown.getInt("default_cooldown", -1);
        TimedExpiringMap<String, Long> playerCooldowns = defaultCooldown > 0 ? new TimedExpiringMap<>(TimeUnit.SECONDS) : null;
        ConfigurationSection groupCooldowns = cooldown.getConfigurationSection("group_cooldowns");
        boolean useLastGroupCooldown = false;
        if (!isSectionNull(groupCooldowns) && plugin.getPerms() != null) {
            for (String groupName : groupCooldowns.getKeys(false)) {
                int cd = groupCooldowns.getInt(groupName);
                groupCooldownsMap.put(groupName, cd);
            }
            useLastGroupCooldown = cooldown.getBoolean("use_last_group_cooldown", false);
        }
        int teleportCooldown = cooldown.getInt("teleport_cooldown", -1);

        return new Cooldown(defaultCooldown, playerCooldowns, groupCooldownsMap, useLastGroupCooldown, teleportCooldown);
    }

    private BossBar setupChannelBossBar(ConfigurationSection bossbar) {
        if (isSectionNull(bossbar)) {
            return new BossBar(false, null, null, null);
        }
        boolean enabled = bossbar.getBoolean("enabled");
        String title = Utils.COLORIZER.colorize(bossbar.getString("title"));
        BarColor color = BarColor.valueOf(bossbar.getString("color").toUpperCase());
        BarStyle style = BarStyle.valueOf(bossbar.getString("style").toUpperCase());

        return new BossBar(enabled, title, color, style);
    }

    private Particles setupChannelParticles(ConfigurationSection particles) {
        if (isSectionNull(particles)) {
            return new Particles(
                    false, null, -1, -1, -1, false, false, false,
                    false, null, -1, -1, -1);
        }
        boolean preTeleportEnabled = false;
        Particle preTeleportId = null;
        int preTeleportDots = 0;
        double preTeleportRadius = 0;
        double preTeleportSpeed = 0;
        boolean preTeleportInvert = false;
        boolean preTeleportJumping = false;
        boolean preTeleportMoveNear = false;
        boolean afterTeleportParticleEnabled = false;
        Particle afterTeleportParticle = null;
        int afterTeleportCount = 0;
        double afterTeleportRadius = 0;
        double afterTeleportSpeed = 0;
        ConfigurationSection preTeleport = particles.getConfigurationSection("pre_teleport");
        if (!isSectionNull(preTeleport)) {
            preTeleportEnabled = preTeleport.getBoolean("enabled", false);
            preTeleportId = Particle.valueOf(preTeleport.getString("id"));
            preTeleportDots = preTeleport.getInt("dots");
            preTeleportRadius = preTeleport.getDouble("radius");
            preTeleportSpeed = preTeleport.getDouble("speed");
            preTeleportInvert = preTeleport.getBoolean("invert");
            preTeleportJumping = preTeleport.getBoolean("jumping");
            preTeleportMoveNear = preTeleport.getBoolean("move_near");
        }
        ConfigurationSection afterTeleport = particles.getConfigurationSection("after_teleport");
        if (!isSectionNull(afterTeleport)) {
            afterTeleportParticleEnabled = afterTeleport.getBoolean("enabled", false);
            afterTeleportParticle = Particle.valueOf(afterTeleport.getString("id"));
            afterTeleportCount = afterTeleport.getInt("count");
            afterTeleportRadius = afterTeleport.getDouble("radius");
            afterTeleportSpeed = afterTeleport.getDouble("speed");
        }

        return new Particles(
                preTeleportEnabled, preTeleportId, preTeleportDots, preTeleportRadius, preTeleportSpeed, preTeleportInvert, preTeleportJumping, preTeleportMoveNear,
                afterTeleportParticleEnabled, afterTeleportParticle, afterTeleportCount, afterTeleportRadius, afterTeleportSpeed);
    }

    private Restrictions setupChannelRestrictions(ConfigurationSection restrictions) {
        boolean isNullSection = isSectionNull(restrictions);
        boolean restrictMove = !isNullSection && restrictions.getBoolean("move", false);
        boolean restrictTeleport = !isNullSection && restrictions.getBoolean("teleport", false);
        boolean restrictDamage = !isNullSection && restrictions.getBoolean("damage", false);
        boolean restrictDamageOthers = !isNullSection && restrictions.getBoolean("damage_others", false);
        boolean damageCheckOnlyPlayers = !isNullSection && restrictions.getBoolean("damage_check_only_players", false);

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
        boolean isNullSection = isSectionNull(actions);
        List<Action> preTeleportActions = isNullSection ? List.of() : getActionList(actions.getStringList("pre_teleport"));
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
        List<Action> afterTeleportActions = isNullSection ? List.of() : getActionList(actions.getStringList("after_teleport"));

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
        Messages defaultMessages = pluginConfig.getDefaultChannelMessages();
        if (isSectionNull(messages)) {
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

    private boolean isSectionNull(ConfigurationSection section) {
        return section == null;
    }

    public Channel getChannelByName(String channelName) {
        if (channelName.isEmpty()) {
            return null;
        }
        return namedChannels.get(channelName);
    }

    public boolean hasActiveTasks(String playerName) {
        return !perPlayerActiveRtpTask.isEmpty() && perPlayerActiveRtpTask.containsKey(playerName);
    }

    public final List<String> teleportingNow = new ArrayList<>();

    public void preTeleport(Player p, Channel channel, World world) {
        if (teleportingNow.contains(p.getName())) {
            return;
        }
        if (Utils.DEBUG) {
            plugin.getPluginLogger().info("Pre teleporting player " + p.getName() + " with channel " + channel.id() + " in world " + world.getName());
        }
        teleportingNow.add(p.getName());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            locationGenerator.getIterationsPerPlayer().put(p.getName(), 1);
            Location loc = switch (channel.type()) {
                case DEFAULT -> locationGenerator.generateRandomLocation(p, channel, world);
                case NEAR_PLAYER -> locationGenerator.generateRandomLocationNearPlayer(p, channel, world);
                case NEAR_REGION -> locationGenerator.getWgLocationGenerator() != null ?
                        locationGenerator.getWgLocationGenerator().generateRandomLocationNearRandomRegion(p, channel, world) :
                        locationGenerator.generateRandomLocation(p, channel, world);
            };
            if (loc == null) {
                teleportingNow.remove(p.getName());
                Utils.sendMessage(channel.messages().failToFindLocation(), p);
                this.returnCost(p, channel);
                return;
            }
            if (channel.cooldown().teleportCooldown() > 0) {
                this.executeActions(p, channel, channel.actions().preTeleportActions(), p.getLocation());
                RtpTask rtpTask = new RtpTask(plugin, this, p.getName(), channel);
                rtpTask.startPreTeleportTimer(p, channel, loc);
                return;
            }
            this.teleportPlayer(p, channel, loc);
        });
    }

    public boolean takeCost(Player p, Channel channel) {
        Costs costs = channel.costs();
        return costs.processMoneyCost(p, channel) &&
                costs.processHungerCost(p, channel) &&
                costs.processExpCost(p, channel);
    }

    private void returnCost(Player p, Channel channel) {
        Costs costs = channel.costs();
        costs.processMoneyReturn(p);
        costs.processHungerReturn(p);
        costs.processExpReturn(p);
    }

    public void teleportPlayer(Player p, Channel channel, Location loc) {
        if (Utils.DEBUG) {
            plugin.getPluginLogger().info("Teleporting player " + p.getName() + " with channel " + channel.id() + " to location " + loc.toString());
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (channel.invulnerableTicks() > 0) {
                p.setInvulnerable(true);
                Bukkit.getScheduler().runTaskLater(plugin, () -> p.setInvulnerable(false), channel.invulnerableTicks());
            }
            p.teleport(loc);
            this.spawnParticles(p, channel.particles());
            teleportingNow.remove(p.getName());
            this.handlePlayerCooldown(p, channel.cooldown());
            this.executeActions(p, channel, channel.actions().afterTeleportActions(), loc);
        });
    }

    public void spawnParticles(Player p, Particles particles) {
        if (particles == null || !particles.afterTeleportEnabled()) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            final Location loc = p.getLocation();
            loc.add(0, 1, 0);
            final World world = loc.getWorld();

            final double goldenAngle = Math.PI * (3 - Math.sqrt(5));

            for (int i = 0; i < particles.afterTeleportCount(); i++) {
                double yOffset = 1 - (2.0 * i) / (particles.afterTeleportCount() - 1);
                double radiusAtHeight = Math.sqrt(1 - yOffset * yOffset);

                double theta = goldenAngle * i;

                double xOffset = particles.afterTeleportRadius() * radiusAtHeight * Math.cos(theta);
                double zOffset = particles.afterTeleportRadius() * radiusAtHeight * Math.sin(theta);

                Location particleLocation = loc.clone().add(xOffset, yOffset * particles.afterTeleportRadius(), zOffset);

                world.spawnParticle(particles.afterTeleportId(), particleLocation, 1, 0, 0, 0, particles.afterTeleportSpeed());
            }
        }, 1L);
    }

    private void handlePlayerCooldown(Player p, Cooldown cooldown) {
        int cooldownTime = getChannelCooldown(p, cooldown);
        if (getChannelCooldown(p, cooldown) > 0 && !p.hasPermission("rtp.bypasscooldown")) {
            cooldown.playerCooldowns().put(p.getName(), System.currentTimeMillis(), cooldownTime);
        }
    }

    public int getChannelCooldown(Player p, Cooldown cooldown) {
        if (cooldown.defaultCooldown() < 0) {
            return -1;
        }
        Object2IntSortedMap<String> groupCooldowns = cooldown.groupCooldowns();
        if (groupCooldowns.isEmpty()) {
            return cooldown.defaultCooldown();
        }
        final String playerGroup = plugin.getPerms().getPrimaryGroup(p);
        int defaultCooldown = cooldown.useLastGroupCooldown()
                ? groupCooldowns.getInt(new ArrayList<>(groupCooldowns.keySet()).get(groupCooldowns.size() - 1))
                : cooldown.defaultCooldown();
        return groupCooldowns.getOrDefault(playerGroup, defaultCooldown);
    }

    private final String[] searchList = {"%player%", "%name%", "%time%", "%x%", "%y%", "%z%"};

    public void executeActions(Player p, Channel channel, List<Action> actions, Location loc) {
        if (actions.isEmpty()) {
            return;
        }
        String name = channel.name();
        String cd = Utils.getTime(channel.cooldown().teleportCooldown());
        String x = Integer.toString(loc.getBlockX());
        String y = Integer.toString(loc.getBlockY());
        String z = Integer.toString(loc.getBlockZ());
        final String[] replacementList = {p.getName(), name, cd, x, y, z};
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (int i = 0; i < actions.size(); i++) {
                actions.get(i).perform(channel, p, searchList, replacementList);
            }
        });
    }
}
