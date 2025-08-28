package ru.overwrite.rtp.logging.impl;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import ru.overwrite.rtp.OvRandomTeleport;
import ru.overwrite.rtp.logging.Logger;

public class PaperLogger implements Logger {

    private final net.kyori.adventure.text.logger.slf4j.ComponentLogger logger;

    private final LegacyComponentSerializer legacySection;

    public PaperLogger(OvRandomTeleport plugin) {
        this.logger = plugin.getComponentLogger();
        this.legacySection = LegacyComponentSerializer.legacySection();
    }

    @Override
    public void info(String msg) {
        logger.info(legacySection.deserialize(msg));
    }

    @Override
    public void warn(String msg) {
        logger.warn(legacySection.deserialize(msg));
    }

}
