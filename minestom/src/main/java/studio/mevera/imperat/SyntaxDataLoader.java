package studio.mevera.imperat;

import net.kyori.adventure.text.Component;
import net.minestom.server.color.Color;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.particle.Particle;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.util.Patterns;
import studio.mevera.imperat.util.TypeUtility;

import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("unchecked")
final class SyntaxDataLoader {

    static CommandExecutor loadExecutor(MinestomImperat imperat) {
        return (sender, commandContext) -> {
            String input = commandContext.getInput();
            int index = input.indexOf(' ');
            String rawArgsOneLine = "";
            if (index != -1) {
                rawArgsOneLine = input.substring(input.indexOf(' '));
            }

            var imperatSender = imperat.wrapSender(sender);
            if (rawArgsOneLine.isBlank()) {
                imperat.execute(
                        imperatSender,
                        commandContext.getCommandName()
                );
            } else {
                imperat.execute(
                        imperatSender,
                        commandContext.getCommandName(),
                        rawArgsOneLine
                );
            }
        };
    }

    static @NotNull CommandCondition loadCondition(MinestomImperat imperat, CommandUsage<MinestomSource> usage) {
        return (sender, commandString) -> imperat.config().getPermissionChecker().hasPermission(imperat.wrapSender(sender), usage);
    }

    static <T> Argument<?>[] loadArguments(
            MinestomImperat imperat,
            Command<MinestomSource> imperatCommand,
            CommandUsage<MinestomSource> usage
    ) {
        Argument<?>[] args = new Argument[usage.size()];
        List<studio.mevera.imperat.command.parameters.Argument<MinestomSource>> parameters = usage.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            studio.mevera.imperat.command.parameters.Argument<MinestomSource> parameter = parameters.get(i);
            Argument<T> arg = (Argument<T>) argFromParameter(parameter);
            arg.setSuggestionCallback((sender, context, suggestion) -> {
                String in = context.getInput();
                if (in.charAt(0) == '/') {
                    in = in.substring(1);
                }
                var source = imperat.wrapSender(sender);
                for (var completion : imperat.autoComplete(source, in).join()) {
                    suggestion.addEntry(new SuggestionEntry(completion));
                }
            });
            //TODO add argument callback somehow
            args[i] = new ArgumentDecorator<>(parameter, arg);
        }
        return args;
    }

    private static Argument<?> argFromParameter(studio.mevera.imperat.command.parameters.Argument<MinestomSource> parameter) {
        var type = parameter.valueType();
        var id = parameter.name();

        if (parameter.isCommand()) {
            return net.minestom.server.command.builder.arguments.ArgumentType.Literal(id);
        }

        if (parameter.isGreedy()) {
            return  net.minestom.server.command.builder.arguments.ArgumentType.StringArray(id);
        }

        if (parameter.isFlag()) {
            if (parameter.asFlagParameter().isSwitch()) {
                return  net.minestom.server.command.builder.arguments.ArgumentType.Word(id).filter(Patterns::isInputFlag);
            }

            return  net.minestom.server.command.builder.arguments.ArgumentType.Group(
                    id,  net.minestom.server.command.builder.arguments.ArgumentType.Word(id).filter(Patterns::isInputFlag),
                    from("value", parameter.asFlagParameter().inputValueType())
            );
        }

        return from(id, type);
    }

    private static Argument<?> from(String id, Type type) {

        if (TypeUtility.matches(type, String.class)) {
            return  net.minestom.server.command.builder.arguments.ArgumentType.String(id);
        }

        if (TypeUtility.matches(type, Integer.class)) {
            return  net.minestom.server.command.builder.arguments.ArgumentType.Integer(id);
        }

        if (TypeUtility.matches(type, boolean.class)) {
            return  net.minestom.server.command.builder.arguments.ArgumentType.Boolean(id);
        }

        if (TypeUtility.matches(type, double.class)) {
            return  net.minestom.server.command.builder.arguments.ArgumentType.Double(id);
        }

        if (TypeUtility.matches(type, float.class)) {
            return  net.minestom.server.command.builder.arguments.ArgumentType.Float(id);
        }


        if (TypeUtility.matches(type, Enum.class)) {
            return  net.minestom.server.command.builder.arguments.ArgumentType.Enum(id, (Class<? extends Enum<?>>) type);
        }

        // Minestom specific types
        //TODO add value resolvers and suggestion resolvers for these extra types
        if (TypeUtility.matches(type, Color.class)) {
            return  net.minestom.server.command.builder.arguments.ArgumentType.Color(id);
        }

        if (TypeUtility.matches(type, Particle.class)) {
            return  net.minestom.server.command.builder.arguments.ArgumentType.Particle(id);
        }

        if (TypeUtility.matches(type, Block.class)) {
            return  net.minestom.server.command.builder.arguments.ArgumentType.BlockState(id);
        }

        if (TypeUtility.matches(type, UUID.class)) {
            return  net.minestom.server.command.builder.arguments.ArgumentType.UUID(id);
        }

        if (TypeUtility.matches(type, ItemStack.class)) {
            return  net.minestom.server.command.builder.arguments.ArgumentType.ItemStack(id);
        }

        if (TypeUtility.matches(type, Component.class)) {
            return  net.minestom.server.command.builder.arguments.ArgumentType.Component(id);
        }
        
        /*if (TypeUtility.matches(valueType, RelativeVec.class))
            return ArgumentType.RelativeVec3(id);
        
        if (TypeUtility.matches(valueType, RelativeVec2.class))
            return ArgumentType.RelativeVec2(id);
        */

        return  net.minestom.server.command.builder.arguments.ArgumentType.Word(id);
    }

}
