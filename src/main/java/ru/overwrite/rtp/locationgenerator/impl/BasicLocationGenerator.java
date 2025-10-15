package ru.overwrite.rtp.locationgenerator.impl;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import ru.overwrite.rtp.RtpManager;
import ru.overwrite.rtp.channels.Settings;
import ru.overwrite.rtp.channels.settings.LocationGenOptions;
import ru.overwrite.rtp.locationgenerator.AbstractLocationGenerator;

import java.util.ArrayList;
import java.util.List;

public class BasicLocationGenerator extends AbstractLocationGenerator {

    public BasicLocationGenerator(RtpManager rtpManager) {
        super(rtpManager);
    }

    @Override
    public Location generateRandomLocation(Player player, Settings settings, World world) {
        LocationGenOptions locationGenOptions = settings.locationGenOptions();
        if (hasReachedMaxIterations(player.getName(), locationGenOptions)) {
            return null;
        }

        LocationGenOptions.Shape shape = locationGenOptions.shape();
        Location location = switch (shape) {
            case SQUARE -> generateRandomSquareLocation(player, settings, world);
            case ROUND -> generateRandomRoundLocation(player, settings, world);
        };

        if (location == null) {
            iterationsPerPlayer.addTo(player.getName(), 1);
            return generateRandomLocation(player, settings, world);
        }
        rtpManager.printDebug(() -> "Location for player '" + player.getName() + "' found in " + iterationsPerPlayer.getInt(player.getName()) + " iterations");
        iterationsPerPlayer.removeInt(player.getName());
        return location;
    }

    @Override
    public Location generateRandomLocationNearPlayer(Player player, Settings settings, World world) {
        LocationGenOptions locationGenOptions = settings.locationGenOptions();
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
        Location location = generateRandomLocationNearPoint(shape, player, centerX, centerZ, settings, world);

        if (location == null) {
            iterationsPerPlayer.addTo(player.getName(), 1);
            return generateRandomLocationNearPlayer(player, settings, world);
        }
        rtpManager.printDebug(() -> "Location for player '" + player.getName() + "' found in " + iterationsPerPlayer.getInt(player.getName()) + " iterations");
        iterationsPerPlayer.removeInt(player.getName());
        return location;
    }

    private List<Player> getNearbyPlayers(Player player, LocationGenOptions locationGenOptions, World world) {
        int minX = locationGenOptions.minX();
        int maxX = locationGenOptions.maxX();
        int minZ = locationGenOptions.minZ();
        int maxZ = locationGenOptions.maxZ();
        List<Player> nearbyPlayers = new ArrayList<>();
        List<Player> worldPlayers = world.getPlayers();
        rtpManager.printDebug(() -> "Players in world " + world.getName() + ": " + worldPlayers.stream().map(Player::getName).toList());
        worldPlayers.remove(player);
        for (int i = 0; i < worldPlayers.size(); i++) {
            Player worldPlayer = worldPlayers.get(i);
            String debugMsg = "Player " + worldPlayer.getName() + " excluded because: ";
            if (worldPlayer.hasPermission("rtp.near.bypass")) {
                rtpManager.printDebug(debugMsg + "has bypass permission");
                continue;
            }
            if (isVanished(worldPlayer)) {
                rtpManager.printDebug(debugMsg + "is vanished");
                continue;
            }
            Location loc = worldPlayer.getLocation();
            int px = loc.getBlockX();
            int pz = loc.getBlockZ();

            if (px >= minX && px <= maxX && pz >= minZ && pz <= maxZ) {
                nearbyPlayers.add(worldPlayer);
            }
        }
        return nearbyPlayers;
    }

    private boolean isVanished(Player player) {
        return player.hasMetadata("vanished") && player.getMetadata("vanished").get(0).asBoolean();
    }

    public Location generateRandomSquareLocation(Player player, Settings settings, World world) {
        LocationGenOptions locationGenOptions = settings.locationGenOptions();
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
        Location location = new Location(world, x + 0.5D, y, z + 0.5D, playerLocation.getYaw(), playerLocation.getPitch());
        if (isLocationRestricted(location, settings.avoidance())) {
            return null;
        }
        location.setY(y + 1D);
        return location;
    }

    public Location generateRandomRoundLocation(Player player, Settings settings, World world) {
        LocationGenOptions locationGenOptions = settings.locationGenOptions();
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
        Location location = new Location(world, x + 0.5D, y, z + 0.5D, playerLocation.getYaw(), playerLocation.getPitch());
        if (isLocationRestricted(location, settings.avoidance())) {
            return null;
        }
        location.setY(y + 1D);
        return location;
    }

    public Location generateRandomLocationNearPoint(LocationGenOptions.Shape shape, Player player, int centerX, int centerZ, Settings settings, World world) {
        return switch (shape) {
            case SQUARE -> generateRandomSquareLocationNearPoint(player, centerX, centerZ, settings, world);
            case ROUND -> generateRandomRoundLocationNearPoint(player, centerX, centerZ, settings, world);
        };
    }

    private Location generateRandomSquareLocationNearPoint(Player player, int centerX, int centerZ, Settings settings, World world) {
        LocationGenOptions locationGenOptions = settings.locationGenOptions();
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
        Location location = new Location(world, x + 0.5D, y, z + 0.5D, playerLocation.getYaw(), playerLocation.getPitch());
        if (isLocationRestricted(location, settings.avoidance())) {
            return null;
        }
        location.setY(y + 1D);
        return location;
    }

    private Location generateRandomRoundLocationNearPoint(Player player, int centerX, int centerZ, Settings settings, World world) {
        LocationGenOptions locationGenOptions = settings.locationGenOptions();
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
        Location location = new Location(world, x + 0.5D, y, z + 0.5D, playerLocation.getYaw(), playerLocation.getPitch());
        if (isLocationRestricted(location, settings.avoidance())) {
            return null;
        }
        location.setY(y + 1D);
        return location;
    }

    @Override
    public Location generateRandomLocationNearRandomRegion(Player player, Settings settings, World world) {
        return generateRandomLocation(player, settings, world);
    }
}