package ru.overwrite.rtp.channels.settings;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import ru.overwrite.rtp.channels.Settings;
import ru.overwrite.rtp.configuration.Config;
import ru.overwrite.rtp.utils.VersionUtils;

import java.util.*;

public record Avoidance(
        boolean avoidBlocksBlacklist,
        Set<Material> avoidBlocks,
        boolean avoidBiomesBlacklist,
        Set<Biome> avoidBiomes,
        boolean avoidRegions,
        boolean avoidTowns) {

    public static Avoidance create(ConfigurationSection avoidSection, Settings template, Config pluginConfig, boolean applyTemplate) {
        if (pluginConfig.isNullSection(avoidSection) && !applyTemplate) {
            return null;
        }

        Avoidance templateAvoidance = template != null ? template.avoidance() : null;
        boolean hasTemplateAvoidance = templateAvoidance != null;

        ConfigurationSection blocksSection = avoidSection.getConfigurationSection("blocks");
        boolean avoidBlocksBlacklist = pluginConfig.isNullSection(blocksSection)
                ? hasTemplateAvoidance && templateAvoidance.avoidBlocksBlacklist()
                : blocksSection.getBoolean("blacklist", hasTemplateAvoidance && templateAvoidance.avoidBlocksBlacklist());

        Set<Material> avoidBlocks = hasTemplateAvoidance ? templateAvoidance.avoidBlocks() : Set.of();
        if (!pluginConfig.isNullSection(blocksSection) && blocksSection.contains("list")) {
            avoidBlocks = createMaterialSet(blocksSection.getStringList("list"));
        }

        ConfigurationSection biomesSection = avoidSection.getConfigurationSection("biomes");
        boolean avoidBiomesBlacklist = pluginConfig.isNullSection(biomesSection)
                ? hasTemplateAvoidance && templateAvoidance.avoidBiomesBlacklist()
                : biomesSection.getBoolean("blacklist", hasTemplateAvoidance && templateAvoidance.avoidBiomesBlacklist());

        Set<Biome> avoidBiomes = hasTemplateAvoidance ? templateAvoidance.avoidBiomes() : Set.of();
        if (!pluginConfig.isNullSection(biomesSection) && biomesSection.contains("list")) {
            avoidBiomes = createBiomeSet(biomesSection.getStringList("list"));
        }

        boolean avoidRegions = parsePluginRelatedAvoidance(
                avoidSection, "regions",
                hasTemplateAvoidance && templateAvoidance.avoidRegions(),
                "WorldGuard"
        );

        boolean avoidTowns = parsePluginRelatedAvoidance(
                avoidSection, "towns",
                hasTemplateAvoidance && templateAvoidance.avoidTowns(),
                "Towny"
        );

        return new Avoidance(avoidBlocksBlacklist, avoidBlocks, avoidBiomesBlacklist, avoidBiomes, avoidRegions, avoidTowns);
    }

    private static Set<Material> createMaterialSet(List<String> stringList) {
        Set<Material> materialSet = EnumSet.noneOf(Material.class);
        for (String material : stringList) {
            materialSet.add(Material.valueOf(material.toUpperCase(Locale.ENGLISH)));
        }
        return materialSet;
    }

    private static Set<Biome> createBiomeSet(List<String> stringList) {
        Set<Biome> biomeSet = VersionUtils.SUB_VERSION > 20
                ? new HashSet<>()
                : EnumSet.noneOf(Biome.class);
        for (String biome : stringList) {
            biomeSet.add(Biome.valueOf(biome.toUpperCase(Locale.ENGLISH)));
        }
        return biomeSet;
    }

    private static boolean parsePluginRelatedAvoidance(ConfigurationSection section, String key, boolean templateValue, String pluginName) {
        return section.getBoolean(key, templateValue) && Bukkit.getPluginManager().isPluginEnabled(pluginName);
    }
}
