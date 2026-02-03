package studio.mevera.imperat.command;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.annotations.base.element.ParseElement;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.FlagParameter;
import studio.mevera.imperat.command.parameters.validator.ArgValidator;
import studio.mevera.imperat.command.parameters.validator.InvalidArgumentException;
import studio.mevera.imperat.command.processors.CommandPostProcessor;
import studio.mevera.imperat.command.processors.CommandPreProcessor;
import studio.mevera.imperat.command.processors.CommandProcessingChain;
import studio.mevera.imperat.command.suggestions.AutoCompleter;
import studio.mevera.imperat.command.tree.CommandPathSearch;
import studio.mevera.imperat.command.tree.CommandTree;
import studio.mevera.imperat.command.tree.CommandTreeVisualizer;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Argument;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.ProcessorException;
import studio.mevera.imperat.exception.ThrowableResolver;
import studio.mevera.imperat.resolvers.SuggestionResolver;
import studio.mevera.imperat.util.ImperatDebugger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

@ApiStatus.Internal
final class CommandImpl<S extends Source> implements Command<S> {

    final static int INITIAL_PERMISSIONS_CAPACITY = 3;
    private final String name;
    private final int position;
    private final List<String> aliases = new ArrayList<>();
    private final Map<String, Command<S>> children = new LinkedHashMap<>();
    private final CommandUsageSet<S> usages = new CommandUsageSet<>();
    private final AutoCompleter<S> autoCompleter;
    private final @Nullable CommandTree<S> tree;
    private final @NotNull CommandTreeVisualizer<S> visualizer;
    private @Nullable String permission;
    private Description description = Description.EMPTY;
    private boolean suppressACPermissionChecks = false;
    private CommandUsage<S> mainUsage = null;
    private CommandUsage<S> defaultUsage;

    private ParseElement<?> annotatedElement = null;

    private final Map<Class<? extends Throwable>, ThrowableResolver<?, S>> errorHandlers = new HashMap<>();

    private @NotNull CommandProcessingChain<S, CommandPreProcessor<S>> preProcessors =
            CommandProcessingChain.<S>preProcessors()
            .build();

    private @NotNull CommandProcessingChain<S, CommandPostProcessor<S>> postProcessors =
            CommandProcessingChain.<S>postProcessors()
            .build();

    private @Nullable Command<S> parent;
    private final CommandUsage<S> emptyUsage;
    private final @NotNull SuggestionResolver<S> suggestionResolver;
    private final Imperat<S> imperat;
    
    CommandImpl(Imperat<S> imperat, String name) {
        this(imperat, name, null);
    }

    CommandImpl(Imperat<S> imperat, String name, ParseElement<?> annotatedElement) {
        this(imperat, null, name, annotatedElement);
    }

    //sub-command constructor
    CommandImpl(Imperat<S> imperat, @Nullable Command<S> parent, String name) {
        this(imperat, parent, name, null);
    }

    CommandImpl(Imperat<S> imperat, @Nullable Command<S> parent, String name, ParseElement<?> annotatedElement) {
        this(imperat, parent, -1, name, annotatedElement);
    }

    CommandImpl(Imperat<S> imperat, @Nullable Command<S> parent, int position, String name) {
        this(imperat, parent, position, name, null);
    }

    CommandImpl(Imperat<S> imperat, @Nullable Command<S> parent, int position, String name, @Nullable ParseElement<?> annotatedElement) {
        this.imperat = imperat;
        this.parent = parent;
        this.position = position;
        this.name = name.toLowerCase();
        this.emptyUsage = CommandUsage.<S>builder().build(this);
        this.defaultUsage = imperat.config().getGlobalDefaultUsage().build(this);
        this.autoCompleter = AutoCompleter.createNative(this);
        this.tree = parent != null ? null : CommandTree.create(imperat.config(), this);
        this.visualizer = CommandTreeVisualizer.of(tree);
        this.suggestionResolver = SuggestionResolver.forCommand(this);
        this.annotatedElement = annotatedElement;
    }

    @Override
    public @NotNull Imperat<S> imperat() {
        return imperat;
    }
    
    /**
     * @return the name of the command
     */
    @Override
    public String name() {
        return name;
    }


    @Override
    public @Nullable ParseElement<?> getAnnotatedElement() {
        return annotatedElement;
    }

    /**
     * @return the aliases for this commands
     */
    @Override
    public @UnmodifiableView List<String> aliases() {
        return aliases;
    }

