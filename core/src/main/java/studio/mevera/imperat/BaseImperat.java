package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.annotations.base.AnnotationParser;
import studio.mevera.imperat.annotations.base.AnnotationReader;
import studio.mevera.imperat.annotations.base.AnnotationReplacer;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.processors.CommandPostProcessor;
import studio.mevera.imperat.command.processors.CommandPreProcessor;
import studio.mevera.imperat.command.suggestions.AutoCompleter;
import studio.mevera.imperat.command.tree.CommandPathSearch;
import studio.mevera.imperat.context.*;
import studio.mevera.imperat.exception.*;
import studio.mevera.imperat.util.ImperatDebugger;
import studio.mevera.imperat.util.Preconditions;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public abstract class BaseImperat<S extends Source> implements Imperat<S> {

    protected final ImperatConfig<S> config;
    private @NotNull AnnotationParser<S> annotationParser;
    private final Map<String, Command<S>> commands = new HashMap<>();

    protected BaseImperat(@NotNull ImperatConfig<S> config) {
        this.config = config;
        annotationParser = AnnotationParser.defaultParser(this);
        config.applyAnnotationReplacers(this);
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
     * Registering a command into the dispatcher
     *
     * @param command the command to register
     */
    @Override
    public void registerCommand(Command<S> command) {
        try {
            var verifier = config.getUsageVerifier();
            for (CommandUsage<S> usage : command.usages()) {
                if (!verifier.verify(usage)) throw new UsageRegistrationException(command, usage);

                for (CommandUsage<S> other : command.usages()) {
                    if (other.equals(usage)) continue;
                    if (verifier.areAmbiguous(usage, other))
                        throw new AmbiguousUsageAdditionException(command, usage, other);
                }
            }
            this.registerCmd(command);
        } catch (RuntimeException ex) {
            ImperatDebugger.error(BaseImperat.class, "registerCommand(CommandProcessingChain command)", ex);
            shutdownPlatform();
        }
    }
    private void registerCmd(@NotNull Command<S> command) {
        
        command.tree().computePermissions();
        
        this.commands.put(command.name().trim().toLowerCase(), command);
        for(var aliases : command.aliases()) {
            this.commands.put(aliases.trim().toLowerCase(), command);
        }
    }

    /**
     * Registers a command class built by the
     * annotations using a parser
     *
     * @param command the annotated command instance to parse
     */
    @Override
    @SuppressWarnings("unchecked")
    public void registerCommand(Object command) {
        if (command instanceof Command<?> commandObj) {
            this.registerCommand((Command<S>) commandObj);
            return;
        }
        annotationParser.parseCommandClass(command);
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
        if(removed != null) {
            for(var aliases : removed.aliases()) {
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

        if (result != null) return result;
        for (Command<S> headCommands : commands.values()) {
            if (headCommands.hasName(cmdName)) return headCommands;
        }
        return null;
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

    /**
     * Registers a valueType of annotations so that it can be
     * detected by {@link AnnotationReader} , it's useful as it allows that valueType of annotation
     * to be recognized as a true Imperat-related annotation to be used in something like checking if a
     * {@link CommandParameter} is annotated and checks for the annotations it has.
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
        if (owningCmd == null) return null;

        for (Command<S> subCommand : owningCmd.getSubCommands()) {
            Command<S> result = search(subCommand, name);
            if (result != null) return result;
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
    private ExecutionResult<S> handleExecution(Context<S> context) throws ImperatException {
        Command<S> command = context.command();
        S source = context.source();
        
        if(!config.getPermissionChecker().hasPermission(source, command.getSinglePermission())) {
            throw new PermissionDeniedException(
                    command.getDefaultUsage(),
                    Objects.requireNonNull(command.getSinglePermission()),
                    command,
                    context
            );
        }
        
        CommandPathSearch<S> searchResult = command.contextMatch(context);
        ImperatDebugger.debug("Search-result: '" + searchResult.getResult().name() + "'");
        
        if(searchResult.getResult() == CommandPathSearch.Result.PAUSE) {
            throw new PermissionDeniedException(searchResult, context);
        }
        
        CommandUsage<S> usage = searchResult.getFoundUsage();
        
        if(usage == null || searchResult.getLastNode() == null ||
                searchResult.getResult() != CommandPathSearch.Result.COMPLETE) {
            ImperatDebugger.debug("Usage not found !");
            throw new InvalidSyntaxException(searchResult, context);
        }
        
        var usageAccessCheckResult = config.getPermissionChecker().hasUsagePermission(source, usage);
        if(!usageAccessCheckResult.right()) {
            ImperatDebugger.debug("Failed usage permission check !");
            throw new PermissionDeniedException(usage, usageAccessCheckResult.left(), null, context);
        }
        
        return executeUsage(command, source, context, usage, searchResult);
    }
    
    protected ExecutionResult<S> executeUsage(
            final Command<S> command,
            final S source,
            final Context<S> context,
            final CommandUsage<S> usage,
            final CommandPathSearch<S> dispatch
    ) throws ImperatException {
        
        //global preprocessing
        globalPreProcessing(context, usage);
        
        //per-command preprocessor
        command.preProcess(this, context, usage);
        
        ExecutionContext<S> resolvedContext = config.getContextFactory().createExecutionContext(context, dispatch);
        
        resolvedContext.resolve();
        usage.execute(this, source, resolvedContext);
        
        globalPostProcessing(resolvedContext);
        command.postProcess(this, resolvedContext, usage);
        
        return ExecutionResult.of(resolvedContext, dispatch, context);
    }
    
    private void globalPreProcessing(
            @NotNull Context<S> context,
            @NotNull CommandUsage<S> usage
    ) throws ProcessorException {
        
        for (CommandPreProcessor<S> preProcessor : config.getPreProcessors()) {
            try {
                preProcessor.process(this, context, usage);
            } catch (Throwable ex) {
                throw new ProcessorException(ProcessorException.Type.PRE, null, ex, context);
            }
        }
        
    }
    
    private void globalPostProcessing(
            @NotNull ExecutionContext<S> context
    ) throws ImperatException {
        for (CommandPostProcessor<S> postProcessor : config.getPostProcessors()) {
            try {
                postProcessor.process(this, context);
            }catch (Throwable ex) {
                throw new ProcessorException(ProcessorException.Type.POST, null, ex, context);
            }
        }
    }
    
    @Override
    public @NotNull ExecutionResult<S> execute(@NotNull Context<S> context) {
        
        try {
            context.command().visualizeTree();
            return handleExecution(context);
        }catch (Exception ex) {
            //handle here
            this.config().handleExecutionThrowable(ex, context, BaseImperat.class, "execute(Context<S> context)");
            return ExecutionResult.failure(ex, context);
        }
    }

    @Override
    public @NotNull ExecutionResult<S> execute(@NotNull S source, @NotNull Command<S> command, @NotNull String commandName, String[] rawInput)  {
        ArgumentInput rawArguments = ArgumentInput.parse(rawInput);
        Context<S> plainContext = config.getContextFactory()
            .createContext(this, source, command, commandName, rawArguments);

        return execute(plainContext);
    }

    @Override
    public @NotNull ExecutionResult<S> execute(@NotNull S source, @NotNull String commandName, String[] rawInput)  {
        Command<S> command = getCommand(commandName);
        if (command == null) {
            throw new IllegalArgumentException("Unknown command input: '" + commandName + "'");
        }
        return execute(source, command, commandName, rawInput);
    }

    @Override
    public @NotNull ExecutionResult<S> execute(@NotNull S sender, @NotNull String commandName, @NotNull String rawArgsOneLine) {
        return execute(sender, commandName, rawArgsOneLine.split(" "));
    }

    @Override
    public @NotNull ExecutionResult<S> execute(@NotNull S sender, @NotNull String line) {
        if(line.isBlank()) {
            throw new IllegalArgumentException("Empty Command Line");
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
        if(firstSpace == -1) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        
        String cmdName = fullCommandLine.substring(0, firstSpace);
        
        Command<S> command = getCommand(cmdName);
        if(command == null ){
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        
        ArgumentInput argumentInput = ArgumentInput.parseAutoCompletion(
                fullCommandLine.substring(firstSpace),
                false
        );
        
        SuggestionContext<S> context =  this.config.getContextFactory()
                .createSuggestionContext(
                        this, source, command, cmdName, argumentInput
                );
        
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
    
    @Override
    public void debug(boolean treeVisualizing) {
        for (var cmd : commands.values()) {
            if (treeVisualizing) {
                cmd.visualizeTree();
            } else {
                ImperatDebugger.debug("Debugging command '%s'", cmd.name());
                for (CommandUsage<S> usage : cmd.usages()) {
                    ImperatDebugger.debug("   - '%s'", CommandUsage.format(cmd, usage));
                }
            }
        }
    }

}
