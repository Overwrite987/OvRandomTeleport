package ru.overwrite.rtp;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.util.XoRoShiRo128PlusRandom;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.channels.settings.Avoidance;
import ru.overwrite.rtp.channels.settings.LocationGenOptions;
import ru.overwrite.rtp.utils.Utils;
import ru.overwrite.rtp.utils.regions.TownyUtils;
import ru.overwrite.rtp.utils.regions.WGUtils;

import java.util.ArrayList;
import java.util.List;

public class LocationGenerator {

    private final RtpManager rtpManager;

    @Getter
    private final XoRoShiRo128PlusRandom random = new XoRoShiRo128PlusRandom();

    @Getter
    private final Object2IntOpenHashMap<String> iterationsPerPlayer = new Object2IntOpenHashMap<>();

    @Getter
    private final WGLocationGenerator wgLocationGenerator;

    public LocationGenerator(Main plugin, RtpManager rtpManager) {
        this.rtpManager = rtpManager;
        this.wgLocationGenerator = plugin.hasWorldGuard() ? new WGLocationGenerator(rtpManager, this) : null;
    }

    public Location generateRandomLocation(Player player, Channel channel, World world) {
        LocationGenOptions locationGenOptions = channel.locationGenOptions();
        if (hasReachedMaxIterations(player.getName(), locationGenOptions)) {
            return null;
        }

        LocationGenOptions.Shape shape = channel.locationGenOptions().shape();
        Location location = switch (shape) {
            case SQUARE -> generateRandomSquareLocation(player, channel, world);
            case ROUND -> generateRandomRoundLocation(player, channel, world);
        };

        if (location == null) {
            iterationsPerPlayer.addTo(player.getName(), 1);
            return generateRandomLocation(player, channel, world);
        }
        rtpManager.printDebug("Location for player '" + player.getName() + "' found in " + iterationsPerPlayer.getInt(player.getName()) + " iterations");
        iterationsPerPlayer.removeInt(player.getName());
        return location;
    }

    public Location generateRandomLocationNearPlayer(Player player, Channel channel, World world) {
        LocationGenOptions locationGenOptions = channel.locationGenOptions();
        if (hasReachedMaxIterations(player.getName(), locationGenOptions)) {
            return null;
        }
        List<Player> nearbyPlayers = getNearbyPlayers(player, locationGenOptions, world);

        if (nearbyPlayers.isEmpty()) {
            rtpManager.printDebug("No players found to generate location near player");
            return null;
        }

        Player targetPlayer = nearbyPlayers.get(random.nextInt(nearbyPlayers.size()));

        Location loc = targetPlayer.getLocation();
        int centerX = loc.getBlockX();
        int centerZ = loc.getBlockZ();

        LocationGenOptions.Shape shape = locationGenOptions.shape();
        Location location = generateRandomLocationNearPoint(shape, player, centerX, centerZ, channel, world);

        if (location == null) {
            iterationsPerPlayer.addTo(player.getName(), 1);
            return generateRandomLocationNearPlayer(player, channel, world);
        }
        rtpManager.printDebug("Location for player '" + player.getName() + "' found in " + iterationsPerPlayer.getInt(player.getName()) + " iterations");
        iterationsPerPlayer.removeInt(player.getName());
        return location;
    }

    private List<Player> getNearbyPlayers(Player player, LocationGenOptions locationGenOptions, World world) {
        int minX = locationGenOptions.minX();
        int maxX = locationGenOptions.maxX();
        int minZ = locationGenOptions.minZ();
        int maxZ = locationGenOptions.maxZ();
        List<Player> nearbyPlayers = new ArrayList<>();
        rtpManager.printDebug("Players in world " + world.getName() + ": " + world.getPlayers().stream().map(Player::getName).toList());
        for (Player worldPlayer : world.getPlayers()) {
            if (worldPlayer.hasPermission("rtp.near.bypass") || isVanished(worldPlayer)) {
                continue;
            }
            Location loc = worldPlayer.getLocation();
            int px = loc.getBlockX();
            int pz = loc.getBlockZ();
            if (px >= minX && px <= maxX && pz >= minZ && pz <= maxZ) {
                nearbyPlayers.add(worldPlayer);
            }
        }
        nearbyPlayers.remove(player);
        return nearbyPlayers;
    }

