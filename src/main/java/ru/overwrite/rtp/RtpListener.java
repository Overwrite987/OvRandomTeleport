package ru.overwrite.rtp;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.projectiles.ProjectileSource;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.utils.LocationUtils;
import ru.overwrite.rtp.utils.Utils;
import ru.overwrite.rtp.utils.VersionUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class RtpListener implements Listener {

    private final Main plugin;
    private final RtpManager rtpManager;

    public RtpListener(Main plugin) {
        this.plugin = plugin;
        this.rtpManager = plugin.getRtpManager();
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!e.hasChangedBlock()) {
            return;
        }
        Player p = e.getPlayer();
        if (e.getTo().getBlockY() < VersionUtils.VOID_LEVEL) {
            Map<Channel, List<World>> voidChannels = rtpManager.getSpecifications().voidChannels();
            if (voidChannels.isEmpty()) {
                return;
            }
            for (Channel channel : voidChannels.keySet()) {
                if (!voidChannels.get(channel).contains(p.getWorld())) {
                    continue;
                }
                if (!p.hasPermission("rtp.channel." + channel.getId())) {
                    continue;
                }
                processTeleport(p, channel);
                return;
            }
        }
        String playerName = p.getName();
        if (rtpManager.hasActiveTasks(playerName)) {
            Channel activeChannel = rtpManager.getPerPlayerActiveRtpTask().get(playerName).getActiveChannel();
            if (activeChannel.getRestrictions().restrictMove()) {
                Utils.sendMessage(activeChannel.getMessages().movedOnTeleportMessage(), p);
                cancelTeleportation(playerName);
            }
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            return;
        }
        Player p = e.getPlayer();
        String playerName = p.getName();
        if (rtpManager.hasActiveTasks(playerName)) {
            Channel activeChannel = rtpManager.getPerPlayerActiveRtpTask().get(playerName).getActiveChannel();
            if (activeChannel.getRestrictions().restrictTeleport()) {
                Utils.sendMessage(activeChannel.getMessages().teleportedOnTeleportMessage(), p);
                cancelTeleportation(playerName);
            }
        }
    }

    @EventHandler
    public void onFirstJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (p.hasPlayedBefore()) {
            return;
        }
        Set<Channel> joinChannels = rtpManager.getSpecifications().joinChannels();
        for (Channel channel : joinChannels) {
            if (!p.hasPermission("rtp.channel." + channel.getId())) {
                continue;
            }
            processTeleport(p, channel);
            return;
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Map<Channel, List<World>> respawnChannels = rtpManager.getSpecifications().respawnChannels();
        if (respawnChannels.isEmpty()) {
            return;
        }
        for (Channel channel : respawnChannels.keySet()) {
            if (!respawnChannels.get(channel).contains(p.getWorld())) {
                continue;
            }
            if (!p.hasPermission("rtp.channel." + channel.getId())) {
                continue;
            }
            processTeleport(p, channel);
            return;
        }
    }

    private void processTeleport(Player p, Channel channel) {
        if (!channel.getActiveWorlds().contains(p.getWorld())) {
            if (channel.isTeleportToFirstAllowedWorld()) {
                rtpManager.preTeleport(p, channel, channel.getActiveWorlds().get(0));
                return;
            }
            return;
        }
        rtpManager.preTeleport(p, channel, p.getWorld());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) {
            return;
        }
        String playerName = p.getName();
        if (rtpManager.hasActiveTasks(playerName)) {
            Channel activeChannel = rtpManager.getPerPlayerActiveRtpTask().get(playerName).getActiveChannel();
            if (activeChannel.getRestrictions().restrictDamage() && !activeChannel.getRestrictions().damageCheckOnlyPlayers()) {
                Utils.sendMessage(activeChannel.getMessages().damagedOnTeleportMessage(), p);
                cancelTeleportation(playerName);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        Entity damagerEntity = e.getDamager();
        Entity damagedEntity = e.getEntity();
        if (damagerEntity instanceof Player damager) {
            String damagerName = damager.getName();
            if (rtpManager.hasActiveTasks(damagerName)) {
                Channel activeChannel = rtpManager.getPerPlayerActiveRtpTask().get(damagerName).getActiveChannel();
                if (activeChannel.getRestrictions().restrictDamageOthers()) {
                    if (activeChannel.getRestrictions().damageCheckOnlyPlayers() && !(damagedEntity instanceof Player)) {
                        return;
                    }
                    damager.sendMessage(activeChannel.getMessages().damagedOtherOnTeleportMessage());
                    cancelTeleportation(damagerName);
                }
            }
        }
        if (damagedEntity instanceof Player damaged) {
            String damagedName = damaged.getName();
            if (rtpManager.hasActiveTasks(damagedName)) {
                Channel activeChannel = rtpManager.getPerPlayerActiveRtpTask().get(damagedName).getActiveChannel();
                if (activeChannel.getRestrictions().restrictDamage()) {
                    Player damager = null;
                    if (damagerEntity instanceof Player p) {
                        damager = p;
                    }
                    if (damagerEntity instanceof Projectile projectile) {
                        ProjectileSource projectileSource = projectile.getShooter();
                        if (projectileSource instanceof Player p) {
                            damager = p;
                        }
                    }
                    if (damager != null && activeChannel.getRestrictions().damageCheckOnlyPlayers()) {
                        return;
                    }
                    damaged.sendMessage(activeChannel.getMessages().damagedOnTeleportMessage());
                    cancelTeleportation(damagedName);
                }
            }
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        handlePlayerLeave(p);
    }

    @EventHandler
    public void onKick(PlayerKickEvent e) {
        Player p = e.getPlayer();
        handlePlayerLeave(p);
    }

    private void handlePlayerLeave(Player p) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String playerName = p.getName();
            if (rtpManager.hasActiveTasks(playerName)) {
                cancelTeleportation(playerName);
            }
            LocationUtils.iterationsPerPlayer.removeInt(playerName);
        });
    }

    private void cancelTeleportation(String playerName) {
        rtpManager.getPerPlayerActiveRtpTask().get(playerName).cancel();
        rtpManager.teleportingNow.remove(playerName);
        rtpManager.getPerPlayerActiveRtpTask().remove(playerName);
        LocationUtils.iterationsPerPlayer.removeInt(playerName);
    }
}
