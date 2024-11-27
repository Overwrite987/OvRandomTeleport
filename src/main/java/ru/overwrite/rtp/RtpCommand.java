package ru.overwrite.rtp;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
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
import ru.overwrite.rtp.configuration.Config;
import ru.overwrite.rtp.configuration.data.CommandMessages;
import ru.overwrite.rtp.utils.Utils;

public class RtpCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final Config pluginConfig;
    private final RtpManager rtpManager;

    public RtpCommand(Main plugin) {
        this.plugin = plugin;
        this.rtpManager = plugin.getRtpManager();
        this.pluginConfig = plugin.getPluginConfig();
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player) && (args.length == 0 || !args[0].equalsIgnoreCase("admin"))) {
            plugin.getPluginLogger().info("Вы должны быть игроком!");
            return true;
        }
        if (args.length == 0) {
            Player p = (Player) sender;
            if (rtpManager.hasActiveTasks(p.getName())) {
                return true;
            }
            Channel channel = rtpManager.getDefaultChannel();
            if (channel == null) {
                Utils.sendMessage(pluginConfig.getCommandMessages().channelNotSpecified(), p);
                return true;
            }
            return processTeleport(p, channel);
        }

        if (args[0].equalsIgnoreCase("admin")) {
            return processAdminCommand(sender, args);
        }

        if (args.length == 1) {
            Player p = (Player) sender;
            if (rtpManager.hasActiveTasks(p.getName())) {
                if (args[0].equalsIgnoreCase("cancel") && p.hasPermission("rtp.cancel")) {
                    rtpManager.getPerPlayerActiveRtpTask().get(p.getName()).cancel();
                    Utils.sendMessage(pluginConfig.getCommandMessages().cancelled(), p);
                }
                return true;
            }
            if (!rtpManager.getNamedChannels().containsKey(args[0])) {
                Utils.sendMessage(pluginConfig.getCommandMessages().incorrectChannel(), p);
                return true;
            }
            Channel channel = rtpManager.getChannelByName(args[0]);
            return processTeleport(p, channel);
        } else {
            sender.sendMessage(pluginConfig.getCommandMessages().incorrectChannel());
            return true;
        }
    }

    private boolean processTeleport(Player p, Channel channel) {
        if (Utils.DEBUG) {
            plugin.getPluginLogger().info("Channel name: " + channel.name() + " Channel permission: " + "rtp.channel." + channel.id());
            plugin.getPluginLogger().info("Player permission status: " + p.hasPermission("rtp.channel." + channel.id()));
        }
        if (!p.hasPermission("rtp.channel." + channel.id())) {
            Utils.sendMessage(channel.messages().noPerms(), p);
            return true;
        }
        if (channel.cooldown().hasCooldown(p)) {
            Utils.sendMessage(channel.messages().cooldown()
                    .replace("%time%",
                            Utils.getTime((int) (rtpManager.getChannelCooldown(p, channel.cooldown()) - (System.currentTimeMillis() - channel.cooldown().playerCooldowns().get(p.getName())) / 1000))), p);
            return true;
        }
        if (channel.minPlayersToUse() > 0 && (Bukkit.getOnlinePlayers().size() - 1) < channel.minPlayersToUse()) {
            Utils.sendMessage(channel.messages().notEnoughPlayers().replace("%required%", Integer.toString(channel.minPlayersToUse())), p);
            return true;
        }
        if (!rtpManager.takeCost(p, channel)) {
            if (Utils.DEBUG) {
                plugin.getPluginLogger().info("Take cost for channel " + channel.id() + " didn't pass");
            }
            return true;
        }
        if (!channel.activeWorlds().contains(p.getWorld())) {
            if (Utils.DEBUG) {
                plugin.getPluginLogger().info("Active worlds for channel " + channel.id() + " does not includes player's world: " + p.getWorld().getName());
            }
            if (channel.teleportToFirstAllowedWorld()) {
                if (Utils.DEBUG) {
                    plugin.getPluginLogger().info("Teleporting to first allowed world: " + channel.activeWorlds().get(0));
                }
                rtpManager.preTeleport(p, channel, channel.activeWorlds().get(0));
                return true;
            }
            Utils.sendMessage(channel.messages().invalidWorld(), p);
            return true;
        }
        rtpManager.preTeleport(p, channel, p.getWorld());
        return true;
    }

    private boolean processAdminCommand(CommandSender sender, String[] args) {
        CommandMessages commandMessages = pluginConfig.getCommandMessages();
        if (!sender.hasPermission("rtp.admin")) {
            sender.sendMessage(commandMessages.incorrectChannel());
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(commandMessages.adminHelp());
            return true;
        }
        switch (args[1].toLowerCase()) {
            case "reload": {
                plugin.reloadConfig();
                final FileConfiguration config = plugin.getConfig();
                Utils.setupColorizer(config.getConfigurationSection("main_settings"));
                pluginConfig.setupMessages(config);
                rtpManager.getNamedChannels().clear();
                rtpManager.getSpecifications().clearAll();
                rtpManager.setupChannels(config, Bukkit.getPluginManager());
                sender.sendMessage(commandMessages.reload());
                return true;
            }
            case "teleport", "forceteleport", "forcertp": {
                if (args.length < 4 || args.length > 5) {
                    sender.sendMessage(commandMessages.unknownArgument());
                    return true;
                }
                Player targetPlayer = Bukkit.getPlayerExact(args[2]);
                if (targetPlayer == null) {
                    sender.sendMessage(commandMessages.playerNotFound());
                    return true;
                }
                if (!rtpManager.getNamedChannels().containsKey(args[3])) {
                    sender.sendMessage(commandMessages.incorrectChannel());
                    return true;
                }
                Channel channel = rtpManager.getChannelByName(args[3]);
                if (!channel.activeWorlds().contains(targetPlayer.getWorld())) {
                    if (channel.teleportToFirstAllowedWorld()) {
                        processForceTeleport(args, targetPlayer, channel, channel.activeWorlds().get(0));
                        return true;
                    }
                    sender.sendMessage(channel.messages().invalidWorld());
                    return true;
                }
                processForceTeleport(args, targetPlayer, channel, targetPlayer.getWorld());
                return true;
            }
            case "help": {
                sender.sendMessage(commandMessages.adminHelp());
                return true;
            }
            case "update": {
                checkAndUpdatePlugin(sender, plugin);
                return true;
            }
            case "debug": {
                Utils.DEBUG = !Utils.DEBUG;
                String message = "§7Дебаг переключен в значение: "
                        + (Utils.DEBUG ? "§a" : "§c")
                        + Utils.DEBUG;
                sender.sendMessage(message);
                return true;
            }
        }
        sender.sendMessage(commandMessages.unknownArgument());
        return true;
    }

    public void checkAndUpdatePlugin(CommandSender sender, Main plugin) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> Utils.checkUpdates(plugin, version -> {
            sender.sendMessage("§6========================================");

            String currentVersion = plugin.getDescription().getVersion();

            if (currentVersion.equals(version)) {
                sender.sendMessage("§aВы уже используете последнюю версию плагина!");
            } else {
                String currentJarName = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().getPath()).getName();
                String downloadUrl = "https://github.com/Overwrite987/OvRandomTeleport/releases/download/" + version + "/" + "OvRandomTeleport-" + version + ".jar";
                try {
                    File updateFolder = Bukkit.getUpdateFolderFile();
                    File targetFile = new File(updateFolder, currentJarName);

                    downloadFile(downloadUrl, targetFile, sender);

                    sender.sendMessage("§aОбновление было загружено успешно!");
                    sender.sendMessage("§aПерезапустите сервер, чтобы применить обновление.");
                } catch (IOException ex) {
                    sender.sendMessage("§cОшибка при загрузке обновления: " + ex.getMessage());
                }
            }
            sender.sendMessage("§6========================================");
        }));
    }

    public void downloadFile(String fileURL, File targetFile, CommandSender sender) throws IOException {
        URL url = new URL(fileURL);
        URLConnection connection = url.openConnection();
        int fileSize = connection.getContentLength();

        try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
             FileOutputStream out = new FileOutputStream(targetFile)) {

            byte[] data = new byte[1024];
            int bytesRead;
            int totalBytesRead = 0;
            int lastPercentage = 0;

            while ((bytesRead = in.read(data, 0, 1024)) != -1) {
                out.write(data, 0, bytesRead);
                totalBytesRead += bytesRead;
                int progressPercentage = (int) ((double) totalBytesRead / fileSize * 100);

                if (progressPercentage >= lastPercentage + 10) {
                    lastPercentage = progressPercentage;
                    int downloadedKB = totalBytesRead / 1024;
                    int fullSizeKB = fileSize / 1024;
                    sender.sendMessage("§aЗагрузка: " + downloadedKB + "/" + fullSizeKB + "KB) (" + progressPercentage + "%)");
                }
            }
        }
    }

    private void processForceTeleport(String[] args, Player targetPlayer, Channel channel, World world) {
        if (args.length == 5 && args[4].equalsIgnoreCase("force")) {
            rtpManager.teleportPlayer(targetPlayer, channel, rtpManager.getLocationGenerator().generateRandomLocation(targetPlayer, channel, world));
            return;
        }
        rtpManager.preTeleport(targetPlayer, channel, world);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        final List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender instanceof Player p) {
                if (rtpManager.hasActiveTasks(p.getName()) && p.hasPermission("rtp.cancel")) {
                    completions.add("cancel");
                    return getResult(args, completions);
                }
                for (String channelName : rtpManager.getNamedChannels().keySet()) {
                    if (p.hasPermission("rtp.channel." + channelName)) {
                        completions.add(channelName);
                    }
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
                    completions.add("teleport");
                    completions.add("forceteleport");
                    completions.add("forcertp");
                    completions.add("update");
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
        final List<String> result = new ArrayList<>();
        for (String c : completions) {
            if (Utils.startsWithIgnoreCase(c, args[args.length - 1])) {
                result.add(c);
            }
        }
        return result;
    }
}
