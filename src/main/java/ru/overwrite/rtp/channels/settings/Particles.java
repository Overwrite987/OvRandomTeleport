package ru.overwrite.rtp.channels.settings;

import org.bukkit.Particle;

import java.util.List;

public record Particles(
        boolean preTeleportEnabled,
        boolean preTeleportSendOnlyToPlayer,
        List<ParticleData> preTeleportParticles,
        int preTeleportDots,
        double preTeleportRadius,
        double preTeleportParticleSpeed,
        double preTeleportSpeed,
        boolean preTeleportInvert,
        boolean preTeleportJumping,
        boolean preTeleportMoveNear,
        boolean afterTeleportEnabled,
        boolean afterTeleportSendOnlyToPlayer,
        ParticleData afterTeleportParticle,
        int afterTeleportCount,
        double afterTeleportRadius,
        double afterTeleportParticleSpeed) {

    public record ParticleData(Particle particle, Particle.DustOptions dustOptions) {
    }
}
