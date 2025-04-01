package ru.overwrite.rtp.actions;

import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.NotNull;
import ru.overwrite.rtp.OvRandomTeleport;

public interface ActionType extends Keyed {

    @NotNull Action instance(@NotNull String context, @NotNull OvRandomTeleport plugin);

}
