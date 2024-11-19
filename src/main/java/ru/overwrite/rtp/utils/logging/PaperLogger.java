package ru.overwrite.rtp.utils.logging;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import ru.overwrite.rtp.Main;

public class PaperLogger implements Logger {

    private final Main plugin;

    private final LegacyComponentSerializer legacySection = LegacyComponentSerializer.legacySection();

    public PaperLogger(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void info(String msg) {
        plugin.getComponentLogger().info(legacySection.deserialize(msg));
    }

    @Override
    public void warn(String msg) {
        plugin.getComponentLogger().warn(legacySection.deserialize(msg));
    }

}
