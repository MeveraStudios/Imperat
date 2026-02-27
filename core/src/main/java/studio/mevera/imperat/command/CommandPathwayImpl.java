package studio.mevera.imperat.command;

import static studio.mevera.imperat.util.Patterns.DOUBLE_FLAG;
import static studio.mevera.imperat.util.Patterns.SINGLE_FLAG;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.annotations.base.element.MethodElement;
import studio.mevera.imperat.command.cooldown.CooldownHandler;
import studio.mevera.imperat.command.cooldown.CooldownRecord;
import studio.mevera.imperat.command.flags.FlagExtractor;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.FlagArgument;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.permissions.PermissionsData;
import studio.mevera.imperat.util.Patterns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@ApiStatus.Internal
final class CommandPathwayImpl<S extends Source> implements CommandPathway<S> {

    private final static int EXPECTED_PARAMETERS_CAPACITY = 8;

    private final List<Argument<S>> arguments = new ArrayList<>(EXPECTED_PARAMETERS_CAPACITY);

    private final @NotNull CommandExecution<S> execution;
    private final FlagExtractor<S> flagExtractor;
    private final List<String> examples = new ArrayList<>(2);
    private PermissionsData permissionsData = PermissionsData.empty();
    private Description description = Description.of("N/A");
    private @NotNull CooldownHandler<S> cooldownHandler;
    private CommandCoordinator<S> commandCoordinator;
    private final @Nullable MethodElement methodElement;
    private @Nullable CooldownRecord cooldown = null;


    CommandPathwayImpl(@Nullable MethodElement methodElement, @NotNull CommandExecution<S> execution) {
        this.methodElement = methodElement;
        this.execution = execution;
        this.cooldownHandler = CooldownHandler.createDefault(this);
        this.commandCoordinator = null;
        this.flagExtractor = FlagExtractor.createNative(this);
    }

    @Override
    public @Nullable MethodElement getMethodElement() {
        return methodElement;
    }

