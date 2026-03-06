package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.annotations.base.AnnotationParser;
import studio.mevera.imperat.annotations.base.AnnotationReader;
import studio.mevera.imperat.annotations.base.AnnotationReplacer;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.suggestions.AutoCompleter;
import studio.mevera.imperat.command.tree.TreeExecutionResult;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.events.Event;
import studio.mevera.imperat.events.EventBus;
import studio.mevera.imperat.events.EventExceptionHandler;
import studio.mevera.imperat.events.EventListenerConsumer;
import studio.mevera.imperat.events.EventSubscription;
import studio.mevera.imperat.events.ExecutionStrategy;
import studio.mevera.imperat.events.exception.EventException;
import studio.mevera.imperat.events.types.CommandPostProcessEvent;
import studio.mevera.imperat.events.types.CommandPostRegistrationEvent;
import studio.mevera.imperat.events.types.CommandPreProcessEvent;
import studio.mevera.imperat.events.types.CommandPreRegistrationEvent;
import studio.mevera.imperat.exception.AmbiguousCommandException;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.InvalidSyntaxException;
import studio.mevera.imperat.exception.PermissionDeniedException;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.exception.UnknownCommandException;
import studio.mevera.imperat.responses.ResponseKey;
import studio.mevera.imperat.util.ImperatDebugger;
import studio.mevera.imperat.util.Preconditions;
import studio.mevera.imperat.util.TypeWrap;
import studio.mevera.imperat.util.priority.Priority;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

public abstract class BaseImperat<S extends Source> implements Imperat<S> {

    protected final ImperatConfig<S> config;
    private final Map<String, Command<S>> commands = new HashMap<>();
    private @NotNull AnnotationParser<S> annotationParser;

    protected BaseImperat(@NotNull ImperatConfig<S> config) {
        this.config = config;
        annotationParser = AnnotationParser.defaultParser(this);
        config.applyAnnotationReplacers(this);
        if(config.getEventBus().isDummyBus()) {
            config.setEventBus(
                    EventBus.builder()
                            .exceptionHandler(
                                    new EventExceptionHandler() {
                                           @Override
                                           public <E extends Event> void handle(
                                                   E event,
                                                   Throwable exception,
                                                   EventSubscription<E> subscription
                                           ) {
                                               var ctxFactory = config.getContextFactory();
                                               CommandContext<S> dummy = ctxFactory.createDummyContext(BaseImperat.this);
                                               String methodName = "handle(event, exception, subscription)";
                                               config.handleExecutionThrowable(
                                                       new EventException(event, subscription, exception),
                                                       dummy ,
                                                       EventBus.class,
                                                       methodName
                                               );
                                           }
                                       }
                            )
                            .executorService(ForkJoinPool.commonPool())
                            .build()
            );
        }

        this.registerEvents();
    }

    @SuppressWarnings("unchecked")
    private void registerEvents() {

        this.listen(CommandPreProcessEvent.class, (event) -> {
            // Per-command pre-processing, executed after global pre-processing.
            Command<S> command = event.getCommand();
            CommandContext<S> context = event.getContext();
            try {
                command.preProcess(context);
            } catch (CommandException e) {
                event.setCancelled(true);
                throw new RuntimeException(e);
            }
        }, Priority.NORMAL, ExecutionStrategy.SYNC);

        this.listen(CommandPostProcessEvent.class, (event) -> {
            // Per-command post-processing, executed before global post-processing.
            Command<S> command = event.getCommand();
            ExecutionContext<S> context = event.getContext();
            try {
                command.postProcess(context);
            } catch (CommandException e) {
                throw new RuntimeException(e);
            }
        }, Priority.NORMAL, ExecutionStrategy.SYNC);
        this.listen(CommandPostProcessEvent.class, (event) -> {
            var context = event.getContext();


            var source = context.source();
            var pathway = context.getDetectedPathway();
            var handler = pathway.getCooldownHandler();
            var cooldown = pathway.getCooldown();

            if (handler.hasCooldown(source)) {
                assert cooldown != null;
                if (cooldown.permission() == null
                            || cooldown.permission().isEmpty()
                            || !context.imperatConfig().getPermissionChecker().hasPermission(source, cooldown.permission())) {

                    var cooldownDuration = cooldown.toDuration();
                    Instant lastTimeExecuted = (Instant) handler.getLastTimeExecuted(source).orElseThrow();
                    var elapsed = Duration.between(lastTimeExecuted, Instant.now());
                    var remaining = cooldownDuration.minus(elapsed);
                    var remainingDuration = remaining.isNegative() ? Duration.ZERO : remaining;

                    event.setCancelled(true);
                    throw ResponseException.of(ResponseKey.COOLDOWN)
                                  .withPlaceholder("seconds", String.valueOf(remainingDuration.toSeconds()))
                                  .withPlaceholder("remaining_duration", remainingDuration.toString())
                                  .withPlaceholder("cooldown_duration", cooldownDuration.toString())
                                  .withPlaceholder("last_executed", lastTimeExecuted.toString());
                }
            }
            handler.registerExecutionMoment(source);
        }, Priority.NORMAL, ExecutionStrategy.SYNC);
        this.listen(CommandPostProcessEvent.class, (event) -> {
            // Check usage-level permission
            var ctx = event.getContext();
            var source = ctx.source();
            var command = event.getCommand();
            var pathway = ctx.getDetectedPathway();
            var cfg = ctx.imperatConfig();
            if (!cfg.getPermissionChecker().hasPermission(source, pathway)) {
                ImperatDebugger.debug("Failed usage permission check!");
                throw new PermissionDeniedException(
                        command.getName(),
                        CommandPathway.format(command, pathway)
                );
            }
        }, Priority.HIGH, ExecutionStrategy.SYNC);
    }

