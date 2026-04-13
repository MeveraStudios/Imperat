package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.FlagArgument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.permissions.PermissionsData;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.util.Patterns;
import studio.mevera.imperat.util.priority.Prioritizable;
import studio.mevera.imperat.util.priority.PriorityList;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

public abstract class CommandNode<S extends CommandSource, T extends Argument<S>> implements Comparable<CommandNode<S, ?>>, Prioritizable {

    protected final @NotNull T data;
    private final PriorityList<CommandNode<S, ?>> children = new PriorityList<>();
    private final int depth;
    private final @Nullable CommandNode<S, ?> parent;
    protected @Nullable CommandPathway<S> executableUsage;
    private @Nullable CommandPathway<S> nearestExecutableUsage;
    private @NotNull CompletionCache<S> completionCache = CompletionCache.empty();

    protected CommandNode(@Nullable CommandNode<S, ?> parent, @NotNull T data, int depth, @Nullable CommandPathway<S> executableUsage) {
        this.parent = parent;
        this.data = data;
        this.depth = depth;
        this.executableUsage = executableUsage;
    }

    public static <S extends CommandSource> LiteralCommandNode<S> createCommandNode(
            @Nullable CommandNode<S, ?> parent,
            @NotNull Command<S> data,
            int depth,
            @Nullable CommandPathway<S> executableUsage
    ) {
        return new LiteralCommandNode<>(parent, data, depth, executableUsage);
    }

    public static <S extends CommandSource> ArgumentNode<S> createArgumentNode(
            CommandNode<S, ?> parent,
            Argument<S> data,
            int depth,
            @Nullable CommandPathway<S> executableUsage
    ) {
        return new ArgumentNode<>(parent, data, depth, executableUsage);
    }

    public int getDepth() {
        return depth;
    }

    public @Nullable CommandPathway<S> getExecutableUsage() {
        return executableUsage;
    }

    public void setExecutableUsage(@Nullable CommandPathway<S> executableUsage) {
        this.executableUsage = executableUsage;
    }

    public @Nullable CommandPathway<S> getNearestExecutableUsage() {
        return nearestExecutableUsage;
    }

    public void setNearestExecutableUsage(@Nullable CommandPathway<S> nearestExecutableUsage) {
        this.nearestExecutableUsage = nearestExecutableUsage;
    }

    public @NotNull CompletionCache<S> getCompletionCache() {
        return completionCache;
    }

    public void setCompletionCache(@NotNull CompletionCache<S> completionCache) {
        this.completionCache = Objects.requireNonNull(completionCache, "completionCache");
    }

    public boolean isExecutable() {
        return this.executableUsage != null;
    }

    @NotNull
    public T getData() {
        return data;
    }

    public void addChild(CommandNode<S, ?> node) {
        if (children.contains(node)) {
            return;
        }
        children.add(node);
    }

    public PriorityList<CommandNode<S, ?>> getChildren() {
        return children;
    }

    private ParseResult<S> parse(
            ArgumentType<S, ?> type,
            int depth,
            CommandContext<S> ctx,
            @Nullable CommandPathway<S> flagScopePathway,
            int requestedTokensToConsume
    ) {
        if (this.isLiteral()) {
            String in = ctx.arguments().getOr(depth, null);
            if (in == null) {
                return ParseResult.failed(new IllegalArgumentException("Empty input given for a literal node"));
            }
            if (data.asCommand().hasName(in)) {
                return ParseResult.successful(data.asCommand(), data, in, depth, depth + 1);
            } else {
                return ParseResult.failed(new CommandException("Unknown sub command '" + in + "'"));
            }
        }

        final int baseCount = type.getNumberOfParametersToConsume(data);
        if (!isGreedyParam() && baseCount == 1 && requestedTokensToConsume <= 1) {
            String rawInput = ctx.arguments().getOr(depth, null);
            if (rawInput == null) {
                return ParseResult.failed(new IllegalArgumentException("No input token available for " + data.format()));
            }

            if (resolveFlagData(ctx, flagScopePathway, depth) == null) {
                try {
                    var obj = type.parse(ctx, this.data, rawInput);
                    return ParseResult.successful(obj, data, rawInput, data.getPosition(), depth + 1);
                } catch (Exception e) {
                    return ParseResult.failed(e);
                }
            }
        }

        final int remainingTokens = countRemainingBindableTokens(ctx, depth, flagScopePathway);
        final int tokensToConsume = resolveMatchTokenCount(type, remainingTokens, requestedTokensToConsume);
        if (tokensToConsume < 1) {
            throw new IllegalArgumentException("Number of args to consume for type " + type.getClass().getSimpleName() + " must be at least 1");
        }

        try {
            var parseOutcome = collectMatchInput(ctx, depth, tokensToConsume, flagScopePathway);
            var obj = type.parse(ctx, this.data, parseOutcome.input());
            return ParseResult.successful(obj, data, parseOutcome.input(), data.getPosition(), parseOutcome.nextDepth());
        } catch (Exception e) {
            return ParseResult.failed(e);
        }
    }

