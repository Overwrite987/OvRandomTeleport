package ru.overwrite.rtp.animations;

import com.destroystokyo.paper.ParticleBuilder;
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

    private double angle;
    private double yOffset = particles.preTeleportInvert() ? 0.0 : 2.0;
    private int tickCounter;

    private final double initialRadius = particles.preTeleportRadius();
    private final double radiusStep = particles.preTeleportMoveNear() ? initialRadius / duration : 0;
    private final double rotationSpeed = ((2 * Math.PI * particles.preTeleportSpeed()) / duration)
            * ((particles.preTeleportInvert() && particles.preTeleportJumping()) ? 2 : 1);
    private final double yStep = particles.preTeleportInvert() ? (2.0 / duration) : (-2.0 / duration);
    private final double verticalRotationSpeed = 2 * Math.PI * 2 / duration;

    private final List<Player> receivers = particles.preTeleportSendOnlyToPlayer() ? List.of(player) : null;

    private Iterator<Particles.ParticleData> preTeleportParticle = particles.preTeleportParticles().iterator();

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
        final double baseX = location.getX();
        final double baseY = location.getY();
        final double baseZ = location.getZ();

        final double yRingOffset = Math.sin((Math.PI * tickCounter) / duration) * 2.0;
        final double currentRadius = particles.preTeleportMoveNear() ? initialRadius - (radiusStep * tickCounter) : initialRadius;

        final double cosRotation = Math.cos(verticalRotationSpeed * tickCounter);
        final double sinRotation = Math.sin(verticalRotationSpeed * tickCounter);

        final ParticleBuilder builder = preTeleportParticleData
                .particle()
                .builder()
                .count(1)
                .offset(0.0, 0.0, 0.0)
                .extra(particles.preTeleportParticleSpeed())
                .data(preTeleportParticleData.dustOptions())
                .receivers(receivers)
                .source(player);

        final double twoPiOverDots = 2.0 * Math.PI / particles.preTeleportDots();

        for (int i = 0; i < particles.preTeleportDots(); i++) {
            double phaseOffset = i * twoPiOverDots;
            double ang = angle + phaseOffset;

            double x = Math.cos(ang) * currentRadius;
            double z = Math.sin(ang) * currentRadius;
            double y;

            if (particles.preTeleportJumping()) {
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

        angle += particles.preTeleportInvert() ? -rotationSpeed : rotationSpeed;

        if (!particles.preTeleportJumping()) {
            yOffset += yStep;
        }
    }
}
