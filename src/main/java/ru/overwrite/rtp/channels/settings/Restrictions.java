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

        boolean isNullSection = pluginConfig.isNullSection(restrictions);

        Restrictions templateRestrictions = template != null ? template.restrictions() : null;
        boolean hasTemplateRestrictions = templateRestrictions != null;

        if (isNullSection) {
            if (!applyTemplate) {
                return null;
            }
            if (!hasTemplateRestrictions) {
                return EMPTY_RESTRICTIONS;
            }
        }

        boolean restrictMove = hasTemplateRestrictions && templateRestrictions.restrictMove();
        boolean restrictTeleport = hasTemplateRestrictions && templateRestrictions.restrictTeleport();
        boolean restrictDamage = hasTemplateRestrictions && templateRestrictions.restrictDamage();
        boolean restrictDamageOthers = hasTemplateRestrictions && templateRestrictions.restrictDamageOthers();
        boolean damageCheckOnlyPlayers = hasTemplateRestrictions && templateRestrictions.damageCheckOnlyPlayers();

        if (!isNullSection) {
            restrictMove = restrictions.getBoolean("move", restrictMove);
            restrictTeleport = restrictions.getBoolean("teleport", restrictTeleport);
            restrictDamage = restrictions.getBoolean("damage", restrictDamage);
            restrictDamageOthers = restrictions.getBoolean("damage_others", restrictDamageOthers);
            damageCheckOnlyPlayers = restrictions.getBoolean("damage_check_only_players", damageCheckOnlyPlayers);
        }

        return new Restrictions(
                restrictMove,
                restrictTeleport,
                restrictDamage,
                restrictDamageOthers,
                damageCheckOnlyPlayers
        );
    }
}
