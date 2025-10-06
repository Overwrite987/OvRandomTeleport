package ru.overwrite.rtp.animations.impl;

import com.destroystokyo.paper.ParticleBuilder;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import ru.overwrite.rtp.animations.Animation;
import ru.overwrite.rtp.channels.settings.Particles;

import java.util.List;

public class CageAnimation extends Animation {

    private static final double C = 2 * Math.PI;

    private final double RADIUS;

    // must have more settings in the config
    private static final int DOTS = 32; // DOTS | LINES
    private static final int LINES = 16; // <= DOTS
    private static final int COUNT_PER_LINE = 8;

    private static final double OFFSET = C / DOTS;

    private static final List<Double> CIRCLES = List.of(2.0, 0.0);

    private final double first = CIRCLES.getFirst();
    private final double last = CIRCLES.getLast();

    private final double LINE_OFFSET = (first - last) / COUNT_PER_LINE;

    final ParticleBuilder builder = Particle.DUST.builder()
            .particle()
            .builder()
            .count(1)
            .offset(0.0, 0.0, 0.0)
            .extra(particles.preTeleportParticleSpeed())
            .data(new Particle.DustOptions(Color.WHITE, 0.5f)) // must have another settings in the config
            .receivers(receivers)
            .source(player);

    private final Location location = player.getLocation();

    public CageAnimation(Player player, int duration, Particles particles) {
        super(player, duration, particles);

        this.RADIUS = particles.preTeleportRadius();
    }

    @Override
    public void run() {
        tickCounter++;
        if (tickCounter >= duration) {
            this.cancel();
            return;
        }

        double angle = 0;

        for (int circle = 0; circle < CIRCLES.size(); circle++) {
            Double yOffset = CIRCLES.get(circle);
            for (int i = 0; i < DOTS; i++) {
                double x = Math.cos(angle) * RADIUS;
                double z = Math.sin(angle) * RADIUS;

                builder.clone().location(this.location.clone().add(x, yOffset, z)).spawn();

                // only one time per circle
                if (circle == 0) {
                    if (i % (DOTS / LINES) == 0) {
                        for (double y = last; y <= first; y += LINE_OFFSET) {
                            builder.clone().location(this.location.clone().add(x, y, z)).spawn();
                        }
                    }
                }

                angle += OFFSET;

            }
        }

    }
}
