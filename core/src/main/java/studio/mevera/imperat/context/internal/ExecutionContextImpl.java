package studio.mevera.imperat.context.internal;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.FlagArgument;
import studio.mevera.imperat.command.tree.CommandTreeMatch;
import studio.mevera.imperat.command.tree.ParseResult;
import studio.mevera.imperat.command.tree.ParsedNode;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.ParsedArgument;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.InvalidSyntaxException;
import studio.mevera.imperat.util.Registry;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;


@ApiStatus.Internal
final class ExecutionContextImpl<S extends CommandSource> extends ContextImpl<S> implements ExecutionContext<S> {

    private CommandPathway<S> pathway;
    private final Registry<String, ParsedFlagArgument<S>> flagRegistry = new Registry<>();
    //per command/subcommand because the class 'Command' can be also treated as a sub command
    private final Registry<Command<S>, Registry<String, ParsedArgument<S>>> resolvedArgumentsPerCommand = new Registry<>(LinkedHashMap::new);
    //all resolved arguments EXCEPT for subcommands and flags.
    private final Registry<String, ParsedArgument<S>> allResolvedArgs = new Registry<>(LinkedHashMap::new);

    private CommandTreeMatch<S> treeMatch = null;
    //last command used
    private Command<S> lastCommand;

    ExecutionContextImpl(
            CommandContext<S> context,
            @Nullable CommandPathway<S> pathway,
            Command<S> lastCommand
    ) {
        super(context.imperat(), context.command(), context.source(), context.getRootCommandLabelUsed(), context.arguments());
        this.lastCommand = lastCommand;
        this.pathway = pathway;
    }

    /**
     * Fetches the arguments of a command/subcommand that got resolved
     * except for the arguments that represent the literal/subcommand name arguments
     *
     * @param command the command/subcommand owning the argument
     * @param name    the name of the argument
     * @return the argument resolved from raw into a value
     */
    @Override
    public @Nullable ParsedArgument<S> getParsedArgument(Command<S> command, String name) {
        return resolvedArgumentsPerCommand.getData(command)
                       .flatMap((resolvedArgs) -> resolvedArgs.getData(name))
                       .orElse(null);
    }

    @Override
    public @Nullable ParsedArgument<S> getParsedArgument(String argumentName) {
        return allResolvedArgs.getData(argumentName).orElse(null);
    }

    /**
     * @param command the command/subcommand with certain args
     * @return the command/subcommand's resolved args in as a new array-list
     */
    @Override
    public List<ParsedArgument<S>> getParsedArguments(Command<S> command) {
        return resolvedArgumentsPerCommand.getData(command)
                       .map((argMap) -> (List<ParsedArgument<S>>) new ArrayList<ParsedArgument<S>>(argMap.getAll()))
                       .orElse(Collections.emptyList());
    }

    /**
     * @return all {@link Command} that have been used in this context
     */
    @Override
    public @NotNull Iterable<? extends Command<S>> getCommandsUsed() {
        return resolvedArgumentsPerCommand.getKeys();
    }

    /**
     * @return an ordered collection of {@link ParsedArgument} just like how they were entered
     * NOTE: the flags are NOT included as a resolved argument, it's treated differently
     */
    @Override
    public Collection<? extends ParsedArgument<S>> getParsedArguments() {
        return allResolvedArgs.getAll();
    }

    /**
     * Fetches a resolved argument's value
     *
     * @param name the name of the command
     * @return the value of the resolved argument
     * @see ParsedArgument
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T getArgument(String name) {
        return (T) allResolvedArgs.getData(name).map(ParsedArgument::getArgumentParsedValue)
                           .orElse(null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> @NotNull R provideSource(Type type) throws CommandException {
        if (!imperatConfig.hasSourceResolver(type)) {
            throw new IllegalArgumentException("Found no SourceProvider for valueType `" + type.getTypeName() + "`");
        }
        var sourceResolver = imperatConfig.getSourceProviderFor(type);
        assert sourceResolver != null;

        return (R) sourceResolver.resolve(this.source(), this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T getContextArgument(Class<T> type) throws CommandException {
        var resolver = imperatConfig.getContextArgumentProvider(type);
        return resolver == null ? null : (T) resolver.provide(this, null);
    }

    /**
     * @return the resolved flag arguments
     */
    @Override
    public Collection<? extends ParsedFlagArgument<S>> getResolvedFlags() {
        return flagRegistry.getAll();
    }

