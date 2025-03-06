package ru.overwrite.rtp.utils.logging;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import ru.overwrite.rtp.Main;

public class PaperLogger implements Logger {

    private final net.kyori.adventure.text.logger.slf4j.ComponentLogger logger;

    private final LegacyComponentSerializer legacySection;

    public PaperLogger(Main plugin) {
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
