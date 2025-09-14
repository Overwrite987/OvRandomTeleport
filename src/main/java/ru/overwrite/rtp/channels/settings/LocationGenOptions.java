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
        if (pluginConfig.isNullSection(locationGenOptions)) {
            if (!applyTemplate) {
                return null;
            }
            return EMPTY_LGE;
        }

        LocationGenOptions templateOptions = template != null ? template.locationGenOptions() : null;
        boolean hasTemplateOptions = templateOptions != null;

        LocationGenOptions.Shape shape = parseShape(locationGenOptions, hasTemplateOptions ? templateOptions.shape() : null, pluginConfig);
        LocationGenOptions.GenFormat genFormat = parseGenFormat(locationGenOptions, hasTemplateOptions ? templateOptions.genFormat() : null, pluginConfig);

        int minX = locationGenOptions.getInt("min_x", hasTemplateOptions ? templateOptions.minX() : 0);
        int maxX = locationGenOptions.getInt("max_x", hasTemplateOptions ? templateOptions.maxX() : 0);
        int minZ = locationGenOptions.getInt("min_z", hasTemplateOptions ? templateOptions.minZ() : 0);
        int maxZ = locationGenOptions.getInt("max_z", hasTemplateOptions ? templateOptions.maxZ() : 0);
        int nearRadiusMin = locationGenOptions.getInt("min_near_point_distance", hasTemplateOptions ? templateOptions.nearRadiusMin() : 30);
        int nearRadiusMax = locationGenOptions.getInt("max_near_point_distance", hasTemplateOptions ? templateOptions.nearRadiusMax() : 60);
        int centerX = locationGenOptions.getInt("center_x", hasTemplateOptions ? templateOptions.centerX() : 0);
        int centerZ = locationGenOptions.getInt("center_z", hasTemplateOptions ? templateOptions.centerZ() : 0);
        int maxLocationAttempts = locationGenOptions.getInt("max_location_attempts", hasTemplateOptions ? templateOptions.maxLocationAttempts() : 50);

        return new LocationGenOptions(shape, genFormat, minX, maxX, minZ, maxZ, nearRadiusMin, nearRadiusMax, centerX, centerZ, maxLocationAttempts);
    }

    private static LocationGenOptions.Shape parseShape(ConfigurationSection section, LocationGenOptions.Shape templateValue, Config pluginConfig) {
        if (!pluginConfig.isNullSection(section) && section.contains("shape")) {
            return LocationGenOptions.Shape.valueOf(section.getString("shape", "SQUARE").toUpperCase(Locale.ROOT));
        }
        return templateValue != null ? templateValue : LocationGenOptions.Shape.SQUARE;
    }

    private static LocationGenOptions.GenFormat parseGenFormat(ConfigurationSection section, LocationGenOptions.GenFormat templateValue, Config pluginConfig) {
        if (!pluginConfig.isNullSection(section) && section.contains("gen_format")) {
            return LocationGenOptions.GenFormat.valueOf(section.getString("gen_format", "RECTANGULAR").toUpperCase(Locale.ROOT));
        }
        return templateValue != null ? templateValue : LocationGenOptions.GenFormat.RECTANGULAR;
    }
}
