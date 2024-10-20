package ru.overwrite.rtp;

import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bstats.bukkit.Metrics;
import org.bukkit.Server;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import ru.overwrite.rtp.utils.Config;
import ru.overwrite.rtp.utils.Utils;
import ru.overwrite.rtp.utils.VersionUtils;
import ru.overwrite.rtp.utils.regions.WGUtils;
import ru.overwrite.rtp.utils.logging.BukkitLogger;
import ru.overwrite.rtp.utils.logging.PaperLogger;

import java.lang.reflect.Constructor;

public final class Main extends JavaPlugin {

    private final Server server = getServer();

    @Getter
    private final Logger pluginLogger = VersionUtils.SUB_VERSION >= 19 ? new PaperLogger(this) : new BukkitLogger(this);

    @Getter
    private final Config pluginConfig = new Config();

    @Getter
    private final RtpManager rtpManager = new RtpManager(this);

    @Getter
    private Economy economy;

    @Getter
    private Permission perms;

    @Override
    public void onLoad() {
        if (server.getPluginManager().isPluginEnabled("PlugManX") || server.getPluginManager().isPluginEnabled("PlugMan")) {
            return;
        }
        if (hasWorldGuard()) {
            WGUtils.setupRtpFlag();
            pluginLogger.info("§5WorldGuard подключён!");
        }
    }

    private Boolean hasWorldGuard = null;

    public boolean hasWorldGuard() {
        if (hasWorldGuard == null) {
            try {
                Class.forName("com.sk89q.worldguard.protection.flags.registry.FlagConflictException");
                hasWorldGuard = true;
            } catch (ClassNotFoundException ex) {
                hasWorldGuard = false;
            }
        }
        return hasWorldGuard;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        PluginManager pluginManager = server.getPluginManager();
        final FileConfiguration config = getConfig();
        final ConfigurationSection mainSettings = config.getConfigurationSection("main_settings");
        Utils.setupColorizer(mainSettings);
        pluginConfig.setupMessages(config);
        registerCommand(pluginManager, mainSettings);
        if (mainSettings.getBoolean("enable_metrics")) {
            new Metrics(this, 22021);
        }
        if (pluginManager.isPluginEnabled("Vault")) {
            ServicesManager servicesManager = server.getServicesManager();
            setupEconomy(servicesManager);
            setupPerms(servicesManager);
        }
        setupPlaceholders(mainSettings, pluginManager);
        pluginManager.registerEvents(new RtpListener(this), this);
        checkForUpdates(mainSettings);
        server.getScheduler().runTaskAsynchronously(this, () -> rtpManager.setupChannels(config, pluginManager));
    }

    public void checkForUpdates(ConfigurationSection mainSettings) {
        if (!mainSettings.getBoolean("update_checker", true)) {
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
                pluginLogger.info("");
                pluginLogger.info("§aИли обновите плагин при помощи §b/rtp admin update");
            }
            pluginLogger.info("§6========================================");
        });
    }

    private void setupEconomy(ServicesManager servicesManager) {
        RegisteredServiceProvider<Economy> economyProvider = servicesManager.getRegistration(Economy.class);
        if (economyProvider == null) {
            return;
        }
        economy = economyProvider.getProvider();
        pluginLogger.info("§6Экономика подключена!");
    }

    private void setupPerms(ServicesManager servicesManager) {
        RegisteredServiceProvider<Permission> permissionProvider = servicesManager.getRegistration(Permission.class);
        if (permissionProvider == null) {
            return;
        }
        perms = permissionProvider.getProvider();
        pluginLogger.info("§aМенеджер прав подключён!");
    }

    private void setupPlaceholders(ConfigurationSection mainSettings, PluginManager pluginManager) {
        if (!mainSettings.getBoolean("papi_support", true) || !pluginManager.isPluginEnabled("PlaceholderAPI")) {
            return;
        }
        Utils.USE_PAPI = true;
        new RtpExpansion(this).register();
        pluginLogger.info("§eПлейсхолдеры подключены!");
    }

    private void registerCommand(PluginManager pluginManager, ConfigurationSection mainSettings) {
        try {
            CommandMap commandMap = server.getCommandMap();
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);
            PluginCommand command = constructor.newInstance(mainSettings.getString("rtp_command", "rtp"), this);
            command.setAliases(mainSettings.getStringList("rtp_aliases"));
            RtpCommand rtpCommand = new RtpCommand(this);
            command.setExecutor(rtpCommand);
            command.setTabCompleter(rtpCommand);
            commandMap.register(getDescription().getName(), command);
        } catch (Exception ex) {
            pluginLogger.info("Unable to register RTP command!" + ex);
            pluginManager.disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        server.getScheduler().cancelTasks(this);
    }

}
