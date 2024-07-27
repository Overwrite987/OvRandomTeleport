package ru.overwrite.rtp.channels;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;

public record Cooldown(
        int defaultCooldown,
        Object2IntLinkedOpenHashMap<String> groupCooldowns,
        boolean useLastGroupCooldown,
        int teleportCooldown) {
}
