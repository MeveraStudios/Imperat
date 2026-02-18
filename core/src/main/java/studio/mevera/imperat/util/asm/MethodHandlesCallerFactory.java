package studio.mevera.imperat.util.asm;

import static java.util.Collections.addAll;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.exception.CommandException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link MethodCallerFactory} that uses the method handles API to generate
 * method callers
 */
final class MethodHandlesCallerFactory implements MethodCallerFactory {

    public static final MethodHandlesCallerFactory INSTANCE = new MethodHandlesCallerFactory();

    /**
     * Sneaky throw idiom to rethrow checked exceptions as unchecked without wrapping them.
     * This uses type erasure to trick the compiler.
     */
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

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
                    } catch (final RuntimeException | Error e) {
                        // Rethrow runtime exceptions and errors as-is
                        throw e;
                    } catch (final CommandException e) {
                        // Use sneaky throw to rethrow CommandException without wrapping
                        sneakyThrow(e);
                        return null; // unreachable
                    } catch (final Throwable e) {
                        // Wrap other checked exceptions in RuntimeException
                        throw new RuntimeException(e);
                    }
                }
                try {
                    return handle.invokeWithArguments(arguments);
                } catch (RuntimeException | Error e) {
                    // Rethrow runtime exceptions and errors as-is
                    throw e;
                } catch (CommandException e) {
                    // Use sneaky throw to rethrow CommandException without wrapping
                    sneakyThrow(e);
                    return null; // unreachable
                } catch (Throwable e) {
                    // Wrap other checked exceptions in RuntimeException
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
