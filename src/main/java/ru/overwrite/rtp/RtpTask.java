package ru.overwrite.rtp;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.channels.settings.Actions;
import ru.overwrite.rtp.channels.settings.Bossbar;
import ru.overwrite.rtp.channels.settings.Cooldown;
import ru.overwrite.rtp.channels.settings.Particles;
import ru.overwrite.rtp.utils.Utils;

import java.util.Iterator;
import java.util.List;

@RequiredArgsConstructor
public class RtpTask {

    private final Main plugin;
    private final RtpManager rtpManager;
    private final String playerName;

    @Getter
    private final Channel activeChannel;

    private int preTeleportCooldown;
    private BossBar bossBar;
    private BukkitTask runnable;
    private BukkitTask particleTask;

    public void startPreTeleportTimer(Player player, Channel channel, Location location) {
        Cooldown cooldown = channel.cooldown();
        this.preTeleportCooldown = rtpManager.getChannelPreTeleportCooldown(player, channel.cooldown());
        if (channel.bossbar().bossbarEnabled()) {
            this.setupBossBar(player, channel.bossbar(), cooldown);
        }
        startParticleAnimation(player, preTeleportCooldown * 20, channel.particles());
        this.runnable = new BukkitRunnable() {
            @Override
            public void run() {
                preTeleportCooldown--;
                if (preTeleportCooldown <= 0) {
                    cleanupAndTeleport(player, channel, location);
                    return;
                }
                updateBossBar(player, channel, cooldown);
                handleCooldownActions(player, channel);
            }
        }.runTaskTimerAsynchronously(plugin, 20L, 20L);
        rtpManager.getPerPlayerActiveRtpTask().put(this.playerName, this);
    }

    public void startParticleAnimation(Player player, int duration, Particles particles) {
        if (!particles.preTeleportEnabled()) {
            return;
        }
        this.particleTask = new BukkitRunnable() {
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

                if (particles.preTeleportInvert()) {
                    angle -= rotationSpeed;
                } else {
                    angle += rotationSpeed;
                }

                if (!particles.preTeleportJumping()) {
                    yOffset += yStep;
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0, 1);
    }

    private void setupBossBar(Player player, Bossbar bossbar, Cooldown cooldown) {
        String title = Utils.COLORIZER.colorize(bossbar.bossbarTitle().replace("%time%", Utils.getTime(rtpManager.getChannelPreTeleportCooldown(player, cooldown))));
        this.bossBar = Bukkit.createBossBar(title, bossbar.bossbarColor(), bossbar.bossbarType());
        this.bossBar.addPlayer(player);
    }

    private void cleanupAndTeleport(Player player, Channel channel, Location location) {
        if (bossBar != null) {
            bossBar.removeAll();
        }
        rtpManager.getPerPlayerActiveRtpTask().remove(playerName);
        rtpManager.teleportPlayer(player, channel, location);
        this.cancel();
    }

    private void updateBossBar(Player player, Channel channel, Cooldown cooldown) {
        if (bossBar == null) {
            return;
        }
        double progress = preTeleportCooldown / (double) rtpManager.getChannelPreTeleportCooldown(player, cooldown);
        if (progress < 1 && progress > 0) {
            bossBar.setProgress(progress);
        }
        String title = Utils.COLORIZER.colorize(channel.bossbar().bossbarTitle().replace("%time%", Utils.getTime(preTeleportCooldown)));
        bossBar.setTitle(title);
    }

    private void handleCooldownActions(Player player, Channel channel) {
        Actions actions = channel.actions();
        if (actions.onCooldownActions().isEmpty()) {
            return;
        }
        for (int time : actions.onCooldownActions().keySet()) {
            if (time == preTeleportCooldown) {
                rtpManager.executeActions(player, channel, actions.onCooldownActions().get(time), player.getLocation());
            }
        }
    }

    public void cancel() {
        if (bossBar != null) {
            bossBar.removeAll();
        }
        if (particleTask != null) {
            particleTask.cancel();
        }
        runnable.cancel();
        rtpManager.getPerPlayerActiveRtpTask().remove(playerName);
        rtpManager.getTeleportingNow().remove(playerName);
        rtpManager.printDebug("RtpTask cancel called");
    }
}
