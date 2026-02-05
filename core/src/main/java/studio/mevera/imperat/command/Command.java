package studio.mevera.imperat.command;
import studio.mevera.imperat.command.parameters.type.ArgumentType;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import studio.mevera.imperat.BaseThrowableHandler;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.annotations.base.element.ParseElement;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.OptionalValueSupplier;
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
    String name();

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
        return this.name().equalsIgnoreCase(name) || this.aliases().contains(name.toLowerCase());
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
        return ArgumentTypes.command(name(), aliases());
    }

    /**
     * Sets the position of this command in a syntax
     * DO NOT USE THIS FOR ANY REASON unless it's necessary to do so
     *
     * @param position the position to set
     */
    @ApiStatus.Internal
    default void position(int position) {
        throw new UnsupportedOperationException("You can't modify the position of a command");
    }


    /**
     * @return the default value if it's input is not present
     * in case of the parameter being optional
     */
    @Override
    default @NotNull OptionalValueSupplier getDefaultValueSupplier() {
        return OptionalValueSupplier.of(name());
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
    void preProcess(@NotNull Imperat<S> api, @NotNull Context<S> context, @NotNull CommandUsage<S> usage) throws CommandException;

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
    void postProcess(@NotNull Imperat<S> api, @NotNull ExecutionContext<S> context, @NotNull CommandUsage<S> usage) throws CommandException;

    /**
     * Retrieves a usage with no args for this command
     * @return A usage with empty parameters.
     */
    @NotNull
    @ApiStatus.AvailableSince("1.9.0")
    CommandUsage<S> getEmptyUsage();

    /**
     * @return the default usage of the command
     * without any args
     */
    @NotNull
    @ApiStatus.AvailableSince("1.9.0")
    CommandUsage<S> getDefaultUsage();


    /**
     * Sets the default command usage representation.
     *
     * @param usage the default command usage instance to be set, which must not be null
     */
    @ApiStatus.AvailableSince("1.9.0")
    void setDefaultUsage(@NotNull CommandUsage<S> usage);


    /**
     * Adds a usage to the command
     *
     * @param usage the usage {@link CommandUsage} of the command
     */
    void addUsage(CommandUsage<S> usage);

    default void addUsage(CommandUsage.Builder<S> builder, boolean help) {
        addUsage(builder.build(this, help));
    }

    default void addUsage(CommandUsage.Builder<S> builder) {
        addUsage(builder, false);
    }

    /**
     * @return All {@link CommandUsage} that were registered
     * to this command by the user
     */
    Collection<? extends CommandUsage<S>> usages();

    /**
     * @return the usage that doesn't include any subcommands, only
     * required parameters
     */
    @NotNull
    CommandUsage<S> getMainUsage();

    /**
     * @return Returns {@link AutoCompleter}
     * that handles all auto-completes for this command
     */
    AutoCompleter<S> autoCompleter();

    /**
     * Injects a created-subcommand directly into the parent's command usages.
     *
     * @param command        the subcommand to inject
     * @param attachmentMode see {@link AttachmentMode}
     */
    void addSubCommand(Command<S> command, AttachmentMode attachmentMode);

    /**
     * Creates and adds a new sub-command (if it doesn't exist) then add
     * the {@link CommandUsage} to the sub-command
     *
     * @param subCommand     the sub-command's unique name
     * @param aliases        of the subcommand
     * @param usage          the usage
     * @param attachmentMode see {@link AttachmentMode}
     */
    void addSubCommandUsage(String subCommand,
            List<String> aliases,
            CommandUsage.Builder<S> usage,
            AttachmentMode attachmentMode);

    default void addSubCommandUsage(String subCommand,
            List<String> aliases,
            CommandUsage.Builder<S> usage) {
        addSubCommandUsage(subCommand, aliases, usage, imperat().config().getDefaultAttachmentMode());
    }

    default void addSubCommandUsage(String subCommand,
            CommandUsage.Builder<S> usage,
            AttachmentMode attachmentMode) {
        addSubCommandUsage(subCommand, Collections.emptyList(), usage, attachmentMode);
    }

    /**
     * Creates and adds a new sub-command (if it doesn't exist) then add
     * the {@link CommandUsage} to the sub-command
     *
     * @param subCommand the sub-command's unique name
     * @param usage      the usage
     */
    default void addSubCommandUsage(String subCommand, CommandUsage.Builder<S> usage) {
        addSubCommandUsage(subCommand, usage, imperat().config().getDefaultAttachmentMode());
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

    default @Nullable CommandUsage<S> getUsage(Predicate<CommandUsage<S>> usagePredicate) {
        for (var usage : usages()) {
            if (usagePredicate.test(usage)) {
                return usage;
            }
        }
        return null;
    }

    default boolean hasParent() {
        return parent() != null;
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
     * @param shortcut the Command instance representing the shortcut to be added
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
        return name();
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

    @ApiStatus.Internal
    @ApiStatus.AvailableSince("1.9.0")
    void registerSubCommand(Command<S> subCommand);

    CommandProcessingChain<S, CommandPreProcessor<S>> getPreProcessors();

    CommandProcessingChain<S, CommandPostProcessor<S>> getPostProcessors();

    void setPreProcessingChain(CommandProcessingChain<S, CommandPreProcessor<S>> chain);

    void setPostProcessingChain(CommandProcessingChain<S, CommandPostProcessor<S>> chain);

    Collection<? extends Command<S>> getAllShortcuts();

    class Builder<S extends Source> {

        private final Imperat<S> imperat;
        private final Command<S> cmd;

        Builder(@NotNull Imperat<S> imperat, @Nullable Command<S> parent, int position, String name, ParseElement<?> parseElement) {
            this.imperat = imperat;
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
            return usage(CommandUsage.<S>builder()
                                 .execute(defaultExec));
        }

        public Builder<S> usage(CommandUsage.Builder<S> usage) {
            cmd.addUsage(usage);
            return this;
        }

        public Builder<S> subCommand(Command<S> subCommand, AttachmentMode attachmentMode) {
            cmd.addSubCommand(subCommand, attachmentMode);
            return this;
        }

        public Builder<S> subCommand(Command<S> subCommand) {
            return subCommand(subCommand, AttachmentMode.DEFAULT);
        }

        public Builder<S> subCommand(String name, CommandUsage.Builder<S> mainUsage, AttachmentMode attachmentMode) {
            return subCommand(
                    Command.create(imperat, name)
                            .usage(mainUsage)
                            .build(),
                    attachmentMode
            );
        }

        public Builder<S> subCommand(String name, CommandUsage.Builder<S> mainUsage, AttachmentMode attachmentMode,
                @Nullable ParseElement<?> annotatedElement) {
            return subCommand(
                    Command.create(imperat, name, annotatedElement)
                            .usage(mainUsage)
                            .build(),
                    attachmentMode
            );
        }

        public Builder<S> subCommand(String name, CommandUsage.Builder<S> mainUsage) {
            return subCommand(name, mainUsage, cmd.imperat().config().getDefaultAttachmentMode());
        }

        public Builder<S> subCommand(String name, CommandUsage.Builder<S> mainUsage, @Nullable ParseElement<?> annotatedElement) {
            return subCommand(name, mainUsage, cmd.imperat().config().getDefaultAttachmentMode(), annotatedElement);
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
            cmd.parent(parentCmd);
            return this;
        }


        public Builder<S> setMetaPropertiesFromOtherCommand(Command<S> other) {
            cmd.setPermissionData(other.getPermissionsData());
            cmd.setDefaultUsage(other.getDefaultUsage());
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
