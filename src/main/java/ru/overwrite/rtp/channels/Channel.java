package ru.overwrite.rtp.channels;

import java.util.List;

import ru.overwrite.rtp.channels.settings.*;

import org.bukkit.World;

public record Channel(
        String id,
        String name,
        ChannelType type,
        List<World> activeWorlds,
        boolean teleportToFirstAllowedWorld,
        int minPlayersToUse,
        Costs costs,
        LocationGenOptions locationGenOptions,
        int invulnerableTicks,
        Cooldown cooldown,
        BossBar bossBar,
        Particles particles,
        Restrictions restrictions,
        Avoidance avoidance,
        Actions actions,
        Messages messages) {

}