    /**
     * @return The permission of the command
     */
    @Override
    public @Unmodifiable Set<String> getPermissions() {
        if(permission == null) {
            return Collections.emptySet();
        }
        return Set.of(permission);
    }

    /**
     * Sets the permission of a command
     *
     * @param permission the permission of a command
     */
    @Override
    public void addPermission(@Nullable String permission) {
        this.permission = permission;
    }

    /**
     * @return The description of a command
     */
    @Override
    public @NotNull Description description() {
        return description;
    }

    @Override
    public void describe(Description description) {
        this.description = description;
    }

    /**
     * @return the index of this parameter
     */
    @Override
    public int position() {
        return position;
    }

    /**
     * Sets the position of this command in a syntax
     * DO NOT USE THIS FOR ANY REASON unless it's necessary to do so
     *
     * @param position the position to set
     */
    @Override
    public void position(int position) {
        throw new UnsupportedOperationException("You can't modify the position of a command");
    }
    
    @Override
    public @Nullable String getSinglePermission() {
        return permission;
    }
    
    @Override
    public @NotNull CommandPathSearch<S> contextMatch(Context<S> context) {
        if (tree != null) {
            var copy = context.arguments().copy();
            copy.removeIf(String::isBlank);
            return tree.contextMatch(context, copy);
        } else {
            throw new IllegalCallerException("Cannot match a sub command in a root's execution !");
        }
    }

    @Override
    public void visualizeTree() {
        ImperatDebugger.debug("Visualizing %s's tree", this.name);
        visualizer.visualizeSimple();
        visualizer.visualizeUniqueTreeSimple();
    }

    /**
     * Sets a pre-processor for the command
     *
     * @param preProcessor the pre-processor for the command
     */
    @Override
    public void addPreProcessor(@NotNull CommandPreProcessor<S> preProcessor) {
        this.preProcessors.add(preProcessor);
    }

    /**
     * Executes the pre-processing instructions in {@link CommandPreProcessor}
     *
     * @param api     the api
     * @param context the context
     * @param usage   the usage detected being used
     */
    @Override
    public void preProcess(@NotNull Imperat<S> api, @NotNull Context<S> context, @NotNull CommandUsage<S> usage) throws ProcessorException {
        for(var processor : preProcessors) {
            try {
                processor.process(api, context, usage);
            } catch (CommandException e) {
                throw new ProcessorException(ProcessorException.Type.PRE, this, e);
            }
        }
    }

    /**
     * Sets a post-processor for the command
     *
     * @param postProcessor the post-processor for the command
     */
    @Override
    public void addPostProcessor(@NotNull CommandPostProcessor<S> postProcessor) {
        this.postProcessors.add(postProcessor);
    }

    /**
     * Executes the post-processing instructions in {@link CommandPostProcessor}
     *
     * @param api     the api
     * @param context the context
     * @param usage   the usage detected being used
     */
    @Override
    public void postProcess(@NotNull Imperat<S> api, @NotNull ExecutionContext<S> context, @NotNull CommandUsage<S> usage) throws ProcessorException {
        for(var processor : postProcessors) {
            try {
                processor.process(api, context);
            } catch (CommandException e) {
                throw new ProcessorException(ProcessorException.Type.POST, this, e);
            }

        }
    }

    /**
     * Retrieves a usage with no args for this command
     *
     * @return A usage with empty parameters.
     */
    @Override 
    public @NotNull CommandUsage<S> getEmptyUsage() {
        return emptyUsage;
    }

    /**
     * Casts the parameter to a flag parameter
     *
     * @return the parameter as a flag
     */
    @Override
    public FlagParameter<S> asFlagParameter() {
        throw new UnsupportedOperationException("A command cannot be treated as a flag !");
    }

    @Override
    public boolean isGreedyString() {
        return false;
    }

    /**
     * Fetches the suggestion resolver linked to this
     * command parameter.
     *
     * @return the {@link SuggestionResolver} for a resolving suggestion
     */
    @Override
    public @NotNull SuggestionResolver<S> getSuggestionResolver() {
        return suggestionResolver;
    }
    
    @Override
    public void setFormat(String format) {
        throw new UnsupportedOperationException("You cannot change the format of a command/literal parameter");
    }
    
    @Override
    public boolean similarTo(CommandParameter<?> parameter) {
        return this.name.equalsIgnoreCase(parameter.name());
    }


    @Override
    public CommandTree<S> tree() {
        return this.tree;
    }

