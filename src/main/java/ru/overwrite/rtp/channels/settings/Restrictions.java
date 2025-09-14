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

    private static final Restrictions EMPTY_RESTRICTIONS = new Restrictions(
            false,
            false,
            false,
            false,
            false
    );


    public static Restrictions create(ConfigurationSection restrictions, Settings template, Config pluginConfig, boolean applyTemplate) {
        if (pluginConfig.isNullSection(restrictions)) {
            if (!applyTemplate) {
                return null;
            }
            return EMPTY_RESTRICTIONS;
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
