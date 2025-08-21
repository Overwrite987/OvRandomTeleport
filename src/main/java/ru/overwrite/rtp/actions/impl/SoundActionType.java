package ru.overwrite.rtp.actions.impl;

import net.kyori.adventure.key.Key;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.overwrite.rtp.OvRandomTeleport;
import ru.overwrite.rtp.actions.Action;
import ru.overwrite.rtp.actions.ActionType;

import java.util.Locale;

public final class SoundActionType implements ActionType {

    private static final Key KEY = Key.key("ovrandomteleport:sound");

    private static final int SOUND_INDEX = 0;
    private static final int VOLUME_INDEX = 1;
    private static final int PITCH_INDEX = 2;

    @Override
    public @NotNull Action instance(@NotNull String context, @NotNull OvRandomTeleport plugin) {
        String[] soundArgs = context.split(";");
        int length = soundArgs.length;

        return new SoundAction(
                Sound.valueOf(soundArgs[SOUND_INDEX].toUpperCase(Locale.ENGLISH)).key().toString(),
                (length > VOLUME_INDEX) ? Float.parseFloat(soundArgs[VOLUME_INDEX]) : 1.0F,
                (length > PITCH_INDEX) ? Float.parseFloat(soundArgs[PITCH_INDEX]) : 1.0F
        );
    }

    @Override
    public @NotNull Key key() {
        return KEY;
    }

    private record SoundAction(
            @NotNull String sound,
            float volume,
            float pitch
    ) implements Action {
        @Override
        public void perform(@NotNull Player player, @NotNull String[] searchList, @NotNull String[] replacementList) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }
}