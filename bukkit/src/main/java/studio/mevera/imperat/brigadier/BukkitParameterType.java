package studio.mevera.imperat.brigadier;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.command.parameters.type.BaseParameterType;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.CommandException;

public class BukkitParameterType<T> extends BaseParameterType<BukkitSource, T> {

    private final MinecraftArgumentType minecraftArgumentType;
    private final ArgumentType<T> argumentType;
    public BukkitParameterType(Class<T> type, @NotNull MinecraftArgumentType minecraftArgumentType) {
        super(type);
        this.minecraftArgumentType = minecraftArgumentType;
        this.argumentType = minecraftArgumentType.get();
    }

    //for arg types that require parameters
    public BukkitParameterType(Class<T> type, @NotNull MinecraftArgumentType minecraftArgumentType, ArgumentType<T> argumentType) {
        super(type);
        this.minecraftArgumentType = minecraftArgumentType;
        this.argumentType = argumentType;
    }

    @Override
    public @Nullable T resolve(
            @NotNull ExecutionContext<BukkitSource> context,
            @NotNull CommandInputStream<BukkitSource> inputStream,
            @NotNull String input
    ) throws CommandException {

        try {

            T value = argumentType.parse(new BukkitStringReader(context, inputStream));
            for (int i = 1; i < minecraftArgumentType.getConsumedArgs(); i++) {
                inputStream.skipRaw();
            }
            return value;
        }catch (CommandSyntaxException ex) {
            throw new CommandException(ex.getMessage(), ex);
        }
    }

    public MinecraftArgumentType getMinecraftArgumentType() {
        return minecraftArgumentType;
    }

    public ArgumentType<T> getArgType() {
        return argumentType;
    }

    public boolean isSupported() {
        return this.minecraftArgumentType.isSupported();
    }

    // TODO almost impossible to sync suggestions due to the presence of a special brigadier context object.
    // TODO Need to figure out a way to sync our SuggestionContext with an instance brigadier's CommandContext
}
