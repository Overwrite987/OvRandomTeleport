package ru.overwrite.rtp;

import lombok.AccessLevel;
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
import ru.overwrite.rtp.configuration.Config;
import ru.overwrite.rtp.utils.PluginMessage;
import ru.overwrite.rtp.utils.Utils;
import ru.overwrite.rtp.utils.VersionUtils;
import ru.overwrite.rtp.utils.logging.*;
import ru.overwrite.rtp.utils.regions.WGUtils;

import java.lang.reflect.Constructor;

@Getter
public final class OvRandomTeleport extends JavaPlugin {

    @Getter(AccessLevel.NONE)
    private final Server server = getServer();

    private final Logger pluginLogger = VersionUtils.SUB_VERSION >= 19 ? new PaperLogger(this) : new BukkitLogger(this);

    private final Config pluginConfig = new Config(this);

    private final RtpManager rtpManager = new RtpManager(this);

    private Economy economy;

    private Permission perms;

    private PluginMessage pluginMessage;

    private RtpExpansion rtpExpansion;

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

    @Getter(AccessLevel.NONE)
    private Boolean hasWorldGuard;

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
        final FileConfiguration config = getConfig();
        final ConfigurationSection mainSettings = config.getConfigurationSection("main_settings");
        Utils.setupColorizer(mainSettings);
        rtpManager.setMaxTeleporting(mainSettings.getInt("max_teleporting"));
        pluginConfig.setupMessages(config);
        pluginConfig.setupTemplates();
        PluginManager pluginManager = server.getPluginManager();
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
        setupProxy(mainSettings);
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
        economy = getProvider(servicesManager, Economy.class);
        if (economy != null) {
            pluginLogger.info("§6Экономика подключена!");
        }
    }

    private void setupPerms(ServicesManager servicesManager) {
        perms = getProvider(servicesManager, Permission.class);
        if (perms != null) {
            pluginLogger.info("§aМенеджер прав подключён!");
        }
    }

    private <T> T getProvider(ServicesManager servicesManager, Class<T> clazz) {
        final RegisteredServiceProvider<T> provider = servicesManager.getRegistration(clazz);
        return provider != null ? provider.getProvider() : null;
    }

    private void setupPlaceholders(ConfigurationSection mainSettings, PluginManager pluginManager) {
        if (!mainSettings.getBoolean("papi_support", true) || !pluginManager.isPluginEnabled("PlaceholderAPI")) {
            return;
        }
        Utils.USE_PAPI = true;
        rtpExpansion = new RtpExpansion(this);
        rtpExpansion.register();
        pluginLogger.info("§eПлейсхолдеры подключены!");
    }

    private void setupProxy(ConfigurationSection mainSettings) {
        ConfigurationSection proxy = mainSettings.getConfigurationSection("proxy");
        if (proxy.getBoolean("enabled", false)) {
            server.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            String serverId = proxy.getString("server_id");
            pluginMessage = new PluginMessage(this, serverId);
            server.getMessenger().registerIncomingPluginChannel(this, "BungeeCord", pluginMessage);
            rtpManager.initProxyCalls();
        }
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
            commandMap.register(getDescription().getName(), command);
        } catch (Exception ex) {
            pluginLogger.info("Unable to register RTP command!" + ex);
            pluginManager.disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        rtpManager.cancelAllTasks();
        if (rtpExpansion != null) {
            rtpExpansion.unregister();
        }
        server.getScheduler().cancelTasks(this);
    }
}