    private boolean isVanished(Player player) {
        return player.hasMetadata("vanished") && player.getMetadata("vanished").get(0).asBoolean();
    }

    public boolean hasReachedMaxIterations(String playerName, LocationGenOptions locationGenOptions) {
        int iterations = iterationsPerPlayer.getInt(playerName);
        rtpManager.printDebug("Iterations for player '" + playerName + "': " + iterations);
        if (iterations >= locationGenOptions.maxLocationAttempts()) {
            iterationsPerPlayer.removeInt(playerName);
            rtpManager.printDebug("Max iterations reached for player " + playerName);
            return true;
        }
        return false;
    }

    public Location generateRandomSquareLocation(Player player, Channel channel, World world) {
        LocationGenOptions locationGenOptions = channel.locationGenOptions();
        int minX = locationGenOptions.minX();
        int maxX = locationGenOptions.maxX();
        int minZ = locationGenOptions.minZ();
        int maxZ = locationGenOptions.maxZ();
        int x = 0, z = 0;

        switch (locationGenOptions.genFormat()) {
            case RECTANGULAR -> {
                x = random.nextInt((maxX - minX) + 1) + minX;
                z = random.nextInt((maxZ - minZ) + 1) + minZ;
            }
            case RADIAL -> {
                int centerX = locationGenOptions.centerX();
                int centerZ = locationGenOptions.centerZ();

                do {
                    x = random.nextInt((maxX - minX) + 1) + minX;
                    z = random.nextInt((maxZ - minZ) + 1) + minZ;
                    x = (random.nextBoolean() ? centerX + x : centerX - x);
                    z = (random.nextBoolean() ? centerZ + z : centerZ - z);
                } while (isInsideRadiusSquare(x, z, minX, minZ, maxX, maxZ, centerX, centerZ));
            }
        }

        int y = findSafeYPoint(world, x, z);
        if (y < 0) {
            return null;
        }

        Location playerLocation = player.getLocation();
        Location location = new Location(world, x + 0.5, y, z + 0.5, playerLocation.getYaw(), playerLocation.getPitch());
        if (isLocationRestricted(location, channel.avoidance())) {
            return null;
        }
        location.setY(y + 1D);
        return location;
    }

    public Location generateRandomRoundLocation(Player player, Channel channel, World world) {
        LocationGenOptions locationGenOptions = channel.locationGenOptions();
        int minX = locationGenOptions.minX();
        int maxX = locationGenOptions.maxX();
        int minZ = locationGenOptions.minZ();
        int maxZ = locationGenOptions.maxZ();
        int x = 0, z = 0;

        switch (locationGenOptions.genFormat()) {
            case RECTANGULAR -> {
                int centerX = (minX + maxX) / 2;
                int centerZ = (minZ + maxZ) / 2;
                int radiusX = (maxX - minX) / 2;
                int radiusZ = (maxZ - minZ) / 2;

                double theta = random.nextDouble() * 2 * Math.PI;
                double r = Math.sqrt(random.nextDouble());

                x = (int) (centerX + r * radiusX * Math.cos(theta));
                z = (int) (centerZ + r * radiusZ * Math.sin(theta));
            }
            case RADIAL -> {
                int centerX = locationGenOptions.centerX();
                int centerZ = locationGenOptions.centerZ();

                double theta;
                double rX, rZ;
                do {
                    theta = random.nextDouble() * 2 * Math.PI;
                    rX = minX + (maxX - minX) * Math.sqrt(random.nextDouble());
                    rZ = minZ + (maxZ - minZ) * Math.sqrt(random.nextDouble());
                    x = (int) (centerX + rX * Math.cos(theta));
                    z = (int) (centerZ + rZ * Math.sin(theta));
                } while (isInsideRadiusCircle(x, z, minX, minZ, maxX, maxZ, centerX, centerZ));
            }
        }

        int y = findSafeYPoint(world, x, z);
        if (y < 0) {
            return null;
        }

        Location playerLocation = player.getLocation();
        Location location = new Location(world, x + 0.5, y, z + 0.5, playerLocation.getYaw(), playerLocation.getPitch());
        if (isLocationRestricted(location, channel.avoidance())) {
            return null;
        }
        location.setY(y + 1D);
        return location;
    }

