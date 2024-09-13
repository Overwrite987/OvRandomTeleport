package ru.overwrite.rtp.utils.color;

import ru.overwrite.rtp.utils.Colorizer;
import ru.overwrite.rtp.utils.Utils;

public class VanillaColorizer implements Colorizer {

    @Override
    public String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        return Utils.translateAlternateColorCodes('&', message);
    }
}
