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
	            location = LocationUtils.generateRandomSquareLocationNearPoint(world, centerX, centerZ, channel);
	            break;
	        case "ROUND":
	            location = LocationUtils.generateRandomRoundLocationNearPoint(world, centerX, centerZ, channel);
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

	public static ApplicableRegionSet getApplicableRegions(Location location) {
		RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
				.get(BukkitAdapter.adapt(location.getWorld()));
		if (regionManager == null || regionManager.getRegions().isEmpty())
			return null;
		return regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
	}

}
