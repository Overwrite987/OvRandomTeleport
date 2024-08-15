package ru.overwrite.rtp.actions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.overwrite.rtp.Main;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActionRegistry {
    private static final Pattern ACTION_PATTERN = Pattern.compile("\\[(\\w+)] ?(.*)");

    private final Main rtpPlugin;
    private final Map<String, ActionType> types;

    public ActionRegistry(Main rtpPlugin) {
        this.rtpPlugin = rtpPlugin;
        this.types = new HashMap<>();
    }

    public void register(@NotNull ActionType type) {
        if (types.put(type.key().toString(), type) != null) {
            rtpPlugin.getSLF4JLogger().warn("Type '{}' was overridden with '{}'", type.key(), type.getClass().getName());
        }
        types.putIfAbsent(type.key().value(), type);
    }

    public @Nullable ActionType getType(@NotNull String typeStr) {
        return types.get(typeStr.toLowerCase(Locale.ROOT));
    }

    public @Nullable ActionInstance resolveAction(@NotNull String actionStr) {
        Matcher matcher = ACTION_PATTERN.matcher(actionStr);
        if (!matcher.matches()) return null;
        ActionType type = getType(matcher.group(1));
        if (type == null) return null;
        return type.instance(matcher.group(2), rtpPlugin);
    }
}
