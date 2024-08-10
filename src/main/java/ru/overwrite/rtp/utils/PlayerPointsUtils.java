package ru.overwrite.rtp.utils;

import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PlayerPointsUtils {

    private static final PlayerPointsAPI ppAPI = PlayerPoints.getInstance().getAPI();

    public static void withdraw(Player p, int amount) {
        UUID uuid = p.getUniqueId();
        ppAPI.take(uuid, amount);
    }

    public static void deposit(Player p, int amount) {
        UUID uuid = p.getUniqueId();
        ppAPI.give(uuid, amount);
    }

    public static int getBalance(Player p) {
        return ppAPI.look(p.getUniqueId());
    }
}
