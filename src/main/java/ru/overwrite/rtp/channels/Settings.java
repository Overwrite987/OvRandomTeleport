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

    public static Settings create(OvRandomTeleport plugin, ConfigurationSection config) {
        return new Settings(
                Costs.create(plugin, config.getConfigurationSection("costs")),
                LocationGenOptions.create(config.getConfigurationSection("location_generation_options")),
                Cooldown.create(plugin, config.getConfigurationSection("cooldown")),
                Bossbar.create(config.getConfigurationSection("bossbar")),
                Particles.create(config.getConfigurationSection("particles")),
                Restrictions.create(config.getConfigurationSection("restrictions")),
                Avoidance.create(config.getConfigurationSection("avoid")),
                Actions.create(plugin, config.getConfigurationSection("actions"))
        );
    }
}