    @Override
    public <E extends Event> void listen(
            @NotNull Class<E> eventType,
            @NotNull EventListenerConsumer<E> handler,
            @NotNull Priority priority,
            @NotNull ExecutionStrategy strategy
    ) {
        config.getEventBus().register(eventType, handler, priority, strategy);
    }

    @Override
    public <E extends Event> void publishEvent(E event) {
        config.getEventBus().post(event);
    }

    @Override
    public boolean removeListener(@NotNull UUID subscriptionId) {
        return config.getEventBus().unregister(subscriptionId);
    }

    /**
     * The config for imperat
     *
     * @return the config holding all variables.
     */
    @Override
    public @NotNull ImperatConfig<S> config() {
        return config;
    }

    /**
     * Checks whether the valueType can be a command sender
     *
     * @param type the valueType
     * @return whether the valueType can be a command sender
     */
    @Override
    public boolean canBeSender(Type type) {
        return TypeWrap.of(Source.class).isSupertypeOf(type);
    }

    /**
     * Registering a command into the global registry,
     * it will check for ambiguity with other commands and their tree before registering,
     * if an ambiguity is detected it will throw an {@link AmbiguousCommandException}
     *
     * @param command the command to register
     */
    @Override
    public void registerSimpleCommand(Command<S> command) {
        checkAmbiguity(command);
        CommandPreRegistrationEvent<S> preRegistrationEvent = new CommandPreRegistrationEvent<>(command);
        publishEvent(preRegistrationEvent);

        if(!preRegistrationEvent.isCancelled()) {
            Throwable error = null;
            try {
                this.registerCmd(command);
            }catch (Throwable ex) {
                error = ex;
            }finally {
                CommandPostRegistrationEvent<S> postRegistrationEvent = new CommandPostRegistrationEvent<>(command, error);
                publishEvent(postRegistrationEvent);
            }
        }
        else {
            //debug cancelled command registration
            ImperatDebugger.debug("Registration of command '%s' was cancelled by an CommandPreRegistrationEvent.", command.getName());
        }

    }

    private void checkAmbiguity(Command<S> command) {
        //check if cmd exists
        var other = getCommand(command.getName());
        if(other != null) {
            if (other == command) {
                return;
            }
            throw new AmbiguousCommandException(command, other);
        }

        //now check its tree for internal ambiguity
        AmbiguityChecker.checkAmbiguity(command);
    }


    private void registerCmd(@NotNull Command<S> command) {

        this.commands.put(command.getName().trim().toLowerCase(), command);
        for (var aliases : command.aliases()) {
            this.commands.put(aliases.trim().toLowerCase(), command);
        }

        for(var shortcut : command.getAllShortcuts()) {
            this.commands.put(shortcut.getName(), shortcut);
        }
    }

    /**
     * Registers a command class built by the
     * annotations using a parser
     *
     * @param commandClass the annotated command instance to parse
     */
    @Override
    public void registerCommand(Class<?> commandClass) {
        Preconditions.notNull(commandClass, "commandClass");
        Object classInstance = config.getInstanceFactory().createInstance(config, commandClass);
        annotationParser.parseCommandClass(
                Objects.requireNonNull(classInstance)
        );
    }

    @Override
    public void registerCommand(Object commandInstance) {
        if (commandInstance instanceof Command<?> command) {
            registerSimpleCommand((Command<S>) command);
        } else {
            // For non-RootCommand, non-Class instances, parse as annotated instance
            annotationParser.parseCommandClass(
                    Objects.requireNonNull(commandInstance)
            );
        }
    }

