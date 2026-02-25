package studio.mevera.imperat;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.minestom.server.color.Color;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.particle.Particle;
import net.minestom.server.utils.location.RelativeVec;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.responses.MinestomResponseKey;
import studio.mevera.imperat.responses.ResponseKey;
import studio.mevera.imperat.util.Patterns;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
final class SyntaxDataLoader {

    private final static Map<Type, MinestomArgTypeData> typeData = new HashMap<>();

    static {
            //register default type data for Minestom's built in argument types
        registerTypeData(String.class, 1, (type, id) -> net.minestom.server.command.builder.arguments.ArgumentType.String(id),
                MinestomResponseKey.INVALID_STRING, () -> "Invalid string '%input%'", "input");
        registerTypeData(Integer.class, 1, (type, id) -> net.minestom.server.command.builder.arguments.ArgumentType.Integer(id),
                MinestomResponseKey.INVALID_INTEGER, () -> "Invalid integer '%input%'", "input");
        registerTypeData(boolean.class, 1, (type, id) -> net.minestom.server.command.builder.arguments.ArgumentType.Boolean(id),
                ResponseKey.INVALID_BOOLEAN, () -> "Invalid boolean '%input%'", "input");
        registerTypeData(double.class, 1, (type, id) -> net.minestom.server.command.builder.arguments.ArgumentType.Double(id),
                MinestomResponseKey.INVALID_DOUBLE, () -> "Invalid double '%input%'", "input");
        registerTypeData(float.class, 1, (type, id) -> net.minestom.server.command.builder.arguments.ArgumentType.Float(id),
                MinestomResponseKey.INVALID_FLOAT, () -> "Invalid float '%input%'", "input");
        registerTypeData(long.class, 1, (type, id) -> net.minestom.server.command.builder.arguments.ArgumentType.Long(id),
                MinestomResponseKey.INVALID_LONG, () -> "Invalid long '%input%'", "input");
        registerTypeData(Long.class, 1, (type, id) -> net.minestom.server.command.builder.arguments.ArgumentType.Long(id),
                MinestomResponseKey.INVALID_LONG, () -> "Invalid long '%input%'", "input");
        registerTypeData(Enum.class, 1,
                (type, id) -> net.minestom.server.command.builder.arguments.ArgumentType.Enum(id, (Class<? extends Enum<?>>) type),
                ResponseKey.INVALID_ENUM, () -> "Invalid enum value '%input%'", "input");

            // Minestom specific types
        registerTypeData(Color.class, 1, (type, id) -> net.minestom.server.command.builder.arguments.ArgumentType.Color(id),
                MinestomResponseKey.INVALID_COLOR, () -> "Invalid color '%input%'", "input");
        registerTypeData(TimeUnit.class, 1, (type, id) -> net.minestom.server.command.builder.arguments.ArgumentType.Time(id),
                MinestomResponseKey.INVALID_TIME, () -> "Invalid time '%input%'", "input");
        registerTypeData(Particle.class, 1, (type, id) -> net.minestom.server.command.builder.arguments.ArgumentType.Particle(id),
                MinestomResponseKey.INVALID_PARTICLE, () -> "Invalid particle '%input%'", "input");
        registerTypeData(Block.class, 1, (type, id) -> net.minestom.server.command.builder.arguments.ArgumentType.BlockState(id),
                MinestomResponseKey.INVALID_BLOCK_STATE, () -> "Invalid block state '%input%'", "input");
        registerTypeData(UUID.class, 1, (type, id) -> net.minestom.server.command.builder.arguments.ArgumentType.UUID(id),
                ResponseKey.INVALID_UUID, () -> "Invalid UUID '%input%'", "input");
        registerTypeData(ItemStack.class, 1, (type, id) -> net.minestom.server.command.builder.arguments.ArgumentType.ItemStack(id),
                MinestomResponseKey.INVALID_ITEM_STACK, () -> "Invalid item stack '%input%'", "input");
        registerTypeData(Component.class, 1, (type, id) -> net.minestom.server.command.builder.arguments.ArgumentType.Component(id),
                MinestomResponseKey.INVALID_COMPONENT, () -> "Invalid component '%input%'", "input");
        registerTypeData(Entity.class, 1, (type, id) -> net.minestom.server.command.builder.arguments.ArgumentType.Entity(id),
                MinestomResponseKey.INVALID_ENTITY, () -> "Invalid entity selector '%input%'", "input");

            // Coordinate types (3D and 2D)
            //Minestom does not differentiate between vec2 and vec3 arguments, so we will just use the 3D version for both and let the argument type handle the parsing.
        registerTypeData(RelativeVec.class, 3, (type, id) -> net.minestom.server.command.builder.arguments.ArgumentType.RelativeVec3(id),
                MinestomResponseKey.INVALID_RELATIVE_VEC, () -> "Invalid relative vector '%input%'", "input");
        registerTypeData(Point.class, 3, (type, id) -> net.minestom.server.command.builder.arguments.ArgumentType.RelativeBlockPosition(id),
                MinestomResponseKey.INVALID_BLOCK_POSITION, () -> "Invalid block position '%input%'", "input");

            // NBT types (using Adventure NBT, which is included with Minestom)
        registerTypeData(BinaryTag.class, 1, (type, id) -> net.minestom.server.command.builder.arguments.ArgumentType.NBT(id),
                MinestomResponseKey.INVALID_NBT, () -> "Invalid NBT '%input%'", "input");
        registerTypeData(CompoundBinaryTag.class, 1, (type, id) -> net.minestom.server.command.builder.arguments.ArgumentType.NbtCompound(id),
                MinestomResponseKey.INVALID_NBT_COMPOUND, () -> "Invalid NBT compound '%input%'", "input");

            // ResourceLocation (uses Adventure Key in modern Minestom)
        registerTypeData(Key.class, 1, (type, id) -> net.minestom.server.command.builder.arguments.ArgumentType.ResourceLocation(id),
                MinestomResponseKey.INVALID_RESOURCE_LOCATION, () -> "Invalid resource location '%input%'", "input");
    }

