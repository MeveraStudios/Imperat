package studio.mevera.imperat.brigadier;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;

public class BukkitArgumentType<T> extends ArgumentType<BukkitSource, T> {

    private final MinecraftArgumentType minecraftArgumentType;
    private final com.mojang.brigadier.arguments.ArgumentType<T> argumentType;

    public BukkitArgumentType(Class<T> type, @NotNull MinecraftArgumentType minecraftArgumentType) {
        super(type);
        this.minecraftArgumentType = minecraftArgumentType;
        this.argumentType = minecraftArgumentType.get();
    }

    //for arg types that require parameters
    public BukkitArgumentType(Class<T> type, @NotNull MinecraftArgumentType minecraftArgumentType, com.mojang.brigadier.arguments.ArgumentType<T> argumentType) {
        super(type);
        this.minecraftArgumentType = minecraftArgumentType;
        this.argumentType = argumentType;
    }

    @Override
    public @Nullable T parse(
            @NotNull ExecutionContext<BukkitSource> context,
            @NotNull Cursor<BukkitSource> cursor,
            @NotNull String correspondingInput
    ) throws CommandException {

        try {

            T value = argumentType.parse(new BukkitStringReader(context, cursor));
            for (int i = 1; i < minecraftArgumentType.getConsumedArgs(); i++) {
                cursor.skipRaw();
            }
            return value;
        } catch (CommandSyntaxException ex) {
            throw new CommandException(ex.getMessage(), ex);
        }
    }

    public MinecraftArgumentType getMinecraftArgumentType() {
        return minecraftArgumentType;
    }

    public com.mojang.brigadier.arguments.ArgumentType<T> getArgType() {
        return argumentType;
    }

    public boolean isSupported() {
        return this.minecraftArgumentType.isSupported();
    }

    // TODO almost impossible to sync suggestions due to the presence of a special brigadier context object.
    // TODO Need to figure out a way to sync our SuggestionContext with an instance brigadier's CommandContext
}
