package studio.mevera.imperat;

import net.minestom.server.command.builder.arguments.Argument;
import studio.mevera.imperat.responses.ResponseKey;

import java.lang.reflect.Type;
import java.util.function.BiFunction;
import java.util.function.Supplier;

record MinestomArgTypeData(
        Type minestomType,
        BiFunction<Type, String, Argument<?>> argumentLoader,
        int numberOfArgsToConsume,
        ResponseKey responseKey,
        Supplier<String> responseMessage,
        String[] responsePlaceholders
) {

}
