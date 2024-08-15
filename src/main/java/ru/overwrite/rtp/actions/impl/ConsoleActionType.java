package ru.overwrite.rtp.actions.impl;

import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.overwrite.rtp.Main;
import ru.overwrite.rtp.actions.Action;
import ru.overwrite.rtp.actions.ActionType;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.utils.Utils;

import java.util.function.UnaryOperator;

public class ConsoleActionType implements ActionType {
    private static final Key KEY = Key.key("ovrandomteleport:console");

    @Override
    public @NotNull Action instance(@NotNull String context, @NotNull Main plugin) {
        return new Instance(context);
    }

    @Override
    public @NotNull Key key() {
        return KEY;
    }

    private record Instance(@NotNull String command) implements Action {
        @Override
        public void perform(@NotNull Channel channel, @NotNull Player player, @NotNull UnaryOperator<String> placeholders) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Utils.replacePlaceholders(command, placeholders));
        }
    }
}
