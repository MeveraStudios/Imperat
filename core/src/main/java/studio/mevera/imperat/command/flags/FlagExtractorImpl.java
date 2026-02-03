package studio.mevera.imperat.command.flags;

import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.FlagParameter;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.UnknownFlagException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

final class FlagExtractorImpl<S extends Source> implements FlagExtractor<S>{

    private final CommandUsage<S> usage;
    private final FlagTrie<S> flagTrie;
    private final Set<FlagParameter<S>> registeredFlags = new HashSet<>();

    FlagExtractorImpl(CommandUsage<S> usage) {
        this.usage = Objects.requireNonNull(usage, "CommandUsage cannot be null");
        this.flagTrie = buildFlagTrie();
    }

    @Override
    public void insertFlag(FlagParameter<S> flagData) {
        registeredFlags.add(flagData);
        flagTrie.insert(flagData.name(), flagData);
        for(String alias : flagData.flagData().aliases()) {
            flagTrie.insert(alias, flagData);
        }
    }

    @Override
    public Set<FlagParameter<S>> extract(String rawInput) throws UnknownFlagException {
        if (rawInput == null || rawInput.isEmpty()) {
            return Collections.emptySet();
        }

        return parseFlags(rawInput);
    }

    @Override
    public Set<FlagParameter<S>> getRegisteredFlags() {
        return Collections.unmodifiableSet(registeredFlags);
    }

    /**
     * Builds a Trie containing all flag aliases for efficient lookup.
     * Each alias maps to its corresponding FlagData.
     */
    private FlagTrie<S> buildFlagTrie() {
        FlagTrie<S> trie = new FlagTrie<>();

        // Get all flags from CommandUsage and build the trie
        Set<FlagParameter<S>> allFlags = usage.getParameters()
                .stream()
                .filter(CommandParameter::isFlag)
                .map((CommandParameter::asFlagParameter))
                .collect(Collectors.toSet());

        for (FlagParameter<S> flagData : allFlags) {
            // Add primary flag name
            trie.insert(flagData.name(), flagData);

            // Add all aliases
            for (String alias : flagData.flagData().aliases()) {
                trie.insert(alias, flagData);
            }
        }

        return trie;
    }

    /**
     * Parses the input string using a greedy longest-match algorithm.
     * This ensures that longer aliases are matched before shorter ones.
     */
    private Set<FlagParameter<S>> parseFlags(String input) throws UnknownFlagException {
        Set<FlagParameter<S>> extractedFlags = new LinkedHashSet<>(3);
        List<String> unmatchedParts = new ArrayList<>();

        int position = 0;
        while (position < input.length()) {
            MatchResult<S> match = flagTrie.findLongestMatch(input, position);

            if (match.isFound()) {
                extractedFlags.add(match.flagData());
                position += match.matchLength();
            } else {
                // Collect unmatched character for error reporting
                unmatchedParts.add(String.valueOf(input.charAt(position)));
                position++;
            }
        }

        // Throw exception if there are unmatched parts
        if (!unmatchedParts.isEmpty()) {
            throw new UnknownFlagException(
                    String.join(", ", unmatchedParts)
                    
            );
        }

        return extractedFlags;
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
        void insert(String alias, FlagParameter<S> flagData) {
            TrieNode<S> current = root;

            for (char c : alias.toCharArray()) {
                current = current.children.computeIfAbsent(c, k -> new TrieNode<>());
            }

            current.flagData = flagData;
            current.isEndOfFlag = true;
        }

        /**
         * Finds the longest matching flag alias starting from the given position.
         * Uses greedy matching to prefer longer aliases over shorter ones.
         */
        MatchResult<S> findLongestMatch(String input, int startPos) {
            TrieNode<S> current = root;
            FlagParameter<S> lastMatchedFlag = null;
            int lastMatchLength = 0;

            for (int i = startPos; i < input.length(); i++) {
                char c = input.charAt(i);
                current = current.children.get(c);

                if (current == null) {
                    break; // No more matches possible
                }

                if (current.isEndOfFlag) {
                    lastMatchedFlag = current.flagData;
                    lastMatchLength = i - startPos + 1;
                }
            }

            return new MatchResult<>(lastMatchedFlag, lastMatchLength);
        }
    }

    /**
     * Represents a node in the Trie structure.
     */
    private static class TrieNode<S extends Source> {
        final Map<Character, TrieNode<S>> children;
        FlagParameter<S> flagData;
        boolean isEndOfFlag;

        TrieNode() {
            this.children = new HashMap<>();
            this.isEndOfFlag = false;
        }
    }

    /**
     * Represents the result of a flag matching operation.
     */
    private record MatchResult<S extends Source>(FlagParameter<S> flagData, int matchLength) {

        boolean isFound() {
            return flagData != null && matchLength > 0;
        }

    }
}
