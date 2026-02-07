package studio.mevera.imperat;

import net.minestom.server.command.builder.arguments.Argument;

import java.lang.reflect.Type;
import java.util.function.BiFunction;

public record MinestomArgTypeData(Type minestomType, BiFunction<Type, String, Argument<?>> argumentLoader, int numberOfArgsToConsume) {

}
