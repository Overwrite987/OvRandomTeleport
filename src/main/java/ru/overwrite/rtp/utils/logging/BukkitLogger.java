package ru.overwrite.rtp.utils.logging;

import ru.overwrite.rtp.Logger;
import ru.overwrite.rtp.Main;

public class BukkitLogger implements Logger {

    private final Main plugin;

    public BukkitLogger(Main plugin) {
        this.plugin = plugin;
    }

    public void info(String msg) {
        plugin.getLogger().info(msg);
    }

    public void warn(String msg) {
        plugin.getLogger().warning(msg);
    }

}
