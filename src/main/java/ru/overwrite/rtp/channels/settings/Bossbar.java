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

    private static final Bossbar EMPTY_BOSSBAR = new Bossbar(
            false,
            null,
            null,
            null,
            false
    );

    public static Bossbar create(ConfigurationSection bossbar, Settings template, Config pluginConfig, boolean applyTemplate) {

        boolean isNullSection = pluginConfig.isNullSection(bossbar);

        Bossbar templateBossbar = applyTemplate && template != null ? template.bossbar() : null;
        boolean hasTemplateBossbar = templateBossbar != null;

        if (isNullSection) {
            if (!applyTemplate) {
                return null;
            }
            if (!hasTemplateBossbar) {
                return EMPTY_BOSSBAR;
            }
        }

        boolean enabled = hasTemplateBossbar && templateBossbar.bossbarEnabled();
        String title = hasTemplateBossbar ? templateBossbar.bossbarTitle() : null;
        BarColor color = hasTemplateBossbar ? templateBossbar.bossbarColor() : BarColor.WHITE;
        BarStyle style = hasTemplateBossbar ? templateBossbar.bossbarStyle() : BarStyle.SEGMENTED_12;
        boolean smoothProgress = hasTemplateBossbar && templateBossbar.smoothProgress();

        if (!isNullSection) {
            enabled = bossbar.getBoolean("enabled", enabled);
            title = Utils.COLORIZER.colorize(bossbar.getString("title"));
            color = BarColor.valueOf(bossbar.getString("color", "WHITE").toUpperCase(Locale.ENGLISH));
            style = BarStyle.valueOf(bossbar.getString("style", "SEGMENTED_12").toUpperCase(Locale.ENGLISH));
            smoothProgress = bossbar.getBoolean("smooth_progress", smoothProgress);
        }

        return new Bossbar(enabled, title, color, style, smoothProgress);
    }
}
