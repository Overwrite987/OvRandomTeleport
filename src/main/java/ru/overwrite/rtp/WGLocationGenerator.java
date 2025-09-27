package ru.overwrite.rtp;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.BukkitPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionType;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import ru.overwrite.rtp.channels.Settings;
import ru.overwrite.rtp.channels.settings.LocationGenOptions;
import ru.overwrite.rtp.utils.regions.WGUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class WGLocationGenerator {

    private final RtpManager rtpManager;
    private final LocationGenerator locationGenerator;

    public Location generateRandomLocationNearRandomRegion(Player player, Settings settings, World world) {
        LocationGenOptions locationGenOptions = settings.locationGenOptions();
        if (locationGenerator.hasReachedMaxIterations(player.getName(), locationGenOptions)) {
            return null;
        }

        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(world));

        Map<String, ProtectedRegion> regions;
        if (regionManager == null || (regions = regionManager.getRegions()).isEmpty()) {
            return null;
        }

        int minX = locationGenOptions.minX();
        int maxX = locationGenOptions.maxX();
        int minZ = locationGenOptions.minZ();
        int maxZ = locationGenOptions.maxZ();

        BukkitPlayer bukkitPlayer = new BukkitPlayer(WorldGuardPlugin.inst(), player);
        List<ProtectedRegion> regionsInRange = new ArrayList<>();
        for (ProtectedRegion region : regions.values()) {
            if (region.getType() == RegionType.GLOBAL) {
                continue;
            }

            if (region.isMember(bukkitPlayer)) {
                rtpManager.printDebug("Skipping region " + region.getId() + " since player is a member of it");
                continue;
            }

            boolean flag = Boolean.TRUE.equals(region.getFlag(WGUtils.RTP_IGNORE_FLAG));
            if (flag) {
                rtpManager.printDebug("Skipping region " + region.getId() + " since it has RTP_IGNORE_FLAG");
                continue;
            }

            BlockVector3 minPoint = region.getMinimumPoint();
            BlockVector3 maxPoint = region.getMaximumPoint();
            if (minPoint.getX() >= minX && maxPoint.getX() <= maxX && minPoint.getZ() >= minZ && maxPoint.getZ() <= maxZ) {
                regionsInRange.add(region);
            }
            rtpManager.printDebug("Skipping region " + region.getId() + " since it is outside of range");
        }

        if (regionsInRange.isEmpty()) {
            rtpManager.printDebug("No regions found to generate location near region");
            return null;
        }

        ProtectedRegion randomRegion = regionsInRange.get(locationGenerator.getRandom().nextInt(regionsInRange.size()));

        int centerX = (randomRegion.getMinimumPoint().getX() + randomRegion.getMaximumPoint().getX()) / 2;
        int centerZ = (randomRegion.getMinimumPoint().getZ() + randomRegion.getMaximumPoint().getZ()) / 2;

        LocationGenOptions.Shape shape = locationGenOptions.shape();
        Location location = locationGenerator.generateRandomLocationNearPoint(shape, player, centerX, centerZ, settings, world);

        if (location == null) {
            locationGenerator.getIterationsPerPlayer().addTo(player.getName(), 1);
            return generateRandomLocationNearRandomRegion(player, settings, world);
        }
        rtpManager.printDebug(() -> "Location for player '" + player.getName() + "' found in " + locationGenerator.getIterationsPerPlayer().getInt(player.getName()) + " iterations");
        locationGenerator.getIterationsPerPlayer().removeInt(player.getName());
        return location;
    }
}
