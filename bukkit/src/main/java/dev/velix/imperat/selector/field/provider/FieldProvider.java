package dev.velix.imperat.selector.field.provider;

import dev.velix.imperat.BukkitSource;
import dev.velix.imperat.context.internal.CommandInputStream;
import dev.velix.imperat.selector.field.SelectionField;
import org.jetbrains.annotations.Nullable;

/**
 * <p>
 * The FieldProvider interface is designed to define a method for providing
 * selection fields based on their names. Implementations of this interface
 * are responsible for defining the logic to retrieve a field corresponding
 * to the provided name.
 * The purpose is to allow dynamic retrieval of fields, which may be used
 * for various purposes such as filtering or selecting specific data
 * attributes.
 * </p>
 */
public sealed interface FieldProvider permits FieldProviderImpl {

    FieldProvider INSTANCE = new FieldProviderImpl();

    /**
     * Provides a selection field based on the given name.
     *
     * @param <V>  The type of the value that the selection field handles.
     * @param name The name of the selection field to retrieve.
     * @return The selection field corresponding to the provided name, or null if no such field exists.
     */
    <V> @Nullable SelectionField<V> provideField(String name, CommandInputStream<BukkitSource> commandInputStream);
    //TODO sync this with the old criteria parsing system.

}
