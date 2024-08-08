package ru.overwrite.rtp.utils;

import it.unimi.dsi.fastutil.chars.CharOpenHashSet;
import it.unimi.dsi.fastutil.chars.CharSet;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import ru.overwrite.rtp.Main;

public class Utils {

    public static boolean DEBUG = false;

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F\\d]{6})");
    public static final int SUB_VERSION = Integer.parseInt(Bukkit.getBukkitVersion().split("-")[0].split("\\.")[1]);

    public static final int VOID_LEVEL = SUB_VERSION >= 18 ? -60 : 0;

    public enum SerializerType {
        LEGACY,
        MINIMESSAGE
    }

    private static final char COLOR_CHAR = '§';

    public static String colorize(String message, SerializerType serializer) {
        return switch (serializer) {
            case LEGACY -> {
                if (SUB_VERSION >= 16) {
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
                }
                yield translateAlternateColorCodes('&', message);
            }
            case MINIMESSAGE -> {
                Component component = MiniMessage.miniMessage().deserialize(message);
                yield LegacyComponentSerializer.legacySection().serialize(component);
            }
        };
    }

    private static final CharSet CODES = new CharOpenHashSet(
            new char[]{
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                    'a', 'b', 'c', 'd', 'e', 'f',
                    'A', 'B', 'C', 'D', 'E', 'F',
                    'k', 'l', 'm', 'n', 'o', 'r', 'x',
                    'K', 'L', 'M', 'N', 'O', 'R', 'X'}
    ); // FUCK YOU 1.16 USERS!

    public static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        char[] b = textToTranslate.toCharArray();

        for (int i = 0, length = b.length - 1; i < length - 1; ++i) {
            if (b[i] == altColorChar && CODES.contains(b[i + 1])) {
                b[i++] = '§';
                b[i] = Character.toLowerCase(b[i]);
            }
        }

        return new String(b);
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

    public static void sendTitleMessage(String[] titleMessages, Player p) {
        if (titleMessages.length > 5) {
            Bukkit.getConsoleSender().sendMessage("Unable to send title. " + Arrays.toString(titleMessages));
            return;
        }
        String title = titleMessages[0];
        String subtitle = (titleMessages.length >= 2 && titleMessages[1] != null) ? titleMessages[1] : "";
        int fadeIn = (titleMessages.length >= 3 && titleMessages[2] != null) ? Integer.parseInt(titleMessages[2]) : 10;
        int stay = (titleMessages.length >= 4 && titleMessages[3] != null) ? Integer.parseInt(titleMessages[3]) : 70;
        int fadeOut = (titleMessages.length == 5 && titleMessages[4] != null) ? Integer.parseInt(titleMessages[4]) : 20;
        p.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    public static void sendSound(String[] soundArgs, Player p) {
        if (soundArgs.length > 3) {
            Bukkit.getConsoleSender().sendMessage("Unable to send sound. " + Arrays.toString(soundArgs));
            return;
        }
        Sound sound = Sound.valueOf(soundArgs[0]);
        float volume = (soundArgs.length >= 2 && soundArgs[1] != null) ? Float.parseFloat(soundArgs[1]) : 1.0f;
        float pitch = (soundArgs.length == 3 && soundArgs[2] != null) ? Float.parseFloat(soundArgs[2]) : 1.0f;
        p.playSound(p.getLocation(), sound, volume, pitch);
    }

    public static void giveEffect(String[] effectArgs, Player p) {
        if (effectArgs.length > 3) {
            Bukkit.getConsoleSender().sendMessage("Unable to give effect. " + Arrays.toString(effectArgs));
            return;
        }
        PotionEffectType effectType = PotionEffectType.getByName(effectArgs[0]);
        int duration = (effectArgs.length >= 2 && effectArgs[1] != null) ? Integer.parseInt(effectArgs[1]) : 1;
        int amplifier = (effectArgs.length == 3 && effectArgs[2] != null) ? Integer.parseInt(effectArgs[2]) : 1;
        PotionEffect effect = new PotionEffect(effectType, duration, amplifier);
        p.addPotionEffect(effect);
    }

    public static String getTime(int time) {
        int hours = time / 3600;
        int minutes = (time % 3600) / 60;
        int seconds = time % 60;

        StringBuilder result = new StringBuilder();

        if (hours > 0) {
            result.append(hours).append(Config.time_hours);
        }

        if (minutes > 0 || (hours > 0 && seconds == 0)) {
            result.append(minutes).append(Config.time_minutes);
        }

        if (seconds > 0 || (minutes == 0 && hours == 0)) {
            result.append(seconds).append(Config.time_seconds);
        }

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
