package ru.overwrite.rtp.utils;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;

@UtilityClass
public final class VersionUtils {

    public final int SUB_VERSION = Integer.parseInt(Bukkit.getBukkitVersion().split("-")[0].split("\\.")[1]);

    public final int VOID_LEVEL = SUB_VERSION >= 18 ? -64 : 0;

}