    /**
     * Unregisters a command from the internal registry
     *
     * @param name the name of the command to unregister
     */
    @Override
    public void unregisterCommand(String name) {
        Preconditions.notNull(name, "commandToRemove");
        Command<S> removed = commands.remove(name.trim().toLowerCase());
        if (removed != null) {
            for (var aliases : removed.aliases()) {
                commands.remove(aliases.trim().toLowerCase());
            }
        }
    }

    /**
     * Unregisters all commands from the internal registry
     */
    @Override
    public void unregisterAllCommands() {
        commands.clear();
    }

    /**
     * @param name the name/alias of the command
     * @return fetches {@link Command} with specific name/alias
     */
    @Override
    public @Nullable Command<S> getCommand(final String name) {
        final String cmdName = name.toLowerCase();
        final Command<S> result = commands.get(cmdName);

        if (result != null) {
            return result;
        }
        for (Command<S> headCommands : commands.values()) {
            if (headCommands.hasName(cmdName)) {
                return headCommands;
            }
        }
        return null;
    }

    /**
     * Registers a valueType of annotations so that it can be
     * detected by {@link AnnotationReader} , it's useful as it allows that valueType of annotation
     * to be recognized as a true Imperat-related annotation to be used in something like checking if a
     * {@link Argument} is annotated and checks for the annotations it has.
     *
     * @param type the valueType of annotation
     */
    @SafeVarargs
    @Override
    public final void registerAnnotations(Class<? extends Annotation>... type) {
        annotationParser.registerAnnotations(type);
    }

    /**
     * Registers {@link AnnotationReplacer}
     *
     * @param type     the valueType to replace the annotation by
     * @param replacer the replacer
     */
    @Override
    public <A extends Annotation> void registerAnnotationReplacer(Class<A> type, AnnotationReplacer<A> replacer) {
        annotationParser.registerAnnotationReplacer(type, replacer);
    }

