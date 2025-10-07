package ru.overwrite.rtp.channels.settings;

import com.google.common.collect.ImmutableList;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import ru.overwrite.rtp.channels.Settings;
import ru.overwrite.rtp.configuration.Config;
import ru.overwrite.rtp.utils.Utils;

import java.util.List;

public record Particles(
        boolean preTeleportEnabled,
        String preTeleportAnimation,
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

    private static final Particles EMPTY_PARTICLES = new Particles(
            false,
            "basic",
            false,
            null,
            0,
            0D,
            0D,
            0D,
            false,
            false,
            false,
            false,
            false,
            null,
            0,
            0D,
            0D
    );


    public static Particles create(ConfigurationSection particles, Settings template, Config pluginConfig, boolean applyTemplate) {

        boolean isNullSection = pluginConfig.isNullSection(particles);

        Particles templateParticles = template != null ? template.particles() : null;
        boolean hasTemplateParticles = templateParticles != null;

        if (isNullSection) {
            if (!applyTemplate) return null;
            if (!hasTemplateParticles) return EMPTY_PARTICLES;
        }

        boolean preTeleportEnabled = hasTemplateParticles && templateParticles.preTeleportEnabled();
        String preTeleportAnimation = hasTemplateParticles ? templateParticles.preTeleportAnimation() : "basic";
        boolean preTeleportSendOnlyToPlayer = hasTemplateParticles && templateParticles.preTeleportSendOnlyToPlayer();
        List<Particles.ParticleData> preTeleportParticles = hasTemplateParticles ? templateParticles.preTeleportParticles() : null;
        int preTeleportDots = hasTemplateParticles ? templateParticles.preTeleportDots() : 0;
        double preTeleportRadius = hasTemplateParticles ? templateParticles.preTeleportRadius() : 0.0;
        double preTeleportParticleSpeed = hasTemplateParticles ? templateParticles.preTeleportParticleSpeed() : 0.0;
        double preTeleportSpeed = hasTemplateParticles ? templateParticles.preTeleportSpeed() : 0.0;
        boolean preTeleportInvert = hasTemplateParticles && templateParticles.preTeleportInvert();
        boolean preTeleportJumping = hasTemplateParticles && templateParticles.preTeleportJumping();
        boolean preTeleportMoveNear = hasTemplateParticles && templateParticles.preTeleportMoveNear();

        boolean afterTeleportParticleEnabled = hasTemplateParticles && templateParticles.afterTeleportEnabled();
        boolean afterTeleportSendOnlyToPlayer = hasTemplateParticles && templateParticles.afterTeleportSendOnlyToPlayer();
        Particles.ParticleData afterTeleportParticle = hasTemplateParticles ? templateParticles.afterTeleportParticle() : null;
        int afterTeleportCount = hasTemplateParticles ? templateParticles.afterTeleportCount() : 0;
        double afterTeleportRadius = hasTemplateParticles ? templateParticles.afterTeleportRadius() : 0.0;
        double afterTeleportParticleSpeed = hasTemplateParticles ? templateParticles.afterTeleportParticleSpeed() : 0.0;

        if (!isNullSection) {
            ConfigurationSection preTeleport = particles.getConfigurationSection("pre_teleport");
            boolean isNullPreTeleportSection = pluginConfig.isNullSection(preTeleport);

            if (!isNullPreTeleportSection) {
                preTeleportEnabled = preTeleport.getBoolean("enabled", preTeleportEnabled);
                preTeleportAnimation = preTeleport.getString("animation", preTeleportAnimation).toLowerCase();
                preTeleportSendOnlyToPlayer = preTeleport.getBoolean("send_only_to_player", preTeleportSendOnlyToPlayer);
                preTeleportDots = preTeleport.getInt("dots", preTeleportDots);
                preTeleportRadius = preTeleport.getDouble("radius", preTeleportRadius);
                preTeleportParticleSpeed = preTeleport.getDouble("particle_speed", preTeleportParticleSpeed);
                preTeleportSpeed = preTeleport.getDouble("speed", preTeleportSpeed);
                preTeleportInvert = preTeleport.getBoolean("invert", preTeleportInvert);
                preTeleportJumping = preTeleport.getBoolean("jumping", preTeleportJumping);
                preTeleportMoveNear = preTeleport.getBoolean("move_near", preTeleportMoveNear);

                if (preTeleport.contains("id")) {
                    ImmutableList.Builder<Particles.ParticleData> builder = ImmutableList.builder();
                    for (String id : pluginConfig.getStringListInAnyCase(preTeleport.get("id"))) {
                        builder.add(Utils.createParticleData(id));
                    }
                    preTeleportParticles = builder.build();
                }
            }

            ConfigurationSection afterTeleport = particles.getConfigurationSection("after_teleport");
            boolean isNullAfterTeleportSection = pluginConfig.isNullSection(afterTeleport);

            if (!isNullAfterTeleportSection) {
                afterTeleportParticleEnabled = afterTeleport.getBoolean("enabled", afterTeleportParticleEnabled);
                afterTeleportSendOnlyToPlayer = afterTeleport.getBoolean("send_only_to_player", afterTeleportSendOnlyToPlayer);
                afterTeleportCount = afterTeleport.getInt("count", afterTeleportCount);
                afterTeleportRadius = afterTeleport.getDouble("radius", afterTeleportRadius);
                afterTeleportParticleSpeed = afterTeleport.getDouble("particle_speed", afterTeleportParticleSpeed);

                if (afterTeleport.contains("id")) {
                    afterTeleportParticle = Utils.createParticleData(afterTeleport.getString("id"));
                }
            }
        }

        return new Particles(
                preTeleportEnabled,
                preTeleportAnimation,
                preTeleportSendOnlyToPlayer,
                preTeleportParticles,
                preTeleportDots,
                preTeleportRadius,
                preTeleportParticleSpeed,
                preTeleportSpeed,
                preTeleportInvert,
                preTeleportJumping,
                preTeleportMoveNear,
                afterTeleportParticleEnabled,
                afterTeleportSendOnlyToPlayer,
                afterTeleportParticle,
                afterTeleportCount,
                afterTeleportRadius,
                afterTeleportParticleSpeed
        );
    }
}
