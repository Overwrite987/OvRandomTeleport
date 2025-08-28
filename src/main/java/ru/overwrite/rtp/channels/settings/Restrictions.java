package ru.overwrite.rtp.channels.settings;

import org.bukkit.configuration.ConfigurationSection;
import ru.overwrite.rtp.channels.Settings;
import ru.overwrite.rtp.configuration.Config;

public record Restrictions(
        boolean restrictMove,
        boolean restrictTeleport,
        boolean restrictDamage,
        boolean restrictDamageOthers,
        boolean damageCheckOnlyPlayers) {

    public static Restrictions create(ConfigurationSection restrictions, Settings template, Config pluginConfig, boolean applyTemplate) {
        if (pluginConfig.isNullSection(restrictions) && !applyTemplate) {
            return null;
        }

        Restrictions templateRestrictions = template != null ? template.restrictions() : null;
        boolean hasTemplateRestrictions = templateRestrictions != null;

        boolean restrictMove = restrictions.getBoolean("move", hasTemplateRestrictions && templateRestrictions.restrictMove());
        boolean restrictTeleport = restrictions.getBoolean("teleport", hasTemplateRestrictions && templateRestrictions.restrictTeleport());
        boolean restrictDamage = restrictions.getBoolean("damage", hasTemplateRestrictions && templateRestrictions.restrictDamage());
        boolean restrictDamageOthers = restrictions.getBoolean("damage_others", hasTemplateRestrictions && templateRestrictions.restrictDamageOthers());
        boolean damageCheckOnlyPlayers = restrictions.getBoolean("damage_check_only_players", hasTemplateRestrictions && templateRestrictions.damageCheckOnlyPlayers());

        return new Restrictions(
                restrictMove,
                restrictTeleport,
                restrictDamage,
                restrictDamageOthers,
                damageCheckOnlyPlayers
        );
    }
}
