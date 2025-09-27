package ru.overwrite.rtp.channels.settings;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMaps;
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
            Object2IntSortedMaps.emptyMap(),
            0,
            Object2IntSortedMaps.emptyMap()
    );

    public static Cooldown create(OvRandomTeleport plugin, ConfigurationSection cooldown, Settings template, Config pluginConfig, boolean applyTemplate) {

        boolean isNullSection = pluginConfig.isNullSection(cooldown);

        Cooldown templateCooldown = template != null ? template.cooldown() : null;
        boolean hasTemplateCooldown = templateCooldown != null;

        if (isNullSection) {
            if (!applyTemplate) {
                return null;
            }
            if (!hasTemplateCooldown) {
                return EMPTY_COOLDOWN;
            }
        }

        int defaultCooldown = hasTemplateCooldown ? templateCooldown.defaultCooldown() : -1;
        int defaultPreTeleportCooldown = hasTemplateCooldown ? templateCooldown.defaultPreTeleportCooldown() : -1;

        Object2IntSortedMap<String> groupCooldownsMap = hasTemplateCooldown ? templateCooldown.groupCooldowns() : new Object2IntLinkedOpenHashMap<>();
        Object2IntSortedMap<String> preTeleportCooldownsMap = hasTemplateCooldown ? templateCooldown.preTeleportCooldowns() : new Object2IntLinkedOpenHashMap<>();

        boolean useLastGroupCooldown = cooldown.getBoolean("use_last_group_cooldown", false);

        if (!isNullSection) {
            defaultCooldown = cooldown.getInt("default_cooldown", defaultCooldown);
            ConfigurationSection groupCooldownsSection = cooldown.getConfigurationSection("group_cooldowns");
            if (!pluginConfig.isNullSection(groupCooldownsSection)) {
                defaultCooldown = processCooldownSection(plugin, groupCooldownsSection, groupCooldownsMap, useLastGroupCooldown, defaultCooldown);
            }

            defaultPreTeleportCooldown = cooldown.getInt("default_pre_teleport_cooldown", defaultPreTeleportCooldown);
            ConfigurationSection preTeleportGroupCooldownsSection = cooldown.getConfigurationSection("pre_teleport_group_cooldowns");
            if (!pluginConfig.isNullSection(preTeleportGroupCooldownsSection)) {
                defaultPreTeleportCooldown = processCooldownSection(plugin, preTeleportGroupCooldownsSection, preTeleportCooldownsMap, useLastGroupCooldown, defaultPreTeleportCooldown);
            }
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
