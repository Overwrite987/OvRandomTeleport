package ru.overwrite.rtp.utils.regions;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import lombok.experimental.UtilityClass;
import org.bukkit.Location;

@UtilityClass
public final class TownyUtils {

    private final TownyAPI API = TownyAPI.getInstance();

    public Town getTownByLocation(Location loc) {
        return API.getTown(loc);
    }

}
