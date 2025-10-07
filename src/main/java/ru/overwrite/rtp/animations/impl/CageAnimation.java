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
    private static final int MAX_DOTS = 32; // DOTS | LINES
    private static final int MAX_LINES = 16; // <= DOTS

    private static final int DOTS_PER_LINE = Math.max(MAX_DOTS, MAX_LINES) / Math.min(MAX_DOTS, MAX_LINES); // fix for DOTS | LINES
    private static final int COUNT_PER_LINE = 8; // how much dots inside the line

    private static final double CIRCLE_OFFSET = C / MAX_DOTS;

    private static final List<Double> CIRCLES = List.of(2.0, 1.0, 0.0); // circle y offsets (do not use the same values)

    private final double first = CIRCLES.getFirst();
    private final double last = CIRCLES.getLast();

    private final double LINE_OFFSET = (first - last) / COUNT_PER_LINE;

    private final ParticleBuilder builder = Particle.DUST.builder()
            .particle()
            .builder()
            .count(1)
            .offset(0.0, 0.0, 0.0)
            .extra(particles.preTeleportParticleSpeed())
            .data(new Particle.DustOptions(Color.WHITE, 0.5f)) // must have another settings in the config
            .receivers(receivers)
            .source(player);

    private final int DOT_PER_TICKS = (int) Math.floor((double) Math.max((duration), MAX_DOTS) / Math.min(duration, MAX_DOTS));

    private int currentDots = 0;

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

        final Location location = player.getLocation();

        if (tickCounter % DOT_PER_TICKS == 0 && currentDots < MAX_DOTS) {
            currentDots++;
        }

        for (int circle = 0; circle < CIRCLES.size(); circle++) {
            double angle = 0;

            Double yOffset = CIRCLES.get(circle);
            for (int i = 0; i < currentDots; i++) {
                double x = Math.cos(angle) * RADIUS;
                double z = Math.sin(angle) * RADIUS;

                builder.clone().location(location.clone().add(x, yOffset, z)).spawn();

                // only one time per circle
                if (circle == 0) {
                    if (i % (DOTS_PER_LINE) == 0) {
                        for (double y = last; y <= first; y += LINE_OFFSET) {
                            builder.clone().data(new Particle.DustOptions(Color.RED, 0.5f)).location(location.clone().add(x, y, z)).spawn();
                        }
                    }
                }

                angle += CIRCLE_OFFSET;

            }
        }

    }
}
