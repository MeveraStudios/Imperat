package studio.mevera.imperat.command.parameters;

import studio.mevera.imperat.context.Source;

import java.util.Collection;
import java.util.LinkedList;

public final class StrictParameterList<S extends Source> extends LinkedList<CommandParameter<S>> {

    @Override
    public void addFirst(CommandParameter<S> parameter) {
        if (containsSimilar(parameter))
            return;

        super.addFirst(parameter);
    }

    @Override
    public boolean add(CommandParameter<S> parameter) {

        if (containsSimilar(parameter))
            return false;

        return super.add(parameter);
    }

    @Override
    public boolean addAll(Collection<? extends CommandParameter<S>> c) {
        for (var e : c) {
            add(e);
        }
        return true;
    }


    @Override
    public boolean contains(Object o) {
        if (!(o instanceof CommandParameter<?> parameter)) return false;
        return super.contains(parameter) || containsSimilar(parameter);
    }

    public boolean containsSimilar(CommandParameter<?> parameter) {
        for (var p : this) {
            if (p.similarTo(parameter))
                return true;
        }
        return false;
    }
}
