package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.exception.parse.WordOutOfRestrictionsException;
import studio.mevera.imperat.util.Preconditions;

import java.util.ArrayList;
import java.util.List;

public final class ParameterWord<S extends Source> extends BaseParameterType<S, String> {

    private final List<String> restrictions = new ArrayList<>();

    ParameterWord() {
        super();
    }

    @Override
    public @NotNull String resolve(@NotNull ExecutionContext<S> context, @NotNull CommandInputStream<S> commandInputStream, @NotNull String input) throws ImperatException {
        if (restrictions.isEmpty()) {
            return input;
        }
        if(!restrictions.contains(input)) {
            throw new WordOutOfRestrictionsException(input, restrictions);
        }
        return input;
    }

    @Override
    public boolean matchesInput(String input, CommandParameter<S> parameter) {
        if (!restrictions.isEmpty()) {
            return restrictions.contains(input);
        }
        return true;
    }

    public ParameterWord<S> withRestriction(String restriction) {
        Preconditions.notNull(restriction, "not null");
        restrictions.add(restriction);
        return this;
    }

    public List<String> getRestrictions() {
        return restrictions;
    }

}
