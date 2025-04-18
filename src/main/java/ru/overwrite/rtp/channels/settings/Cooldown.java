package ru.overwrite.rtp.channels.settings;

import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import org.bukkit.entity.Player;
import ru.overwrite.rtp.utils.TimedExpiringMap;

public record Cooldown(
        int defaultCooldown,
        TimedExpiringMap<String, Long> playerCooldowns,
        Object2IntSortedMap<String> groupCooldowns,
        int defaultPreTeleportCooldown,
        Object2IntSortedMap<String> preTeleportCooldowns) {

    public boolean hasCooldown(Player player) {
        return playerCooldowns != null && !playerCooldowns.isEmpty() && playerCooldowns.containsKey(player.getName());
    }

    public void setCooldown(String name, long cooldownTime) {
        playerCooldowns.put(name, System.currentTimeMillis(), cooldownTime);
    }
}