    /**
     * @param owningCommand the command owning this sub-command
     * @param name          the name of the subcommand you're looking for
     * @return the subcommand of a command
     */
    @Override
    public @Nullable Command<S> getSubCommand(String owningCommand, String name) {
        Command<S> owningCmd = getCommand(owningCommand);
        if (owningCmd == null) {
            return null;
        }

        for (Command<S> subCommand : owningCmd.getSubCommands()) {
            Command<S> result = search(subCommand, name);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    private Command<S> search(Command<S> sub, String name) {
        if (sub.hasName(name)) {
            return sub;
        }

        for (Command<S> other : sub.getSubCommands()) {

            if (other.hasName(name)) {
                return other;
            } else {
                return search(other, name);
            }
        }

        return null;
    }

    private ExecutionResult<S> handleExecution(CommandContext<S> context) throws CommandException {
        Command<S> command = context.command();
        S source = context.source();

        if (!config.getPermissionChecker().hasPermission(source, command)) {
            throw new PermissionDeniedException(
                    command.getName(),
                    CommandPathway.format(command, command.getDefaultPathway())
            );
        }
        var preProcessEvent = new CommandPreProcessEvent<>(command, context);
        this.publishEvent(preProcessEvent);
        if (preProcessEvent.isCancelled()) {
            ImperatDebugger.debug("Execution of command '%s' was cancelled by a CommandPreProcessEvent.", command.getName());
            return ExecutionResult.failure(context);
        }

        // Direct execution: traverse tree, resolve args, and execute in one step
        TreeExecutionResult<S> treeResult = command.execute(context);
        ImperatDebugger.debug("Tree execution status: '%s'", treeResult.getStatus().name());

        if (treeResult.getStatus() == TreeExecutionResult.Status.PERMISSION_DENIED) {
            var closestUsage = treeResult.getClosestUsage();
            throw new PermissionDeniedException(
                    command.getName(),
                    closestUsage != null
                            ? CommandPathway.format(command, closestUsage)
                            : CommandPathway.format(command, command.getDefaultPathway())
            );
        }

        if (treeResult.getStatus() == TreeExecutionResult.Status.NO_MATCH) {
            ImperatDebugger.debug("No matching pathway found!");
            var closestUsage = treeResult.getClosestUsage();
            String invalidUsage = context.getRootCommandLabelUsed() + " " + context.arguments().join(" ");
            throw new InvalidSyntaxException(
                    invalidUsage,
                    closestUsage != null
                            ? CommandPathway.format(command, closestUsage)
                            : "No usage found"
            );
        }

        // SUCCESS: The tree already resolved args and created ExecutionContext
        CommandPathway<S> pathway = treeResult.getMatchedPathway();
        ExecutionContext<S> executionContext = treeResult.getExecutionContext();
        assert pathway != null && executionContext != null;

        ImperatDebugger.debug("Usage Found Format: '%s'", CommandPathway.formatWithTypes(command, pathway));

        // Post-processing
        var postProcessEvent = new CommandPostProcessEvent<>(command, executionContext);
        this.publishEvent(postProcessEvent);


        // Execute
        if (!postProcessEvent.isCancelled()) {
            ImperatDebugger.debug("Executing command '%s' for source '%s'", command.getName(), source);
            pathway.execute(this, source, executionContext);
            return ExecutionResult.of(executionContext, context);
        } else {
            ImperatDebugger.debug("Execution of command '%s' was cancelled by a CommandPostProcessEvent.", command.getName());
            return ExecutionResult.failure(executionContext);
        }
    }

    @Override
    public @NotNull ExecutionResult<S> execute(@NotNull CommandContext<S> context) {

        try {
            context.command().visualizeTree();
            return handleExecution(context);
        } catch (Exception ex) {
            //handle here
            this.config().handleExecutionThrowable(ex, context, BaseImperat.class, "execute(CommandContext<S> context)");
            return ExecutionResult.failure(ex, context);
        }
    }

    @Override
    public @NotNull ExecutionResult<S> execute(@NotNull S source, @NotNull Command<S> command, @NotNull String commandName, String[] rawInput) {
        ArgumentInput rawArguments = ArgumentInput.parse(rawInput);
        CommandContext<S> plainContext = config.getContextFactory()
                                          .createContext(this, source, command, commandName, rawArguments);

        return execute(plainContext);
    }

    @Override
    public @NotNull ExecutionResult<S> execute(@NotNull S source, @NotNull String commandName, String[] rawInput) {
        Command<S> command = getCommand(commandName);
        if (command == null) {
            throw new UnknownCommandException(commandName);
        }
        return execute(source, command, commandName, rawInput);
    }

    @Override
    public @NotNull ExecutionResult<S> execute(@NotNull S sender, @NotNull String commandName, @NotNull String rawArgsOneLine) {
        return execute(sender, commandName, rawArgsOneLine.split(" "));
    }

    @Override
    public @NotNull ExecutionResult<S> execute(@NotNull S sender, @NotNull String line) {
        if (line.isBlank()) {
            throw new UnknownCommandException(line);
        }
        String[] lineArgs = line.split(" ");
        String[] argumentsOnly = new String[lineArgs.length - 1];
        System.arraycopy(lineArgs, 1, argumentsOnly, 0, lineArgs.length - 1);
        return execute(sender, lineArgs[0], argumentsOnly);
    }

    /**
     * @param source          the sender writing the command
     * @param fullCommandLine the full command line
     * @return the suggestions at the current position
     */
    @Override
    public CompletableFuture<List<String>> autoComplete(@NotNull S source, @NotNull String fullCommandLine) {
        int firstSpace = fullCommandLine.indexOf(' ');
        if (firstSpace == -1) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        String cmdName = fullCommandLine.substring(0, firstSpace);

        Command<S> command = getCommand(cmdName);
        if (command == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        ArgumentInput argumentInput = ArgumentInput.parseAutoCompletion(
                fullCommandLine.substring(firstSpace),
                false
        );

        SuggestionContext<S> context = this.config.getContextFactory()
                                               .createSuggestionContext(
                                                       this, source, command, cmdName, argumentInput
                                               );

        command.visualizeTree();
        return command.autoCompleter()
                       .autoComplete(context)
                       .exceptionally((ex) -> {
                           this.config.handleExecutionThrowable(ex, context, AutoCompleter.class, "autoComplete(dispatcher, sender, args)");
                           return Collections.emptyList();
                       });
    }

    /**
     * Gets all registered commands
     *
     * @return the registered commands
     */
    @Override
    public Collection<? extends Command<S>> getRegisteredCommands() {
        return commands.values();
    }

    @Override
    public @NotNull AnnotationParser<S> getAnnotationParser() {
        return annotationParser;
    }

    /**
     * Changes the instance of {@link AnnotationParser}
     *
     * @param parser the parser
     */
    @Override
    public void setAnnotationParser(AnnotationParser<S> parser) {
        Preconditions.notNull(parser, "Parser");
        this.annotationParser = parser;
    }

    @Override
    public void debug() {
        for (var cmd : commands.values()) {
            cmd.visualizeTree();
        }
    }

}
