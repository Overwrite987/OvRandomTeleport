package ru.overwrite.rtp.channels;

public record ChannelMessages(
        String noPermsMessage,
        String invalidWorldMessage,
        String notEnoughPlayersMessage,
        String notEnoughMoneyMessage,
        String cooldownMessage,
        String movedOnTeleportMessage,
        String damagedOnTeleportMessage,
        String failToFindLocationMessage,
        String alreadyTeleportingMessage) {
}