    @Override
    public Description getDescription() {
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

    @Override
    public boolean hasFlag(String input) {
        return getFlagParameterFromRaw(input) != null;
    }

    @Override
    public @Nullable FlagData<S> getFlagParameterFromRaw(String rawInput) {
        String raw = rawInput;
        if (Patterns.isInputFlag(rawInput)) {
            boolean isSingle = SINGLE_FLAG.matcher(rawInput).matches();
            boolean isDouble = DOUBLE_FLAG.matcher(rawInput).matches();
            int offset = 0;
            if (isSingle) {
                offset = 1;
            } else if (isDouble) {
                offset = 2;
            }
            raw = rawInput.substring(offset);
        }

        for (var param : arguments) {
            if (!param.isFlag()) {
                continue;
            }
            FlagData<S> flag = param.asFlagParameter().flagData();
            if (flag.acceptsInput(raw)) {
                return flag;
            }
        }
        return null;
    }

    @Override
    public void addFlag(FlagArgument<S> flagArgumentData) {
        flagExtractor.insertFlag(flagArgumentData);
    }

    @SafeVarargs
    @Override
    public final void addArguments(Argument<S>... params) {
        addArguments(Arrays.asList(params));
    }

    @Override
    public void addArguments(List<Argument<S>> params) {
        for (var param : params) {
            if (param.isFlag()) {
                addFlag(param.asFlagParameter());
            } else {
                arguments.add(param);
                if (param.isRequired()) {
                    this.permissionsData.append(param.getPermissionsData());
                }
            }
        }
    }

    /**
     * Returns PERSONAL parameters only - for TREE BUILDING.
     * Inherited parameters are resolved at execution time, not parsed from input.
     */
    @Override
    public List<Argument<S>> getArguments() {
        return arguments;
    }

    /**
     * Returns ALL parameters including inherited - for EXECUTION.
     */
    @Override
    public List<Argument<S>> getParametersWithFlags() {
        var combined = new ArrayList<>(arguments);

        int start = arguments.size();
        var lastParam = arguments.isEmpty() ? null : arguments.get(arguments.size() - 1);
        if (lastParam != null && lastParam.isGreedy()) {
            start = arguments.size() - 1;
        }

        for (var flagParam : flagExtractor.getRegisteredFlags()) {
            flagParam.setPosition(start);
            combined.add(start, flagParam);
            start++;
        }
        return combined;
    }

    @Override
    public boolean hasMatchingPartialSequence(List<Argument<S>> otherArgs) {

        List<Argument<S>> matching = new ArrayList<>();
        // Check if the otherArgs is a valid partial sequence of this pathway's arguments
        for (int i = 0; i < otherArgs.size(); i++) {
            if (i >= arguments.size()) {
                return false; // otherArgs is longer than this pathway's arguments
            }
            Argument<S> thisArg = arguments.get(i);
            Argument<S> otherArg = otherArgs.get(i);
            if (!thisArg.similarTo(otherArg)) {
                break; // Arguments at position i do not match
            }
            matching.add(thisArg);
        }

        return matching.size() <= arguments.size() && !matching.isEmpty();
    }

    @Override
    public List<String> getExamples() {
        return examples;
    }

    @Override
    public void addExample(String example) {
        if (examples.contains(example)) {
            return;
        }
        examples.add(example);
    }

    @Override
    public @Nullable Argument<S> getArgumentAt(int index) {
        if (index < 0 || index >= arguments.size()) {
            return null;
        }
        return arguments.get(index);
    }

    @Override
    public @NotNull CommandExecution<S> getExecution() {
        return execution;
    }

    @Override
    public boolean hasParamType(Class<?> clazz) {
        return arguments.stream()
                       .anyMatch((param) -> param.valueType().equals(clazz));
    }

    @Override
    public int getMinLength() {
        return (int) arguments.stream()
                             .filter((param) -> !param.isFlag())
                             .filter((param) -> !param.isOptional())
                             .count();
    }

    @Override
    public int getMaxLength() {
        return arguments.size() + flagExtractor.getRegisteredFlags().size();
    }

    @Override
    public boolean hasParameters(Predicate<Argument<S>> parameterPredicate) {
        for (Argument<S> parameter : arguments) {
            if (parameterPredicate.test(parameter)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @Nullable Argument<S> getArgumentAt(Predicate<Argument<S>> parameterPredicate) {
        for (Argument<S> parameter : arguments) {
            if (parameterPredicate.test(parameter)) {
                return parameter;
            }
        }
        return null;
    }

    @Override
    public @Nullable CooldownRecord getCooldown() {
        return cooldown;
    }

    @Override
    public void setCooldown(@Nullable CooldownRecord usageCooldown) {
        this.cooldown = usageCooldown;
    }

    @Override
    public @NotNull CooldownHandler<S> getCooldownHandler() {
        return cooldownHandler;
    }

    @Override
    public void setCooldownHandler(@NotNull CooldownHandler<S> cooldownHandler) {
        this.cooldownHandler = cooldownHandler;
    }

    @Override
    public CommandCoordinator<S> getCoordinator() {
        return commandCoordinator;
    }

    @Override
    public void setCoordinator(CommandCoordinator<S> commandCoordinator) {
        this.commandCoordinator = commandCoordinator;
    }

    @Override
    public void execute(Imperat<S> imperat, S source, ExecutionContext<S> context) throws CommandException {
        CommandCoordinator<S> coordinator = commandCoordinator;
        if (coordinator == null) {
            coordinator = imperat.config().getGlobalCommandCoordinator();
        }
        coordinator.coordinate(imperat, source, context, this.execution);
    }

    @Override
    public boolean hasParameters(List<Argument<S>> parameters) {
        if (this.arguments.size() != parameters.size()) {
            return false;
        }

        for (int i = 0; i < parameters.size(); i++) {
            Argument<S> thisParam = this.arguments.get(i);
            Argument<S> otherParam = parameters.get(i);
            if (!thisParam.similarTo(otherParam)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public @NotNull PermissionsData getPermissionsData() {
        return permissionsData;
    }

    @Override
    public void setPermissionData(@NotNull PermissionsData permission) {
        this.permissionsData = permission;
    }

    @Override
    public @NotNull Iterator<Argument<S>> iterator() {
        return arguments.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CommandPathwayImpl<?> that = (CommandPathwayImpl<?>) o;
        if (this.arguments.size() != that.arguments.size()) {
            return false;
        }
        for (int i = 0; i < this.arguments.size(); i++) {
            var thisP = this.arguments.get(i);
            var thatP = that.arguments.get(i);
            assert thisP != null;
            if (!thisP.similarTo(thatP)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(arguments);
    }
}