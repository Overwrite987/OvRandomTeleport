package ru.overwrite.rtp.channels.settings;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.bukkit.configuration.ConfigurationSection;
import ru.overwrite.rtp.OvRandomTeleport;
import ru.overwrite.rtp.actions.Action;
import ru.overwrite.rtp.actions.ActionRegistry;
import ru.overwrite.rtp.utils.Utils;

import java.util.List;
import java.util.Objects;

public record Actions(
        List<Action> preTeleportActions,
        Int2ObjectMap<List<Action>> onCooldownActions,
        List<Action> afterTeleportActions
) {

    private static final Actions EMPTY_ACTIONS = new Actions(
            List.of(),
            Int2ObjectMaps.emptyMap(),
            List.of()
    );

    public static Actions create(OvRandomTeleport plugin, ConfigurationSection actions) {
        if (actions == null) {
            return EMPTY_ACTIONS;
        }

        ActionRegistry actionRegistry = plugin.getRtpManager().getActionRegistry();

        List<Action> preTeleportActions = getActionList(plugin, actionRegistry, actions.getStringList("pre_teleport"));

        Int2ObjectMap<List<Action>> onCooldownActions = new Int2ObjectOpenHashMap<>();
        ConfigurationSection cdSection = actions.getConfigurationSection("on_cooldown");
        if (cdSection != null) {
            for (String key : cdSection.getKeys(false)) {
                if (!Utils.isNumeric(key)) {
                    continue;
                }
                int time = Integer.parseInt(key);
                List<Action> list = getActionList(plugin, actionRegistry, cdSection.getStringList(key));
                onCooldownActions.put(time, list);
            }
        }

        List<Action> afterTeleportActions = getActionList(plugin, actionRegistry, actions.getStringList("after_teleport"));

        return new Actions(preTeleportActions, onCooldownActions, afterTeleportActions);
    }

    private static List<Action> getActionList(OvRandomTeleport plugin, ActionRegistry actionRegistry, List<String> actionStrings) {
        if (actionStrings.isEmpty()) {
            return List.of();
        }
        ImmutableList.Builder<Action> builder = ImmutableList.builder();
        for (String actionStr : actionStrings) {
            try {
                builder.add(Objects.requireNonNull(actionRegistry.resolveAction(actionStr), "Type doesn't exist"));
            } catch (Exception ex) {
                plugin.getSLF4JLogger().warn("Couldn't create action for string '{}'", actionStr, ex);
            }
        }
        return builder.build();
    }
}