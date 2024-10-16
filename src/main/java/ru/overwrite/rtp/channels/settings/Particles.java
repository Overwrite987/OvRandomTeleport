package ru.overwrite.rtp.channels.settings;

import org.bukkit.Particle;

public record Particles(
        Particle id,
        int count,
        double radius,
        double speed) {
}
