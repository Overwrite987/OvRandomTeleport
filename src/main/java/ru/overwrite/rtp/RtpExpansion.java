package ru.overwrite.rtp;

import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.channels.settings.Cooldown;
import ru.overwrite.rtp.channels.settings.Costs;
import ru.overwrite.rtp.utils.Config;
import ru.overwrite.rtp.utils.Utils;

public class RtpExpansion extends PlaceholderExpansion {

    private final RtpManager rtpManager;
    private final Config.PlaceholderMessages placeholderMessages;

    public RtpExpansion(Main plugin) {
        this.rtpManager = plugin.getRtpManager();
        this.placeholderMessages = plugin.getPluginConfig().getPlaceholderMessages();
    }

    @Override
    public @NotNull String getAuthor() {
        return "OverwriteMC";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ovrtp";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (isPlayerInvalid(player)) {
            return "Player is not online! Can't parse placeholder";
        }

        final String[] args = params.split("_");
        if (args.length < 2) {
            return null;
        }

        final String placeholderType = args[0].toLowerCase();
        final Channel channel = rtpManager.getChannelByName(args[1]);
        final Cooldown channelCooldown = channel.cooldown();

        return switch (placeholderType) {
            case "hascooldown" -> getBooleanPlaceholder(channelCooldown.hasCooldown(player));
            case "cooldown" -> processCooldownPlaceholder(player, args, channelCooldown);
            case "cost" -> getCostValue(channel, args);
            default -> null;
        };
    }

    private boolean isPlayerInvalid(Player player) {
        return player == null || !player.isOnline();
    }

    private String processCooldownPlaceholder(Player player, String[] args, Cooldown channelCooldown) {
        if (!channelCooldown.hasCooldown(player)) {
            return placeholderMessages.noCooldown();
        }
        final int cooldown = calculateCooldown(player, channelCooldown);
        if (args.length < 3) {
            return Utils.getTime(cooldown);
        }
        return getCooldownTimeComponent(args[2], cooldown);
    }

    private int calculateCooldown(Player player, Cooldown channelCooldown) {
        final long playerCooldownStart = channelCooldown.playerCooldowns().get(player.getName());
        return (int) (rtpManager.getChannelCooldown(player, channelCooldown) - (System.currentTimeMillis() - playerCooldownStart) / 1000);
    }

    private String getCooldownTimeComponent(String timeUnit, int cooldown) {
        return switch (timeUnit) {
            case "hours" -> Integer.toString(Utils.getHours(cooldown));
            case "minutes" -> Integer.toString(Utils.getMinutes(cooldown));
            case "seconds" -> Integer.toString(Utils.getSeconds(cooldown));
            default -> null;
        };
    }

    private String getCostValue(Channel channel, String[] args) {
        if (args.length < 3) {
            return null;
        }
        Costs costs = channel.costs();
        if (costs == null) {
            return placeholderMessages.noValue();
        }
        final String costIdentifier = args[2];
        return switch (costIdentifier) {
            case "money" -> getOrDefaultValue(Double.toString(costs.moneyCost()));
            case "hunger" -> getOrDefaultValue(Integer.toString(costs.hungerCost()));
            case "exp" -> getOrDefaultValue(Double.toString(costs.expCost()));
            default -> null;
        };
    }

    private <T> String getOrDefaultValue(T value) {
        return value != null ? value.toString() : placeholderMessages.noValue();
    }

    public String getBooleanPlaceholder(boolean b) {
        return b ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse(); // why...
    }
}
