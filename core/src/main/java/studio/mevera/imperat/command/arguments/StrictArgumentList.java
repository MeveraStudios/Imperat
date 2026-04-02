package studio.mevera.imperat.command.arguments;

import studio.mevera.imperat.context.CommandSource;

import java.util.Collection;
import java.util.LinkedList;

public final class StrictArgumentList<S extends CommandSource> extends LinkedList<Argument<S>> {

    @Override
    public void addFirst(Argument<S> parameter) {
        if (containsSimilar(parameter)) {
            return;
        }

        super.addFirst(parameter);
    }

    @Override
    public boolean add(Argument<S> parameter) {

        if (containsSimilar(parameter)) {
            return false;
        }

        return super.add(parameter);
    }

    @Override
    public boolean addAll(Collection<? extends Argument<S>> c) {
        for (var e : c) {
            add(e);
        }
        return true;
    }


    @Override
    public boolean contains(Object o) {
        if (!(o instanceof Argument<?> parameter)) {
            return false;
        }
        return super.contains(parameter) || containsSimilar(parameter);
    }

    public boolean containsSimilar(Argument<?> parameter) {
        for (var p : this) {
            if (p.similarTo(parameter)) {
                return true;
            }
        }
        return false;
    }
}
