package ru.overwrite.rtp.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionType;

import ru.overwrite.rtp.channels.Channel;

public class WGUtils {
	
	private static final Random random = new Random();
	
	private static final Map<String, Integer> iterationsPerPlayer = new HashMap<>();

	public static Location generateRandomLocationNearRandomRegion(Player p, Channel channel, World world) {
		if (iterationsPerPlayer.getOrDefault(p.getName(), 0) > channel.getMaxLocationAttempts()) {
	    	iterationsPerPlayer.remove(p.getName());
	        return null;
	    }
	    RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
				.get(BukkitAdapter.adapt(world));

	    int minX = channel.getMinX();
	    int maxX = channel.getMaxX();
	    int minZ = channel.getMinZ();
	    int maxZ = channel.getMaxZ();

	    List<ProtectedRegion> regionsInRange = regionManager.getRegions().values().stream()
	    	.filter(region -> region.getType() != RegionType.GLOBAL)
	        .filter(region -> {
	            BlockVector3 minPoint = region.getMinimumPoint();
	            BlockVector3 maxPoint = region.getMaximumPoint();
	            return minPoint.x() >= minX && maxPoint.x() <= maxX &&
	                   minPoint.z() >= minZ && maxPoint.z() <= maxZ;
	        })
	        .collect(Collectors.toList());

	    if (regionsInRange.isEmpty()) {
	        return null;
	    }

	    ProtectedRegion randomRegion = regionsInRange.get(random.nextInt(regionsInRange.size()));

	    int centerX = (randomRegion.getMinimumPoint().x() + randomRegion.getMaximumPoint().x()) / 2;
	    int centerZ = (randomRegion.getMinimumPoint().z() + randomRegion.getMaximumPoint().z()) / 2;

	    String shape = channel.getShape();
	    Location location;

	    switch (shape) {
	        case "SQUARE":
	            location = generateRandomSquareLocationNearPoint(world, centerX, centerZ, channel);
	            break;
	        case "ROUND":
	            location = generateRandomRoundLocationNearPoint(world, centerX, centerZ, channel);
	            break;
	        default:
	            location = null;
	            return null;
	    }

	    if (location == null) {
	    	iterationsPerPlayer.put(p.getName(), iterationsPerPlayer.getOrDefault(p.getName(), 0)+1);
	        return generateRandomLocationNearRandomRegion(p, channel, world);
	    } else {
	    	iterationsPerPlayer.remove(p.getName());
	        return location;
	    }
	}

	private static Location generateRandomSquareLocationNearPoint(World world, int centerX, int centerZ, Channel channel) {
	    int minX = channel.getMinX();
	    int maxX = channel.getMaxX();
	    int minZ = channel.getMinZ();
	    int maxZ = channel.getMaxZ();

	    int x, z;
	    do {
	        x = centerX + random.nextInt(61) - 30;
	        z = centerZ + random.nextInt(61) - 30;
	    } while (x < minX || x > maxX || z < minZ || z > maxZ);

	    int y = world.getEnvironment() != Environment.NETHER ? world.getHighestBlockYAt(x, z) : Utils.findSafeNetherLocation(world, x, z);
	    if (y < 0) {
	    	return null;
	    }

	    Location location = new Location(world, x + 0.5, y, z + 0.5);
	    if (isDisallowedBlock(location, channel) || isDisallowedBiome(location, channel) || isInsideRegion(location, channel) || isInsideTown(location, channel)) {
	        return null;
	    } else {
	        location.setY(y + 1);
	        return location;
	    }
	}

	private static Location generateRandomRoundLocationNearPoint(World world, int centerX, int centerZ, Channel channel) {
	    int minX = channel.getMinX();
	    int maxX = channel.getMaxX();
	    int minZ = channel.getMinZ();
	    int maxZ = channel.getMaxZ();

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

	    Location location = new Location(world, x + 0.5, y, z + 0.5);
	    if (isDisallowedBlock(location, channel) || isDisallowedBiome(location, channel) || isInsideRegion(location, channel) || isInsideTown(location, channel)) {
	        return null;
	    } else {
	        location.setY(y + 1);
	        return location;
	    }
	}
	
	private static boolean isDisallowedBlock(Location loc, Channel channel) {
		if (channel.isAvoidBlocksBlacklist()) {
			return !channel.getAvoidBlocks().isEmpty() && channel.getAvoidBlocks().contains(loc.getBlock().getType());
		} else {
			return !channel.getAvoidBlocks().isEmpty() && !channel.getAvoidBlocks().contains(loc.getBlock().getType());
		}
	}

	private static boolean isDisallowedBiome(Location loc, Channel channel) {
		if (channel.isAvoidBiomesBlacklist()) {
			return !channel.getAvoidBiomes().isEmpty() && channel.getAvoidBiomes().contains(loc.getBlock().getBiome());
		} else {
			return !channel.getAvoidBiomes().isEmpty() && !channel.getAvoidBiomes().contains(loc.getBlock().getBiome());
		}
	}

	private static boolean isInsideRegion(Location loc, Channel channel) {
		return channel.isAvoidRegions() && !getApplicableRegions(loc).getRegions().isEmpty();
	}

	private static boolean isInsideTown(Location loc, Channel channel) {
		return channel.isAvoidTowns() && TownyUtils.getTownByLocation(loc) != null;
	}

	public static ApplicableRegionSet getApplicableRegions(Location location) {
		RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
				.get(BukkitAdapter.adapt(location.getWorld()));
		if (regionManager == null || regionManager.getRegions().isEmpty())
			return null;
		return regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
	}

}
