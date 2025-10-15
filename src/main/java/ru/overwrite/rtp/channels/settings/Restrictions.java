package ru.overwrite.rtp.channels.settings;

import org.bukkit.configuration.ConfigurationSection;

public record Restrictions(
        boolean restrictMove,
        boolean restrictTeleport,
        boolean restrictDamage,
        boolean restrictDamageOthers,
        boolean damageCheckOnlyPlayers
) {

    private static final Restrictions EMPTY_RESTRICTIONS = new Restrictions(
            false,
            false,
            false,
            false,
            false
    );


    public static Restrictions create(ConfigurationSection restrictions) {
        if (restrictions == null) {
            return EMPTY_RESTRICTIONS;
        }

        return new Restrictions(
                restrictions.getBoolean("move", false),
                restrictions.getBoolean("teleport", false),
                restrictions.getBoolean("damage", false),
                restrictions.getBoolean("damage_others", false),
                restrictions.getBoolean("damage_check_only_players", false)
        );
    }
}