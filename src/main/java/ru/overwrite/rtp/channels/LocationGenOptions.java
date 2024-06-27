package ru.overwrite.rtp.channels;

public record LocationGenOptions(
        Shape shape,
        int minX,
        int maxX,
        int minZ,
        int maxZ,
        int radiusMin,
        int radiusMax,
        int maxLocationAttempts) {

    public enum Shape {
        SQUARE,
        ROUND
    }
}
