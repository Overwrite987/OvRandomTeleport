package ru.overwrite.rtp;

import org.bstats.bukkit.Metrics;

import org.bukkit.Server;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;

import lombok.Getter;
import ru.overwrite.rtp.utils.Config;
import ru.overwrite.rtp.utils.Utils;
import ru.overwrite.rtp.utils.logging.BukkitLogger;
import ru.overwrite.rtp.utils.logging.PaperLogger;

import java.lang.reflect.Constructor;

public class Main extends JavaPlugin {

	private final Server server = getServer();

	@Getter
	private final Logger pluginLogger = Utils.FOLIA ? new PaperLogger(this) : new BukkitLogger(this);
	
	@Getter
	private final Config pluginConfig = new Config();
	
	@Getter 
	private final RtpManager rtpManager = new RtpManager(this);
	
	@Getter
	private Economy economy;

	@Override
	public void onEnable() {
		if (!isPaper()) {
			return;
		}
		saveDefaultConfig();
		PluginManager pluginManager = server.getPluginManager();
		FileConfiguration config = getConfig();
		pluginConfig.setupMessages(config);
		ConfigurationSection mainSettings = config.getConfigurationSection("main_settings");
		registerCommand(pluginManager, mainSettings);
		if (mainSettings.getBoolean("enable_metrics")) {
			new Metrics(this, 22021);
		}
		setupEconomy(pluginManager);
		pluginManager.registerEvents(new RtpListener(this), this);
		server.getScheduler().runTaskAsynchronously(this, () -> rtpManager.setupChannels(config, pluginManager));
	}
	
	public boolean isPaper() {
		if (server.getName().equals("CraftBukkit")) {
			pluginLogger.info(" ");
			pluginLogger.info("§6============= §6! WARNING ! §c=============");
			pluginLogger.info("§eЭтот плагин работает только на Paper и его форках!");
			pluginLogger.info("§eАвтор плагина §cкатегорически §eвыступает за отказ от использования устаревшего и уязвимого софта!");
			pluginLogger.info("§eСкачать Paper: §ahttps://papermc.io/downloads/all");
			pluginLogger.info("§6============= §6! WARNING ! §c=============");
			pluginLogger.info(" ");
			this.setEnabled(false);
			return false;
		}
		return true;
	}

	public void checkForUpdates(FileConfiguration config) {
		if (!config.getBoolean("main-settings.update-checker")) {
			return;
		}
		Utils.checkUpdates(this, version -> {
			pluginLogger.info("§6========================================");
			if (getDescription().getVersion().equals(version)) {
				pluginLogger.info("§aВы используете последнюю версию плагина!");
			} else {
				pluginLogger.info("§aВы используете устаревшую плагина!");
				pluginLogger.info("§aВы можете скачать новую версию здесь:");
				pluginLogger.info("§bgithub.com/Overwrite987/OvRandomTeleport/releases/");
			}
			pluginLogger.info("§6========================================");
		});
	}
	
	private void setupEconomy(PluginManager pluginManager) {
        if (!pluginManager.isPluginEnabled("Vault")) {
            return;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return;
        }
        economy = rsp.getProvider();
    }

	private void registerCommand(PluginManager pluginManager, ConfigurationSection mainSettings) {
		try {
			CommandMap commandMap = server.getCommandMap();
			Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
			constructor.setAccessible(true);
			PluginCommand command = constructor.newInstance(mainSettings.getString("rtp_command"), this);
			RtpCommand rtpCommand = new RtpCommand(this);
			command.setExecutor(rtpCommand);
			command.setTabCompleter(rtpCommand);
			commandMap.register(getDescription().getName(), command);
		} catch (Exception ex) {
			pluginLogger.info("Unable to register password command!");
			ex.printStackTrace();
			pluginManager.disablePlugin(this);
		}
	}

	@Override
	public void onDisable() {
		server.getScheduler().cancelTasks(this);
	}

}
