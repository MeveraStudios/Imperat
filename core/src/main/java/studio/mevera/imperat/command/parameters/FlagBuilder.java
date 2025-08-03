package studio.mevera.imperat.command.parameters;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.type.ParameterType;
import studio.mevera.imperat.command.parameters.type.ParameterTypes;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.ExtractedInputFlag;
import studio.mevera.imperat.resolvers.SuggestionResolver;

import java.util.ArrayList;
import java.util.List;

public final class FlagBuilder<S extends Source, T> extends ParameterBuilder<S, ExtractedInputFlag> {

    private final ParameterType<S, T> inputType;
    private final List<String> aliases = new ArrayList<>();
    private boolean free;
    private OptionalValueSupplier defaultValueSupplier;
    private SuggestionResolver<S> suggestionResolver;

    private FlagBuilder(String name, @Nullable ParameterType<S, T> inputType) {
        super(name, ParameterTypes.flag(FlagData.create(name, List.of(), inputType)), true, false);
        this.inputType = inputType;
        this.defaultValueSupplier = inputType == null ? OptionalValueSupplier.of("false") : inputType.supplyDefaultValue();
    }

    //for switches
    private FlagBuilder(String name) {
        this(name, null);
    }

    public static <S extends Source, T> FlagBuilder<S, T> ofFlag(String name, ParameterType<S, T> inputType) {
        return new FlagBuilder<>(name, inputType);
    }

    public static <S extends Source, T> FlagBuilder<S, T> ofSwitch(String name) {
        return new FlagBuilder<>(name);
    }

    public FlagBuilder<S, T> aliases(List<String> aliases) {
        this.aliases.addAll(aliases);
        return this;
    }

    public FlagBuilder<S, T> aliases(String... aliases) {
        this.aliases.addAll(List.of(aliases));
        return this;
    }

    public FlagBuilder<S, T> flagDefaultInputValue(OptionalValueSupplier valueSupplier) {
        if (inputType == null) {
            throw new IllegalArgumentException("Flag of valueType switches, cannot have a default value supplier !");
        }
        this.defaultValueSupplier = valueSupplier;
        return this;
    }

    public FlagBuilder<S, T> suggestForInputValue(SuggestionResolver<S> suggestionResolver) {
        if (inputType == null) {
            throw new IllegalArgumentException("Flag of valueType switches, cannot have a default value supplier !");
        }
        this.suggestionResolver = suggestionResolver;
        return this;
    }

    public FlagBuilder<S, T> setFree(boolean free) {
        this.free = free;
        return this;
    }


    @Override
    public FlagParameter<S> build() {
        FlagData<S> flag = FlagData.create(name, aliases, inputType, free);
        if (inputType == null) {
            defaultValueSupplier = OptionalValueSupplier.of("false");
        }
        return new FlagCommandParameter<>(flag, permission, description, defaultValueSupplier, suggestionResolver);
    }

}
