package ru.overwrite.rtp.channels;

import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import ru.overwrite.rtp.utils.ExpiringMap;

import org.bukkit.World;

@Getter
public class Channel {

    private final ExpiringMap<String, Long> playerCooldowns;

    private final String id;

    private final String name;

    private final ChannelType type;

    private final List<World> activeWorlds;

    private final boolean teleportToFirstAllowedWorld;

    private final int minPlayersToUse;

    private final double teleportCost;

    private final LocationGenOptions locationGenOptions;

    private final int invulnerableTicks;

    private final int cooldown;

    private final int teleportCooldown;

    private final BossBar bossBar;

    private final Restrictions restrictions;

    private final Avoidance avoidance;

    private final Actions actions;

    private final Messages messages;

    public Channel(String id,
                   String name,
                   ChannelType type,
                   List<World> activeWorlds,
                   boolean teleportToFirstAllowedWorld,
                   int minPlayersToUse,
                   double teleportCost,
                   LocationGenOptions locationGenOptions,
                   int invulnerableTicks,
                   int cooldown,
                   int teleportCooldown,
                   BossBar bossBar,
                   Restrictions restrictions,
                   Avoidance avoidance,
                   Actions actions,
                   Messages messages) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.activeWorlds = activeWorlds;
        this.teleportToFirstAllowedWorld = teleportToFirstAllowedWorld;
        this.minPlayersToUse = minPlayersToUse;
        this.teleportCost = teleportCost;
        this.locationGenOptions = locationGenOptions;
        this.invulnerableTicks = invulnerableTicks;
        this.cooldown = cooldown;
        this.playerCooldowns = new ExpiringMap<>(cooldown, TimeUnit.SECONDS);
        this.teleportCooldown = teleportCooldown;
        this.bossBar = bossBar;
        this.restrictions = restrictions;
        this.avoidance = avoidance;
        this.actions = actions;
        this.messages = messages;
    }

}
