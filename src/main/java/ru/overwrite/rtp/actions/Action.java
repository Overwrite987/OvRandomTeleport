package ru.overwrite.rtp.actions;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface Action {

    void perform(@NotNull Player player, @NotNull String[] searchList, @NotNull String[] replacementList);

}
