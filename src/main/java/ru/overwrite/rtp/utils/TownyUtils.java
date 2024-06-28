package ru.overwrite.rtp.utils;

import org.bukkit.Location;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;

public class TownyUtils {

    private static final TownyAPI api = TownyAPI.getInstance();

    public static Town getTownByLocation(Location loc) {
        return api.getTown(loc);
    }

}
