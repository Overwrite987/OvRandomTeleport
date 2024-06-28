package ru.overwrite.rtp;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;

import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.utils.Utils;

import java.util.Map;

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
		if (e.getTo().getBlockY() < Utils.VOID_LEVEL) {
			Map<Channel, String> voidChannels = rtpManager.getVoidChannels();
			for (Channel channel : voidChannels.keySet()) {
				if (!p.hasPermission("rtp.channel." + voidChannels.get(channel))) {
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
				p.sendMessage(activeChannel.getMessages().movedOnTeleportMessage());
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
				p.sendMessage(activeChannel.getMessages().teleportedOnTeleportMessage());
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
		Map<Channel, String> joinChannels = rtpManager.getJoinChannels();
		for (Channel channel : joinChannels.keySet()) {
			if (!p.hasPermission("rtp.channel." + joinChannels.get(channel))) {
				continue;
			}
			processTeleport(p, channel);
			return;
		}
	}

	@EventHandler
	public void onRespawn(PlayerRespawnEvent e) {
		Player p = e.getPlayer();
		Map<Channel, String> respawnChannels = rtpManager.getRespawnChannels();
		for (Channel channel : respawnChannels.keySet()) {
			if (!p.hasPermission("rtp.channel." + respawnChannels.get(channel))) {
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
				p.sendMessage(activeChannel.getMessages().damagedOnTeleportMessage());
				cancelTeleportation(playerName);
			}
		}
	}

	@EventHandler
	public void onDamageOther(EntityDamageByEntityEvent e) {
		if (e.getDamager() instanceof Player damager) {
			String damagerName = damager.getName();
			if (rtpManager.hasActiveTasks(damagerName)) {
				Channel activeChannel = rtpManager.getPerPlayerActiveRtpTask().get(damagerName).getActiveChannel();
				if (activeChannel.getRestrictions().restrictDamageOthers()) {
					if (activeChannel.getRestrictions().damageCheckOnlyPlayers() && !(e.getEntity() instanceof Player)) {
						return;
					}
					damager.sendMessage(activeChannel.getMessages().damagedOtherOnTeleportMessage());
					cancelTeleportation(damagerName);
				}
			}
		}
		if (e.getEntity() instanceof Player damaged) {
			String damagedName = damaged.getName();
			if (rtpManager.hasActiveTasks(damagedName)) {
				Channel activeChannel = rtpManager.getPerPlayerActiveRtpTask().get(damagedName).getActiveChannel();
				if (activeChannel.getRestrictions().restrictDamage()) {
					if (activeChannel.getRestrictions().damageCheckOnlyPlayers() && !(e.getDamager() instanceof Player)) {
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
		});
	}

	private void cancelTeleportation(String playerName) {
		rtpManager.getPerPlayerActiveRtpTask().get(playerName).cancel();
		rtpManager.teleportingNow.remove(playerName);
		rtpManager.getPerPlayerActiveRtpTask().remove(playerName);
	}
}
