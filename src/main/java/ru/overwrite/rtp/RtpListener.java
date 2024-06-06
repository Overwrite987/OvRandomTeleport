package ru.overwrite.rtp;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.utils.Config;
import ru.overwrite.rtp.utils.Utils;

public class RtpListener implements Listener {
	
	private final Main plugin;
	private final Config pluginConfig;
	private final RtpManager rtpManager;
	
	public RtpListener(Main plugin) {
		this.plugin = plugin;
		this.pluginConfig = plugin.getPluginConfig();
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
		if (hasActiveTasks(playerName) && rtpManager.perPlayerActiveRtpChannel.get(playerName).isRestrictMove()) {
			p.sendMessage(pluginConfig.messages_moved_on_teleport);
			rtpManager.teleportingNow.remove(playerName);
			rtpManager.perPlayerActiveRtpTask.get(playerName).cancel();
			rtpManager.perPlayerActiveRtpTask.remove(playerName);
			rtpManager.perPlayerActiveRtpChannel.remove(playerName);
			if (rtpManager.perPlayerBossBar.containsKey(playerName)) {
				rtpManager.perPlayerBossBar.get(playerName).removeAll();
				rtpManager.perPlayerBossBar.remove(playerName);
			}
			rtpManager.perPlayerPreTeleportCooldown.remove(playerName);
		}
	}
	
	@EventHandler
	public void onDamage(EntityDamageEvent e) {
		if (!(e.getEntity() instanceof Player)) {
			return;
		}
		Player p = (Player) e.getEntity();
		String playerName = p.getName();
		if (hasActiveTasks(playerName) && rtpManager.perPlayerActiveRtpChannel.get(playerName).isRestrictDamage()) {
			p.sendMessage(pluginConfig.messages_damaged_on_teleport);
			rtpManager.teleportingNow.remove(playerName);
			rtpManager.perPlayerActiveRtpTask.get(playerName).cancel();
			rtpManager.perPlayerActiveRtpTask.remove(playerName);
			rtpManager.perPlayerActiveRtpChannel.remove(playerName);
			if (rtpManager.perPlayerBossBar.containsKey(playerName)) {
				rtpManager.perPlayerBossBar.get(playerName).removeAll();
				rtpManager.perPlayerBossBar.remove(playerName);
			}
			rtpManager.perPlayerPreTeleportCooldown.remove(playerName);
		}
	}
	
	@EventHandler
	public void onLeave(PlayerQuitEvent e) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			String playerName = e.getPlayer().getName();
			if (hasActiveTasks(playerName)) {
				rtpManager.perPlayerActiveRtpTask.get(playerName).cancel();
				rtpManager.perPlayerActiveRtpTask.remove(playerName);
			}
			rtpManager.perPlayerActiveRtpChannel.remove(playerName);
			rtpManager.perPlayerBossBar.remove(playerName);
			rtpManager.perPlayerPreTeleportCooldown.remove(playerName);
		});
	}
	
	@EventHandler
	public void onKick(PlayerKickEvent e) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			String playerName = e.getPlayer().getName();
			if (hasActiveTasks(playerName)) {
				rtpManager.perPlayerActiveRtpTask.get(playerName).cancel();
				rtpManager.perPlayerActiveRtpTask.remove(playerName);
			}
			rtpManager.perPlayerActiveRtpChannel.remove(playerName);
			rtpManager.perPlayerBossBar.remove(playerName);
			rtpManager.perPlayerPreTeleportCooldown.remove(playerName);
		});
	}

	private boolean hasActiveTasks(String playerName) {
		return !rtpManager.perPlayerActiveRtpTask.isEmpty() && rtpManager.perPlayerActiveRtpTask.containsKey(playerName);
	}
}
