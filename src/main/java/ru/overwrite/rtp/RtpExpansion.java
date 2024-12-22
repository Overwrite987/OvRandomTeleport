package ru.overwrite.rtp;

import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.channels.settings.Cooldown;
import ru.overwrite.rtp.channels.settings.Costs;
import ru.overwrite.rtp.configuration.Config;
import ru.overwrite.rtp.utils.Utils;

public class RtpExpansion extends PlaceholderExpansion {

    private final RtpManager rtpManager;
    private final Config pluginConfig;

    public RtpExpansion(Main plugin) {
        this.rtpManager = plugin.getRtpManager();
        this.pluginConfig = plugin.getPluginConfig();
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

        final String[] args = params.split("_");
        if (args.length < 2) {
            return null;
        }

        final String placeholderType = args[0].toLowerCase();
        final Channel channel = rtpManager.getChannelById(args[1]);
        final Cooldown channelCooldown = channel.cooldown();

        return switch (placeholderType) {
            case "hascooldown" -> getBooleanPlaceholder(channelCooldown.hasCooldown(player));
            case "cooldown" -> getCooldownValue(player, args, channelCooldown);
            case "settings" -> getSettingValue(player, channel, args);
            default -> null;
        };
    }

    private String getSettingValue(Player player, Channel channel, String[] args) {
        if (args.length < 3) {
            return null;
        }
        String settingName = args[2].toLowerCase();
        return switch (settingName) {
            case "name" -> channel.name();
            case "type" -> channel.type().toString();
            case "playersrequired" -> Integer.toString(channel.minPlayersToUse());
            case "cost" -> getCostValue(channel, args);
            case "cooldown" -> getCooldownValue(player, channel, args);
            default -> null;
        };
    }

    private boolean isPlayerValid(Player player) {
        return player != null && player.isOnline();
    }

    private String getCooldownValue(Player player, String[] args, Cooldown channelCooldown) {
        if (!channelCooldown.hasCooldown(player)) {
            return pluginConfig.getPlaceholderMessages().noCooldown();
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
        if (args.length < 4) {
            return null;
        }
        Costs costs = channel.costs();
        String costIdentifier = args[3];
        return switch (costIdentifier) {
            case "money" -> getValueIfPositiveOrDefault(costs.moneyCost());
            case "hunger" -> getValueIfPositiveOrDefault(costs.hungerCost());
            case "exp" -> getValueIfPositiveOrDefault(costs.expCost());
            default -> null;
        };
    }

    private String getCooldownValue(Player player, Channel channel, String[] args) {
        if (args.length < 4) {
            return null;
        }
        Cooldown cooldown = channel.cooldown();
        String cooldownIdentifier = args[3];
        return switch (cooldownIdentifier) {
            case "default" -> getValueIfPositiveOrDefault(cooldown.defaultCooldown());
            case "byplayergroup" -> isPlayerValid(player) ?
                    (args.length == 5 && args[4].equalsIgnoreCase("formatted") ?
                            Utils.getTime(rtpManager.getChannelCooldown(player, cooldown)) :
                            getValueIfPositiveOrDefault(rtpManager.getChannelCooldown(player, cooldown))) :
                    null;
            default -> null;
        };
    }

    private String getValueIfPositiveOrDefault(int value) {
        return value > 0 ? Integer.toString(value) : pluginConfig.getPlaceholderMessages().noValue();
    }

    private String getValueIfPositiveOrDefault(double value) {
        return value > 0 ? Double.toString(value) : pluginConfig.getPlaceholderMessages().noValue();
    }

    public String getBooleanPlaceholder(boolean b) {
        return b ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
    }
}
