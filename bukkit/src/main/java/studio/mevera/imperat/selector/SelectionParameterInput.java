package studio.mevera.imperat.selector;

import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.responses.BukkitResponseKey;
import studio.mevera.imperat.selector.field.SelectionField;
import studio.mevera.imperat.selector.field.provider.FieldProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SelectionParameterInput<V> {

    private final SelectionField<V> field;
    private final V value;

    private SelectionParameterInput(SelectionField<V> field, String input, CommandContext<BukkitCommandSource> ctx) throws CommandException {
        this.field = field;
        this.value = field.parseFieldValue(input, ctx);
    }

    public static <V> SelectionParameterInput<V> from(SelectionField<V> field, String input, CommandContext<BukkitCommandSource> ctx)
            throws CommandException {
        return new SelectionParameterInput<>(field, input, ctx);
    }

    /**
     * Parses a single {@code key=value} expression for a selector parameter
     * block (e.g. {@code tag=foo} from {@code @a[tag=foo,distance=5]}).
     *
     * @param expression  the {@code key=value} expression as collected from the
     *                    selector parameter block
     * @param sourceInput the full original selector input (used for error
     *                    reporting only)
     * @param ctx         the command context
     */
    public static SelectionParameterInput<?> parse(
            String expression,
            String sourceInput,
            CommandContext<BukkitCommandSource> ctx
    ) throws CommandException {
        String[] split = expression.split(String.valueOf(SelectionField.VALUE_EQUALS));
        if (split.length != 2) {
            throw ResponseException.of(BukkitResponseKey.INVALID_SELECTOR_FIELD)
                          .withPlaceholder("criteria_expression", expression)
                          .withPlaceholder("input", sourceInput);
        }
        String field = split[0], value = split[1];
        SelectionField<?> selectionField = FieldProvider.INSTANCE.provideField(field);
        if (selectionField == null) {
            throw ResponseException.of(BukkitResponseKey.UNKNOWN_SELECTOR_FIELD)
                          .withPlaceholder("field_entered", field)
                          .withPlaceholder("input", sourceInput);
        }

        return new SelectionParameterInput<>(selectionField, value, ctx);
    }

    /**
     * Parses every comma-separated {@code key=value} expression inside a
     * selector parameter block.
     *
     * @param paramsString the joined parameter block contents (without the
     *                     surrounding {@code [} / {@code ]})
     * @param sourceInput  the full original selector input, threaded through
     *                     for error reporting
     * @param ctx          the command context
     */
    public static List<SelectionParameterInput<?>> parseAll(
            String paramsString,
            String sourceInput,
            CommandContext<BukkitCommandSource> ctx
    ) throws CommandException {
        String[] params = paramsString.split(String.valueOf(SelectionField.SEPARATOR));
        if (params.length == 0) {
            return Collections.emptyList();
        }
        List<SelectionParameterInput<?>> list = new ArrayList<>();
        for (String param : params) {
            SelectionParameterInput<?> from = parse(param, sourceInput, ctx);
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
