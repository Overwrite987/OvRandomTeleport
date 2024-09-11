package ru.overwrite.rtp.utils;

import java.util.List;
import java.util.Random;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionType;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.channels.settings.LocationGenOptions;

public class WGUtils {

    private static final Random random = new Random();
    public static StateFlag RTP_IGNORE_FLAG;

    public static void setupRtpFlag() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            StateFlag flag = new StateFlag("rtp-base-no-teleport", true);
            registry.register(flag);
            RTP_IGNORE_FLAG = flag;
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get("rtp-base-no-teleport");
            if (existing instanceof StateFlag) {
                RTP_IGNORE_FLAG = (StateFlag) existing;
            }
        }
    }

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
                .filter(region -> region.getFlag(RTP_IGNORE_FLAG) != StateFlag.State.ALLOW)
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
