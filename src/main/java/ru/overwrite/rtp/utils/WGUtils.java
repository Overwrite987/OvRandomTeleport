package ru.overwrite.rtp.utils;

import java.util.List;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionType;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import ru.overwrite.rtp.channels.Channel;
import ru.overwrite.rtp.channels.settings.LocationGenOptions;

public class WGUtils {

    private static final FastRandom random = new FastRandom();
    public static StateFlag RTP_IGNORE_FLAG;

    public static void setupRtpFlag() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            StateFlag flag = new StateFlag("rtp-base-no-teleport", true);
            registry.register(flag);
            RTP_IGNORE_FLAG = flag;
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get("rtp-base-no-teleport");
            if (existing instanceof StateFlag) {
                RTP_IGNORE_FLAG = (StateFlag) existing;
            }
        }
    }

    public static ApplicableRegionSet getApplicableRegions(Location location) {
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(location.getWorld()));
        if (regionManager == null || regionManager.getRegions().isEmpty())
            return null;
        return regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
    }

}
