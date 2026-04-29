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

import java.util.List;
import java.util.Locale;

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
        // Order matters for Brigadier: optional + child literals BEFORE the
        // catch-all greedy flag fallback so the parse picks the more
        // specific path first. With the fallback registered first, an empty
        // input parsed cleanly into the greedy node and the client never
        // saw the optional's suggestions.
        appendOptionalContinuation(rootCommand, scope, parentBuilder, optionalIndex);
        appendChildContinuations(rootCommand, scope, parentBuilder);
        appendFlagFallback(rootCommand, scope, parentBuilder);
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
     * Registers each scope flag as a pair of linked Brigadier nodes:
     * <ul>
     *   <li>A {@link LiteralArgumentBuilder literal} per alias
     *       ({@code -force}, {@code -f}) — child of {@code parentBuilder}.</li>
     *   <li>For value flags, a child {@link RequiredArgumentBuilder required arg}
     *       holding the flag's input value, with its own suggestion
     *       provider sourced from the flag's
     *       {@link FlagArgument#inputSuggestionResolver()}.</li>
     * </ul>
     *
     * <p>Switches expose only the literal — no value child. Both nodes carry
     * an {@code executes} callback so partial input (e.g. {@code /fly -force}
     * with no value) still dispatches into Imperat for proper error handling.
     * Visibility on each node mirrors the framework's
     * {@link #isFlagVisible(Command, ProjectedFlag, CommandSource)} check.</p>
     *
     * <p>Replaces the legacy single-node "greedy fallback" that confused
     * Mojang's tab UI by competing with sibling positional args for the
     * empty-input parse — proper Brigadier branches give the client a
     * standard tree to render.</p>
     */
    private <BS> void appendFlagFallback(
            Command<S> command,
            ProjectedNode<S> scope,
            ArgumentBuilder<BS, ?> parentBuilder
    ) {
        if (scope.flags().isEmpty()) {
            return;
        }

        for (ProjectedFlag<S> projectedFlag : scope.flags()) {
            FlagArgument<S> flag = projectedFlag.flag();
            for (String alias : projectedFlag.aliases()) {
                LiteralArgumentBuilder<BS> flagLiteral = LiteralArgumentBuilder.literal("-" + alias);
                flagLiteral.requires((obj) -> isFlagVisible(command, projectedFlag, wrapCommandSource(obj)));
                executor(flagLiteral);

                if (!projectedFlag.isSwitch()) {
                    RequiredArgumentBuilder<BS, String> valueBuilder =
                            RequiredArgumentBuilder.argument(alias + "_value", StringArgumentType.string());
                    valueBuilder.requires((obj) -> isFlagVisible(command, projectedFlag, wrapCommandSource(obj)));
                    valueBuilder.suggests(createFlagValueProvider(command, projectedFlag));
                    executor(valueBuilder);
                    flagLiteral.then(valueBuilder);
                }

                parentBuilder.then(flagLiteral.build());
            }
        }
    }

    private @NotNull <BS> com.mojang.brigadier.suggestion.SuggestionProvider<BS> createFlagValueProvider(
            Command<S> command,
            ProjectedFlag<S> projectedFlag
    ) {
        FlagArgument<S> flag = projectedFlag.flag();
        return (context, builder) -> {
            SuggestionContext<S> ctx = createSuggestionContext(command, context.getSource(), context.getInput());
            CompletionArg arg = ctx.getArgToComplete();
            String prefix = arg.isEmpty() ? "" : arg.value().toLowerCase(Locale.ROOT);

            List<String> values = collectFlagValueSuggestions(ctx, flag);
            for (String value : values) {
                if (value == null || value.isEmpty()) {
                    continue;
                }
                if (prefix.isEmpty() || value.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    builder.suggest(value);
                }
            }
            return builder.buildFuture();
        };
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

            // Realign suggestions to the actual arg-token start so the
            // client renders them at the cursor instead of overwriting
            // earlier tokens. Brigadier's default builder.start can be the
            // start of an earlier node (e.g. the literal) when parse picks
            // a different sibling — without this, suggestions silently get
            // dropped client-side because their replace-range is wrong.
            var alignedBuilder = builder.createOffset(resolveSuggestionStart(context.getInput(), arg));
            String prefix = arg.isEmpty() ? "" : arg.value().toLowerCase();

            return dispatcher.config().getParameterSuggestionResolver(parameter).provideAsynchronously(ctx, parameter)
                           .thenCompose((results) -> {
                               results
                                       .stream()
                                       .filter((candidate) -> prefix.isEmpty()
                                                                      || candidate.toLowerCase().startsWith(prefix))
                                       .forEachOrdered((result) -> alignedBuilder.suggest(result, tooltip));
                               return alignedBuilder.buildFuture();
                           });
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
