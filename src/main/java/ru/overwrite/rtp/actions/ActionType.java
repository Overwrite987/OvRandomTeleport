package ru.overwrite.rtp.actions;

import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.NotNull;
import ru.overwrite.rtp.Main;

public interface ActionType extends Keyed {
    @NotNull Action instance(@NotNull String context, @NotNull Main rtpPlugin);
}
