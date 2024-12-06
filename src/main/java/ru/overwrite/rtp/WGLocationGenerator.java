package ru.overwrite.rtp;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.BooleanFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionType;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.channels.settings.LocationGenOptions;
import ru.overwrite.rtp.utils.regions.WGUtils;

import java.util.ArrayList;
import java.util.List;

public class WGLocationGenerator {

    private final LocationGenerator locationGenerator;

    public WGLocationGenerator(LocationGenerator locationGenerator) {
        this.locationGenerator = locationGenerator;
    }

    public Location generateRandomLocationNearRandomRegion(Player player, Channel channel, World world) {
        LocationGenOptions locationGenOptions = channel.locationGenOptions();
        if (locationGenerator.hasReachedMaxIterations(player, locationGenOptions)) {
            return null;
        }

        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(world));

        if (regionManager == null || regionManager.getRegions().isEmpty()) {
            return null;
        }

        int minX = locationGenOptions.minX();
        int maxX = locationGenOptions.maxX();
        int minZ = locationGenOptions.minZ();
        int maxZ = locationGenOptions.maxZ();

        List<ProtectedRegion> regionsInRange = new ArrayList<>();
        for (ProtectedRegion region : regionManager.getRegions().values()) {
            if (region.getType() == RegionType.GLOBAL || region.getMembers().contains(player.getName())) {
                continue;
            }

            boolean flag = Boolean.TRUE.equals(region.getFlag(WGUtils.RTP_IGNORE_FLAG));
            if (flag) {
                continue;
            }

            BlockVector3 minPoint = region.getMinimumPoint();
            BlockVector3 maxPoint = region.getMaximumPoint();
            if (minPoint.getX() >= minX && maxPoint.getX() <= maxX && minPoint.getZ() >= minZ && maxPoint.getZ() <= maxZ) {
                regionsInRange.add(region);
            }
        }

        if (regionsInRange.isEmpty()) {
            return null;
        }

        ProtectedRegion randomRegion = regionsInRange.get(locationGenerator.random.nextInt(regionsInRange.size()));

        int centerX = (randomRegion.getMinimumPoint().getX() + randomRegion.getMaximumPoint().getX()) / 2;
        int centerZ = (randomRegion.getMinimumPoint().getZ() + randomRegion.getMaximumPoint().getZ()) / 2;

        LocationGenOptions.Shape shape = locationGenOptions.shape();
        Location location = locationGenerator.generateRandomLocationNearPoint(shape, player, centerX, centerZ, channel, world);

        if (location == null) {
            locationGenerator.getIterationsPerPlayer().addTo(player.getName(), 1);
            return generateRandomLocationNearRandomRegion(player, channel, world);
        } else {
            locationGenerator.getIterationsPerPlayer().removeInt(player.getName());
            return location;
        }
    }
}
