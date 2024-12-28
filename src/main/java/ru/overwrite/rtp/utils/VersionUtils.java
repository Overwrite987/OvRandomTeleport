package ru.overwrite.rtp.utils;

import org.bukkit.Bukkit;

public final class VersionUtils {

    private VersionUtils() {}

    public static final int SUB_VERSION = Integer.parseInt(Bukkit.getBukkitVersion().split("-")[0].split("\\.")[1]);

    public static final int VOID_LEVEL = SUB_VERSION >= 18 ? -64 : 0;

}
