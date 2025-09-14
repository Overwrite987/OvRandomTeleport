package ru.overwrite.rtp.channels;

import org.jetbrains.annotations.NotNull;
import ru.overwrite.rtp.channels.settings.Messages;

import java.util.List;

public record Channel(
        String id,
        String name,
        ChannelType type,
        List<String> activeWorlds,
        boolean teleportToFirstAllowedWorld,
        String serverToMove,
        int minPlayersToUse,
        int invulnerableTicks,
        boolean allowInCommand,
        boolean bypassMaxTeleportLimit,
        @NotNull Settings settings,
        @NotNull Messages messages) {
}
