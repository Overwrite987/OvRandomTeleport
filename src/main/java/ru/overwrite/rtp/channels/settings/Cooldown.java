package ru.overwrite.rtp.channels.settings;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMaps;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import ru.overwrite.rtp.OvRandomTeleport;
import ru.overwrite.rtp.utils.TimedExpiringMap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public record Cooldown(
        int defaultCooldown,
        TimedExpiringMap<String, Long> playerCooldowns,
        Object2IntSortedMap<String> groupCooldowns,
        int defaultPreTeleportCooldown,
        Object2IntSortedMap<String> preTeleportCooldowns
) {

    private static final Cooldown EMPTY_COOLDOWN = new Cooldown(
            0,
            null,
            Object2IntSortedMaps.emptyMap(),
            0,
            Object2IntSortedMaps.emptyMap()
    );

    public static Cooldown create(OvRandomTeleport plugin, ConfigurationSection cooldown) {
        if (cooldown == null) {
            return EMPTY_COOLDOWN;
        }

        Object2IntSortedMap<String> groupCooldownsMap = new Object2IntLinkedOpenHashMap<>();
        Object2IntSortedMap<String> preTeleportCooldownsMap = new Object2IntLinkedOpenHashMap<>();

        boolean useLastGroupCooldown = cooldown.getBoolean("use_last_group_cooldown", false);

        int defaultCooldown = cooldown.getInt("default_cooldown", -1);
        ConfigurationSection groupCooldownsSection = cooldown.getConfigurationSection("group_cooldowns");
        if (groupCooldownsSection != null) {
            defaultCooldown = processCooldownSection(plugin, groupCooldownsSection, groupCooldownsMap, useLastGroupCooldown, defaultCooldown);
        }

        int defaultPreTeleportCooldown = cooldown.getInt("default_pre_teleport_cooldown", -1);
        ConfigurationSection preTeleportGroupCooldownsSection = cooldown.getConfigurationSection("pre_teleport_group_cooldowns");
        if (preTeleportGroupCooldownsSection != null) {
            defaultPreTeleportCooldown = processCooldownSection(plugin, preTeleportGroupCooldownsSection, preTeleportCooldownsMap, useLastGroupCooldown, defaultPreTeleportCooldown);
        }

        TimedExpiringMap<String, Long> playerCooldowns = defaultCooldown > 0 ? new TimedExpiringMap<>(TimeUnit.SECONDS) : null;

        return new Cooldown(defaultCooldown, playerCooldowns, groupCooldownsMap, defaultPreTeleportCooldown, preTeleportCooldownsMap);
    }

    private static int processCooldownSection(OvRandomTeleport plugin, ConfigurationSection section, Object2IntSortedMap<String> map, boolean useLastGroup, int currentDefault) {
        if (plugin.getPerms() != null) {
            for (String groupName : section.getKeys(false)) {
                map.put(groupName, section.getInt(groupName));
            }
            if (!map.isEmpty() && useLastGroup) {
                List<String> keys = new ArrayList<>(map.keySet());
                currentDefault = section.getInt(keys.get(keys.size() - 1));
            }
        }
        return currentDefault;
    }

    public boolean hasCooldown(Player player) {
        return playerCooldowns != null && !playerCooldowns.isEmpty() && playerCooldowns.containsKey(player.getName());
    }

    public void setCooldown(String name, long cooldownTime) {
        playerCooldowns.put(name, System.currentTimeMillis(), cooldownTime);
    }
}