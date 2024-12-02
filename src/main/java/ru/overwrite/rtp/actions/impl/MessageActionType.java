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
import org.jetbrains.annotations.Nullable;
import ru.overwrite.rtp.Main;
import ru.overwrite.rtp.actions.Action;
import ru.overwrite.rtp.actions.ActionType;
import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.utils.Utils;

import java.util.Collections;

public final class MessageActionType implements ActionType {

    private static final Key KEY = Key.key("ovrandomteleport:message");

    private static final String HOVER_TEXT_PREFIX = "hoverText={";
    private static final String CLICK_EVENT_PREFIX = "clickEvent={";
    private static final String SUFFIX = "}";
    private static final String[] HOVER_MARKERS = new String[]{HOVER_TEXT_PREFIX, CLICK_EVENT_PREFIX};

    @Override
    public @NotNull Action instance(@NotNull String context, @NotNull Main plugin) {
        String text = Utils.COLORIZER.colorize(context);
        String message = extractMessage(text, HOVER_MARKERS);
        String hoverTextString = extractValue(text, HOVER_TEXT_PREFIX, SUFFIX);
        Component hoverText = hoverTextString != null ? LegacyComponentSerializer.legacySection().deserialize(hoverTextString) : null;
        String clickEventText = extractValue(text, CLICK_EVENT_PREFIX, SUFFIX);
        String[] clickEvent =  clickEventText != null ? clickEventText.split(";", 2) : null;
        return new MessageAction(
                message,
                hoverText,
                clickEvent
        );
    }

    public static String extractMessage(String message, String[] markers) {
        IntList indices = new IntArrayList();
        for (String marker : markers) {
            int index = message.indexOf(marker);
            if (index != -1) {
                indices.add(index);
            }
        }
        int endIndex = indices.isEmpty() ? message.length() : Collections.min(indices);
        return message.substring(0, endIndex).trim();
    }

    public static String extractValue(String message, String prefix, String suffix) {
        int startIndex = message.indexOf(prefix);
        if (startIndex != -1) {
            startIndex += prefix.length();
            int endIndex = message.indexOf(suffix, startIndex);
            if (endIndex != -1) {
                return message.substring(startIndex, endIndex);
            }
        }
        return null;
    }

    @Override
    public @NotNull Key key() {
        return KEY;
    }

    private record MessageAction(
            @NotNull String message,
            @Nullable Component hoverText,
            @Nullable String[] clickEvent
    ) implements Action {
        @Override
        public void perform(@NotNull Channel channel, @NotNull Player player, @NotNull String[] searchList, @NotNull String[] replacementList) {
            String messageToPlayer = Utils.replaceEach(message, searchList, replacementList);
            if (hoverText == null && clickEvent == null) {
                player.sendMessage(messageToPlayer);
                return;
            }
            Component component = LegacyComponentSerializer.legacySection().deserialize(messageToPlayer);
            if (hoverText != null) {
                component = createHoverText(component, hoverText);
            }
            if (clickEvent != null) {
                component = createClickEvent(component, clickEvent);
            }
            player.sendMessage(component);
        }

        private Component createHoverText(Component message, Component hoverText) {
            HoverEvent<Component> hover = HoverEvent.showText(hoverText);
            return message.hoverEvent(hover);
        }

        public Component createClickEvent(Component message, String[] clickEvent) {
            ClickEvent.Action action = ClickEvent.Action.valueOf(clickEvent[0].toUpperCase(Locale.ROOT));
            String context = clickEvent[1];
            ClickEvent click = ClickEvent.clickEvent(action, context);
            return message.clickEvent(click);
        }
    }
}
