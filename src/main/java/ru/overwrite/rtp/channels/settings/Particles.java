package ru.overwrite.rtp.channels.settings;

import org.bukkit.Particle;

public record Particles(
        boolean preTeleportEnabled,
        Particle preTeleportId,
        int preTeleportDots,
        double preTeleportRadius,
        double preTeleportSpeed,
        boolean preTeleportInvert,
        boolean preTeleportJumping,
        boolean afterTeleportEnabled,
        Particle afterTeleportId,
        int afterTeleportCount,
        double afterTeleportRadius,
        double afterTeleportSpeed) {
}
