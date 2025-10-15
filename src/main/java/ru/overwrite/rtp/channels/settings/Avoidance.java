package ru.overwrite.rtp.channels.settings;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import ru.overwrite.rtp.utils.VersionUtils;

import java.util.*;

public record Avoidance(
        boolean avoidBlocksBlacklist,
        Set<Material> avoidBlocks,
        boolean avoidBiomesBlacklist,
        Set<Biome> avoidBiomes,
        boolean avoidRegions,
        boolean avoidTowns
) {

    private static final Avoidance EMPTY_AVOIDANCE = new Avoidance(
            false,
            Set.of(),
            false,
            Set.of(),
            false,
            false
    );

    public static Avoidance create(ConfigurationSection avoidance) {
        if (avoidance == null) {
            return EMPTY_AVOIDANCE;
        }

        boolean avoidBlocksBlacklist = false;
        Set<Material> avoidBlocks = Set.of();

        boolean avoidBiomesBlacklist = false;
        Set<Biome> avoidBiomes = Set.of();


        ConfigurationSection blocksSection = avoidance.getConfigurationSection("blocks");
        boolean isNullBlocksSection = blocksSection == null;
        if (!isNullBlocksSection) {
            avoidBlocksBlacklist = blocksSection.getBoolean("blacklist", false);
            if (blocksSection.contains("list")) {
                avoidBlocks = createMaterialSet(blocksSection.getStringList("list"));
            }
        }
        ConfigurationSection biomesSection = avoidance.getConfigurationSection("biomes");
        boolean isNullBiomesSection = biomesSection == null;
        if (!isNullBiomesSection) {
            avoidBiomesBlacklist = biomesSection.getBoolean("blacklist", false);
            if (biomesSection.contains("list")) {
                avoidBiomes = createBiomeSet(biomesSection.getStringList("list"));
            }
        }

        boolean avoidRegions = parsePluginRelatedAvoidance(avoidance, "regions", "WorldGuard");
        boolean avoidTowns = parsePluginRelatedAvoidance(avoidance, "towns", "Towny");

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

    private static boolean parsePluginRelatedAvoidance(ConfigurationSection section, String key, String pluginName) {
        return Bukkit.getPluginManager().isPluginEnabled(pluginName) && section.getBoolean(key, false);
    }
}