    @Override
    public void parseArgument(ParsedArgument<S> parsedArgument) throws CommandException {
        var argument = parsedArgument.getOriginalArgument();
        if (!argument.isCommand()) {
            argument.validate(this, parsedArgument);
        }
        resolvedArgumentsPerCommand.update(getLastUsedCommand(), (existingResolvedArgs) -> {
            if (existingResolvedArgs != null) {
                return existingResolvedArgs.setData(argument.getName(), parsedArgument);
            }
            return new Registry<>(argument.getName(), parsedArgument, LinkedHashMap::new);
        });
        allResolvedArgs.setData(argument.getName(), parsedArgument);
    }


    @Override
    public Optional<ParsedFlagArgument<S>> getFlag(String flagName) {
        return flagRegistry.getData(flagName);
    }

    /**
     * Fetches the flag input value
     * returns null if the flag is a switch
     * OR if the value hasn't been resolved somehow
     *
     * @param flagName the flag name
     * @return the resolved value of the flag input
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T getFlagValue(String flagName) {
        return (T) getFlag(flagName)
                           .map(ParsedArgument::getArgumentParsedValue)
                           .orElse(null);
    }

    /**
     * @return The used usage to use it to resolve commands
     */
    @Override
    public CommandPathway<S> getDetectedPathway() {
        return pathway;
    }

    @Override
    public void resolveFlag(ParsedFlagArgument<S> flag) throws CommandException {
        flag.getOriginalArgument().validate(this, flag);
        flagRegistry.setData(flag.getOriginalArgument().getName(), flag);
    }

    /**
     * Fetches the last used resolved command
     * of a resolved context!
     *
     * @return the last used command/subcommand
     */
    @Override
    public @NotNull Command<S> getLastUsedCommand() {
        return lastCommand;
    }

    @Override
    public void setLastUsedCommand(@NotNull Command<S> command) {
        this.lastCommand = command;
    }

    @Override
    public void setDetectedPathway(CommandPathway<S> pathway) {
        this.pathway = pathway;
        this.lastCommand = resolveLastCommand(pathway, this.command());
    }

    @Override
    public boolean hasResolvedFlag(FlagData<S> flagData) {
        return flagRegistry.getData(flagData.name())
                       .isPresent();
    }

    @Override
    public CommandTreeMatch<S> getTreeMatch() {
        if (treeMatch == null) {
            throw new RuntimeException("TreeMatch hasn't been initialized yet!");
        }
        return treeMatch;
    }

    @Override
    public void setTreeMatch(CommandTreeMatch<S> treeMatch) {
        this.treeMatch = treeMatch;
    }

