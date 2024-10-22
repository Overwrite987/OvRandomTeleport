package ru.overwrite.rtp;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.channels.settings.Actions;
import ru.overwrite.rtp.channels.settings.Cooldown;
import ru.overwrite.rtp.channels.settings.Particles;
import ru.overwrite.rtp.utils.Utils;

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

    public RtpTask(Main plugin, RtpManager rtpManager, String playerName, Channel channel) {
        this.plugin = plugin;
        this.rtpManager = rtpManager;
        this.playerName = playerName;
        this.activeChannel = channel;
    }

    public void startPreTeleportTimer(Player p, Channel channel, Location location) {
        Cooldown cooldown = channel.cooldown();
        this.preTeleportCooldown = cooldown.teleportCooldown();
        if (channel.bossBar().bossbarEnabled()) {
            this.setupBossBar(p, channel, cooldown);
        }
        startParticleAnimation(p, preTeleportCooldown * 20, channel.particles());
        this.runnable = new BukkitRunnable() {
            @Override
            public void run() {
                preTeleportCooldown--;
                if (preTeleportCooldown <= 0) {
                    cleanupAndTeleport(p, channel, location);
                    return;
                }
                updateBossBar(channel, cooldown);
                handleCooldownActions(p, channel);
            }
        }.runTaskTimerAsynchronously(plugin, 20L, 20L);
        rtpManager.getPerPlayerActiveRtpTask().put(this.playerName, this);
    }

    public void startParticleAnimation(Player player, int duration, Particles particles) {
        this.particleTask = new BukkitRunnable() {
            double angle = 0;
            double yOffset = 0;
            int tickCounter = 0;

            @Override
            public void run() {
                tickCounter++;
                if (tickCounter >= duration) {
                    this.cancel();
                    return;
                }
                final Location location = player.getLocation();
                double yStep = 2.0 / duration;
                if (!particles.preTeleportUp()) {
                    yStep = -yStep;
                }
                double baseSpeedMultiplier = 4.0;
                double rotationSpeed = (2 * Math.PI * baseSpeedMultiplier) / (duration * particles.preTeleportDots()); // Вращение в 4 раза быстрее и корректировка по количеству точек
                for (int i = 0; i < particles.preTeleportDots(); i++) {
                    double phaseOffset = i * (2 * Math.PI / particles.preTeleportDots());
                    double x = Math.cos(angle + phaseOffset) * particles.preTeleportRadius();
                    double z = Math.sin(angle + phaseOffset) * particles.preTeleportRadius();

                    location.add(x, yOffset, z);
                    player.getWorld().spawnParticle(particles.preTeleportId(), location, 1, 0, 0, 0, 0);
                    location.subtract(x, yOffset, z);
                }
                angle += rotationSpeed;
                yOffset += yStep;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void setupBossBar(Player player, Channel channel, Cooldown cooldown) {
        String title = Utils.COLORIZER.colorize(channel.bossBar().bossbarTitle().replace("%time%", Utils.getTime(cooldown.teleportCooldown())));
        this.bossBar = Bukkit.createBossBar(title, channel.bossBar().bossbarColor(), channel.bossBar().bossbarType());
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

    private void updateBossBar(Channel channel, Cooldown cooldown) {
        double progress = preTeleportCooldown / (double) cooldown.teleportCooldown();
        if (progress < 1 && progress > 0) {
            bossBar.setProgress(progress);
        }
        String title = Utils.COLORIZER.colorize(channel.bossBar().bossbarTitle().replace("%time%", Utils.getTime(preTeleportCooldown)));
        bossBar.setTitle(title);
    }

    private void handleCooldownActions(Player player, Channel channel) {
        Actions actions = channel.actions();
        if (!actions.onCooldownActions().isEmpty()) {
            for (int time : actions.onCooldownActions().keySet()) {
                if (time == preTeleportCooldown) {
                    rtpManager.executeActions(player, channel, actions.onCooldownActions().get(time), player.getLocation());
                }
            }
        }
    }

    public void cancel() {
        if (bossBar != null) {
            bossBar.removeAll();
        }
        runnable.cancel();
        if (particleTask != null) {
            particleTask.cancel();
        }
        rtpManager.getPerPlayerActiveRtpTask().remove(playerName);
    }
}
