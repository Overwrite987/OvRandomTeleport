package ru.overwrite.rtp.actions.impl;

import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.overwrite.rtp.Main;
import ru.overwrite.rtp.actions.Action;
import ru.overwrite.rtp.actions.ActionType;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.utils.Utils;

public class TitleActionType implements ActionType {

    private static final Key KEY = Key.key("ovrandomteleport:title");

    private static final int TITLE_INDEX = 0;
    private static final int SUBTITLE_INDEX = 1;
    private static final int FADE_IN_INDEX = 2;
    private static final int STAY_INDEX = 3;
    private static final int FADE_OUT_INDEX = 4;

    @Override
    public @NotNull Action instance(@NotNull String context, @NotNull Main plugin) {
        String[] titleMessages = context.split(";");
        int length = titleMessages.length;

        return new TitleAction(
                Utils.COLORIZER.colorize(titleMessages[TITLE_INDEX]),
                (length > SUBTITLE_INDEX) ? Utils.COLORIZER.colorize(titleMessages[SUBTITLE_INDEX]) : "",
                (length > FADE_IN_INDEX) ? Integer.parseInt(titleMessages[FADE_IN_INDEX]) : 10,
                (length > STAY_INDEX) ? Integer.parseInt(titleMessages[STAY_INDEX]) : 70,
                (length > FADE_OUT_INDEX) ? Integer.parseInt(titleMessages[FADE_OUT_INDEX]) : 20
        );
    }

    @Override
    public @NotNull Key key() {
        return KEY;
    }

    private record TitleAction(
            @NotNull String title,
            @NotNull String subtitle,
            int fadeIn,
            int stay,
            int fadeOut
    ) implements Action {
        @Override
        public void perform(@NotNull Channel channel, @NotNull Player player, @NotNull String[] searchList, @NotNull String[] replacementList) {
            player.sendTitle(
                    Utils.replaceEach(title, searchList, replacementList),
                    Utils.replaceEach(subtitle, searchList, replacementList),
                    fadeIn, stay, fadeOut
            );
        }
    }
}