package studio.mevera.imperat.command;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import studio.mevera.imperat.BaseThrowableHandler;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.annotations.base.element.ParseElement;
import studio.mevera.imperat.annotations.parameters.AnnotatedArgument;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.DefaultValueProvider;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.command.parameters.type.ArgumentTypes;
import studio.mevera.imperat.command.processors.CommandPostProcessor;
import studio.mevera.imperat.command.processors.CommandPreProcessor;
import studio.mevera.imperat.command.processors.CommandProcessingChain;
import studio.mevera.imperat.command.suggestions.AutoCompleter;
import studio.mevera.imperat.command.tree.CommandPathSearch;
import studio.mevera.imperat.command.tree.CommandTree;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.permissions.PermissionsData;
import studio.mevera.imperat.util.TypeWrap;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Represents a wrapper for the actual command's data
 *
 * @param <S> the command sender valueType
 */
@ApiStatus.AvailableSince("1.0.0")
public interface Command<S extends Source> extends Argument<S>, BaseThrowableHandler<S> {

    static <S extends Source> Command.Builder<S> create(@NotNull Imperat<S> imperat, String name) {
        return create(imperat, null, name);
    }

    static <S extends Source> Command.Builder<S> create(@NotNull Imperat<S> imperat, String name, @Nullable ParseElement<?> annotatedElement) {
        return create(imperat, null, name, annotatedElement);
    }

    static <S extends Source> Command.Builder<S> create(
            @NotNull Imperat<S> imperat,
            @Nullable Command<S> parent,
            @NotNull String name
    ) {
        return create(imperat, parent, -1, name);
    }

    static <S extends Source> Command.Builder<S> create(
            @NotNull Imperat<S> imperat,
            @Nullable Command<S> parent,
            @NotNull String name,
            @Nullable ParseElement<?> annotatedElement
    ) {
        return create(imperat, parent, -1, name, annotatedElement);
    }

    static <S extends Source> Command.Builder<S> create(
            @NotNull Imperat<S> imperat,
            @Nullable Command<S> parent,
            int position,
            @NotNull String name
    ) {
        return new Builder<>(imperat, parent, position, name);
    }

    static <S extends Source> Command.Builder<S> create(
            @NotNull Imperat<S> imperat,
            @Nullable Command<S> parent,
            int position,
            @NotNull String name,
            @Nullable ParseElement<?> annotatedElement
    ) {
        return new Builder<>(imperat, parent, position, name, annotatedElement);
    }

    @NotNull
    Imperat<S> imperat();

    /**
     * @return The name of the command
     */
    String getName();

    /**
     * @return The aliases for this commands
     */
    @UnmodifiableView
    List<String> aliases();

    /**
     * @return the annotated element this object got extracted from, null if it was created from a {@link ParseElement}
     */
    @Nullable ParseElement<?> getAnnotatedElement();

    @Override
    default boolean isAnnotated() {
        return getAnnotatedElement() != null;
    }

    @Override
    default AnnotatedArgument<S> asAnnotatedArgument() {
        throw new UnsupportedOperationException("This command is not an annotated argument");
    }

    /**
     * Sets the aliases of a command
     *
     * @param aliases the aliases for te command to set
     */
    void addAliases(List<String> aliases);

    /**
     * Adds aliases for the command using an array of alias strings.
     * <p>
     * This method internally converts the array to a list and calls
     * the {@code addAliases(List<String> aliases)} method to set the aliases.
     * </p>
     * @param aliases the array of alias strings to be added
     */
    default void addAliases(String... aliases) {
        addAliases(List.of(aliases));
    }

    /**
     * @param name the name used
     * @return Whether this command has this name/alias
     */
    default boolean hasName(String name) {
        return this.getName().equalsIgnoreCase(name) || this.aliases().contains(name.toLowerCase());
    }


    /**
     * @return The tree for the command
     */
    CommandTree<S> tree();

    /**
     * Debugs or visualizes all tree nodes
     * from {@link CommandTree}.
     * If the command is not a root command,
     * nothing will be visualized.
     */
    void visualizeTree();

    /**
     * Traverses the {@link CommandTree} linked to
     * this command object, searching for the most suitable usage that
     * best suites the context input by the user
     *
     * @param context the context of the execution
     */
    @NotNull
    CommandPathSearch<S> contextMatch(Context<S> context);

    /**
     * @return The description of a command
     */
    @NotNull
    Description getDescription();

    /**
     * Retrieves the parameter type associated with the current command,
     * including its name and any aliases.
     *
     * @return a ArgumentType instance representing the command's parameter type,
     *         encapsulating its name and aliases
     */
    @Override
    default @NotNull ArgumentType<S, ?> type() {
        return ArgumentTypes.command(getName(), aliases());
    }

