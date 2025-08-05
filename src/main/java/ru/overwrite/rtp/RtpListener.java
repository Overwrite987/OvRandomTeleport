package ru.overwrite.rtp;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.projectiles.ProjectileSource;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.channels.settings.Restrictions;
import ru.overwrite.rtp.utils.Utils;
import ru.overwrite.rtp.utils.VersionUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class RtpListener implements Listener {

    private final OvRandomTeleport plugin;
    private final RtpManager rtpManager;

    public RtpListener(OvRandomTeleport plugin) {
        this.plugin = plugin;
        this.rtpManager = plugin.getRtpManager();
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (!e.hasChangedBlock()) {
            return;
        }
        Player player = e.getPlayer();
        RtpManager.Specifications specifications = rtpManager.getSpecifications();
        Map<String, List<World>> voidChannels = specifications.voidChannels();
        if (!voidChannels.isEmpty() && e.getFrom().getBlockY() > e.getTo().getBlockY()) {
            for (Map.Entry<String, List<World>> entry : voidChannels.entrySet()) {
                String channelId = entry.getKey();
                Object2IntMap<String> voidLevels = specifications.voidLevels();
                if (e.getTo().getBlockY() >
                        (voidLevels.isEmpty()
                                ? VersionUtils.VOID_LEVEL
                                : voidLevels.getOrDefault(channelId, VersionUtils.VOID_LEVEL))) {
                    continue;
                }
                List<World> worlds = entry.getValue();
                if (!worlds.contains(player.getWorld())) {
                    continue;
                }
                if (!player.hasPermission("rtp.channel." + channelId)) {
                    continue;
                }
                this.processTeleport(player, rtpManager.getChannelById(channelId), true);
                return;
            }
        }
        String playerName = player.getName();
        if (rtpManager.hasActiveTasks(playerName)) {
            Channel activeChannel = getActiveChannel(playerName);
            if (activeChannel.settings().restrictions().restrictMove()) {
                Utils.sendMessage(activeChannel.messages().movedOnTeleport(), player);
                this.cancelTeleportation(playerName);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            return;
        }
        Player player = e.getPlayer();
        String playerName = player.getName();
        if (rtpManager.hasActiveTasks(playerName)) {
            Channel activeChannel = getActiveChannel(playerName);
            if (activeChannel.settings().restrictions().restrictTeleport()) {
                Utils.sendMessage(activeChannel.messages().teleportedOnTeleport(), player);
                this.cancelTeleportation(playerName);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        if (rtpManager.getProxyCalls() != null && !rtpManager.getProxyCalls().isEmpty()) {
            String data = rtpManager.getProxyCalls().get(player.getName());
            if (data == null) {
                return;
            }
            int separatorIndex = data.indexOf(';');
            Channel channel = rtpManager.getChannelById(data.substring(0, separatorIndex));
            World world = Bukkit.getWorld(data.substring(separatorIndex + 1));
            rtpManager.preTeleport(player, channel, world, false);
            rtpManager.getProxyCalls().remove(player.getName());
            return;
        }
        if (player.hasPlayedBefore()) {
            return;
        }
        Set<String> joinChannels = rtpManager.getSpecifications().joinChannels();
        if (joinChannels.isEmpty()) {
            return;
        }
        for (String channelId : joinChannels) {
            if (!player.hasPermission("rtp.channel." + channelId)) {
                continue;
            }
            this.processTeleport(player, rtpManager.getChannelById(channelId), false);
            return;
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Map<String, List<World>> respawnChannels = rtpManager.getSpecifications().respawnChannels();
        if (respawnChannels.isEmpty()) {
            return;
        }
        Player player = e.getPlayer();
        for (Map.Entry<String, List<World>> entry : respawnChannels.entrySet()) {
            List<World> worlds = entry.getValue();
            if (!worlds.contains(player.getWorld())) {
                continue;
            }
            String channelId = entry.getKey();
            if (!player.hasPermission("rtp.channel." + channelId)) {
                continue;
            }
            this.processTeleport(player, rtpManager.getChannelById(channelId), false);
            return;
        }
    }

    private void processTeleport(Player player, Channel channel, boolean force) {
        if (!channel.activeWorlds().contains(player.getWorld())) {
            if (channel.teleportToFirstAllowedWorld()) {
                rtpManager.preTeleport(player, channel, channel.activeWorlds().get(0), force);
            }
            return;
        }
        rtpManager.preTeleport(player, channel, player.getWorld(), force);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) {
            return;
        }
        String playerName = player.getName();
        if (rtpManager.hasActiveTasks(playerName)) {
            Channel activeChannel = rtpManager.getPerPlayerActiveRtpTask().get(playerName).getActiveChannel();
            Restrictions restrictions = activeChannel.settings().restrictions();
            if (restrictions.restrictDamage() && !restrictions.damageCheckOnlyPlayers()) {
                Utils.sendMessage(activeChannel.messages().damagedOnTeleport(), player);
                this.cancelTeleportation(playerName);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        Entity damagerEntity = e.getDamager();
        Entity damagedEntity = e.getEntity();

        if (damagerEntity instanceof Player damager) {
            this.handleDamagerPlayer(damager, damagedEntity);
        }
        if (damagedEntity instanceof Player damaged) {
            this.handleDamagedPlayer(damagerEntity, damaged);
        }
    }

    private void handleDamagerPlayer(Player damager, Entity damagedEntity) {
        String damagerName = damager.getName();
        if (rtpManager.hasActiveTasks(damagerName)) {
            Channel activeChannel = getActiveChannel(damagerName);
            Restrictions restrictions = activeChannel.settings().restrictions();
            if (restrictions.restrictDamageOthers()) {
                if (restrictions.damageCheckOnlyPlayers() && !(damagedEntity instanceof Player)) {
                    return;
                }
                Utils.sendMessage(activeChannel.messages().damagedOtherOnTeleport(), damager);
                this.cancelTeleportation(damagerName);
            }
        }
    }

    private void handleDamagedPlayer(Entity damagerEntity, Player damaged) {
        String damagedName = damaged.getName();
        if (rtpManager.hasActiveTasks(damagedName)) {
            Channel activeChannel = getActiveChannel(damagedName);
            Restrictions restrictions = activeChannel.settings().restrictions();
            if (restrictions.restrictDamage()) {
                Player damager = getDamager(damagerEntity);
                if (damager == null && restrictions.damageCheckOnlyPlayers()) {
                    return;
                }
                Utils.sendMessage(activeChannel.messages().damagedOnTeleport(), damaged);
                this.cancelTeleportation(damagedName);
            }
        }
    }

    private Channel getActiveChannel(String playerName) {
        return rtpManager.getPerPlayerActiveRtpTask().get(playerName).getActiveChannel();
    }

    private Player getDamager(Entity damagerEntity) {
        if (damagerEntity instanceof Player player) {
            return player;
        }
        if (damagerEntity instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player) {
                return player;
            }
        }
        if (damagerEntity instanceof AreaEffectCloud areaEffectCloud) {
            ProjectileSource source = areaEffectCloud.getSource();
            if (source instanceof Player player) {
                return player;
            }
        }
        if (damagerEntity instanceof TNTPrimed tntPrimed) {
            Entity source = tntPrimed.getSource();
            if (source instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDeath(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof Player player)) {
            return;
        }
        this.handlePlayerLeave(player);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        this.handlePlayerLeave(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onKick(PlayerKickEvent e) {
        Player player = e.getPlayer();
        this.handlePlayerLeave(player);
    }

    private void handlePlayerLeave(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String playerName = player.getName();
            if (rtpManager.hasActiveTasks(playerName)) {
                this.cancelTeleportation(playerName);
            }
        });
    }

    private void cancelTeleportation(String playerName) {
        rtpManager.printDebug("Teleportation for player " + playerName + " was cancelled because of restrictions");
        rtpManager.getPerPlayerActiveRtpTask().get(playerName).cancel(true);
        rtpManager.getLocationGenerator().getIterationsPerPlayer().removeInt(playerName);
    }
}
