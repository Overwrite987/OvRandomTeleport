package ru.overwrite.rtp.channels.settings;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import ru.overwrite.rtp.actions.Action;

import java.util.List;

public record Actions(
        List<Action> preTeleportActions,
        Int2ObjectMap<List<Action>> onCooldownActions,
        List<Action> afterTeleportActions) {
}
