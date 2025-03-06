package ru.overwrite.rtp.utils.logging;

import ru.overwrite.rtp.Main;

public class BukkitLogger implements Logger {

    private final java.util.logging.Logger logger;

    public BukkitLogger(Main plugin) {
        this.logger = plugin.getLogger();
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void warn(String msg) {
        logger.warning(msg);
    }

}
