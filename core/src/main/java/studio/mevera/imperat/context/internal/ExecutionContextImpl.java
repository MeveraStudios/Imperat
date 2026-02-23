package studio.mevera.imperat.context.internal;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.tree.CommandPathSearch;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.ParsedArgument;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.flow.ParameterValueAssigner;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.providers.ContextArgumentProvider;
import studio.mevera.imperat.util.ImperatDebugger;
import studio.mevera.imperat.util.Registry;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;


@ApiStatus.Internal
final class ExecutionContextImpl<S extends Source> extends ContextImpl<S> implements ExecutionContext<S> {

    private final CommandPathway<S> usage;
    private final Registry<String, ParsedFlagArgument<S>> flagRegistry = new Registry<>();
    //per command/subcommand because the class 'CommandProcessingChain' can be also treated as a sub command
    private final Registry<Command<S>, Registry<String, ParsedArgument<S>>> resolvedArgumentsPerCommand = new Registry<>(LinkedHashMap::new);
    //all resolved arguments EXCEPT for subcommands and flags.
    private final Registry<String, ParsedArgument<S>> allResolvedArgs = new Registry<>(LinkedHashMap::new);

    //last command used
    private final Command<S> lastCommand;

    private final CommandPathSearch<S> pathSearch;

    ExecutionContextImpl(
            Context<S> context,
            CommandPathSearch<S> pathSearch
    ) {
        super(context.imperat(), context.command(), context.source(), context.getRootCommandLabelUsed(), context.arguments());
        this.pathSearch = pathSearch;
        var lastCmdNode = pathSearch.getLastCommandNode();
        this.lastCommand = lastCmdNode.getData();
        this.usage = pathSearch.getFoundUsage();
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
    public @Nullable ParsedArgument<S> getResolvedArgument(Command<S> command, String name) {
        return resolvedArgumentsPerCommand.getData(command)
                       .flatMap((resolvedArgs) -> resolvedArgs.getData(name))
                       .orElse(null);
    }

    /**
     * @param command the command/subcommand with certain args
     * @return the command/subcommand's resolved args in as a new array-list
     */
    @Override
    public List<ParsedArgument<S>> getResolvedArguments(Command<S> command) {
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
    public Collection<? extends ParsedArgument<S>> getResolvedArguments() {
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
    public <R> @NotNull R getResolvedSource(Type type) throws CommandException {
        if (!imperatConfig.hasSourceResolver(type)) {
            throw new IllegalArgumentException("Found no SourceProvider for valueType `" + type.getTypeName() + "`");
        }
        var sourceResolver = imperatConfig.getSourceProviderFor(type);
        assert sourceResolver != null;

        return (R) sourceResolver.resolve(this.source(), this);
    }

    /**
     * Fetches the argument/input resolved by the context
     * using {@link ContextArgumentProvider}
     *
     * @param type valueType of argument to return
     * @return the argument/input resolved by the context
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T getContextResolvedArgument(Class<T> type) throws CommandException {
        var resolver = imperatConfig.getContextResolver(type);
        return resolver == null ? null : (T) resolver.resolve(this, null);
    }

    /**
     * @return the resolved flag arguments
     */
    @Override
    public Collection<? extends ParsedFlagArgument<S>> getResolvedFlags() {
        return flagRegistry.getAll();
    }


    @Override
    public @NotNull CommandPathSearch<S> getPathwaySearch() {
        return pathSearch;
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


    @Override
    public void resolve() throws CommandException {
        var resolver = ParameterValueAssigner.create(this, usage);
        resolver.resolve();
    }


    @Override
    public <T> void resolveArgument(
            @NotNull Cursor<S> cursor,
            @Nullable T value
    ) throws CommandException {
        var argument = cursor.currentParameterIfPresent();
        if (argument == null) {
            throw new IllegalStateException(
                    "No argument found at index " + cursor.position().parameter + " for command " + getLastUsedCommand().name());
        }

        String raw = cursor.currentRawIfPresent();
        if(argument.type().getNumberOfParametersToConsume() > 1) {
            StringBuilder builder = new StringBuilder();
            for (int i = cursor.position().parameter; i <= cursor.position().raw; i++)
                builder.append(arguments().get(i)).append(" ");
            raw = builder.toString();
        }

        final ParsedArgument<S> parsedArgument = new ParsedArgument<>(raw, argument, cursor.position().parameter, value);
        resolveArgument(parsedArgument);
    }

    @Override
    public void resolveArgument(ParsedArgument<S> parsedArgument) throws CommandException {
        var argument = parsedArgument.getOriginalArgument();
        argument.validate(this, parsedArgument);
        resolvedArgumentsPerCommand.update(getLastUsedCommand(), (existingResolvedArgs) -> {
            if (existingResolvedArgs != null) {
                return existingResolvedArgs.setData(argument.name(), parsedArgument);
            }
            return new Registry<>(argument.name(), parsedArgument, LinkedHashMap::new);
        });
        allResolvedArgs.setData(argument.name(), parsedArgument);
    }

    @Override
    public void resolveFlag(ParsedFlagArgument<S> flag) throws CommandException {
        flag.getOriginalArgument().validate(this, flag);
        flagRegistry.setData(flag.getOriginalArgument().name(), flag);
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

    /**
     * @return The used usage to use it to resolve commands
     */
    @Override
    public CommandPathway<S> getDetectedUsage() {
        return usage;
    }

    @Override
    public boolean hasResolvedFlag(FlagData<S> flagData) {
        return flagRegistry.getData(flagData.name())
                       .isPresent();
    }

    @Override
    public void debug() {
        if (allResolvedArgs.size() == 0) {
            ImperatDebugger.debug("No arguments were resolved!");
            return;
        }

        for (var arg : allResolvedArgs.getAll()) {
            ImperatDebugger.debug("Argument '%s' at index #%s with input='%s' with value='%s'",
                    arg.getOriginalArgument().format(), arg.getInputPosition(), arg.getArgumentRawInput(), arg.getArgumentParsedValue());
        }
    }

}
