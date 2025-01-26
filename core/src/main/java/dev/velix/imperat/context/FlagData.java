package dev.velix.imperat.context;

import dev.velix.imperat.command.parameters.type.ParameterType;
import dev.velix.imperat.util.StringUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents a flag that has been parsed/read/loaded from a
 * context of a command entered by the {@link Source}
 * <p>
 * A flag has merely 2 types:
 * 1) A true flag is a flag that has an input value next to it in the raw input
 * 2) A switch (flag that it's value input is represented by its mere presence
 */
@ApiStatus.AvailableSince("1.0.0")
public interface FlagData<S extends Source> {

    //TODO fix FLAGS STRUCTURE
    static <S extends Source, T> FlagData<S> create(String name, List<String> alias, ParameterType<S, T> inputType, boolean free) {
        return new FlagDataImpl<>(name, alias, inputType, free);
    }

    static <S extends Source, T> FlagData<S> create(String name, List<String> alias, ParameterType<S, T> inputType) {
        return create(name, alias, inputType, false);
    }

    //TODO fix FLAGS STRUCTURE
    static <S extends Source, T> FlagData<S> createSwitch(String name, List<String> aliases) {
        return create(name, aliases, null);
    }

    /**
     * The main name of the flag
     *
     * @return the name(unique) of the flag
     */
    @NotNull
    String name();

    /**
     * @return the alias of the flag
     */
    @NotNull
    List<String> aliases();

    /**
     * @return whether the flag is free
     */
    boolean isFree();

    /**
     * @return the valueType of input
     * from the flag
     */
    <T> ParameterType<S, T> inputType();

    default boolean hasAlias(String alias) {
        return aliases().contains(alias.toLowerCase());
    }

    default String format() {

        String display = this.name();
        String valueFormat = this.inputType() == null ? "" : " <value>";
        return StringUtils.normalizedParameterFormatting("-" + display
            + valueFormat, true);
    }

    default boolean acceptsInput(String input) {
        return this.name().equalsIgnoreCase(input) || hasAlias(input);
    }


    record FlagDataImpl<S extends Source>(String name, List<String> aliases, ParameterType<S, ?> inputType,
                                          boolean free) implements FlagData<S> {
        @Override
        public boolean isFree() {
            return free;
        }

    }

    default boolean isSwitch() {
        return inputType() == null;
    }

}
