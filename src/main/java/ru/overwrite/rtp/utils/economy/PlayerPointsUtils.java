package ru.overwrite.rtp.utils.economy;

import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class PlayerPointsUtils {

    private PlayerPointsUtils() {}

    private static final PlayerPointsAPI API = PlayerPoints.getInstance().getAPI();

    public static void withdraw(Player player, int amount) {
        UUID uuid = player.getName();
        API.take(uuid, amount);
    }

    public static void deposit(Player player, int amount) {
        UUID uuid = player.getName();
        API.give(uuid, amount);
    }

    public static int getBalance(Player player) {
        return API.look(player.getName());
    }
}