    private int resolveMatchTokenCount(ArgumentType<S, ?> type, int remainingTokens, int requestedTokensToConsume) {
        if (requestedTokensToConsume > 0) {
            return Math.min(requestedTokensToConsume, remainingTokens);
        }
        final int baseCount = type.getNumberOfParametersToConsume(data);
        if (!isGreedyParam()) {
            return Math.min(baseCount, remainingTokens);
        }

        final int greedyLimit = data.greedyLimit();
        return greedyLimit > 0 ? Math.min(greedyLimit, remainingTokens) : remainingTokens;
    }

    private int countRemainingBindableTokens(
            CommandContext<S> ctx,
            int depth,
            @Nullable CommandPathway<S> flagScopePathway
    ) {
        int rawIndex = depth;
        int count = 0;

        while (rawIndex < ctx.arguments().size()) {
            FlagData<S> flagData = resolveFlagData(ctx, flagScopePathway, rawIndex);
            if (flagData != null) {
                rawIndex += flagData.isSwitch() ? 1 : 2;
                count++;
                continue;
            }

            count++;
            rawIndex++;
        }
        return count;
    }

    private MatchCollection collectMatchInput(
            CommandContext<S> ctx,
            int depth,
            int tokensToConsume,
            @Nullable CommandPathway<S> flagScopePathway
    ) {
        var args = ctx.arguments();
        StringBuilder input = new StringBuilder();
        int rawIndex = depth;
        int consumed = 0;

        while (rawIndex < args.size() && consumed < tokensToConsume) {
            FlagData<S> flagData = resolveFlagData(ctx, flagScopePathway, rawIndex);
            if (flagData != null) {
                rawIndex += flagData.isSwitch() ? 1 : 2;
                continue;
            }

            if (consumed > 0) {
                input.append(' ');
            }

            input.append(args.get(rawIndex));
            consumed++;
            rawIndex++;
        }

        if (consumed < 1) {
            throw new IllegalArgumentException("No input tokens were collected for " + data.format());
        }

        return new MatchCollection(input.toString(), rawIndex);
    }

    private @Nullable FlagData<S> resolveFlagData(
            CommandContext<S> ctx,
            @Nullable CommandPathway<S> flagScopePathway,
            int rawIndex
    ) {
        if (flagScopePathway == null) {
            return null;
        }

        String raw = ctx.arguments().getOr(rawIndex, null);
        if (raw == null || !Patterns.isInputFlag(raw)) {
            return null;
        }

        return flagScopePathway.getFlagDataFromInput(raw);
    }

    public ParseResult<S> parse(int depth, CommandContext<S> ctx) {
        return parse(depth, ctx, null);
    }

    public ParseResult<S> parse(int depth, CommandContext<S> ctx, @Nullable CommandPathway<S> flagScopePathway) {
        var primaryType = data.type();
        return parse(primaryType, depth, ctx, flagScopePathway, -1);
    }

