package studio.mevera.imperat;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.Description;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.FlagArgument;
import studio.mevera.imperat.command.suggestions.CompletionArg;
import studio.mevera.imperat.command.tree.projection.CommandTreeProjection;
import studio.mevera.imperat.command.tree.projection.ProjectedFlag;
import studio.mevera.imperat.command.tree.projection.ProjectedNode;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.util.Patterns;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Brigadier-tree builder backed by a {@link CommandTreeProjection}. The
 * projection is built once per command at registration time; this class
 * walks it to emit the corresponding Brigadier {@link CommandNode} graph.
 *
 * <p>Behaviourally equivalent to the previous monolithic walker — the
 * projection takes over the per-scope pathway / flag resolution that was
 * previously inlined here, leaving this class to focus on Brigadier-shape
 * translation.</p>
 */
@SuppressWarnings("unchecked")
public abstract non-sealed class BaseBrigadierManager<S extends CommandSource> implements BrigadierManager<S> {

    protected final Imperat<S> dispatcher;

    protected BaseBrigadierManager(Imperat<S> dispatcher) {
        this.dispatcher = dispatcher;
    }

    private static <BS> LiteralCommandNode<BS> cloneWithDiffName(
            LiteralCommandNode<BS> brigOriginalNode,
            String newName
    ) {
        var clone = new LiteralCommandNode<>(newName,
                brigOriginalNode.getCommand(), brigOriginalNode.getRequirement(), brigOriginalNode.getRedirect(),
                brigOriginalNode.getRedirectModifier(), brigOriginalNode.isFork());
        for (var child : brigOriginalNode.getChildren()) {
            clone.addChild(child);
        }
        return clone;
    }

    private static <S extends CommandSource, BS> void injectCommandNodeAliasesIntoBrigadier(
            Command<S> imperatCommand,
            LiteralCommandNode<BS> brigCommandNode,
            ArgumentBuilder<BS, ?> parentBrigNodeBuilder
    ) {
        for (var alias : imperatCommand.aliases()) {
            parentBrigNodeBuilder.then(cloneWithDiffName(brigCommandNode, alias));
        }
    }

    @Override
    public @NotNull <BS> LiteralCommandNode<BS> parseCommandIntoNode(@NotNull Command<S> command) {
        CommandTreeProjection<S> projection = CommandTreeProjection.of(command);
        return this.<BS>convertRoot(command, projection.root()).build();
    }

    private <BS> LiteralArgumentBuilder<BS> convertRoot(Command<S> rootCommand, ProjectedNode<S> root) {
        Command<S> rootCmdLit = root.mainArgument().asCommand();
        LiteralArgumentBuilder<BS> builder = (LiteralArgumentBuilder<BS>)
                                                     literal(rootCmdLit.getName())
                    .requires((obj) -> {
                        var source = wrapCommandSource(obj);
                        return rootCmdLit.isIgnoringACPerms()
                                       || dispatcher.config().getPermissionChecker().hasPermission(source, rootCmdLit);
                    });
        executor(builder);
        appendContinuations(rootCommand, root, builder, 0);
        return builder;
    }

    private <BS> CommandNode<BS> convertProjectedNode(
            Command<S> rootCommand,
            ProjectedNode<S> projected
    ) {
        ArgumentBuilder<BS, ?> childBuilder = createBrigadierBuilder(rootCommand, projected);
        executor(childBuilder);

        Argument<S> main = projected.mainArgument();
        if (!main.isCommand()) {
            ((RequiredArgumentBuilder<BS, ?>) childBuilder).suggests(
                    createSuggestionProvider(rootCommand, main)
            );
        }

        appendContinuations(rootCommand, projected, childBuilder, 0);
        return childBuilder.build();
    }

    private <BS> ArgumentBuilder<BS, ?> createBrigadierBuilder(
            Command<S> rootCommand,
            ProjectedNode<S> projected
    ) {
        Argument<S> argument = projected.mainArgument();
        ArgumentBuilder<BS, ?> builder = argument.isCommand()
                                                 ? LiteralArgumentBuilder.literal(argument.asCommand().getName())
                                                 : RequiredArgumentBuilder.argument(argument.getName(), getArgumentType(argument));

        builder.requires((obj) -> isNodeVisible(rootCommand, projected, wrapCommandSource(obj)));
        return builder;
    }

    private boolean isNodeVisible(Command<S> rootCommand, ProjectedNode<S> projected, S source) {
        Argument<S> argument = projected.mainArgument();
        if (argument.isCommand() && argument.asCommand().isSecret()) {
            return false;
        }

        if (rootCommand.isIgnoringACPerms() || (argument.isCommand() && argument.asCommand().isIgnoringACPerms())) {
            return true;
        }

        var checker = dispatcher.config().getPermissionChecker();
        return checker.hasPermission(source, projected.originalPathway())
                       && checker.hasPermission(source, argument);
    }

    /**
     * Adds — in order — the flag fallback (so {@code --flag} / unknown-flag
     * input still dispatches into Imperat), the next nested optional
     * continuation, and every child node. The flag fallback's suggestion
     * provider reads from the projection's pre-resolved flag list, so we no
     * longer round-trip through Imperat's auto-completer just to enumerate
     * flag names.
     */
    private <BS> void appendContinuations(
            Command<S> rootCommand,
            ProjectedNode<S> scope,
            ArgumentBuilder<BS, ?> parentBuilder,
            int optionalIndex
    ) {
        appendFlagFallback(rootCommand, scope, parentBuilder);
        appendOptionalContinuation(rootCommand, scope, parentBuilder, optionalIndex);
        appendChildContinuations(rootCommand, scope, parentBuilder);
    }

    private <BS> void appendOptionalContinuation(
            Command<S> rootCommand,
            ProjectedNode<S> scope,
            ArgumentBuilder<BS, ?> parentBuilder,
            int optionalIndex
    ) {
        List<Argument<S>> optionals = scope.optionalArguments();
        if (optionalIndex >= optionals.size()) {
            return;
        }

        Argument<S> optional = optionals.get(optionalIndex);
        RequiredArgumentBuilder<BS, ?> optionalBuilder =
                RequiredArgumentBuilder.argument(optional.getName(), getArgumentType(optional));
        optionalBuilder.requires((obj) -> isArgumentVisible(rootCommand, optional, scope.originalPathway(), wrapCommandSource(obj)));
        optionalBuilder.suggests(createSuggestionProvider(rootCommand, optional));
        executor(optionalBuilder);

        appendContinuations(rootCommand, scope, optionalBuilder, optionalIndex + 1);
        parentBuilder.then(optionalBuilder);
    }

    private <BS> void appendChildContinuations(
            Command<S> rootCommand,
            ProjectedNode<S> scope,
            ArgumentBuilder<BS, ?> parentBuilder
    ) {
        for (ProjectedNode<S> child : scope.children()) {
            var childBrigNode = this.<BS>convertProjectedNode(rootCommand, child);
            parentBuilder.then(childBrigNode);

            Argument<S> childArgument = child.mainArgument();
            if (childArgument.isCommand()) {
                injectCommandNodeAliasesIntoBrigadier(
                        childArgument.asCommand(),
                        (LiteralCommandNode<BS>) childBrigNode,
                        parentBuilder
                );
            }
        }
    }

    private boolean isArgumentVisible(
            Command<S> rootCommand,
            Argument<S> argument,
            @Nullable CommandPathway<S> pathway,
            S source
    ) {
        if (argument.isCommand() && argument.asCommand().isSecret()) {
            return false;
        }

        if (rootCommand.isIgnoringACPerms() || (argument.isCommand() && argument.asCommand().isIgnoringACPerms())) {
            return true;
        }

        var checker = dispatcher.config().getPermissionChecker();
        return (pathway == null || checker.hasPermission(source, pathway))
                       && checker.hasPermission(source, argument);
    }

    /**
     * Catch-all greedy-string node that surfaces flag-name + flag-value
     * suggestions and dispatches any input (including {@code --}-prefixed
     * forms or unknown flags) back into Imperat. Replaces the legacy
     * "_imperat_flags_<depth>" tunnel; the suggestion provider now reads
     * from the projection's flag list instead of round-tripping through the
     * auto-completer.
     */
    private <BS> void appendFlagFallback(
            Command<S> command,
            ProjectedNode<S> scope,
            ArgumentBuilder<BS, ?> parentBuilder
    ) {
        if (scope.flags().isEmpty()) {
            return;
        }

        RequiredArgumentBuilder<BS, String> fallback =
                RequiredArgumentBuilder.argument(flagFallbackName(scope), StringArgumentType.greedyString());
        fallback.suggests(createFlagSuggestionProvider(command, scope));
        executor(fallback);
        parentBuilder.then(fallback);
    }

    private @NotNull <BS> SuggestionProvider<BS> createSuggestionProvider(
            Command<S> command,
            Argument<S> parameter
    ) {
        return (context, builder) -> {
            SuggestionContext<S> ctx = createSuggestionContext(command, context.getSource(), context.getInput());
            CompletionArg arg = ctx.getArgToComplete();

            String paramFormat = parameter.format();
            Description desc = parameter.getDescription();
            Message tooltip = new LiteralMessage(paramFormat + (desc.isEmpty() ? "" : " - " + desc.getValue()));

            return dispatcher.config().getParameterSuggestionResolver(parameter).provideAsynchronously(ctx, parameter)
                           .thenCompose((results) -> {
                               results
                                       .stream()
                                       .filter((candidate) -> arg.isEmpty()
                                                                      || candidate.toLowerCase().startsWith(arg.value().toLowerCase()))
                                       .forEachOrdered((result) -> builder.suggest(result, tooltip));
                               return builder.buildFuture();
                           });
        };
    }

    private @NotNull <BS> SuggestionProvider<BS> createFlagSuggestionProvider(
            Command<S> command,
            ProjectedNode<S> scope
    ) {
        return (context, builder) -> {
            SuggestionContext<S> ctx = createSuggestionContext(command, context.getSource(), context.getInput());
            CompletionArg arg = ctx.getArgToComplete();
            String prefix = arg.isEmpty() ? "" : arg.value();

            ProjectedFlag<S> valueTarget = findFlagValueTarget(scope, ctx, arg);
            if (valueTarget != null) {
                var targetBuilder = builder.createOffset(resolveSuggestionStart(context.getInput(), arg));
                List<String> values = collectFlagValueSuggestions(ctx, valueTarget.flag());
                values.stream()
                        .filter(v -> v != null && !v.isEmpty())
                        .filter(v -> prefix.isEmpty() || v.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT)))
                        .forEachOrdered(targetBuilder::suggest);
                return targetBuilder.buildFuture();
            }

            if (!shouldSuggestFlagNames(arg)) {
                return builder.buildFuture();
            }

            var targetBuilder = builder.createOffset(resolveSuggestionStart(context.getInput(), arg));
            Set<String> seen = new LinkedHashSet<>();
            for (ProjectedFlag<S> flag : scope.flags()) {
                if (!isFlagVisible(command, flag, ctx.source())) {
                    continue;
                }
                for (String alias : flag.aliases()) {
                    String formatted = "-" + alias;
                    if (!seen.add(formatted)) {
                        continue;
                    }
                    if (prefix.isEmpty() || formatted.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
                        targetBuilder.suggest(formatted);
                    }
                }
            }
            return targetBuilder.buildFuture();
        };
    }

    private boolean isFlagVisible(Command<S> rootCommand, ProjectedFlag<S> flag, S source) {
        if (rootCommand.isIgnoringACPerms()) {
            return true;
        }
        var checker = dispatcher.config().getPermissionChecker();
        return checker.hasPermission(source, flag.owningPathway())
                       && checker.hasPermission(source, flag.flag());
    }

    /**
     * If the cursor is currently sitting inside a flag's value span, returns
     * that flag so the caller can surface its value suggestions. Walks
     * BACKWARDS to the most recent flag form and returns it iff the offset
     * between the flag and the completion position is within the flag's
     * declared value-token arity. This generalises the legacy "previous
     * token must be a flag" check so multi-token value flags (e.g. a 3-token
     * coordinate triple bound to {@code -coords}) light up at every value
     * position within the span, not just the first.
     */
    private @Nullable ProjectedFlag<S> findFlagValueTarget(
            ProjectedNode<S> scope,
            SuggestionContext<S> ctx,
            CompletionArg arg
    ) {
        int completionIndex = arg.index();
        if (completionIndex <= 0) {
            return null;
        }
        for (int back = 1; back <= completionIndex; back++) {
            int candidate = completionIndex - back;
            String token = ctx.arguments().getOr(candidate, null);
            if (token == null || !Patterns.isInputFlag(token)) {
                continue;
            }
            for (ProjectedFlag<S> flag : scope.flags()) {
                if (!flag.flag().flagData().acceptsInput(token)) {
                    continue;
                }
                if (flag.isSwitch()) {
                    return null;
                }
                if (back <= valueArityOf(flag.flag())) {
                    return flag;
                }
                return null;
            }
            return null;
        }
        return null;
    }

    private int valueArityOf(FlagArgument<S> flag) {
        var inputType = flag.flagData().inputType();
        if (inputType == null) {
            return 1;
        }
        return Math.max(1, inputType.getNumberOfParametersToConsume(flag));
    }

    private List<String> collectFlagValueSuggestions(SuggestionContext<S> ctx, FlagArgument<S> flag) {
        var provider = flag.inputSuggestionResolver();
        if (provider != null) {
            List<String> result = provider.provide(ctx, flag);
            return result == null ? List.of() : result;
        }
        var inputType = flag.flagData().inputType();
        if (inputType == null) {
            return List.of();
        }
        var inputProvider = inputType.getSuggestionProvider();
        if (inputProvider == null) {
            return List.of();
        }
        List<String> result = inputProvider.provide(ctx, flag);
        return result == null ? List.of() : result;
    }

    private @NotNull SuggestionContext<S> createSuggestionContext(
            Command<S> command,
            Object rawSource,
            String rawInput
    ) {
        S source = wrapCommandSource(rawSource);
        String input = normalizeInput(rawInput);
        int firstSpaceIndex = input.indexOf(' ');
        String label = firstSpaceIndex == -1 ? input : input.substring(0, firstSpaceIndex);
        boolean endsWithSpace = !input.isEmpty() && Character.isWhitespace(input.charAt(input.length() - 1));
        int argumentsStart = firstSpaceIndex == -1 ? input.length() : firstSpaceIndex + 1;
        int argumentsEnd = endsWithSpace ? input.length() - 1 : input.length();
        String argumentsSection = argumentsStart >= argumentsEnd
                                          ? ""
                                          : input.substring(argumentsStart, argumentsEnd);
        ArgumentInput args = ArgumentInput.parseAutoCompletion(argumentsSection, endsWithSpace);
        return dispatcher.config().getContextFactory().createSuggestionContext(dispatcher, source, command, label, args);
    }

    private boolean shouldSuggestFlagNames(CompletionArg arg) {
        return arg.isEmpty() || arg.value().startsWith("-");
    }

    private int resolveSuggestionStart(String rawInput, CompletionArg arg) {
        if (arg.isEmpty()) {
            return rawInput.length();
        }
        return Math.max(0, rawInput.length() - arg.value().length());
    }

    private String normalizeInput(String input) {
        while (input.startsWith("/")) {
            input = input.substring(1);
        }
        return input;
    }

    private String flagFallbackName(ProjectedNode<S> scope) {
        return "_imperat_flags_" + nodeDepth(scope);
    }

    private int nodeDepth(ProjectedNode<S> scope) {
        int depth = 0;
        var current = scope.sourceNode();
        while (current.getParent() != null) {
            depth++;
            current = current.getParent();
        }
        return depth;
    }

    private void executor(ArgumentBuilder<?, ?> builder) {
        builder.executes((context) -> {
            String input = context.getInput();
            S sender = this.wrapCommandSource(context.getSource());
            dispatcher.execute(sender, input);
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        });
    }


    protected StringArgumentType getStringArgType(Argument<S> parameter) {
        if (parameter.isGreedy()) {
            return StringArgumentType.greedyString();
        } else {
            return StringArgumentType.string();
        }
    }

}
