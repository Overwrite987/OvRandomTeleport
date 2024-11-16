package ru.overwrite.rtp.channels.settings;

public record Messages(
        String noPerms,
        String invalidWorld,
        String notEnoughPlayers,
        String notEnoughMoney,
        String notEnoughHunger,
        String notEnoughExp,
        String cooldown,
        String movedOnTeleport,
        String teleportedOnTeleport,
        String damagedOnTeleport,
        String damagedOtherOnTeleport,
        String failToFindLocation) {
}
