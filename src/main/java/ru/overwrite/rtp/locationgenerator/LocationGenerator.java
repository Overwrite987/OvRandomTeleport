package ru.overwrite.rtp.locationgenerator;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import ru.overwrite.rtp.channels.Settings;

public interface LocationGenerator {

    Location generateRandomLocation(Player player, Settings settings, World world);

    Location generateRandomLocationNearPlayer(Player player, Settings settings, World world);

    Location generateRandomLocationNearRandomRegion(Player player, Settings settings, World world);

}