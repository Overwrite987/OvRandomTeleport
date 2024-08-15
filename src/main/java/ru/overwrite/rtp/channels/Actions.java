package ru.overwrite.rtp.channels;

import ru.overwrite.rtp.actions.ActionInstance;

import java.util.List;
import java.util.Map;

public record Actions(
        List<ActionInstance> preTeleportActions,
        Map<Integer, List<ActionInstance>> onCooldownActions,
        List<ActionInstance> afterTeleportActions) {
}
