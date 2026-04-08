package studio.mevera.imperat.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.context.CommandSource;

public final class UsageFormatting {

    private UsageFormatting() {
    }

    public static @NotNull String formatInput(@Nullable String prefix, @NotNull String label, @Nullable String suffix) {
        StringBuilder builder = new StringBuilder();
        if (prefix != null && !prefix.isEmpty()) {
            builder.append(prefix);
        }
        builder.append(label);
        if (suffix != null && !suffix.isBlank()) {
            builder.append(' ').append(suffix);
        }
        return builder.toString();
    }

    public static <S extends CommandSource> @NotNull String formatClosestUsage(
            @Nullable String prefix,
            @NotNull String label,
            @Nullable CommandPathway<S> pathway
    ) {
        return formatInput(prefix, label, pathway == null ? null : pathway.formatted());
    }
}
