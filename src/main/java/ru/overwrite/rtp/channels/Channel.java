package ru.overwrite.rtp.channels;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import ru.overwrite.rtp.channels.settings.*;

import org.bukkit.World;

public record Channel(
        String id,
        String name,
        ChannelType type,
        List<World> activeWorlds,
        boolean teleportToFirstAllowedWorld,
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
