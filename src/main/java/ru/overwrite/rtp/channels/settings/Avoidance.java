package ru.overwrite.rtp.channels.settings;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.bukkit.Material;
import org.bukkit.block.Biome;

public record Avoidance(
        boolean avoidBlocksBlacklist,
        ObjectSet<Material> avoidBlocks,
        boolean avoidBiomesBlacklist,
        ObjectSet<Biome> avoidBiomes,
        boolean avoidRegions,
        boolean avoidTowns) {
}
