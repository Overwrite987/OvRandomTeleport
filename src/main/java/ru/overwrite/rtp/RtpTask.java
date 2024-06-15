package ru.overwrite.rtp;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.utils.Utils;

public class RtpTask {

    private final Main plugin;
    private final RtpManager rtpManager;
    private final String playerName;

    @Getter
    private final Channel activeChannel;

    private BossBar bossBar;
    private Integer preTeleportCooldown;
    private BukkitTask runnable;

    public RtpTask(Main plugin, RtpManager rtpManager, String playerName, Channel channel) {
        this.plugin = plugin;
        this.rtpManager = rtpManager;
        this.playerName = playerName;
        this.activeChannel = channel;
    }

    public void startPreTeleportTimer(Player p, Channel channel, Location loc) {
        String playerName = p.getName();
        preTeleportCooldown = channel.getTeleportCooldown();
        if (channel.isBossbarEnabled()) {
            String barTitle = Utils.colorize(channel.getBossbarTitle().replace("%time%", Utils.getTime(channel.getTeleportCooldown())));
            bossBar = Bukkit.createBossBar(barTitle, channel.getBossbarColor(), channel.getBossbarType());
            bossBar.addPlayer(p);
        }
        runnable = (new BukkitRunnable() {
            @Override
            public void run() {
                preTeleportCooldown--;
                if (preTeleportCooldown <= 0) {
                    if (bossBar!= null) {
                        bossBar.removeAll();
                    }
                    rtpManager.getPerPlayerActiveRtpTask().remove(playerName);
                    rtpManager.teleportPlayer(p, channel, loc);
                    cancel();
                    return;
                }
                if (channel.isBossbarEnabled()) {
                    double percents = (channel.getTeleportCooldown() - (channel.getTeleportCooldown() - preTeleportCooldown))
                            / (double) channel.getTeleportCooldown();
                    if (percents < 1 && percents > 0) {
                        bossBar.setProgress(percents);
                    }
                    String barTitle = Utils.colorize(channel.getBossbarTitle().replace("%time%", Utils.getTime(preTeleportCooldown)));
                    bossBar.setTitle(barTitle);
                }
                if (!channel.getOnCooldownActions().isEmpty()) {
                    for (int i : channel.getOnCooldownActions().keySet()) {
                        if (i == preTeleportCooldown) {
                            rtpManager.executeActions(p, channel, channel.getOnCooldownActions().get(i), p.getLocation());
                        }
                    }
                }
            }
        }).runTaskTimerAsynchronously(plugin, 20L, 20L);
        rtpManager.getPerPlayerActiveRtpTask().put(playerName, this);
    }

    public void cancel() {
        if (bossBar!= null) {
            bossBar.removeAll();
        }
        runnable.cancel();
        rtpManager.getPerPlayerActiveRtpTask().remove(playerName);
    }
}
