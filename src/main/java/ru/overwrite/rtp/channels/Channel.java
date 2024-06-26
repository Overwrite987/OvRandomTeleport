package ru.overwrite.rtp.channels;

import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import ru.overwrite.rtp.utils.ExpiringMap;

import org.bukkit.World;

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
	
	private final BossBar bossBar;
	
	private final boolean restrictMove;
	
	private final boolean restrictDamage;

	private final Avoidance avoidance;

	private final Actions actions;

	private final Messages messages;
	
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
			BossBar bossBar,
			boolean restrictMove,
			boolean restrictDamage,
			Avoidance avoidance,
			Actions actions,
			Messages messages) {
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
		 this.playerCooldowns = new ExpiringMap<>(cooldown, TimeUnit.SECONDS);
		 this.teleportCooldown = teleportCooldown;
		 this.bossBar = bossBar;
		 this.restrictMove = restrictMove;
		 this.restrictDamage = restrictDamage;
		 this.avoidance = avoidance;
		 this.actions = actions;
		 this.messages = messages;
	}

}
