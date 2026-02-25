package studio.mevera.imperat.command;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.annotations.base.element.ParseElement;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.FlagArgument;
import studio.mevera.imperat.command.parameters.validator.ArgValidator;
import studio.mevera.imperat.command.processors.CommandPostProcessor;
import studio.mevera.imperat.command.processors.CommandPreProcessor;
import studio.mevera.imperat.command.processors.CommandProcessingChain;
import studio.mevera.imperat.command.suggestions.AutoCompleter;
import studio.mevera.imperat.command.tree.CommandPathSearch;
import studio.mevera.imperat.command.tree.CommandTree;
import studio.mevera.imperat.command.tree.CommandTreeVisualizer;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.ParsedArgument;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.ProcessorException;
import studio.mevera.imperat.exception.ThrowableResolver;
import studio.mevera.imperat.permissions.PermissionsData;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.util.ImperatDebugger;
import studio.mevera.imperat.util.PriorityList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ApiStatus.Internal
final class CommandImpl<S extends Source> implements Command<S> {

    private final Imperat<S> imperat;

    private final String name;
    private final int position;
    private final List<String> aliases = new ArrayList<>();
    private final Map<String, Command<S>> children = new LinkedHashMap<>();
    private final CommandPathwaySet<S> allPathways = new CommandPathwaySet<>();
    private final AutoCompleter<S> autoCompleter;
    private final @NotNull CommandTree<S> tree;
    private final @NotNull CommandTreeVisualizer<S> visualizer;
    private final Map<Class<? extends Throwable>, ThrowableResolver<?, S>> errorHandlers = new HashMap<>();
    private final @NotNull SuggestionProvider<S> suggestionProvider;
    private PermissionsData permissions = PermissionsData.empty();
    private Description description = Description.EMPTY;
    private boolean suppressACPermissionChecks = false;

    //pathways that are directly linked to this command, meaning that they don't include any sub-command in their allPathways
    private final CommandPathwaySet<S> dedicatedPathways = new CommandPathwaySet<>();
    private CommandPathway<S> defaultPathway;

    private final Map<String, Command<S>> shortcuts = new HashMap<>();

    private ParseElement<?> annotatedElement = null;

    private @NotNull CommandProcessingChain<S, CommandPreProcessor<S>> preProcessors =
            CommandProcessingChain.<S>preProcessors()
                    .build();

    private @NotNull CommandProcessingChain<S, CommandPostProcessor<S>> postProcessors =
            CommandProcessingChain.<S>postProcessors()
                    .build();
    private @Nullable Command<S> parent;


    CommandImpl(
            Imperat<S> imperat,
            @Nullable Command<S> parent,
            int position,
            String name,
            @Nullable ParseElement<?> annotatedElement
    ) {
        this.imperat = imperat;
        this.parent = parent;
        this.position = position;
        this.name = name.toLowerCase();
        this.setDefaultPathwayWithValidation(imperat.config().getGlobalDefaultPathway().build(this));
        this.autoCompleter = AutoCompleter.createNative(this);
        this.tree = CommandTree.create(imperat.config(), this);
        this.visualizer = CommandTreeVisualizer.of(tree);
        this.suggestionProvider = SuggestionProvider.forCommand(this);
        this.annotatedElement = annotatedElement;
    }

    private void setDefaultPathwayWithValidation(CommandPathway<S> pathway) {
        if (!pathway.isDefault()) {
            throw new IllegalArgumentException(
                    "The provided pathway is not a default pathway, it must be marked as default to be set as the default pathway");
        }
        //dedicatedPathways.put(pathway);
        this.defaultPathway = pathway;
    }

    @Override
    public Collection<? extends CommandPathway<S>> getDedicatedPathways() {
        return dedicatedPathways.asSortedSet();
    }

    @Override
    public @NotNull Imperat<S> imperat() {
        return imperat;
    }

