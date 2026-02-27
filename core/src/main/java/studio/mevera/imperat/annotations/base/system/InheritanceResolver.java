package studio.mevera.imperat.annotations.base.system;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.annotations.InheritedArg;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.context.Source;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Resolves parameter inheritance chains for subcommand pathways.
 * Determines which ancestor pathway provides parameters for inheritance.
 */
public final class InheritanceResolver<S extends Source> {

    private final ImperatConfig<S> config;

    public InheritanceResolver(ImperatConfig<S> config) {
        this.config = config;
    }

    /**
     * Resolves the inheritance chain for a method's parameters.
     *
     * @param parentCmd The immediate parent command (can be null for root)
     * @param methodParams The method's parameters with their annotations
     * @return The resolved inheritance chain and remaining personal parameters
     */
    public InheritanceResolution<S> resolve(
            @Nullable Command<S> parentCmd,
            List<ParameterSignature> methodParams
    ) throws IllegalStateException {
        if (parentCmd == null || methodParams.isEmpty()) {
            // No inheritance possible
            return new InheritanceResolution<>(
                    ParameterInheritanceChain.empty(),
                    methodParams
            );
        }

        // Separate inherited from personal parameters
        List<InheritanceRequest> inheritedRequests = new ArrayList<>();
        List<ParameterSignature> personalParams = new ArrayList<>();

        for (ParameterSignature param : methodParams) {
            InheritedArg annotation = param.getAnnotation(InheritedArg.class);
            if (annotation != null) {
                inheritedRequests.add(new InheritanceRequest(
                        annotation.value().isEmpty() ? param.getName() : annotation.value(),
                        annotation.type() == Void.class ? param.getType() : annotation.type(),
                        param
                ));
            } else {
                personalParams.add(param);
            }
        }

        if (inheritedRequests.isEmpty()) {
            // No inheritance requested
            return new InheritanceResolution<>(
                    ParameterInheritanceChain.empty(),
                    methodParams
            );
        }

        // Build ancestor chain from root to parent
        List<Command<S>> ancestorChain = buildAncestorChain(parentCmd);

        // Find the best matching pathway from ancestors
        ParameterInheritanceChain<S> inheritanceChain = resolveInheritanceChain(
                ancestorChain,
                inheritedRequests
        );

        return new InheritanceResolution<>(inheritanceChain, personalParams);
    }

    /**
     * Builds the ancestor chain from root to the given command
     */
    private List<Command<S>> buildAncestorChain(Command<S> from) {
        List<Command<S>> chain = new ArrayList<>();
        Command<S> current = from;

        while (current != null) {
            chain.add(0, current); // Add at beginning to get root -> ... -> current
            current = current.getParent();
        }

        return chain;
    }

    /**
     * Resolves which ancestor pathway provides the requested inherited parameters.
     * Prefers pathways with MOST matching parameters (fail fast if ambiguous).
     */
    private ParameterInheritanceChain<S> resolveInheritanceChain(
            List<Command<S>> ancestorChain,
            List<InheritanceRequest> requests
    ) throws IllegalStateException {
        // Collect all pathways from all ancestors with their match scores

        List<PathwayMatch<S>> candidates = new ArrayList<>();

        for (Command<S> ancestor : ancestorChain) {
            for (CommandPathway<S> pathway : ancestor.getDedicatedPathways()) {
                List<MatchedParam<S>> matches = findMatchingParameters(pathway, requests);
                int score = calculateMatchScore(matches, requests);

                if (score > 0) {
                    candidates.add(new PathwayMatch<>(ancestor, pathway, score, matches));
                }
            }
        }

        if (candidates.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot resolve inherited parameters: " + requests.stream()
                                                                      .map(r -> r.name)
                                                                      .toList() +
                            ". No ancestor pathway contains matching parameters."
            );
        }

        // Sort by score descending
        candidates.sort((a, b) -> Integer.compare(b.matchScore(), a.matchScore()));

