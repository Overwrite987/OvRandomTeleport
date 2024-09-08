package ru.overwrite.rtp;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.utils.Config;
import ru.overwrite.rtp.utils.Utils;

public class RtpExpansion extends PlaceholderExpansion {

    private final Main plugin;
    private final RtpManager rtpManager;

    public RtpExpansion(Main plugin) {
        this.plugin = plugin;
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
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        Player p = player.getPlayer();
        if (p == null) {
            return "";
        }
        String[] args = params.split("_"); // will also be useful in the future
        if (args[0].equalsIgnoreCase("cooldown")) {
            Channel channel = rtpManager.getChannelByName(args[1]);
            if (!rtpManager.hasCooldown(channel, p)) {
                return Config.papi_nocooldown;
            }
            int cooldown = (int) (rtpManager.getChannelCooldown(p, channel.getCooldown()) - (System.currentTimeMillis() - channel.getPlayerCooldowns().get(p.getName())) / 1000);
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
}
