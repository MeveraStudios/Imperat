package studio.mevera.imperat;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.context.Source;

/**
 * A class that manages parsing {@link Command}
 * into brigadier {@link CommandNode}
 *
 * @param <S> the command-source valueType
 */
public sealed interface BrigadierManager<S extends Source> permits BaseBrigadierManager {

    /**
     * Converts the original command source from brigadier
     * into the platform's command-source
     *
     * @param commandSource the command source
     * @return the platform's command sender/source
     */
    S wrapCommandSource(Object commandSource);

    /**
     * Registers the argument valueType to its class valueType
     *
     * @param type                 the valueType to register to the value-valueType obj
     * @param argumentTypeResolver the value valueType resolver
     * @param <T>                  the valueType parameter for the valueType.
     */
    <T> void registerArgumentResolver(Class<T> type, ArgumentTypeResolver argumentTypeResolver);

    /**
     * Registers the argument valueType resolver
     *
     * @param argumentTypeResolver the value valueType resolver
     */
    void registerArgumentResolver(ArgumentTypeResolver argumentTypeResolver);


    /**
     * Fetches the argument valueType from the parameter
     *
     * @param parameter the parameter
     * @return the {@link ArgumentType} for the {@link CommandParameter}
     */
    @NotNull
    ArgumentType<?> getArgumentType(CommandParameter<S> parameter);

    /**
     * Parses the registered {@link Command} to brigadier node
     *
     * @return the parsed node
     */
    <T> LiteralCommandNode<T> parseCommandIntoNode(Command<S> command);

}
