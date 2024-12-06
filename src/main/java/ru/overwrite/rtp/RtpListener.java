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

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (!e.hasChangedBlock()) {
            return;
        }
        Player p = e.getPlayer();
        if (e.getTo().getBlockY() < VersionUtils.VOID_LEVEL) {
            Map<String, List<World>> voidChannels = rtpManager.getSpecifications().voidChannels();
            if (voidChannels.isEmpty()) {
                return;
            }
            for (Map.Entry<String, List<World>> entry : voidChannels.entrySet()) {
                List<World> worlds = entry.getValue();
                if (!worlds.contains(p.getWorld())) {
                    continue;
                }
                String channelId = entry.getKey();
                if (!p.hasPermission("rtp.channel." + channelId)) {
                    continue;
                }
                processTeleport(p, rtpManager.getChannelById(channelId));
                return;
            }
        }
        String playerName = p.getName();
        if (rtpManager.hasActiveTasks(playerName)) {
            Channel activeChannel = getActiveChannel(playerName);
            if (activeChannel.restrictions().restrictMove()) {
                Utils.sendMessage(activeChannel.messages().movedOnTeleport(), p);
                cancelTeleportation(playerName);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            return;
        }
        Player p = e.getPlayer();
        String playerName = p.getName();
        if (rtpManager.hasActiveTasks(playerName)) {
            Channel activeChannel = getActiveChannel(playerName);
            if (activeChannel.restrictions().restrictTeleport()) {
                Utils.sendMessage(activeChannel.messages().teleportedOnTeleport(), p);
                cancelTeleportation(playerName);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (rtpManager.getProxyCalls() != null && !rtpManager.getProxyCalls().isEmpty()) {
            String data = rtpManager.getProxyCalls().get(p.getName());
            if (data == null) {
                return;
            }
            int separatorIndex = data.indexOf(';');
            Channel channel = rtpManager.getChannelById(data.substring(0, separatorIndex));
            World world = Bukkit.getWorld(data.substring(separatorIndex + 1));
            rtpManager.preTeleport(p, channel, world);
            rtpManager.getProxyCalls().remove(p.getName());
            return;
        }
        if (p.hasPlayedBefore()) {
            return;
        }
        Set<String> joinChannels = rtpManager.getSpecifications().joinChannels();
        if (joinChannels.isEmpty()) {
            return;
        }
        for (String channelId : joinChannels) {
            if (!p.hasPermission("rtp.channel." + channelId)) {
                continue;
            }
            processTeleport(p, rtpManager.getChannelById(channelId));
            return;
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Map<String, List<World>> respawnChannels = rtpManager.getSpecifications().respawnChannels();
        if (respawnChannels.isEmpty()) {
            return;
        }
        Player p = e.getPlayer();
        for (Map.Entry<String, List<World>> entry : respawnChannels.entrySet()) {
            List<World> worlds = entry.getValue();
            if (!worlds.contains(p.getWorld())) {
                continue;
            }
            String channelId = entry.getKey();
            if (!p.hasPermission("rtp.channel." + channelId)) {
                continue;
            }
            processTeleport(p, rtpManager.getChannelById(channelId));
            return;
        }
    }

    private void processTeleport(Player p, Channel channel) {
        if (!channel.activeWorlds().contains(p.getWorld())) {
            if (channel.teleportToFirstAllowedWorld()) {
                rtpManager.preTeleport(p, channel, channel.activeWorlds().get(0));
            }
            return;
        }
        rtpManager.preTeleport(p, channel, p.getWorld());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) {
            return;
        }
        String playerName = p.getName();
        if (rtpManager.hasActiveTasks(playerName)) {
            Channel activeChannel = rtpManager.getPerPlayerActiveRtpTask().get(playerName).getActiveChannel();
            if (activeChannel.restrictions().restrictDamage() && !activeChannel.restrictions().damageCheckOnlyPlayers()) {
                Utils.sendMessage(activeChannel.messages().damagedOnTeleport(), p);
                cancelTeleportation(playerName);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        Entity damagerEntity = e.getDamager();
        Entity damagedEntity = e.getEntity();

        if (damagerEntity instanceof Player damager) {
            handleDamagerPlayer(damager, damagedEntity);
        }
        if (damagedEntity instanceof Player damaged) {
            handleDamagedPlayer(damagerEntity, damaged);
        }
    }

    private void handleDamagerPlayer(Player damager, Entity damagedEntity) {
        String damagerName = damager.getName();
        if (rtpManager.hasActiveTasks(damagerName)) {
            Channel activeChannel = getActiveChannel(damagerName);
            if (activeChannel.restrictions().restrictDamageOthers()) {
                if (activeChannel.restrictions().damageCheckOnlyPlayers() && !(damagedEntity instanceof Player)) {
                    return;
                }
                damager.sendMessage(activeChannel.messages().damagedOtherOnTeleport());
                cancelTeleportation(damagerName);
            }
        }
    }

    private void handleDamagedPlayer(Entity damagerEntity, Player damaged) {
        String damagedName = damaged.getName();
        if (rtpManager.hasActiveTasks(damagedName)) {
            Channel activeChannel = getActiveChannel(damagedName);
            if (activeChannel.restrictions().restrictDamage()) {
                Player damager = getDamager(damagerEntity);
                if (damager == null && activeChannel.restrictions().damageCheckOnlyPlayers()) {
                    return;
                }
                damaged.sendMessage(activeChannel.messages().damagedOnTeleport());
                cancelTeleportation(damagedName);
            }
        }
    }

    private Channel getActiveChannel(String playerName) {
        return rtpManager.getPerPlayerActiveRtpTask().get(playerName).getActiveChannel();
    }

    private Player getDamager(Entity damagerEntity) {
        if (damagerEntity instanceof Player p) {
            return p;
        }
        if (damagerEntity instanceof Projectile projectile) {
            ProjectileSource projectileSource = projectile.getShooter();
            if (projectileSource instanceof Player p) {
                return p;
            }
        }
        return null;
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        handlePlayerLeave(p);
    }

    @EventHandler(ignoreCancelled = true)
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
        });
    }

    private void cancelTeleportation(String playerName) {
        if (Utils.DEBUG) {
            plugin.getPluginLogger().info("Teleportation for player " + playerName + " was cancelled because of restrictions");
        }
        rtpManager.getPerPlayerActiveRtpTask().get(playerName).cancel();
        rtpManager.getLocationGenerator().getIterationsPerPlayer().removeInt(playerName);
    }
}
