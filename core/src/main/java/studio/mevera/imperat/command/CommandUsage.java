package studio.mevera.imperat.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.command.cooldown.CooldownHandler;
import studio.mevera.imperat.command.cooldown.UsageCooldown;
import studio.mevera.imperat.command.flags.FlagExtractor;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.FlagParameter;
import studio.mevera.imperat.command.parameters.ParameterBuilder;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.UnknownFlagException;
import studio.mevera.imperat.util.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Represents a usage of a command
 * that can be used in the future during an execution
 *
 * @see Command
 */
public sealed interface CommandUsage<S extends Source> extends Iterable<CommandParameter<S>>, PermissionHolder, DescriptionHolder, CooldownHolder  permits CommandUsageImpl{

    /**
     * Retrieves the flag extractor instance for parsing command flags from input strings.
     *
     * <p>The returned {@link FlagExtractor} is capable of parsing flag aliases from compact
     * string representations (e.g., "-abc", "alphaby") and resolving them to their
     * corresponding {@link FlagData} objects based on the command's usage configuration.
     *
     * <p>The extractor uses a greedy longest-match algorithm to handle overlapping aliases
     * efficiently. For example, if both "a" and "alpha" are valid aliases for the same flag,
     * the input "alpha" will match the longer alias rather than just "a".
     *
     * <p><strong>Usage Examples:</strong>
     * <pre>{@code
     * // Given flags: alpha["a", "alfa"], beta["b"], gamma["g", "gam"]
     * FlagExtractor<MySource> extractor = command.getFlagExtractor();
     *
     * Set<FlagData<MySource>> flags1 = extractor.extract("ab");     // alpha, beta
     * Set<FlagData<MySource>> flags2 = extractor.extract("alphag"); // alpha, gamma
     * Set<FlagData<MySource>> flags3 = extractor.extract("abx");    // throws UnknownFlagException
     * }</pre>
     *
     * <p><strong>Error Handling:</strong>
     * The extractor will throw an {@link UnknownFlagException} if the input contains
     * any characters that cannot be matched to known flag aliases.
     *
     * <p><strong>Thread Safety:</strong>
     * The returned extractor instance is thread-safe for concurrent read operations
     * but should not be used across different command contexts.
     *
     * @return a non-null flag extractor configured for this command's flag definitions
     * @throws IllegalStateException if the command usage has not been properly initialized
     *                               or if no flag definitions are available
     * @see FlagParameter
     * @since 1.9.6
     */
    @NotNull FlagExtractor<S> getFlagExtractor();

    /**
     * Checks whether the raw input is a flag
     * registered by this usage
     *
     * @param input the raw input
     * @return Whether the input is a flag and is registered in the usage
     */
    boolean hasFlag(String input);

    /**
     * Fetches the flag from the input
     *
     * @param rawInput the input
     * @return the flag from the raw input, null if it cannot be a flag
     */
    @Nullable
    FlagData<S> getFlagParameterFromRaw(String rawInput);

    default void addFlag(CommandParameter<S> flagParam) {
        addFlag(flagParam.asFlagParameter());
    }

    /**
     * Adds a free flag to the usage
     * @param flagData adds a free flag to the usage
     */
    void addFlag(FlagParameter<S> flagData);

    /**
     * Adds parameters to the usage
     *
     * @param params the parameters to add
     */
    void addParameters(CommandParameter<S>... params);

    /**
     * Adds parameters to the usage
     *
     * @param params the parameters to add
     */
    void addParameters(List<CommandParameter<S>> params);

    /**
     * @return the parameters for this usage
     * @see CommandParameter
     */
    List<CommandParameter<S>> getParameters();

    /**
     * The pre-defined syntax examples for this usage.
     * @return the pre-defined examples for this {@link CommandUsage}
     */
    List<String> getExamples();
    
    /**
     * Adds an example to the usage
     * @param example the example to add using this usage.
     */
    void addExample(String example);
    
    default void addExamples(List<String> examples) {
        examples.forEach(this::addExample);
    }
    
    /**
     * Fetches the parameter at the index
     *
     * @param index the index of the parameter
     * @return the parameter at specified index/position
     */
    @Nullable
    CommandParameter<S> getParameter(int index);

