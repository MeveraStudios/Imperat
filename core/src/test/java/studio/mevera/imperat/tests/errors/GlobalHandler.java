package studio.mevera.imperat.tests.errors;

import studio.mevera.imperat.annotations.ExceptionHandler;
import studio.mevera.imperat.command.parameters.NumericRange;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.exception.NumberOutOfRangeException;
import studio.mevera.imperat.exception.PermissionDeniedException;
import studio.mevera.imperat.tests.TestSource;

public final class GlobalHandler {

    @ExceptionHandler(PermissionDeniedException.class)
    public void globalHandler(PermissionDeniedException ex, Context<TestSource> ctx) {
        var src = ctx.source();
        String lackingPermission = ex.getLackingPermission();
        src.reply("You lack the permission '" + lackingPermission + "' to do this!");
    }

    @ExceptionHandler(NumberOutOfRangeException.class)
    public void handleNumberOutOfRange(NumberOutOfRangeException ex, Context<TestSource> ctx) {
        NumericRange range = ex.getRange();
        final StringBuilder builder = new StringBuilder();
        if (range.getMin() != Double.MIN_VALUE && range.getMax() != Double.MAX_VALUE) {
            builder.append("within ").append(range.getMin()).append('-').append(range.getMax());
        } else if (range.getMin() != Double.MIN_VALUE) {
            builder.append("at least '").append(range.getMin()).append("'");
        } else if (range.getMax() != Double.MAX_VALUE) {
            builder.append("at most '").append(range.getMax()).append("'");
        } else {
            builder.append("(Open range)");
        }

        String rangeFormatted = builder.toString();
        ctx.source().reply(
                "Value '" + ex.getValue() + "' entered for parameter '"
                        + ex.getParameter().format() + "' must be " + rangeFormatted
        );
    }
}
