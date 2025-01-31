package ru.overwrite.rtp.channels;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.overwrite.rtp.channels.settings.*;

public record ChannelTemplate(
        @NotNull String id,
        @Nullable Costs costs,
        @Nullable LocationGenOptions locationGenOptions,
        @Nullable Cooldown cooldown,
        @Nullable Bossbar bossbar,
        @Nullable Particles particles,
        @Nullable Restrictions restrictions,
        @Nullable Avoidance avoidance,
        @Nullable Actions actions) {
}
