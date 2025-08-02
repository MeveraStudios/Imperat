package dev.velix.imperat.context.internal;

import dev.velix.imperat.command.Command;
import dev.velix.imperat.command.CommandUsage;
import dev.velix.imperat.command.parameters.CommandParameter;
import dev.velix.imperat.command.parameters.NumericParameter;
import dev.velix.imperat.command.parameters.NumericRange;
import dev.velix.imperat.command.tree.CommandPathSearch;
import dev.velix.imperat.context.Context;
import dev.velix.imperat.context.ExecutionContext;
import dev.velix.imperat.context.FlagData;
import dev.velix.imperat.context.Source;
import dev.velix.imperat.context.internal.sur.ParameterValueAssigner;
import dev.velix.imperat.exception.ImperatException;
import dev.velix.imperat.exception.NumberOutOfRangeException;
import dev.velix.imperat.resolvers.ContextResolver;
import dev.velix.imperat.util.ImperatDebugger;
import dev.velix.imperat.util.Registry;
import dev.velix.imperat.util.TypeUtility;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.*;


@ApiStatus.Internal
final class ExecutionContextImpl<S extends Source> extends ContextImpl<S> implements ExecutionContext<S> {

    private final CommandUsage<S> usage;
    private final Registry<String, ExtractedInputFlag> flagRegistry = new Registry<>();
    //per command/subcommand because the class 'CommandProcessingChain' can be also treated as a sub command
    private final Registry<Command<S>, Registry<String, Argument<S>>> resolvedArgumentsPerCommand = new Registry<>(LinkedHashMap::new);
    //all resolved arguments EXCEPT for subcommands and flags.
    private final Registry<String, Argument<S>> allResolvedArgs = new Registry<>(LinkedHashMap::new);
    
    //last command used
    private final Command<S> lastCommand;
    
    ExecutionContextImpl(Context<S> context, CommandUsage<S> usage) {
        super(context.imperat(), context.command(), context.source(), context.label(), context.arguments());
        this.lastCommand = context.command();
        this.usage = usage;
    }
    
    ExecutionContextImpl(
        Context<S> context,
        CommandPathSearch<S> result
    ) {
        super(context.imperat(), context.command(), context.source(), context.label(), context.arguments());
        var lastCmdNode = result.getLastCommandNode();
        this.lastCommand = lastCmdNode == null ? context.command() : lastCmdNode.getData();
        this.usage = result.getFoundUsage();
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
    public @Nullable Argument<S> getResolvedArgument(Command<S> command, String name) {
        return resolvedArgumentsPerCommand.getData(command)
            .flatMap((resolvedArgs) -> resolvedArgs.getData(name))
            .orElse(null);
    }

    /**
     * @param command the command/subcommand with certain args
     * @return the command/subcommand's resolved args in as a new array-list
     */
    @Override
    public List<Argument<S>> getResolvedArguments(Command<S> command) {
        return resolvedArgumentsPerCommand.getData(command)
            .map((argMap) -> (List<Argument<S>>) new ArrayList<Argument<S>>(argMap.getAll()))
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
     * @return an ordered collection of {@link Argument} just like how they were entered
     * NOTE: the flags are NOT included as a resolved argument, it's treated differently
     */
    @Override
    public Collection<? extends Argument<S>> getResolvedArguments() {
        return allResolvedArgs.getAll();
    }

    /**
     * Fetches a resolved argument's value
     *
     * @param name the name of the command
     * @return the value of the resolved argument
     * @see Argument
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T getArgument(String name) {
        return (T) allResolvedArgs.getData(name).map(Argument::value)
            .orElse(null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> @NotNull R getResolvedSource(Type type) throws ImperatException {
        if (!imperatConfig.hasSourceResolver(type)) {
            throw new IllegalArgumentException("Found no SourceResolver for valueType `" + type.getTypeName() + "`");
        }
        var sourceResolver = imperatConfig.getSourceResolver(type);
        assert sourceResolver != null;

        return (R) sourceResolver.resolve(this.source());
    }

    /**
     * Fetches the argument/input resolved by the context
     * using {@link ContextResolver}
     *
     * @param type valueType of argument to return
     * @return the argument/input resolved by the context
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T getContextResolvedArgument(Class<T> type) throws ImperatException {
        var resolver = imperatConfig.getContextResolver(type);
        return resolver == null ? null : (T) resolver.resolve(this, null);
    }

    /**
     * @return the resolved flag arguments
     */
    @Override
    public Collection<? extends ExtractedInputFlag> getResolvedFlags() {
        return flagRegistry.getAll();
    }


    @Override
    public Optional<ExtractedInputFlag> getFlag(String flagName) {
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
            .map(ExtractedInputFlag::value)
            .orElse(null);
    }
    
    
    @Override
    public void resolve() throws ImperatException {
        var resolver = ParameterValueAssigner.create(this, usage);
        resolver.resolve();
    }



    @Override
    public <T> void resolveArgument(
        Command<S> command,
        @Nullable String raw,
        int index,
        CommandParameter<S> parameter,
        @Nullable T value
    ) throws ImperatException {

        if (value != null && TypeUtility.isNumericType(value.getClass())
            && parameter instanceof NumericParameter<S> numericParameter
            && numericParameter.hasRange()
            && !numericParameter.matchesRange((Number) value)) {

            NumericRange range = numericParameter.getRange();
            throw new NumberOutOfRangeException(raw, numericParameter, (Number) value, range);
        }
        final Argument<S> argument = new Argument<>(raw, parameter, index, value);
        resolvedArgumentsPerCommand.update(command, (existingResolvedArgs) -> {
            if (existingResolvedArgs != null) {
                return existingResolvedArgs.setData(parameter.name(), argument);
            }
            return new Registry<>(parameter.name(), argument, LinkedHashMap::new);
        });
        allResolvedArgs.setData(parameter.name(), argument);
    }

    @Override
    public void resolveFlag(ExtractedInputFlag flag) {
        flagRegistry.setData(flag.flag().name(), flag);
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
    public CommandUsage<S> getDetectedUsage() {
        return usage;
    }

    @Override
    public boolean hasResolvedFlag(FlagData<S> flagData) {
        return flagRegistry.getData(flagData.name())
                .isPresent();
    }

    @Override
    public void debug() {
        if(allResolvedArgs.size() == 0) {
            ImperatDebugger.debug("No arguments were resolved!");
            return;
        }

        for (var arg : allResolvedArgs.getAll()) {
            ImperatDebugger.debug("Argument '%s' at index #%s with input='%s' with value='%s'",
                arg.parameter().format(), arg.index(), arg.raw(), arg.value());
        }
    }

}
