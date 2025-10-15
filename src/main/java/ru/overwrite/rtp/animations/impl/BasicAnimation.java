package ru.overwrite.rtp.animations.impl;

import com.destroystokyo.paper.ParticleBuilder;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import ru.overwrite.rtp.animations.Animation;
import ru.overwrite.rtp.channels.settings.Particles;

import java.util.Iterator;

public class BasicAnimation extends Animation {

    private double angle;
    private double yOffset = particles.preTeleport().invert() ? 0.0D : 2.0D;

    private final double speed = Math.max(0, particles.preTeleport().particleSpeed());
    private final double radius = Math.max(0.1, particles.preTeleport().radius());
    private final double radiusStep = particles.preTeleport().moveNear() ? radius / duration : 0D;
    private final double rotationSpeed = ((2 * Math.PI * particles.preTeleport().speed()) / duration)
            * ((particles.preTeleport().invert() && particles.preTeleport().jumping()) ? 2 : 1);
    private final double yStep = particles.preTeleport().invert() ? (2.0 / duration) : (-2.0 / duration);
    private final double verticalRotationSpeed = 2 * Math.PI * 2 / duration;

    private Iterator<Particles.ParticleData> particleDataIterator;

    public BasicAnimation(Player player, int duration, Particles particles) {
        super(player, duration, particles);
    }

    @Override
    public void run() {
        tickCounter++;
        if (tickCounter >= duration) {
            this.cancel();
            return;
        }
        if (particleDataIterator == null || !particleDataIterator.hasNext()) {
            particleDataIterator = particles.preTeleport().particles().iterator();
        }
        Particles.ParticleData preTeleportParticleData = particleDataIterator.next();

        final Location location = player.getLocation();
        final World world = location.getWorld();
        final double baseX = location.getX();
        final double baseY = location.getY();
        final double baseZ = location.getZ();

        final double yRingOffset = Math.sin((Math.PI * tickCounter) / duration) * 2.0;
        final double currentRadius = particles.preTeleport().moveNear() ? radius - (radiusStep * tickCounter) : radius;

        final double cosRotation = Math.cos(verticalRotationSpeed * tickCounter);
        final double sinRotation = Math.sin(verticalRotationSpeed * tickCounter);

        final ParticleBuilder builder = preTeleportParticleData
                .particle()
                .builder()
                .count(1)
                .offset(0.0, 0.0, 0.0)
                .extra(speed)
                .data(preTeleportParticleData.dustOptions())
                .receivers(receivers)
                .source(player);

        final double twoPiOverDots = 2.0 * Math.PI / particles.preTeleport().dots();

        for (int i = 0; i < particles.preTeleport().dots(); i++) {
            double phaseOffset = i * twoPiOverDots;
            double ang = angle + phaseOffset;

            double x = Math.cos(ang) * currentRadius;
            double z = Math.sin(ang) * currentRadius;
            double y;

            if (particles.preTeleport().jumping()) {
                y = yRingOffset;

                double rotatedX = x * cosRotation - z * sinRotation;
                double rotatedZ = x * sinRotation + z * cosRotation;
                x = rotatedX;
                z = rotatedZ;
            } else {
                y = yOffset;
            }

            double px = baseX + x;
            double py = baseY + y;
            double pz = baseZ + z;

            builder.location(world, px, py, pz).spawn();
        }

        angle += particles.preTeleport().invert() ? -rotationSpeed : rotationSpeed;

        if (!particles.preTeleport().jumping()) {
            yOffset += yStep;
        }
    }
}
