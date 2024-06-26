package ru.overwrite.rtp.channels;

import ru.overwrite.rtp.actions.Action;

import java.util.List;
import java.util.Map;

public record ChannelActions(
        List<Action> preTeleportActions,
        Map<Integer, List<Action>> onCooldownActions,
        List<Action> afterTeleportActions) {
}