    public Location generateRandomLocationNearPoint(LocationGenOptions.Shape shape, Player player, int centerX, int centerZ, Channel channel, World world) {
        return switch (shape) {
            case SQUARE -> generateRandomSquareLocationNearPoint(player, centerX, centerZ, channel, world);
            case ROUND -> generateRandomRoundLocationNearPoint(player, centerX, centerZ, channel, world);
        };
    }

    private Location generateRandomSquareLocationNearPoint(Player player, int centerX, int centerZ, Channel channel, World world) {
        LocationGenOptions locationGenOptions = channel.locationGenOptions();
        int minX = locationGenOptions.minX();
        int maxX = locationGenOptions.maxX();
        int minZ = locationGenOptions.minZ();
        int maxZ = locationGenOptions.maxZ();

        int radiusMin = locationGenOptions.nearRadiusMin();
        int radiusMax = locationGenOptions.nearRadiusMax();

        int x = 0, z = 0;

        switch (locationGenOptions.genFormat()) {
            case RECTANGULAR -> {
                do {
                    x = centerX + (random.nextInt(radiusMax * 2 + 1) - radiusMax);
                    z = centerZ + (random.nextInt(radiusMax * 2 + 1) - radiusMax);
                } while ((x < minX || x > maxX) && (z < minZ || z > maxZ));
            }
            case RADIAL -> {
                int genCenterX = locationGenOptions.centerX();
                int genCenterZ = locationGenOptions.centerZ();

                double theta, r;
                do {
                    theta = random.nextDouble() * 2 * Math.PI;
                    r = radiusMin + (radiusMax - radiusMin) * Math.sqrt(random.nextDouble());
                    x = (int) (centerX + r * Math.cos(theta));
                    z = (int) (centerZ + r * Math.sin(theta));
                } while (isInsideRadiusSquare(x, z, minX, minZ, maxX, maxZ, genCenterX, genCenterZ));
            }
        }

        int y = findSafeYPoint(world, x, z);
        if (y < 0) {
            return null;
        }

        Location playerLocation = player.getLocation();
        Location location = new Location(world, x + 0.5, y, z + 0.5, playerLocation.getYaw(), playerLocation.getPitch());
        if (isLocationRestricted(location, channel.avoidance())) {
            return null;
        }
        location.setY(y + 1D);
        return location;
    }

    private Location generateRandomRoundLocationNearPoint(Player player, int centerX, int centerZ, Channel channel, World world) {
        LocationGenOptions locationGenOptions = channel.locationGenOptions();
        int minX = locationGenOptions.minX();
        int maxX = locationGenOptions.maxX();
        int minZ = locationGenOptions.minZ();
        int maxZ = locationGenOptions.maxZ();

        int radiusMin = locationGenOptions.nearRadiusMin();
        int radiusMax = locationGenOptions.nearRadiusMax();

        int x = 0, z = 0;

        switch (locationGenOptions.genFormat()) {
            case RECTANGULAR -> {
                do {
                    x = centerX + (random.nextInt(radiusMax * 2 + 1) - radiusMax);
                    z = centerZ + (random.nextInt(radiusMax * 2 + 1) - radiusMax);
                } while ((x < minX || x > maxX) && (z < minZ || z > maxZ));
            }
            case RADIAL -> {
                int genCenterX = locationGenOptions.centerX();
                int genCenterZ = locationGenOptions.centerZ();

                double theta, r;
                do {
                    theta = random.nextDouble() * 2 * Math.PI;
                    r = radiusMin + (radiusMax - radiusMin) * Math.sqrt(random.nextDouble());
                    x = (int) (centerX + r * Math.cos(theta));
                    z = (int) (centerZ + r * Math.sin(theta));
                } while (isInsideRadiusCircle(x, z, minX, minZ, maxX, maxZ, genCenterX, genCenterZ));
            }
        }

        int y = findSafeYPoint(world, x, z);
        if (y < 0) {
            return null;
        }

        Location playerLocation = player.getLocation();
        Location location = new Location(world, x + 0.5, y, z + 0.5, playerLocation.getYaw(), playerLocation.getPitch());
        if (isLocationRestricted(location, channel.avoidance())) {
            return null;
        }
        location.setY(y + 1D);
        return location;
    }

