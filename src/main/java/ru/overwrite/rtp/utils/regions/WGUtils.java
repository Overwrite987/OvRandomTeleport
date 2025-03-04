package ru.overwrite.rtp.utils.regions;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.BooleanFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import lombok.experimental.UtilityClass;
import org.bukkit.Location;

@UtilityClass
public final class WGUtils {

    public BooleanFlag RTP_IGNORE_FLAG = new BooleanFlag("rtp-base-no-teleport");

    public void setupRtpFlag() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            registry.register(RTP_IGNORE_FLAG);
        } catch (FlagConflictException ex) {
            Flag<?> existing = registry.get("rtp-base-no-teleport");
            if (existing instanceof BooleanFlag flag) {
                RTP_IGNORE_FLAG = flag;
            }
        }
    }

    public ApplicableRegionSet getApplicableRegions(Location location) {
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(location.getWorld()));
        if (regionManager == null || regionManager.getRegions().isEmpty())
            return null;
        return regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
    }

}
