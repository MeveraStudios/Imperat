package studio.mevera.imperat.selector.field.provider;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.selector.field.SelectionField;

final class FieldProviderImpl implements FieldProvider {

    @Override
    @SuppressWarnings("unchecked")
    public <V> @Nullable SelectionField<V> provideField(String name, CommandInputStream<BukkitSource> commandInputStream) {
        for (var field : SelectionField.ALL) {
            if (field.getName().equalsIgnoreCase(name)) {
                return (SelectionField<V>) field;
            }
        }
        return null;
    }
}
