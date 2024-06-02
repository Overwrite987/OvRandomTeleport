package ru.overwrite.rtp.utils;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class Config {
	
	public String messages_no_perms, messages_invalid_world, messages_not_enough_money, messages_cooldown, messages_incorrect_channel, messages_moved_on_teleport, messages_damaged_on_teleport, messages_fail_to_find_location, messages_already_teleporting, 
	messages_reload, messages_unknown_argument, messages_player_not_found, messages_admin_help;
	
	public static String time_hours, time_minutes, time_seconds;
	
	public void setupMessages(FileConfiguration config) {
		ConfigurationSection messages = config.getConfigurationSection("messages");
		String prefix = messages.getString("prefix", "messages.prefix");
		messages_no_perms = Utils.colorize(messages.getString("no_perms", "messages.no_perms").replace("%prefix%", prefix));
		messages_invalid_world = Utils.colorize(messages.getString("invalid_world", "messages.invalid_world").replace("%prefix%", prefix));
		messages_not_enough_money = Utils.colorize(messages.getString("not_enough_money", "messages.not_enough_money").replace("%prefix%", prefix));
		messages_cooldown = Utils.colorize(messages.getString("cooldown", "messages.cooldown").replace("%prefix%", prefix));
		messages_incorrect_channel = Utils.colorize(messages.getString("incorrect_channel", "messages.incorrect_channel").replace("%prefix%", prefix));
		messages_moved_on_teleport = Utils.colorize(messages.getString("moved_on_teleport", "messages.moved_on_teleport").replace("%prefix%", prefix));
		messages_damaged_on_teleport = Utils.colorize(messages.getString("damaged_on_teleport", "messages.damaged_on_teleport").replace("%prefix%", prefix));
		messages_fail_to_find_location = Utils.colorize(messages.getString("fail_to_find_location", "messages.fail_to_find_location").replace("%prefix%", prefix));
		messages_already_teleporting = Utils.colorize(messages.getString("already_teleporting", "messages.already_teleporting").replace("%prefix%", prefix));
		ConfigurationSection admin = messages.getConfigurationSection("admin");
		messages_reload = Utils.colorize(admin.getString("reload").replace("%prefix%", prefix));
		messages_unknown_argument = Utils.colorize(admin.getString("unknown_argument").replace("%prefix%", prefix));
		messages_player_not_found = Utils.colorize(admin.getString("player_not_found").replace("%prefix%", prefix));
		messages_admin_help = Utils.colorize(admin.getString("admin_help").replace("%prefix%", prefix));
		ConfigurationSection time = messages.getConfigurationSection("time");
		time_hours =  Utils.colorize(time.getString("hours", " ч."));
		time_minutes =  Utils.colorize(time.getString("minutes", " мин."));
		time_seconds =  Utils.colorize(time.getString("seconds", " сек."));
	}

}