    /**
     * Drains the tree's parse-result chain into this context's registries.
     *
     * <p>The tree walk is the single source of truth: each {@link ParsedNode}
     * already carries fully-resolved {@link ParseResult}s for its main argument,
     * its bound optionals, and any inline flags. Trailing flags (those that
     * appear after the last positional/optional consumed by the chain) live on
     * a dedicated channel inside {@link CommandTreeMatch} so they can carry
     * parse errors without polluting the tree's failure-penalty scoring.</p>
     *
     * <p>This method:
     * <ol>
     *   <li>Iterates the chain and binds command literals, flags, and positional
     *       arguments. Required positionals with errors throw immediately;
     *       optional ones fall back to their declared default.</li>
     *   <li>Drains the trailing-flag channel through the same flag-binding logic
     *       so malformed value-flags surface their error here, not silently.</li>
     *   <li>Materialises declared defaults for any pathway argument or flag the
     *       chain never reached.</li>
     *   <li>Verifies all required positionals were satisfied and that no
     *       trailing input remains beyond what greedy-limit rules permit.</li>
     * </ol></p>
     */
    @Override
    public void parse(List<ParsedNode<S>> parsedNodes) throws Throwable {
        CommandPathway<S> usage = getDetectedPathway();
        if (usage == null) {
            return;
        }

        Set<String> resolvedNames = new HashSet<>();
        for (ParsedNode<S> parsedNode : parsedNodes) {
            for (ParseResult<S> result : parsedNode.getParseResults().values()) {
                Argument<S> arg = result.getArgument();
                if (!resolvedNames.add(arg.getName())) {
                    continue;
                }
                applyParseResult(arg, result);
            }
        }

        Map<String, ParseResult<S>> trailingFlags = treeMatch == null
                                                            ? Collections.emptyMap()
                                                            : treeMatch.trailingFlagResults();
        for (ParseResult<S> result : trailingFlags.values()) {
            Argument<S> arg = result.getArgument();
            if (!arg.isFlag()) {
                continue;
            }
            applyFlagResult(arg.asFlagParameter(), result);
        }

        materialiseDeclaredDefaults(usage, resolvedNames);
        materialiseUnresolvedFlagDefaults(usage);
        validatePresenceOfRequired(usage);
        validateNoTrailingInput(usage);
    }

    /**
     * Routes a single {@link ParseResult} from the tree to the correct registry.
     */
    @SuppressWarnings("unchecked")
    private void applyParseResult(Argument<S> arg, ParseResult<S> result) throws Throwable {
        if (arg.isCommand()) {
            Object resolvedValue = result.getParsedValue();
            Command<S> resolvedCommand = (resolvedValue instanceof Command<?>)
                                                 ? (Command<S>) resolvedValue
                                                 : arg.asCommand();
            setLastUsedCommand(resolvedCommand);
            parseArgument(new ParsedArgument<>(result.getInput(), arg, resolvedCommand));
            return;
        }

        if (arg.isFlag()) {
            applyFlagResult(arg.asFlagParameter(), result);
            return;
        }

        if (result.getError() != null && !arg.isOptional()) {
            throw result.getError();
        }
        Object value = result.getParsedValue();
        if (value == null && arg.isOptional()) {
            value = getDefaultValue(arg);
        }
        parseArgument(new ParsedArgument<>(result.getInput(), arg, value));
    }

    /**
     * Common flag-binding path used both for chain-resident flags (inside a
     * node's optional/flag span) and trailing flags (post-chain, carried on a
     * separate channel by the tree match).
     *
     * <p>Note: {@link ParsedFlagArgument#forSwitch} is reserved for switches
     * that <em>did not appear</em> in the input — its constructor hard-codes
     * the parsed value to {@code false}. For switches that actually fired we
     * must use {@link ParsedFlagArgument#forFlag} with {@code Boolean.TRUE},
     * matching the legacy {@code resolveInputFlags} semantics.</p>
     */
    private void applyFlagResult(FlagArgument<S> flag, ParseResult<S> result) throws Throwable {
        if (result.getError() != null) {
            throw result.getError();
        }
        Object value = flag.isSwitch() ? Boolean.TRUE : result.getParsedValue();
        resolveFlag(ParsedFlagArgument.forFlag(
                flag,
                result.getInput(),
                result.getInput(),
                -1,
                -1,
                value
        ));
    }

    /**
     * For every optional positional argument declared on the pathway that the
     * chain never bound, materialise its declared default value and register it
     * so handler injection sees a complete argument set.
     */
    private void materialiseDeclaredDefaults(CommandPathway<S> usage, Set<String> resolvedNames) throws CommandException {
        for (Argument<S> arg : usage) {
            if (arg.isCommand() || arg.isFlag()) {
                continue;
            }
            if (resolvedNames.contains(arg.getName())) {
                continue;
            }
            if (!arg.isOptional()) {
                // Required arguments are validated by validatePresenceOfRequired.
                continue;
            }
            Object defaultValue = getDefaultValue(arg);
            parseArgument(new ParsedArgument<>(null, arg, defaultValue));
            resolvedNames.add(arg.getName());
        }
    }

