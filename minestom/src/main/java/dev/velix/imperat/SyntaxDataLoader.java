package dev.velix.imperat;

import dev.velix.imperat.command.Command;
import dev.velix.imperat.command.CommandUsage;
import dev.velix.imperat.command.parameters.CommandParameter;
import dev.velix.imperat.util.Patterns;
import dev.velix.imperat.util.TypeUtility;
import net.kyori.adventure.text.Component;
import net.minestom.server.color.Color;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.particle.Particle;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("unchecked")
final class SyntaxDataLoader {

    static CommandExecutor loadExecutor(MinestomImperat imperat) {
        return (sender, context) -> {
            imperat.dispatch(imperat.wrapSender(sender), context.getCommandName(), context.getInput());
        };
    }

    static @NotNull CommandCondition loadCondition(MinestomImperat imperat, CommandUsage<MinestomSource> usage) {
        return (sender, commandString) ->
            imperat.config().getPermissionResolver()
                .hasUsagePermission(imperat.wrapSender(sender), usage);
    }

    static <T> Argument<?>[] loadArguments(
        MinestomImperat imperat,
        Command<MinestomSource> imperatCommand,
        CommandUsage<MinestomSource> usage
    ) {

        Argument<?>[] args = new Argument[usage.size()];
        List<CommandParameter<MinestomSource>> parameters = usage.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            CommandParameter<MinestomSource> parameter = parameters.get(i);
            Argument<T> arg = (Argument<T>) argFromParameter(parameter);
            if (parameter.isOptional()) {
                arg.setDefaultValue(
                    commandSender -> {
                        var supplier = parameter.getDefaultValueSupplier();
                        return (T) supplier.supply(imperat.wrapSender(commandSender), parameter);
                    });

            }
            arg.setSuggestionCallback((sender, context, suggestion) -> {
                var source = imperat.wrapSender(sender);
                for (var completion : imperat.autoComplete(imperatCommand, source, context.getCommandName(), context.getInput()).join()) {
                    suggestion.addEntry(new SuggestionEntry(completion));
                }
            });
            //TODO add argument callback somehow
            args[i] = new ArgumentDecorator<>(parameter, arg);
        }
        return args;
    }

    private static Argument<?> argFromParameter(CommandParameter<MinestomSource> parameter) {
        var type = parameter.valueType();
        var id = parameter.name();

        if (parameter.isCommand()) {
            return ArgumentType.Literal(id);
        }

        if (parameter.isGreedy()) {
            return ArgumentType.StringArray(id);
        }

        if (parameter.isFlag()) {

            if (parameter.asFlagParameter().isSwitch()) {
                return ArgumentType.Word(id).filter(Patterns::isInputFlag);
            }

            return ArgumentType.Group(
                id, ArgumentType.Word(id).filter(Patterns::isInputFlag),
                from("value", parameter.asFlagParameter().inputValueType())
            );
        }

        return from(id, type);
    }

    private static Argument<?> from(String id, Type type) {

        if (TypeUtility.matches(type, String.class))
            return ArgumentType.String(id);

        if (TypeUtility.matches(type, Integer.class)) {
            return ArgumentType.Integer(id);
        }

        if (TypeUtility.matches(type, boolean.class))
            return ArgumentType.Boolean(id);

        if (TypeUtility.matches(type, double.class))
            return ArgumentType.Double(id);

        if (TypeUtility.matches(type, float.class))
            return ArgumentType.Float(id);


        if (TypeUtility.matches(type, Enum.class))
            return ArgumentType.Enum(id, (Class<? extends Enum<?>>) type);

        // Minestom specific types
        //TODO add value resolvers and suggestion resolvers for these extra types
        if (TypeUtility.matches(type, Color.class))
            return ArgumentType.Color(id);

        if (TypeUtility.matches(type, Particle.class))
            return ArgumentType.Particle(id);

        if (TypeUtility.matches(type, Block.class))
            return ArgumentType.BlockState(id);

        if (TypeUtility.matches(type, UUID.class))
            return ArgumentType.UUID(id);

        if (TypeUtility.matches(type, ItemStack.class))
            return ArgumentType.ItemStack(id);

        if (TypeUtility.matches(type, Component.class))
            return ArgumentType.Component(id);
        
        /*if (TypeUtility.matches(valueType, RelativeVec.class))
            return ArgumentType.RelativeVec3(id);
        
        if (TypeUtility.matches(valueType, RelativeVec2.class))
            return ArgumentType.RelativeVec2(id);
        */

        throw new IllegalArgumentException("Unsupported parameter valueType: " + type);
    }

}
