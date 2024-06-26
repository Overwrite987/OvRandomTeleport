package ru.overwrite.rtp.channels;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import ru.overwrite.rtp.actions.Action;
import ru.overwrite.rtp.utils.ExpiringMap;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

@Getter
public class Channel {
	
	private final ExpiringMap<String, Long> playerCooldowns;
	
	private final String id;
	
	private final String name;
	
	private final ChannelType type;
	
	private final List<World> activeWorlds;
	
	private final boolean teleportToFirstAllowedWorld;

	private final boolean teleportOnFirstJoin;
	
	private final boolean teleportOnVoid;

	private final boolean teleportOnRespawn;

	private final int minPlayersToUse;
	
	private final double teleportCost;
	
	private final String shape;
	
	private final int minX, maxX;
	
	private final int minZ, maxZ;

	private final int radiusMin, radiusMax;
	
	private final int maxLocationAttempts;
	
	private final int invulnerableTicks;
	
	private final int cooldown;
	
	private final int teleportCooldown;
	
	private final boolean bossbarEnabled;
	
	private final String bossbarTitle;
	
	private final BarColor bossbarColor;
	
	private final BarStyle bossbarType;
	
	private final boolean restrictMove;
	
	private final boolean restrictDamage;
	
	private final boolean avoidBlocksBlacklist;
	
	private final Set<Material> avoidBlocks;
	
	private final boolean avoidBiomesBlacklist;
	
	private final Set<Biome> avoidBiomes;
	
	private final boolean avoidRegions;
	
	private final boolean avoidTowns;

	private final ChannelActions channelActions;

	private final ChannelMessages channelMessages;
	
	public Channel(String id,
			String name,
			ChannelType type,
			List<World> activeWorlds,
			boolean teleportToFirstAllowedWorld,
			boolean teleportOnFirstJoin,
			boolean teleportOnVoid,
			boolean teleportOnRespawn,
			int minPlayersToUse,
			double teleportCost,
			String shape,
			int minX, int maxX,
			int minZ, int maxZ,
			int radiusMin, int radiusMax,
			int maxLocationAttempts,
			int invulnerableTicks,
			int cooldown,
			int teleportCooldown,
			boolean bossbarEnabled,
			String bossbarTitle,
			BarColor bossbarColor,
			BarStyle bossbarType,
			boolean restrictMove,
			boolean restrictDamage, 
			boolean avoidBlocksBlacklist,
			Set<Material> avoidBlocks,
			boolean avoidBiomesBlacklist,
			Set<Biome> avoidBiomes,
			boolean avoidRegions,
			boolean avoidTowns,
			ChannelActions channelActions,
			ChannelMessages channelMessages) {
		this.id = id;
		 this.name = name;
		 this.type = type;
		 this.activeWorlds = activeWorlds;
		 this.teleportToFirstAllowedWorld = teleportToFirstAllowedWorld;
		 this.teleportOnFirstJoin = teleportOnFirstJoin;
		 this.teleportOnVoid = teleportOnVoid;
		 this.teleportOnRespawn = teleportOnRespawn;
		 this.minPlayersToUse = minPlayersToUse;
		 this.teleportCost = teleportCost;
		 this.shape = shape;
		 this.minX = minX; 
		 this.maxX = maxX;
		 this.minZ = minZ; 
		 this.maxZ = maxZ;
		 this.radiusMin = radiusMin;
		 this.radiusMax = radiusMax;
		 this.maxLocationAttempts = maxLocationAttempts;
		 this.invulnerableTicks = invulnerableTicks;
		 this.cooldown = cooldown;
		 this.teleportCooldown = teleportCooldown;
		 this.bossbarEnabled = bossbarEnabled;
		 this.bossbarTitle = bossbarTitle;
		 this.bossbarColor = bossbarColor;
		 this.bossbarType = bossbarType;
		 this.restrictMove = restrictMove;
		 this.restrictDamage = restrictDamage;
		 this.avoidBlocksBlacklist = avoidBlocksBlacklist;
		 this.avoidBlocks = avoidBlocks;
		 this.avoidBiomesBlacklist = avoidBiomesBlacklist;
		 this.avoidBiomes = avoidBiomes;
		 this.avoidRegions = avoidRegions;
		 this.avoidTowns = avoidTowns;
		 this.playerCooldowns = new ExpiringMap<>(cooldown, TimeUnit.SECONDS);
		 this.channelActions = channelActions;
		 this.channelMessages = channelMessages;
	}

}
