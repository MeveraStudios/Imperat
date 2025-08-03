package studio.mevera.imperat.selector.field;

import studio.mevera.imperat.util.TypeWrap;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * AbstractField is an abstract base class that implements the SelectionField
 * interface. It provides a skeletal implementation of the SelectionField
 * interface to minimize the effort required to implement this interface.
 *
 * @param <V> The type of the value that this field handles.
 */
public abstract class AbstractField<V> implements SelectionField<V> {

    /**
     * The name of the selection field.
     */
    protected final String name;
    /**
     * Represents the type information of the value that this field handles.
     * This is a wrapped type of the value that supports various operations
     * related to type management in this field.
     */
    protected final Type type;


    /**
     * A list that holds suggestion strings related to the selection field's value.
     * This list can be used to provide autocomplete or assistive suggestions for the user.
     */
    protected final List<String> suggestions = new ArrayList<>();

    /**
     * Constructs an AbstractField instance with the specified name and type.
     *
     * @param name The name of the selection field.
     * @param type The type information of the value that this field handles, wrapped in a TypeWrap object.
     */
    protected AbstractField(
        String name,
        TypeWrap<V> type
    ) {
        this.name = name;
        this.type = type.getType();
    }

    /**
     * Retrieves the name of the selection field.
     *
     * @return The name of the selection field.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Retrieves the type of the value handled by the selection field.
     *
     * @return The type information wrapped in a TypeWrap object, which represents
     * the value type of the selection field.
     */
    @Override
    public Type getValueType() {
        return type;
    }

    /**
     * Retrieves a list of suggestions related to this selection field's value
     *
     * @return A list of suggestion strings.
     */
    @Override
    public List<String> getSuggestions() {
        return suggestions;
    }
}
