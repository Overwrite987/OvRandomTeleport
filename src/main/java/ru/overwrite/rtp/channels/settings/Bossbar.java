package ru.overwrite.rtp.channels.settings;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import ru.overwrite.rtp.channels.Settings;
import ru.overwrite.rtp.configuration.Config;
import ru.overwrite.rtp.utils.Utils;

import java.util.Locale;

public record Bossbar(
        boolean bossbarEnabled,
        String bossbarTitle,
        BarColor bossbarColor,
        BarStyle bossbarStyle,
        boolean smoothProgress) {

    public static Bossbar create(ConfigurationSection bossbar, Settings template, Config pluginConfig, boolean applyTemplate) {
        if (pluginConfig.isNullSection(bossbar) && !applyTemplate) {
            return null;
        }

        Bossbar templateBossbar = template != null ? template.bossbar() : null;
        boolean hasTemplateBossbar = templateBossbar != null;

        boolean enabled = bossbar.getBoolean("enabled", hasTemplateBossbar && templateBossbar.bossbarEnabled());

        String title = bossbar.contains("title")
                ? Utils.COLORIZER.colorize(bossbar.getString("title"))
                : hasTemplateBossbar ? templateBossbar.bossbarTitle() : null;

        BarColor color = bossbar.contains("color")
                ? BarColor.valueOf(bossbar.getString("color", "WHITE").toUpperCase(Locale.ENGLISH))
                : hasTemplateBossbar ? templateBossbar.bossbarColor() : null;

        BarStyle style = bossbar.contains("style")
                ? BarStyle.valueOf(bossbar.getString("style", "SEGMENTED_12").toUpperCase(Locale.ENGLISH))
                : hasTemplateBossbar ? templateBossbar.bossbarStyle() : null;

        boolean smoothProgress = bossbar.getBoolean("smooth_progress", hasTemplateBossbar && templateBossbar.smoothProgress());

        return new Bossbar(enabled, title, color, style, smoothProgress);
    }
}