    /**
     * Sets the position of this command in a syntax
     * DO NOT USE THIS FOR ANY REASON unless it's necessary to do so
     *
     * @param position the position to set
     */
    @ApiStatus.Internal
    default void setPosition(int position) {
        throw new UnsupportedOperationException("You can't modify the position of a command");
    }


    /**
     * @return the default value if it's input is not present
     * in case of the parameter being optional
     */
    @Override
    default @NotNull DefaultValueProvider getDefaultValueSupplier() {
        return DefaultValueProvider.of(getName());
    }

    /**
     * Sets a pre-processor for the command
     *
     * @param preProcessor the pre-processor for the command
     */
    void addPreProcessor(@NotNull CommandPreProcessor<S> preProcessor);

    /**
     * Executes the pre-processing instructions in {@link CommandPreProcessor}
     *
     * @param api     the api
     * @param context the context
     * @param usage   the usage detected being used
     */
    void preProcess(@NotNull Imperat<S> api, @NotNull Context<S> context, @NotNull CommandPathway<S> usage) throws CommandException;

    /**
     * Sets a post-processor for the command
     *
     * @param postProcessor the post-processor for the command
     */
    void addPostProcessor(@NotNull CommandPostProcessor<S> postProcessor);

    /**
     * Executes the post-processing instructions in {@link CommandPostProcessor}
     *
     * @param api     the api
     * @param context the context
     * @param usage   the usage detected being used
     */
    void postProcess(@NotNull Imperat<S> api, @NotNull ExecutionContext<S> context, @NotNull CommandPathway<S> usage) throws CommandException;

    /**
     * @return the default usage of the command
     * without any args
     */
    @NotNull
    @ApiStatus.AvailableSince("1.9.0")
    CommandPathway<S> getDefaultPathway();


    /**
     * Sets the default command usage representation.
     *
     * @param usage the default command usage instance to be set, which must not be null
     */
    @ApiStatus.AvailableSince("1.9.0")
    void setDefaultPathway(@NotNull CommandPathway<S> usage);


    /**
     * Adds a usage to the command
     *
     * @param usage the usage {@link CommandPathway} of the command
     */
    void addPathway(CommandPathway<S> usage);

    default void addPathway(CommandPathway.Builder<S> builder) {
        addPathway(builder.build(this));
    }

    /**
     * @return All {@link CommandPathway} that were registered
     * to this command by the user
     */
    Collection<? extends CommandPathway<S>> getAllPossiblePathways();

    /**
     * @return the pathways that are directly attached to this command
     * without being inherited from a parent command
     */
    Collection<? extends CommandPathway<S>> getDedicatedPathways();

    /**
     * @return Returns {@link AutoCompleter}
     * that handles all auto-completes for this command
     */
    AutoCompleter<S> autoCompleter();

    /**
     * Injects a created-subcommand directly into the parent's command usages.
     *
     * @param command the subcommand to inject
     */
    void addSubCommand(Command<S> command);

    /**
     * Creates and adds a new sub-command (if it doesn't exist) then add
     * the {@link CommandPathway} to the sub-command
     *
     * @param subCommand the sub-command's unique name
     * @param aliases    of the subcommand
     * @param usage      the usage
     */
    void addSubCommandUsage(
            String subCommand,
            List<String> aliases,
            CommandPathway.Builder<S> usage);

    default void addSubCommandUsage(
            String subCommand,
            CommandPathway.Builder<S> usage
    ) {
        addSubCommandUsage(subCommand, Collections.emptyList(), usage);
    }

    /**
     * @param name the name of the wanted sub-command
     * @return the sub-command of specific name
     */
    @Nullable
    Command<S> getSubCommand(String name);

    /**
     * @return the subcommands of this command
     */
    @NotNull
    Collection<? extends Command<S>> getSubCommands();

    default @Nullable CommandPathway<S> getUsage(Predicate<CommandPathway<S>> usagePredicate) {
        for (var usage : getDedicatedPathways()) {
            if (usagePredicate.test(usage)) {
                return usage;
            }
        }
        return null;
    }

    default boolean hasParent() {
        return getParent() != null;
    }

    default boolean isSubCommand() {
        return hasParent();
    }

    /**
     * @return the parent command of this command, null if it doesn't have a parent
     */
    @Nullable Command<S> getShortcut(String shortcutName);

    /**
     * @return the shortcuts of this command
     */
    @UnmodifiableView Collection<? extends Command<S>> getShortcuts();

