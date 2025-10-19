package ru.overwrite.rtp.channels.settings;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
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

    public static Particles create(ConfigurationSection particles) {
        if (particles == null) {
            return EMPTY_PARTICLES;
        }

        boolean preTeleportEnabled = false;
        AnimationType preTeleportAnimation = null;
        boolean preTeleportSendOnlyToPlayer = false;
        List<ParticleData> preTeleportParticles =  null;
        int preTeleportDots = 0;
        int preTeleportLines = 0;
        int preTeleportDotsPerLine = 0;
        DoubleList preTeleportCirclesOffset = null;
        double preTeleportRadius = 0.0D;
        double preTeleportParticleSpeed = 0.0D;
        double preTeleportSpeed = 0.0D;
        boolean preTeleportInvert = false;
        boolean preTeleportJumping = false;
        boolean preTeleportMoveNear = false;

        boolean afterTeleportEnabled = false;
        boolean afterTeleportSendOnlyToPlayer = false;
        ParticleData afterTeleportParticle = null;
        int afterTeleportCount = 0;
        double afterTeleportRadius = 0.0D;
        double afterTeleportParticleSpeed = 0.0D;

        ConfigurationSection preTeleportSection = particles.getConfigurationSection("pre_teleport");
        if (preTeleportSection != null) {
            preTeleportEnabled = preTeleportSection.getBoolean("enabled", false);
            preTeleportAnimation = AnimationType.valueOf(preTeleportSection.getString("animation", "BASIC").toUpperCase(Locale.ENGLISH));
            preTeleportSendOnlyToPlayer = preTeleportSection.getBoolean("send_only_to_player", false);
            preTeleportDots = preTeleportSection.getInt("dots", 0);
            preTeleportLines = preTeleportSection.getInt("lines", 0);
            preTeleportDotsPerLine = preTeleportSection.getInt("dots_per_line", 0);
            preTeleportRadius = preTeleportSection.getDouble("radius", 0.0D);
            preTeleportParticleSpeed = preTeleportSection.getDouble("particle_speed", 0.0D);
            preTeleportSpeed = preTeleportSection.getDouble("speed", 0.0D);
            preTeleportInvert = preTeleportSection.getBoolean("invert", false);
            preTeleportJumping = preTeleportSection.getBoolean("jumping", false);
            preTeleportMoveNear = preTeleportSection.getBoolean("move_near", false);

            List<String> particleDataList = preTeleportSection.getStringList("id");
            if (!particleDataList.isEmpty()) {
                ImmutableList.Builder<ParticleData> builder = ImmutableList.builder();
                for (String id : particleDataList) {
                    builder.add(Utils.createParticleData(id));
                }
                preTeleportParticles = builder.build();
            }

            List<?> offsetStrings = preTeleportSection.getList("circles_offset");
            if (offsetStrings != null && !offsetStrings.isEmpty()) {
                preTeleportCirclesOffset = new DoubleArrayList();
                for (Object offsetStr : offsetStrings) {
                    if (!(offsetStr instanceof Double d)) {
                        continue;
                    }
                    preTeleportCirclesOffset.add(d.doubleValue());
                }
            }
        }

        ConfigurationSection afterTeleportSection = particles.getConfigurationSection("after_teleport");
        if (afterTeleportSection != null) {
            afterTeleportEnabled = afterTeleportSection.getBoolean("enabled", false);
            afterTeleportSendOnlyToPlayer = afterTeleportSection.getBoolean("send_only_to_player", false);
            afterTeleportCount = afterTeleportSection.getInt("count", afterTeleportCount);
            afterTeleportRadius = afterTeleportSection.getDouble("radius", 0.0D);
            afterTeleportParticleSpeed = afterTeleportSection.getDouble("particle_speed", 0.0D);

            String particleDataString = afterTeleportSection.getString("id");
            if (particleDataString != null) {
                afterTeleportParticle = Utils.createParticleData(particleDataString);
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