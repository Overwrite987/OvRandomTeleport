package ru.overwrite.rtp.channels.settings;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;

public record LocationGenOptions(
        Shape shape,
        GenFormat genFormat,
        int minX,
        int maxX,
        int minZ,
        int maxZ,
        int nearRadiusMin,
        int nearRadiusMax,
        int centerX,
        int centerZ,
        int maxLocationAttempts
) {

    public enum Shape {
        SQUARE,
        ROUND
    }

    public enum GenFormat {
        RECTANGULAR,
        RADIAL
    }

    private static final LocationGenOptions EMPTY_LGE = new LocationGenOptions(
            null,
            null,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0
    );

    public static LocationGenOptions create(ConfigurationSection locationGenOptions) {
        if (locationGenOptions == null) {
            return EMPTY_LGE;
        }

        LocationGenOptions.Shape shape = parseShape(locationGenOptions);
        LocationGenOptions.GenFormat genFormat = parseGenFormat(locationGenOptions);

        int minX = locationGenOptions.getInt("min_x", 0);
        int maxX = locationGenOptions.getInt("max_x", 0);
        int minZ = locationGenOptions.getInt("min_z", 0);
        int maxZ = locationGenOptions.getInt("max_z", 0);
        int nearRadiusMin = locationGenOptions.getInt("min_near_point_distance", 30);
        int nearRadiusMax = locationGenOptions.getInt("max_near_point_distance", 60);
        int centerX = locationGenOptions.getInt("center_x", 0);
        int centerZ = locationGenOptions.getInt("center_z", 0);
        int maxLocationAttempts = locationGenOptions.getInt("max_location_attempts", 50);

        return new LocationGenOptions(shape, genFormat, minX, maxX, minZ, maxZ, nearRadiusMin, nearRadiusMax, centerX, centerZ, maxLocationAttempts);
    }

    private static LocationGenOptions.Shape parseShape(ConfigurationSection section) {
        if (section.contains("shape")) {
            return LocationGenOptions.Shape.valueOf(section.getString("shape", "SQUARE").toUpperCase(Locale.ROOT));
        }
        return LocationGenOptions.Shape.SQUARE;
    }

    private static LocationGenOptions.GenFormat parseGenFormat(ConfigurationSection section) {
        if (section.contains("gen_format")) {
            return LocationGenOptions.GenFormat.valueOf(section.getString("gen_format", "RECTANGULAR").toUpperCase(Locale.ROOT));
        }
        return LocationGenOptions.GenFormat.RECTANGULAR;
    }
}