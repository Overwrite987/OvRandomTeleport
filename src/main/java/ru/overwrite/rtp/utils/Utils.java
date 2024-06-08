package ru.overwrite.rtp.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import static net.md_5.bungee.api.ChatColor.COLOR_CHAR;

public class Utils {
	
	public static final boolean FOLIA;

	static {
		boolean folia;
		try {
			Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
			folia = true;
		} catch (ClassNotFoundException e) {
			folia = false;
		}
		FOLIA = folia;
	}
	
	public static boolean DEBUG = false;
	
	private static final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F\\d]{6})");
	public static final int SUB_VERSION = Integer.parseInt(Bukkit.getBukkitVersion().split("\\.")[1]);
	
	public static final int VOID_LEVEL = SUB_VERSION >= 18 ? -60 : 0;

	public static String colorize(String message) {
		if (SUB_VERSION >= 16) {
			Matcher matcher = HEX_PATTERN.matcher(message);
			StringBuilder builder = new StringBuilder(message.length() + 4 * 8);
			while (matcher.find()) {
				String group = matcher.group(1);
				matcher.appendReplacement(builder,
						COLOR_CHAR + "x" +
								COLOR_CHAR + group.charAt(0) +
								COLOR_CHAR + group.charAt(1) +
								COLOR_CHAR + group.charAt(2) +
								COLOR_CHAR + group.charAt(3) +
								COLOR_CHAR + group.charAt(4) +
								COLOR_CHAR + group.charAt(5));
			}
			message = matcher.appendTail(builder).toString();
		}
		return ChatColor.translateAlternateColorCodes('&', message);
	}
	
	public static void sendTitleMessage(String[] titleMessages, Player p) {
		if (titleMessages.length > 5) {
			Bukkit.getConsoleSender().sendMessage ("Unable to send title. " + titleMessages.toString());
			return;
		}
		String title = titleMessages[0];
		String subtitle = (titleMessages.length >= 2 && titleMessages[1] != null) ? titleMessages[1] : "";
		int fadeIn = (titleMessages.length >= 3 && titleMessages[2] != null) ? Integer.parseInt(titleMessages[2]) : 10;
		int stay = (titleMessages.length >= 4 && titleMessages[3] != null) ? Integer.parseInt(titleMessages[3]) : 70;
		int fadeOut = (titleMessages.length == 5 && titleMessages[4] != null) ? Integer.parseInt(titleMessages[4]) : 20;
		p.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
	}

	public static void sendSound(String[] soundArgs, Player p) {
		if (soundArgs.length > 3) {
			Bukkit.getConsoleSender().sendMessage ("Unable to send sound. " + soundArgs.toString());
			return;
		}
		Sound sound = Sound.valueOf(soundArgs[0]);
		float volume = (soundArgs.length >= 2 && soundArgs[1] != null) ? Float.parseFloat(soundArgs[1]) : 1.0f;
		float pitch = (soundArgs.length == 3 && soundArgs[2] != null) ? Float.parseFloat(soundArgs[2]) : 1.0f;
		p.playSound(p.getLocation(), sound, volume, pitch);
	}

	public static void giveEffect(String[] effectArgs, Player p) {
		if (effectArgs.length > 3) {
			Bukkit.getConsoleSender().sendMessage ("Unable to give effect. " + effectArgs.toString());
			return;
		}
		PotionEffectType effectType = PotionEffectType.getByName(effectArgs[0]);
		int duration = (effectArgs.length >= 2 && effectArgs[1] != null) ? Integer.parseInt(effectArgs[1]) : 1;
		int amplifier = (effectArgs.length == 3 && effectArgs[2] != null) ? Integer.parseInt(effectArgs[2]) : 1;
		PotionEffect effect = new PotionEffect(effectType, duration, amplifier);
		p.addPotionEffect(effect);
	}
	
	public static String getTime(int time) {
	    int hours = time / 3600;
	    int minutes = (time % 3600) / 60;
	    int seconds = time % 60;

	    StringBuilder result = new StringBuilder();

	    if (hours > 0) {
	        result.append(hours).append(Config.time_hours);
	    }

	    if (minutes > 0 || (hours > 0 && seconds == 0)) {
	        result.append(minutes).append(Config.time_minutes);
	    }

	    if (seconds > 0 || (minutes == 0 && hours == 0)) {
	        result.append(seconds).append(Config.time_seconds);
	    }

	    return result.toString();
	}

}