    public ParseResult<S> parse(
            int depth,
            CommandContext<S> ctx,
            @Nullable CommandPathway<S> flagScopePathway,
            int requestedTokensToConsume
    ) {
        var primaryType = data.type();
        return parse(primaryType, depth, ctx, flagScopePathway, requestedTokensToConsume);
    }

    /*private @Nullable Pair<ParseResult, CommandNode<S, ?>> findNeighborOfType(int depth, CommandContext<S> ctx) {
        if (parent == null) {
            return null;
        }
        for (var sibling : parent.getChildren()) {
            if (sibling.equals(this)) {
                continue;
            }
            ParseResult siblingParseResult = sibling.parse(depth, ctx, true);
            if (siblingParseResult.isSuccessful()) {
                return new Pair<>(siblingParseResult, sibling);
            }
        }
        return null;
    }*/


    public abstract String format();

    public boolean isLast() {
        return children.isEmpty();
    }


    public boolean isGreedyParam() {
        return data.isGreedy();
    }

    public boolean isOptional() {
        return (this instanceof ArgumentNode<?> param) && param.data.isOptional();
    }

    public @Nullable CommandNode<S, ?> getChild(Predicate<CommandNode<S, ?>> predicate) {
        for (var child : getChildren()) {
            if (predicate.test(child)) {
                return child;
            }
        }
        return null;
    }

    public CommandNode<S, ?> getNextCommandChild() {
        return getChild((child) -> child instanceof LiteralCommandNode<?>);
    }

    public CommandNode<S, ?> getNextParameterChild() {
        return getChild((child) -> true);
    }

    public boolean isRequired() {
        return data.isRequired();
    }

    public boolean isLiteral() {
        return this instanceof LiteralCommandNode || data.isCommand();
    }

    public boolean isTrueFlag() {
        return this.data.isFlag() && !this.data.asFlagParameter().isSwitch();
    }

    public boolean isFlag() {
        return this.data.isFlag();
    }

    /**
     * Whether this node represents a secret command.
     * Only meaningful for literal (command) nodes.
     */
    public boolean isSecret() {
        return isLiteral() && data.asCommand().isSecret();
    }

    public @Nullable CommandNode<S, ?> getParent() {
        return parent;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public int getNumberOfParametersToConsume() {
        int incrementation = this.data.type().getNumberOfParametersToConsume(data);
        if (incrementation < 1) {
            incrementation = 1;
        }
        return incrementation;
    }

    public PermissionsData getPermissionsData() {
        return data.getPermissionsData();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CommandNode<?, ?> that)) {
            return false;
        }
        return Objects.equals(this.parent, that.parent) && Objects.equals(data.getName(), that.data.getName()) && this.depth == that.depth
                       && Objects.equals(
                children, that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.parent, data.getName(), this.depth, children);
    }

    @Override
    public int compareTo(@NotNull CommandNode<S, ?> o) {
        //the highest priority comes first
        return this.getPriority().compareTo(o.getPriority());
    }

    public @Nullable CommandNode<S, ?> findNode(Predicate<CommandNode<S, ?>> predicate) {
        if (predicate.test(this)) {
            return this;
        }
        for (var child : getChildren()) {
            var found = child.findNode(predicate);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private record MatchCollection(String input, int nextDepth) {

    }

    public record CompletionCache<S extends CommandSource>(
            @NotNull SuggestionProvider<S> suggestionProvider,
            @NotNull List<FlagArgument<S>> visibleFlags,
            @NotNull Map<String, FlagArgument<S>> flagLookup,
            @NotNull List<CommandNode<S, ?>> optionalOverlapDescendants,
            @NotNull List<CommandNode<S, ?>> literalChildren,
            @NotNull List<CommandNode<S, ?>> nonLiteralChildren,
            @NotNull Map<String, List<CommandNode<S, ?>>> literalChildLookup
    ) {

        public static <S extends CommandSource> @NotNull CompletionCache<S> empty() {
            return new CompletionCache<>((context, argument) -> List.of(), List.of(), Map.of(), List.of(), List.of(), List.of(), Map.of());
        }
    }

}
