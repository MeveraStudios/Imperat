package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.annotations.base.AnnotationParser;
import studio.mevera.imperat.annotations.base.AnnotationReader;
import studio.mevera.imperat.annotations.base.AnnotationReplacer;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.events.Event;
import studio.mevera.imperat.events.EventListenerConsumer;
import studio.mevera.imperat.events.ExecutionStrategy;
import studio.mevera.imperat.events.types.CommandPostRegistrationEvent;
import studio.mevera.imperat.events.types.CommandPreRegistrationEvent;
import studio.mevera.imperat.exception.AmbiguousCommandException;
import studio.mevera.imperat.util.ImperatDebugger;
import studio.mevera.imperat.util.Preconditions;
import studio.mevera.imperat.util.TypeWrap;
import studio.mevera.imperat.util.priority.Priority;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class BaseImperat<S extends CommandSource> implements Imperat<S> {

    protected final ImperatConfig<S> config;
    private final CommandRegistry<S> commandRegistry = new CommandRegistry<>();
    private final ImperatExecutor<S> executor;
    private final ImperatAutoCompleter<S> autoCompleter;
    private @NotNull AnnotationParser<S> annotationParser;

    protected BaseImperat(@NotNull ImperatConfig<S> config) {
        this.config = config;
        this.executor = new ImperatExecutor<>(this, config);
        this.autoCompleter = new ImperatAutoCompleter<>(this, config);
        this.annotationParser = AnnotationParser.defaultParser(this);

        config.applyAnnotationReplacers(this);
        if (config.getEventBus().isDummyBus()) {
            config.setEventBus(DefaultEventBusFactory.create(this, config));
        }
        new ImperatEventBootstrap<>(this, config).registerDefaultListeners();
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

    @Override
    public @NotNull ImperatConfig<S> config() {
        return config;
    }

    @Override
    public boolean canBeSender(Type type) {
        return TypeWrap.of(CommandSource.class).isSupertypeOf(type);
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
        AmbiguityChecker.checkAmbiguity(command);
        CommandPreRegistrationEvent<S> preRegistrationEvent = new CommandPreRegistrationEvent<>(command);
        publishEvent(preRegistrationEvent);

        if (!preRegistrationEvent.isCancelled()) {
            Throwable error = null;
            try {
                commandRegistry.register(command);
            } catch (Throwable ex) {
                error = ex;
            } finally {
                CommandPostRegistrationEvent<S> postRegistrationEvent = new CommandPostRegistrationEvent<>(command, error);
                publishEvent(postRegistrationEvent);
            }
        } else {
            ImperatDebugger.debug("Registration of command '%s' was cancelled by an CommandPreRegistrationEvent.", command.getName());
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
        annotationParser.parseCommandClass(Objects.requireNonNull(classInstance));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerCommand(Object commandInstance) {
        if (commandInstance instanceof Command<?> command) {
            registerSimpleCommand((Command<S>) command);
        } else {
            annotationParser.parseCommandClass(Objects.requireNonNull(commandInstance));
        }
    }

    @Override
    public void unregisterCommand(String name) {
        commandRegistry.unregister(name);
    }

    @Override
    public void unregisterAllCommands() {
        commandRegistry.clear();
    }

    @Override
    public @Nullable Command<S> getCommand(final String name) {
        return commandRegistry.get(name);
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

    @Override
    public <A extends Annotation> void registerAnnotationReplacer(Class<A> type, AnnotationReplacer<A> replacer) {
        annotationParser.registerAnnotationReplacer(type, replacer);
    }

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
            }
            return search(other, name);
        }
        return null;
    }

    @Override
    public @NotNull ExecutionResult<S> execute(@NotNull CommandContext<S> context) {
        return executor.execute(context);
    }

    @Override
    public @NotNull ExecutionResult<S> execute(@NotNull S source, @NotNull Command<S> command, @NotNull String commandName, String[] rawInput) {
        return executor.execute(source, command, commandName, rawInput);
    }

    @Override
    public @NotNull ExecutionResult<S> execute(@NotNull S source, @NotNull String commandName, String[] rawInput) {
        return executor.execute(source, commandName, rawInput);
    }

    @Override
    public @NotNull ExecutionResult<S> execute(@NotNull S sender, @NotNull String commandName, @NotNull String rawArgsOneLine) {
        return executor.execute(sender, commandName, rawArgsOneLine);
    }

    @Override
    public @NotNull ExecutionResult<S> execute(@NotNull S sender, @NotNull String line) {
        return executor.execute(sender, line);
    }

    @Override
    public CompletableFuture<List<String>> autoComplete(@NotNull S source, @NotNull String fullCommandLine) {
        return autoCompleter.autoComplete(source, fullCommandLine);
    }

    @Override
    public Collection<? extends Command<S>> getRegisteredCommands() {
        return commandRegistry.values();
    }

    @Override
    public @NotNull AnnotationParser<S> getAnnotationParser() {
        return annotationParser;
    }

    @Override
    public void setAnnotationParser(AnnotationParser<S> parser) {
        Preconditions.notNull(parser, "Parser");
        this.annotationParser = parser;
    }

    @Override
    public void debug() {
        for (var cmd : commandRegistry.values()) {
            cmd.visualizeTree();
        }
    }
}
