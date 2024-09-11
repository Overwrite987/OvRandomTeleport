package ru.overwrite.rtp.channels.settings;

public record Messages(
        String noPermsMessage,
        String invalidWorldMessage,
        String notEnoughPlayersMessage,
        String notEnoughMoneyMessage,
        String notEnoughHungerMessage,
        String notEnoughExpMessage,
        String cooldownMessage,
        String movedOnTeleportMessage,
        String teleportedOnTeleportMessage,
        String damagedOnTeleportMessage,
        String damagedOtherOnTeleportMessage,
        String failToFindLocationMessage,
        String alreadyTeleportingMessage) {
}
