package ru.overwrite.rtp.channels;

import org.bukkit.Material;
import org.bukkit.block.Biome;

import java.util.Set;

public record Avoidance(
        boolean avoidBlocksBlacklist,
        Set<Material> avoidBlocks,
        boolean avoidBiomesBlacklist,
        Set<Biome> avoidBiomes,
        boolean avoidRegions,
        boolean avoidTowns) {
}
