package ru.overwrite.rtp.channels.settings;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.bukkit.configuration.ConfigurationSection;
import ru.overwrite.rtp.OvRandomTeleport;
import ru.overwrite.rtp.actions.Action;
import ru.overwrite.rtp.actions.ActionRegistry;
import ru.overwrite.rtp.channels.Settings;
import ru.overwrite.rtp.configuration.Config;
import ru.overwrite.rtp.utils.Utils;

import java.util.List;
import java.util.Objects;

public record Actions(
        List<Action> preTeleportActions,
        Int2ObjectMap<List<Action>> onCooldownActions,
        List<Action> afterTeleportActions) {

    private static final Actions EMPTY_ACTIONS = new Actions(
            null,
            null,
            null
    );

    public static Actions create(OvRandomTeleport plugin, ConfigurationSection actions, Settings template, Config pluginConfig, boolean applyTemplate) {
        if (pluginConfig.isNullSection(actions)) {
            if (!applyTemplate) {
                return null;
            }
            return EMPTY_ACTIONS;
        }

        Actions templateActions = template != null ? template.actions() : null;
        boolean hasTemplateActions = templateActions != null;

        ActionRegistry actionRegistry = plugin.getRtpManager().getActionRegistry();

        List<Action> preTeleportActions = actions.contains("pre_teleport")
                ? getActionList(plugin, actionRegistry, actions.getStringList("pre_teleport"))
                : hasTemplateActions ? templateActions.preTeleportActions() : List.of();

        Int2ObjectMap<List<Action>> onCooldownActions = new Int2ObjectOpenHashMap<>();
        if (actions.contains("on_cooldown")) {
            ConfigurationSection cdSection = actions.getConfigurationSection("on_cooldown");
            if (!pluginConfig.isNullSection(cdSection)) {
                for (String key : cdSection.getKeys(false)) {
                    if (!Utils.isNumeric(key)) {
                        continue;
                    }
                    int time = Integer.parseInt(key);
                    List<Action> list = getActionList(plugin, actionRegistry, cdSection.getStringList(key));
                    onCooldownActions.put(time, list);
                }
            }
        } else if (hasTemplateActions) {
            onCooldownActions.putAll(templateActions.onCooldownActions());
        }

        List<Action> afterTeleportActions = actions.contains("after_teleport")
                ? getActionList(plugin, actionRegistry, actions.getStringList("after_teleport"))
                : hasTemplateActions ? templateActions.afterTeleportActions() : List.of();

        return new Actions(preTeleportActions, onCooldownActions, afterTeleportActions);
    }

    private static ImmutableList<Action> getActionList(OvRandomTeleport plugin, ActionRegistry actionRegistry, List<String> actionStrings) {
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
