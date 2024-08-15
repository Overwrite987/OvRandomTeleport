package ru.overwrite.rtp.actions.impl;

import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import ru.overwrite.rtp.Main;
import ru.overwrite.rtp.actions.ActionInstance;
import ru.overwrite.rtp.actions.ActionType;
import ru.overwrite.rtp.channels.Channel;

import java.util.function.UnaryOperator;

public class EffectAction implements ActionType {
    private static final Key KEY = Key.key("ovrandomteleport:effect");

    private static final int POTION_INDEX = 0;
    private static final int DURATION_INDEX = 1;
    private static final int AMPLIFIER_INDEX = 2;

    @Override
    public @NotNull ActionInstance instance(@NotNull String context, @NotNull Main rtpPlugin) {
        String[] effectArgs = context.split(";");
        int length = effectArgs.length;

        return new Instance(new PotionEffect(
                PotionEffectType.getByName(effectArgs[POTION_INDEX]),
                (effectArgs.length > DURATION_INDEX) ? Integer.parseInt(effectArgs[DURATION_INDEX]) : 1,
                (effectArgs.length > AMPLIFIER_INDEX) ? Integer.parseInt(effectArgs[AMPLIFIER_INDEX]) : 1
        ));
    }

    @Override
    public @NotNull Key key() {
        return KEY;
    }

    private record Instance(
            @NotNull PotionEffect effect
    ) implements ActionInstance {
        @Override
        public void perform(@NotNull Channel channel, @NotNull Player player, @NotNull UnaryOperator<String> placeholders) {
            player.addPotionEffect(effect);
        }
    }
}