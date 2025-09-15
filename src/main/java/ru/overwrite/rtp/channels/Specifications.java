package ru.overwrite.rtp.channels;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.bukkit.configuration.ConfigurationSection;
import ru.overwrite.rtp.utils.VersionUtils;

import java.util.*;

public record Specifications(Set<String> joinChannels,
                             Map<String, List<String>> voidChannels,
                             Object2IntMap<String> voidLevels,
                             Map<String, List<String>> respawnChannels) {

    public static Specifications create() {
        return new Specifications(new HashSet<>(), new HashMap<>(), new Object2IntOpenHashMap<>(), new HashMap<>());
    }

    public void clearAll() {
        this.joinChannels.clear();
        this.voidChannels.clear();
        this.voidLevels.clear();
        this.respawnChannels.clear();
    }

    public void assign(Channel newChannel, ConfigurationSection section) {
        if (section == null) {
            return;
        }
        if (section.getBoolean("teleport_on_first_join", false)) {
            joinChannels.add(newChannel.id());
        }
        List<String> voidWorlds = section.getStringList("void_worlds");
        if (!voidWorlds.isEmpty()) {
            voidChannels.put(newChannel.id(), voidWorlds);
        }
        int voidLevel = section.getInt("voidLevel");
        if (voidLevel != VersionUtils.VOID_LEVEL && voidChannels.containsKey(newChannel.id())) {
            voidLevels.put(newChannel.id(), section.getInt("voidLevel"));
        }
        List<String> respawnWorlds = section.getStringList("respawn_worlds");
        if (!respawnWorlds.isEmpty()) {
            respawnChannels.put(newChannel.id(), respawnWorlds);
        }
    }
}