    public static void registerTypeData(Type type, MinestomArgTypeData data) {
        typeData.put(type, data);
    }

    public static void registerTypeData(
            Type type, int numberOfArgs,
            BiFunction<Type, String, Argument<?>> argumentCreator,
            ResponseKey responseKey, Supplier<String> responseMessage, String... responsePlaceholders
    ) {
        registerTypeData(type, new MinestomArgTypeData(
                type, argumentCreator, numberOfArgs,
                responseKey, responseMessage, responsePlaceholders
        ));
    }

    public static <T> void migrateToImperatTypeData(MinestomImperat imperat) {
        var responseRegistry = imperat.config().getResponseRegistry();
        typeData.forEach((type, argData)-> {
            MinestomArgumentType<T> imperatArgType = new MinestomArgumentType<>(
                    argData.minestomType(),
                    argData.argumentLoader(),
                    argData.numberOfArgsToConsume()
            );
            imperat.config().registerArgType(type, imperatArgType);
            responseRegistry.registerResponse(
                    argData.responseKey(),
                    argData.responseMessage(),
                    argData.responsePlaceholders()
            );
        });
    }

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

    static @NotNull CommandCondition loadCondition(MinestomImperat imperat, CommandPathway<MinestomSource> usage) {
        return (sender, _) -> imperat.config().getPermissionChecker().hasPermission(imperat.wrapSender(sender), usage);
    }

    static <T> Argument<?>[] loadArguments(
            MinestomImperat imperat,
            CommandPathway<MinestomSource> usage
    ) {
        Argument<?>[] args = new Argument[usage.size()];
        List<studio.mevera.imperat.command.parameters.Argument<MinestomSource>> parameters = usage.getArguments();
        for (int i = 0; i < parameters.size(); i++) {
            studio.mevera.imperat.command.parameters.Argument<MinestomSource> parameter = parameters.get(i);
            Argument<T> minestomArg = (Argument<T>) argFromParameter(imperat, parameter);
            minestomArg.setSuggestionCallback((sender, context, suggestion) -> {
                String in = context.getInput();
                if (in.charAt(0) == '/') {
                    in = in.substring(1);
                }
                var source = imperat.wrapSender(sender);
                for (var completion : imperat.autoComplete(source, in).join()) {
                    suggestion.addEntry(new SuggestionEntry(completion));
                }
            });
            args[i] = new ArgumentDecorator<>(parameter, minestomArg);
        }
        return args;
    }

    private static Argument<?> argFromParameter(MinestomImperat imperat,
            studio.mevera.imperat.command.parameters.Argument<MinestomSource> parameter) {
        var type = parameter.valueType();
        var id = parameter.getName();

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

            var inputType = parameter.asFlagParameter().inputValueType();
            var inputArgType = imperat.config().getArgumentType(inputType);
            if(!(inputArgType instanceof MinestomArgumentType<?> minestomArgumentType)) {
                throw new IllegalArgumentException(
                        String.format(
                                "Flag parameter '%s' has input type of '%s' which isn't a valid Minestom argument type",
                                parameter.format(), inputType.getTypeName()
                        )
                );
            }

            return  net.minestom.server.command.builder.arguments.ArgumentType.Group(
                    id,  net.minestom.server.command.builder.arguments.ArgumentType.Word(id).filter(Patterns::isInputFlag),
                    minestomArgumentType.getMinestomType("value")
            );
        }

        if(parameter.type() instanceof MinestomArgumentType<?>) {
            return ((MinestomArgumentType<?>) parameter.type()).getMinestomType(parameter.getName());
        }

        return net.minestom.server.command.builder.arguments.ArgumentType.String(id);
    }

}
