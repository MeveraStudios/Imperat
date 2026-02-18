package studio.mevera.imperat.selector;

import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.responses.BukkitResponseKey;
import studio.mevera.imperat.selector.field.SelectionField;
import studio.mevera.imperat.selector.field.provider.FieldProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SelectionParameterInput<V> {

    private final SelectionField<V> field;
    private final V value;

    private SelectionParameterInput(SelectionField<V> field, String input, Context<BukkitSource> ctx) throws CommandException {
        this.field = field;
        this.value = field.parseFieldValue(input, ctx);
    }

    public static <V> SelectionParameterInput<V> from(SelectionField<V> field, String input, Context<BukkitSource> ctx) throws CommandException {
        return new SelectionParameterInput<>(field, input, ctx);
    }

    public static SelectionParameterInput<?> parse(String str, Cursor<BukkitSource> cursor, Context<BukkitSource> ctx) throws
            CommandException {
        String[] split = str.split(String.valueOf(SelectionField.VALUE_EQUALS));
        if (split.length != 2) {
            throw new CommandException(BukkitResponseKey.INVALID_SELECTOR_FIELD)
                          .withPlaceholder("fieldCriteriaInput", str)
                          .withPlaceholder("input", cursor.readInput());
        }
        String field = split[0], value = split[1];
        SelectionField<?> selectionField = FieldProvider.INSTANCE.provideField(field, cursor);
        if (selectionField == null) {
            throw new CommandException(BukkitResponseKey.UNKNOWN_SELECTOR_FIELD)
                          .withPlaceholder("fieldEntered", field)
                          .withPlaceholder("input", cursor.currentRaw().orElseThrow());
        }

        return new SelectionParameterInput<>(selectionField, value, ctx);
    }

    public static List<SelectionParameterInput<?>> parseAll(String paramsString, Cursor<BukkitSource> inputStream,
            Context<BukkitSource> ctx) throws
            CommandException {
        String[] params = paramsString.split(String.valueOf(SelectionField.SEPARATOR));
        if (params.length == 0) {
            return Collections.emptyList();
        }
        List<SelectionParameterInput<?>> list = new ArrayList<>();
        for (String str : params) {
            SelectionParameterInput<?> from = parse(str, inputStream, ctx);
            list.add(from);
        }
        return list;
    }

    public SelectionField<V> getField() {
        return field;
    }

    public V getValue() {
        return value;
    }
}
