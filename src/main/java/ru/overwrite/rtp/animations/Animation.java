package ru.overwrite.rtp.animations;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import ru.overwrite.rtp.channels.settings.Particles;

import java.util.List;

public abstract class Animation extends BukkitRunnable {

    protected final Player player;
    protected final int duration;
    protected final Particles particles;

    protected final List<Player> receivers;

    protected int tickCounter;

    protected Animation(Player player, int duration, Particles particles) {
        this.player = player;
        this.duration = duration;
        this.particles = particles;

        this.receivers = particles.preTeleport().sendOnlyToPlayer() ? List.of(player) : null;
    }

}