    /**
     * @return the execution for this usage
     */
    @NotNull
    CommandExecution<S> getExecution();

    /**
     * Creates a new command usage instance to use it to Merge
     * this usage with the usage regarding parameters
     * and takes the targetToLoad's execution as well
     *
     * <p>
     * it also includes the subcommand's parameter itself
     * into the command's usage !
     * </p>
     *
     * @param subCommand the subcommand owning that usage
     * @param usage      the usage to merge with
     * @return the merged command usage!
     */
    default CommandUsage<S> mergeWithCommand(Command<S> subCommand, CommandUsage<S> usage) {
        List<CommandParameter<S>> comboParams = new ArrayList<>(this.getParameters());
        comboParams.add(subCommand);
        for (CommandParameter<S> param : usage.getParameters()) {
            if (this.hasParameters((p) -> p.equals(param))) {
                continue;
            }
            comboParams.add(param);
        }
        //comboParams.addAll(usage.getParameters());

        return CommandUsage.<S>builder()
            .coordinator(usage.getCoordinator())
            .description(subCommand.description().getValue())
            .cooldown(usage.getCooldown())
            .parameters(comboParams)
            .execute(usage.getExecution())
            .build(subCommand, usage.isHelp());
    }

    /**
     * @param clazz the valueType of the parameter to check upon
     * @return Whether the usage has a specific valueType of parameter
     */
    boolean hasParamType(Class<?> clazz);

    /**
     * @return Gets the minimal possible number
     * of parameters that are acceptable to initiate this
     * usage of a command.
     */
    int getMinLength();

    /**
     * @return Gets the maximum possible number
     * of parameters that are acceptable to initiate this
     * usage of a command.
     */
    int getMaxLength();

    /**
     * Searches for a parameter with specific valueType
     *
     * @param parameterPredicate the parameter condition
     * @return whether this usage has atLeast on {@link CommandParameter} with specific condition
     * or not
     */
    boolean hasParameters(Predicate<CommandParameter<S>> parameterPredicate);

    /**
     * @param parameterPredicate the condition
     * @return the parameter to get using a condition
     */
    @Nullable
    CommandParameter<S> getParameter(Predicate<CommandParameter<S>> parameterPredicate);

    /**
     * @return the cool down handler {@link CooldownHandler}
     */
    @NotNull
    CooldownHandler<S> getCooldownHandler();

    /**
     * Sets the cooldown handler {@link CooldownHandler}
     *
     * @param cooldownHandler the cool down handler to set
     */
    void setCooldownHandler(CooldownHandler<S> cooldownHandler);

    default boolean isDefault() {
        return getParameters().isEmpty() || getParameters().stream().noneMatch(CommandParameter::isRequired);
    }

    /**
     * @return the coordinator for execution of the command
     */
    CommandCoordinator<S> getCoordinator();

    /**
     * Sets the command coordinator
     *
     * @param commandCoordinator the coordinator to set
     */
    void setCoordinator(CommandCoordinator<S> commandCoordinator);

    /**
     * Executes the usage's actions
     * using the supplied {@link CommandCoordinator}
     *
     * @param imperat the api
     * @param source  the command source/sender
     * @param context the context of the command
     */
    void execute(Imperat<S> imperat, S source, ExecutionContext<S> context) throws CommandException;

    /**
     * @return Whether this usage is a help-subcommand usage
     */
    boolean isHelp();

    /**
     * @param parameters the parameters
     * @return whether this usage has this sequence of parameters
     */
    boolean hasParameters(List<CommandParameter<S>> parameters);
    
    default int size() {
        return getParameters().size();
    }

    default String formatted() {
        return format((String) null, this);
    }

    static <S extends Source> String format(@Nullable String label, CommandUsage<S> usage) {
        Preconditions.notNull(usage, "usage");
        StringBuilder builder = new StringBuilder(label == null ? "" : label);
        if(label != null) {
            builder.append(' ');
        }

        int i = 0;
        for (CommandParameter<S> parameter : usage.getParameters()) {
            builder.append(parameter.format());
            if (i != usage.getParameters().size() - 1) {
                builder.append(' ');
            }
            i++;
        }
        return builder.toString();
    }

