package ru.overwrite.rtp;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
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
        if (!(sender instanceof Player) && (args.length == 0 || !args[0].equalsIgnoreCase("admin"))) {
            plugin.getPluginLogger().info("Вы должны быть игроком!");
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

        if (args[0].equalsIgnoreCase("admin")) {
            return processAdminCommand(sender, args);
        }

        if (args.length == 1) {
            Player p = (Player) sender;
            if (rtpManager.hasActiveTasks(p.getName())) {
                return false;
            }
            if (!rtpManager.getNamedChannels().containsKey(args[0])) {
                Utils.sendMessage(pluginConfig.messages_incorrect_channel, p);
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
        if (Utils.DEBUG) {
            plugin.getPluginLogger().info("Channel name: " + channel.getName() + " Channel permission: " + "rtp.channel." + channel.getName());
            plugin.getPluginLogger().info("Player permission status: " + p.hasPermission("rtp.channel." + channel.getId()));
        }
        if (!p.hasPermission("rtp.channel." + channel.getId())) {
            Utils.sendMessage(channel.getMessages().noPermsMessage(), p);
            return false;
        }
        if (channel.getCooldown().hasCooldown(p)) {
            Utils.sendMessage(channel.getMessages().cooldownMessage()
                    .replace("%time%",
                            Utils.getTime((int) (rtpManager.getChannelCooldown(p, channel.getCooldown()) - (System.currentTimeMillis() - channel.getCooldown().playerCooldowns().get(p.getName())) / 1000))), p);
            return false;
        }
        if (channel.getMinPlayersToUse() > 0 && (Bukkit.getOnlinePlayers().size() - 1) < channel.getMinPlayersToUse()) {
            Utils.sendMessage(channel.getMessages().notEnoughPlayersMessage().replace("%required%", Integer.toString(channel.getMinPlayersToUse())), p);
            return false;
        }
        if (!rtpManager.takeCost(p, channel)) {
            return false;
        }
        if (!channel.getActiveWorlds().contains(p.getWorld())) {
            if (Utils.DEBUG) {
                plugin.getPluginLogger().info("Active worlds for channel " + channel.getName() + " does not includes player's world: " + p.getWorld().getName());
            }
            if (channel.isTeleportToFirstAllowedWorld()) {
                if (Utils.DEBUG) {
                    plugin.getPluginLogger().info("Teleporting to first allowed world: " + channel.getActiveWorlds().get(0));
                }
                rtpManager.preTeleport(p, channel, channel.getActiveWorlds().get(0));
                return true;
            }
            Utils.sendMessage(channel.getMessages().invalidWorldMessage(), p);
            return false;
        }
        rtpManager.preTeleport(p, channel, p.getWorld());
        return true;
    }

    private boolean processAdminCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rtp.admin")) {
            sender.sendMessage(pluginConfig.messages_incorrect_channel);
            return false;
        }
        if (args.length < 2) {
            sender.sendMessage(pluginConfig.messages_admin_help);
            return true;
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
            case "teleport", "forceteleport", "forcertp": {
                if (args.length < 4 || args.length > 5) {
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
                        processForceTeleport(args, targetPlayer, channel, channel.getActiveWorlds().get(0));
                        return true;
                    }
                    sender.sendMessage(channel.getMessages().invalidWorldMessage());
                    return false;
                }
                processForceTeleport(args, targetPlayer, channel, targetPlayer.getWorld());
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

    private void processForceTeleport(String[] args, Player targetPlayer, Channel channel, World world) {
        if (args.length == 5 && args[4].equalsIgnoreCase("force")) {
            rtpManager.teleportPlayer(targetPlayer, channel, rtpManager.generateRandomLocation(targetPlayer, channel, world));
            return;
        }
        rtpManager.preTeleport(targetPlayer, channel, world);
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
                if (args.length > 2 && isForceRtp(args[1])) {
                    getForceRtpTabCompletion(args, completions);
                }
            }
        }
        return getResult(args, completions);
    }

    private boolean isForceRtp(String arg) {
        return arg.equalsIgnoreCase("forceteleport") || arg.equalsIgnoreCase("forcertp");
    }

    private void getForceRtpTabCompletion(String[] args, List<String> completions) {
        if (args.length == 3) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        }
        if (args.length == 4) {
            completions.addAll(rtpManager.getNamedChannels().keySet());
        }
        if (args.length == 5) {
            completions.add("force");
        }
    }

    private List<String> getResult(String[] args, List<String> completions) {
        List<String> result = new ArrayList<>();
        for (String c : completions) {
            if (c.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
                result.add(c);
            }
        }
        return result;
    }
}
