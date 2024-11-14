package ru.overwrite.rtp;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.util.XoRoShiRo128PlusRandom;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import ru.overwrite.rtp.channels.settings.Avoidance;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.channels.settings.LocationGenOptions;
import ru.overwrite.rtp.utils.*;
import ru.overwrite.rtp.utils.regions.TownyUtils;
import ru.overwrite.rtp.utils.regions.WGUtils;

import java.util.ArrayList;
import java.util.List;

public class LocationGenerator {

    private final Main plugin;
    protected final XoRoShiRo128PlusRandom random = new XoRoShiRo128PlusRandom();

    @Getter
    private final WGLocationGenerator wgLocationGenerator;

    @Getter
    protected final Object2IntOpenHashMap<String> iterationsPerPlayer;

    public LocationGenerator(Main plugin) {
        this.plugin = plugin;
        this.iterationsPerPlayer = new Object2IntOpenHashMap<>();
        if (plugin.hasWorldGuard()) {
            wgLocationGenerator = new WGLocationGenerator(this);
        } else {
            wgLocationGenerator = null;
        }
    }

    public Location generateRandomLocation(Player p, Channel channel, World world) {
        LocationGenOptions locationGenOptions = channel.locationGenOptions();
        if (hasReachedMaxIterations(p, locationGenOptions)) {
            return null;
        }

        LocationGenOptions.Shape shape = channel.locationGenOptions().shape();
        Location location = switch (shape) {
            case SQUARE -> generateRandomSquareLocation(p, channel, world);
            case ROUND -> generateRandomRoundLocation(p, channel, world);
        };

        if (location == null) {
            iterationsPerPlayer.addTo(p.getName(), 1);
            return generateRandomLocation(p, channel, world);
        } else {
            if (Utils.DEBUG) {
                plugin.getPluginLogger().info("Location for player " + p.getName() + " found in " + iterationsPerPlayer.getInt(p.getName()) + " iterations");
            }
            iterationsPerPlayer.removeInt(p.getName());
            return location;
        }
    }

    public Location generateRandomLocationNearPlayer(Player p, Channel channel, World world) {
        LocationGenOptions locationGenOptions = channel.locationGenOptions();
        if (hasReachedMaxIterations(p, locationGenOptions)) {
            return null;
        }
        List<Player> nearbyPlayers = getNearbyPlayers(p, locationGenOptions, world);

        if (nearbyPlayers.isEmpty()) {
            if (Utils.DEBUG) {
                plugin.getPluginLogger().info("No players to generate location near player");
            }
            return null;
        }

        Player targetPlayer = nearbyPlayers.get(random.nextInt(nearbyPlayers.size()));

        Location loc = targetPlayer.getLocation();
        int centerX = loc.getBlockX();
        int centerZ = loc.getBlockZ();

        LocationGenOptions.Shape shape = locationGenOptions.shape();
        Location location = generateRandomLocationNearPoint(shape, p, centerX, centerZ, channel, world);

        if (location == null) {
            iterationsPerPlayer.addTo(p.getName(), 1);
            return generateRandomLocationNearPlayer(p, channel, world);
        } else {
            if (Utils.DEBUG) {
                plugin.getPluginLogger().info("Location for player " + p.getName() + " found in " + iterationsPerPlayer.getInt(p.getName()) + " iterations");
            }
            iterationsPerPlayer.removeInt(p.getName());
            return location;
        }
    }

    private List<Player> getNearbyPlayers(Player player, LocationGenOptions locationGenOptions, World world) {
        int minX = locationGenOptions.minX();
        int maxX = locationGenOptions.maxX();
        int minZ = locationGenOptions.minZ();
        int maxZ = locationGenOptions.maxZ();
        List<Player> nearbyPlayers = new ArrayList<>();
        for (Player p : world.getPlayers()) {
            if (p.equals(player) || p.hasPermission("rtp.near.bypass")) {
                continue;
            }
            Location loc = p.getLocation();
            int px = loc.getBlockX();
            int pz = loc.getBlockZ();
            if (px >= minX && px <= maxX && pz >= minZ && pz <= maxZ) {
                nearbyPlayers.add(p);
            }
        }
        return nearbyPlayers;
    }

    public boolean hasReachedMaxIterations(Player p, LocationGenOptions locationGenOptions) {
        if (Utils.DEBUG) {
            plugin.getPluginLogger().info("Iterations for player " + p.getName() + ": " + iterationsPerPlayer.getInt(p.getName()));
        }
        if (iterationsPerPlayer.getInt(p.getName()) >= locationGenOptions.maxLocationAttempts()) {
            iterationsPerPlayer.removeInt(p.getName());
            if (Utils.DEBUG) {
                plugin.getPluginLogger().info("Max iterations reached for player " + p.getName());
            }
            return true;
        }
        return false;
    }

    public Location generateRandomSquareLocation(Player p, Channel channel, World world) {
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

        Location location = new Location(world, x + 0.5, y, z + 0.5, p.getLocation().getYaw(), p.getLocation().getPitch());
        if (isLocationRestricted(location, channel.avoidance())) {
            return null;
        } else {
            location.setY(y + 1);
            return location;
        }
    }

