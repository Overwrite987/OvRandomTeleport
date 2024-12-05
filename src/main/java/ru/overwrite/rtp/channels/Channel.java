package ru.overwrite.rtp.channels;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import ru.overwrite.rtp.channels.settings.*;

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
        @NotNull Costs costs,
        @NotNull LocationGenOptions locationGenOptions,
        @NotNull Cooldown cooldown,
        @NotNull Bossbar bossbar,
        @NotNull Particles particles,
        @NotNull Restrictions restrictions,
        @NotNull Avoidance avoidance,
        @NotNull Actions actions,
        @NotNull Messages messages) {

}
