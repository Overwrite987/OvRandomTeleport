package ru.overwrite.rtp.channels.settings;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

public record BossBar(
        boolean bossbarEnabled,
        String bossbarTitle,
        BarColor bossbarColor,
        BarStyle bossbarType) {
}