        // Check for ambiguity (multiple with same top score)
        if (candidates.size() >= 2) {
            PathwayMatch<S> best = candidates.get(0);
            PathwayMatch<S> second = candidates.get(1);

            if (best.matchScore() == second.matchScore()) {
                throw new IllegalStateException(
                        "Ambiguous parameter inheritance: multiple pathways match equally well. " +
                                "Best matches: '" + best.pathway().formatted() + "' and '" +
                                second.pathway().formatted() + "' both score " + best.matchScore()
                );
            }
        }

        // Build the inheritance chain from the best match
        PathwayMatch<S> bestMatch = candidates.get(0);
        return buildChainFromMatch(bestMatch, requests);
    }

    /**
     * Finds which parameters in the pathway match the inheritance requests
     */
    private List<MatchedParam<S>> findMatchingParameters(
            CommandPathway<S> pathway,
            List<InheritanceRequest> requests
    ) {
        List<MatchedParam<S>> matches = new ArrayList<>();
        // CRITICAL: Use loadCombinedParameters() to include inherited params from ancestors
        List<Argument<S>> params = pathway.getParametersWithFlags();

        for (InheritanceRequest request : requests) {
            for (int i = 0; i < params.size(); i++) {
                Argument<S> param = params.get(i);
                if (matches(request, param)) {
                    matches.add(new MatchedParam<>(request, param, i));
                    break; // One match per request
                }
            }
        }

        return matches;
    }

    private boolean matches(InheritanceRequest request, Argument<S> param) {
        // Name match (case insensitive)
        boolean nameMatches = param.getName().equalsIgnoreCase(request.name);

        // Type match (if specified in annotation)
        boolean typeMatches = request.type == null ||
                                      request.type == Object.class ||
                                      param.valueType().equals(request.type);

        return nameMatches && typeMatches;
    }

    private int calculateMatchScore(List<MatchedParam<S>> matches, List<InheritanceRequest> requests) {
        // Score based on: number of matches, sequence preservation, type specificity
        int score = matches.size() * 100;

        // Bonus for preserving sequence order
        int lastIndex = -1;
        boolean sequencePreserved = true;
        for (MatchedParam<S> match : matches) {
            if (match.index < lastIndex) {
                sequencePreserved = false;
                break;
            }
            lastIndex = match.index;
        }

        if (sequencePreserved) {
            score += 50;
        }

        // Bonus for matching all requested params
        if (matches.size() == requests.size()) {
            score += 25;
        }

        return score;
    }

    private ParameterInheritanceChain<S> buildChainFromMatch(
            PathwayMatch<S> match,
            List<InheritanceRequest> requests
    ) {
        ParameterInheritanceChain.Builder<S> builder = ParameterInheritanceChain.<S>builder()
                                                               .sourcePathway(match.pathway())
                                                               .sourceCommand(match.command());

        // Add matched parameters in order they appear in the source pathway
        List<MatchedParam<S>> sortedMatches = new ArrayList<>(match.matches());
        sortedMatches.sort(Comparator.comparingInt(MatchedParam::index));

        for (MatchedParam<S> matched : sortedMatches) {
            builder.addInherited(
                    matched.param(),
                    match.command(),
                    matched.index()
            );
        }

        return builder.build();
    }

    /**
     * Signature of a method parameter for inheritance resolution
     */
    public interface ParameterSignature {

        String getName();

        Class<?> getType();

        <A extends java.lang.annotation.Annotation> A getAnnotation(Class<A> annotationClass);
    }

    record PathwayMatch<S extends Source>(Command<S> command, CommandPathway<S> pathway, int matchScore, List<MatchedParam<S>> matches) {

    }

    private record InheritanceRequest(String name, Class<?> type, ParameterSignature originalParam) {

    }

    private record MatchedParam<S extends Source>(InheritanceRequest request, Argument<S> param, int index) {

    }

    /**
     * Result of inheritance resolution
     */
    public record InheritanceResolution<S extends Source>(
            ParameterInheritanceChain<S> inheritanceChain,
            List<ParameterSignature> personalParameters
    ) {

    }
}