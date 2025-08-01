package ru.overwrite.rtp.animations;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import ru.overwrite.rtp.channels.settings.Particles;

public abstract class Animation extends BukkitRunnable {

    protected Player player;
    protected int duration;
    protected Particles particles;

    protected Animation(Player player, int duration, Particles particles) {
        this.player = player;
        this.duration = duration;
        this.particles = particles;
    }

}
