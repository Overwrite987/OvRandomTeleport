package ru.overwrite.rtp.utils;

import it.unimi.dsi.fastutil.chars.CharOpenHashSet;
import it.unimi.dsi.fastutil.chars.CharSet;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.overwrite.rtp.Main;
import ru.overwrite.rtp.utils.color.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Utils {

    public static boolean DEBUG = false;

    public static Colorizer COLORIZER;

    public static void setupColorizer(ConfigurationSection mainSettings) {
        COLORIZER = switch (mainSettings.getString("serializer", "LEGACY").toUpperCase()) {
            case "MINIMESSAGE" -> new MiniMessageColorizer();
            case "LEGACY" -> new LegacyColorizer();
            case "LEGACY_ADVANCED" -> new LegacyAdvancedColorizer();
            default -> new VanillaColorizer();
        };
    }

    public static List<World> getWorldList(List<String> worldNames) {
        List<World> worldList = new ArrayList<>();
        for (String w : worldNames) {
            worldList.add(Bukkit.getWorld(w));
        }
        return worldList;
    }

    public static void checkUpdates(Main plugin, Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new URL("https://raw.githubusercontent.com/Overwrite987/OvRandomTeleport/master/VERSION")
                            .openStream()))) {
                consumer.accept(reader.readLine().trim());
            } catch (IOException ex) {
                plugin.getLogger().warning("Unable to check for updates: " + ex.getMessage());
            }
        });
    }

    private static final CharSet CODES = new CharOpenHashSet(new char[]{
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f',
            'A', 'B', 'C', 'D', 'E', 'F',
            'k', 'l', 'm', 'n', 'o', 'r', 'x',
            'K', 'L', 'M', 'N', 'O', 'R', 'X'
    });

    public static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        char[] b = textToTranslate.toCharArray();

        for (int i = 0, length = b.length - 1; i < length; ++i) {
            if (b[i] == altColorChar && CODES.contains(b[i + 1])) {
                b[i++] = '§';
                b[i] = Character.toLowerCase(b[i]);
            }
        }

        return new String(b);
    }

    public static boolean USE_PAPI = false;

    public static void sendMessage(String message, Player p) {
        if (message.isEmpty() || message.isBlank()) {
            return;
        }
        if (USE_PAPI) {
            p.sendMessage(parsePlaceholders(message, p));
            return;
        }
        p.sendMessage(message);
    }

    public static String parsePlaceholders(String message, Player p) {
        if (PlaceholderAPI.containsPlaceholders(message)) {
            message = PlaceholderAPI.setPlaceholders(p, message);
        }
        return message;
    }

    public static String getTime(int time) {
        final int hours = getHours(time);
        final int minutes = getMinutes(time);
        final int seconds = getSeconds(time);

        final StringBuilder result = new StringBuilder();

        if (hours > 0) {
            result.append(hours).append(Config.time_hours);
        }

        if (minutes > 0 || hours > 0) {
            result.append(minutes).append(Config.time_minutes);
        }

        result.append(seconds).append(Config.time_seconds);

        return result.toString();
    }

    public static int getHours(int time) {
        return time / 3600;
    }

    public static int getMinutes(int time) {
        return (time % 3600) / 60;
    }

    public static int getSeconds(int time) {
        return time % 60;
    }

    public static boolean isNumeric(CharSequence cs) {
        if (cs == null || cs.isEmpty()) {
            return false;
        }
        int sz = cs.length();
        for (int i = 0; i < sz; ++i) {
            if (!Character.isDigit(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static String replaceEach(@NotNull String text, @NotNull String[] searchList, @NotNull String[] replacementList) {
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

    public static boolean startsWithIgnoreCase(String str, String prefix) {
        if (str == null || prefix == null) {
            return false;
        }
        return str.regionMatches(true, 0, prefix, 0, prefix.length());
    }
}
