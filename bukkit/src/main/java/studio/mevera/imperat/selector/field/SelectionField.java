package studio.mevera.imperat.selector.field;

import studio.mevera.imperat.exception.ImperatException;

import java.lang.reflect.Type;
import java.util.List;

//selection field for mojang entity/target-selectors in minecraft

/**
 * The SelectionField interface defines methods for handling selection fields
 * with generic types. It provides functionalities to get the type of the value
 * and to parse a string representation into the field's value type.
 *
 * @param <V> The type of the value that the selection field handles.
 */
public interface SelectionField<V> extends SelectionFields {
    char VALUE_EQUALS = '=', SEPARATOR = ',';

    /**
     * Retrieves the name of the selection field.
     *
     * @return The name of the selection field.
     */
    String getName();

    /**
     * Retrieves the type of the value handled by the selection field.
     *
     * @return The type information wrapped in a TypeWrap object, which represents
     * the value type of the selection field.
     */
    Type getValueType();

    /**
     * Parses the given string representation of the value and converts it into the field's value type.
     *
     * @param value the string representation of the value to be parsed
     * @return the parsed value of the field's type
     * @throws ImperatException if the parsing fails
     */
    V parseFieldValue(String value) throws ImperatException;


    /**
     * Retrieves a list of suggestions related to this selection field's value
     *
     * @return A list of suggestion strings.
     */
    List<String> getSuggestions();
}
