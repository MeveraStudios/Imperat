package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.flags.FlagExtractor;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.util.Patterns;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class SuperCommandTree<S extends CommandSource> {

    static final int SUCCESSFUL_PARSE_SCORE = 1;
    static final int FAILED_PARSE_SCORE = 0;
    static final int UNACCEPTABLE_SCORE = -1;

    public static class Node<S extends CommandSource> {

        /*
         * The required argument this node is based on
         */
        final Argument<S> main;

        /**
         * List of optional arguments within this node
         */
        final List<Argument<S>> optionals;

        /**
         * The map of flags for this node.
         */
        final FlagExtractor<S> flags;

        final @Nullable Node<S> parent;

        final Map<String, Node<S>> children = new HashMap<>();

        private @Nullable CommandPathway<S> originalPathway = null;

        Node(@Nullable Node<S> parent, @Nullable CommandPathway<S> originalPathway, Argument<S> main, List<Argument<S>> optionals,
                FlagExtractor<S> flags) {
            this.parent = parent;
            this.originalPathway = originalPathway;
            this.main = main;
            this.optionals = optionals;
            this.flags = flags;
        }

        public @Nullable Node<S> getParent() {
            return parent;
        }

        public boolean isRoot() {
            return getParent() == null;
        }

        public boolean isLeaf() {
            return children.isEmpty();
        }

        public @Nullable CommandPathway<S> getOriginalPathway() {
            return originalPathway;
        }

        public boolean isExecutable() {
            return originalPathway != null;
        }

        @Nullable
        public ParsedNode<S> parseArgument(RawInputStream<S> inputStream) throws CommandException {

            RawInputStream<S> delegateInputStream = inputStream.copy();
            Map<String, ParseResult<S>> parseResultMap = new HashMap<>();

            var mainParsedResult = this.parseArgument(main, delegateInputStream);
            if (mainParsedResult.isUnAcceptableScore()) {
                return null;
            }

            parseResultMap.put(main.getName(), mainParsedResult);

            Iterator<Argument<S>> optionalsIterator = optionals.iterator();
            while (delegateInputStream.hasNext()) {
                String in = delegateInputStream.next();
                if (Patterns.isInputFlag(in)) {
                    var extracted = flags.extract(in);
                    for (var extractedFlag : extracted) {
                        parseResultMap.put(extractedFlag.getName(), this.parseArgument(extractedFlag, delegateInputStream));
                    }
                } else {
                    //treat as optional
                    if (optionalsIterator.hasNext()) {
                        Argument<S> optional = optionalsIterator.next();
                        var optionalParseResult = this.parseArgument(optional, delegateInputStream);
                        if (optionalParseResult.isUnAcceptableScore() || optionalParseResult.isFailureScore()) {
                            continue;
                        }
                        parseResultMap.put(optional.getName(), optionalParseResult);
                    }
                }
            }

            return new ParsedNode<>(this, parseResultMap);
        }

        private ParseResult<S> parseArgument(Argument<S> argument, RawInputStream<S> inputStream) {

            int consumed = 0;
            final int toConsume = argument.type().getNumberOfParametersToConsume(argument);
            StringBuilder builder = new StringBuilder();
            while (inputStream.hasNext()) {
                if (consumed > toConsume) {
                    break;
                }
                String input = inputStream.next();
                builder.append(input);
                consumed++;
            }

            final String input = builder.toString();
            if (input.isBlank()) {
                return ParseResult.unacceptableParse(argument, input, new IndexOutOfBoundsException());
            }

            try {
                var result = argument.type().parse(inputStream.getContext(), argument, input);
                return ParseResult.of(argument, input, result, null);
            } catch (Throwable error) {
                if (argument.isCommand()) {
                    return ParseResult.unacceptableParse(argument, input, error);
                }
                return ParseResult.of(argument, input, null, error);
            }
        }
    }

    public final static class ParsedNode<S extends CommandSource> extends Node<S> {

        private final Node<S> delegate;
        private final Map<String, ParseResult<S>> parseResults;

        ParsedNode(@NotNull Node<S> delegate, Map<String, ParseResult<S>> parseResults) {
            super(delegate.parent, delegate.originalPathway, delegate.main, delegate.optionals, delegate.flags);
            this.delegate = delegate;
            this.parseResults = parseResults;
        }

        public Node<S> getDelegate() {
            return delegate;
        }

        public Map<String, ParseResult<S>> getParseResults() {
            return parseResults;
        }

        public int getTotalParseScore() {
            return getParseResults().values().stream().mapToInt(ParseResult::getParseScore)
                           .sum();
        }

        @Override
        public ParsedNode<S> parseArgument(RawInputStream<S> inputStream) {
            //            throw new UnsupportedOperationException("Node '" + delegate.main.format() + "' is already parsed");
            return this;
        }
    }

    public final static class ParseResult<S extends CommandSource> {

        final @NotNull Argument<S> argument;
        final String input;
        final @Nullable Object parsedValue;
        final @Nullable Throwable error;

        int parseScore = FAILED_PARSE_SCORE;

        private ParseResult(@NotNull Argument<S> argument, String input, @Nullable Object parsedValue, @Nullable Throwable error) {
            this.argument = argument;
            this.input = input;
            this.parsedValue = parsedValue;
            this.error = error;
            this.parseScore = calculateParseScore();
        }

        public static <S extends CommandSource> ParseResult<S> of(@NotNull Argument<S> node, String input, @Nullable Object parsedValue,
                @Nullable Throwable error) {
            return new ParseResult<>(node, input, parsedValue, error);
        }

        public static <S extends CommandSource> ParseResult<S> unacceptableParse(@NotNull Argument<S> argument, String input,
                @NotNull Throwable error) {
            ParseResult<S> result = new ParseResult<>(argument, input, null, error);
            result.parseScore = UNACCEPTABLE_SCORE;
            return result;
        }

        public boolean isUnAcceptableScore() {
            return parseScore == UNACCEPTABLE_SCORE;
        }

        public int getParseScore() {
            return parseScore;
        }

        private int calculateParseScore() {
            if (error != null) {
                //no match, return failure
                return FAILED_PARSE_SCORE;
            }

            return SUCCESSFUL_PARSE_SCORE;
        }

        public boolean isFailureScore() {
            return parseScore == FAILED_PARSE_SCORE;
        }
    }
}
