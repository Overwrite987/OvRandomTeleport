package ru.overwrite.rtp.animations.impl;

import com.destroystokyo.paper.ParticleBuilder;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import ru.overwrite.rtp.animations.Animation;
import ru.overwrite.rtp.channels.settings.Particles;

import java.util.Iterator;

public class CageAnimation extends Animation {

    private static final double C = 2 * Math.PI;

    private final DoubleList circles = particles.preTeleport().circlesOffset();
    private final double first = circles.getDouble(0);
    private final double last = circles.getDouble(circles.size() - 1);

    private final int dots = particles.preTeleport().dots();
    private final int lines = particles.preTeleport().lines();
    private final int dotsPerLine = particles.preTeleport().dotsPerLine();
    private final int countPerLine = Math.max(dots, lines) / Math.min(dots, lines);

    private final double circleOffset = C / dots;
    private final double lineOffset = (first - last) / countPerLine;

    private final double speed = Math.max(0, particles.preTeleport().particleSpeed());
    private final double radius = Math.max(0.1, particles.preTeleport().radius());

    private Iterator<Particles.ParticleData> particleDataIterator;

    private final int dotPerTicks = (int) Math.floor((double) Math.max((duration), dots) / Math.min(duration, dots));

    private int currentDots = 0;

    public CageAnimation(Player player, int duration, Particles particles) {
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


        final ParticleBuilder builder = preTeleportParticleData
                .particle()
                .builder()
                .count(1)
                .offset(0.0, 0.0, 0.0)
                .extra(speed)
                .data(preTeleportParticleData.dustOptions())
                .receivers(receivers)
                .source(player);

        final Location location = player.getLocation();

        if (tickCounter % dotPerTicks == 0 && currentDots < particles.preTeleport().dots()) {
            currentDots++;
        }

        for (int circle = 0; circle < circles.size(); circle++) {
            double angle = 0;

            double yOffset = circles.getDouble(circle);
            for (int i = 0; i < currentDots; i++) {
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                builder.location(location.clone().add(x, yOffset, z)).spawn();

                if (circle == 0 && i % (dotsPerLine) == 0) {
                    for (double y = last; y <= first; y += lineOffset) {
                        builder.location(location.clone().add(x, y, z)).spawn();
                    }
                }

                angle += circleOffset;
            }
        }
    }
}