package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.context.CommandSource;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The result of resolving an input against a command tree. Carries everything
 * the execution-context drain needs in order to materialise typed values,
 * without having to re-parse the raw input:
 * <ul>
 *   <li>{@link #parsedNodes()} — the chain of nodes the parser took, each one
 *       holding the per-argument {@link ParseResult}s captured during the walk.</li>
 *   <li>{@link #trailingFlagResults()} — flags that appeared after the last
 *       positional/optional token consumed by the chain. Kept on a separate
 *       channel so they can carry parse errors without polluting the tree's
 *       failure-penalty scoring (which is positional-arg sensitive).</li>
 *   <li>{@link #consumedIndex()} — the index (into the raw {@code ArgumentInput})
 *       of the last token any branch of this match consumed. Anything beyond it
 *       is true trailing input and should fail with an invalid-syntax error
 *       unless ignored by greedy-limit or trailing-flag rules.</li>
 * </ul>
 */
public record CommandTreeMatch<S extends CommandSource>(
        @NotNull List<ParsedNode<S>> parsedNodes,
        @NotNull Map<String, ParseResult<S>> trailingFlagResults,
        @NotNull Command<S> command,
        @Nullable CommandPathway<S> pathway,
        int consumedIndex
) {

    public CommandTreeMatch {
        parsedNodes = List.copyOf(parsedNodes);
        trailingFlagResults = Map.copyOf(trailingFlagResults);
    }

    /**
     * Convenience constructor that defaults the trailing-flag channel and
     * consumed-index to "nothing was consumed" — used by the execute path
     * before the additional metadata existed and preserved here so existing
     * callers keep compiling.
     */
    public CommandTreeMatch(
            @NotNull List<ParsedNode<S>> parsedNodes,
            @NotNull Command<S> command,
            @Nullable CommandPathway<S> pathway
    ) {
        this(parsedNodes, Collections.emptyMap(), command, pathway, -1);
    }

    public static <S extends CommandSource> CommandTreeMatch<S> empty(@NotNull Command<S> command) {
        return new CommandTreeMatch<>(Collections.emptyList(), Collections.emptyMap(), command, null, -1);
    }
}
