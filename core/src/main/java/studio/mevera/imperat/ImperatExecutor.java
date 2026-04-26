package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.tree.ParsedNode;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.events.types.CommandPostProcessEvent;
import studio.mevera.imperat.events.types.CommandPreProcessEvent;
import studio.mevera.imperat.exception.InvalidSyntaxException;
import studio.mevera.imperat.exception.PermissionDeniedException;
import studio.mevera.imperat.exception.UnknownCommandException;
import studio.mevera.imperat.permissions.PermissionHolder;
import studio.mevera.imperat.util.ImperatDebugger;
import studio.mevera.imperat.util.Pair;
import studio.mevera.imperat.util.UsageFormatting;

import java.util.Objects;
import java.util.Optional;

final class ImperatExecutor<S extends CommandSource> {

    private final Imperat<S> imperat;
    private final ImperatConfig<S> config;

    ImperatExecutor(Imperat<S> imperat, ImperatConfig<S> config) {
        this.imperat = imperat;
        this.config = config;
    }

    private static PermissionHolder deniedPermissionHolder(PermissionHolder denied, PermissionHolder fallback) {
        return denied == null ? fallback : denied;
    }

    @NotNull ExecutionResult<S> execute(@NotNull CommandContext<S> context) {
        try {
            context.command().visualizeTree();
            return handleExecution(context);
        } catch (Throwable ex) {
            config.handleExecutionError(ex, context, BaseImperat.class, "execute(CommandContext<S> context)");
            return ExecutionResult.failure(ex, context);
        }
    }

    @NotNull ExecutionResult<S> execute(
            @NotNull S source,
            @NotNull Command<S> command,
            @NotNull String commandName,
            String[] rawInput
    ) {
        ArgumentInput rawArguments = ArgumentInput.parse(rawInput);
        CommandContext<S> plainContext = config.getContextFactory()
                                                 .createContext(imperat, source, command, commandName, rawArguments);

        return execute(plainContext);
    }

    @NotNull ExecutionResult<S> execute(@NotNull S source, @NotNull String commandName, String[] rawInput) {
        Command<S> command = imperat.getCommand(commandName);
        if (command == null) {
            throw new UnknownCommandException(commandName);
        }
        return execute(source, command, commandName, rawInput);
    }

    @NotNull ExecutionResult<S> execute(@NotNull S sender, @NotNull String commandName, @NotNull String rawArgsOneLine) {
        return execute(sender, commandName, rawArgsOneLine.split(" "));
    }

    @NotNull ExecutionResult<S> execute(@NotNull S sender, @NotNull String line) {
        if (line.isBlank()) {
            throw new UnknownCommandException(line);
        }
        String[] lineArgs = line.split(" ");
        String[] argumentsOnly = new String[lineArgs.length - 1];
        System.arraycopy(lineArgs, 1, argumentsOnly, 0, lineArgs.length - 1);
        return execute(sender, lineArgs[0], argumentsOnly);
    }

    private ExecutionResult<S> handleExecution(CommandContext<S> context) throws Throwable {
        Command<S> command = context.command();
        S source = context.source();

        Pair<PermissionHolder, Boolean> commandPermissionResult = config.getPermissionChecker().checkPermission(source, command);
        if (!commandPermissionResult.right()) {
            throw new PermissionDeniedException(
                    command.getName(),
                    command.getDefaultPathway(),
                    deniedPermissionHolder(command, commandPermissionResult.left())
            );
        }
        var preProcessEvent = new CommandPreProcessEvent<>(command, context);
        imperat.publishEvent(preProcessEvent);
        if (preProcessEvent.isCancelled()) {
            ImperatDebugger.debug("Execution of command '%s' was cancelled by a CommandPreProcessEvent.", command.getName());
            return ExecutionResult.failure(context);
        }

        ExecutionContext<S> executionContext = config.getContextFactory().createExecutionContext(
                context,
                null,
                command
        );
        var treeMatch = command.execute(executionContext);
        var parsedNodes = treeMatch.parsedNodes();
        Command<S> terminalCommand = treeMatch.command();
        CommandPathway<S> detectedPathway = treeMatch.pathway();
        if (detectedPathway == null) {
            String invalidUsage = UsageFormatting.formatInput(
                    config.commandPrefix(),
                    context.getRootCommandLabelUsed(),
                    context.arguments().join(" ")
            );
            throw new InvalidSyntaxException(invalidUsage, command.tree().getClosestPathwayToContext(context, treeMatch));
        }

        executionContext.setDetectedPathway(detectedPathway);
        executionContext.setLastUsedCommand(terminalCommand);
        executionContext.setTreeMatch(treeMatch);

        Optional<Pair<ParsedNode<S>, Argument<S>>> inAccessibleNode =
                parsedNodes.stream().map((n) -> {
                            var argOpt = n.findInAccessibleArgument(config, source);
                            return argOpt.map(argument -> new Pair<>(n, argument)).orElse(null);
                        })
                        .filter(Objects::nonNull)
                        .findFirst();

        if (inAccessibleNode.isPresent() || !config.getPermissionChecker().hasPermission(source, detectedPathway)) {
            var inAccessible = inAccessibleNode.orElse(null);
            PermissionHolder holder = inAccessible == null ? detectedPathway : inAccessible.right();
            throw new PermissionDeniedException(
                    command.getName(),
                    detectedPathway,
                    holder
            );
        }

        executionContext.parse(parsedNodes);

        var postProcessEvent = new CommandPostProcessEvent<>(command, executionContext);
        imperat.publishEvent(postProcessEvent);

        if (!postProcessEvent.isCancelled()) {
            ImperatDebugger.debug("Executing command '%s' for source '%s'", command.getName(), source);
            detectedPathway.execute(imperat, source, executionContext);
            return ExecutionResult.of(executionContext, context);
        }
        ImperatDebugger.debug("Execution of command '%s' was cancelled by a CommandPostProcessEvent.", command.getName());
        return ExecutionResult.failure(executionContext);
    }
}
