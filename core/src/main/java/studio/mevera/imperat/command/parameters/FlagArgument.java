package studio.mevera.imperat.command.parameters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.providers.SuggestionProvider;

import java.lang.reflect.Type;

public interface FlagArgument<S extends Source> extends Argument<S> {

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
        if (type == null) {
            return Boolean.class;
        }
        return type.type();
    }

    /**
     * @return the {@link SuggestionProvider} for input value of this flag
     * null if the flag is switch, check using {@link FlagArgument#isSwitch()}
     */
    @Nullable
    SuggestionProvider<S> inputSuggestionResolver();

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
