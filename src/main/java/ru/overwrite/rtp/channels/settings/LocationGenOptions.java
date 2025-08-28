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

    public static LocationGenOptions create(ConfigurationSection locationGenOptions, Settings template, Config pluginConfig, boolean applyTemplate) {
        if (pluginConfig.isNullSection(locationGenOptions) && !applyTemplate) {
            return null;
        }

        LocationGenOptions templateOptions = template != null ? template.locationGenOptions() : null;
        boolean hasTemplateOptions = templateOptions != null;

        LocationGenOptions.Shape shape = locationGenOptions.contains("shape")
                ? LocationGenOptions.Shape.valueOf(locationGenOptions.getString("shape", "SQUARE").toUpperCase(Locale.ENGLISH))
                : (hasTemplateOptions ? templateOptions.shape() : LocationGenOptions.Shape.SQUARE);

        LocationGenOptions.GenFormat genFormat = locationGenOptions.contains("gen_format")
                ? LocationGenOptions.GenFormat.valueOf(locationGenOptions.getString("gen_format", "RECTANGULAR").toUpperCase(Locale.ENGLISH))
                : (hasTemplateOptions ? templateOptions.genFormat() : LocationGenOptions.GenFormat.RECTANGULAR);

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
}
