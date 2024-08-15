package ru.overwrite.rtp.actions.impl;

import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.overwrite.rtp.Main;
import ru.overwrite.rtp.actions.ActionInstance;
import ru.overwrite.rtp.actions.ActionType;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.utils.Config;
import ru.overwrite.rtp.utils.Utils;

import java.util.function.UnaryOperator;

public class MessageAction implements ActionType {
    private static final Key KEY = Key.key("ovrandomteleport:message");

    @Override
    public @NotNull ActionInstance instance(@NotNull String context, @NotNull Main rtpPlugin) {
        return new Instance(Utils.colorize(context, Config.serializer));
    }

    @Override
    public @NotNull Key key() {
        return KEY;
    }

    private record Instance(@NotNull String message) implements ActionInstance {
        @Override
        public void perform(@NotNull Channel channel, @NotNull Player player, @NotNull UnaryOperator<String> placeholders) {
            player.sendMessage(Utils.replacePlaceholders(message, placeholders));
        }
    }
}
