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
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.FlagArgument;
import studio.mevera.imperat.command.suggestions.CompletionArg;
import studio.mevera.imperat.command.tree.projection.CommandTreeProjection;
import studio.mevera.imperat.command.tree.projection.ProjectedFlag;
import studio.mevera.imperat.command.tree.projection.ProjectedNode;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.SuggestionContext;

import java.util.HashSet;
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
        Argument<S> main = projected.mainArgument();

        // SimpleArgumentType / similar fixed-arity Imperat types declare
        // their token count via {@code getNumberOfParametersToConsume}.
        // Brigadier requires one node per consumed token for the client-side
        // tree to render the input as N segments — a single Brigadier node
        // would only consume the first whitespace-separated token and paint
        // the rest grey/red. Detect the count and chain N-1 string-typed
        // filler nodes after the head; the deepest node owns the executor
        // and continuations so children/optionals/flags only surface after
        // the user has typed all N tokens.
        int tokenCount = tokenCountOf(main);
        if (tokenCount > 1) {
            return chainMultiTokenNode(rootCommand, projected, tokenCount);
        }

        ArgumentBuilder<BS, ?> childBuilder = createBrigadierBuilder(rootCommand, projected);
        executor(childBuilder);
        if (!main.isCommand()) {
            ((RequiredArgumentBuilder<BS, ?>) childBuilder).suggests(
                    createSuggestionProvider(rootCommand, main)
            );
        }

        appendContinuations(rootCommand, projected, childBuilder, 0);
        return childBuilder.build();
    }

    /**
     * Splits {@code argument.format()} on whitespace after stripping the
     * angle / square brackets ({@code <}, {@code >}, {@code [}, {@code ]})
     * and returns the part names if and only if the count matches
     * {@code expectedCount} exactly. {@code null} otherwise — callers fall
     * back to the auto-generated {@code _partN} naming scheme.
     *
     * <p>Lets users opt into named multi-token Brigadier segments by
     * authoring a {@code @Format} like {@code "<chunkX chunkZ>"} — the
     * client then renders the chain as {@code <chunkX> <chunkZ>} instead
     * of {@code <chunk> <chunk_part2>}.</p>
     */
    private @Nullable String[] derivePartNamesFromFormat(Argument<S> argument, int expectedCount) {
        String format = argument.format();
        if (format == null || format.isBlank()) {
            return null;
        }
        String cleaned = format
                                 .replace("<", "")
                                 .replace(">", "")
                                 .replace("[", "")
                                 .replace("]", "")
                                 .trim();
        if (cleaned.isEmpty() || !cleaned.contains(" ")) {
            return null;
        }
        String[] parts = cleaned.split("\\s+");
        if (parts.length != expectedCount) {
            return null;
        }
        for (String part : parts) {
            if (part.isBlank()) {
                return null;
            }
        }
        return parts;
    }

    /**
     * Returns the fixed-arity token count for {@code argument}, or {@code 1}
     * for command literals, greedy types, and any type whose
     * {@link studio.mevera.imperat.command.arguments.type.ArgumentType#getNumberOfParametersToConsume}
     * throws — those don't fit the chain-of-nodes shape (greedy/complex
     * types stay as a single Brigadier node and consume their own tokens
     * server-side via {@link #executor}).
     */
    private int tokenCountOf(Argument<S> argument) {
        if (argument.isCommand() || argument.isGreedy()) {
            return 1;
        }
        try {
            return Math.max(1, argument.type().getNumberOfParametersToConsume(argument));
        } catch (Throwable ignored) {
            return 1;
        }
    }

    /**
     * Builds the N-token chain for a fixed-arity positional argument:
     * head node carrying the proper Brigadier {@code ArgumentType} +
     * {@code tokenCount - 1} string-typed filler nodes. Each node is
     * independently {@link #executor executable} so partial-input
     * dispatch still routes through Imperat (the user gets a typed
     * parse error rather than a Brigadier syntax fail). Suggestions on
     * every node delegate to the same Imperat-side suggester so client
     * autocomplete reads the same list at every segment. Only the
     * DEEPEST node receives {@link #appendContinuations} — children,
     * optionals, and flags reachable from the projected scope come
     * after all N tokens, matching how Imperat's tree consumes them.
     *
     * <p>Per-segment naming: if {@link #derivePartNamesFromFormat}
     * yields N parts the chain uses those (head → parts[0], filler[i]
     * → parts[i+1]). Otherwise head keeps {@code argument.getName()}
     * and fillers fall back to the auto-generated {@code _partN}
     * scheme.</p>
     */
    private <BS> CommandNode<BS> chainMultiTokenNode(
            Command<S> rootCommand,
            ProjectedNode<S> projected,
            int tokenCount
    ) {
        Argument<S> main = projected.mainArgument();
        String[] partNames = derivePartNamesFromFormat(main, tokenCount);
        java.util.function.Predicate<Object> visibility =
                (obj) -> isNodeVisible(rootCommand, projected, wrapCommandSource(obj));

        String headName = partNames != null ? partNames[0] : main.getName();
        @SuppressWarnings({"rawtypes", "unchecked"})
        RequiredArgumentBuilder<BS, ?> head = (RequiredArgumentBuilder)
                                                      RequiredArgumentBuilder.argument(headName, getArgumentType(main));
        head.requires(visibility::test);
        head.suggests(createSuggestionProvider(rootCommand, main));
        executor(head);

        ArgumentBuilder<BS, ?> deepest = appendStringTokenFillers(
                rootCommand, main, head, tokenCount, visibility, partNames
        );
        appendContinuations(rootCommand, projected, deepest, 0);
        return head.build();
    }

    /**
     * Chains {@code tokenCount - 1} string-typed filler nodes after
     * {@code head}, returning the deepest builder. Used by both the
     * positional-arg path ({@link #chainMultiTokenNode}) and the
     * optional-arg path ({@link #appendOptionalContinuation}) so
     * fixed-arity Imperat types render with one Brigadier node per
     * consumed token regardless of where they appear. Each filler
     * shares the head's visibility predicate + the same Imperat-side
     * suggester so autocomplete reads the same list at every segment.
     *
     * <p>Filler naming: if {@code partNames} is non-null filler {@code i}
     * (1-indexed) uses {@code partNames[i]}. Otherwise the auto-generated
     * {@code <argName>_part<i+1>} scheme is used.</p>
     */
    private <BS> ArgumentBuilder<BS, ?> appendStringTokenFillers(
            Command<S> rootCommand,
            Argument<S> argument,
            ArgumentBuilder<BS, ?> head,
            int tokenCount,
            java.util.function.Predicate<Object> visibility,
            @Nullable String[] partNames
    ) {
        ArgumentBuilder<BS, ?> deepest = head;
        for (int i = 1; i < tokenCount; i++) {
            String fillerName = partNames != null
                                        ? partNames[i]
                                        : argument.getName() + "_part" + (i + 1);
            @SuppressWarnings({"rawtypes", "unchecked"})
            RequiredArgumentBuilder<BS, ?> filler = (RequiredArgumentBuilder)
                                                            RequiredArgumentBuilder.argument(fillerName, StringArgumentType.string());
            filler.requires(visibility::test);
            filler.suggests(createSuggestionProvider(rootCommand, argument));
            executor(filler);
            deepest.then(filler);
            deepest = filler;
        }
        return deepest;
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
        appendContinuations(rootCommand, scope, parentBuilder, optionalIndex, Set.of());
    }

    private <BS> void appendContinuations(
            Command<S> rootCommand,
            ProjectedNode<S> scope,
            ArgumentBuilder<BS, ?> parentBuilder,
            int optionalIndex,
            Set<ProjectedFlag<S>> excludedFlags
    ) {
        // Order matters for Brigadier: optional + child literals BEFORE the
        // catch-all greedy flag fallback so the parse picks the more
        // specific path first. With the fallback registered first, an empty
        // input parsed cleanly into the greedy node and the client never
        // saw the optional's suggestions.
        appendOptionalContinuation(rootCommand, scope, parentBuilder, optionalIndex, excludedFlags);
        appendChildContinuations(rootCommand, scope, parentBuilder);
        appendFlagFallback(rootCommand, scope, parentBuilder, excludedFlags);
        // Catch-all sibling for the inline `-name=value` form. Stock
        // StringArgumentType halts at `=` so the bare positional sibling
        // leaves `=value` orphaned and the client renders red. This node
        // consumes the WHOLE inline-flag token so the parse succeeds
        // (green) — suggestions delegate back to the Imperat tree.
        appendInlineFlagSibling(rootCommand, scope, parentBuilder);
    }

    /**
     * Catch-all sibling per scope: a {@code <flag>} required-argument node
     * whose custom {@link InlineFlagArgumentType} consumes a whole
     * {@code -name=value} token. Falls through to other siblings for
     * non-flag input. Suggestions delegate to Imperat's tree suggester
     * (which formats inline values via the same single-node logic).
     */
    private <BS> void appendInlineFlagSibling(
            Command<S> rootCommand,
            ProjectedNode<S> scope,
            ArgumentBuilder<BS, ?> parentBuilder
    ) {
        if (scope.flags().isEmpty()) {
            return;
        }
        com.mojang.brigadier.arguments.ArgumentType<?> inlineFlagType = inlineFlagArgumentType();
        if (inlineFlagType == null) {
            // Backend opted out (e.g. modern Paper before a CustomArgumentType
            // wrapper is wired up). Inline `=`-form will render red on the
            // client, but completions still flow through the
            // sibling-positional suggester wrapper that delegates to the
            // Imperat tree.
            return;
        }
        @SuppressWarnings({"unchecked", "rawtypes"})
        RequiredArgumentBuilder<BS, ?> inlineFlagBuilder =
                (RequiredArgumentBuilder) RequiredArgumentBuilder.argument("flag", inlineFlagType);
        inlineFlagBuilder.requires((obj) -> {
            S source = wrapCommandSource(obj);
            if (rootCommand.isIgnoringACPerms()) {
                return true;
            }
            // Show this catch-all whenever ANY flag in the scope is visible
            // — fine-grained filtering happens in the suggestion provider.
            for (ProjectedFlag<S> flag : scope.flags()) {
                if (isFlagVisible(rootCommand, flag, source)) {
                    return true;
                }
            }
            return false;
        });
        inlineFlagBuilder.suggests(createInlineFlagSuggestionProvider(rootCommand));
        executor(inlineFlagBuilder);
        parentBuilder.then(inlineFlagBuilder.build());
    }

    private @NotNull <BS> com.mojang.brigadier.suggestion.SuggestionProvider<BS>
    createInlineFlagSuggestionProvider(Command<S> command) {
        return (context, builder) -> {
            SuggestionContext<S> ctx = createSuggestionContext(command, context.getSource(), context.getInput());
            CompletionArg arg = ctx.getArgToComplete();
            var alignedBuilder = builder.createOffset(resolveSuggestionStart(context.getInput(), arg));
            for (String suggestion : command.tree().tabComplete(ctx)) {
                if (suggestion == null || suggestion.isEmpty()) {
                    continue;
                }
                alignedBuilder.suggest(suggestion);
            }
            return alignedBuilder.buildFuture();
        };
    }

    private <BS> void appendOptionalContinuation(
            Command<S> rootCommand,
            ProjectedNode<S> scope,
            ArgumentBuilder<BS, ?> parentBuilder,
            int optionalIndex,
            Set<ProjectedFlag<S>> excludedFlags
    ) {
        List<Argument<S>> optionals = scope.optionalArguments();
        if (optionalIndex >= optionals.size()) {
            return;
        }

        Argument<S> optional = optionals.get(optionalIndex);
        int tokenCount = tokenCountOf(optional);
        String[] partNames = tokenCount > 1
                                     ? derivePartNamesFromFormat(optional, tokenCount)
                                     : null;
        java.util.function.Predicate<Object> visibility =
                (obj) -> isArgumentVisible(rootCommand, optional, scope.originalPathway(), wrapCommandSource(obj));

        // Use the format-derived head name when available so multi-token
        // optionals match their declared format ([chunkX chunkZ] →
        // [chunkX]). Falls back to the argument's own name for the
        // single-token case + the auto-generated {@code _partN} scheme
        // when the format doesn't split evenly.
        String headName = partNames != null ? partNames[0] : optional.getName();
        RequiredArgumentBuilder<BS, ?> optionalBuilder =
                RequiredArgumentBuilder.argument(headName, getArgumentType(optional));
        optionalBuilder.requires(visibility::test);
        optionalBuilder.suggests(createSuggestionProvider(rootCommand, optional));
        executor(optionalBuilder);

        // Chain string-typed filler nodes for fixed-arity types whose
        // {@code getNumberOfParametersToConsume} > 1, so multi-token
        // optionals render as N segments client-side too. Continuation
        // attaches to the deepest filler so the next optional / sibling
        // arrives only after all N tokens.
        ArgumentBuilder<BS, ?> deepest = tokenCount > 1
                                                 ? appendStringTokenFillers(
                rootCommand, optional, optionalBuilder,
                tokenCount, visibility, partNames
        )
                                                 : optionalBuilder;

        appendContinuations(rootCommand, scope, deepest, optionalIndex + 1, excludedFlags);
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
            ArgumentBuilder<BS, ?> parentBuilder,
            Set<ProjectedFlag<S>> excludedFlags
    ) {
        if (scope.flags().isEmpty()) {
            return;
        }

        for (ProjectedFlag<S> projectedFlag : scope.flags()) {
            // Skip already-consumed flags in this Brigadier path. A single
            // excluded-flag check was insufficient: with N flags, each flag's
            // continuation would register the other N-1, whose continuations
            // register them back, causing unbounded mutual recursion.
            if (excludedFlags.contains(projectedFlag)) {
                continue;
            }
            // Permissive: `--<primary>` AND `-<primary>` both register, plus
            // `-<alias>` for each non-primary alias. Mirrors FlagExtractor's
            // single-dash primary fallback so client autocomplete shows the
            // same forms the parser accepts.
            String primary = projectedFlag.name();
            registerFlagLiteral(command, scope, projectedFlag, "--" + primary, primary, parentBuilder, excludedFlags);
            registerFlagLiteral(command, scope, projectedFlag, "-" + primary, primary, parentBuilder, excludedFlags);

            for (String alias : projectedFlag.aliases()) {
                if (alias.equals(primary)) {
                    continue;
                }
                registerFlagLiteral(command, scope, projectedFlag, "-" + alias, alias, parentBuilder, excludedFlags);
            }
        }
    }

    private <BS> void registerFlagLiteral(
            Command<S> command,
            ProjectedNode<S> scope,
            ProjectedFlag<S> projectedFlag,
            String literalName,
            String suffixForValueArg,
            ArgumentBuilder<BS, ?> parentBuilder,
            Set<ProjectedFlag<S>> excludedFlags
    ) {
        LiteralArgumentBuilder<BS> flagLiteral = LiteralArgumentBuilder.literal(literalName);
        flagLiteral.requires((obj) -> isFlagVisible(command, projectedFlag, wrapCommandSource(obj)));
        executor(flagLiteral);

        Set<ProjectedFlag<S>> nextExcluded = new HashSet<>(excludedFlags);
        nextExcluded.add(projectedFlag);

        if (!projectedFlag.isSwitch()) {
            // Resolve the flag's value-type to a Brigadier-native type so
            // client-side rendering matches whatever the input ArgumentType
            // declares (selector charset for TargetSelector, NBT braces for
            // ItemStack, etc.) — without this the value node is stuck on
            // stock `StringArgumentType.string()` which halts at `[`/`=`/`@`
            // and paints valid native syntax red.
            com.mojang.brigadier.arguments.ArgumentType<?> valueType =
                    getFlagValueArgumentType(projectedFlag.flag());
            @SuppressWarnings({"unchecked", "rawtypes"})
            RequiredArgumentBuilder<BS, ?> valueBuilder = (RequiredArgumentBuilder)
                                                                  RequiredArgumentBuilder.argument(suffixForValueArg + "_value", valueType);
            valueBuilder.requires((obj) -> isFlagVisible(command, projectedFlag, wrapCommandSource(obj)));
            // Prefer the value-type's own native suggestions (selector
            // filter keys, block-state keys, NBT path completions, etc.)
            // when the backend exposes them. Falls back to the
            // Imperat-side createFlagValueProvider for plain flags.
            SuggestionProvider<BS> nativeSugg = createNativeFlagValueSuggester(projectedFlag.flag());
            valueBuilder.suggests(nativeSugg != null ? nativeSugg : createFlagValueProvider(command, projectedFlag));
            executor(valueBuilder);
            appendContinuations(command, scope, valueBuilder, 0, nextExcluded);
            flagLiteral.then(valueBuilder);
        } else {
            // Switch — no value child. Attach continuations directly to the
            // literal so siblings still complete after `-switch`.
            appendContinuations(command, scope, flagLiteral, 0, nextExcluded);
        }

        parentBuilder.then(flagLiteral.build());
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

    protected @NotNull <BS> SuggestionProvider<BS> createSuggestionProvider(
            Command<S> command,
            Argument<S> parameter
    ) {

        return (context, builder) -> {
            SuggestionContext<S> ctx = createSuggestionContext(command, context.getSource(), context.getInput());
            CompletionArg arg = ctx.getArgToComplete();

            String paramFormat = parameter.format();
            Message tooltip = new LiteralMessage(paramFormat);

            // Realign suggestions to the actual arg-token start so the
            // client renders them at the cursor instead of overwriting
            // earlier tokens. Brigadier's default builder.start can be the
            // start of an earlier node (e.g. the literal) when parse picks
            // a different sibling — without this, suggestions silently get
            // dropped client-side because their replace-range is wrong.
            var alignedBuilder = builder.createOffset(resolveSuggestionStart(context.getInput(), arg));
            String prefix = arg.isEmpty() ? "" : arg.value().toLowerCase();

            // Inline-flag partial (`-name=` / `-name=partial`) is structurally
            // a single token, so it falls into whichever ArgumentNode parses
            // any string — usually a sibling positional. The positional's
            // own suggester has no idea how to complete a flag value, so we
            // delegate to Imperat's tree suggester which DOES handle the
            // inline form natively (single-node flag completion).
            String currentToken = arg.value();
            if (currentToken != null
                        && currentToken.indexOf('=') >= 0
                        && studio.mevera.imperat.util.Patterns.isInputFlag(currentToken)) {
                List<String> inline = command.tree().tabComplete(ctx);
                for (String suggestion : inline) {
                    if (suggestion == null || suggestion.isEmpty()) {
                        continue;
                    }
                    alignedBuilder.suggest(suggestion, tooltip);
                }
                return alignedBuilder.buildFuture();
            }

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


    /**
     * Argument type for the inline-flag catch-all sibling node. Default is
     * the platform-agnostic {@link InlineFlagArgumentType}. Backends whose
     * underlying registrar rejects raw Brigadier types (e.g. modern Paper's
     * {@code Commands} API requires {@code CustomArgumentType} wrappers)
     * MUST override to return a wrapped instance — or {@code null} to skip
     * registering the inline-flag sibling and accept red coloring on the
     * client side for inline {@code =}-form input.
     */
    protected @Nullable com.mojang.brigadier.arguments.ArgumentType<?> inlineFlagArgumentType() {
        return new InlineFlagArgumentType();
    }

    /**
     * Resolves the Brigadier {@link com.mojang.brigadier.arguments.ArgumentType}
     * to register for a flag's VALUE node — driven by the flag's
     * {@link FlagArgument#flagData() input type}. Default falls back to
     * {@link StringArgumentType#string()} (no native rendering).
     *
     * <p>Backends that map Imperat-side {@code ArgumentType}s onto native
     * Paper / Brigadier types (e.g. {@code ModernPaperBrigadierManager}'s
     * {@code PaperBukkitArgumentType} + {@code PaperNativeAware} bridges)
     * SHOULD override to delegate the flag's value-type lookup through the
     * same channel as positional arguments — keeps client coloring +
     * native autocomplete consistent between {@code <arg>} positional
     * nodes and {@code --flag <arg>} flag-value nodes.</p>
     */
    protected com.mojang.brigadier.arguments.@NotNull ArgumentType<?> getFlagValueArgumentType(
            @NotNull FlagArgument<S> flag
    ) {
        return StringArgumentType.string();
    }

    /**
     * Optional native suggestions delegate for a flag's value node. Default
     * returns {@code null} — the framework then uses the Imperat-side
     * {@code createFlagValueProvider} (built from the flag's
     * {@link FlagArgument#inputSuggestionResolver() inputSuggestionResolver}
     * + its input type's suggestion provider).
     *
     * <p>Backends that map flag value-types onto Brigadier-native
     * {@link com.mojang.brigadier.arguments.ArgumentType ArgumentTypes}
     * (e.g. {@code ModernPaperBrigadierManager} routing {@code TargetSelector}
     * to {@code ArgumentTypes.entities()}) SHOULD override and return a
     * provider that delegates to {@code nativeType.listSuggestions} so
     * the client gets selector filter keys / NBT keys / etc. directly
     * from Mojang's native parser instead of Imperat's flat string list.
     * Returning {@code null} means "fall back to the Imperat path".</p>
     */
    protected <BS> @Nullable SuggestionProvider<BS> createNativeFlagValueSuggester(
            @NotNull FlagArgument<S> flag
    ) {
        return null;
    }

    protected StringArgumentType getStringArgType(Argument<S> parameter) {
        if (parameter.isGreedy()) {
            return StringArgumentType.greedyString();
        } else {
            return StringArgumentType.string();
        }
    }

}
