package ru.overwrite.rtp;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.overwrite.rtp.animations.BasicAnimation;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.channels.Settings;
import ru.overwrite.rtp.channels.settings.Actions;
import ru.overwrite.rtp.channels.settings.Bossbar;
import ru.overwrite.rtp.utils.Utils;

@RequiredArgsConstructor
public class RtpTask {

    private final OvRandomTeleport plugin;
    private final RtpManager rtpManager;
    private final String playerName;
    private final int finalPreTeleportCooldown;

    @Getter
    private final Channel activeChannel;

    private int preTeleportCooldown;
    private BossBar bossBar;
    private BukkitTask countdownTask;
    private BukkitTask animationTask;

    public void startPreTeleportTimer(Player player, Channel channel, Location location) {
        this.preTeleportCooldown = this.finalPreTeleportCooldown;
        Settings settings = channel.settings();
        if (settings.bossbar().bossbarEnabled()) {
            this.setupBossBar(player, settings.bossbar());
        }
        if (settings.particles().preTeleportEnabled()) {
            this.animationTask = new BasicAnimation(player, preTeleportCooldown * 20, settings.particles()).runTaskTimerAsynchronously(plugin, 0, 1);
        }
        this.countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                preTeleportCooldown--;
                if (preTeleportCooldown <= 0) {
                    cleanupAndTeleport(player, channel, location);
                    return;
                }
                updateBossBar(channel);
                handleCooldownActions(player, channel);
            }
        }.runTaskTimerAsynchronously(plugin, 20L, 20L);
        rtpManager.getPerPlayerActiveRtpTask().put(this.playerName, this);
    }

    private void setupBossBar(Player player, Bossbar bossbar) {
        String title = Utils.COLORIZER.colorize(bossbar.bossbarTitle().replace("%time%", Utils.getTime(finalPreTeleportCooldown)));
        this.bossBar = Bukkit.createBossBar(title, bossbar.bossbarColor(), bossbar.bossbarStyle());
        this.bossBar.addPlayer(player);
    }

    private void cleanupAndTeleport(Player player, Channel channel, Location location) {
        if (bossBar != null) {
            bossBar.removeAll();
        }
        rtpManager.teleportPlayer(player, channel, location);
        this.cancel();
    }

    private void updateBossBar(Channel channel) {
        if (bossBar == null) {
            return;
        }
        double progress = (double) preTeleportCooldown / finalPreTeleportCooldown;
        if (progress < 1 && progress > 0) {
            bossBar.setProgress(progress);
        }
        String title = Utils.COLORIZER.colorize(channel.settings().bossbar().bossbarTitle().replace("%time%", Utils.getTime(preTeleportCooldown)));
        bossBar.setTitle(title);
    }

    private void handleCooldownActions(Player player, Channel channel) {
        Actions actions = channel.settings().actions();
        if (actions.onCooldownActions().isEmpty()) {
            return;
        }
        for (int time : actions.onCooldownActions().keySet()) {
            if (time == preTeleportCooldown) {
                rtpManager.executeActions(player, channel, finalPreTeleportCooldown, actions.onCooldownActions().get(time), player.getLocation());
            }
        }
    }

    public void cancel() {
        if (bossBar != null) {
            bossBar.removeAll();
        }
        if (animationTask != null) {
            animationTask.cancel();
        }
        countdownTask.cancel();
        rtpManager.getPerPlayerActiveRtpTask().remove(playerName);
        rtpManager.getTeleportingNow().remove(playerName);
        rtpManager.printDebug("RtpTask cancel called");
    }
}
