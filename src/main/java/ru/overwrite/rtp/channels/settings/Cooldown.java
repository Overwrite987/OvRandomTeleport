package ru.overwrite.rtp.channels.settings;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.bukkit.entity.Player;
import ru.overwrite.rtp.utils.TimedExpiringMap;

public record Cooldown(
        int defaultCooldown,
        TimedExpiringMap<String, Long> playerCooldowns,
        Object2IntMap<String> groupCooldowns,
        boolean useLastGroupCooldown,
        int teleportCooldown) {

    public boolean hasCooldown(Player p) {
        return playerCooldowns != null && playerCooldowns.containsKey(p.getName());
    }
}
