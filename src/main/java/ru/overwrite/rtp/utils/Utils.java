package ru.overwrite.rtp.utils;

import it.unimi.dsi.fastutil.chars.CharOpenHashSet;
import it.unimi.dsi.fastutil.chars.CharSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.overwrite.rtp.Main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static boolean DEBUG = false;

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F\\d]{6})");

    public enum SerializerType {
        LEGACY,
        MINIMESSAGE
    }

    private static final char COLOR_CHAR = '§';

    public static String colorize(String message, SerializerType serializer) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        return switch (serializer) {
            case LEGACY -> {
                Matcher matcher = HEX_PATTERN.matcher(message);
                StringBuilder builder = new StringBuilder(message.length() + 32);
                while (matcher.find()) {
                    String group = matcher.group(1);
                    matcher.appendReplacement(builder,
                            COLOR_CHAR + "x" +
                                    COLOR_CHAR + group.charAt(0) +
                                    COLOR_CHAR + group.charAt(1) +
                                    COLOR_CHAR + group.charAt(2) +
                                    COLOR_CHAR + group.charAt(3) +
                                    COLOR_CHAR + group.charAt(4) +
                                    COLOR_CHAR + group.charAt(5));
                }
                message = matcher.appendTail(builder).toString();
                yield translateAlternateColorCodes('&', message);
            }
            case MINIMESSAGE -> {
                Component component = MiniMessage.miniMessage().deserialize(message);
                yield LegacyComponentSerializer.legacySection().serialize(component);
            }
        };
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
            } catch (IOException exception) {
                plugin.getLogger().warning("Can't check for updates: " + exception.getMessage());
            }
        });
    }

    public static void sendMessage(String message, Player p) {
        if (message.isEmpty() || message.isBlank()) {
            return;
        }
        p.sendMessage(message);
    }

    public static String getTime(int time) {
        final int hours = time / 3600;
        final int minutes = (time % 3600) / 60;
        final int seconds = time % 60;

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

    // Original - org.apache.commons.lang3.StringUtils#isNumeric
    public static boolean isNumeric(CharSequence cs) {
        if (cs == null || cs.isEmpty()) {
            return false;
        } else {
            int sz = cs.length();

            for (int i = 0; i < sz; ++i) {
                if (!Character.isDigit(cs.charAt(i))) {
                    return false;
                }
            }

            return true;
        }
    }

    public static String replaceEach(@NotNull String text, @NotNull String[] searchList, @NotNull String[] replacementList) {
        if (text.isEmpty() || searchList.length == 0 || replacementList.length == 0) {
            return text;
        }

        if (searchList.length != replacementList.length) {
            throw new IllegalArgumentException("Search and replacement arrays must have the same length.");
        }

        StringBuilder result = new StringBuilder(text);

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
