package studio.mevera.imperat.command.flags;

import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.FlagArgument;
import studio.mevera.imperat.context.Source;
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

final class FlagExtractorImpl<S extends Source> implements FlagExtractor<S> {

    private final CommandUsage<S> usage;
    private final FlagTrie<S> flagTrie;
    private final Set<FlagArgument<S>> registeredFlagArguments = new LinkedHashSet<>();

    FlagExtractorImpl(CommandUsage<S> usage) {
        this.usage = Objects.requireNonNull(usage, "CommandUsage cannot be null");
        this.flagTrie = buildFlagTrie();
    }

    @Override
    public void insertFlag(FlagArgument<S> flagArgumentData) {
        registeredFlagArguments.add(flagArgumentData);
        flagTrie.insert(flagArgumentData.name(), flagArgumentData);
        for (String alias : flagArgumentData.flagData().aliases()) {
            flagTrie.insert(alias, flagArgumentData);
        }
    }

    @Override
    public Set<FlagArgument<S>> extract(String rawInput) throws CommandException {
        if (rawInput == null || rawInput.isEmpty()) {
            return Collections.emptySet();
        }

        return parseFlags(Patterns.withoutFlagSign(rawInput));
    }

    @Override
    public Set<FlagArgument<S>> getRegisteredFlags() {
        return Collections.unmodifiableSet(registeredFlagArguments);
    }

    /**
     * Builds a Trie containing all flag aliases for efficient lookup.
     * Each alias maps to its corresponding FlagData.
     */
    private FlagTrie<S> buildFlagTrie() {
        FlagTrie<S> trie = new FlagTrie<>();

        // Get all flags from CommandUsage and build the trie
        Set<FlagArgument<S>> allFlagArguments = usage.getParameters()
                                                 .stream()
                                                 .filter(Argument::isFlag)
                                                 .map((Argument::asFlagParameter))
                                                 .collect(Collectors.toSet());

        for (FlagArgument<S> flagArgumentData : allFlagArguments) {
            // Add primary flag name
            trie.insert(flagArgumentData.name(), flagArgumentData);

            // Add all aliases
            for (String alias : flagArgumentData.flagData().aliases()) {
                trie.insert(alias, flagArgumentData);
            }
        }

        return trie;
    }

    /**
     * Parses the input string using a greedy longest-match algorithm.
     * This ensures that longer aliases are matched before shorter ones.
     */
    private Set<FlagArgument<S>> parseFlags(String input) throws CommandException {
        Set<FlagArgument<S>> extractedFlagArguments = new LinkedHashSet<>(3);
        List<String> unmatchedParts = new ArrayList<>();

        int position = 0;
        while (position < input.length()) {
            MatchResult<S> match = flagTrie.findLongestMatch(input, position);

            if (match.isFound()) {
                extractedFlagArguments.add(match.flagArgumentData());
                position += match.matchLength();
            } else {
                // Collect unmatched character for error reporting
                unmatchedParts.add(String.valueOf(input.charAt(position)));
                position++;
            }
        }

        // Throw exception if there are unmatched parts
        if (!unmatchedParts.isEmpty()) {
            throw new CommandException(ResponseKey.UNKNOWN_FLAG)
                          .withPlaceholder("input", String.join(", ", unmatchedParts));
        }

        return extractedFlagArguments;
    }

    /**
     * Trie data structure optimized for flag alias storage and retrieval.
     * Uses a Map-based approach for efficient character lookup.
     */
    private static class FlagTrie<S extends Source> {

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
    private static class TrieNode<S extends Source> {

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
    private record MatchResult<S extends Source>(FlagArgument<S> flagArgumentData, int matchLength) {

        boolean isFound() {
            return flagArgumentData != null && matchLength > 0;
        }

    }
}
