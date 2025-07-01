package ru.overwrite.rtp.channels;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import ru.overwrite.rtp.channels.settings.Messages;

import java.util.List;

public record Channel(
        String id,
        String name,
        ChannelType type,
        List<World> activeWorlds,
        boolean teleportToFirstAllowedWorld,
        String serverToMove,
        int minPlayersToUse,
        int invulnerableTicks,
        boolean allowInCommand,
        @NotNull Settings settings,
        @NotNull Messages messages) {
}
