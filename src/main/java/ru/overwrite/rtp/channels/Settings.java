package ru.overwrite.rtp.channels;

import org.bukkit.configuration.ConfigurationSection;
import ru.overwrite.rtp.OvRandomTeleport;
import ru.overwrite.rtp.channels.settings.*;
import ru.overwrite.rtp.configuration.Config;

public record Settings(
        Costs costs,
        LocationGenOptions locationGenOptions,
        Cooldown cooldown,
        Bossbar bossbar,
        Particles particles,
        Restrictions restrictions,
        Avoidance avoidance,
        Actions actions) {

    public static Settings create(OvRandomTeleport plugin, ConfigurationSection config, Config pluginConfig, Settings template, boolean applyTemplate) {
        return new Settings(
                Costs.create(plugin, config.getConfigurationSection("costs"), template, pluginConfig, applyTemplate),
                LocationGenOptions.create(config.getConfigurationSection("location_generation_options"), template, pluginConfig, applyTemplate),
                Cooldown.create(plugin, config.getConfigurationSection("cooldown"), template, pluginConfig, applyTemplate),
                Bossbar.create(config.getConfigurationSection("bossbar"), template, pluginConfig, applyTemplate),
                Particles.create(config.getConfigurationSection("particles"), template, pluginConfig, applyTemplate),
                Restrictions.create(config.getConfigurationSection("restrictions"), template, pluginConfig, applyTemplate),
                Avoidance.create(config.getConfigurationSection("avoid"), template, pluginConfig, applyTemplate),
                Actions.create(plugin, config.getConfigurationSection("actions"), template, pluginConfig, applyTemplate)
        );
    }
}
