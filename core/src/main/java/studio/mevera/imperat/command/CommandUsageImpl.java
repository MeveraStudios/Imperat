package studio.mevera.imperat.command;

import static studio.mevera.imperat.util.Patterns.DOUBLE_FLAG;
import static studio.mevera.imperat.util.Patterns.SINGLE_FLAG;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.command.cooldown.CooldownHandler;
import studio.mevera.imperat.command.cooldown.UsageCooldown;
import studio.mevera.imperat.command.flags.FlagExtractor;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.FlagParameter;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.util.Patterns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

@ApiStatus.Internal
final class CommandUsageImpl<S extends Source> implements CommandUsage<S> {

    private final static int EXPECTED_PARAMETERS_CAPACITY = 8;
    
    private final List<CommandParameter<S>> parameters = new ArrayList<>(EXPECTED_PARAMETERS_CAPACITY);
    private final @NotNull CommandExecution<S> execution;
    private final boolean help;
    private final Set<String> permissions = new HashSet<>(CommandImpl.INITIAL_PERMISSIONS_CAPACITY);
    private Description description = Description.of("N/A");
    private @NotNull CooldownHandler<S> cooldownHandler;
    private @Nullable UsageCooldown cooldown = null;
    private CommandCoordinator<S> commandCoordinator ;
    private final Map<String, FlagExtractor<S>> subFlagExtractors = new HashMap<>(); // each subcommand have single flag registry/extractor
    private final FlagExtractor<S> flagExtractor;

    private final List<String> examples = new ArrayList<>(2);

    CommandUsageImpl(@NotNull CommandExecution<S> execution) {
        this(execution, false);
    }

    CommandUsageImpl(@NotNull CommandExecution<S> execution, boolean help) {
        this.execution = execution;
        this.cooldownHandler = CooldownHandler.createDefault(this);
        this.commandCoordinator = null;
        this.help = help;
        this.flagExtractor = FlagExtractor.createNative(this);
    }

    @Override
    public FlagExtractor<S> getSubCommandFlagExtractor(String subCommandName) {
        return subFlagExtractors.computeIfAbsent(subCommandName, k -> FlagExtractor.createNative(this));
    }

    @Override
    public void setSubCommandFlagExtractor(String subCommandName, FlagExtractor<S> extractor) {
        this.subFlagExtractors.put(subCommandName, extractor);
    }

    /**
     * @return the permission for this usage
     */
    @Override
    public @Unmodifiable Set<String> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    /**
     * The permission for this usage
     *
     * @param permission permission to set
     */
    @Override
    public void addPermission(String permission) {
        this.permissions.add(permission);
    }

    /**
     * @return the description for the
     * command usage
     */
    @Override
    public Description description() {
        return description;
    }

    @Override
    public void describe(Description description) {
        this.description = description;
    }

    @Override
    public @NotNull FlagExtractor<S> getFlagExtractor() {
        return flagExtractor;
    }

    /**
     * Checks whether the raw input is a flag
     * registered by this usage
     *
     * @param input the raw input
     * @return Whether the input is a flag and is registered in the usage
     */
    @Override
    public boolean hasFlag(String input) {
        return getFlagParameterFromRaw(input) != null;
    }

    /**
     * Fetches the flag from the input
     *
     * @param rawInput the input
     * @return the flag from the raw input, null if it cannot be a flag
     */
    @Override
    public @Nullable FlagData<S> getFlagParameterFromRaw(String rawInput) {

        String raw = rawInput;
        if (Patterns.isInputFlag(rawInput)) {
            boolean isSingle = SINGLE_FLAG.matcher(rawInput).matches();
            boolean isDouble = DOUBLE_FLAG.matcher(rawInput).matches();
            int offset = 0;
            if(isSingle) {
                offset = 1;
            }else if(isDouble) {
                offset = 2;
            }
            raw = rawInput.substring(offset);
        }

        for (var param : parameters) {
            if (!param.isFlag()) continue;
            FlagData<S> flag = param.asFlagParameter().flagData();
            if (flag.acceptsInput(raw)) {
                return flag;
            }
        }
        return null;
    }

    @Override
    public void addFlag(FlagParameter<S> flagData) {
        flagExtractor.insertFlag(flagData);
    }


    /**
     * Adds parameters to the usage
     *
     * @param params the parameters to add
     */
    @SafeVarargs
    @Override
    public final void addParameters(CommandParameter<S>... params) {
        addParameters(Arrays.asList(params));
    }

    /**
     * Adds parameters to the usage
     *
     * @param params the parameters to add
     */
    @Override
    public void addParameters(List<CommandParameter<S>> params) {
        for (var param : params) {
            if (param.isFlag()) {
                addFlag(param.asFlagParameter());
            }else {
                parameters.add(param);
            }
        }
    }

    /**
     * @return the parameters for this usage
     * @see CommandParameter
     */
    @Override
    public List<CommandParameter<S>> getParameters() {
        return parameters;
    }

    @Override
    public List<String> getExamples() {
        return examples;
    }
    
    @Override
    public void addExample(String example) {
        if(examples.contains(example))return;
        examples.add(example);
    }
    
