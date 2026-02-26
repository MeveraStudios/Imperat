package studio.mevera.imperat.command;

import static studio.mevera.imperat.util.Patterns.DOUBLE_FLAG;
import static studio.mevera.imperat.util.Patterns.SINGLE_FLAG;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.annotations.base.element.MethodElement;
import studio.mevera.imperat.command.cooldown.CooldownHandler;
import studio.mevera.imperat.command.cooldown.UsageCooldown;
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

    private final List<Argument<S>> personalParameters = new ArrayList<>(EXPECTED_PARAMETERS_CAPACITY);
    private final List<Argument<S>> allParametersView = new ArrayList<>(EXPECTED_PARAMETERS_CAPACITY);
    private List<Argument<S>> inheritedParameters = new ArrayList<>();

    private final @NotNull CommandExecution<S> execution;
    private final FlagExtractor<S> flagExtractor;
    private final List<String> examples = new ArrayList<>(2);
    private PermissionsData permissionsData = PermissionsData.empty();
    private Description description = Description.of("N/A");
    private @NotNull CooldownHandler<S> cooldownHandler;
    private @Nullable UsageCooldown cooldown = null;
    private CommandCoordinator<S> commandCoordinator;
    private final @Nullable MethodElement methodElement;
    private @Nullable CommandPathway<S> inheritedPathway = null;


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
    public @Nullable CommandPathway<S> getInheritedPathway() {
        return inheritedPathway;
    }

    @Override
    public void setInheritedPathway(@Nullable CommandPathway<S> inheritedPathway) {
        this.inheritedPathway = inheritedPathway;
    }

    /**
     * Sets inherited parameters separately. These are NOT part of the tree structure.
     */
    @Override
    public void setInheritedParameters(List<Argument<S>> inherited) {
        this.inheritedParameters = new ArrayList<>(inherited);
        rebuildAllParametersView();
    }

    /**
     * Sets personal parameters (parsed from user input). These ARE part of the tree structure.
     */
    @Override
    public void setPersonalParameters(List<Argument<S>> personal) {
        this.personalParameters.clear();
        this.personalParameters.addAll(personal);
        rebuildAllParametersView();
    }

    private void rebuildAllParametersView() {
        this.allParametersView.clear();
        this.allParametersView.addAll(inheritedParameters);
        this.allParametersView.addAll(personalParameters);
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

        for (var param : personalParameters) {
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
    public final void addParameters(Argument<S>... params) {
        addParameters(Arrays.asList(params));
    }

    @Override
    public void addParameters(List<Argument<S>> params) {
        for (var param : params) {
            if (param.isFlag()) {
                addFlag(param.asFlagParameter());
            } else {
                personalParameters.add(param);
                if (param.isRequired()) {
                    this.permissionsData.append(param.getPermissionsData());
                }
            }
        }
        rebuildAllParametersView();
    }

    /**
     * Returns PERSONAL parameters only - for TREE BUILDING.
     * Inherited parameters are resolved at execution time, not parsed from input.
     */
    @Override
    public List<Argument<S>> getArguments() {
        return personalParameters;
    }

    /**
     * Returns ALL parameters including inherited - for EXECUTION.
     */
    @Override
    public List<Argument<S>> loadCombinedParameters() {
        var combined = new ArrayList<>(allParametersView);

        int start = allParametersView.size();
        var lastParam = allParametersView.isEmpty() ? null : allParametersView.get(allParametersView.size() - 1);
        if (lastParam != null && lastParam.isGreedy()) {
            start = allParametersView.size() - 1;
        }

        for (var flagParam : flagExtractor.getRegisteredFlags()) {
            flagParam.setPosition(start);
            combined.add(start, flagParam);
            start++;
        }
        return combined;
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
        if (index < 0 || index >= personalParameters.size()) {
            return null;
        }
        return personalParameters.get(index);
    }

    @Override
    public @NotNull CommandExecution<S> getExecution() {
        return execution;
    }

    @Override
    public boolean hasParamType(Class<?> clazz) {
        return personalParameters.stream()
                       .anyMatch((param) -> param.valueType().equals(clazz));
    }

    @Override
    public int getMinLength() {
        return (int) personalParameters.stream()
                             .filter((param) -> !param.isFlag())
                             .filter((param) -> !param.isOptional())
                             .count();
    }

    @Override
    public int getMaxLength() {
        return personalParameters.size() + flagExtractor.getRegisteredFlags().size();
    }

    @Override
    public boolean hasParameters(Predicate<Argument<S>> parameterPredicate) {
        for (Argument<S> parameter : personalParameters) {
            if (parameterPredicate.test(parameter)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @Nullable Argument<S> getArgumentAt(Predicate<Argument<S>> parameterPredicate) {
        for (Argument<S> parameter : personalParameters) {
            if (parameterPredicate.test(parameter)) {
                return parameter;
            }
        }
        return null;
    }

    @Override
    public @Nullable UsageCooldown getCooldown() {
        return cooldown;
    }

    @Override
    public void setCooldown(@Nullable UsageCooldown usageCooldown) {
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
        if (this.personalParameters.size() != parameters.size()) {
            return false;
        }

        for (int i = 0; i < parameters.size(); i++) {
            Argument<S> thisParam = this.personalParameters.get(i);
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
        return personalParameters.iterator();
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
        if (this.personalParameters.size() != that.personalParameters.size()) {
            return false;
        }
        for (int i = 0; i < this.personalParameters.size(); i++) {
            var thisP = this.personalParameters.get(i);
            var thatP = that.personalParameters.get(i);
            assert thisP != null;
            if (!thisP.similarTo(thatP)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(personalParameters);
    }
}