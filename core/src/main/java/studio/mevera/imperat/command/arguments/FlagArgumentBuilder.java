package studio.mevera.imperat.command.arguments;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.command.arguments.type.ArgumentTypes;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.internal.ParsedFlagArgument;
import studio.mevera.imperat.providers.SuggestionProvider;

import java.util.ArrayList;
import java.util.List;

public final class FlagArgumentBuilder<S extends CommandSource, T> extends ArgumentBuilder<S, ParsedFlagArgument<S>> {

    private final ArgumentType<S, T> inputType;
    private final List<String> aliases = new ArrayList<>();
    private DefaultValueProvider defaultValueSupplier;
    private SuggestionProvider<S> suggestionProvider;

    private FlagArgumentBuilder(String name, @Nullable ArgumentType<S, T> inputType) {
        super(name, ArgumentTypes.flag(FlagData.create(name, List.of(), inputType)), true, false);
        this.inputType = inputType;
        this.defaultValueSupplier = inputType == null ? DefaultValueProvider.of("false") : inputType.getDefaultValueProvider();
    }

    //for switches
    private FlagArgumentBuilder(String name) {
        this(name, null);
    }

    public static <S extends CommandSource, T> FlagArgumentBuilder<S, T> ofFlag(String name, ArgumentType<S, T> inputType) {
        return new FlagArgumentBuilder<>(name, inputType);
    }

    public static <S extends CommandSource, T> FlagArgumentBuilder<S, T> ofSwitch(String name) {
        return new FlagArgumentBuilder<>(name);
    }

    public FlagArgumentBuilder<S, T> aliases(List<String> aliases) {
        this.aliases.addAll(aliases);
        return this;
    }

    public FlagArgumentBuilder<S, T> aliases(String... aliases) {
        this.aliases.addAll(List.of(aliases));
        return this;
    }

    public FlagArgumentBuilder<S, T> flagDefaultInputValue(DefaultValueProvider valueSupplier) {
        if (inputType == null) {
            throw new IllegalArgumentException("Flag of valueType switches, cannot have a default value supplier !");
        }
        this.defaultValueSupplier = valueSupplier;
        return this;
    }

    public FlagArgumentBuilder<S, T> suggestForInputValue(SuggestionProvider<S> suggestionProvider) {
        if (inputType == null) {
            throw new IllegalArgumentException("Flag of valueType switches, cannot have a default value supplier !");
        }
        this.suggestionProvider = suggestionProvider;
        return this;
    }


    @Override
    public FlagArgument<S> build() {
        FlagData<S> flag = FlagData.create(name, aliases, inputType);
        if (inputType == null) {
            defaultValueSupplier = DefaultValueProvider.of("false");
        }
        return new FlagArgumentImpl<>(flag, permission, description, defaultValueSupplier, suggestionProvider);
    }

}