    /**
     * Sets the aliases of a command
     *
     * @param aliases the aliases for te command to set
     */
    @Override
    public void addAliases(List<String> aliases) {
        for (String alias : aliases)
            this.aliases.add(alias.toLowerCase());
    }

    /**
     * @return the default usage of the command
     * without any args
     */
    @Override
    public @NotNull CommandUsage<S> getDefaultUsage() {
        return defaultUsage;
    }

    /**
     * Sets the default command usage representation.
     *
     * @param usage the default command usage instance to be set, which must not be null
     */
    @Override
    public void setDefaultUsage(@NotNull CommandUsage<S> usage) {
        this.defaultUsage = Objects.requireNonNull(usage, "Default usage cannot be null");
    }
    

    /**
     * Adds a usage to the command
     *
     * @param usage the usage {@link CommandUsage} of the command
     */
    @Override
    public void addUsage(CommandUsage<S> usage) {
        
        if(tree != null) {
            tree.parseUsage(usage);
        }
        
        if (usage.isDefault()) {
            this.defaultUsage = usage;
        }

        usages.put(usage);

        if (mainUsage == null && !usage.isDefault() && usage.getMaxLength() >= 1 && !usage.hasParamType(Command.class)) {
            mainUsage = usage;
        }
    }
    
    /**
     * @return all {@link CommandUsage} that were registered
     * to this command by the user
     */
    @Override
    public Collection<? extends CommandUsage<S>> usages() {
        return usages.asSortedSet();
    }
    
    /**
     * @return the usage that doesn't include any subcommands, only
     * required parameters
     */
    @Override
    public @NotNull CommandUsage<S> getMainUsage() {
        return Optional.ofNullable(mainUsage)
            .orElse(defaultUsage);
    }

    /**
     * @return Returns {@link AutoCompleter}
     * that handles all auto-completes for this command
     */
    @Override
    public AutoCompleter<S> autoCompleter() {
        return autoCompleter;
    }

    /**
     * @return Whether this command is a sub command or not
     */
    @Override
    public @Nullable Command<S> parent() {
        return parent;
    }

    /**
     * sets the parent command
     *
     * @param parent the parent to set.
     */
    @Override
    public void parent(@NotNull Command<S> parent) {
        this.parent = parent;
    }

    @Override
    public void registerSubCommand(Command<S> command) {
        children.put(command.name(), command);
    }
    
    @Override
    public @NotNull CommandProcessingChain<S, CommandPreProcessor<S>> getPreProcessors() {
        return preProcessors;
    }
    
    @Override
    public @NotNull CommandProcessingChain<S, CommandPostProcessor<S>> getPostProcessors() {
        return postProcessors;
    }
    
    @Override
    public void setPreProcessingChain(CommandProcessingChain<S, CommandPreProcessor<S>> chain) {
        this.preProcessors = chain;
    }
    
    @Override
    public void setPostProcessingChain(CommandProcessingChain<S, CommandPostProcessor<S>> chain) {
        this.postProcessors = chain;
    }
    
    /**
     * Injects a created-subcommand directly into the parent's command usages.
     *
     * @param command        the subcommand to inject
     * @param attachmentMode see {@link AttachmentMode}
     */
    @Override
    public void addSubCommand(Command<S> command, AttachmentMode attachmentMode) {
        command.parent(this);
        registerSubCommand(command);

        final CommandUsage<S> prime;
        switch (attachmentMode) {
            case EMPTY -> prime = getEmptyUsage();
            case MAIN, UNSET -> prime = getMainUsage();
            case DEFAULT -> prime = getDefaultUsage();
            default -> throw new IllegalArgumentException("Unknown attachment mode: " + attachmentMode);
        }
        CommandUsage<S> combo = prime.mergeWithCommand(command, command.getMainUsage());
        //adding the merged command usage

        this.addUsage(combo);

        for (CommandUsage<S> subUsage : command.usages()) {
            if (subUsage.equals(command.getMainUsage())) continue;
            combo = prime.mergeWithCommand(command, subUsage);
            //adding the merged command usage

            this.addUsage(
                combo
            );
        }

    }
    
