package ru.overwrite.rtp.utils;

import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionType;

import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.channels.LocationGenOptions;

public class WGUtils {

    private static final Random random = new Random();

    public static Location generateRandomLocationNearRandomRegion(Player p, Channel channel, World world) {
        LocationGenOptions locationGenOptions = channel.getLocationGenOptions();
        if (LocationUtils.iterationsPerPlayer.getInt(p.getName()) >= locationGenOptions.maxLocationAttempts()) {
            LocationUtils.iterationsPerPlayer.removeInt(p.getName());
            return null;
        }
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(world));

        if (regionManager == null || regionManager.getRegions().isEmpty())
            return null;

        int minX = locationGenOptions.minX();
        int maxX = locationGenOptions.maxX();
        int minZ = locationGenOptions.minZ();
        int maxZ = locationGenOptions.maxZ();

        List<ProtectedRegion> regionsInRange = regionManager.getRegions().values().stream()
                .filter(region -> region.getType() != RegionType.GLOBAL)
                .filter(region -> {
                    BlockVector3 minPoint = region.getMinimumPoint();
                    BlockVector3 maxPoint = region.getMaximumPoint();
                    return minPoint.getX() >= minX && maxPoint.getX() <= maxX &&
                            minPoint.getZ() >= minZ && maxPoint.getZ() <= maxZ;
                })
                .toList();

        if (regionsInRange.isEmpty()) {
            return null;
        }

        ProtectedRegion randomRegion = regionsInRange.get(random.nextInt(regionsInRange.size()));

        int centerX = (randomRegion.getMinimumPoint().getX() + randomRegion.getMaximumPoint().getX()) / 2;
        int centerZ = (randomRegion.getMinimumPoint().getZ() + randomRegion.getMaximumPoint().getZ()) / 2;

        LocationGenOptions.Shape shape = locationGenOptions.shape();
        Location location = LocationUtils.generateRandomLocationNearPoint(shape, p, centerX, centerZ, channel, world);

        if (location == null) {
            LocationUtils.iterationsPerPlayer.addTo(p.getName(), 1);
            return generateRandomLocationNearRandomRegion(p, channel, world);
        } else {
            LocationUtils.iterationsPerPlayer.removeInt(p.getName());
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
