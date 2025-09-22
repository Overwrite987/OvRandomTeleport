package ru.overwrite.rtp.channels.settings;

import org.bukkit.configuration.ConfigurationSection;
import ru.overwrite.rtp.channels.Settings;
import ru.overwrite.rtp.configuration.Config;

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
        int maxLocationAttempts) {

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

    public static LocationGenOptions create(ConfigurationSection locationGenOptions, Settings template, Config pluginConfig, boolean applyTemplate) {

        boolean isNullSection = pluginConfig.isNullSection(locationGenOptions);

        LocationGenOptions templateOptions = template != null ? template.locationGenOptions() : null;
        boolean hasTemplateOptions = templateOptions != null;

        if (isNullSection) {
            if (!applyTemplate) {
                return null;
            }
            if (!hasTemplateOptions) {
                return EMPTY_LGE;
            }
        }

        LocationGenOptions.Shape shape = parseShape(locationGenOptions, hasTemplateOptions ? templateOptions.shape() : null, isNullSection);
        LocationGenOptions.GenFormat genFormat = parseGenFormat(locationGenOptions, hasTemplateOptions ? templateOptions.genFormat() : null, isNullSection);

        int minX = hasTemplateOptions ? templateOptions.minX() : 0;
        int maxX = hasTemplateOptions ? templateOptions.maxX() : 0;
        int minZ = hasTemplateOptions ? templateOptions.minZ() : 0;
        int maxZ = hasTemplateOptions ? templateOptions.maxZ() : 0;
        int nearRadiusMin = hasTemplateOptions ? templateOptions.nearRadiusMin() : 30;
        int nearRadiusMax = hasTemplateOptions ? templateOptions.nearRadiusMax() : 60;
        int centerX = hasTemplateOptions ? templateOptions.centerX() : 0;
        int centerZ = hasTemplateOptions ? templateOptions.centerZ() : 0;
        int maxLocationAttempts = hasTemplateOptions ? templateOptions.maxLocationAttempts() : 50;

        if (!isNullSection) {
            minX = locationGenOptions.getInt("min_x", minX);
            maxX = locationGenOptions.getInt("max_x", maxX);
            minZ = locationGenOptions.getInt("min_z", minZ);
            maxZ = locationGenOptions.getInt("max_z", maxZ);
            nearRadiusMin = locationGenOptions.getInt("min_near_point_distance", nearRadiusMin);
            nearRadiusMax = locationGenOptions.getInt("max_near_point_distance", nearRadiusMax);
            centerX = locationGenOptions.getInt("center_x", centerX);
            centerZ = locationGenOptions.getInt("center_z", centerZ);
            maxLocationAttempts = locationGenOptions.getInt("max_location_attempts", maxLocationAttempts);
        }

        return new LocationGenOptions(shape, genFormat, minX, maxX, minZ, maxZ, nearRadiusMin, nearRadiusMax, centerX, centerZ, maxLocationAttempts);
    }

    private static LocationGenOptions.Shape parseShape(ConfigurationSection section, LocationGenOptions.Shape templateValue, boolean isNullSection) {
        if (!isNullSection && section.contains("shape")) {
            return LocationGenOptions.Shape.valueOf(section.getString("shape", "SQUARE").toUpperCase(Locale.ROOT));
        }
        return templateValue != null ? templateValue : LocationGenOptions.Shape.SQUARE;
    }

    private static LocationGenOptions.GenFormat parseGenFormat(ConfigurationSection section, LocationGenOptions.GenFormat templateValue, boolean isNullSection) {
        if (!isNullSection && section.contains("gen_format")) {
            return LocationGenOptions.GenFormat.valueOf(section.getString("gen_format", "RECTANGULAR").toUpperCase(Locale.ROOT));
        }
        return templateValue != null ? templateValue : LocationGenOptions.GenFormat.RECTANGULAR;
    }
}
