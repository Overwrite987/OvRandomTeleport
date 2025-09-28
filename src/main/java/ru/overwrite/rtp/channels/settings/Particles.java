package ru.overwrite.rtp.channels.settings;

import com.google.common.collect.ImmutableList;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import ru.overwrite.rtp.utils.Utils;

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
        double afterTeleportParticleSpeed
) {

    public record ParticleData(Particle particle, Particle.DustOptions dustOptions) {
    }

    private static final Particles EMPTY_PARTICLES = new Particles(
            false,
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


    public static Particles create(ConfigurationSection particles) {

        if (particles == null) {
            return EMPTY_PARTICLES;
        }

        boolean preTeleportEnabled = false;
        boolean preTeleportSendOnlyToPlayer = false;
        List<Particles.ParticleData> preTeleportParticles = null;
        int preTeleportDots = 0;
        double preTeleportRadius = 0.0D;
        double preTeleportParticleSpeed = 0.0D;
        double preTeleportSpeed = 0.0D;
        boolean preTeleportInvert = false;
        boolean preTeleportJumping = false;
        boolean preTeleportMoveNear = false;

        boolean afterTeleportParticleEnabled = false;
        boolean afterTeleportSendOnlyToPlayer = false;
        Particles.ParticleData afterTeleportParticle = null;
        int afterTeleportCount = 0;
        double afterTeleportRadius = 0.0D;
        double afterTeleportParticleSpeed = 0.0D;

        ConfigurationSection preTeleport = particles.getConfigurationSection("pre_teleport");

        if (preTeleport != null) {
            preTeleportEnabled = preTeleport.getBoolean("enabled", false);
            preTeleportSendOnlyToPlayer = preTeleport.getBoolean("send_only_to_player", false);
            preTeleportDots = preTeleport.getInt("dots", 0);
            preTeleportRadius = preTeleport.getDouble("radius", 0.0D);
            preTeleportParticleSpeed = preTeleport.getDouble("particle_speed", 0.0D);
            preTeleportSpeed = preTeleport.getDouble("speed", 0.0D);
            preTeleportInvert = preTeleport.getBoolean("invert", false);
            preTeleportJumping = preTeleport.getBoolean("jumping", false);
            preTeleportMoveNear = preTeleport.getBoolean("move_near", false);

            if (preTeleport.contains("id")) {
                ImmutableList.Builder<Particles.ParticleData> builder = ImmutableList.builder();
                for (String id : preTeleport.getStringList("id")) {
                    builder.add(Utils.createParticleData(id));
                }
                preTeleportParticles = builder.build();
            }
        }

        ConfigurationSection afterTeleport = particles.getConfigurationSection("after_teleport");

        if (afterTeleport != null) {
            afterTeleportParticleEnabled = afterTeleport.getBoolean("enabled", false);
            afterTeleportSendOnlyToPlayer = afterTeleport.getBoolean("send_only_to_player", false);
            afterTeleportCount = afterTeleport.getInt("count", 0);
            afterTeleportRadius = afterTeleport.getDouble("radius", 0.0D);
            afterTeleportParticleSpeed = afterTeleport.getDouble("particle_speed", 0.0D);

            if (afterTeleport.contains("id")) {
                afterTeleportParticle = Utils.createParticleData(afterTeleport.getString("id", "CLOUD"));
            }
        }

        return new Particles(
                preTeleportEnabled,
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
