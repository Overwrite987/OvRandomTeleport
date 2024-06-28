package ru.overwrite.rtp.utils.logging;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import ru.overwrite.rtp.Logger;
import ru.overwrite.rtp.Main;

public class PaperLogger implements Logger {

    private final Main plugin;

    private final LegacyComponentSerializer legacySection = LegacyComponentSerializer.legacySection();

    public PaperLogger(Main plugin) {
        this.plugin = plugin;
    }

    public void info(String msg) {
        plugin.getComponentLogger().info(legacySection.deserialize(msg));
    }

    public void warn(String msg) {
        plugin.getComponentLogger().warn(legacySection.deserialize(msg));
    }

}