    private int findSafeYPoint(World world, int x, int z) {
        return world.getEnvironment() != World.Environment.NETHER ? world.getHighestBlockYAt(x, z) : findSafeNetherYPoint(world, x, z);
    }

    private int findSafeNetherYPoint(World world, int x, int z) {
        for (int y = 32; y < 90; y++) {
            Location location = new Location(world, x, y, z);

            if (location.getBlock().getType().isSolid() && !isInsideBlocks(location, false)) {
                return location.getBlockY();
            }
        }
        return -1;
    }

    public boolean isInsideRadiusSquare(int x, int z, int minX, int minZ, int maxX, int maxZ, int centerX, int centerZ) {
        int realMinX = centerX + minX;
        int realMinZ = centerZ + minZ;
        int realMaxX = centerX + maxX;
        int realMaxZ = centerZ + maxZ;

        return (x >= realMinX && x <= realMaxX && z >= realMinZ && z <= realMaxZ);
    }

    public boolean isInsideRadiusCircle(int x, int z, int minX, int minZ, int maxX, int maxZ, int centerX, int centerZ) {
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

    private boolean isLocationRestricted(Location location, Avoidance avoidance) {
        if (isOutsideWorldBorder(location)) {
            rtpManager.printDebug("Location " + Utils.locationToString(location) + " is outside the world border.");
            return true;
        }
        if (location.getWorld().getEnvironment() != World.Environment.NETHER && isInsideBlocks(location, true)) {
            rtpManager.printDebug("Location " + Utils.locationToString(location) + " is inside blocks.");
            return true;
        }
        if (isDisallowedBlock(location, avoidance)) {
            rtpManager.printDebug("Location " + Utils.locationToString(location) + " contains a disallowed block.");
            return true;
        }
        if (isDisallowedBiome(location, avoidance)) {
            rtpManager.printDebug("Location " + Utils.locationToString(location) + " is in a disallowed biome.");
            return true;
        }
        if (isInsideRegion(location, avoidance)) {
            rtpManager.printDebug("Location " + Utils.locationToString(location) + " is inside a disallowed region.");
            return true;
        }
        if (isInsideTown(location, avoidance)) {
            rtpManager.printDebug("Location " + Utils.locationToString(location) + " is inside a disallowed town.");
            return true;
        }
        return false;
    }

    private boolean isOutsideWorldBorder(Location loc) {
        return !loc.getWorld().getWorldBorder().isInside(loc);
    }

    private boolean isInsideBlocks(Location location, boolean onlyCheckOneBlockUp) {
        Location aboveLocation = location.clone().add(0, 2, 0);
        if (!aboveLocation.getBlock().getType().isAir()) {
            return true;
        }
        return !onlyCheckOneBlockUp && !aboveLocation.subtract(0, 1, 0).getBlock().getType().isAir();
    }

    private boolean isDisallowedBlock(Location loc, Avoidance avoidance) {
        if (avoidance.avoidBlocks().isEmpty()) {
            return false;
        }
        boolean contains = avoidance.avoidBlocks().contains(loc.getBlock().getType());
        return avoidance.avoidBlocksBlacklist() == contains;
    }

    private boolean isDisallowedBiome(Location loc, Avoidance avoidance) {
        if (avoidance.avoidBiomes().isEmpty()) {
            return false;
        }
        boolean contains = avoidance.avoidBiomes().contains(loc.getBlock().getBiome());
        return avoidance.avoidBiomesBlacklist() == contains;
    }

    private boolean isInsideRegion(Location loc, Avoidance avoidance) {
        return avoidance.avoidRegions() && (WGUtils.getApplicableRegions(loc) != null && !WGUtils.getApplicableRegions(loc).getRegions().isEmpty());
    }

    private boolean isInsideTown(Location loc, Avoidance avoidance) {
        return avoidance.avoidTowns() && TownyUtils.getTownByLocation(loc) != null;
    }
}
