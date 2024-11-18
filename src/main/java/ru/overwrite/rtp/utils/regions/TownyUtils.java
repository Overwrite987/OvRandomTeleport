package ru.overwrite.rtp.utils.regions;

import org.bukkit.Location;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;

public final class TownyUtils {

    private static final TownyAPI api = TownyAPI.getInstance();

    public static Town getTownByLocation(Location loc) {
        return api.getTown(loc);
    }

}
