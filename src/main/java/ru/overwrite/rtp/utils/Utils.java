package ru.overwrite.rtp.utils;

import lombok.experimental.UtilityClass;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.overwrite.rtp.OvRandomTeleport;
import ru.overwrite.rtp.channels.settings.Particles;
import ru.overwrite.rtp.color.Colorizer;
import ru.overwrite.rtp.color.impl.LegacyAdvancedColorizer;
import ru.overwrite.rtp.color.impl.LegacyColorizer;
import ru.overwrite.rtp.color.impl.MiniMessageColorizer;
import ru.overwrite.rtp.configuration.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

@UtilityClass
public final class Utils {

    public boolean DEBUG = Boolean.getBoolean("OvRandomTeleport.Debug");

    public Colorizer COLORIZER;

    public void setupColorizer(ConfigurationSection mainSettings) {
        COLORIZER = switch (mainSettings.getString("serializer", "LEGACY").toUpperCase(Locale.ENGLISH)) {
            case "MINIMESSAGE" -> new MiniMessageColorizer();
            case "LEGACY_ADVANCED" -> new LegacyAdvancedColorizer();
            default -> new LegacyColorizer();
        };
    }

//    public List<World> getWorldList(List<String> worldNames) {
//        if (!worldNames.isEmpty() && worldNames.get(0).equals("*")) {
//            return Bukkit.getWorlds();
//        }
//        final List<World> worldList = new ArrayList<>(worldNames.size());
//        for (String w : worldNames) {
//            worldList.add(Bukkit.getWorld(w));
//        }
//        return worldList;
//    }

    public Particles.ParticleData createParticleData(String id) {
        int separatorIndex = id.indexOf(';');

        Particle particle = (separatorIndex == -1)
                ? Particle.valueOf(id.toUpperCase(Locale.ENGLISH))
                : Particle.valueOf(id.substring(0, separatorIndex).toUpperCase(Locale.ENGLISH));

        Particle.DustOptions data = (separatorIndex != -1)
                ? parseParticleData(particle, id.substring(separatorIndex + 1))
                : null;

        return new Particles.ParticleData(particle, data);
    }

    private Particle.DustOptions parseParticleData(Particle particle, String value) {
        if (!particle.getDataType().isAssignableFrom(Particle.DustOptions.class)) {
            return null;
        }
        String[] parts = value.split(",");
        if (parts.length < 3) {
            return null;
        }
        int red = Integer.parseInt(parts[0].trim());
        int green = Integer.parseInt(parts[1].trim());
        int blue = Integer.parseInt(parts[2].trim());
        float size = (parts.length > 3) ? Float.parseFloat(parts[3].trim()) : 1.0F;

        return new Particle.DustOptions(Color.fromRGB(red, green, blue), size);
    }

    public void checkUpdates(OvRandomTeleport plugin, Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new URL("https://raw.githubusercontent.com/Overwrite987/OvRandomTeleport/master/VERSION")
                            .openStream()))) {
                consumer.accept(reader.readLine().trim());
            } catch (IOException ex) {
                plugin.getLogger().warning("Unable to check for updates: " + ex.getMessage());
            }
        }, 30L);
    }

    public final char COLOR_CHAR = '§';

    public String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        final char[] b = textToTranslate.toCharArray();

        for (int i = 0, length = b.length - 1; i < length; i++) {
            if (b[i] == altColorChar && isValidColorCharacter(b[i + 1])) {
                b[i++] = COLOR_CHAR;
                b[i] |= 0x20;
            }
        }

        return new String(b);
    }

    private boolean isValidColorCharacter(char c) {
        return switch (c) {
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'A', 'B', 'C', 'D',
                 'E', 'F', 'r', 'R', 'k', 'K', 'l', 'L', 'm', 'M', 'n', 'N', 'o', 'O', 'x', 'X' -> true;
            default -> false;
        };
    }

    public boolean USE_PAPI;

    public void sendMessage(String message, Player player) {
        if (message.isBlank()) {
            return;
        }
        if (USE_PAPI) {
            player.sendMessage(parsePlaceholders(message, player));
            return;
        }
        player.sendMessage(message);
    }

    public String parsePlaceholders(String message, Player player) {
        if (PlaceholderAPI.containsPlaceholders(message)) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        }
        return message;
    }

    public String locationToString(Location location) {
        return "(" + location.getWorld().getName() + "/"
                + location.getBlockX() + "/"
                + location.getBlockY() + "/"
                + location.getBlockZ() + ")";
    }

    public String getTime(int time) {
        final int hours = getHours(time);
        final int minutes = getMinutes(time);
        final int seconds = getSeconds(time);

        final StringBuilder result = new StringBuilder();

        if (hours > 0) {
            result.append(hours).append(Config.timeHours);
        }

        if (minutes > 0 || hours > 0) {
            result.append(minutes).append(Config.timeMinutes);
        }

        if (seconds > 0 || (hours == 0 && minutes == 0)) {
            result.append(seconds).append(Config.timeSeconds);
        }

        return result.toString();
    }

    public int getHours(int time) {
        return time / 3600;
    }

    public int getMinutes(int time) {
        return (time % 3600) / 60;
    }

    public int getSeconds(int time) {
        return time % 60;
    }

    public boolean isNumeric(CharSequence cs) {
        if (cs == null || cs.isEmpty()) {
            return false;
        }
        for (int i = 0, length = cs.length(); i < length; ++i) {
            if (!Character.isDigit(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public String replaceEach(@NotNull String text, @NotNull String[] searchList, @NotNull String[] replacementList) {
        if (text.isEmpty() || searchList.length == 0 || replacementList.length == 0) {
            return text;
        }

        if (searchList.length != replacementList.length) {
            throw new IllegalArgumentException("Search and replacement arrays must have the same length.");
        }

        final StringBuilder result = new StringBuilder(text);

        for (int i = 0; i < searchList.length; i++) {
            String search = searchList[i];
            String replacement = replacementList[i];

            int start = 0;

            while ((start = result.indexOf(search, start)) != -1) {
                result.replace(start, start + search.length(), replacement);
                start += replacement.length();
            }
        }

        return result.toString();
    }
}
