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
    private final Player player;
    @Getter
    private final Channel activeChannel;
    private final int finalPreTeleportCooldown;

    private int preTeleportCooldown;
    private BossBar bossBar;
    private BukkitTask countdownTask;
    private BukkitTask animationTask;

    public void startPreTeleportTimer(Location location) {
        this.preTeleportCooldown = this.finalPreTeleportCooldown;
        Settings settings = this.activeChannel.settings();
        if (settings.bossbar().bossbarEnabled()) {
            this.setupBossBar(settings.bossbar());
        }
        if (settings.particles().preTeleportEnabled()) {
            this.animationTask = new BasicAnimation(this.player, preTeleportCooldown * 20, settings.particles()).runTaskTimerAsynchronously(plugin, 0, 1);
        }
        this.countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                preTeleportCooldown--;
                if (preTeleportCooldown <= 0) {
                    cleanupAndTeleport(location);
                    return;
                }
                updateBossBar();
                handleCooldownActions();
            }
        }.runTaskTimerAsynchronously(plugin, 20L, 20L);
        rtpManager.getPerPlayerActiveRtpTask().put(this.player.getName(), this);
    }

    private void setupBossBar(Bossbar bossbar) {
        String title = Utils.COLORIZER.colorize(bossbar.bossbarTitle().replace("%time%", Utils.getTime(finalPreTeleportCooldown)));
        this.bossBar = Bukkit.createBossBar(title, bossbar.bossbarColor(), bossbar.bossbarStyle());
        this.bossBar.addPlayer(this.player);
    }

    private void cleanupAndTeleport(Location location) {
        if (bossBar != null) {
            bossBar.removeAll();
        }
        rtpManager.teleportPlayer(this.player, this.activeChannel, location);
        this.cancel();
    }

    private void updateBossBar() {
        if (bossBar == null) {
            return;
        }
        double progress = (double) preTeleportCooldown / finalPreTeleportCooldown;
        if (progress < 1 && progress > 0) {
            bossBar.setProgress(progress);
        }
        String title = Utils.COLORIZER.colorize(this.activeChannel.settings().bossbar().bossbarTitle().replace("%time%", Utils.getTime(preTeleportCooldown)));
        bossBar.setTitle(title);
    }

    private void handleCooldownActions() {
        Actions actions = this.activeChannel.settings().actions();
        if (actions.onCooldownActions().isEmpty()) {
            return;
        }
        for (int time : actions.onCooldownActions().keySet()) {
            if (time == preTeleportCooldown) {
                rtpManager.executeActions(this.player, this.activeChannel, finalPreTeleportCooldown, actions.onCooldownActions().get(time), this.player.getLocation());
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
        rtpManager.getPerPlayerActiveRtpTask().remove(this.player.getName());
        rtpManager.getTeleportingNow().remove(this.player.getName());
        rtpManager.printDebug("RtpTask cancel called");
        rtpManager.returnCost(player, this.activeChannel);
    }
}
