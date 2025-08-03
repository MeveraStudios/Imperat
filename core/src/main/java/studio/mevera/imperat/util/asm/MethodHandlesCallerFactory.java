package studio.mevera.imperat.util.asm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.addAll;

/**
 * A {@link MethodCallerFactory} that uses the method handles API to generate
 * method callers
 */
final class MethodHandlesCallerFactory implements MethodCallerFactory {

    public static final MethodHandlesCallerFactory INSTANCE = new MethodHandlesCallerFactory();

    @Override
    public @NotNull MethodCaller createFor(@NotNull Method method) throws Throwable {
        method.setAccessible(true);
        MethodHandle handle = MethodHandles.lookup().unreflect(method);
        String methodString = method.toString();
        boolean isStatic = Modifier.isStatic(method.getModifiers());
        return new MethodCaller() {
            @Override
            public Object call(@Nullable Object instance, Object... arguments) {
                if (!isStatic) {
                    final List<Object> args = new ArrayList<>();
                    args.add(instance);
                    addAll(args, arguments);
                    try {
                        return handle.invokeWithArguments(args);
                    } catch (final Throwable e) {
                        throw new RuntimeException(e);
                    }
                }
                try {
                    return handle.invokeWithArguments(arguments);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public String toString() {
                return "MethodHandlesCaller(" + methodString + ")";
            }
        };
    }

    @Override
    public String toString() {
        return "MethodHandlesCallerFactory";
    }
}
