package ru.overwrite.rtp.utils.color;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import ru.overwrite.rtp.utils.Colorizer;

public class MiniMessageColorizer implements Colorizer {

    @Override
    public String colorize(String message) {
        Component component = MiniMessage.miniMessage().deserialize(message);
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
}