    @Override
    public void addSubCommandUsage(
        String subCommand,
        List<String> aliases,
        CommandUsage.Builder<S> usage,
        AttachmentMode attachmentMode
    ) {
        int position;
        if (attachmentMode == AttachmentMode.EMPTY) {
            position = position() + 1;
        } else {
            CommandUsage<S> main = attachmentMode == AttachmentMode.MAIN ? getMainUsage() : getDefaultUsage();
            position = this.position() + (main.getMinLength() == 0 ? 1 : main.getMinLength());
        }

        //creating subcommand to modify
        Command<S> subCmd =
            Command.create(imperat, this, position, subCommand.toLowerCase())
                .aliases(aliases)
                .usage(usage)
                .build();
        addSubCommand(subCmd, attachmentMode);
    }

    /**
     * @param name the name of the wanted sub-command
     * @return the sub-command of a specific name directly from
     * this instance of a command, meaning that
     * it won't go deeply in search for sub-command
     */
    @Override
    public @Nullable Command<S> getSubCommand(String name) {
        Command<S> sub = children.get(name);
        if (sub != null)
            return sub;

        for (String subsNames : children.keySet()) {
            Command<S> other = children.get(subsNames);
            if (other.hasName(name)) return other;
            else if (subsNames.startsWith(name)) return other;
        }
        return null;
    }

    /**
     * @return the subcommands of this command
     */
    @Override
    public @NotNull Collection<? extends Command<S>> getSubCommands() {
        return children.values();
    }

    /**
     * whether to ignore permission checks on the auto-completion of command and
     * sub commands or not
     *
     * @return whether to ignore permission checks on the auto-completion of command and
     * sub commands or not
     */
    @Override
    public boolean isIgnoringACPerms() {
        return suppressACPermissionChecks;
    }

    /**
     * if true, it will ignore permission checks
     * on the auto-completion of command and sub commands
     * <p>
     * otherwise, it will perform permission checks and
     * only tab-completes the usages/subcommands that you have permission for
     *
     * @param suppress true if you want to ignore the permission checks on tab completion of args
     */
    @Override
    public void ignoreACPermissions(boolean suppress) {
        this.suppressACPermissionChecks = suppress;
    }

    
    @Override
    public <T extends Throwable> void setThrowableResolver(Class<T> exception, ThrowableResolver<T, S> resolver) {
        errorHandlers.put(exception, resolver);
    }
    
    @Override @SuppressWarnings("unchecked")
    public @Nullable <T extends Throwable> ThrowableResolver<T, S> getThrowableResolver(Class<T> exception) {
        
        Class<?> current = exception;
        while (current != null && Throwable.class.isAssignableFrom(current)) {
            var resolver = errorHandlers.get(current);
            if (resolver != null) {
                return (ThrowableResolver<T, S>) resolver;
            }
            current = current.getSuperclass();
        }
        
        return null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommandImpl<?> command)) return false;
        return Objects.equals(name, command.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
    
    /**
     * Creates a copy of this command with a different position.
     * Useful for commands that have multiple syntaxes.
     *
     * @param newPosition the new position to set
     * @return a copy of this command with the new position
     */
    @Override
    public CommandParameter<S> copyWithDifferentPosition(int newPosition) {
        CommandImpl<S> copy = new CommandImpl<>(this.imperat, this.parent, newPosition, this.name);
        
        // Copy basic properties
        copy.permission = this.permission;
        copy.description = this.description;
        copy.suppressACPermissionChecks = this.suppressACPermissionChecks;
        copy.aliases.addAll(this.aliases);
        
        // Copy usages
        for (CommandUsage<S> usage : this.usages()) {
            copy.addUsage(usage);
        }
        
        // Copy sub-commands
        for (Command<S> subCommand : this.getSubCommands()) {
            copy.registerSubCommand(subCommand);
        }
        
        // Copy flags
        //copy.freeFlags.addAll(this.freeFlags);
        
        // Copy error handlers
        copy.errorHandlers.putAll(this.errorHandlers);
        
        // Copy processors
        for (CommandPreProcessor<S> processor : this.preProcessors) {
            copy.addPreProcessor(processor);
        }
        for (CommandPostProcessor<S> processor : this.postProcessors) {
            copy.addPostProcessor(processor);
        }
        
        // Set default usage if it was customized
        copy.setDefaultUsage(this.defaultUsage);
        
        return copy;
    }

    @Override
    public @NotNull Queue<ArgValidator<S>> getValidatorsQueue() {
        throw new UnsupportedOperationException("A command does not have argument validators !");
    }

    @Override
    public void validate(Context<S> context, Argument<S> argument) throws InvalidArgumentException {
        throw new UnsupportedOperationException("A command does not have argument validators !");
    }

}
