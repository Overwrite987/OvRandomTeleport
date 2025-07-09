package ru.overwrite.rtp.actions.impl;

import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.overwrite.rtp.OvRandomTeleport;
import ru.overwrite.rtp.actions.Action;
import ru.overwrite.rtp.actions.ActionType;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.utils.Utils;

public final class PlayerActionType implements ActionType {

    private static final Key KEY = Key.key("ovrandomteleport:player");

    @Override
    public @NotNull Action instance(@NotNull String context, @NotNull OvRandomTeleport plugin) {
        return new PlayerAction(context);
    }

    @Override
    public @NotNull Key key() {
        return KEY;
    }

    private record PlayerAction(@NotNull String command) implements Action {
        @Override
        public void perform(@NotNull Channel channel, @NotNull Player player, @NotNull String[] searchList, @NotNull String[] replacementList) {
            player.chat("/" + command);
        }
    }
}
