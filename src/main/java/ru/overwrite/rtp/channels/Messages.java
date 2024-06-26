package ru.overwrite.rtp.channels;

public record Messages(
        String noPermsMessage,
        String invalidWorldMessage,
        String notEnoughPlayersMessage,
        String notEnoughMoneyMessage,
        String cooldownMessage,
        String movedOnTeleportMessage,
        String teleportedOnTeleportMessage,
        String damagedOnTeleportMessage,
        String damagedOtherOnTeleportMessage,
        String failToFindLocationMessage,
        String alreadyTeleportingMessage) {
}
