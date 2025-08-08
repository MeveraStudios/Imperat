package studio.mevera.imperat.selector;

import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.exception.selector.InvalidSelectorFieldCriteriaFormat;
import studio.mevera.imperat.exception.selector.UnknownSelectorFieldException;
import studio.mevera.imperat.selector.field.SelectionField;
import studio.mevera.imperat.selector.field.provider.FieldProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SelectionParameterInput<V> {

    private final SelectionField<V> field;
    private final V value;

    private SelectionParameterInput(SelectionField<V> field, String input, Context<BukkitSource> ctx) throws ImperatException {
        this.field = field;
        this.value = field.parseFieldValue(input, ctx);
    }

    public static <V> SelectionParameterInput<V> from(SelectionField<V> field, String input, Context<BukkitSource> ctx) throws ImperatException {
        return new SelectionParameterInput<>(field, input, ctx);
    }

    public SelectionField<V> getField() {
        return field;
    }

    public V getValue() {
        return value;
    }

    public static SelectionParameterInput<?> parse(String str, CommandInputStream<BukkitSource> commandInputStream, Context<BukkitSource> ctx) throws ImperatException {
        String[] split = str.split(String.valueOf(SelectionField.VALUE_EQUALS));
        if (split.length != 2) {
            throw new InvalidSelectorFieldCriteriaFormat(str, commandInputStream.readInput(), ctx);
        }
        String field = split[0], value = split[1];
        SelectionField<?> selectionField = FieldProvider.INSTANCE.provideField(field, commandInputStream);
        if (selectionField == null) {
            throw new UnknownSelectorFieldException(field, commandInputStream.currentRaw().orElseThrow(), ctx);
        }

        return new SelectionParameterInput<>(selectionField, value, ctx);
    }


    public static List<SelectionParameterInput<?>> parseAll(String paramsString, CommandInputStream<BukkitSource> inputStream, Context<BukkitSource> ctx) throws ImperatException {
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
}
