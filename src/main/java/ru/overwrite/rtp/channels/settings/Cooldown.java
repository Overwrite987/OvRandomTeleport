package ru.overwrite.rtp.channels.settings;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import ru.overwrite.rtp.OvRandomTeleport;
import ru.overwrite.rtp.channels.Settings;
import ru.overwrite.rtp.configuration.Config;
import ru.overwrite.rtp.utils.TimedExpiringMap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public record Cooldown(
        int defaultCooldown,
        TimedExpiringMap<String, Long> playerCooldowns,
        Object2IntSortedMap<String> groupCooldowns,
        int defaultPreTeleportCooldown,
        Object2IntSortedMap<String> preTeleportCooldowns) {

    private static final Cooldown EMPTY_COOLDOWN = new Cooldown(
            0,
            null,
            null,
            0,
            null
    );

    public static Cooldown create(OvRandomTeleport plugin, ConfigurationSection cooldown, Settings template, Config pluginConfig, boolean applyTemplate) {
        if (pluginConfig.isNullSection(cooldown)) {
            if (!applyTemplate) {
                return null;
            }
            return EMPTY_COOLDOWN;
        }

        Cooldown templateCooldown = template != null ? template.cooldown() : null;
        boolean hasTemplateCooldown = templateCooldown != null;

        int defaultCooldown = cooldown.getInt("default_cooldown",
                hasTemplateCooldown ? templateCooldown.defaultCooldown() : -1);

        TimedExpiringMap<String, Long> playerCooldowns =
                defaultCooldown > 0 ? new TimedExpiringMap<>(TimeUnit.SECONDS) : null;

        Object2IntSortedMap<String> groupCooldownsMap = new Object2IntLinkedOpenHashMap<>();
        Object2IntSortedMap<String> preTeleportCooldownsMap = new Object2IntLinkedOpenHashMap<>();

        boolean useLastGroupCooldown = cooldown.getBoolean("use_last_group_cooldown", false);

        ConfigurationSection groupCooldowns = cooldown.getConfigurationSection("group_cooldowns");
        if (!pluginConfig.isNullSection(groupCooldowns)) {
            defaultCooldown = processCooldownSection(plugin, groupCooldowns, groupCooldownsMap, useLastGroupCooldown, defaultCooldown);
        } else if (hasTemplateCooldown) {
            groupCooldownsMap = templateCooldown.groupCooldowns();
        }

        int defaultPreTeleportCooldown = cooldown.getInt("default_pre_teleport_cooldown",
                hasTemplateCooldown ? templateCooldown.defaultPreTeleportCooldown() : -1);

        ConfigurationSection preTeleportGroupCooldowns = cooldown.getConfigurationSection("pre_teleport_group_cooldowns");
        if (!pluginConfig.isNullSection(preTeleportGroupCooldowns)) {
            defaultPreTeleportCooldown = processCooldownSection(plugin, preTeleportGroupCooldowns, preTeleportCooldownsMap, useLastGroupCooldown, defaultPreTeleportCooldown);
        } else if (hasTemplateCooldown) {
            preTeleportCooldownsMap = templateCooldown.preTeleportCooldowns();
        }

        return new Cooldown(defaultCooldown, playerCooldowns,
                groupCooldownsMap, defaultPreTeleportCooldown, preTeleportCooldownsMap);
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
