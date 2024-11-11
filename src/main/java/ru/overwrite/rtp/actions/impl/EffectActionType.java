package ru.overwrite.rtp.actions.impl;

import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import ru.overwrite.rtp.Main;
import ru.overwrite.rtp.actions.Action;
import ru.overwrite.rtp.actions.ActionType;
import ru.overwrite.rtp.channels.Channel;

public class EffectActionType implements ActionType {

    private static final Key KEY = Key.key("ovrandomteleport:effect");

    private static final int POTION_INDEX = 0;
    private static final int AMPLIFIER_INDEX = 1;
    private static final int DURATION_INDEX = 2;

    @Override
    public @NotNull Action instance(@NotNull String context, @NotNull Main plugin) {
        String[] effectArgs = context.split(";");
        int length = effectArgs.length;

        return new EffectAction(new PotionEffect(
                PotionEffectType.getByName(effectArgs[POTION_INDEX]),
                (length > AMPLIFIER_INDEX) ? Integer.parseInt(effectArgs[AMPLIFIER_INDEX]) : 1,
                (length > DURATION_INDEX) ? Integer.parseInt(effectArgs[DURATION_INDEX]) : 1
        ));
    }

    @Override
    public @NotNull Key key() {
        return KEY;
    }

    private record EffectAction(
            @NotNull PotionEffect effect
    ) implements Action {
        @Override
        public void perform(@NotNull Channel channel, @NotNull Player player, @NotNull String[] searchList, @NotNull String[] replacementList) {
            player.addPotionEffect(effect);
        }
    }
}
