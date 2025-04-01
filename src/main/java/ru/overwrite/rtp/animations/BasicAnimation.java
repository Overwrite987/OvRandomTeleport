package ru.overwrite.rtp.animations;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import ru.overwrite.rtp.channels.settings.Particles;

import java.util.Iterator;
import java.util.List;

public class BasicAnimation extends Animation {

    public BasicAnimation(Player player, int duration, Particles particles) {
        super(player, duration, particles);
    }

    double angle;
    double yOffset = particles.preTeleportInvert() ? 0.0 : 2.0;
    int tickCounter;

    final double initialRadius = particles.preTeleportRadius();
    final double radiusStep = particles.preTeleportMoveNear() ? initialRadius / duration : 0;
    final double rotationSpeed = ((2 * Math.PI * particles.preTeleportSpeed()) / duration)
            * ((particles.preTeleportInvert() && particles.preTeleportJumping()) ? 2 : 1);
    final double yStep = particles.preTeleportInvert() ? (2.0 / duration) : (-2.0 / duration);
    final double verticalRotationSpeed = 2 * Math.PI * 2 / duration;

    final List<Player> receivers = particles.preTeleportSendOnlyToPlayer() ? List.of(player) : null;

    Iterator<Particles.ParticleData> preTeleportParticle = particles.preTeleportParticles().iterator();

    @Override
    public void run() {
        tickCounter++;
        if (tickCounter >= duration) {
            this.cancel();
            return;
        }
        if (!preTeleportParticle.hasNext()) {
            preTeleportParticle = particles.preTeleportParticles().iterator();
        }
        Particles.ParticleData preTeleportParticleData = preTeleportParticle.next();

        final Location location = player.getLocation();
        final World world = location.getWorld();
        final double yRingOffset = Math.sin((Math.PI * tickCounter) / duration) * 2;

        double currentRadius = particles.preTeleportMoveNear() ? initialRadius - (radiusStep * tickCounter) : initialRadius;

        for (int i = 0; i < particles.preTeleportDots(); i++) {
            double phaseOffset = i * (2 * Math.PI / particles.preTeleportDots());

            double x, y, z;

            if (particles.preTeleportJumping()) {
                y = yRingOffset;

                x = Math.cos(angle + phaseOffset) * currentRadius;
                z = Math.sin(angle + phaseOffset) * currentRadius;

                double cosRotation = Math.cos(verticalRotationSpeed * tickCounter);
                double sinRotation = Math.sin(verticalRotationSpeed * tickCounter);

                double rotatedX = x * cosRotation - z * sinRotation;
                double rotatedZ = x * sinRotation + z * cosRotation;

                x = rotatedX;
                z = rotatedZ;
            } else {
                x = Math.cos(angle + phaseOffset) * currentRadius;
                y = yOffset;
                z = Math.sin(angle + phaseOffset) * currentRadius;
            }

            location.add(x, y, z);

            world.spawnParticle(
                    preTeleportParticleData.particle(),
                    receivers,
                    player,
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    1,
                    0,
                    0,
                    0,
                    particles.preTeleportParticleSpeed(),
                    preTeleportParticleData.dustOptions());

            location.subtract(x, y, z);
        }

        angle += particles.preTeleportInvert() ? -rotationSpeed : rotationSpeed;

        if (!particles.preTeleportJumping()) {
            yOffset += yStep;
        }
    }
}
