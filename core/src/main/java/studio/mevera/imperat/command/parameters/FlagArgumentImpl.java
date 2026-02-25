package studio.mevera.imperat.command.parameters;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Description;
import studio.mevera.imperat.command.parameters.type.ArgumentTypes;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.permissions.PermissionsData;
import studio.mevera.imperat.providers.SuggestionProvider;

import java.util.Objects;

@ApiStatus.Internal
public final class FlagArgumentImpl<S extends Source> extends InputParameter<S> implements FlagArgument<S> {

    private final FlagData<S> flag;
    private final DefaultValueProvider inputValueSupplier;
    private final SuggestionProvider<S> inputValueSuggestionProvider;

    FlagArgumentImpl(
            FlagData<S> flag,
            PermissionsData permission,
            Description description,
            DefaultValueProvider inputValueSupplier,
            SuggestionProvider<S> inputValueSuggestionProvider
    ) {
        super(
                flag.name(), ArgumentTypes.flag(flag),
                permission, description,
                true, true, false,
                DefaultValueProvider.empty(),
                null
        );
        this.flag = flag;
        this.inputValueSupplier = inputValueSupplier;
        this.inputValueSuggestionProvider = inputValueSuggestionProvider;
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
    public @NotNull DefaultValueProvider getDefaultValueSupplier() {
        return inputValueSupplier;
    }

    @Override
    public @Nullable SuggestionProvider<S> inputSuggestionResolver() {
        if (isSwitch()) {
            return null;
        } else {
            return inputValueSuggestionProvider;
        }
    }

    @Override public boolean equals(Object o) {
        if (!(o instanceof FlagArgumentImpl<?> that)) {
            return false;
        }
        return Objects.equals(flag.name(), that.flag.name());
    }

    @Override public int hashCode() {
        return Objects.hash(flag.name());
    }
}
