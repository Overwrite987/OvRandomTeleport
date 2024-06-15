package ru.overwrite.rtp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.channels.ChannelType;
import ru.overwrite.rtp.utils.*;

public class RtpManager {
	
	private final Main plugin;
	private final Config pluginConfig;
	
	@Getter
	private Channel defaultChannel;
	
	@Getter
	private final Map<String, Channel> namedChannels = new HashMap<>();
	
	@Getter
	public final Map<Channel, String> voidChannels = new HashMap<>();

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
			String name = channelSection.getString("name", "Абстрактный канал");
			ChannelType type = channelSection.getString("type") == null ? ChannelType.DEFAULT : ChannelType.valueOf(channelSection.getString("type").toUpperCase());
			if (type == ChannelType.NEAR_REGION && !pluginManager.isPluginEnabled("WorldGuard")) {
				type = ChannelType.DEFAULT;
			}
			List<World> activeWorlds = new ArrayList<>();
			for (String w : channelSection.getStringList("active_worlds")) {
				activeWorlds.add(Bukkit.getWorld(w));
			}
			boolean teleportToFirstAllowedWorld = channelSection.getBoolean("teleport_to_first_world", false);
			boolean teleportOnVoid = channelSection.getBoolean("teleport_on_void", false);
			double teleportCost = plugin.getEconomy() != null ? channelSection.getDouble("teleport_cost", -1) : -1;
			ConfigurationSection locationGenOptions = channelSection.getConfigurationSection("location_generation_options");
			if (locationGenOptions == null) {
				continue;
			}
			String shape = locationGenOptions.getString("shape").toUpperCase();
			int minX = locationGenOptions.getInt("min_x");
			int maxX = locationGenOptions.getInt("max_x");
			int minZ = locationGenOptions.getInt("min_z"); 
			int maxZ = locationGenOptions.getInt("max_z");
			int maxLocationAttempts = channelSection.getInt("max_location_attemps", 50);
			int invulnerableTicks = channelSection.getInt("invulnerable_after_teleport", 1);
			int cooldown = channelSection.getInt("cooldown", 60);
			int teleportCooldown = channelSection.getInt("teleport_cooldown", -1);
			ConfigurationSection bossbar = channelSection.getConfigurationSection("bossbar");
			boolean bossbarEnabled = bossbar != null && bossbar.getBoolean("enabled", false);
			String bossbarTitle = bossbar == null ? "" : Utils.colorize(bossbar.getString("title"));
			BarColor bossbarColor = bossbar == null ? BarColor.PURPLE : BarColor.valueOf(bossbar.getString("color").toUpperCase());
			BarStyle bossbarType = bossbar == null ? BarStyle.SOLID :  BarStyle.valueOf(bossbar.getString("style").toUpperCase());
			ConfigurationSection restrictions = channelSection.getConfigurationSection("restrictions");
			boolean restrictMove = restrictions != null && restrictions.getBoolean("move");
			boolean restrictDamage = restrictions != null && restrictions.getBoolean("damage");
			ConfigurationSection avoid = channelSection.getConfigurationSection("avoid");
			Set<Material> avoidBlocks = new HashSet<>();
			boolean avoidBlocksBlacklist = true;
			if (avoid != null) {
				avoidBlocksBlacklist= avoid.getBoolean("blocks.blacklist", true);
				for (String m : avoid.getStringList("blocks.list")) {
					avoidBlocks.add(Material.valueOf(m.toUpperCase()));
				}
			}
			Set<Biome> avoidBiomes = new HashSet<>();
			boolean avoidBiomesBlacklist = true;
			if (avoid != null) {
				avoidBiomesBlacklist = avoid.getBoolean("biomes.blacklist", true);
				for (String b : avoid.getStringList("biomes.list")) {
					avoidBiomes.add(Biome.valueOf(b.toUpperCase()));
				}
			}
			boolean avoidRegions = avoid != null && avoid.getBoolean("regions", false) && pluginManager.isPluginEnabled("WorldGuard");
			boolean avoidTowns = avoid != null && avoid.getBoolean("towns", false) && pluginManager.isPluginEnabled("Towny");
			ConfigurationSection actions = channelSection.getConfigurationSection("actions");
			List<Action> preTeleportActions = new ArrayList<>();
			for (String a : actions.getStringList("pre_teleport")) {
				preTeleportActions.add(Action.fromString(a));
			}
			Map<Integer, List<Action>> onCooldownActions = new HashMap<>();
			ConfigurationSection cdActions = actions.getConfigurationSection("on_cooldown");
			if (cdActions != null) {
				for (String s : cdActions.getKeys(false)) {
					if (!isNumber(s)) {
						continue;
					}
					int time = Integer.parseInt(s);
					List<Action> actionList = new ArrayList<>();
					for (String a : cdActions.getStringList(s)) {
						actionList.add(Action.fromString(a));
					}
					onCooldownActions.put(time, actionList);
				}
			}
			List<Action> afterTeleportActions = new ArrayList<>();
			for (String a : actions.getStringList("after_teleport")) {
				afterTeleportActions.add(Action.fromString(a));
			}
			ConfigurationSection messages = channelSection.getConfigurationSection("messages");
			String prefix = doesConfigValueExists(messages, "prefix")
					? pluginConfig.messages_prefix : messages.getString("prefix");
			String noPermsMessage = doesConfigValueExists(messages, "no_perms")
					? pluginConfig.messages_no_perms : pluginConfig.getPrefixed(messages.getString("no_perms"), prefix);
			String invalidWorldMessage = doesConfigValueExists(messages, "invalid_world")
					? pluginConfig.messages_invalid_world : pluginConfig.getPrefixed(messages.getString("invalid_world"), prefix);
			String notEnoughMoneyMessage = doesConfigValueExists(messages, "not_enough_money")
					? pluginConfig.messages_not_enough_money : pluginConfig.getPrefixed(messages.getString("not_enough_money"), prefix);
			String cooldownMessage = doesConfigValueExists(messages, "cooldown")
					? pluginConfig.messages_cooldown : pluginConfig.getPrefixed(messages.getString("cooldown"), prefix);
			String movedOnTeleportMessage = doesConfigValueExists(messages, "moved_on_teleport")
					? pluginConfig.messages_moved_on_teleport : pluginConfig.getPrefixed(messages.getString("moved_on_teleport"), prefix);
			String damagedOnTeleportMessage = doesConfigValueExists(messages, "damaged_on_teleport")
					? pluginConfig.messages_damaged_on_teleport : pluginConfig.getPrefixed(messages.getString("damaged_on_teleport"), prefix);
			String failToFindLocationMessage = doesConfigValueExists(messages, "fail_to_find_location")
					? pluginConfig.messages_fail_to_find_location : pluginConfig.getPrefixed(messages.getString("fail_to_find_location"), prefix);
			String alreadyTeleportingMessage = doesConfigValueExists(messages, "already_teleporting")
					? pluginConfig.messages_already_teleporting: pluginConfig.getPrefixed(messages.getString("already_teleporting"), prefix);
			Channel newChannel = new Channel(channelId,
					name,
					type,
					activeWorlds, 
					teleportToFirstAllowedWorld,
					teleportOnVoid,
					teleportCost,
					shape,
					minX, maxX,
					minZ, maxZ,
					maxLocationAttempts,
					invulnerableTicks,
					cooldown,
					teleportCooldown,
					bossbarEnabled,
					bossbarTitle,
					bossbarColor,
					bossbarType,
					restrictMove,
					restrictDamage, 
					avoidBlocksBlacklist,
					avoidBlocks,
					avoidBiomesBlacklist,
					avoidBiomes,
					avoidRegions,
					avoidTowns,
					preTeleportActions,
					onCooldownActions,
					afterTeleportActions,
					noPermsMessage,
					invalidWorldMessage,
					notEnoughMoneyMessage,
					cooldownMessage,
					movedOnTeleportMessage,
					damagedOnTeleportMessage,
					failToFindLocationMessage,
					alreadyTeleportingMessage);
			namedChannels.put(channelId, newChannel);
			if (teleportOnVoid) {
				voidChannels.put(newChannel, channelId);
			}
		}
		this.defaultChannel = getChannelByName(config.getString("main_settings.default_channel"));
	}

	private boolean doesConfigValueExists(ConfigurationSection section, String key) {
		return (section == null || section.getString(key) == null);
	}
	
	private boolean isNumber(String string) {
        if (string == null || string.isEmpty() || string.isBlank()) {
            return false;
        }
        for (char c : string.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }
	
	public Channel getChannelByName(String channelName) {
		return namedChannels.get(channelName);
	}

	public final List<String> teleportingNow = new ArrayList<>();
	
	public void preTeleport(Player p, Channel channel, World world) {
		if (teleportingNow.contains(p.getName())) {
			return;
		}
		teleportingNow.add(p.getName());
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			Location loc = null;
			switch (channel.getType()) {
				case DEFAULT: {
					loc = generateRandomLocation(p, channel, world);
					break;
				}
				case NEAR_PLAYER: {
					loc = generateRandomLocationNearPlayer(p, channel, world);
					break;
				}
				case NEAR_REGION: {
					loc = WGUtils.generateRandomLocationNearRandomRegion(p, channel, world);
					break;
				}
				default: {
					break;
				}
			}
			if (loc == null) {
				teleportingNow.remove(p.getName());
				p.sendMessage(pluginConfig.messages_fail_to_find_location);
				return;
			}
			if (channel.getTeleportCooldown() > 0) {
				this.executeActions(p, channel, channel.getPreTeleportActions(), p.getLocation());
				RtpTask rtpTask = new RtpTask(plugin, this, p.getName(), channel);
				perPlayerActiveRtpTask.put(p.getName(), rtpTask);
				rtpTask.startPreTeleportTimer(p, channel, loc);
				return;
			}
			teleportPlayer(p, channel, loc);
		});
	}
	
	private final Map<String, Integer> iterationsPerPlayer = new HashMap<>();
	
	private Location generateRandomLocation(Player p, Channel channel, World world) {
		if (Utils.DEBUG) {
			plugin.getPluginLogger().info("Iterations for player " + p.getName() + ": " + iterationsPerPlayer.getOrDefault(p.getName(), 0));
		}
	    if (iterationsPerPlayer.getOrDefault(p.getName(), 0) > channel.getMaxLocationAttempts()) {
	    	iterationsPerPlayer.remove(p.getName());
	        return null;
	    }
	    
	    String shape = channel.getShape();
	    Location location;
	    
	    switch (shape) {
	    	case "SQUARE":	{
	    		location = LocationUtils.generateRandomSquareLocation(p, channel, world);
	    		break;
	    	}
	    	case "ROUND":	{
	    		location = LocationUtils.generateRandomRoundLocation(p, channel, world);
	    		break;
	    	}
	    	default: {
	    		return null;
	    	}
	    }
	    
	    if (location == null) {
	        iterationsPerPlayer.put(p.getName(), iterationsPerPlayer.getOrDefault(p.getName(), 0)+1);
	        return generateRandomLocation(p, channel, world);
	    } else {
			if (Utils.DEBUG) {
				plugin.getPluginLogger().info("Location for player " + p.getName() + " found in " + iterationsPerPlayer.get(p.getName()) + "iterations");
			}
	        iterationsPerPlayer.remove(p.getName());
	        return location;
	    }
	}
	
	private Location generateRandomLocationNearPlayer(Player p, Channel channel, World world) {
		if (iterationsPerPlayer.getOrDefault(p.getName(), 0) > channel.getMaxLocationAttempts()) {
			iterationsPerPlayer.remove(p.getName());
		    return null;
		}
	    List<Player> nearbyPlayers = getNearbyPlayers(p, channel, world);

	    nearbyPlayers = nearbyPlayers.stream()
	        .filter(player -> !player.hasPermission("rtpnear.unaffected"))
	        .collect(Collectors.toList());

	    if (nearbyPlayers.isEmpty()) {
	        return null;
	    }

	    Player targetPlayer = nearbyPlayers.get(random.nextInt(nearbyPlayers.size()));

	    String shape = channel.getShape();
	    Location location;

	    switch (shape) {
    		case "SQUARE":	{
    			location = LocationUtils.generateRandomSquareLocationNearPlayer(p, targetPlayer, channel, world);
    			break;
    		}
    		case "ROUND":	{
    			location = LocationUtils.generateRandomRoundLocationNearPlayer(p, targetPlayer, channel, world);
    			break;
    		}
    		default: {
    			return null;
    		}
	    }

	    if (location == null) {
	        iterationsPerPlayer.put(p.getName(), iterationsPerPlayer.getOrDefault(p.getName(), 0)+1);
	        return generateRandomLocationNearPlayer(p, channel, world);
	    } else {
			if (Utils.DEBUG) {
				plugin.getPluginLogger().info("Location for player " + p.getName() + " found in " + iterationsPerPlayer.get(p.getName()) + " iterations");
			}
	        iterationsPerPlayer.remove(p.getName());
	        return location;
	    }
	}

	private List<Player> getNearbyPlayers(Player player, Channel channel, World world) {
	    List<Player> nearbyPlayers = new ArrayList<>();
	    int minX = channel.getMinX();
	    int maxX = channel.getMaxX();
	    int minZ = channel.getMinZ();
	    int maxZ = channel.getMaxZ();

	    for (Player p : world.getPlayers()) {
	        if (!p.equals(player)) {
	            int px = p.getLocation().getBlockX();
	            int pz = p.getLocation().getBlockZ();
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
			this.executeActions(p, channel, channel.getAfterTeleportActions(), loc);
		});
	}

	private final String[] searchList = { "%player%", "%name%", "%time%", "%x%", "%y%", "%z%"};
	
	public void executeActions(Player p, Channel channel, List<Action> actions, Location loc) {
		if (actions.isEmpty()) {
			return;
		}
		for (Action action : actions) {
			switch (action.getType()) {
				case MESSAGE: {
					Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
						String name = channel.getName();
						String cd = Utils.getTime(channel.getTeleportCooldown());
						String x = Integer.toString(loc.getBlockX());
						String y = Integer.toString(loc.getBlockY());
						String z = Integer.toString(loc.getBlockZ());
						String[] replacementList = { p.getName(), name, cd, x, y, z };
						String message = Utils.colorize(StringUtils.replaceEach(action.getContext(), searchList, replacementList));
						p.sendMessage(message);
					});
					break;
				}
				case TITLE: {
					Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
						String name = channel.getName();
						String cd = Utils.getTime(channel.getTeleportCooldown());
						String x = Integer.toString(loc.getBlockX());
						String y = Integer.toString(loc.getBlockY());
						String z = Integer.toString(loc.getBlockZ());
						String[] replacementList = { p.getName(), name, cd, x, y, z };
						String result = Utils.colorize(StringUtils.replaceEach(action.getContext(), searchList, replacementList));
						String[] titledMessage = result.split(";");
						Utils.sendTitleMessage(titledMessage, p);
					});
					break;
				}
				case SOUND: {
					Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
						Utils.sendSound(action.getContext().split(";"), p);
					});
					break;
				}
				case EFFECT: {
					Utils.giveEffect(action.getContext().split(";"), p);
					break;
				}
				case CONSOLE: {
					String name = channel.getName();
					String cd = Utils.getTime(channel.getTeleportCooldown());
					String x = Integer.toString(loc.getBlockX());
					String y = Integer.toString(loc.getBlockY());
					String z = Integer.toString(loc.getBlockZ());
					String[] replacementList = { p.getName(), name, cd, x, y, z };
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), StringUtils.replaceEach(action.getContext(), searchList, replacementList));
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