    /**
     * @return the name of the command
     */
    @Override
    public String getName() {
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
     * @return The description of a command
     */
    @Override
    public @NotNull Description getDescription() {
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
    public int getPosition() {
        return position;
    }

    /**
     * Sets the position of this command in a syntax
     * DO NOT USE THIS FOR ANY REASON unless it's necessary to do so
     *
     * @param position the position to set
     */
    @Override
    public void setPosition(int position) {
        throw new UnsupportedOperationException("You can't modify the position of a command");
    }

    @Override
    public @NotNull CommandPathSearch<S> contextMatch(Context<S> context) {
        var copy = context.arguments().copy();
        copy.removeIf(String::isBlank);
        return tree.contextMatch(context, copy);
    }

    @Override
    public void visualizeTree() {
        ImperatDebugger.debug("Visualizing %s's tree", this.name);
        visualizer.visualizeSimple();
        visualizer.visualizeUniqueTreeSimple();
        ImperatDebugger.debug("Visualizing %s's unflagged tree", this.name);
        visualizer.visualizeUnflaggedTreeSimple();
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
    public void preProcess(@NotNull Imperat<S> api, @NotNull Context<S> context, @NotNull CommandPathway<S> usage) throws ProcessorException {
        for (var processor : preProcessors) {
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
    public void postProcess(@NotNull Imperat<S> api, @NotNull ExecutionContext<S> context, @NotNull CommandPathway<S> usage)
            throws ProcessorException {
        for (var processor : postProcessors) {
            try {
                processor.process(api, context);
            } catch (CommandException e) {
                throw new ProcessorException(ProcessorException.Type.POST, this, e);
            }

        }
    }

    /**
     * Casts the parameter to a flag parameter
     *
     * @return the parameter as a flag
     */
    @Override
    public FlagArgument<S> asFlagParameter() {
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
     * @return the {@link SuggestionProvider} for a resolving suggestion
     */
    @Override
    public @NotNull SuggestionProvider<S> getSuggestionResolver() {
        return suggestionProvider;
    }

    @Override
    public void setFormat(String format) {
        throw new UnsupportedOperationException("You cannot change the format of a command/literal parameter");
    }

    @Override
    public boolean similarTo(Argument<?> parameter) {
        return this.name.equalsIgnoreCase(parameter.getName());
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
        for (String alias : aliases) {
            this.aliases.add(alias.toLowerCase());
        }
    }

    /**
     * @return the default usage of the command
     * without any args
     */
    @Override
    public @NotNull CommandPathway<S> getDefaultPathway() {
        return defaultPathway;
    }

    /**
     * Sets the default command usage representation.
     *
     * @param usage the default command usage instance to be set, which must not be null
     */
    @Override
    public void setDefaultPathway(@NotNull CommandPathway<S> usage) {
        this.defaultPathway = Objects.requireNonNull(usage, "Default usage cannot be null");
    }


    /**
     * Adds a usage to the command
     *
     * @param usage the usage {@link CommandPathway} of the command
     */
    @Override
    public void addPathway(CommandPathway<S> usage) {

        tree.parseUsage(usage);

        if (usage.isDefault()) {
            System.out.println("DEFAULT PATHWAY DETECTED FOR COMMAND " + this.name + ", SETTING IT AS THE DEFAULT PATHWAY");
            this.defaultPathway = usage;
        }

        dedicatedPathways.put(usage);
        System.out.println("INTERNAL FROM DEDICATED ITSELF");
        for (CommandPathway<S> dedicated : dedicatedPathways) {
            System.out.println("ADDED-PATHWAY: '" + dedicated.formatted() + "', METHOD: " + (dedicated.getMethodElement() != null ?
                                                                                                     dedicated.getMethodElement().getName() :
                                                                                                     "null"));
        }

        System.out.println("Added pathway '" + usage.formatted() + "' to command " + this.name);
        System.out.println("METHOD: " + (usage.getMethodElement() != null ? usage.getMethodElement().getName() : "null"));
    }

    /**
     * @return all {@link CommandPathway} that were registered
     * to this command by the user
     */
    @Override
    public Collection<? extends CommandPathway<S>> getAllPossiblePathways() {
        return allPathways.asSortedSet();
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
    public @Nullable Command<S> getParent() {
        return parent;
    }

    /**
     * sets the parent command
     *
     * @param parent the parent to set.
     */
    @Override
    public void setParent(@NotNull Command<S> parent) {
        this.parent = parent;
        System.out.println("Setting parent of command '" + this.name + "' to '" + parent.getName() + "'");
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

    @Override
    public Collection<? extends Command<S>> getAllShortcuts() {
        List<Command<S>> allShortcuts = new ArrayList<>(shortcuts.values());
        for (Command<S> subCommand : this.getSubCommands()) {
            allShortcuts.addAll(subCommand.getAllShortcuts());
        }
        return allShortcuts;
    }


    /**
     * Injects a created-subcommand directly into the parent's command allPathways.
     *
     * @param subCommand the subcommand to inject
     */
    @Override
    public void addSubCommand(Command<S> subCommand) {
        subCommand.setParent(this);
        children.put(subCommand.getName(), subCommand);
        for (String alias : subCommand.aliases()) {
            children.put(alias, subCommand);
        }
        this.tree.parseSubCommand(subCommand);
    }

    @Override
    public void addSubCommandUsage(
            String subCommand,
            List<String> aliases,
            CommandPathway.Builder<S> usage
    ) {
        //creating subcommand to modify
        Command<S> subCmd =
                Command.create(imperat, this, subCommand.toLowerCase())
                        .aliases(aliases)
                        .usage(usage)
                        .build();
        addSubCommand(subCmd);
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
        if (sub != null) {
            return sub;
        }

        for (String subsNames : children.keySet()) {
            Command<S> other = children.get(subsNames);
            if (other.hasName(name)) {
                return other;
            } else if (subsNames.startsWith(name)) {
                return other;
            }
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

    @Override
    public @Nullable Command<S> getShortcut(String shortcutName) {
        return shortcuts.get(shortcutName);
    }

    @Override
    public @UnmodifiableView Collection<? extends Command<S>> getShortcuts() {
        return Collections.unmodifiableMap(shortcuts).values();
    }

    @Override
    public void addShortcut(Command<S> shortcut) {
        shortcuts.put(shortcut.getName(), shortcut);
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
     * only tab-completes the allPathways/subcommands that you have permission for
     *
     * @param suppress true if you want to ignore the permission checks on tab completion of args
     */
    @Override
    public void setIgnoreACPermissions(boolean suppress) {
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
        if (this == o) {
            return true;
        }
        if (!(o instanceof CommandImpl<?> command)) {
            return false;
        }
        return Objects.equals(name, command.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return format();
    }

    @Override
    public @NotNull PriorityList<ArgValidator<S>> getValidators() {
        throw new UnsupportedOperationException("A command does not have argument validators !");
    }

    @Override
    public void addValidator(@NotNull ArgValidator<S> validator) {
        throw new UnsupportedOperationException("A command does not have argument validators !");
    }

    @Override
    public void validate(ExecutionContext<S> context, ParsedArgument<S> parsedArgument) throws CommandException {
        throw new UnsupportedOperationException("A command does not have argument validators !");
    }

    @Override
    public @NotNull PermissionsData getPermissionsData() {
        return permissions;
    }

    @Override
    public void setPermissionData(@NotNull PermissionsData permissions) {
        this.permissions = permissions;
    }
}
