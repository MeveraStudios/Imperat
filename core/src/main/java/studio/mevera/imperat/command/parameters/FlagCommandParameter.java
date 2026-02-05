package studio.mevera.imperat.command.parameters;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Description;
import studio.mevera.imperat.command.parameters.type.ParameterTypes;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.permissions.PermissionsData;
import studio.mevera.imperat.resolvers.SuggestionResolver;

import java.util.Objects;

@ApiStatus.Internal
public final class FlagCommandParameter<S extends Source> extends InputParameter<S> implements FlagParameter<S> {

    private final FlagData<S> flag;
    private final OptionalValueSupplier inputValueSupplier;
    private final SuggestionResolver<S> inputValueSuggestionResolver;

    FlagCommandParameter(
            FlagData<S> flag,
            PermissionsData permission,
            Description description,
            OptionalValueSupplier inputValueSupplier,
            SuggestionResolver<S> inputValueSuggestionResolver
    ) {
        super(
                flag.name(), ParameterTypes.flag(flag),
                permission, description,
                true, true, false,
                OptionalValueSupplier.empty(),
                null
        );
        this.flag = flag;
        this.inputValueSupplier = inputValueSupplier;
        this.inputValueSuggestionResolver = inputValueSuggestionResolver;
    }

    @Override
    public String format() {
        if (!this.format.equals(this.name)) {
            return super.format();
        }
        return flag.format();
    }

    /**
     * @return The flag's data
     */
    @Override
    public @NotNull FlagData<S> flagData() {
        return flag;
    }

    /**
     * @return the default value if it's input is not present
     * in case of the parameter being optional
     */
    @Override
    public @NotNull OptionalValueSupplier getDefaultValueSupplier() {
        return inputValueSupplier;
    }

    @Override
    public @Nullable SuggestionResolver<S> inputSuggestionResolver() {
        if (isSwitch()) {
            return null;
        } else {
            return inputValueSuggestionResolver;
        }
    }

    /**
     * Creates a copy of this parameter with a different position.
     * Useful for commands that have multiple syntaxes.
     *
     * @param newPosition the new position to set
     * @return a copy of this parameter with the new position
     */
    @Override
    public CommandParameter<S> copyWithDifferentPosition(int newPosition) {
        FlagCommandParameter<S> copy = new FlagCommandParameter<>(
                this.flag,
                this.permissionsData,
                this.description,
                this.inputValueSupplier,
                this.inputValueSuggestionResolver
        );
        copy.position(newPosition);
        copy.setFormat(this.format);
        return copy;
    }

    @Override public boolean equals(Object o) {
        if (!(o instanceof FlagCommandParameter<?> that)) {
            return false;
        }
        return Objects.equals(flag.name(), that.flag.name());
    }

    @Override public int hashCode() {
        return Objects.hash(flag.name());
    }
}
