package ru.overwrite.rtp.channels.settings;

import org.bukkit.Particle;

public record Particles(
        boolean preTeleportEnabled,
        boolean preTeleportSendOnlyToPlayer,
        Particle preTeleportId,
        int preTeleportDots,
        double preTeleportRadius,
        double preTeleportSpeed,
        boolean preTeleportInvert,
        boolean preTeleportJumping,
        boolean preTeleportMoveNear,
        boolean afterTeleportEnabled,
        boolean afterTeleportSendOnlyToPlayer,
        Particle afterTeleportId,
        int afterTeleportCount,
        double afterTeleportRadius,
        double afterTeleportSpeed) {
}
