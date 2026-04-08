package studio.mevera.imperat.context.internal;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.FlagArgument;
import studio.mevera.imperat.command.tree.ParseResult;
import studio.mevera.imperat.command.tree.TreeExecutionResult;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.ParsedArgument;
import studio.mevera.imperat.exception.CombinedFlagsException;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.InvalidSyntaxException;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.providers.ContextArgumentProvider;
import studio.mevera.imperat.responses.ResponseKey;
import studio.mevera.imperat.util.ImperatDebugger;
import studio.mevera.imperat.util.Patterns;
import studio.mevera.imperat.util.Registry;
import studio.mevera.imperat.util.TypeUtility;
import studio.mevera.imperat.util.UsageFormatting;

import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@ApiStatus.Internal
final class ExecutionContextImpl<S extends CommandSource> extends ContextImpl<S> implements ExecutionContext<S> {

    private CommandPathway<S> pathway;
    private final Registry<String, ParsedFlagArgument<S>> flagRegistry = new Registry<>();
    //per command/subcommand because the class 'Command' can be also treated as a sub command
    private final Registry<Command<S>, Registry<String, ParsedArgument<S>>> resolvedArgumentsPerCommand = new Registry<>(LinkedHashMap::new);
    //all resolved arguments EXCEPT for subcommands and flags.
    private final Registry<String, ParsedArgument<S>> allResolvedArgs = new Registry<>(LinkedHashMap::new);

    //last command used
    private final Command<S> lastCommand;

    private TreeExecutionResult<S> treeExecutionResult;

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

    /**
     * Fetches the argument/input resolved by the context
     * using {@link ContextArgumentProvider}
     *
     * @param type valueType of argument to return
     * @return the argument/input resolved by the context
     */
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
    public @NotNull TreeExecutionResult<S> getTreeExecutionResult() {
        if (treeExecutionResult == null) {
            throw new IllegalStateException("The ExecutionContext hasn't been resolved yet, please call ExecutionContext#resolve");
        }
        return treeExecutionResult;
    }

    @Override
    public void handleRemainingParsing(TreeExecutionResult<S> result) throws CommandException {
        if (pathway == null) {
            return;
        }

        Cursor<S> cursor = Cursor.of(this.arguments(), pathway);
        OptionalArgumentHandler<S> optionalArgumentHandler = new OptionalArgumentHandler<>();
        Deque<ParseResult<S>> preParsedArguments = new ArrayDeque<>(result.getParsedArguments());
        while (cursor.isCurrentParameterAvailable()) {
            Argument<S> currentParameter = cursor.currentParameterIfPresent();
            if (currentParameter == null) {
                break;
            }

            String currentRaw = cursor.currentRawIfPresent();
            if (currentParameter.isCommand()) {
                if (currentRaw == null) {
                    throw invalidSyntax(result);
                }
                ArgumentValueBinder.bindCurrentSubCommand(cursor);
                continue;
            }

            if (currentRaw == null) {
                if (!currentParameter.isOptional()) {
                    throw invalidSyntax(result);
                }
                parseMissingOptional(cursor, currentParameter);
                cursor.skipParameter();
                continue;
            }

            if (ArgumentValueBinder.skipCurrentFlag(this, cursor)) {
                continue;
            }

            ParseResult<S> preParsed = preParsedArguments.peekFirst();
            if (preParsed != null && preParsed.getArgument() == currentParameter) {
                ArgumentValueBinder.bindParsedParameter(this, cursor, preParsed);
                preParsedArguments.removeFirst();
                continue;
            }

            if (currentParameter.isOptional()) {
                optionalArgumentHandler.handle(result, this, cursor);
                continue;
            }

            ArgumentValueBinder.bindCurrentParameter(this, cursor);
        }

        var usage = this.getDetectedPathway();
        Command<S> lastCmd = this.command();

        for (int rPos = 0; rPos < cursor.rawsLength(); rPos++) {
            String raw = this.getRawArgument(rPos);
            if (!Patterns.isInputFlag(raw)) {
                var sub = lastCmd.getSubCommand(raw, false);
                if (sub != null) {
                    lastCmd = sub;
                }
                continue;
            }
            String nextRaw = rPos + 1 < cursor.rawsLength() ? this.getRawArgument(rPos + 1) : null;
            //identify if its a registered flag
            Set<FlagArgument<S>> extracted = usage.getFlagExtractor().extract(Patterns.withoutFlagSign(raw));
            String inputRaw = validateExtractedFlagsAndGetInputRaw(raw, nextRaw, extracted);

            //all flags here must be resolved inside the this
            for (var flagParam : extracted) {

                var lastCmdPathways = lastCmd.getDedicatedPathways();
                boolean foundOutsideScope = true;
                for (var pathway : lastCmdPathways) {
                    if (pathway.getFlagExtractor().getRegisteredFlags().contains(flagParam)) {
                        foundOutsideScope = false;
                        break;
                    }

                }
                if (foundOutsideScope) {
                    throw ResponseException.of(ResponseKey.FLAG_OUTSIDE_SCOPE)
                                  .withPlaceholder("flag_input", raw)
                                  .withPlaceholder("wrong_cmd", lastCmd.getName());
                }
                this.resolveFlag(
                        ParsedFlagArgument.forFlag(
                                flagParam,
                                raw,
                                inputRaw,
                                rPos,
                                flagParam.isSwitch() ? rPos : rPos + 1,
                                flagParam.isSwitch() ? true : Objects.requireNonNull(flagParam.flagData().inputType()).parse(this, flagParam,
                                        inputRaw)
                        )
                );
            }

        }

        for (FlagArgument<S> registered : usage.getFlagExtractor().getRegisteredFlags()) {
            if (this.hasResolvedFlag(registered.flagData())) {
                continue;
            }
            resolveFlagDefaultValue(registered);
        }

    }

