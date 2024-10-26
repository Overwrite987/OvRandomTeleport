package ru.overwrite.rtp.channels;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.overwrite.rtp.channels.settings.*;

import org.bukkit.World;

public record Channel(
        String id,
        String name,
        ChannelType type,
        List<World> activeWorlds,
        boolean teleportToFirstAllowedWorld,
        int minPlayersToUse,
        @Nullable Costs costs,
        LocationGenOptions locationGenOptions,
        int invulnerableTicks,
        @NotNull Cooldown cooldown,
        @NotNull BossBar bossBar,
        @NotNull Particles particles,
        @NotNull Restrictions restrictions,
        @NotNull Avoidance avoidance,
        @NotNull Actions actions,
        @NotNull Messages messages) {

}
