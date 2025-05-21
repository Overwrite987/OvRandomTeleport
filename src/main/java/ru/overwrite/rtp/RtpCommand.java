package ru.overwrite.rtp;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.channels.settings.Cooldown;
import ru.overwrite.rtp.configuration.Config;
import ru.overwrite.rtp.configuration.data.CommandMessages;
import ru.overwrite.rtp.utils.Utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class RtpCommand implements TabExecutor {

    private final OvRandomTeleport plugin;
    private final Config pluginConfig;
    private final RtpManager rtpManager;

    public RtpCommand(OvRandomTeleport plugin) {
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
            Player player = (Player) sender;
            if (rtpManager.hasActiveTasks(player.getName())) {
                return true;
            }
            Channel channel = rtpManager.getDefaultChannel();
            if (channel == null) {
                Utils.sendMessage(pluginConfig.getCommandMessages().channelNotSpecified(), player);
                return true;
            }
            processTeleport(player, channel);
            return true;
        }

        if (args[0].equalsIgnoreCase("admin")) {
            processAdminCommand(sender, args);
            return true;
        }

        if (args.length == 1) {
            Player player = (Player) sender;
            if (rtpManager.hasActiveTasks(player.getName())) {
                if (args[0].equalsIgnoreCase("cancel") && player.hasPermission("rtp.cancel")) {
                    rtpManager.getPerPlayerActiveRtpTask().get(player.getName()).cancel();
                    Utils.sendMessage(pluginConfig.getCommandMessages().cancelled(), player);
                }
                return true;
            }
            Channel channel = rtpManager.getChannelById(args[0]);
            if (channel == null) {
                Utils.sendMessage(pluginConfig.getCommandMessages().incorrectChannel(), player);
                return true;
            }
            processTeleport(player, channel);
            return true;
        }
        sender.sendMessage(pluginConfig.getCommandMessages().incorrectChannel());
        return true;
    }

    private void processTeleport(Player player, Channel channel) {
        rtpManager.printDebug("Channel name: " + channel.name() + " Channel permission: rtp.channel." + channel.id());
        rtpManager.printDebug(() -> "Player permission status: " + player.hasPermission("rtp.channel." + channel.id()));
        if (!player.hasPermission("rtp.channel." + channel.id())) {
            Utils.sendMessage(channel.messages().noPerms(), player);
            return;
        }
        Cooldown cooldown = channel.settings().cooldown();
        if (cooldown.hasCooldown(player)) {
            Utils.sendMessage(channel.messages().cooldown()
                    .replace("%time%",
                            Utils.getTime((int) (rtpManager.getChannelCooldown(player, cooldown) - (System.currentTimeMillis() - cooldown.playerCooldowns().get(player.getName())) / 1000))), player);
            return;
        }
        if (channel.minPlayersToUse() > 0 && (Bukkit.getOnlinePlayers().size() - 1) < channel.minPlayersToUse()) {
            Utils.sendMessage(channel.messages().notEnoughPlayers().replace("%required%", Integer.toString(channel.minPlayersToUse())), player);
            return;
        }
        if (!rtpManager.takeCost(player, channel)) {
            rtpManager.printDebug("Take cost for channel " + channel.id() + " didn't pass");
            return;
        }
        if (!channel.activeWorlds().contains(player.getWorld())) {
            rtpManager.printDebug("Active worlds for channel " + channel.id() + " does not includes player's world: " + player.getWorld().getName());
            if (channel.teleportToFirstAllowedWorld()) {
                rtpManager.printDebug("Teleporting to first allowed world: " + channel.activeWorlds().get(0).getName());
                rtpManager.preTeleport(player, channel, channel.activeWorlds().get(0), false);
                return;
            }
            Utils.sendMessage(channel.messages().invalidWorld(), player);
            return;
        }
        rtpManager.preTeleport(player, channel, player.getWorld(), false);
    }

    private void processAdminCommand(CommandSender sender, String[] args) {
        CommandMessages commandMessages = pluginConfig.getCommandMessages();
        if (!sender.hasPermission("rtp.admin")) {
            sender.sendMessage(commandMessages.incorrectChannel());
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(commandMessages.adminHelp());
            return;
        }
        switch (args[1].toLowerCase()) {
            case "reload": {
                rtpManager.cancelAllTasks();
                plugin.reloadConfig();
                final FileConfiguration config = plugin.getConfig();
                Utils.setupColorizer(config.getConfigurationSection("main_settings"));
                pluginConfig.setupMessages(config);
                pluginConfig.setupTemplates();
                rtpManager.getNamedChannels().clear();
                rtpManager.getSpecifications().clearAll();
                rtpManager.setupChannels(config, Bukkit.getPluginManager());
                sender.sendMessage(commandMessages.reload());
                return;
            }
            case "teleport", "forceteleport", "forcertp": {
                if (args.length < 4 || args.length > 5) {
                    sender.sendMessage(commandMessages.unknownArgument());
                    return;
                }
                Player targetPlayer = Bukkit.getPlayerExact(args[2]);
                if (targetPlayer == null) {
                    sender.sendMessage(commandMessages.playerNotFound());
                    return;
                }
                Channel channel = rtpManager.getChannelById(args[3]);
                if (channel == null) {
                    sender.sendMessage(commandMessages.incorrectChannel());
                    return;
                }
                if (!channel.activeWorlds().contains(targetPlayer.getWorld())) {
                    if (channel.teleportToFirstAllowedWorld()) {
                        processForceTeleport(args, targetPlayer, channel, channel.activeWorlds().get(0));
                        return;
                    }
                    sender.sendMessage(channel.messages().invalidWorld());
                    return;
                }
                processForceTeleport(args, targetPlayer, channel, targetPlayer.getWorld());
                return;
            }
            case "help": {
                sender.sendMessage(commandMessages.adminHelp());
                return;
            }
            case "update": {
                checkAndUpdatePlugin(sender, plugin);
                return;
            }
            case "debug": {
                Utils.DEBUG = !Utils.DEBUG;
                String message = "§7Дебаг переключен в значение: "
                        + (Utils.DEBUG ? "§a" : "§c")
                        + Utils.DEBUG;
                sender.sendMessage(message);
                return;
            }
            default: {
                sender.sendMessage(commandMessages.unknownArgument());
            }
        }
    }

    public void checkAndUpdatePlugin(CommandSender sender, OvRandomTeleport plugin) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> Utils.checkUpdates(plugin, version -> {
            sender.sendMessage("§6Подождите немного...");

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
                    sender.sendMessage("§aЗагрузка: " + downloadedKB + "/" + fullSizeKB + "KB (" + progressPercentage + "%)");
                }
            }
        }
    }

    private void processForceTeleport(String[] args, Player targetPlayer, Channel channel, World world) {
        if (args.length == 5 && args[4].equalsIgnoreCase("force")) {
            rtpManager.preTeleport(targetPlayer, channel, world, true);
            return;
        }
        rtpManager.preTeleport(targetPlayer, channel, world, false);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        final List<String> completions = new ArrayList<>();
        if (args.length == 1 && sender instanceof Player player) {
            if (rtpManager.hasActiveTasks(player.getName()) && player.hasPermission("rtp.cancel")) {
                completions.add("cancel");
                return getResult(args, completions);
            }
            for (String channelName : rtpManager.getNamedChannels().keySet()) {
                if (player.hasPermission("rtp.channel." + channelName)) {
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
        return arg.equalsIgnoreCase("forceteleport")
                || arg.equalsIgnoreCase("forcertp")
                || arg.equalsIgnoreCase("teleport");
    }

    private void getForceRtpTabCompletion(String[] args, List<String> completions) {
        if (args.length == 3) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                completions.add(onlinePlayer.getName());
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
        if (completions.isEmpty()) {
            return completions;
        }
        final List<String> result = new ArrayList<>();
        for (int i = 0; i < completions.size(); i++) {
            String c = completions.get(i);
            if (StringUtil.startsWithIgnoreCase(c, args[args.length - 1])) {
                result.add(c);
            }
        }
        return result;
    }
}
