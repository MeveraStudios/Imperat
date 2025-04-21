package dev.velix.imperat.command;

import dev.velix.imperat.FlagRegistrar;
import dev.velix.imperat.Imperat;
import dev.velix.imperat.command.parameters.CommandParameter;
import dev.velix.imperat.command.parameters.OptionalValueSupplier;
import dev.velix.imperat.command.parameters.type.ParameterType;
import dev.velix.imperat.command.parameters.type.ParameterTypes;
import dev.velix.imperat.command.processors.CommandPostProcessor;
import dev.velix.imperat.command.processors.CommandPreProcessor;
import dev.velix.imperat.command.suggestions.AutoCompleter;
import dev.velix.imperat.command.tree.CommandDispatch;
import dev.velix.imperat.command.tree.CommandTree;
import dev.velix.imperat.context.Context;
import dev.velix.imperat.context.ResolvedContext;
import dev.velix.imperat.context.Source;
import dev.velix.imperat.exception.ImperatException;
import dev.velix.imperat.help.HelpProvider;
import dev.velix.imperat.util.TypeWrap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

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
public interface Command<S extends Source> extends CommandParameter<S>, FlagRegistrar<S> {

    static <S extends Source> Command.Builder<S> create(String name) {
        return create(null, name);
    }

    static <S extends Source> Command.Builder<S> create(@Nullable Command<S> parent, @NotNull String name) {
        return create(parent, 0, name);
    }

    static <S extends Source> Command.Builder<S> create(
        @Nullable Command<S> parent,
        int position,
        @NotNull String name
    ) {
        return new Builder<>(parent, position, name);
    }

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
    CommandDispatch<S> contextMatch(Context<S> context);


    /**
     * @return The description of a command
     */
    @NotNull
    Description description();

    /**
     * Retrieves the parameter type associated with the current command,
     * including its name and any aliases.
     *
     * @return a ParameterType instance representing the command's parameter type,
     *         encapsulating its name and aliases
     */
    @Override
    default @NotNull ParameterType<S, ?> type() {
        return ParameterTypes.command(name(), aliases());
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
     * Retrieves the HelpProvider instance associated with the current context.
     *
     * @return the HelpProvider instance of type S
     */
    @Nullable HelpProvider<S> getHelpProvider();


    /**
     * Sets the help provider for the current context. The provided help provider can be used
     * to supply contextual help or assistance in various scenarios.
     *
     * @param helpProvider the help provider instance to set. Can be null to indicate
     *                     that no help provider is to be used.
     */
    void setHelpProvider(@Nullable HelpProvider<S> helpProvider);


    /**
     * Determines whether a help provider is available.
     *
     * @return true if a help provider is present, false otherwise
     */
    default boolean hasHelpProvider() {
        return getHelpProvider() != null;
    }

    /**
     * Sets a pre-processor for the command
     *
     * @param preProcessor the pre-processor for the command
     */
    void setPreProcessor(@NotNull CommandPreProcessor<S> preProcessor);

    /**
     * Executes the pre-processing instructions in {@link CommandPreProcessor}
     *
     * @param api     the api
     * @param context the context
     * @param usage   the usage detected being used
     */
    boolean preProcess(@NotNull Imperat<S> api, @NotNull Context<S> context, @NotNull CommandUsage<S> usage) throws ImperatException;

    /**
     * Sets a post-processor for the command
     *
     * @param postProcessor the post-processor for the command
     */
    void setPostProcessor(@NotNull CommandPostProcessor<S> postProcessor);

    /**
     * Executes the post-processing instructions in {@link CommandPostProcessor}
     *
     * @param api     the api
     * @param context the context
     * @param usage   the usage detected being used
     */
    boolean postProcess(@NotNull Imperat<S> api, @NotNull ResolvedContext<S> context, @NotNull CommandUsage<S> usage) throws ImperatException;

    /**
     * Retrieves a usage with no args for this command
     * @return A usage with empty parameters.
     */
    @NotNull
    CommandUsage<S> getEmptyUsage();

    /**
     * @return the default usage of the command
     * without any args
     */
    @NotNull
    CommandUsage<S> getDefaultUsage();


    /**
     * Sets the default command usage representation.
     *
     * @param usage the default command usage instance to be set, which must not be null
     */
    void setDefaultUsage(@NotNull CommandUsage<S> usage);

    /**
     * @param execution sets what happens when there are no parameters
     */
    void setDefaultUsageExecution(CommandExecution<S> execution);

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
     * Fetches the usage with specific sequence of parameters
     *
     * @param parameters the parameters
     * @return the usage from its sequence of parameters, null if no usage has such a sequence of parameters
     */
    @Nullable
    CommandUsage<S> getUsage(List<CommandParameter<S>> parameters);

    /**
     * @return All {@link CommandUsage} that were registered
     * to this command by the user
     */
    Collection<? extends CommandUsage<S>> usages();

    /**
     * @param predicate the criteria
     * @return a list of usages that match a certain criteria
     */
    Collection<? extends CommandUsage<S>> findUsages(Predicate<CommandUsage<S>> predicate);

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
        addSubCommandUsage(subCommand, aliases, usage, AttachmentMode.DEFAULT);
    }

