package studio.mevera.imperat.providers;

import java.util.function.Supplier;

/**
 * A functional interface that supplies a generic dependency
 * object. This interface extends the {@link Supplier} interface
 * from the Java standard library, allowing it to be used
 * directly in scenarios where a {@code Supplier<Object>} is expected.
 * <p>
 * This interface is designed to act as a generic provider
 * of dependency objects in various contexts, enabling dependency
 * resolution or injection frameworks to handle or supply appropriate
 * objects dynamically at runtime.
 * </p>
 * Being a functional interface, it can be implemented using
 * a lambda expression or a method reference to streamline
 * the handling of such suppliers.
 */
@FunctionalInterface
public interface DependencySupplier extends Supplier<Object> {

}
