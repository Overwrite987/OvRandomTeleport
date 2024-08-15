package ru.overwrite.rtp.actions;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.overwrite.rtp.channels.Channel;

import java.util.function.UnaryOperator;

public interface Action {

    void perform(@NotNull Channel channel, @NotNull Player player, @NotNull UnaryOperator<String> placeholders);

}
