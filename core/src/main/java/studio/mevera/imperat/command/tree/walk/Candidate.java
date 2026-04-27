package studio.mevera.imperat.command.tree.walk;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.tree.ParseResult;
import studio.mevera.imperat.command.tree.ParsedNode;
import studio.mevera.imperat.context.CommandSource;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A single root-to-terminal branch surviving the tree walk. Shared by
 * {@link TreeParser} (execution) and {@link TreeSuggester} (tab-completion);
 * package-private so it never leaks into the public API.
 */
record Candidate<S extends CommandSource>(
        List<ParsedNode<S>> chain,
        Map<String, ParseResult<S>> trailingFlagResults,
        int consumedIndex,
        @Nullable Command<S> command,
        @Nullable CommandPathway<S> pathway
) {

    static <S extends CommandSource> Candidate<S> completion(List<ParsedNode<S>> chain, int consumedIndex) {
        return new Candidate<>(chain, Collections.emptyMap(), consumedIndex, null, null);
    }
}
