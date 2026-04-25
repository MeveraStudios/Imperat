package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.context.CommandSource;

import java.util.Collections;
import java.util.List;

public record CommandTreeMatch<S extends CommandSource>(
        @NotNull List<ParsedNode<S>> parsedNodes,
        @NotNull Command<S> command,
        @Nullable CommandPathway<S> pathway
) {

    public CommandTreeMatch {
        parsedNodes = List.copyOf(parsedNodes);
    }

    public static <S extends CommandSource> CommandTreeMatch<S> empty(@NotNull Command<S> command) {
        return new CommandTreeMatch<>(Collections.emptyList(), command, null);
    }
}
