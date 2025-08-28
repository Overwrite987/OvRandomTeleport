package ru.overwrite.rtp.logging.impl;

import ru.overwrite.rtp.OvRandomTeleport;
import ru.overwrite.rtp.logging.Logger;

public class BukkitLogger implements Logger {

    private final java.util.logging.Logger logger;

    public BukkitLogger(OvRandomTeleport plugin) {
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
