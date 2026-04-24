package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.FlagArgument;
import studio.mevera.imperat.command.flags.FlagExtractor;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.util.Patterns;
import studio.mevera.imperat.util.priority.Prioritizable;
import studio.mevera.imperat.util.priority.Priority;
import studio.mevera.imperat.util.priority.PriorityList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A node is made of the MAIN REQUIRED argument its based on, additional optional args afterward, and flags
 * INSTEAD OF HAVING a node for every argument.
 * @param <S> the command source
 */
public class Node<S extends CommandSource> implements Prioritizable {

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
    final Map<String, Node<S>> children = new HashMap<>();
    final @NotNull CommandPathway<S> originalPathway;
    @Nullable Node<S> parent;

    Node(@Nullable Node<S> parent, @NotNull CommandPathway<S> originalPathway, Argument<S> main, List<Argument<S>> optionals) {
        this.parent = parent;
        this.originalPathway = originalPathway;
        this.main = main;
        this.optionals = optionals;
        this.flags = originalPathway.getFlagExtractor();
    }

    Node(@Nullable Node<S> parent, @NotNull CommandPathway<S> originalPathway, Argument<S> main) {
        this(parent, originalPathway, main, new ArrayList<>());
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

    public @NotNull CommandPathway<S> getOriginalPathway() {
        return originalPathway;
    }

    public boolean isExecutable() {
        return isLeaf();
    }

    /**
     * Parses this node against the given input stream. Advances the stream
     * by the number of tokens consumed by this node (main + optionals + flags).
     * Caller is responsible for snapshotting the stream (via {@link RawInputStream#copy()}
     * or {@link RawInputStream#getRawIndex()}) before calling if backtracking is needed.
     *
     * @return a {@link ParsedNode} wrapping this node + per-argument {@link ParseResult}s,
     *         or {@code null} if this node's main argument failed with an UNACCEPTABLE score.
     */
    @Nullable
    public ParsedNode<S> parseArgument(RawInputStream<S> inputStream) throws CommandException {
        Map<String, ParseResult<S>> parseResultMap = new HashMap<>();

        ParseResult<S> mainParsedResult = this.parseArgument(main, inputStream);
        if (mainParsedResult.isUnAcceptableScore()) {
            return null;
        }
        parseResultMap.put(main.getName(), mainParsedResult);

        Iterator<Argument<S>> optionalsIterator = optionals.iterator();
        while (inputStream.hasNext()) {
            String peek = inputStream.next();
            if (Patterns.isInputFlag(peek)) {
                try {
                    var extracted = flags.extract(peek);
                    if (!extracted.isEmpty()) {
                        boolean hasValueFlags = extracted.stream().anyMatch(flag -> !flag.isSwitch());
                        String sharedValueInput = hasValueFlags && inputStream.hasNext() ? inputStream.next() : null;
                        for (var extractedFlag : extracted) {
                            parseResultMap.put(
                                    extractedFlag.getName(),
                                    this.parseFlagArgument(extractedFlag, peek, sharedValueInput, inputStream)
                            );
                        }
                        continue;
                    }
                } catch (CommandException ignored) {
                    // Not a registered flag for this pathway: treat as normal token.
                }
                inputStream.backward();
            } else {
                inputStream.backward();
            }

            // non-flag token: if we still have optionals, consume; else hand back to children
            if (!optionalsIterator.hasNext()) {
                break;
            }
            Argument<S> optional = optionalsIterator.next();
            ParseResult<S> optionalParseResult = this.parseArgument(optional, inputStream);
            parseResultMap.put(optional.getName(), optionalParseResult);
        }

        return new ParsedNode<>(this, parseResultMap);
    }

    private ParseResult<S> parseFlagArgument(
            FlagArgument<S> flag,
            String rawFlagInput,
            @Nullable String sharedValueInput,
            RawInputStream<S> inputStream
    ) {
        if (flag.isSwitch()) {
            return ParseResult.of(flag, rawFlagInput, true, null);
        }

        if (sharedValueInput == null || sharedValueInput.isBlank()) {
            return ParseResult.failedParse(flag, sharedValueInput == null ? "" : sharedValueInput, null);
        }

        var inputType = flag.flagData().inputType();
        if (inputType == null) {
            return ParseResult.failedParse(flag, sharedValueInput, new IllegalStateException("Missing input type for value flag"));
        }
        try {
            Object parsed = inputType.parse(inputStream.getContext(), flag, sharedValueInput);
            return ParseResult.of(flag, sharedValueInput, parsed, null);
        } catch (Throwable error) {
            return ParseResult.of(flag, sharedValueInput, null, error);
        }
    }

    private ParseResult<S> parseArgument(Argument<S> argument, RawInputStream<S> inputStream) {
        StringBuilder builder = new StringBuilder();

        if (argument.isGreedy()) {
            int limit = argument.greedyLimit();
            int consumed = 0;
            while (inputStream.hasNext()) {
                if (limit > 0 && consumed >= limit) {
                    break;
                }
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(inputStream.next());
                consumed++;
            }
        } else {
            final int toConsume = argument.type().getNumberOfParametersToConsume(argument);
            int consumed = 0;
            while (inputStream.hasNext() && consumed < toConsume) {
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(inputStream.next());
                consumed++;
            }
        }

        final String input = builder.toString();
        if (input.isBlank()) {
            if (argument.isCommand()) {
                return ParseResult.unacceptableParse(argument, input, new IndexOutOfBoundsException());
            }
            return ParseResult.failedParse(argument, input, null);
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

    public Argument<S> getMainArgument() {
        return main;
    }

    public Argument<S> getData() {
        return main;
    }

    public @NotNull Collection<? extends Argument<S>> getOptionalArguments() {
        return optionals;
    }

    public String format() {
        return main.format();
    }

    public boolean isGreedy() {
        return getMainArgument().isGreedy();
    }

    public PriorityList<Node<S>> getChildren() {
        PriorityList<Node<S>> sorted = new PriorityList<>();
        for (Node<S> child : children.values()) {
            sorted.add(child);
        }
        return sorted;
    }

    @Override
    public @NotNull Priority getPriority() {
        return main.type().getPriority();
    }
}
