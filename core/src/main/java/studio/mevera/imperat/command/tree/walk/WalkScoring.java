package studio.mevera.imperat.command.tree.walk;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.tree.ParseResult;
import studio.mevera.imperat.command.tree.ParsedNode;
import studio.mevera.imperat.context.CommandSource;

import java.util.List;

/**
 * Scoring helpers shared between {@link TreeParser} and {@link TreeSuggester}.
 * Identical semantics to the inline helpers that previously lived on
 * {@code SuperCommandTree}.
 */
final class WalkScoring {

    private WalkScoring() {
    }

    static <S extends CommandSource> boolean hasUnacceptable(List<ParsedNode<S>> chain) {
        for (ParsedNode<S> pn : chain) {
            for (ParseResult<S> r : pn.getParseResults().values()) {
                if (r.isUnAcceptableScore()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Weighted score. Priorities (highest tier first):
     * <ol>
     *   <li>Fewest failed-parse arguments — a clean (zero-failure) match always
     *       wins over any partially-failed match, regardless of depth. Among
     *       equally-failed branches the deeper / more-matched one wins, which is
     *       what error reporting wants for "closest usage".</li>
     *   <li>Number of matched command literals (subcommand depth).</li>
     *   <li>Full input consumption.</li>
     *   <li>Sum of per-argument parse scores.</li>
     *   <li>Depth of the chain.</li>
     *   <li>Pathway-level tweaks (default/method/flag preferences).</li>
     * </ol>
     */
    static <S extends CommandSource> int scoreCandidate(Candidate<S> candidate, int totalTokens) {
        int consumed = candidate.consumedIndex() + 1; // raw index is -1 at start
        boolean fullyConsumed = totalTokens == 0 || consumed >= totalTokens;

        int totalScore = 0;
        int failures = 0;
        int commandLiterals = 0;
        int depth = candidate.chain().size();
        for (ParsedNode<S> pn : candidate.chain()) {
            if (!pn.isRoot() && pn.getMainArgument().isCommand()) {
                commandLiterals++;
            }
            totalScore += pn.getTotalParseScore();
            for (ParseResult<S> r : pn.getParseResults().values()) {
                if (r.isFailureScore()) {
                    failures++;
                }
            }
        }

        return -failures * 100_000_000
                       + commandLiterals * 2_000_000
                       + (fullyConsumed ? 1_000_000 : 0)
                       + totalScore * 1_000
                       + depth * 10
                       + scoreCandidatePathway(candidate.pathway());
    }

    private static <S extends CommandSource> int scoreCandidatePathway(@Nullable CommandPathway<S> pathway) {
        if (pathway == null) {
            return 0;
        }

        int score = pathway.getFlagExtractor().getRegisteredFlags().size() * 15;
        if (pathway.isDefault()) {
            score -= 20;
        }
        if (pathway.isDefault() && pathway.getMethodElement() == null) {
            score -= 400;
        }
        return score;
    }
}