    /**
     * Adds a shortcut to this command. Shortcuts are alternative names for the same command, allowing users to execute the command using different aliases.
     *
     * @param shortcut the RootCommand instance representing the shortcut to be added
     */
    void addShortcut(Command<S> shortcut);

    /**
     * @return the value valueType of this parameter
     */
    @Override
    default TypeWrap<?> wrappedType() {
        return TypeWrap.of(Command.class);
    }

    /**
     * @return whether this is an optional argument
     */
    @Override
    default boolean isOptional() {
        return false;
    }

    /**
     * @return checks whether this parameter
     * consumes all the args input after it.
     */
    @Override
    default boolean isGreedy() {
        return false;
    }

    /**
     * @return checks whether this parameter is a flag
     */
    @Override
    default boolean isFlag() {
        return false;
    }

    @Override
    @SuppressWarnings("all")
    default Command<S> asCommand() {
        return (Command<S>) this;
    }

    /**
     * Formats the usage parameter
     *
     * @return the formatted parameter
     */
    @Override
    default String format() {
        return getName();
    }

    /**
     * whether to ignore permission checks on the auto-completion of command and
     * sub commands or not
     *
     * @return whether to ignore permission checks on the auto-completion of command and
     * sub commands or not
     */
    boolean isIgnoringACPerms();

    /**
     * if true, it will ignore permission checks
     * on the auto-completion of command and sub commands
     * <p>
     * otherwise, it will perform permission checks and
     * only tab-completes the usages/subcommands that you have permission for
     *
     * @param ignore true if you want to ignore the permission checks on tab completion of args
     */
    void setIgnoreACPermissions(boolean ignore);

    CommandProcessingChain<S, CommandPreProcessor<S>> getPreProcessors();

    CommandProcessingChain<S, CommandPostProcessor<S>> getPostProcessors();

    void setPreProcessingChain(CommandProcessingChain<S, CommandPreProcessor<S>> chain);

    void setPostProcessingChain(CommandProcessingChain<S, CommandPostProcessor<S>> chain);

    Collection<? extends Command<S>> getAllShortcuts();

    class Builder<S extends Source> {

        private final Command<S> cmd;

        Builder(@NotNull Imperat<S> imperat, @Nullable Command<S> parent, int position, String name, ParseElement<?> parseElement) {
            this.cmd = new CommandImpl<>(imperat, parent, position, name, parseElement);
        }

        Builder(@NotNull Imperat<S> imperat, @Nullable Command<S> parent, int position, String name) {
            this(imperat, parent, position, name, null);
        }

        public Builder<S> ignoreACPermissions(boolean ignore) {
            this.cmd.setIgnoreACPermissions(ignore);
            return this;
        }

        public Builder<S> aliases(String... aliases) {
            this.cmd.addAliases(aliases);
            return this;
        }

        public Builder<S> aliases(List<String> aliases) {
            this.cmd.addAliases(aliases);
            return this;
        }

        public Builder<S> description(String description) {
            this.cmd.describe(description);
            return this;
        }

        public Builder<S> description(Description description) {
            return description(description.getValue());
        }

        public Builder<S> permission(PermissionsData permission) {
            this.cmd.setPermissionData(permission);
            return this;
        }

        public Builder<S> defaultExecution(CommandExecution<S> defaultExec) {
            return usage(CommandPathway.<S>builder()
                                 .execute(defaultExec));
        }

        public Builder<S> usage(CommandPathway.Builder<S> usage) {
            cmd.addPathway(usage);
            return this;
        }

        public Builder<S> subCommand(Command<S> subCommand) {
            cmd.addSubCommand(subCommand);
            return this;
        }


        public Builder<S> preProcessor(CommandPreProcessor<S> preProcessor) {
            cmd.addPreProcessor(preProcessor);
            return this;
        }

        public Builder<S> postProcessor(CommandPostProcessor<S> postProcessor) {
            cmd.addPostProcessor(postProcessor);
            return this;
        }

        public Builder<S> parent(@Nullable Command<S> parentCmd) {
            cmd.setParent(parentCmd);
            return this;
        }


        public Builder<S> setMetaPropertiesFromOtherCommand(Command<S> other) {
            cmd.setPermissionData(other.getPermissionsData());
            cmd.setDefaultPathway(other.getDefaultPathway());
            cmd.setPreProcessingChain(other.getPreProcessors());
            cmd.setPostProcessingChain(other.getPostProcessors());
            cmd.describe(other.getDescription());
            cmd.setIgnoreACPermissions(other.isIgnoringACPerms());

            return this;
        }

        public Command<S> build() {
            return cmd;
        }
    }
}