    static <S extends Source> String format(@Nullable Command<S> command, CommandUsage<S> usage) {
        String label = command == null ? null : command.name();
        return format(label, usage);
    }

    static <S extends Source> Builder<S> builder() {
        return new Builder<>();
    }

    default CommandParameter<S> getLastParam() {
        return getParameter(getParameters().size() - 1);
    }

    class Builder<S extends Source> {

        private final List<CommandParameter<S>> parameters = new ArrayList<>();
        private CommandExecution<S> execution = CommandExecution.empty();
        private String description = "N/A";
        private String permission = null;
        private UsageCooldown cooldown = null;
        private CommandCoordinator<S> commandCoordinator = CommandCoordinator.sync();
        private final Set<FlagParameter<S>> flags = new HashSet<>();
        private final List<String> examples = new ArrayList<>(3);
        
        Builder() {

        }
        

        public Builder<S> coordinator(CommandCoordinator<S> commandCoordinator) {
            this.commandCoordinator = commandCoordinator;
            return this;
        }
        
        public Builder<S> examples(String... examples) {
            this.examples.addAll(Arrays.asList(examples));
            return this;
        }

        public Builder<S> execute(CommandExecution<S> execution) {
            this.execution = execution;
            return this;
        }

        public Builder<S> permission(String permission) {
            this.permission = permission;
            return this;
        }

        public Builder<S> cooldown(long value, TimeUnit unit) {
            return cooldown(value, unit, null);
        }

        public Builder<S> cooldown(long value, TimeUnit unit, @Nullable String permission) {
            this.cooldown = new UsageCooldown(value, unit, permission);
            return this;
        }

        public Builder<S> cooldown(@Nullable UsageCooldown cooldown) {
            this.cooldown = cooldown;
            return this;
        }

        public Builder<S> description(String description) {
            if (description != null) {
                this.description = description;
            }
            return this;
        }

        @SafeVarargs
        public final Builder<S> parameters(ParameterBuilder<S, ?>... builders) {
            return parameters(
                Arrays.stream(builders).map(ParameterBuilder::build).toList()
            );
        }
        
        public final Builder<S> parameterBuilders(List<? extends ParameterBuilder<S, ?>> builders) {
            return parameters(
                    builders.stream().map(ParameterBuilder::build).toList()
            );
        }

        @SafeVarargs
        public final Builder<S> parameters(CommandParameter<S>... params) {
            return parameters(List.of(params));
        }

        public Builder<S> parameters(List<CommandParameter<S>> params) {
            for (int i = 0; i < params.size(); i++) {
                CommandParameter<S> parameter = params.get(i);
                if (!parameter.isCommand()) {
                    parameter.position(i);
                }

                this.parameters.add(parameter);
            }
            return this;
        }

        public Builder<S> registerFlags(Set<FlagParameter<S>> flags) {
            this.flags.addAll(flags);
            return this;
        }
        
        public Builder<S> setPropertiesFromCommandMainUsage(Command<S> command) {
            CommandUsage<S> mainUsage = command.getMainUsage();
            
            //copy only meta properties
            this.description = mainUsage.description().getValue();
            this.permission = mainUsage.getPermissions().stream()
                    .findAny().orElse(null);
            
            this.execution = mainUsage.getExecution();
            this.cooldown = mainUsage.getCooldown();
            this.commandCoordinator = mainUsage.getCoordinator();
            this.execution = mainUsage.getExecution();
            
            return this;
        }

        public CommandUsage<S> build(@NotNull Command<S> command, boolean help) {
            CommandUsageImpl<S> impl = new CommandUsageImpl<>(execution, help);
            impl.setCoordinator(commandCoordinator);
            impl.addPermission(permission);
            impl.describe(description);
            impl.setCooldown(cooldown);
            impl.addParameters(
                parameters.stream().peek((p) -> p.parent(command)).toList()
            );
            flags.forEach(impl::addFlag);
            impl.addExamples(this.examples);
            return impl;
        }


        public CommandUsage<S> build(@NotNull Command<S> command) {
            return build(command, false);
        }

        public CommandUsage<S> buildAsHelp(@NotNull Command<S> command) {
            return build(command, true);
        }
    }
    

}
