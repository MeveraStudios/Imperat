package studio.mevera.imperat.command.flags;

import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.FlagArgument;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.ArgumentParseException;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.responses.ResponseKey;
import studio.mevera.imperat.util.Patterns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

final class FlagExtractorImpl<S extends CommandSource> implements FlagExtractor<S> {

    private final CommandPathway<S> usage;
    private final FlagTrie<S> aliasTrie;
    private final Map<String, FlagArgument<S>> primaryByName = new HashMap<>();
    private final Set<FlagArgument<S>> registeredFlagArguments = new LinkedHashSet<>();

    FlagExtractorImpl(CommandPathway<S> usage) {
        this.usage = Objects.requireNonNull(usage, "CommandPathway cannot be null");
        this.aliasTrie = buildAliasTrie();
    }

    @Override
    public void insertFlag(FlagArgument<S> flagArgumentData) {
        registeredFlagArguments.add(flagArgumentData);
        primaryByName.put(flagArgumentData.getName(), flagArgumentData);
        registerAliases(flagArgumentData);
    }

    /**
     * Single-name flags ({@code @Switch("ip")} with no additional aliases)
     * are accepted under both {@code -ip} and {@code --ip} (permissive).
     * Multi-name flags ({@code @Switch({"silent", "s"})}) follow the strict
     * convention: primary uses {@code --silent}, only additional aliases
     * combine under {@code -s}.
     */
    private void registerAliases(FlagArgument<S> flagArgumentData) {
        List<String> additionalAliases = flagArgumentData.flagData().aliases();
        if (additionalAliases.isEmpty()) {
            aliasTrie.insert(flagArgumentData.getName(), flagArgumentData);
            return;
        }
        for (String alias : additionalAliases) {
            if (alias.equals(flagArgumentData.getName())) {
                continue;
            }
            aliasTrie.insert(alias, flagArgumentData);
        }
    }

    /**
     * <p>Distinguishes by prefix:
     * <ul>
     *   <li>{@code --name} — primary flag name lookup. Exact match only,
     *       NOT combinable (long form).</li>
     *   <li>{@code -aliases} — greedy alias trie. Combinable: {@code -abc}
     *       resolves to whichever aliases the trie can chain.</li>
     * </ul>
     * Inline {@code =value} suffix is stripped via
     * {@link Patterns#withoutFlagSign}.</p>
     */
    @Override
    public Set<FlagArgument<S>> extract(String rawInput) throws CommandException {
        if (rawInput == null || rawInput.isEmpty()) {
            return Collections.emptySet();
        }

        boolean longForm = Patterns.isDoubleFlag(rawInput);
        String name = Patterns.withoutFlagSign(rawInput);
        if (name.isEmpty()) {
            return Collections.emptySet();
        }
        if (longForm) {
            FlagArgument<S> primary = primaryByName.get(name);
            if (primary == null) {
                throw new ArgumentParseException(ResponseKey.UNKNOWN_FLAG, name);
            }
            return new LinkedHashSet<>(List.of(primary));
        }
        return parseAliasChain(name);
    }

    @Override
    public Set<FlagArgument<S>> getRegisteredFlags() {
        return Collections.unmodifiableSet(registeredFlagArguments);
    }

    /**
     * Builds a Trie containing only flag ALIASES (excluding primary names).
     * Each alias maps to its owning FlagArgument. Aliases are the
     * combinable short-form keys; primary names are looked up separately
     * via {@link #primaryByName}.
     */
    private FlagTrie<S> buildAliasTrie() {
        FlagTrie<S> trie = new FlagTrie<>();

        Set<FlagArgument<S>> allFlagArguments = usage.getArguments()
                                                 .stream()
                                                 .filter(Argument::isFlag)
                                                 .map((Argument::asFlagParameter))
                                                 .collect(Collectors.toSet());

        for (FlagArgument<S> flagArgumentData : allFlagArguments) {
            primaryByName.put(flagArgumentData.getName(), flagArgumentData);
            List<String> additionalAliases = flagArgumentData.flagData().aliases();
            if (additionalAliases.isEmpty()) {
                trie.insert(flagArgumentData.getName(), flagArgumentData);
                continue;
            }
            for (String alias : additionalAliases) {
                if (alias.equals(flagArgumentData.getName())) {
                    continue;
                }
                trie.insert(alias, flagArgumentData);
            }
        }

        return trie;
    }

    /**
     * Combines aliases via greedy longest-match. {@code -abc} where
     * {@code a}, {@code b}, {@code c} are registered aliases yields three
     * resolved flags. Unmatched characters surface as
     * {@link ResponseKey#UNKNOWN_FLAG}.
     */
    private Set<FlagArgument<S>> parseAliasChain(String input) throws CommandException {
        Set<FlagArgument<S>> extractedFlagArguments = new LinkedHashSet<>(3);
        List<String> unmatchedParts = new ArrayList<>();

        int position = 0;
        while (position < input.length()) {
            MatchResult<S> match = aliasTrie.findLongestMatch(input, position);

            if (match.isFound()) {
                extractedFlagArguments.add(match.flagArgumentData());
                position += match.matchLength();
            } else {
                unmatchedParts.add(String.valueOf(input.charAt(position)));
                position++;
            }
        }

        if (!unmatchedParts.isEmpty()) {
            throw new ArgumentParseException(ResponseKey.UNKNOWN_FLAG, input);
        }

        return extractedFlagArguments;
    }

    /**
     * Trie data structure optimized for flag alias storage and retrieval.
     * Uses a Map-based approach for efficient character lookup.
     */
    private static class FlagTrie<S extends CommandSource> {

        private final TrieNode<S> root;

        FlagTrie() {
            this.root = new TrieNode<>();
        }

        /**
         * Inserts a flag alias into the trie.
         */
        void insert(String alias, FlagArgument<S> flagArgumentData) {
            TrieNode<S> current = root;

            for (char c : alias.toCharArray()) {
                current = current.children.computeIfAbsent(c, k -> new TrieNode<>());
            }

            current.flagArgumentData = flagArgumentData;
            current.isEndOfFlag = true;
        }

        /**
         * Finds the longest matching flag alias starting from the given position.
         * Uses greedy matching to prefer longer aliases over shorter ones.
         */
        MatchResult<S> findLongestMatch(String input, int startPos) {
            TrieNode<S> current = root;
            FlagArgument<S> lastMatchedFlagArgument = null;
            int lastMatchLength = 0;

            for (int i = startPos; i < input.length(); i++) {
                char c = input.charAt(i);
                current = current.children.get(c);

                if (current == null) {
                    break; // No more matches possible
                }

                if (current.isEndOfFlag) {
                    lastMatchedFlagArgument = current.flagArgumentData;
                    lastMatchLength = i - startPos + 1;
                }
            }

            return new MatchResult<>(lastMatchedFlagArgument, lastMatchLength);
        }
    }

    /**
     * Represents a node in the Trie structure.
     */
    private static class TrieNode<S extends CommandSource> {

        final Map<Character, TrieNode<S>> children;
        FlagArgument<S> flagArgumentData;
        boolean isEndOfFlag;

        TrieNode() {
            this.children = new HashMap<>();
            this.isEndOfFlag = false;
        }
    }

    /**
     * Represents the result of a flag matching operation.
     */
    private record MatchResult<S extends CommandSource>(FlagArgument<S> flagArgumentData, int matchLength) {

        boolean isFound() {
            return flagArgumentData != null && matchLength > 0;
        }

    }
}