    @Override
    public @Nullable CommandParameter<S> getParameter(int index) {
        if (index < 0 || index >= parameters.size()) return null;
        return parameters.get(index);
    }

    /**
     * @return the execution for this command
     */
    @Override
    public @NotNull CommandExecution<S> getExecution() {
        return execution;
    }

    /**
     * @param clazz the valueType of the parameter to check upon
     * @return Whether the usage has a specific valueType of parameter
     */
    @Override
    public boolean hasParamType(Class<?> clazz) {
        return getParameters()
            .stream()
            .anyMatch((param) -> param.valueType().equals(clazz));
    }

    /**
     * @return Gets the minimal possible number
     * of parameters that are acceptable to initiate this
     * usage of a command.
     */
    @Override
    public int getMinLength() {
        return (int) getParameters().stream()
            .filter((param) -> !param.isFlag())
            .filter((param) -> !param.isOptional())
            .count();
    }

    /**
     * @return Gets the maximum possible number
     * of parameters that are acceptable to initiate this
     * usage of a command.
     */
    @Override
    public int getMaxLength() {
        return parameters.size()+flagExtractor.getRegisteredFlags().size();
    }

    /**
     * Searches for a parameter with specific valueType
     *
     * @param parameterPredicate the parameter condition
     * @return whether this usage has atLeast on {@link CommandParameter} with specific condition
     * or not
     */
    @Override
    public boolean hasParameters(Predicate<CommandParameter<S>> parameterPredicate) {
        for (CommandParameter<S> parameter : getParameters())
            if (parameterPredicate.test(parameter))
                return true;

        return false;
    }

    /**
     * @param parameterPredicate the condition
     * @return the parameter to get using a condition
     */
    @Override
    public @Nullable CommandParameter<S> getParameter(Predicate<CommandParameter<S>> parameterPredicate) {
        for (CommandParameter<S> parameter : getParameters()) {
            if (parameterPredicate.test(parameter))
                return parameter;
        }
        return null;
    }

    /**
     * @return The cooldown for this usage {@link UsageCooldown}
     * returns null if no cooldown has been set
     */
    @Override
    public @Nullable UsageCooldown getCooldown() {
        return cooldown;
    }

    /**
     * Sets the command usage's cooldown {@link UsageCooldown}
     *
     * @param usageCooldown the cool down for this usage
     */
    @Override
    public void setCooldown(@Nullable UsageCooldown usageCooldown) {
        this.cooldown = usageCooldown;
    }

    /**
     * @return the cool down handler {@link CooldownHandler}
     */
    @Override
    public @NotNull CooldownHandler<S> getCooldownHandler() {
        return cooldownHandler;
    }

    /**
     * Sets the cooldown handler {@link CooldownHandler}
     *
     * @param cooldownHandler the cool down handler to set
     */
    @Override
    public void setCooldownHandler(@NotNull CooldownHandler<S> cooldownHandler) {
        this.cooldownHandler = cooldownHandler;
    }

    /**
     * @return the coordinator for execution of the command
     */
    @Override
    public CommandCoordinator<S> getCoordinator() {
        return commandCoordinator;
    }

    /**
     * Sets the command coordinator
     *
     * @param commandCoordinator the coordinator to set
     */
    @Override
    public void setCoordinator(CommandCoordinator<S> commandCoordinator) {
        this.commandCoordinator = commandCoordinator;
    }

    /**
     * Executes the usage's actions
     * using the supplied {@link CommandCoordinator}
     *
     * @param imperat the api
     * @param source  the command source/sender
     * @param context the context of the command
     */
    @Override
    public void execute(Imperat<S> imperat, S source, ExecutionContext<S> context) throws CommandException {
        CommandCoordinator<S> coordinator = commandCoordinator;
        if(coordinator == null) {
            coordinator = imperat.config().getGlobalCommandCoordinator();
        }
        coordinator.coordinate(imperat, source, context, this.execution);
    }

    @Override
    public boolean isHelp() {
        return help;
    }

    @Override
    public boolean hasParameters(List<CommandParameter<S>> parameters) {

        if(this.parameters.size() != parameters.size())return false;

        for (int i = 0; i < parameters.size(); i++) {
            CommandParameter<S> thisParam = this.parameters.get(i);
            CommandParameter<S> otherParam = parameters.get(i);
            if(!thisParam.similarTo(otherParam)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public @NotNull List<CommandParameter<S>> loadCombinedParameters() {

        var combinedParameters = new ArrayList<>(parameters);
        int start = parameters.size();
        var lastParam = getLastParam();
        if(lastParam != null && lastParam.isGreedy()) {
            start = parameters.size() - 1;
        }

        for (var flagParam : flagExtractor.getRegisteredFlags()) {
            flagParam.position(start);
            combinedParameters.add(start, flagParam);
            start++;
        }
        return combinedParameters;
    }

    @Override
    public @NotNull Iterator<CommandParameter<S>> iterator() {
        return parameters.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommandUsageImpl<?> that = (CommandUsageImpl<?>) o;
        if (this.size() != that.size()) return false;
        for (int i = 0; i < this.size(); i++) {
            var thisP = this.getParameter(i);
            var thatP = that.getParameter(i);
            assert thisP != null;
            if (!thisP.similarTo(thatP)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameters);
    }
    

}