    public Location generateRandomRoundLocation(Player p, Channel channel, World world) {
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

        Location location = new Location(world, x + 0.5, y, z + 0.5, p.getLocation().getYaw(), p.getLocation().getPitch());
        if (isLocationRestricted(location, channel.avoidance())) {
            return null;
        } else {
            location.setY(y + 1);
            return location;
        }
    }

    public Location generateRandomLocationNearPoint(LocationGenOptions.Shape shape, Player p, int centerX, int centerZ, Channel channel, World world) {
        return switch (shape) {
            case SQUARE -> generateRandomSquareLocationNearPoint(p, centerX, centerZ, channel, world);
            case ROUND -> generateRandomRoundLocationNearPoint(p, centerX, centerZ, channel, world);
        };
    }

    private Location generateRandomSquareLocationNearPoint(Player p, int centerX, int centerZ, Channel channel, World world) {
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

        Location location = new Location(world, x + 0.5, y, z + 0.5, p.getLocation().getYaw(), p.getLocation().getPitch());
        if (isLocationRestricted(location, channel.avoidance())) {
            return null;
        } else {
            location.setY(y + 1);
            return location;
        }
    }

    private Location generateRandomRoundLocationNearPoint(Player p, int centerX, int centerZ, Channel channel, World world) {
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

        Location location = new Location(world, x + 0.5, y, z + 0.5, p.getLocation().getYaw(), p.getLocation().getPitch());
        if (isLocationRestricted(location, channel.avoidance())) {
            return null;
        } else {
            location.setY(y + 1);
            return location;
        }
    }

    private int findSafeYPoint(World world, int x, int z) {
        return world.getEnvironment() != World.Environment.NETHER ? world.getHighestBlockYAt(x, z) : findSafeNetherYPoint(world, x, z);
    }

    private int findSafeNetherYPoint(World world, int x, int z) {
        for (int y = 16; y < 112; y++) {
            Location location = new Location(world, x, y, z);

            if (location.getBlock().getType().isSolid() && !location.getBlock().getType().isAir() && !isInsideBlocks(location)) {
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
            if (Utils.DEBUG) {
                plugin.getPluginLogger().info("Location " + locationToString(location) + " is outside the world border.");
            }
            return true;
        }
        if (location.getWorld().getEnvironment() != World.Environment.NETHER && isInsideBlocks(location)) {
            if (Utils.DEBUG) {
                plugin.getPluginLogger().info("Location " + locationToString(location) + " is inside blocks.");
            }
            return true;
        }
        if (isDisallowedBlock(location, avoidance)) {
            if (Utils.DEBUG) {
                plugin.getPluginLogger().info("Location " + locationToString(location) + " contains a disallowed block.");
            }
            return true;
        }
        if (isDisallowedBiome(location, avoidance)) {
            if (Utils.DEBUG) {
                plugin.getPluginLogger().info("Location " + locationToString(location) + " is in a disallowed biome.");
            }
            return true;
        }
        if (isInsideRegion(location, avoidance)) {
            if (Utils.DEBUG) {
                plugin.getPluginLogger().info("Location " + locationToString(location) + " is inside a disallowed region.");
            }
            return true;
        }
        if (isInsideTown(location, avoidance)) {
            if (Utils.DEBUG) {
                plugin.getPluginLogger().info("Location " + locationToString(location) + " is inside a disallowed town.");
            }
            return true;
        }
        return false;
    }

    public static String locationToString(Location location) {
        return "(" + location.getWorld().getName() + "/"
                + location.getBlockX() + "/"
                + location.getBlockY() + "/"
                + location.getBlockZ() + ")";
    }

    private boolean isOutsideWorldBorder(Location loc) {
        return !loc.getWorld().getWorldBorder().isInside(loc);
    }

    private boolean isInsideBlocks(Location location) {
        Location above1 = location.clone().add(0, 1, 0);
        Location above2 = location.clone().add(0, 2, 0);

        return !above1.getBlock().getType().isAir() && !above2.getBlock().getType().isAir();
    }

    private boolean isDisallowedBlock(Location loc, Avoidance avoidance) {
        if (avoidance.avoidBlocksBlacklist()) {
            return !avoidance.avoidBlocks().isEmpty() && avoidance.avoidBlocks().contains(loc.getBlock().getType());
        } else {
            return !avoidance.avoidBlocks().isEmpty() && !avoidance.avoidBlocks().contains(loc.getBlock().getType());
        }
    }

    private boolean isDisallowedBiome(Location loc, Avoidance avoidance) {
        if (avoidance.avoidBiomesBlacklist()) {
            return !avoidance.avoidBiomes().isEmpty() && avoidance.avoidBiomes().contains(loc.getBlock().getBiome());
        } else {
            return !avoidance.avoidBiomes().isEmpty() && !avoidance.avoidBiomes().contains(loc.getBlock().getBiome());
        }
    }

    private boolean isInsideRegion(Location loc, Avoidance avoidance) {
        return avoidance.avoidRegions() && (WGUtils.getApplicableRegions(loc) != null && !WGUtils.getApplicableRegions(loc).getRegions().isEmpty());
    }

    private boolean isInsideTown(Location loc, Avoidance avoidance) {
        return avoidance.avoidTowns() && TownyUtils.getTownByLocation(loc) != null;
    }
}
