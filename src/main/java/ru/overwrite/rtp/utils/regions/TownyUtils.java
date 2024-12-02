package ru.overwrite.rtp.utils.regions;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Location;

public final class TownyUtils {

    private TownyUtils() {}

    private static final TownyAPI API = TownyAPI.getInstance();

    public static Town getTownByLocation(Location loc) {
        return API.getTown(loc);
    }

}
