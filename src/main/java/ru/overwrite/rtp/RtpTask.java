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
            setupBossBar(p, channel, cooldown);
        }
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
        cancel();
    }

    private void updateBossBar(Channel channel, Cooldown cooldown) {
        if (channel.bossBar().bossbarEnabled()) {
            double progress = preTeleportCooldown / (double) cooldown.teleportCooldown();
            if (progress < 1 && progress > 0) {
                bossBar.setProgress(progress);
            }
            String title = Utils.COLORIZER.colorize(channel.bossBar().bossbarTitle().replace("%time%", Utils.getTime(preTeleportCooldown)));
            bossBar.setTitle(title);
        }
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
        rtpManager.getPerPlayerActiveRtpTask().remove(playerName);
    }
}
