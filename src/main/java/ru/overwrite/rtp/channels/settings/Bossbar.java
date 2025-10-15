package ru.overwrite.rtp.channels.settings;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import ru.overwrite.rtp.utils.Utils;

import java.util.Locale;

public record Bossbar(
        boolean bossbarEnabled,
        String bossbarTitle,
        BarColor bossbarColor,
        BarStyle bossbarStyle,
        boolean smoothProgress
) {

    private static final Bossbar EMPTY_BOSSBAR = new Bossbar(
            false,
            null,
            null,
            null,
            false
    );

    public static Bossbar create(ConfigurationSection bossbar) {
        if (bossbar == null) {
            return EMPTY_BOSSBAR;
        }

        boolean enabled = bossbar.getBoolean("enabled", false);
        String title = Utils.COLORIZER.colorize(bossbar.getString("title"));
        BarColor color = BarColor.valueOf(bossbar.getString("color", "WHITE").toUpperCase(Locale.ENGLISH));
        BarStyle style = BarStyle.valueOf(bossbar.getString("style", "SEGMENTED_12").toUpperCase(Locale.ENGLISH));
        boolean smoothProgress = bossbar.getBoolean("smooth_progress", false);

        return new Bossbar(enabled, title, color, style, smoothProgress);
    }
}