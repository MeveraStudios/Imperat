package studio.mevera.imperat.config;

import org.jetbrains.annotations.NotNull;

/**
 * Static helper isolating the kotlinx-coroutines feature-detection and
 * scope-validation logic that previously lived inline on {@code ImperatConfigImpl}.
 *
 * <p>Coroutines are an optional dependency. If {@code kotlinx.coroutines} is not
 * on the classpath, {@link #isAvailable()} returns {@code false} and any
 * attempt to set a scope through {@link #requireScope(Object)} fails fast with
 * a descriptive {@link IllegalStateException}.</p>
 */
public final class CoroutineSupport {

    private static final boolean AVAILABLE;
    private static final Class<?> SCOPE_CLASS;

    static {
        boolean available = false;
        Class<?> scopeClass = null;
        try {
            scopeClass = Class.forName("kotlinx.coroutines.CoroutineScope");
            available = true;
        } catch (ClassNotFoundException ignored) {
            // optional dependency
        }
        AVAILABLE = available;
        SCOPE_CLASS = scopeClass;
    }

    private CoroutineSupport() {
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    /**
     * Validates that {@code scope} is a non-null {@code kotlinx.coroutines.CoroutineScope}
     * and returns it for assignment. Throws {@link IllegalStateException} if
     * coroutines are not on the classpath, or {@link IllegalArgumentException}
     * if the supplied object is the wrong type.
     */
    public static @NotNull Object requireScope(@NotNull Object scope) {
        if (!AVAILABLE) {
            throw new IllegalStateException(
                    "Cannot set coroutine scope - kotlinx.coroutines is not available on the classpath. "
                            + "Add 'org.jetbrains.kotlinx:kotlinx-coroutines-core' dependency to enable coroutine support.");
        }
        if (!SCOPE_CLASS.isInstance(scope)) {
            throw new IllegalArgumentException(
                    "Provided scope must be an instance of kotlinx.coroutines.CoroutineScope, got: "
                            + scope.getClass().getName());
        }
        return scope;
    }
}
