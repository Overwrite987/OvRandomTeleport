package ru.overwrite.rtp.actions.impl;

import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.overwrite.rtp.Main;
import ru.overwrite.rtp.actions.Action;
import ru.overwrite.rtp.actions.ActionType;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.utils.Utils;

public final class ActionBarActionType implements ActionType {

    private static final Key KEY = Key.key("ovrandomteleport:actionbar");

    @Override
    public @NotNull Action instance(@NotNull String context, @NotNull Main plugin) {
        return new ActionBarAction(context);
    }

    @Override
    public @NotNull Key key() {
        return KEY;
    }

    private record ActionBarAction(@NotNull String message) implements Action {
        @Override
        public void perform(@NotNull Channel channel, @NotNull Player player, @NotNull String[] searchList, @NotNull String[] replacementList) {
            player.sendActionBar(Utils.replaceEach(message, searchList, replacementList));
        }
    }
}
