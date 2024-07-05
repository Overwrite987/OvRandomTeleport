package ru.overwrite.rtp.utils;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import ru.overwrite.rtp.channels.Avoidance;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.channels.LocationGenOptions;

import java.util.Random;

public class LocationUtils {

    private static final Random random = new Random();

    public static final Object2IntOpenHashMap<String> iterationsPerPlayer = new Object2IntOpenHashMap<>();

    public static Location generateRandomSquareLocation(Player p, Channel channel, World world) {
        LocationGenOptions locationGenOptions = channel.getLocationGenOptions();
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

        Location location = new Location(world, x + 0.5, y, z + 0.5, p.getLocation().getYaw(), p.getLocation().getPitch());
        Avoidance avoidance = channel.getAvoidance();
        if (isDisallowedBlock(location, avoidance) || isDisallowedBiome(location, avoidance) || isInsideRegion(location, avoidance) || isInsideTown(location, avoidance)) {
            return null;
        } else {
            location.setY(y + 1);
            return location;
        }
    }

    public static Location generateRandomRoundLocation(Player p, Channel channel, World world) {
        LocationGenOptions locationGenOptions = channel.getLocationGenOptions();
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

        Location location = new Location(world, x + 0.5, y, z + 0.5, p.getLocation().getYaw(), p.getLocation().getPitch());
        Avoidance avoidance = channel.getAvoidance();
        if (isDisallowedBlock(location, avoidance) || isDisallowedBiome(location, avoidance) || isInsideRegion(location, avoidance) || isInsideTown(location, avoidance)) {
            return null;
        } else {
            location.setY(y + 1);
            return location;
        }
    }


    public static Location generateRandomLocationNearPoint(LocationGenOptions.Shape shape, Player p, int centerX, int centerZ, Channel channel, World world) {
        return switch (shape) {
            case SQUARE -> generateRandomSquareLocationNearPoint(p, centerX, centerZ, channel, world);
            case ROUND -> generateRandomRoundLocationNearPoint(p, centerX, centerZ, channel, world);
        };
    }

    private static Location generateRandomSquareLocationNearPoint(Player p, int centerX, int centerZ, Channel channel, World world) {
        LocationGenOptions locationGenOptions = channel.getLocationGenOptions();
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
                    System.out.println("x: " + x);
                    z = centerZ + (random.nextInt(radiusMax * 2 + 1) - radiusMax);
                    System.out.println("x: " + z);
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
                } while (isInsideRadiusSquare(x, z, minX, minZ, maxX, maxZ, centerX, centerZ));
            }
        }

        int y = findSafeYPoint(world, x, z);
        if (y < 0) {
            return null;
        }

        Location location = new Location(world, x + 0.5, y, z + 0.5, p.getLocation().getYaw(), p.getLocation().getPitch());
        Avoidance avoidance = channel.getAvoidance();
        if (isDisallowedBlock(location, avoidance) || isDisallowedBiome(location, avoidance) || isInsideRegion(location, avoidance) || isInsideTown(location, avoidance)) {
            return null;
        } else {
            location.setY(y + 1);
            return location;
        }
    }

    private static Location generateRandomRoundLocationNearPoint(Player p, int centerX, int centerZ, Channel channel, World world) {
        LocationGenOptions locationGenOptions = channel.getLocationGenOptions();
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

        Location location = new Location(world, x + 0.5, y, z + 0.5, p.getLocation().getYaw(), p.getLocation().getPitch());
        Avoidance avoidance = channel.getAvoidance();
        if (isDisallowedBlock(location, avoidance) || isDisallowedBiome(location, avoidance) || isInsideRegion(location, avoidance) || isInsideTown(location, avoidance)) {
            return null;
        } else {
            location.setY(y + 1);
            return location;
        }
    }

    private static int findSafeYPoint(World world, int x, int z) {
        return world.getEnvironment() != World.Environment.NETHER ? world.getHighestBlockYAt(x, z) : findSafeNetherYPoint(world, x, z);
    }

    private static int findSafeNetherYPoint(World world, int x, int z) {
        for (int y = 16; y < 112; y++) {
            Location location = new Location(world, x, y, z);
            Location above1 = location.clone().add(0, 1, 0);
            Location above2 = location.clone().add(0, 2, 0);

            if (location.getBlock().getType().isSolid() && !location.getBlock().getType().isAir() &&
                    above1.getBlock().getType().isAir() &&
                    above2.getBlock().getType().isAir()) {
                return location.getBlockY();
            }
        }

        return -1;
    }

    public static boolean isInsideRadiusSquare(int x, int z, int minX, int minZ, int maxX, int maxZ, int centerX, int centerZ) {
        int realMinX = centerX + minX;
        int realMinZ = centerZ + minZ;
        int realMaxX = centerX + maxX;
        int realMaxZ = centerZ + maxZ;

        return (x >= realMinX && x <= realMaxX && z >= realMinZ && z <= realMaxZ);
    }

    public static boolean isInsideRadiusCircle(int x, int z, int minX, int minZ, int maxX, int maxZ, int centerX, int centerZ) {
        int deltaX = x - centerX;
        int deltaZ = z - centerZ;
        int distanceSquared = deltaX * deltaX + deltaZ * deltaZ;

        int minRadius = Math.min(minX, minZ);
        int maxRadius = Math.max(maxX, maxZ);

        return (distanceSquared >= minRadius * minRadius && distanceSquared <= maxRadius * maxRadius);
    }

    private static boolean isDisallowedBlock(Location loc, Avoidance avoidance) {
        if (avoidance.avoidBlocksBlacklist()) {
            return !avoidance.avoidBlocks().isEmpty() && avoidance.avoidBlocks().contains(loc.getBlock().getType());
        } else {
            return !avoidance.avoidBlocks().isEmpty() && !avoidance.avoidBlocks().contains(loc.getBlock().getType());
        }
    }

    private static boolean isDisallowedBiome(Location loc, Avoidance avoidance) {
        if (avoidance.avoidBiomesBlacklist()) {
            return !avoidance.avoidBiomes().isEmpty() && avoidance.avoidBiomes().contains(loc.getBlock().getBiome());
        } else {
            return !avoidance.avoidBiomes().isEmpty() && !avoidance.avoidBiomes().contains(loc.getBlock().getBiome());
        }
    }

    private static boolean isInsideRegion(Location loc, Avoidance avoidance) {
        return avoidance.avoidRegions() && (WGUtils.getApplicableRegions(loc) != null && !WGUtils.getApplicableRegions(loc).getRegions().isEmpty());
    }

    private static boolean isInsideTown(Location loc, Avoidance avoidance) {
        return avoidance.avoidTowns() && TownyUtils.getTownByLocation(loc) != null;
    }
}