    private void materialiseUnresolvedFlagDefaults(CommandPathway<S> usage) throws CommandException {
        for (FlagArgument<S> registered : usage.getFlagExtractor().getRegisteredFlags()) {
            if (hasResolvedFlag(registered.flagData())) {
                continue;
            }
            resolveFlagDefaultValue(registered);
        }
    }

    private void validatePresenceOfRequired(CommandPathway<S> usage) throws InvalidSyntaxException {
        for (Argument<S> arg : usage) {
            if (!arg.isRequired() || arg.isCommand() || arg.isFlag()) {
                continue;
            }
            if (allResolvedArgs.getData(arg.getName()).isEmpty()) {
                throw invalidSyntax(usage);
            }
        }
    }

    private void validateNoTrailingInput(CommandPathway<S> usage) throws InvalidSyntaxException {
        if (treeMatch == null) {
            return;
        }
        int consumedIndex = treeMatch.consumedIndex();
        if (consumedIndex + 1 >= arguments().size()) {
            return;
        }
        if (canIgnoreTrailingRawAfterLimitedGreedy(usage)) {
            return;
        }
        throw invalidSyntax(usage);
    }

    private boolean canIgnoreTrailingRawAfterLimitedGreedy(CommandPathway<S> usage) {
        for (int index = usage.size() - 1; index >= 0; index--) {
            Argument<S> argument = usage.getArgumentAt(index);
            if (argument == null || argument.isFlag() || argument.isCommand()) {
                continue;
            }
            return (argument.isGreedy() || argument.type().isGreedy(argument)) && argument.greedyLimit() > 0;
        }
        return false;
    }

    private Object getDefaultValue(Argument<S> argument) throws CommandException {
        var supplier = argument.getDefaultValueSupplier();
        if (supplier.isEmpty()) {
            return null;
        }
        String raw = supplier.provide(this, argument);
        if (raw == null) {
            return null;
        }
        return argument.type().parse(
                this,
                argument,
                studio.mevera.imperat.command.arguments.type.Cursor.single(this, raw)
        );
    }

    private void resolveFlagDefaultValue(FlagArgument<S> flagArgument) throws CommandException {
        if (flagArgument.isSwitch()) {
            this.resolveFlag(ParsedFlagArgument.forDefaultSwitch(flagArgument));
            return;
        }

        String defValue = flagArgument.getDefaultValueSupplier().provide(this, flagArgument);
        if (defValue != null) {
            Object flagValueResolved = flagArgument.getDefaultValueSupplier().isEmpty() ? null
                                               : Objects.requireNonNull(flagArgument.flagData().inputType()).parse(
                    this,
                    flagArgument,
                    studio.mevera.imperat.command.arguments.type.Cursor.single(this, defValue)
            );
            this.resolveFlag(ParsedFlagArgument.forDefaultFlag(flagArgument, defValue, flagValueResolved));
        }
    }

    private InvalidSyntaxException invalidSyntax(CommandPathway<S> usage) {
        StringBuilder invalidUsage = new StringBuilder(this.getRootCommandLabelUsed());
        for (String raw : arguments()) {
            invalidUsage.append(" ").append(raw);
        }
        return new InvalidSyntaxException(invalidUsage.toString(), usage);
    }

    private @NotNull Command<S> resolveLastCommand(@Nullable CommandPathway<S> pathway, @NotNull Command<S> fallback) {
        if (pathway == null) {
            return fallback;
        }
        Command<S> resolved = fallback;
        for (Argument<S> argument : pathway.getArguments()) {
            if (argument.isCommand()) {
                resolved = argument.asCommand();
            }
        }
        return resolved;
    }

}
