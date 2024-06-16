package ru.overwrite.rtp;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;

import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.utils.Config;
import ru.overwrite.rtp.utils.Utils;

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
			for (Channel channel : rtpManager.getVoidChannels().keySet()) {
				if (!p.hasPermission("rtp.channel." + rtpManager.getVoidChannels().get(channel))) {
					continue;
				}
				if (!channel.getActiveWorlds().contains(p.getWorld())) {
					if (channel.isTeleportToFirstAllowedWorld()) {
						rtpManager.preTeleport(p, channel, channel.getActiveWorlds().get(0));
						return;
					}
					return;
				}
				rtpManager.preTeleport(p, channel, p.getWorld());
				return;
			}
		}
		String playerName = p.getName();
		if (rtpManager.hasActiveTasks(playerName) && rtpManager.getPerPlayerActiveRtpTask().get(playerName).getActiveChannel().isRestrictMove()) {
			p.sendMessage(rtpManager.getPerPlayerActiveRtpTask().get(playerName).getActiveChannel().getMovedOnTeleportMessage());
			rtpManager.getPerPlayerActiveRtpTask().get(playerName).cancel();
			rtpManager.teleportingNow.remove(playerName);
			rtpManager.getPerPlayerActiveRtpTask().remove(playerName);
		}
	}

	@EventHandler
	public void onFirstJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		if (p.hasPlayedBefore()) {
			return;
		}
		for (Channel channel : rtpManager.getJoinChannels().keySet()) {
			if (!p.hasPermission("rtp.channel." + rtpManager.getJoinChannels().get(channel))) {
				continue;
			}
			if (!channel.getActiveWorlds().contains(p.getWorld())) {
				if (channel.isTeleportToFirstAllowedWorld()) {
					rtpManager.preTeleport(p, channel, channel.getActiveWorlds().get(0));
					return;
				}
				return;
			}
			rtpManager.preTeleport(p, channel, p.getWorld());
			return;
		}
	}

	@EventHandler
	public void onRespawn(PlayerRespawnEvent e) {
		Player p = e.getPlayer();
		for (Channel channel : rtpManager.getRespawnChannels().keySet()) {
			if (!p.hasPermission("rtp.channel." + rtpManager.getRespawnChannels().get(channel))) {
				continue;
			}
			if (!channel.getActiveWorlds().contains(p.getWorld())) {
				if (channel.isTeleportToFirstAllowedWorld()) {
					rtpManager.preTeleport(p, channel, channel.getActiveWorlds().get(0));
					return;
				}
				return;
			}
			rtpManager.preTeleport(p, channel, p.getWorld());
			return;
		}
	}
	
	@EventHandler
	public void onDamage(EntityDamageEvent e) {
		if (!(e.getEntity() instanceof Player)) {
			return;
		}
		Player p = (Player) e.getEntity();
		String playerName = p.getName();
		if (rtpManager.hasActiveTasks(playerName) && rtpManager.getPerPlayerActiveRtpTask().get(playerName).getActiveChannel().isRestrictDamage()) {
			p.sendMessage(rtpManager.getPerPlayerActiveRtpTask().get(playerName).getActiveChannel().getDamagedOnTeleportMessage());
			rtpManager.getPerPlayerActiveRtpTask().get(playerName).cancel();
			rtpManager.teleportingNow.remove(playerName);
			rtpManager.getPerPlayerActiveRtpTask().remove(playerName);
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
				rtpManager.getPerPlayerActiveRtpTask().get(playerName).cancel();
				rtpManager.teleportingNow.remove(playerName);
				rtpManager.getPerPlayerActiveRtpTask().remove(playerName);
			}
		});
	}
}
