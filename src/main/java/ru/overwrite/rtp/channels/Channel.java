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

public class Channel {
	
	@Getter
	private final ExpiringMap<String, Long> playerCooldowns;
	
	@Getter
	private String id;
	
	@Getter
	private String name;
	
	@Getter
	private ChannelType type;
	
	@Getter
	private List<World> activeWorlds;
	
	@Getter
	private boolean teleportToFirstAllowedWorld;
	
	@Getter 
	private boolean teleportOnVoid;
	
	@Getter
	private double teleportCost;
	
	@Getter
	private String shape;
	
	@Getter
	private int minX, maxX;
	
	@Getter
	private int minZ, maxZ;
	
	@Getter
	private int maxLocationAttempts;
	
	@Getter
	private int invulnerableTicks;
	
	@Getter
	private int cooldown;
	
	@Getter
	private int teleportCooldown;
	
	@Getter
	private boolean bossbarEnabled;
	
	@Getter
	private String bossbarTitle;
	
	@Getter
	private BarColor bossbarColor;
	
	@Getter
	private BarStyle bossbarType;
	
	@Getter
	private boolean restrictMove;
	
	@Getter
	private boolean restrictDamage;
	
	@Getter 
	private boolean avoidBlocksBlacklist;
	
	@Getter
	private Set<Material> avoidBlocks;
	
	@Getter
	private boolean avoidBiomesBlacklist;
	
	@Getter
	private Set<Biome> avoidBiomes;
	
	@Getter
	private boolean avoidRegions;
	
	@Getter
	private boolean avoidTowns;
	
	@Getter
	private List<Action> preTeleportActions;
	
	@Getter
	private Map<Integer, List<Action>> onCooldownActions;
	
	@Getter
	private List<Action> afterTeleportActions;
	
	public Channel(String id,
			String name,
			ChannelType type,
			List<World> activeWorlds, 
			boolean teleportToFirstAllowedWorld,
			boolean teleportOnVoid,
			double teleportCost,
			String shape,
			int minX, int maxX,
			int minZ, int maxZ,
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
			List<Action> preTeleportActions,
			Map<Integer, List<Action>> onCooldownActions,
			List<Action> afterTeleportActions) {
		this.id = id;
		 this.name = name;
		 this.type = type;
		 this.activeWorlds = activeWorlds;
		 this.teleportToFirstAllowedWorld = teleportToFirstAllowedWorld;
		 this.teleportOnVoid = teleportOnVoid;
		 this.teleportCost = teleportCost;
		 this.shape = shape;
		 this.minX = minX; 
		 this.maxX = maxX;
		 this.minZ = minZ; 
		 this.maxZ = maxZ;
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
		 this.preTeleportActions = preTeleportActions;
		 this.onCooldownActions = onCooldownActions;
		 this.afterTeleportActions = afterTeleportActions;
		 this.playerCooldowns = new ExpiringMap<String, Long>(cooldown, TimeUnit.SECONDS);
	}

}
