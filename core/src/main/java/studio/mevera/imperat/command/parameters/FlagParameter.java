package studio.mevera.imperat.command.parameters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.resolvers.SuggestionResolver;

import java.lang.reflect.Type;

public interface FlagParameter<S extends Source> extends CommandParameter<S> {

    /**
     * @return The flag's data
     */
    @NotNull
    FlagData<S> flagData();

    /**
     * @return The valueType of input value
     */
    default Type inputValueType() {
        var type = flagData().inputType();
        if (type == null)
            return Boolean.class;
        return type.type();
    }

    /**
     * @return the {@link SuggestionResolver} for input value of this flag
     * null if the flag is switch, check using {@link FlagParameter#isSwitch()}
     */
    @Nullable
    SuggestionResolver<S> inputSuggestionResolver();

    /**
     * @return checks whether this parameter is a flag
     */
    @Override
    default boolean isFlag() {
        return true;
    }

    default boolean isSwitch() {
        return flagData().inputType() == null;
    }

}
