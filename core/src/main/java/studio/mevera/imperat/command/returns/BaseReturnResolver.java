package studio.mevera.imperat.command.returns;

import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.reflect.Type;

public abstract class BaseReturnResolver<S extends Source, T> implements ReturnResolver<S, T> {

    private final Type type;

    public BaseReturnResolver(Type type) {
        this.type = type;
    }

    public BaseReturnResolver(TypeWrap<T> type) {
        this.type = type.getType();
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "BaseReturnResolver{" +
            "type=" + type +
            '}';
    }

}