    default void addSubCommandUsage(String subCommand,
                                    CommandUsage.Builder<S> usage,
                                    boolean attachDirectly) {
        addSubCommandUsage(subCommand, Collections.emptyList(), usage, AttachmentMode.DEFAULT);
    }

    /**
     * Creates and adds a new sub-command (if it doesn't exist) then add
     * the {@link CommandUsage} to the sub-command
     *
     * @param subCommand the sub-command's unique name
     * @param usage      the usage
     */
    default void addSubCommandUsage(String subCommand, CommandUsage.Builder<S> usage) {
        addSubCommandUsage(subCommand, usage, false);
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
    void ignoreACPermissions(boolean ignore);

    /**
     * Adds help as a sub-command to the command chain
     *
     * @param dispatcher    the api
     * @param params        the parameters of the help command
     * @param helpExecution the help execution
     */
    void addHelpCommand(Imperat<S> dispatcher,
                        List<CommandParameter<S>> params,
                        CommandExecution<S> helpExecution);


    class Builder<S extends Source> {

        private final Command<S> cmd;

        Builder(@Nullable Command<S> parent, int position, String name) {
            this.cmd = new CommandImpl<>(parent, position, name);
        }

        public Builder<S> ignoreACPermissions(boolean ignore) {
            this.cmd.ignoreACPermissions(ignore);
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
            return description(description.toString());
        }

        public Builder<S> permission(String permission) {
            this.cmd.permission(permission);
            return this;
        }

        public Builder<S> defaultExecution(CommandExecution<S> defaultExec) {
            cmd.setDefaultUsageExecution(defaultExec);
            return this;
        }

        public Builder<S> usage(CommandUsage.Builder<S> usage) {
            cmd.addUsage(usage);
            return this;
        }

        public Builder<S> helpProvider(HelpProvider<S> helpProvider) {
            cmd.setHelpProvider(helpProvider);
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
                Command.<S>create(name)
                    .usage(mainUsage)
                    .build(),
                attachmentMode
            );
        }

        public Builder<S> subCommand(String name, CommandUsage.Builder<S> mainUsage) {
            return subCommand(name, mainUsage, AttachmentMode.DEFAULT);
        }

        public Builder<S> preProcessor(CommandPreProcessor<S> preProcessor) {
            cmd.setPreProcessor(preProcessor);
            return this;
        }

        public Builder<S> postProcessor(CommandPostProcessor<S> postProcessor) {
            cmd.setPostProcessor(postProcessor);
            return this;
        }

        public Command<S> build() {
            return cmd;
        }

    }

}
