package ru.overwrite.rtp.locationgenerator;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.util.XoRoShiRo128PlusRandom;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import ru.overwrite.rtp.RtpManager;
import ru.overwrite.rtp.channels.settings.Avoidance;
import ru.overwrite.rtp.channels.settings.LocationGenOptions;
import ru.overwrite.rtp.utils.Utils;
import ru.overwrite.rtp.utils.regions.TownyUtils;
import ru.overwrite.rtp.utils.regions.WGUtils;

public abstract class AbstractLocationGenerator implements LocationGenerator {

    protected final RtpManager rtpManager;

    @Getter
    protected final XoRoShiRo128PlusRandom random;

    @Getter
    protected final Reference2IntOpenHashMap<String> iterationsPerPlayer;

    protected AbstractLocationGenerator(RtpManager rtpManager) {
        this.rtpManager = rtpManager;
        this.random = new XoRoShiRo128PlusRandom();
        this.iterationsPerPlayer = new Reference2IntOpenHashMap<>();
    }

    protected boolean hasReachedMaxIterations(String playerName, LocationGenOptions locationGenOptions) {
        int iterations = iterationsPerPlayer.getInt(playerName);
        rtpManager.printDebug("Iterations for player '" + playerName + "': " + iterations);
        if (iterations >= locationGenOptions.maxLocationAttempts()) {
            iterationsPerPlayer.removeInt(playerName);
            rtpManager.printDebug("Max iterations reached for player " + playerName);
            return true;
        }
        return false;
    }

    protected int findSafeYPoint(World world, int x, int z) {
        return world.getEnvironment() != World.Environment.NETHER ? world.getHighestBlockYAt(x, z) : findSafeNetherYPoint(world, x, z);
    }

    protected int findSafeNetherYPoint(World world, int x, int z) {
        for (int y = 32; y < 90; y++) {
            Location location = new Location(world, x, y, z);

            if (location.getBlock().getType().isSolid() && !isInsideBlocks(location, false)) {
                return location.getBlockY();
            }
        }
        return -1;
    }

    protected boolean isInsideRadiusSquare(int x, int z, int minX, int minZ, int maxX, int maxZ, int centerX, int centerZ) {
        int realMinX = centerX + minX;
        int realMinZ = centerZ + minZ;
        int realMaxX = centerX + maxX;
        int realMaxZ = centerZ + maxZ;

        return (x >= realMinX && x <= realMaxX && z >= realMinZ && z <= realMaxZ);
    }

    protected boolean isInsideRadiusCircle(int x, int z, int minX, int minZ, int maxX, int maxZ, int centerX, int centerZ) {
        int deltaX = x - centerX;
        int deltaZ = z - centerZ;

        double maxDistanceRatioX = (double) deltaX / maxX;
        double maxDistanceRatioZ = (double) deltaZ / maxZ;
        double maxDistance = maxDistanceRatioX * maxDistanceRatioX + maxDistanceRatioZ * maxDistanceRatioZ;

        double minDistanceRatioX = (double) deltaX / minX;
        double minDistanceRatioZ = (double) deltaZ / minZ;
        double minDistance = minDistanceRatioX * minDistanceRatioX + minDistanceRatioZ * minDistanceRatioZ;

        return maxDistance <= 1 && minDistance >= 2;
    }

    protected boolean isLocationRestricted(Location location, Avoidance avoidance) {
        if (isOutsideWorldBorder(location)) {
            rtpManager.printDebug(() -> "Location " + Utils.locationToString(location) + " is outside the world border.");
            return true;
        }
        if (location.getWorld().getEnvironment() != World.Environment.NETHER && isInsideBlocks(location, true)) {
            rtpManager.printDebug(() -> "Location " + Utils.locationToString(location) + " is inside blocks.");
            return true;
        }
        Block block = location.getBlock();
        if (isDisallowedBlock(block, avoidance)) {
            rtpManager.printDebug(() -> "Location " + Utils.locationToString(location) + " contains a disallowed block.");
            return true;
        }
        if (isDisallowedBiome(block, avoidance)) {
            rtpManager.printDebug(() -> "Location " + Utils.locationToString(location) + " is in a disallowed biome.");
            return true;
        }
        if (isInsideRegion(location, avoidance)) {
            rtpManager.printDebug(() -> "Location " + Utils.locationToString(location) + " is inside a disallowed region.");
            return true;
        }
        if (isInsideTown(location, avoidance)) {
            rtpManager.printDebug(() -> "Location " + Utils.locationToString(location) + " is inside a disallowed town.");
            return true;
        }
        return false;
    }

    private boolean isOutsideWorldBorder(Location location) {
        return !location.getWorld().getWorldBorder().isInside(location);
    }

    private boolean isInsideBlocks(Location location, boolean onlyCheckOneBlockUp) {
        Location aboveLocation = location.clone().add(0, 2, 0);
        if (!aboveLocation.getBlock().getType().isAir()) {
            return true;
        }
        return !onlyCheckOneBlockUp && !aboveLocation.subtract(0, 1, 0).getBlock().getType().isAir();
    }

    private boolean isDisallowedBlock(Block block, Avoidance avoidance) {
        if (avoidance.avoidBlocks().isEmpty()) {
            return false;
        }
        return avoidance.avoidBlocksBlacklist() == avoidance.avoidBlocks().contains(block.getType());
    }

    private boolean isDisallowedBiome(Block block, Avoidance avoidance) {
        if (avoidance.avoidBiomes().isEmpty()) {
            return false;
        }
        return avoidance.avoidBiomesBlacklist() == avoidance.avoidBiomes().contains(block.getBiome());
    }

    private boolean isInsideRegion(Location loc, Avoidance avoidance) {
        if (!avoidance.avoidRegions()) {
            return false;
        }
        ApplicableRegionSet regionSet = WGUtils.getApplicableRegions(loc);
        return regionSet != null && !regionSet.getRegions().isEmpty();
    }

    private boolean isInsideTown(Location loc, Avoidance avoidance) {
        return avoidance.avoidTowns() && TownyUtils.getTownByLocation(loc) != null;
    }
}