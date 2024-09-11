package ru.overwrite.rtp.channels.settings;

public record Restrictions(
        boolean restrictMove,
        boolean restrictTeleport,
        boolean restrictDamage,
        boolean restrictDamageOthers,
        boolean damageCheckOnlyPlayers) {
}
