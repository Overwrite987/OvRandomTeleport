package ru.overwrite.rtp;

import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.utils.Config;
import ru.overwrite.rtp.utils.Utils;

public class RtpExpansion extends PlaceholderExpansion {

    private final RtpManager rtpManager;

    public RtpExpansion(Main plugin) {
        this.rtpManager = plugin.getRtpManager();
    }

    @Override
    public String getAuthor() {
        return "OverwriteMC";
    }

    @Override
    public String getIdentifier() {
        return "ovrtp";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null || !player.isOnline()) {
            return "Player is not online! Can't parse placeholder";
        }
        String[] args = params.split("_");
        if (args.length < 2) {
            return null;
        }
        if (args[0].equalsIgnoreCase("hascooldown")) {
            Channel channel = rtpManager.getChannelByName(args[1]);
            return getBooleanPlaceholder(rtpManager.hasCooldown(channel, player));
        }
        if (args[0].equalsIgnoreCase("cooldown")) {
            Channel channel = rtpManager.getChannelByName(args[1]);
            if (!rtpManager.hasCooldown(channel, player)) {
                return Config.papi_nocooldown;
            }
            int cooldown = (int) (rtpManager.getChannelCooldown(player, channel.getCooldown()) - (System.currentTimeMillis() - channel.getPlayerCooldowns().get(player.getName())) / 1000);
            if (args.length < 3) {
                return Utils.getTime(cooldown);
            }
            return switch (args[2]) {
                case "hours" -> Integer.toString(Utils.getHours(cooldown));
                case "minutes" -> Integer.toString(Utils.getMinutes(cooldown));
                case "seconds" -> Integer.toString(Utils.getSeconds(cooldown));
                default -> null;
            };
        }
        return null;
    }

    public String getBooleanPlaceholder(boolean b) {
        return b ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse(); // why...
    }
}
