package ru.overwrite.rtp.actions.impl;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.overwrite.rtp.OvRandomTeleport;
import ru.overwrite.rtp.actions.Action;
import ru.overwrite.rtp.actions.ActionType;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class MessageActionType implements ActionType {

    private static final Key KEY = Key.key("ovrandomteleport:message");

    @Override
    public @NotNull Action instance(@NotNull String context, @NotNull OvRandomTeleport plugin) {
        String text = Utils.COLORIZER.colorize(context);
        return new MessageAction(text);
    }

    @Override
    public @NotNull Key key() {
        return KEY;
    }

    private record MessageAction(@NotNull String message) implements Action {

        private static final String HOVER_TEXT_PREFIX = "hoverText={";
        private static final String CLICK_EVENT_PREFIX = "clickEvent={";
        private static final String BUTTON_PREFIX = "button={";
        public static final String[] HOVER_MARKERS = {HOVER_TEXT_PREFIX, CLICK_EVENT_PREFIX};

        @Override
        public void perform(@NotNull Channel channel, @NotNull Player player, @NotNull String[] searchList, @NotNull String[] replacementList) {
            String messageToPlayer = Utils.replaceEach(message, searchList, replacementList);
            if (Utils.USE_PAPI) {
                messageToPlayer = Utils.parsePlaceholders(messageToPlayer, player);
            }

            boolean hasAdvancedFormatting = messageToPlayer.contains(BUTTON_PREFIX) ||
                    messageToPlayer.contains(HOVER_TEXT_PREFIX) ||
                    messageToPlayer.contains(CLICK_EVENT_PREFIX);

            if (hasAdvancedFormatting) {
                Component component = parseMessage(messageToPlayer);
                player.sendMessage(component);
                return;
            }
            player.sendMessage(messageToPlayer);
        }

        private Component parseMessage(String formattedMessage) {
            List<Component> components = new ArrayList<>();
            int currentIndex = 0;

            String globalHoverText = null;
            String globalClickEvent = null;

            while (currentIndex < formattedMessage.length()) {
                int buttonStart = formattedMessage.indexOf(BUTTON_PREFIX, currentIndex);

                if (buttonStart == -1) {
                    String remainingText = formattedMessage.substring(currentIndex);

                    if (!remainingText.isEmpty()) {
                        globalHoverText = extractValue(remainingText, HOVER_TEXT_PREFIX);
                        globalClickEvent = extractValue(remainingText, CLICK_EVENT_PREFIX);

                        remainingText = extractMessage(remainingText);
                        if (!remainingText.isEmpty()) {
                            components.add(LegacyComponentSerializer.legacySection().deserialize(remainingText));
                        }
                    }
                    break;
                }

                if (buttonStart > currentIndex) {
                    String beforeButton = formattedMessage.substring(currentIndex, buttonStart);

                    globalHoverText = extractValue(beforeButton, HOVER_TEXT_PREFIX);
                    globalClickEvent = extractValue(beforeButton, CLICK_EVENT_PREFIX);

                    beforeButton = extractMessage(beforeButton);
                    if (!beforeButton.isEmpty()) {
                        components.add(LegacyComponentSerializer.legacySection().deserialize(beforeButton));
                    }
                }

                int buttonEnd = findClosingBracket(formattedMessage, buttonStart + BUTTON_PREFIX.length());
                if (buttonEnd == -1) {
                    throw new IllegalArgumentException("Некорректный формат кнопки: отсутствует закрывающая }");
                }

                String buttonContent = formattedMessage.substring(buttonStart + BUTTON_PREFIX.length(), buttonEnd);
                Component buttonComponent = parseButtonContent(buttonContent);

                boolean hasLeadingSpace = buttonStart > 0 && formattedMessage.charAt(buttonStart - 1) == ' ';
                boolean hasTrailingSpace = buttonEnd + 1 < formattedMessage.length() && formattedMessage.charAt(buttonEnd + 1) == ' ';

                if (hasLeadingSpace) {
                    components.add(Component.text(" "));
                }

                components.add(buttonComponent);

                if (hasTrailingSpace) {
                    components.add(Component.text(" "));
                }

                currentIndex = buttonEnd + 1;
            }

            Component finalComponent = Component.text().append(components).build();

            if (globalHoverText != null) {
                finalComponent = createHoverEvent(finalComponent, globalHoverText);
            }
            if (globalClickEvent != null) {
                finalComponent = createClickEvent(finalComponent, globalClickEvent);
            }

            return finalComponent;
        }

        private int findClosingBracket(String message, int startIndex) {
            int depth = 0;
            char[] chars = message.toCharArray();
            for (int i = startIndex; i < chars.length; i++) {
                char currentChar = chars[i];
                if (currentChar == '{') {
                    depth++;
                } else if (currentChar == '}') {
                    if (depth == 0) {
                        return i;
                    }
                    depth--;
                }
            }

            return -1;
        }

        private Component parseButtonContent(String buttonContent) {
            String buttonText = null;
            String hoverText = null;
            String clickEvent = null;

            List<String> parts = getParts(buttonContent);

            for (int i = 0; i < parts.size(); i++) {
                String part = parts.get(i);
                if (part.startsWith(HOVER_TEXT_PREFIX)) {
                    hoverText = extractValue(part, HOVER_TEXT_PREFIX);
                } else if (part.startsWith(CLICK_EVENT_PREFIX)) {
                    clickEvent = extractValue(part, CLICK_EVENT_PREFIX);
                } else {
                    if (buttonText == null) {
                        buttonText = part;
                    } else {
                        throw new IllegalArgumentException("Некорректный формат кнопки: несколько текстовых частей.");
                    }
                }
            }

            if (buttonText == null || buttonText.isEmpty()) {
                throw new IllegalArgumentException("Кнопка должна содержать текст.");
            }

            Component buttonComponent = LegacyComponentSerializer.legacySection().deserialize(buttonText);

            if (hoverText != null) {
                buttonComponent = createHoverEvent(buttonComponent, hoverText);
            }

            if (clickEvent != null) {
                buttonComponent = createClickEvent(buttonComponent, clickEvent);
            }

            return buttonComponent;
        }

        private List<String> getParts(String buttonContent) {
            List<String> parts = new ArrayList<>();
            int start = 0;
            int depth = 0;
            char[] chars = buttonContent.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                } else if (c == ';' && depth == 0) {
                    parts.add(buttonContent.substring(start, i).trim());
                    start = i + 1;
                }
            }
            parts.add(buttonContent.substring(start).trim());
            return parts;
        }

        private String extractValue(String message, String prefix) {
            int startIndex = message.indexOf(prefix);
            if (startIndex != -1) {
                startIndex += prefix.length();
                int endIndex = findClosingBracket(message, startIndex);
                if (endIndex != -1) {
                    return message.substring(startIndex, endIndex);
                }
            }
            return null;
        }

        private String extractMessage(String message) {
            String baseMessage = getBaseMessage(message);

            for (int i = 0; i < HOVER_MARKERS.length; i++) {
                String marker = HOVER_MARKERS[i];
                int startIndex = message.indexOf(marker);
                if (startIndex != -1) {
                    int endIndexMarker = findClosingBracket(message, startIndex + marker.length() - 1);
                    if (endIndexMarker != -1) {
                        message = message.substring(0, startIndex).trim() + " " + message.substring(endIndexMarker + 1).trim();
                    }
                }
            }

            return baseMessage.trim();
        }

        private String getBaseMessage(String message) {
            IntList indices = new IntArrayList();
            for (int i = 0; i < HOVER_MARKERS.length; i++) {
                String marker = HOVER_MARKERS[i];
                int index = message.indexOf(marker);
                if (index != -1) {
                    indices.add(index);
                }
            }
            int endIndex = indices.isEmpty() ? message.length() : Collections.min(indices);

            return message.substring(0, endIndex).trim();
        }

        private Component createHoverEvent(Component message, String hoverText) {
            HoverEvent<Component> hover = HoverEvent.showText(LegacyComponentSerializer.legacySection().deserialize(hoverText));
            return message.hoverEvent(hover);
        }

        private Component createClickEvent(Component message, String clickEvent) {
            int separatorIndex = clickEvent.indexOf(';');
            if (separatorIndex == -1) {
                throw new IllegalArgumentException("Некорректный формат clickEvent: отсутствует разделитель ';'");
            }

            String actionStr = clickEvent.substring(0, separatorIndex).trim();
            String context = clickEvent.substring(separatorIndex + 1).trim();

            ClickEvent.Action action = ClickEvent.Action.valueOf(actionStr.toUpperCase(Locale.ENGLISH));
            ClickEvent click = ClickEvent.clickEvent(action, context);

            return message.clickEvent(click);
        }
    }
}
