package ru.overwrite.rtp.channels.settings;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import ru.overwrite.rtp.channels.Settings;
import ru.overwrite.rtp.configuration.Config;
import ru.overwrite.rtp.utils.Utils;

import java.util.List;
import java.util.Locale;

public record Particles(
        PreTeleportParticles preTeleport,
        AfterTeleportParticles afterTeleport
) {
    public record PreTeleportParticles(
            boolean enabled,
            AnimationType animation,
            boolean sendOnlyToPlayer,
            List<ParticleData> particles,
            int dots,
            int lines,
            int dotsPerLine,
            DoubleList circlesOffset,
            double radius,
            double particleSpeed,
            double speed,
            boolean invert,
            boolean jumping,
            boolean moveNear
    ) {
    }

    public record AfterTeleportParticles(
            boolean enabled,
            boolean sendOnlyToPlayer,
            ParticleData particle,
            int count,
            double radius,
            double particleSpeed
    ) {
    }

    public record ParticleData(Particle particle, Particle.DustOptions dustOptions) {
    }

    public enum AnimationType {
        BASIC,
        CAGE
    }

    private static final Particles EMPTY_PARTICLES = new Particles(
            new PreTeleportParticles(
                    false,
                    AnimationType.BASIC,
                    false,
                    null,
                    0,
                    8,
                    8,
                    null,
                    0D,
                    0D,
                    0D,
                    false,
                    false,
                    false
            ),
            new AfterTeleportParticles(
                    false,
                    false,
                    null,
                    0,
                    0D,
                    0D
            )
    );

    public static Particles create(ConfigurationSection particles, Settings template, Config pluginConfig, boolean applyTemplate) {
        boolean isNullSection = pluginConfig.isNullSection(particles);

        Particles templateParticles = template != null ? template.particles() : null;
        boolean hasTemplateParticles = templateParticles != null;

        if (isNullSection) {
            if (!applyTemplate) {
                return null;
            }
            if (!hasTemplateParticles) {
                return EMPTY_PARTICLES;
            }
        }

        boolean preTeleportEnabled = hasTemplateParticles && templateParticles.preTeleport().enabled();
        AnimationType preTeleportAnimation = hasTemplateParticles ? templateParticles.preTeleport().animation() : AnimationType.BASIC;
        boolean preTeleportSendOnlyToPlayer = hasTemplateParticles && templateParticles.preTeleport().sendOnlyToPlayer();
        List<ParticleData> preTeleportParticles = hasTemplateParticles ? templateParticles.preTeleport().particles() : null;
        int preTeleportDots = hasTemplateParticles ? templateParticles.preTeleport().dots() : 0;
        int preTeleportLines = hasTemplateParticles ? templateParticles.preTeleport().lines() : 8;
        int preTeleportDotsPerLine = hasTemplateParticles ? templateParticles.preTeleport().dotsPerLine() : 8;
        DoubleList preTeleportCirclesOffset = hasTemplateParticles ? templateParticles.preTeleport().circlesOffset() : new DoubleArrayList(new double[]{2.0, 0.0});
        double preTeleportRadius = hasTemplateParticles ? templateParticles.preTeleport().radius() : 0.0;
        double preTeleportParticleSpeed = hasTemplateParticles ? templateParticles.preTeleport().particleSpeed() : 0.0;
        double preTeleportSpeed = hasTemplateParticles ? templateParticles.preTeleport().speed() : 0.0;
        boolean preTeleportInvert = hasTemplateParticles && templateParticles.preTeleport().invert();
        boolean preTeleportJumping = hasTemplateParticles && templateParticles.preTeleport().jumping();
        boolean preTeleportMoveNear = hasTemplateParticles && templateParticles.preTeleport().moveNear();

        boolean afterTeleportEnabled = hasTemplateParticles && templateParticles.afterTeleport().enabled();
        boolean afterTeleportSendOnlyToPlayer = hasTemplateParticles && templateParticles.afterTeleport().sendOnlyToPlayer();
        ParticleData afterTeleportParticle = hasTemplateParticles ? templateParticles.afterTeleport().particle() : null;
        int afterTeleportCount = hasTemplateParticles ? templateParticles.afterTeleport().count() : 0;
        double afterTeleportRadius = hasTemplateParticles ? templateParticles.afterTeleport().radius() : 0.0;
        double afterTeleportParticleSpeed = hasTemplateParticles ? templateParticles.afterTeleport().particleSpeed() : 0.0;

        if (!isNullSection) {
            ConfigurationSection preTeleportSection = particles.getConfigurationSection("pre_teleport");
            boolean isNullPreTeleportSection = pluginConfig.isNullSection(preTeleportSection);

            if (!isNullPreTeleportSection) {
                preTeleportEnabled = preTeleportSection.getBoolean("enabled", preTeleportEnabled);
                preTeleportAnimation = AnimationType.valueOf(preTeleportSection.getString("animation", "BASIC").toUpperCase(Locale.ENGLISH));
                preTeleportSendOnlyToPlayer = preTeleportSection.getBoolean("send_only_to_player", preTeleportSendOnlyToPlayer);
                preTeleportDots = preTeleportSection.getInt("dots", preTeleportDots);
                preTeleportLines = preTeleportSection.getInt("lines", preTeleportLines);
                preTeleportDotsPerLine = preTeleportSection.getInt("dots_per_line", preTeleportDotsPerLine);
                preTeleportRadius = preTeleportSection.getDouble("radius", preTeleportRadius);
                preTeleportParticleSpeed = preTeleportSection.getDouble("particle_speed", preTeleportParticleSpeed);
                preTeleportSpeed = preTeleportSection.getDouble("speed", preTeleportSpeed);
                preTeleportInvert = preTeleportSection.getBoolean("invert", preTeleportInvert);
                preTeleportJumping = preTeleportSection.getBoolean("jumping", preTeleportJumping);
                preTeleportMoveNear = preTeleportSection.getBoolean("move_near", preTeleportMoveNear);

                if (preTeleportSection.contains("id")) {
                    ImmutableList.Builder<ParticleData> builder = ImmutableList.builder();
                    for (String id : pluginConfig.getStringListInAnyCase(preTeleportSection.get("id"))) {
                        builder.add(Utils.createParticleData(id));
                    }
                    preTeleportParticles = builder.build();
                }

                if (preTeleportSection.contains("circles_offset")) {
                    List<String> offsetStrings = pluginConfig.getStringListInAnyCase(preTeleportSection.get("circles_offset"));
                    preTeleportCirclesOffset = new DoubleArrayList();
                    for (String offsetStr : offsetStrings) {
                        if (Utils.isNumeric(offsetStr)) {
                            preTeleportCirclesOffset.add(Double.parseDouble(offsetStr));
                        }
                    }
                }
            }

            ConfigurationSection afterTeleportSection = particles.getConfigurationSection("after_teleport");
            boolean isNullAfterTeleportSection = pluginConfig.isNullSection(afterTeleportSection);

            if (!isNullAfterTeleportSection) {
                afterTeleportEnabled = afterTeleportSection.getBoolean("enabled", afterTeleportEnabled);
                afterTeleportSendOnlyToPlayer = afterTeleportSection.getBoolean("send_only_to_player", afterTeleportSendOnlyToPlayer);
                afterTeleportCount = afterTeleportSection.getInt("count", afterTeleportCount);
                afterTeleportRadius = afterTeleportSection.getDouble("radius", afterTeleportRadius);
                afterTeleportParticleSpeed = afterTeleportSection.getDouble("particle_speed", afterTeleportParticleSpeed);

                if (afterTeleportSection.contains("id")) {
                    afterTeleportParticle = Utils.createParticleData(afterTeleportSection.getString("id"));
                }
            }
        }

        PreTeleportParticles preTeleport = new PreTeleportParticles(
                preTeleportEnabled,
                preTeleportAnimation,
                preTeleportSendOnlyToPlayer,
                preTeleportParticles,
                preTeleportDots,
                preTeleportLines,
                preTeleportDotsPerLine,
                preTeleportCirclesOffset,
                preTeleportRadius,
                preTeleportParticleSpeed,
                preTeleportSpeed,
                preTeleportInvert,
                preTeleportJumping,
                preTeleportMoveNear
        );

        AfterTeleportParticles afterTeleport = new AfterTeleportParticles(
                afterTeleportEnabled,
                afterTeleportSendOnlyToPlayer,
                afterTeleportParticle,
                afterTeleportCount,
                afterTeleportRadius,
                afterTeleportParticleSpeed
        );

        return new Particles(preTeleport, afterTeleport);
    }
}