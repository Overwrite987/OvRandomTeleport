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

    private static final Avoidance EMPTY_AVOIDANCE = new Avoidance(
            false,
            Set.of(),
            false,
            Set.of(),
            false,
            false
    );

    public static Avoidance create(ConfigurationSection avoidance, Settings template, Config pluginConfig, boolean applyTemplate) {

        boolean isNullSection = pluginConfig.isNullSection(avoidance);

        Avoidance templateAvoidance = applyTemplate && template != null ? template.avoidance() : null;
        boolean hasTemplateAvoidance = templateAvoidance != null;

        if (isNullSection) {
            if (!applyTemplate) {
                return null;
            }
            if (!hasTemplateAvoidance) {
                return EMPTY_AVOIDANCE;
            }
        }

        boolean avoidBlocksBlacklist = hasTemplateAvoidance && templateAvoidance.avoidBlocksBlacklist();
        Set<Material> avoidBlocks = hasTemplateAvoidance ? templateAvoidance.avoidBlocks() : Set.of();

        boolean avoidBiomesBlacklist = hasTemplateAvoidance && templateAvoidance.avoidBiomesBlacklist();
        Set<Biome> avoidBiomes = hasTemplateAvoidance ? templateAvoidance.avoidBiomes() : Set.of();

        boolean avoidRegions = hasTemplateAvoidance && templateAvoidance.avoidRegions();
        boolean avoidTowns = hasTemplateAvoidance && templateAvoidance.avoidTowns();

        if (!isNullSection) {
            ConfigurationSection blocksSection = avoidance.getConfigurationSection("blocks");
            boolean isNullBlocksSection = pluginConfig.isNullSection(blocksSection);
            if (!isNullBlocksSection) {
                avoidBlocksBlacklist = blocksSection.getBoolean("blacklist", avoidBlocksBlacklist);
                if (blocksSection.contains("list")) {
                    avoidBlocks = createMaterialSet(blocksSection.getStringList("list"));
                }
            }
            ConfigurationSection biomesSection = avoidance.getConfigurationSection("biomes");
            boolean isNullBiomesSection = pluginConfig.isNullSection(biomesSection);
            if (!isNullBiomesSection) {
                avoidBiomesBlacklist = biomesSection.getBoolean("blacklist", avoidBiomesBlacklist);
                if (biomesSection.contains("list")) {
                    avoidBiomes = createBiomeSet(biomesSection.getStringList("list"));
                }
            }
            avoidRegions = parsePluginRelatedAvoidance(avoidance, "regions", avoidRegions, "WorldGuard", isNullSection);
            avoidTowns = parsePluginRelatedAvoidance(avoidance, "towns", avoidTowns, "Towny", isNullSection);
        }

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
        Set<Biome> biomeSet = VersionUtils.SUB_VERSION > 20 ? new HashSet<>() : EnumSet.noneOf(Biome.class);
        for (String biome : stringList) {
            biomeSet.add(Biome.valueOf(biome.toUpperCase(Locale.ENGLISH)));
        }
        return biomeSet;
    }

    private static boolean parsePluginRelatedAvoidance(ConfigurationSection section, String key, boolean templateValue, String pluginName, boolean isNullSection) {
        return Bukkit.getPluginManager().isPluginEnabled(pluginName) && !isNullSection
                ? section.getBoolean(key, templateValue)
                : templateValue;
    }
}
