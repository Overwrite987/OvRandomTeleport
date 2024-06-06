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

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import lombok.Getter;
import ru.overwrite.rtp.actions.Action;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.channels.ChannelType;
import ru.overwrite.rtp.utils.Config;
import ru.overwrite.rtp.utils.TownyUtils;
import ru.overwrite.rtp.utils.Utils;
import ru.overwrite.rtp.utils.WGUtils;

public class RtpManager {
	
	private final Main plugin;
	private final Config pluginConfig;
	
	@Getter
	private Channel defaultChannel;
	
	@Getter
	private final Map<String, Channel> namedChannels = new HashMap<>();
	
	@Getter
	public final Map<Channel, String> voidChannels = new HashMap<>();
	
	private final Random random = new Random();
	
	public RtpManager(Main plugin) {
		this.plugin = plugin;
		this.pluginConfig = plugin.getPluginConfig();
	}
	
	public void setupChannels(FileConfiguration config, PluginManager pluginManager) {
		for (String channelId : config.getConfigurationSection("channels").getKeys(false)) {
			ConfigurationSection channelSection = config.getConfigurationSection("channels." + channelId);
			String name = channelSection.getString("name", "Абстрактный канал");
			ChannelType type = channelSection.getString("type") == null ? ChannelType.DEFAULT : ChannelType.valueOf(channelSection.getString("type").toUpperCase());
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
			String prefix = messages == null ? pluginConfig.messages_prefix : messages.getString("prefix");
			String noPermsMessage = messages == null ? pluginConfig.messages_no_perms : pluginConfig.getPrefixed(messages.getString("no_perms"), prefix);
			String invalidWorldMessage = messages == null ? pluginConfig.messages_invalid_world : pluginConfig.getPrefixed(messages.getString("invalid_world"), prefix);
			String notEnoughMoneyMessage = messages == null ? pluginConfig.messages_not_enough_money : pluginConfig.getPrefixed(messages.getString("not_enough_money"), prefix);
			String cooldownMessage = messages == null ? pluginConfig.messages_cooldown : pluginConfig.getPrefixed(messages.getString("cooldown"), prefix);
			String movedOnTeleportMessage = messages == null ? pluginConfig.messages_moved_on_teleport : pluginConfig.getPrefixed(messages.getString("moved_on_teleport"), prefix);
			String damagedOnTeleportMessage = messages == null ? pluginConfig.messages_damaged_on_teleport : pluginConfig.getPrefixed(messages.getString("damaged_on_teleport"), prefix);
			String failToFindLocationMessage = messages == null ? pluginConfig.messages_fail_to_find_location : pluginConfig.getPrefixed(messages.getString("fail_to_find_location"), prefix);
			String alreadyTeleportingMessage = messages == null ? pluginConfig.messages_already_teleporting: pluginConfig.getPrefixed(messages.getString("already_teleporting"), prefix);
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
	
	// ...
	public final List<String> teleportingNow = new ArrayList<>();
	
	public void preTeleport(Player p, Channel channel, World world) {
		if (teleportingNow.contains(p.getName())) {
			return;
		}
		teleportingNow.add(p.getName());
		switch (channel.getType()) {
			case DEFAULT: {
				Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
					Location loc = generateRandomLocation(p, channel, world);
					if (loc == null) {
						teleportingNow.remove(p.getName());
						p.sendMessage(pluginConfig.messages_fail_to_find_location);
						return;
					}
					if (channel.getTeleportCooldown() > 0) {
						this.executeActions(p, channel, channel.getPreTeleportActions(), p.getLocation());
						startPreTeleportTimer(p, channel, loc);
						return;
					}
					teleportPlayer(p, channel, loc);
				});
				break;
			}
			case NEAR_PLAYER: {
				Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
					Location loc = generateRandomLocationNearPlayer(p, channel, world);
					if (loc == null) {
						teleportingNow.remove(p.getName());
						p.sendMessage(pluginConfig.messages_fail_to_find_location);
						return;
					}
					if (channel.getTeleportCooldown() > 0) {
						this.executeActions(p, channel, channel.getPreTeleportActions(), p.getLocation());
						startPreTeleportTimer(p, channel, loc);
						return;
					}
					teleportPlayer(p, channel, loc);
				});
				break;
			}
			case NEAR_REGION: {
				Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
					Location loc = WGUtils.generateRandomLocationNearRandomRegion(p, channel, world);
					if (loc == null) {
						teleportingNow.remove(p.getName());
						p.sendMessage(pluginConfig.messages_fail_to_find_location);
						return;
					}
					if (channel.getTeleportCooldown() > 0) {
						this.executeActions(p, channel, channel.getPreTeleportActions(), p.getLocation());
						startPreTeleportTimer(p, channel, loc);
						return;
					}
					teleportPlayer(p, channel, loc);
				});
				break;
			}
			default: {
				break;
			}
		}
	}
	
	private final Map<String, Integer> iterationsPerPlayer = new HashMap<>();
	
	private Location generateRandomLocation(Player p, Channel channel, World world) {
	    if (iterationsPerPlayer.getOrDefault(p.getName(), 0) > channel.getMaxLocationAttempts()) {
	    	iterationsPerPlayer.remove(p.getName());
	        return null;
	    }
	    
	    String shape = channel.getShape();
	    Location location;
	    
	    switch (shape) {
	    	case "SQUARE":	{
	    		location = generateRandomSquareLocation(p, channel, world);
	    		break;
	    	}
	    	case "ROUND":	{
	    		location = generateRandomRoundLocation(p, channel, world);
	    		break;
	    	}
	    	default: {
	    		location = null;
	    		return null;
	    	}
	    }
	    
	    if (location == null) {
	        iterationsPerPlayer.put(p.getName(), iterationsPerPlayer.getOrDefault(p.getName(), 0)+1);
	        return generateRandomLocation(p, channel, world);
	    } else {
	        iterationsPerPlayer.remove(p.getName());
	        return location;
	    }
	}

	private Location generateRandomSquareLocation(Player p, Channel channel, World world) {
	    int minX = channel.getMinX();
	    int maxX = channel.getMaxX();
	    int minZ = channel.getMinZ();
	    int maxZ = channel.getMaxZ();

	    int x = random.nextInt((maxX - minX) + 1) + minX;
	    int z = random.nextInt((maxZ - minZ) + 1) + minZ;
	    
	    int y = world.getEnvironment() != Environment.NETHER ? world.getHighestBlockYAt(x, z) : Utils.findSafeNetherLocation(world, x, z);
	    if (y < 0) {
	    	return null;
	    }

	    Location location = new Location(world, x + 0.5, y, z + 0.5, p.getLocation().getYaw(), p.getLocation().getPitch());
	    if (isDisallowedBlock(location, channel) || isDisallowedBiome(location, channel) || isInsideRegion(location, channel) || isInsideTown(location, channel)) {
	        return null;
	    } else {
	        location.setY(y + 1);
	        return location;
	    }
	}

	private Location generateRandomRoundLocation(Player p, Channel channel, World world) {
	    int minX = channel.getMinX();
	    int maxX = channel.getMaxX();
	    int minZ = channel.getMinZ();
	    int maxZ = channel.getMaxZ();

	    int centerX = (minX + maxX) / 2;
	    int centerZ = (minZ + maxZ) / 2;
	    int radiusX = (maxX - minX) / 2;
	    int radiusZ = (maxZ - minZ) / 2;
	    
	    double theta = random.nextDouble() * 2 * Math.PI;
	    double r = Math.sqrt(random.nextDouble());

	    int x = (int) (centerX + r * radiusX * Math.cos(theta));
	    int z = (int) (centerZ + r * radiusZ * Math.sin(theta));
	    
	    int y = world.getEnvironment() != Environment.NETHER ? world.getHighestBlockYAt(x, z) : Utils.findSafeNetherLocation(world, x, z);
	    if (y < 0) {
	    	return null;
	    }

	    Location location = new Location(world, x + 0.5, y, z + 0.5, p.getLocation().getYaw(), p.getLocation().getPitch());
	    if (isDisallowedBlock(location, channel) || isDisallowedBiome(location, channel) || isInsideRegion(location, channel) || isInsideTown(location, channel)) {
	        return null;
	    } else {
	        location.setY(y + 1);
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
    			location = generateRandomSquareLocationNearPlayer(p, targetPlayer, channel, world);
    			break;
    		}
    		case "ROUND":	{
    			location = generateRandomRoundLocationNearPlayer(p, targetPlayer, channel, world);
    			break;
    		}
    		default: {
    			location = null;
    			return null;
    		}
	    }

	    if (location == null) {
	        iterationsPerPlayer.put(p.getName(), iterationsPerPlayer.getOrDefault(p.getName(), 0)+1);
	        return generateRandomLocation(p, channel, world);
	    } else {
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

	private Location generateRandomSquareLocationNearPlayer(Player sourcePlayer, Player targetPlayer, Channel channel, World world) {
	    int minX = channel.getMinX();
	    int maxX = channel.getMaxX();
	    int minZ = channel.getMinZ();
	    int maxZ = channel.getMaxZ();

	    int centerX = targetPlayer.getLocation().getBlockX();
	    int centerZ = targetPlayer.getLocation().getBlockZ();

	    int x, z;
	    do {
	        x = centerX + random.nextInt(61) - 30;
	        z = centerZ + random.nextInt(61) - 30;
	    } while (x < minX || x > maxX || z < minZ || z > maxZ);

	    int y = world.getEnvironment() != Environment.NETHER ? world.getHighestBlockYAt(x, z) : Utils.findSafeNetherLocation(world, x, z);
	    if (y < 0) {
	    	return null;
	    }

	    Location location = new Location(world, x + 0.5, y, z + 0.5, sourcePlayer.getLocation().getYaw(), sourcePlayer.getLocation().getPitch());
	    if (isDisallowedBlock(location, channel) || isDisallowedBiome(location, channel) || isInsideRegion(location, channel) || isInsideTown(location, channel)) {
	        return null;
	    } else {
	        location.setY(y + 1);
	        return location;
	    }
	}

	private Location generateRandomRoundLocationNearPlayer(Player sourcePlayer, Player targetPlayer, Channel channel, World world) {
	    int minX = channel.getMinX();
	    int maxX = channel.getMaxX();
	    int minZ = channel.getMinZ();
	    int maxZ = channel.getMaxZ();

	    int centerX = targetPlayer.getLocation().getBlockX();
	    int centerZ = targetPlayer.getLocation().getBlockZ();
	    int radiusMin = 30;
	    int radiusMax = 60;

	    int x, z;
	    do {
	        double theta = random.nextDouble() * 2 * Math.PI;
	        double r = radiusMin + (radiusMax - radiusMin) * Math.sqrt(random.nextDouble());
	        x = (int) (centerX + r * Math.cos(theta));
	        z = (int) (centerZ + r * Math.sin(theta));
	    } while (x < minX || x > maxX || z < minZ || z > maxZ);

	    int y = world.getEnvironment() != Environment.NETHER ? world.getHighestBlockYAt(x, z) : Utils.findSafeNetherLocation(world, x, z);
	    if (y < 0) {
	    	return null;
	    }

	    Location location = new Location(world, x + 0.5, y, z + 0.5, sourcePlayer.getLocation().getYaw(), sourcePlayer.getLocation().getPitch());
	    if (isDisallowedBlock(location, channel) || isDisallowedBiome(location, channel) || isInsideRegion(location, channel) || isInsideTown(location, channel)) {
	        return null;
	    } else {
	        location.setY(y + 1);
	        return location;
	    }
	}
	
	// ............................?
	// TODO: Replace all this with abstract RTPTask
	public final Map<String, BossBar> perPlayerBossBar = new ConcurrentHashMap<>();
	public final Map<String, Integer> perPlayerPreTeleportCooldown = new ConcurrentHashMap<>();
	public final Map<String, BukkitTask> perPlayerActiveRtpTask = new ConcurrentHashMap<>();
	public final Map<String, Channel> perPlayerActiveRtpChannel = new ConcurrentHashMap<>();
	
	private void startPreTeleportTimer(Player p, Channel channel, Location loc) {
		String playerName = p.getName();
		perPlayerPreTeleportCooldown.put(playerName, channel.getTeleportCooldown());
		if (channel.isBossbarEnabled()) {
			if (perPlayerBossBar.get(playerName) == null) {
				String barTitle = Utils.colorize(channel.getBossbarTitle().replace("%time%", Utils.getTime(channel.getTeleportCooldown())));
				BossBar bossbar = Bukkit.createBossBar(barTitle, channel.getBossbarColor(), channel.getBossbarType());
				perPlayerBossBar.put(playerName, bossbar);
				bossbar.addPlayer(p);
			}
		}
		BukkitTask runnable = (new BukkitRunnable() {
			@Override
			public void run() {
				perPlayerPreTeleportCooldown.compute(p.getName(), (k, currentTimeRemaining) -> currentTimeRemaining - 1);
				if (perPlayerPreTeleportCooldown.get(playerName) <= 0) {
					if (perPlayerBossBar.containsKey(playerName)) {
						perPlayerBossBar.get(playerName).removeAll();
						perPlayerBossBar.remove(playerName);
					}
					perPlayerPreTeleportCooldown.remove(playerName);
					perPlayerActiveRtpTask.remove(playerName);
					perPlayerActiveRtpChannel.remove(playerName);
					teleportPlayer(p, channel, loc);
					cancel();
					return;
				}
				if (channel.isBossbarEnabled()) {
					double percents = (channel.getTeleportCooldown() - (channel.getTeleportCooldown() - perPlayerPreTeleportCooldown.get(playerName)))
							/ (double) channel.getTeleportCooldown();
					if (percents < 1 && percents > 0) {
						perPlayerBossBar.get(playerName).setProgress(percents);
					}
					String barTitle = Utils.colorize(channel.getBossbarTitle().replace("%time%", Utils.getTime(perPlayerPreTeleportCooldown.get(playerName))));
					perPlayerBossBar.get(playerName).setTitle(barTitle);
				}
				if (!channel.getOnCooldownActions().isEmpty()) {
					for (int i : channel.getOnCooldownActions().keySet()) {
						if (i == perPlayerPreTeleportCooldown.get(playerName)) {
							executeActions(p, channel, channel.getOnCooldownActions().get(i), p.getLocation());
						}
					}
				}
			}
		}).runTaskTimerAsynchronously(plugin, 20L, 20L);
		perPlayerActiveRtpTask.put(playerName, runnable);
		perPlayerActiveRtpChannel.put(playerName, channel);
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
	
	private boolean isDisallowedBlock(Location loc, Channel channel) {
		if (channel.isAvoidBlocksBlacklist()) {
			return !channel.getAvoidBlocks().isEmpty() && channel.getAvoidBlocks().contains(loc.getBlock().getType());
		} else {
			return !channel.getAvoidBlocks().isEmpty() && !channel.getAvoidBlocks().contains(loc.getBlock().getType());
		}
	}

	private boolean isDisallowedBiome(Location loc, Channel channel) {
		if (channel.isAvoidBiomesBlacklist()) {
			return !channel.getAvoidBiomes().isEmpty() && channel.getAvoidBiomes().contains(loc.getBlock().getBiome());
		} else {
			return !channel.getAvoidBiomes().isEmpty() && !channel.getAvoidBiomes().contains(loc.getBlock().getBiome());
		}
	}

	private boolean isInsideRegion(Location loc, Channel channel) {
		return channel.isAvoidRegions() && (WGUtils.getApplicableRegions(loc) != null && !WGUtils.getApplicableRegions(loc).getRegions().isEmpty());
	}

	private boolean isInsideTown(Location loc, Channel channel) {
		return channel.isAvoidTowns() && TownyUtils.getTownByLocation(loc) != null;
	}
	
	private void executeActions(Player p, Channel channel, List<Action> actions, Location loc) {
		if (actions.isEmpty()) {
			return;
		}
		for (Action action : actions) {
			switch (action.getType()) {
			case MESSAGE: {
				Bukkit.getScheduler().runTaskAsynchronously(plugin,() -> {
					String name = channel.getName();
					String cd = Utils.getTime(channel.getTeleportCooldown());
					String x = Integer.toString(loc.getBlockX());
					String y = Integer.toString(loc.getBlockY());
					String z = Integer.toString(loc.getBlockZ());
					String message = Utils.colorize(action.getContext().replace("%player%", p.getName()).replace("%name%", name).replace("%time%", cd)
							.replace("%x%", x).replace("%y%", y).replace("%z%", z));
					p.sendMessage(message);
				});
				break;
			}
			case SOUND: {
				Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
					String[] splittedContext = action.getContext().split(";");
					Sound sound = Sound.valueOf(splittedContext[0]);
					float volume = Float.parseFloat(splittedContext[1]);
					float pitch = Float.parseFloat(splittedContext[2]);
					p.playSound(p.getLocation(), sound, volume, pitch);
				});
				break;
			}
			case TITLE: {
				Bukkit.getScheduler().runTaskAsynchronously(plugin,() -> {
					String name = channel.getName();
					String cd = Utils.getTime(channel.getTeleportCooldown());
					String x = Integer.toString(loc.getBlockX());
					String y = Integer.toString(loc.getBlockY());
					String z = Integer.toString(loc.getBlockZ());
					String result = Utils.colorize(action.getContext().replace("%player%", p.getName()).replace("%name%", name).replace("%time%", cd)
							.replace("%x%", x).replace("%y%", y).replace("%z%", z));
					String[] titledMessage = result.split(";");
					Utils.sendTitleMessage(titledMessage, p);
				});
				break;
			}
			case EFFECT: {
				String[] effectSplitted = action.getContext().split(";");
				PotionEffectType effectType = PotionEffectType.getByName(effectSplitted[0]);
				int duration = Integer.parseInt(effectSplitted[1]);
				int amplifier = Integer.parseInt(effectSplitted[2]);
				PotionEffect effect = new PotionEffect(effectType, duration, amplifier);
				p.addPotionEffect(effect);
				break;
			}
			case CONSOLE: {
				String name = channel.getName();
				String cd = Utils.getTime(channel.getTeleportCooldown());
				String x = Integer.toString(loc.getBlockX());
				String y = Integer.toString(loc.getBlockY());
				String z = Integer.toString(loc.getBlockZ());
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), action.getContext().replace("%player%", p.getName()).replace("%name%", name).replace("%time%", cd)
						.replace("%x%", x).replace("%y%", y).replace("%z%", z));
				break;
			}
			default: {
				break;
			}
			}
		}
	}

}