    private InvalidSyntaxException invalidSyntax(TreeExecutionResult<S> result) {
        var closestUsage = result.getClosestUsage();
        String invalidUsage = UsageFormatting.formatInput(
                imperatConfig().commandPrefix(),
                getRootCommandLabelUsed(),
                arguments().join(" ")
        );
        return new InvalidSyntaxException(invalidUsage, closestUsage);
    }

    private void parseMissingOptional(Cursor<S> cursor, Argument<S> optionalParameter) throws CommandException {
        Object value = getDefaultValue(optionalParameter);
        this.parseArgument(
                new ParsedArgument<>(
                        null,
                        optionalParameter,
                        cursor.position().getParameter(),
                        value
                )
        );
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
        return argument.type().parse(this, argument, raw);
    }

    private String validateExtractedFlagsAndGetInputRaw(String currentRaw, @Nullable String nextRaw, Set<FlagArgument<S>> extracted)
            throws CommandException {
        long numberOfSwitches = extracted.stream().filter(FlagArgument::isSwitch).count();
        long numberOfTrueFlags = extracted.size() - numberOfSwitches;

        if (extracted.size() != numberOfSwitches && extracted.size() != numberOfTrueFlags) {
            throw new CombinedFlagsException("Unsupported use of a mixture of switches and true flags!");
        }

        if (extracted.size() == numberOfTrueFlags && !TypeUtility.areTrueFlagsOfSameInputType(extracted)) {
            throw new CombinedFlagsException("You cannot use compressed true-flags, while they are not of same input type");
        }

        boolean areAllSwitches = extracted.size() == numberOfSwitches;

        String inputRaw = areAllSwitches ? currentRaw : nextRaw;
        if (!areAllSwitches && inputRaw == null) {
            throw ResponseException.of(ResponseKey.MISSING_FLAG_INPUT)
                          .withPlaceholder("flags", extracted.stream().map(FlagArgument::getName).collect(Collectors.joining(",")));
        }
        return inputRaw;
    }

    private void resolveFlagDefaultValue(FlagArgument<S> flagArgument) throws
            CommandException {

        if (flagArgument.isSwitch()) {
            this.resolveFlag(
                    ParsedFlagArgument.forDefaultSwitch(
                            flagArgument
                    )
            );
            return;
        }

        String defValue = flagArgument.getDefaultValueSupplier().provide(this, flagArgument);
        if (defValue != null) {
            Object flagValueResolved = flagArgument.getDefaultValueSupplier().isEmpty() ? null :
                                               Objects.requireNonNull(flagArgument.flagData().inputType()).parse(
                                                       this, flagArgument, defValue);
            this.resolveFlag(ParsedFlagArgument.forDefaultFlag(flagArgument, defValue, flagValueResolved));
        }
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


    @Override
    public <T> void parseArgument(
            @NotNull Cursor<S> cursor,
            @Nullable T value
    ) throws CommandException {
        var argument = cursor.currentParameterIfPresent();
        if (argument == null) {
            throw new IllegalStateException(
                    "No argument found at index " + cursor.position().parameter + " for command " + getLastUsedCommand().getName());
        }

        String raw = cursor.currentRawIfPresent();
        if (argument.type().getNumberOfParametersToConsume(argument) > 1) {
            StringBuilder builder = new StringBuilder();
            for (int i = cursor.position().parameter; i <= cursor.position().raw; i++)
                builder.append(arguments().get(i)).append(" ");
            raw = builder.toString();
        }

        final ParsedArgument<S> parsedArgument = new ParsedArgument<>(raw, argument, cursor.position().parameter, value);
        parseArgument(parsedArgument);
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
    public void setDetectedPathway(CommandPathway<S> pathway) {
        this.pathway = pathway;
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

    @Override
    public void setTreeResult(TreeExecutionResult<S> treeResult) {
        this.treeExecutionResult = treeResult;
    }

}
