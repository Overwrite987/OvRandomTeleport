package ru.overwrite.rtp;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.utils.Config;
import ru.overwrite.rtp.utils.Utils;

public class RtpCommand implements CommandExecutor, TabCompleter {
	
	private final Main plugin;
	private final Config pluginConfig;
	private final RtpManager rtpManager;
	
	public RtpCommand(Main plugin) {
		this.plugin = plugin;
		this.pluginConfig = plugin.getPluginConfig();
		this.rtpManager = plugin.getRtpManager();
	}
	
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
		if (!(sender instanceof Player) && (args.length == 0 || !args[0].equals("admin"))) {
			plugin.getLogger().info("Вы должны быть игроком!");
			return true;
		}
		if (args.length == 0) {
			Player p = (Player) sender;
			if (rtpManager.hasActiveTasks(p.getName())) {
				return false;
			}
			Channel channel = rtpManager.getDefaultChannel();
			return processTeleport(p, channel);
		}

		if (args.length == 1) {
			if (args[0].equals("admin")) {
				return processAdminCommand(sender, args);
			}
			Player p = (Player) sender;
			if (rtpManager.hasActiveTasks(p.getName())) {
				return false;
			}
			if (!rtpManager.getNamedChannels().containsKey(args[0])) {
				p.sendMessage(pluginConfig.messages_incorrect_channel);
				return false;
			}
			Channel channel = rtpManager.getChannelByName(args[0]);
			return processTeleport(p, channel);
		} else {
			sender.sendMessage(pluginConfig.messages_incorrect_channel);
			return false;
		}
	}

	private boolean processTeleport(Player p, Channel channel) {
		if (!p.hasPermission("rtp.channel." + channel.getId())) {
			p.sendMessage(channel.getNoPermsMessage());
			return false;
		}
		if (channel.getPlayerCooldowns().containsKey(p.getName())) {
			p.sendMessage(channel.getCooldownMessage()
					.replace("%time%", Utils.getTime((int) (channel.getCooldown() - (System.currentTimeMillis() - channel.getPlayerCooldowns().get(p.getName())) / 1000))));
			return false;
		}
		if (!channel.getActiveWorlds().contains(p.getWorld())) {
			if (channel.isTeleportToFirstAllowedWorld()) {
				rtpManager.preTeleport(p, channel, channel.getActiveWorlds().get(0));
				return true;
			}
			p.sendMessage(channel.getInvalidWorldMessage());
			return false;
		}
		if (plugin.getEconomy() != null) {
			if (plugin.getEconomy().getBalance(p) < channel.getTeleportCost()) {
				p.sendMessage(channel.getNotEnoughMoneyMessage());
				return false;
			}
			plugin.getEconomy().withdrawPlayer(p, channel.getTeleportCost());
		}
		rtpManager.preTeleport(p, channel, p.getWorld());
		return true;
	}

	private boolean processAdminCommand(CommandSender sender, String[] args) {
		if (!sender.hasPermission("rtp.admin")) {
			sender.sendMessage(pluginConfig.messages_incorrect_channel);
			return false;
		}
		switch (args[1].toLowerCase()) {
			case "reload": {
				plugin.reloadConfig();
				FileConfiguration config = plugin.getConfig();
				pluginConfig.setupMessages(config);
				rtpManager.getNamedChannels().clear();
				rtpManager.setupChannels(config, Bukkit.getPluginManager());
				sender.sendMessage(pluginConfig.messages_reload);
				return true;
			}
			case "forceteleport":
			case "forcertp": {
				if (args.length < 4) {
					sender.sendMessage(pluginConfig.messages_unknown_argument);
					return false;
				}
				Player targetPlayer = Bukkit.getPlayerExact(args[2]);
				if (targetPlayer == null) {
					sender.sendMessage(pluginConfig.messages_player_not_found);
					return false;
				}
				if (!rtpManager.getNamedChannels().containsKey(args[3])) {
					sender.sendMessage(pluginConfig.messages_incorrect_channel);
					return false;
				}
				Channel channel = rtpManager.getChannelByName(args[3]);
				if (!channel.getActiveWorlds().contains(targetPlayer.getWorld())) {
					if (channel.isTeleportToFirstAllowedWorld()) {
						rtpManager.preTeleport(targetPlayer, channel, channel.getActiveWorlds().get(0));
						return true;
					}
					sender.sendMessage(channel.getInvalidWorldMessage());
					return false;
				}
				rtpManager.preTeleport(targetPlayer, channel, targetPlayer.getWorld());
				return true;
			}
			case "help": {
				sender.sendMessage(pluginConfig.messages_admin_help);
				return true;
			}
			case "debug": {
				Utils.DEBUG = !Utils.DEBUG;
				sender.sendMessage("Дебаг переключен в значение: " + Utils.DEBUG);
				return true;
			}
		}
		sender.sendMessage(pluginConfig.messages_unknown_argument);
		return false;
	}
	
	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
		List<String> completions = new ArrayList<>();
		if (args.length == 1) {
			for (String channelName : rtpManager.getNamedChannels().keySet()) {
				if (sender.hasPermission("rtp.channel." + channelName)) {
					completions.add(channelName);
				}
			}
		}
		if (sender.hasPermission("rtp.admin")) {
			if (args.length == 1) {
				completions.add("admin");
			}
			if (args[0].equalsIgnoreCase("admin")) {
				if (args.length == 2) {
					completions.add("help");
					completions.add("reload");
					completions.add("forceteleport");
					completions.add("forcertp");
					completions.add("debug");
				}
				if (args[1].equalsIgnoreCase("forceteleport")) {
					if (args.length == 3) {
						for (Player p : Bukkit.getOnlinePlayers()) {
							completions.add(p.getName());
						}
					}
					if (args.length == 4) {
						completions.addAll(rtpManager.getNamedChannels().keySet());
					}
				}
			}
		}
		List<String> result = new ArrayList<>();
		for (String c : completions) {
			if (c.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
				result.add(c);
			}
		}
		return result;
	}
}
